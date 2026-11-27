from __future__ import annotations

from collections.abc import Mapping
from typing import Any

from langchain.agents import create_agent
from langchain_core.messages import AIMessage, ToolMessage
from langgraph.graph import END, START, StateGraph
from langgraph.graph.state import CompiledStateGraph
from langgraph.runtime import Runtime
from langgraph.types import interrupt

from app.agent.context import AgentRuntimeContext
from app.agent.llm import LlmFactory
from app.agent.prompts import SHOPPING_ADVISOR_SYSTEM_PROMPT
from app.agent.state import CustomerServiceState, OrderDraft, empty_turn_outputs
from app.agent.subgraphs import (
    collect_unique_citations,
    parse_tool_message_payload,
    slice_new_messages,
)
from app.agent.tools._common import format_decimal, pick_field, to_int, to_optional_int
from app.agent.tools.shopping_tools import build_shopping_tools
from app.clients.mall_client import MallClientError


def _merge_products(
    target: list[dict[str, Any]],
    seen_ids: set[int],
    candidates: list[Any],
) -> None:
    for item in candidates:
        if not isinstance(item, Mapping):
            continue
        product = dict(item)
        pid = to_optional_int(product.get("id"))
        if pid is not None:
            if pid in seen_ids:
                continue
            seen_ids.add(pid)
        target.append(product)


def _extract_shopping_outputs(
    new_messages: list[Any],
) -> tuple[list[dict[str, Any]], OrderDraft | None, list[dict[str, Any]], list[dict[str, Any]]]:
    recommended_products: list[dict[str, Any]] = []
    seen_product_ids: set[int] = set()
    detail_product: dict[str, Any] | None = None
    pending_draft: OrderDraft | None = None

    for msg in new_messages:
        if not isinstance(msg, ToolMessage):
            continue
        payload = parse_tool_message_payload(msg.content)
        if not payload or not payload.get("ok"):
            continue

        if msg.name in ("list_hot_and_popular_products", "search_products"):
            raw_products = payload.get("products")
            if isinstance(raw_products, list):
                _merge_products(recommended_products, seen_product_ids, raw_products)
        elif msg.name == "get_product_detail_and_skus":
            raw_product = payload.get("product")
            if isinstance(raw_product, Mapping):
                detail_product = dict(raw_product)
        elif msg.name == "prepare_order_draft":
            raw_draft = payload.get("draft")
            if isinstance(raw_draft, Mapping):
                pending_draft = dict(raw_draft)  # type: ignore[assignment]

    if not recommended_products and detail_product is not None and pending_draft is None:
        _merge_products(recommended_products, seen_product_ids, [detail_product])

    rag_citations = collect_unique_citations(new_messages, {"search_presale_kb"})
    ui_events: list[dict[str, Any]] = []
    if recommended_products:
        ui_events.append(
            {
                "card_type": "product_list",
                "payload": {"products": recommended_products},
            }
        )
    if rag_citations:
        ui_events.append(
            {
                "card_type": "citations",
                "payload": {"citations": rag_citations},
            }
        )
    return recommended_products, pending_draft, rag_citations, ui_events


async def _resolve_owned_address_id(
    draft: Mapping[str, Any],
    resume_payload: Mapping[str, Any],
    mall_client: Any,
    user_id: int,
) -> int | None:
    raw_address_id = resume_payload.get("address_id")
    if raw_address_id is None:
        selected_id = to_optional_int(draft.get("address_id"))
    else:
        selected_id = to_optional_int(raw_address_id)
        if selected_id is None:
            return None

    if selected_id is None or selected_id <= 0:
        return None

    available_addresses = draft.get("available_addresses") or []
    owned_ids = {
        to_optional_int(addr.get("id"))
        for addr in available_addresses
        if isinstance(addr, Mapping) and to_optional_int(addr.get("id")) is not None
    }
    if owned_ids:
        return selected_id if selected_id in owned_ids else None

    if selected_id == to_optional_int(draft.get("address_id")):
        return selected_id

    if mall_client is not None and hasattr(mall_client, "list_user_addresses"):
        try:
            fetched = await mall_client.list_user_addresses(user_id)
        except MallClientError:
            return None
        fetched_ids = {
            to_optional_int(addr.get("id"))
            for addr in (fetched or [])
            if isinstance(addr, Mapping) and to_optional_int(addr.get("id")) is not None
        }
        return selected_id if selected_id in fetched_ids else None

    return None


def _has_order_draft(state: CustomerServiceState) -> str:
    return "confirm" if state.get("pending_order_draft") else "done"


def _route_after_confirm(state: CustomerServiceState) -> str:
    resume_payload = state.get("order_resume_payload")
    if isinstance(resume_payload, Mapping) and resume_payload.get("action") == "confirm":
        return "confirm"
    return "cancel"


