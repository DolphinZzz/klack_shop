from __future__ import annotations

from contextlib import asynccontextmanager
from dataclasses import dataclass
from types import SimpleNamespace
from typing import Any
import uuid

import json
import pytest
from langchain_core.language_models.fake_chat_models import GenericFakeChatModel
from langchain_core.messages import AIMessage, AIMessageChunk, HumanMessage
from langchain_core.outputs import ChatGenerationChunk
from langchain_core.runnables import RunnableLambda
from langgraph.checkpoint.memory import InMemorySaver
from langgraph.types import Command


class StubChatModel(GenericFakeChatModel):
    """Deterministic fake ChatModel supporting bind_tools, streaming tool calls, and structured output."""

    structured_outputs: list[Any] = []

    def bind_tools(self, tools, **kwargs):
        return self

    def _stream(self, messages, stop=None, run_manager=None, **kwargs):
        chat_result = self._generate(messages, stop=stop, run_manager=run_manager, **kwargs)
        msg = chat_result.generations[0].message
        if isinstance(msg, AIMessage) and not msg.content and msg.tool_calls:
            tool_call_chunks = [
                {
                    "name": tc["name"],
                    "args": json.dumps(tc.get("args") or {}),
                    "id": tc.get("id"),
                    "index": idx,
                }
                for idx, tc in enumerate(msg.tool_calls)
            ]
            chunk = ChatGenerationChunk(
                message=AIMessageChunk(content="", id=msg.id, tool_call_chunks=tool_call_chunks)
            )
            yield chunk
            return
        if isinstance(msg, AIMessage) and msg.content:
            tokens = msg.content.split(" ")
            for idx, token in enumerate(tokens):
                piece = token if idx == len(tokens) - 1 else token + " "
                chunk = ChatGenerationChunk(message=AIMessageChunk(content=piece, id=msg.id))
                if run_manager:
                    run_manager.on_llm_new_token(piece, chunk=chunk)
                yield chunk
            return
        yield ChatGenerationChunk(message=AIMessageChunk(content="", id=msg.id))

    def with_structured_output(self, schema, **kwargs):
        model_self = self

        async def _ainvoke(messages, config=None):
            if model_self.structured_outputs:
                item = model_self.structured_outputs.pop(0)
                if callable(item):
                    item = item(messages)
                if isinstance(item, schema):
                    return item
                return schema.model_validate(item)
            raise AssertionError(f"No stub structured output left for schema {schema.__name__}")

        def _invoke(messages, config=None):
            raise RuntimeError("Sync structured output not used in async graph tests")

        return RunnableLambda(_invoke, afunc=_ainvoke)


class FakeKbRetriever:
    """In-memory KB retriever returning deterministic law and policy citations."""

    def __init__(self, hits: list[Any] | None = None) -> None:
        self.calls: list[tuple[str, list[str] | None, int]] = []
        self._hits = hits or []

    async def retrieve(
        self,
        query: str,
        categories: list[str] | None = None,
        top_k: int = 5,
    ) -> list[Any]:
        self.calls.append((query, categories, top_k))
        return list(self._hits)


@dataclass
class FakeKbHit:
    chunk_id: str
    doc_id: str
    doc_title: str
    category: str
    section_path: str
    page_no: int
    scope: str
    content: str
    score: float


class FakeTicketRepo:
    """In-memory ticket repository recording create_ticket calls."""

    def __init__(self) -> None:
        self.created: list[dict[str, Any]] = []

    async def create_ticket(
        self,
        user_id: int,
        session_id: uuid.UUID | str | None,
        category: str,
        title: str,
        summary: str | None = None,
        related_order_no: str | None = None,
    ) -> SimpleNamespace:
        record = SimpleNamespace(
            id=uuid.uuid4(),
            ticket_no=f"TK20261126{len(self.created) + 1:04d}",
            user_id=user_id,
            session_id=session_id,
            category=category,
            title=title,
            summary=summary,
            related_order_no=related_order_no,
            status="OPEN",
        )
        self.created.append(
            {
                "ticket_no": record.ticket_no,
                "user_id": user_id,
                "session_id": str(session_id) if session_id is not None else None,
                "category": category,
                "title": title,
                "summary": summary,
                "related_order_no": related_order_no,
                "status": "OPEN",
            }
        )
        return record


