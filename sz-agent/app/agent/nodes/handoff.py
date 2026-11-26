from __future__ import annotations

from collections.abc import AsyncIterator, Sequence
from contextlib import asynccontextmanager
import inspect
import re
from typing import Any

from langchain_core.messages import AIMessage, AnyMessage, HumanMessage, SystemMessage
from langgraph.runtime import Runtime

from app.agent.context import AgentRuntimeContext
from app.agent.llm import (
    LlmFactory,
    coerce_message_text,
    extract_conversation_messages,
    invoke_structured_output,
)
from app.agent.prompts import HANDOFF_EXTRACTOR_SYSTEM_PROMPT
from app.agent.state import (
    CustomerServiceState,
    HandoffSummary,
    VALID_TICKET_CATEGORIES,
)

_QUICK_SUBMIT_PHRASES = frozenset(
    {
        "直接提交空背景工单",
        "直接提交工单",
        "提交空背景工单",
    }
)

_BARE_HANDOFF_RE = re.compile(
    r"^(?:请|麻烦|帮我|我要|我想|想|可以|能否)?"
    r"(?:联系|找|转接|转到|转|接入)?"
    r"(?:人工(?:客服|坐席|服务)?|客服(?:人员)?)"
    r"(?:处理|服务|吧|啊|吗|呢|谢谢)?$"
)


def _normalize_utterance(text: str) -> str:
    return re.sub(r"[\s，。！？!?,.\-_:：；;\"'“”‘’()（）]+", "", text or "")


def _is_bare_first_turn_handoff(
    messages: Sequence[AnyMessage],
    *,
    force_ticket: bool,
) -> bool:
    if force_ticket:
        return False
    human_texts = [
        coerce_message_text(msg.content).strip()
        for msg in messages
        if isinstance(msg, HumanMessage) and coerce_message_text(msg.content).strip()
    ]
    if len(human_texts) != 1:
        return False
    normalized = _normalize_utterance(human_texts[0])
    if not normalized or normalized in _QUICK_SUBMIT_PHRASES:
        return False
    return bool(_BARE_HANDOFF_RE.match(normalized))


@asynccontextmanager
async def _open_ticket_repo(ticket_repo_factory: Any) -> AsyncIterator[Any]:
    resource = ticket_repo_factory()
    if inspect.isawaitable(resource):
        resource = await resource
    if hasattr(resource, "__aenter__") and hasattr(resource, "__aexit__"):
        async with resource as repo:
            yield repo
    elif hasattr(resource, "__enter__") and hasattr(resource, "__exit__"):
        with resource as repo:
            yield repo
    else:
        yield resource


def _serialize_ticket(ticket: Any) -> dict[str, Any]:
    if isinstance(ticket, dict):
        data = dict(ticket)
    else:
        data = {
            "id": str(getattr(ticket, "id")) if getattr(ticket, "id", None) is not None else None,
            "ticket_no": str(getattr(ticket, "ticket_no", "") or ""),
            "category": str(getattr(ticket, "category", "other") or "other"),
            "title": str(getattr(ticket, "title", "") or ""),
            "summary": getattr(ticket, "summary", None),
            "related_order_no": getattr(ticket, "related_order_no", None),
            "status": str(getattr(ticket, "status", "OPEN") or "OPEN"),
        }
    data.setdefault("action_url", "/cs/tickets")
    data.setdefault("tab", "tickets")
    return data


def build_handoff_node(*, llm_factory: LlmFactory, ticket_repo_factory: Any):
    """Build the human-handoff node with anti-empty-ticket clarification and PG ticket creation."""

    async def handoff_node(
        state: CustomerServiceState,
        runtime: Runtime[AgentRuntimeContext],
    ) -> dict[str, Any]:
        force_ticket = bool(state.get("force_ticket"))
        if _is_bare_first_turn_handoff(state["messages"], force_ticket=force_ticket):
            return {
                "created_ticket": None,
                "ui_events": [],
                "force_ticket": False,
                "messages": [
                    AIMessage(
                        content=(
                            "请问您具体遇到了什么问题（例如订单号、发货进度或退换货诉求）？"
                            "补充一句话后我立即为您创建人工工单；如暂不便说明，也可点击或回复「直接提交空背景工单」。"
                        )
                    )
                ],
            }

        history = extract_conversation_messages(state["messages"], max_turns=6)
        extractor_llm = llm_factory("handoff_extractor")
        extracted = await invoke_structured_output(
            extractor_llm,
            HandoffSummary,
            [SystemMessage(content=HANDOFF_EXTRACTOR_SYSTEM_PROMPT), *history],
        )

        category = extracted.category if extracted.category in VALID_TICKET_CATEGORIES else "other"
        title = (extracted.title or "人工客服工单").strip()[:128] or "人工客服工单"
        last_user_text = next(
            (coerce_message_text(m.content).strip() for m in reversed(history) if isinstance(m, HumanMessage)),
            "",
        )
        summary = (extracted.summary or last_user_text or title).strip()
        related_order_no = (
            extracted.related_order_no.strip()[:64]
            if isinstance(extracted.related_order_no, str) and extracted.related_order_no.strip()
            else None
        )

        if ticket_repo_factory is None:
            return {
                "created_ticket": None,
                "ui_events": [],
                "force_ticket": False,
                "messages": [AIMessage(content="当前人工工单服务暂不可用，请稍后再试。")],
            }

        async with _open_ticket_repo(ticket_repo_factory) as ticket_repo:
            ticket = await ticket_repo.create_ticket(
                user_id=runtime.context.user_id,
                session_id=runtime.context.session_id,
                category=category,
                title=title,
                summary=summary,
                related_order_no=related_order_no,
            )

        created_ticket = _serialize_ticket(ticket)
        ticket_no = created_ticket["ticket_no"]
        return {
            "created_ticket": created_ticket,
            "ui_events": [{"card_type": "ticket_created", "payload": created_ticket}],
            "force_ticket": False,
            "messages": [
                AIMessage(
                    content=(
                        f"已为您生成人工客服工单（工单号：{ticket_no}），"
                        "售后专员会尽快查看并回复，您可在「我的工单」中查看处理进度或继续补充说明。"
                    )
                )
            ],
        }

    return handoff_node