def build_recommend_order_subgraph(
    *,
    mall_client: Any,
    kb_retriever: Any,
    llm_factory: LlmFactory,
) -> CompiledStateGraph:
    """构建商品推荐与基于 `interrupt` 人工确认下单的子图。"""
    shopping_tools = build_shopping_tools(mall_client, kb_retriever)
    shopping_agent = create_agent(
        model=llm_factory("shopping"),
        tools=shopping_tools,
        system_prompt=SHOPPING_ADVISOR_SYSTEM_PROMPT,
        context_schema=AgentRuntimeContext,
    )

    async def shopping_advisor(
        state: CustomerServiceState,
        runtime: Runtime[AgentRuntimeContext],
    ) -> dict[str, Any]:
        prior_count = len(state["messages"])
        result = await shopping_agent.ainvoke(
            {"messages": state["messages"]},
            context=runtime.context,
        )
        new_messages = slice_new_messages(result["messages"], prior_count)
        (
            recommended_products,
            pending_draft,
            rag_citations,
            ui_events,
        ) = _extract_shopping_outputs(new_messages)
        return {
            "messages": new_messages,
            "recommended_products": recommended_products,
            "pending_order_draft": pending_draft,
            "rag_citations": rag_citations,
            "ui_events": ui_events,
        }

    def confirm_order_interrupt(state: CustomerServiceState) -> dict[str, Any]:
        draft = state.get("pending_order_draft")
        decision = interrupt({"type": "order_confirm", "draft": draft})
        resume_payload = (
            dict(decision) if isinstance(decision, Mapping) else {"action": "cancel"}
        )
        return {"order_resume_payload": resume_payload}

    async def execute_create_order(
        state: CustomerServiceState,
        runtime: Runtime[AgentRuntimeContext],
    ) -> dict[str, Any]:
        draft = state.get("pending_order_draft")
        resume_payload = state.get("order_resume_payload") or {}
        base_clear = {
            **empty_turn_outputs(),
            "pending_order_draft": None,
            "order_resume_payload": None,
        }
        if not isinstance(draft, Mapping):
            return {
                **base_clear,
                "messages": [AIMessage(content="当前待确认下单草稿已失效，请重新挑选商品下单。")],
            }

        selected_address_id = await _resolve_owned_address_id(
            draft,
            resume_payload,
            mall_client,
            runtime.context.user_id,
        )
        if selected_address_id is None:
            return {
                **base_clear,
                "messages": [
                    AIMessage(
                        content="所选收货地址无效或不属于当前账户，未能提交订单。请核对个人中心收货地址后重试。"
                    )
                ],
            }

        try:
            order_result = await mall_client.direct_create_order(
                user_id=runtime.context.user_id,
                product_id=int(draft["product_id"]),
                sku_id=int(draft["sku_id"]),
                quantity=int(draft["quantity"]),
                address_id=selected_address_id,
                idempotency_key=str(draft["draft_id"]),
            )
        except MallClientError as exc:
            return {
                **base_clear,
                "messages": [
                    AIMessage(content=f"订单创建失败（{exc.message}），请稍后重试或联系人工客服。")
                ],
            }

        order_no = str(pick_field(order_result, "orderNo", "order_no", default="") or "")
        pay_price = format_decimal(
            pick_field(order_result, "payPrice", "pay_price", default=draft.get("total_pay_price"))
        )
        remaining_seconds = to_int(
            pick_field(order_result, "remainingSeconds", "remaining_seconds"),
            0,
        )
        payment_url = str(
            pick_field(order_result, "paymentUrl", "payment_url", default="")
            or (f"/order_payment?orderNo={order_no}" if order_no else "")
        )
        created_order = {
            "order_no": order_no,
            "product_id": draft.get("product_id"),
            "product_name": draft.get("product_name"),
            "product_image": draft.get("product_image"),
            "sku_id": draft.get("sku_id"),
            "sku_name": draft.get("sku_name"),
            "quantity": draft.get("quantity"),
            "pay_price": pay_price,
            "remaining_seconds": remaining_seconds,
            "payment_url": payment_url,
            "paid": to_int(order_result.get("paid"), 0),
            "status": to_int(order_result.get("status"), 0),
        }
        countdown_hint = (
            f"请在 {max(remaining_seconds // 60, 1)} 分钟内完成支付。"
            if remaining_seconds > 0
            else "请尽快前往收银台完成支付。"
        )
        return {
            **base_clear,
            "created_order": created_order,
            "ui_events": [{"card_type": "order_created", "payload": created_order}],
            "messages": [
                AIMessage(
                    content=(
                        f"已为您创建待支付订单（订单号：{order_no}，应付金额：¥{pay_price}），"
                        f"{countdown_hint}请点击下方「立即去支付」前往支付。"
                    )
                )
            ],
        }

    def cancel_order_draft(state: CustomerServiceState) -> dict[str, Any]:
        resume_payload = state.get("order_resume_payload") or {}
        reason = resume_payload.get("reason") if isinstance(resume_payload, Mapping) else None
        text = (
            "已为您取消上一笔待确认订单草稿。"
            if reason == "superseded_by_new_message"
            else "好的，已为您取消本次下单确认。如需调整规格或推荐其他商品，随时告诉我。"
        )
        return {
            **empty_turn_outputs(),
            "pending_order_draft": None,
            "order_resume_payload": None,
            "messages": [AIMessage(content=text)],
        }

    subgraph = StateGraph(CustomerServiceState, context_schema=AgentRuntimeContext)
    subgraph.add_node("shopping_advisor", shopping_advisor)
    subgraph.add_node("confirm_order_interrupt", confirm_order_interrupt)
    subgraph.add_node("execute_create_order", execute_create_order)
    subgraph.add_node("cancel_order_draft", cancel_order_draft)

    subgraph.add_edge(START, "shopping_advisor")
    subgraph.add_conditional_edges(
        "shopping_advisor",
        _has_order_draft,
        {
            "confirm": "confirm_order_interrupt",
            "done": END,
        },
    )
    subgraph.add_conditional_edges(
        "confirm_order_interrupt",
        _route_after_confirm,
        {
            "confirm": "execute_create_order",
            "cancel": "cancel_order_draft",
        },
    )
    subgraph.add_edge("execute_create_order", END)
    subgraph.add_edge("cancel_order_draft", END)
    return subgraph.compile()