class FakeMallClient:
    """In-memory mall client implementing product, address, order, and direct-create endpoints."""

    def __init__(self) -> None:
        self.direct_create_calls: list[dict[str, Any]] = []
        self.detail_calls: list[tuple[int, str]] = []
        self.addresses: list[dict[str, Any]] = [
            {
                "id": 9,
                "realName": "张三",
                "phone": "13800000000",
                "province": "上海市",
                "city": "上海市",
                "district": "浦东新区",
                "detail": "张江路 100 号",
                "fullAddress": "上海市浦东新区张江路 100 号",
                "isDefault": 1,
            },
            {
                "id": 88,
                "realName": "李四",
                "phone": "13900000000",
                "province": "北京市",
                "city": "北京市",
                "district": "朝阳区",
                "detail": "望京街 88 号",
                "fullAddress": "北京市朝阳区望京街 88 号",
                "isDefault": 0,
            },
        ]

    async def recommend_products(
        self, sort: str = "hot", category_id: int | None = None, limit: int = 5
    ) -> list[dict[str, Any]]:
        return [
            {
                "id": 10,
                "productName": "KlackCraft K80 客制化机械键盘",
                "productInfo": "Gasket 结构 / 热插拔",
                "image": "/images/k80.png",
                "price": "399.00",
                "otPrice": "499.00",
                "stock": 25,
                "sales": 320,
                "browse": 1800,
                "isHot": 1,
                "isPostage": 1,
                "postage": "0.00",
            }
        ]

    async def search_products(
        self,
        keyword: str | None = None,
        category_id: int | None = None,
        min_price: Any = None,
        max_price: Any = None,
        limit: int = 6,
    ) -> list[dict[str, Any]]:
        return await self.recommend_products(limit=limit)

    async def get_product_detail(self, product_id: int) -> dict[str, Any]:
        return {
            "id": product_id,
            "productName": "KlackCraft K80 客制化机械键盘",
            "image": "/images/k80.png",
            "isShow": 1,
            "isDel": 0,
            "activeFlashSale": False,
            "isPostage": 1,
            "postage": "0.00",
            "stock": 25,
            "skus": [
                {
                    "skuId": 101,
                    "sku": "星空黑 / 静谧红轴",
                    "price": "399.00",
                    "stock": 12,
                    "image": "/images/k80-red.png",
                    "isDefault": 1,
                }
            ],
        }

    async def list_user_addresses(self, user_id: int) -> list[dict[str, Any]]:
        return list(self.addresses)

    async def list_user_orders(
        self, user_id: int, tab: str = "all", keyword: str | None = None, page: int = 1, limit: int = 5
    ) -> dict[str, Any]:
        return {
            "current": 1,
            "limit": limit,
            "totalPage": 1,
            "total": 1,
            "rows": [
                {
                    "orderNo": "ORD_PAID_1",
                    "paid": 1,
                    "status": 1,
                    "displayTab": "wait_deliver",
                    "displayStatus": "待发货",
                    "payPrice": "399.00",
                    "totalNum": 1,
                    "canApplyAfterSale": False,
                    "orderInfoList": [
                        {
                            "productId": 10,
                            "skuId": 101,
                            "productName": "KlackCraft K80 客制化机械键盘",
                            "sku": "星空黑 / 静谧红轴",
                            "price": "399.00",
                            "cartNum": 1,
                        }
                    ],
                }
            ],
        }

    async def get_order_detail(self, user_id: int, order_no: str) -> dict[str, Any]:
        self.detail_calls.append((user_id, order_no))
        return {
            "orderNo": order_no,
            "paid": 1,
            "status": 1,
            "statusDesc": "待发货",
            "displayTab": "wait_deliver",
            "displayStatus": "待发货",
            "payPrice": "399.00",
            "freightTotalFee": "0.00",
            "totalNum": 1,
            "canApplyAfterSale": False,
            "trackingSupported": False,
            "expressNo": None,
            "shipment": None,
            "orderInfoList": [
                {
                    "productId": 10,
                    "skuId": 101,
                    "productName": "KlackCraft K80 客制化机械键盘",
                    "sku": "星空黑 / 静谧红轴",
                    "price": "399.00",
                    "cartNum": 1,
                }
            ],
            "orderStatusList": [
                {"changeType": "create", "changeMessage": "订单已创建"},
                {"changeType": "pay", "changeMessage": "订单支付成功，等待商家发货"},
            ],
            "afterSale": None,
        }

    async def list_user_after_sales(
        self,
        user_id: int,
        order_no: str | None = None,
        service_no: str | None = None,
        limit: int = 10,
    ) -> list[dict[str, Any]]:
        return []

    async def direct_create_order(
        self,
        user_id: int,
        product_id: int,
        sku_id: int,
        quantity: int,
        address_id: int,
        idempotency_key: str,
    ) -> dict[str, Any]:
        self.direct_create_calls.append(
            {
                "user_id": user_id,
                "product_id": product_id,
                "sku_id": sku_id,
                "quantity": quantity,
                "address_id": address_id,
                "idempotency_key": idempotency_key,
            }
        )
        return {
            "orderNo": "ORD202611268888",
            "totalPrice": "399.00",
            "freightPrice": "0.00",
            "payPrice": "399.00",
            "remainingSeconds": 1800,
            "paymentUrl": "/order_payment?orderNo=ORD202611268888",
            "paid": 0,
            "status": 0,
        }


def _ctx(user_id: int = 777, session_id: str = "11111111-1111-4111-8111-111111111111"):
    from app.agent.context import AgentRuntimeContext

    return AgentRuntimeContext(user_id=user_id, session_id=session_id, trace_id="trace-1")


def _make_llm_factory(models: dict[str, StubChatModel]):
    def _factory(role: str) -> StubChatModel:
        if role in models:
            return models[role]
        return StubChatModel(messages=iter([]))

    return _factory


@pytest.mark.asyncio
async def test_intent_router_routes_all_5_branches_and_low_confidence_to_fallback():
    from app.agent.graph import build_customer_service_graph, build_thread_id
    from app.agent.state import HandoffSummary, RouterDecision

    mall = FakeMallClient()
    ticket_repo = FakeTicketRepo()

    router_stub = StubChatModel(
        messages=iter([]),
        structured_outputs=[
            RouterDecision(intent="chitchat", confidence=0.95, reason="greeting"),
            RouterDecision(intent="recommend_order", confidence=0.92, reason="recommend"),
            RouterDecision(intent="consult_after_sales", confidence=0.91, reason="order_query"),
            RouterDecision(intent="handoff", confidence=0.96, reason="explicit_handoff"),
            RouterDecision(intent="fallback", confidence=0.80, reason="nonsense"),
            RouterDecision(intent="recommend_order", confidence=0.55, reason="low_confidence"),
            RouterDecision(intent="chitchat", confidence=0.98, reason="back_to_chat"),
        ],
    )
    chitchat_stub = StubChatModel(
        messages=iter(
            [
                AIMessage(content="你好呀！我是咔嗒造物外设顾问，想看机械键盘、键帽还是桌搭好物？"),
                AIMessage(content="收到，有需要选购外设或查订单随时告诉我！"),
            ]
        )
    )
    shopping_stub = StubChatModel(
        messages=iter(
            [
                AIMessage(
                    content="",
                    tool_calls=[
                        {"name": "list_hot_and_popular_products", "args": {"sort_by": "hot"}, "id": "s1"}
                    ],
                ),
                AIMessage(content="为您推荐这款热销的 KlackCraft K80 客制化机械键盘。"),
            ]
        )
    )
    after_sales_stub = StubChatModel(
        messages=iter(
            [
                AIMessage(
                    content="",
                    tool_calls=[{"name": "list_my_orders", "args": {"tab": "all"}, "id": "a1"}],
                ),
                AIMessage(content="您最近的订单 ORD_PAID_1 当前处于待发货状态。"),
            ]
        )
    )
    handoff_stub = StubChatModel(
        messages=iter([]),
        structured_outputs=[
            HandoffSummary(
                category="order_issue",
                title="订单 ORD_PAID_1 催发货",
                summary="用户要求转人工催促订单 ORD_PAID_1 发货",
                related_order_no="ORD_PAID_1",
            )
        ],
    )
    fallback_stub = StubChatModel(
        messages=iter(
            [
                AIMessage(
                    content=(
                        "抱歉没太明白您的意思，我可以为您提供以下 4 项服务：\n"
                        "1. 热销与个性商品推荐下单\n"
                        "2. 订单与发货/物流进度查询\n"
                        "3. 售后政策与消费者权益法规解答\n"
                        "4. 人工工单服务"
                    )
                ),
                AIMessage(
                    content=(
                        "抱歉还不太确定您的需求，目前我支持这 4 项核心服务：\n"
                        "1. 商品推荐与聊天确认下单\n"
                        "2. 订单状态与物流查询\n"
                        "3. 售后规则与消保法规解答\n"
                        "4. 人工客服工单"
                    )
                ),
            ]
        )
    )

    models = {
        "router": router_stub,
        "chitchat": chitchat_stub,
        "shopping": shopping_stub,
        "after_sales": after_sales_stub,
        "handoff_extractor": handoff_stub,
        "fallback": fallback_stub,
    }

    graph = build_customer_service_graph(
        checkpointer=InMemorySaver(),
        mall_client=mall,
        kb_retriever=None,
        ticket_repo_factory=lambda: ticket_repo,
        llm_factory=lambda role: models[role],
    )
    ctx = _ctx()

    # 1. chitchat
    res_chat = await graph.ainvoke(
        {"messages": [HumanMessage(content="你好呀")]},
        context=ctx,
        config={"configurable": {"thread_id": build_thread_id(777, "s-chat")}},
    )
    assert res_chat["intent"] == "chitchat"
    assert "咔嗒造物" in res_chat["messages"][-1].content

    # 2. recommend_order (produces recommended_products & product_list card)
    rec_config = {"configurable": {"thread_id": build_thread_id(777, "s-rec")}}
    res_rec = await graph.ainvoke(
        {"messages": [HumanMessage(content="推荐几款热销机械键盘")]},
        context=ctx,
        config=rec_config,
    )
    assert res_rec["intent"] == "recommend_order"
    assert len(res_rec["recommended_products"]) == 1
    assert res_rec["ui_events"][0]["card_type"] == "product_list"

    # 3. consult_after_sales
    res_as = await graph.ainvoke(
        {"messages": [HumanMessage(content="查一下我的订单状态")]},
        context=ctx,
        config={"configurable": {"thread_id": build_thread_id(777, "s-as")}},
    )
    assert res_as["intent"] == "consult_after_sales"
    assert "ORD_PAID_1" in res_as["messages"][-1].content

    # 4. handoff with concrete issue
    res_ho = await graph.ainvoke(
        {"messages": [HumanMessage(content="订单 ORD_PAID_1 一直没发货，转人工催一下")]},
        context=ctx,
        config={"configurable": {"thread_id": build_thread_id(777, "s-ho")}},
    )
    assert res_ho["intent"] == "handoff"
    assert res_ho["created_ticket"] is not None
    assert res_ho["ui_events"][0]["card_type"] == "ticket_created"

    # 5. explicit fallback
    res_fb1 = await graph.ainvoke(
        {"messages": [HumanMessage(content="asdfghjkl???")]},
        context=ctx,
        config={"configurable": {"thread_id": build_thread_id(777, "s-fb1")}},
    )
    assert res_fb1["intent"] == "fallback"
    assert "人工工单" in res_fb1["messages"][-1].content

    # 6. low confidence (<0.6) forced to fallback even if model guessed recommend_order
    res_fb2 = await graph.ainvoke(
        {"messages": [HumanMessage(content="那个什么东东")]},
        context=ctx,
        config={"configurable": {"thread_id": build_thread_id(777, "s-fb2")}},
    )
    assert res_fb2["intent"] == "fallback"
    assert res_fb2["intent_confidence"] == pytest.approx(0.55)
    assert "4 项" in res_fb2["messages"][-1].content

    # 7. Next turn on same thread as recommend_order must reset stale cards & recommended_products
    res_rec_next = await graph.ainvoke(
        {"messages": [HumanMessage(content="谢谢")]},
        context=ctx,
        config=rec_config,
    )
    assert res_rec_next["intent"] == "chitchat"
    assert res_rec_next["recommended_products"] == []
    assert res_rec_next["ui_events"] == []


@pytest.mark.asyncio
async def test_recommend_order_interrupt_confirm_and_cancel():
    from app.agent.graph import build_customer_service_graph, build_thread_id
    from app.agent.state import RouterDecision

    def make_graph(mall: FakeMallClient):
        router_stub = StubChatModel(
            messages=iter([]),
            structured_outputs=[
                RouterDecision(intent="recommend_order", confidence=0.95, reason="buy"),
            ],
        )
        shopping_stub = StubChatModel(
            messages=iter(
                [
                    AIMessage(
                        content="",
                        tool_calls=[
                            {
                                "name": "prepare_order_draft",
                                "args": {"product_id": 10, "sku_id": 101, "quantity": 1},
                                "id": "draft_1",
                            }
                        ],
                    ),
                    AIMessage(content="已为您生成下单确认卡片，请核对后提交订单。"),
                ]
            )
        )
        models = {"router": router_stub, "shopping": shopping_stub}
        return build_customer_service_graph(
            checkpointer=InMemorySaver(),
            mall_client=mall,
            kb_retriever=None,
            ticket_repo_factory=lambda: FakeTicketRepo(),
            llm_factory=_make_llm_factory(models),
        )

    ctx = _ctx()

    # Case A: confirm with owned address_id=88
    mall_confirm = FakeMallClient()
    graph_confirm = make_graph(mall_confirm)
    cfg_confirm = {"configurable": {"thread_id": build_thread_id(777, "s-confirm")}}

    interrupted = await graph_confirm.ainvoke(
        {"messages": [HumanMessage(content="帮我买一件 K80 红轴")]},
        context=ctx,
        config=cfg_confirm,
    )
    assert "__interrupt__" in interrupted
    interrupt_payload = interrupted["__interrupt__"][0].value
    assert interrupt_payload["type"] == "order_confirm"
    draft_id = interrupt_payload["draft"]["draft_id"]
    assert mall_confirm.direct_create_calls == []

    resumed_confirm = await graph_confirm.ainvoke(
        Command(resume={"action": "confirm", "address_id": 88}),
        context=ctx,
        config=cfg_confirm,
    )
    assert "__interrupt__" not in resumed_confirm
    assert len(mall_confirm.direct_create_calls) == 1
    assert mall_confirm.direct_create_calls[0]["user_id"] == 777
    assert mall_confirm.direct_create_calls[0]["address_id"] == 88
    assert mall_confirm.direct_create_calls[0]["idempotency_key"] == draft_id
    assert resumed_confirm["pending_order_draft"] is None
    assert resumed_confirm["created_order"]["order_no"] == "ORD202611268888"
    assert resumed_confirm["ui_events"] == [
        {
            "card_type": "order_created",
            "payload": resumed_confirm["created_order"],
        }
    ]
    assert "/order_payment?orderNo=ORD202611268888" in resumed_confirm["created_order"]["payment_url"]

    # Case B: cancel must NOT call direct_create_order
    mall_cancel = FakeMallClient()
    graph_cancel = make_graph(mall_cancel)
    cfg_cancel = {"configurable": {"thread_id": build_thread_id(777, "s-cancel")}}

    await graph_cancel.ainvoke(
        {"messages": [HumanMessage(content="买这件")]},
        context=ctx,
        config=cfg_cancel,
    )
    resumed_cancel = await graph_cancel.ainvoke(
        Command(resume={"action": "cancel"}),
        context=ctx,
        config=cfg_cancel,
    )
    assert mall_cancel.direct_create_calls == []
    assert resumed_cancel["pending_order_draft"] is None
    assert resumed_cancel["created_order"] is None
    assert resumed_cancel["ui_events"] == []

    # Case C: non-owned address_id=9999 must be rejected and NOT call direct_create_order
    mall_bad_addr = FakeMallClient()
    graph_bad_addr = make_graph(mall_bad_addr)
    cfg_bad_addr = {"configurable": {"thread_id": build_thread_id(777, "s-bad-addr")}}

    await graph_bad_addr.ainvoke(
        {"messages": [HumanMessage(content="买这件")]},
        context=ctx,
        config=cfg_bad_addr,
    )
    resumed_bad = await graph_bad_addr.ainvoke(
        Command(resume={"action": "confirm", "address_id": 9999}),
        context=ctx,
        config=cfg_bad_addr,
    )
    assert mall_bad_addr.direct_create_calls == []
    assert resumed_bad["pending_order_draft"] is None
    assert resumed_bad["created_order"] is None
    assert "地址" in resumed_bad["messages"][-1].content

    # Case D: confirm without explicit address_id falls back to draft["address_id"] (default 9)
    mall_default_addr = FakeMallClient()
    graph_default_addr = make_graph(mall_default_addr)
    cfg_default_addr = {"configurable": {"thread_id": build_thread_id(777, "s-default-addr")}}

    await graph_default_addr.ainvoke(
        {"messages": [HumanMessage(content="买这件")]},
        context=ctx,
        config=cfg_default_addr,
    )
    resumed_default = await graph_default_addr.ainvoke(
        Command(resume={"action": "confirm"}),
        context=ctx,
        config=cfg_default_addr,
    )
    assert len(mall_default_addr.direct_create_calls) == 1
    assert mall_default_addr.direct_create_calls[0]["address_id"] == 9
    assert resumed_default["created_order"]["order_no"] == "ORD202611268888"


@pytest.mark.asyncio
async def test_new_message_during_interrupt_auto_cancels_draft():
    from app.agent.graph import build_customer_service_graph, build_thread_id
    from app.agent.state import RouterDecision

    mall = FakeMallClient()
    router_stub = StubChatModel(
        messages=iter([]),
        structured_outputs=[
            RouterDecision(intent="recommend_order", confidence=0.95, reason="buy"),
            RouterDecision(intent="consult_after_sales", confidence=0.94, reason="check_orders"),
        ],
    )
    shopping_stub = StubChatModel(
        messages=iter(
            [
                AIMessage(
                    content="",
                    tool_calls=[
                        {
                            "name": "prepare_order_draft",
                            "args": {"product_id": 10, "sku_id": 101, "quantity": 1},
                            "id": "draft_stuck",
                        }
                    ],
                ),
                AIMessage(content="已生成下单确认卡。"),
            ]
        )
    )
    after_sales_stub = StubChatModel(
        messages=iter(
            [
                AIMessage(
                    content="",
                    tool_calls=[{"name": "list_my_orders", "args": {"tab": "all"}, "id": "as_after"}],
                ),
                AIMessage(content="已为您查到订单 ORD_PAID_1，目前待发货。"),
            ]
        )
    )
    models = {
        "router": router_stub,
        "shopping": shopping_stub,
        "after_sales": after_sales_stub,
    }
    graph = build_customer_service_graph(
        checkpointer=InMemorySaver(),
        mall_client=mall,
        kb_retriever=None,
        ticket_repo_factory=lambda: FakeTicketRepo(),
        llm_factory=lambda role: models[role],
    )
    ctx = _ctx()
    cfg = {"configurable": {"thread_id": build_thread_id(777, "s-antistuck")}}

    # Turn 1 suspends at confirm_order_interrupt
    suspended = await graph.ainvoke(
        {"messages": [HumanMessage(content="帮我下单 K80 红轴")]},
        context=ctx,
        config=cfg,
    )
    assert "__interrupt__" in suspended
    snapshot = await graph.aget_state(cfg)
    assert snapshot.interrupts

    # User sends new text instead of clicking confirm/cancel -> caller auto-cancels pending interrupt first
    cancelled = await graph.ainvoke(
        Command(resume={"action": "cancel", "reason": "superseded_by_new_message"}),
        context=ctx,
        config=cfg,
    )
    assert mall.direct_create_calls == []
    assert cancelled["pending_order_draft"] is None
    snapshot_after_cancel = await graph.aget_state(cfg)
    assert not snapshot_after_cancel.interrupts

    # Then new user message routes cleanly to after-sales without touching direct_create_order
    follow_up = await graph.ainvoke(
        {"messages": [HumanMessage(content="算了，帮我查下我之前的订单")]},
        context=ctx,
        config=cfg,
    )
    assert mall.direct_create_calls == []
    assert follow_up["intent"] == "consult_after_sales"
    assert follow_up["pending_order_draft"] is None
    assert "ORD_PAID_1" in follow_up["messages"][-1].content


@pytest.mark.asyncio
async def test_handoff_clarifies_empty_first_turn_and_creates_ticket_with_context():
    from app.agent.graph import build_customer_service_graph, build_thread_id
    from app.agent.state import HandoffSummary, RouterDecision

    ticket_repo = FakeTicketRepo()
    router_stub = StubChatModel(
        messages=iter([]),
        structured_outputs=[
            RouterDecision(intent="handoff", confidence=0.98, reason="bare_handoff"),
            RouterDecision(intent="handoff", confidence=0.97, reason="followup_details"),
            RouterDecision(intent="handoff", confidence=0.99, reason="with_context"),
            RouterDecision(intent="handoff", confidence=0.95, reason="forced"),
        ],
    )
    handoff_stub = StubChatModel(
        messages=iter([]),
        structured_outputs=[
            HandoffSummary(
                category="order_issue",
                title="订单 ORD900 未发货",
                summary="用户反馈订单 ORD900 付款后一直未发货，申请人工介入处理",
                related_order_no="ORD900",
            ),
            HandoffSummary(
                category="after_sale",
                title="订单 ORD901 待发货退款申请",
                summary="订单 ORD901 已付款待发货，用户申请人工退款",
                related_order_no="ORD901",
            ),
            HandoffSummary(
                category="other",
                title="用户请求人工客服协助",
                summary="用户直接提交空背景人工客服工单",
                related_order_no=None,
            ),
        ],
    )
    models = {"router": router_stub, "handoff_extractor": handoff_stub}
    graph = build_customer_service_graph(
        checkpointer=InMemorySaver(),
        mall_client=FakeMallClient(),
        kb_retriever=None,
        ticket_repo_factory=lambda: ticket_repo,
        llm_factory=_make_llm_factory(models),
    )
    ctx = _ctx()

    # 1. Bare first turn "转人工" -> asks 1 clarifying sentence, NO ticket created
    cfg1 = {"configurable": {"thread_id": build_thread_id(777, "s-handoff-1")}}
    res1 = await graph.ainvoke(
        {"messages": [HumanMessage(content="转人工")]},
        context=ctx,
        config=cfg1,
    )
    assert res1["intent"] == "handoff"
    assert res1["created_ticket"] is None
    assert res1["ui_events"] == []
    assert len(ticket_repo.created) == 0
    assert "直接提交空背景工单" in res1["messages"][-1].content

    # 2. Turn 2 on same thread adds order/issue context -> creates OPEN ticket
    res2 = await graph.ainvoke(
        {"messages": [HumanMessage(content="订单 ORD900 一直没发货，帮我处理")]},
        context=ctx,
        config=cfg1,
    )
    assert res2["created_ticket"] is not None
    assert res2["created_ticket"]["status"] == "OPEN"
    assert res2["created_ticket"]["related_order_no"] == "ORD900"
    assert res2["ui_events"][0]["card_type"] == "ticket_created"
    assert len(ticket_repo.created) == 1
    assert ticket_repo.created[0]["user_id"] == 777

    # 3. Single turn with explicit issue & order -> immediately creates OPEN ticket
    cfg2 = {"configurable": {"thread_id": build_thread_id(777, "s-handoff-2")}}
    res3 = await graph.ainvoke(
        {"messages": [HumanMessage(content="帮我转人工，订单 ORD901 已经付款但还没发货我要退款")]},
        context=ctx,
        config=cfg2,
    )
    assert res3["created_ticket"] is not None
    assert res3["created_ticket"]["related_order_no"] == "ORD901"
    assert len(ticket_repo.created) == 2

    # 4. Bare first turn with explicit force_ticket=True -> bypasses clarification and creates ticket
    cfg3 = {"configurable": {"thread_id": build_thread_id(777, "s-handoff-3")}}
    res4 = await graph.ainvoke(
        {"messages": [HumanMessage(content="转人工")], "force_ticket": True},
        context=ctx,
        config=cfg3,
    )
    assert res4["created_ticket"] is not None
    assert res4["created_ticket"]["status"] == "OPEN"
    assert len(ticket_repo.created) == 3


@pytest.mark.asyncio
async def test_after_sales_status1_refund_guides_to_ticket_and_attaches_citations():
    from app.agent.graph import build_customer_service_graph, build_thread_id
    from app.agent.state import RouterDecision

    mall = FakeMallClient()
    retriever = FakeKbRetriever(
        hits=[
            FakeKbHit(
                chunk_id="law1:0",
                doc_id="law1",
                doc_title="中华人民共和国消费者权益保护法",
                category="consumer_law",
                section_path="第三章 > 第二十五条",
                page_no=3,
                scope="public",
                content="经营者采用网络方式销售商品，消费者有权自收到商品之日起七日内退货。",
                score=0.92,
            ),
            FakeKbHit(
                chunk_id="pol1:0",
                doc_id="pol1",
                doc_title="咔嗒造物商城售后服务规范",
                category="mall_policy",
                section_path="售后申请通道 > 待发货订单退款",
                page_no=1,
                scope="public",
                content="已付款待发货订单暂不支持前端自助售后通道，请通过客服转人工工单办理拦截退款。",
                score=0.95,
            ),
        ]
    )
    router_stub = StubChatModel(
        messages=iter([]),
        structured_outputs=[
            RouterDecision(intent="consult_after_sales", confidence=0.96, reason="refund_and_law")
        ],
    )
    after_sales_stub = StubChatModel(
        messages=iter(
            [
                AIMessage(
                    content="",
                    tool_calls=[
                        {"name": "get_my_order_detail", "args": {"order_no": "ORD_PAID_1"}, "id": "c1"},
                        {
                            "name": "search_policy_and_law_kb",
                            "args": {"query": "已付款未发货退款 七天无理由退货"},
                            "id": "c2",
                        },
                    ],
                ),
                AIMessage(
                    content=(
                        "您的订单 ORD_PAID_1 当前处于已付款待发货（status=1）状态，暂不支持前端自助售后退款通道，"
                        "建议您直接回复“转人工”创建人工工单由后台客服核实退款。"
                        "根据《中华人民共和国消费者权益保护法》第三章 > 第二十五条及《咔嗒造物商城售后服务规范》，"
                        "常规网购商品签收次日起支持七日无理由退货。"
                    )
                ),
            ]
        )
    )
    models = {"router": router_stub, "after_sales": after_sales_stub}
    graph = build_customer_service_graph(
        checkpointer=InMemorySaver(),
        mall_client=mall,
        kb_retriever=retriever,
        ticket_repo_factory=lambda: FakeTicketRepo(),
        llm_factory=_make_llm_factory(models),
    )

    result = await graph.ainvoke(
        {"messages": [HumanMessage(content="我的订单 ORD_PAID_1 已经付款还没发货能退款吗？消保法怎么规定七天无理由？")]},
        context=_ctx(user_id=777),
        config={"configurable": {"thread_id": build_thread_id(777, "s-law")}},
    )
    assert result["intent"] == "consult_after_sales"
    assert mall.detail_calls == [(777, "ORD_PAID_1")]
    assert len(result["rag_citations"]) == 2
    assert result["rag_citations"][0]["doc_title"] == "中华人民共和国消费者权益保护法"
    assert result["ui_events"] == [
        {"card_type": "citations", "payload": {"citations": result["rag_citations"]}}
    ]
    assert "人工" in result["messages"][-1].content
    assert "第二十五条" in result["messages"][-1].content


def test_default_llm_factory_and_checkpointer_helpers():
    from app.agent.graph import build_thread_id, to_psycopg_dsn
    from app.agent.llm import create_default_llm_factory
    from app.config import Settings

    settings = Settings(deepseek_api_key="sk-test-only", deepseek_model="deepseek-chat")
    factory = create_default_llm_factory(settings)

    router_llm = factory("router")
    handoff_llm = factory("handoff_extractor")
    chitchat_llm = factory("chitchat")
    shopping_llm = factory("shopping")
    after_sales_llm = factory("after_sales")
    fallback_llm = factory("fallback")

    assert router_llm.temperature == 0.0
    assert handoff_llm.temperature == 0.0
    assert 0.3 <= chitchat_llm.temperature <= 0.6 and chitchat_llm.streaming is True
    assert 0.3 <= shopping_llm.temperature <= 0.6 and shopping_llm.streaming is True
    assert 0.3 <= after_sales_llm.temperature <= 0.6 and after_sales_llm.streaming is True
    assert 0.3 <= fallback_llm.temperature <= 0.6 and fallback_llm.streaming is True

    assert build_thread_id(777, "abc-123") == "u:777:s:abc-123"
    assert (
        to_psycopg_dsn("postgresql+asyncpg://kada:pass@127.0.0.1:5432/kada_cs")
        == "postgresql://kada:pass@127.0.0.1:5432/kada_cs"
    )


@pytest.mark.asyncio
async def test_graph_astream_messages_and_updates_supports_interrupt_and_resume():
    from app.agent.graph import build_customer_service_graph, build_thread_id
    from app.agent.state import RouterDecision

    mall = FakeMallClient()
    models = {
        "router": StubChatModel(
            messages=iter([]),
            structured_outputs=[
                RouterDecision(intent="recommend_order", confidence=0.95, reason="buy")
            ],
        ),
        "shopping": StubChatModel(
            messages=iter(
                [
                    AIMessage(
                        content="",
                        tool_calls=[
                            {
                                "name": "prepare_order_draft",
                                "args": {"product_id": 10, "sku_id": 101, "quantity": 1},
                                "id": "stream_draft_1",
                            }
                        ],
                    ),
                    AIMessage(content="已为您生成下单确认卡片。"),
                ]
            )
        ),
    }
    graph = build_customer_service_graph(
        checkpointer=InMemorySaver(),
        mall_client=mall,
        kb_retriever=None,
        ticket_repo_factory=lambda: FakeTicketRepo(),
        llm_factory=_make_llm_factory(models),
    )
    cfg = {"configurable": {"thread_id": build_thread_id(777, "s-astream")}}
    ctx = _ctx()

    streamed_tokens: list[str] = []
    interrupt_updates: list[Any] = []
    async for _ns, mode, data in graph.astream(
        {"messages": [HumanMessage(content="帮我买 K80 红轴")]},
        context=ctx,
        config=cfg,
        stream_mode=["messages", "updates"],
        subgraphs=True,
    ):
        if mode == "messages":
            msg_chunk, _meta = data
            if isinstance(msg_chunk, AIMessage) and msg_chunk.content:
                streamed_tokens.append(str(msg_chunk.content))
        elif mode == "updates" and _ns == () and "__interrupt__" in data:
            interrupt_updates.extend(data["__interrupt__"])

    assert any("已为您生成下单确认卡片" in text for text in streamed_tokens)
    assert len(interrupt_updates) == 1
    assert interrupt_updates[0].value["type"] == "order_confirm"

    resume_updates: list[dict[str, Any]] = []
    async for ns, mode, data in graph.astream(
        Command(resume={"action": "confirm", "address_id": 88}),
        context=ctx,
        config=cfg,
        stream_mode=["messages", "updates"],
        subgraphs=True,
    ):
        if mode == "updates" and ns == ():
            resume_updates.append(data)

    assert len(mall.direct_create_calls) == 1
    assert any(
        "recommend_order_subgraph" in upd
        and upd["recommend_order_subgraph"]["ui_events"][0]["card_type"] == "order_created"
        for upd in resume_updates
    )
