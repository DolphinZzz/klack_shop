from __future__ import annotations

import asyncio
from collections.abc import AsyncIterator, Mapping
from contextlib import suppress
from dataclasses import dataclass
import json
import logging
import time
from typing import Any
import uuid

from fastapi import Request
from langchain_core.messages import AIMessage, AIMessageChunk, HumanMessage
from langgraph.graph.state import CompiledStateGraph
from langgraph.types import Command
from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker

from app.agent.context import AgentRuntimeContext
from app.agent.graph import build_thread_id
from app.agent.llm import coerce_message_text
from app.db.base import utcnow
from app.db.models import CsMessage
from app.repositories.session_repo import SessionRepository

logger = logging.getLogger(__name__)

SSE_HEARTBEAT = ": heartbeat\n\n"
_STREAMING_CONVERSATIONAL_NODES = frozenset({"chitchat_node", "fallback_node"})
_STATIC_REPLY_NODES = frozenset(
    {"handoff_node", "execute_create_order", "cancel_order_draft"}
)
_SUBGRAPH_ADVISOR_PREFIXES = ("shopping_advisor:", "after_sales_advisor:")
_SUBGRAPH_ROLLUP_KEYS = frozenset(
    {"recommend_order_subgraph", "consult_after_sales_subgraph"}
)


def format_sse_event(event_name: str, payload: Mapping[str, Any]) -> str:
    """Format a named SSE event frame with compact UTF-8 JSON data."""
    return f"event: {event_name}\ndata: {json.dumps(dict(payload), ensure_ascii=False)}\n\n"


def _has_tool_call_payload(msg: Any) -> bool:
    if getattr(msg, "tool_calls", None) or getattr(msg, "tool_call_chunks", None):
        return True
    additional = getattr(msg, "additional_kwargs", None)
    return isinstance(additional, Mapping) and bool(additional.get("tool_calls"))


def _extract_user_facing_delta(
    msg: Any,
    metadata: Mapping[str, Any],
    ns: tuple[str, ...],
) -> str | None:
    """Allow only user-facing assistant text deltas, filtering router/extractor/tool JSON."""
    if not isinstance(msg, AIMessage) or _has_tool_call_payload(msg):
        return None

    node = str(metadata.get("langgraph_node") or "")
    if node in _STREAMING_CONVERSATIONAL_NODES:
        pass
    elif node == "model" and any(
        part.startswith(_SUBGRAPH_ADVISOR_PREFIXES) for part in ns
    ):
        pass
    elif node in _STATIC_REPLY_NODES:
        if isinstance(msg, AIMessageChunk) or metadata.get("ls_integration") == "langchain_chat_model":
            return None
    else:
        return None

    text = coerce_message_text(msg.content)
    return text if text else None


def _extract_latest_ai_text(messages: Any) -> str | None:
    if not isinstance(messages, list):
        return None
    for msg in reversed(messages):
        if isinstance(msg, AIMessage) and not _has_tool_call_payload(msg):
            text = coerce_message_text(msg.content).strip()
            if text:
                return text
    return None


def _normalize_interrupt_payload(raw_interrupt: Any, session_id: str) -> dict[str, Any]:
    raw_val = getattr(raw_interrupt, "value", raw_interrupt)
    if isinstance(raw_val, Mapping) and "draft" in raw_val:
        interrupt_type = str(raw_val.get("type") or "order_confirm")
        payload = raw_val["draft"]
    elif isinstance(raw_val, Mapping):
        interrupt_type = str(raw_val.get("type") or "order_confirm")
        payload = dict(raw_val)
    else:
        interrupt_type = "order_confirm"
        payload = {"value": raw_val}
    return {
        "interrupt_type": interrupt_type,
        "session_id": session_id,
        "payload": payload,
    }


@dataclass(slots=True)
class PreparedTurn:
    """Validated session turn metadata ready for SSE graph streaming."""

    session_id: uuid.UUID
    user_id: int
    thread_id: str
    graph_input: Any
    initial_intent: str | None = None


class ChatService:
    """Orchestrates user-isolated LangGraph streaming, auto-cancel, persistence, and SSE."""

    def __init__(
        self,
        *,
        graph: CompiledStateGraph,
        session_factory: async_sessionmaker[AsyncSession],
        heartbeat_interval: float = 15.0,
        disconnect_check_interval: float = 0.2,
    ) -> None:
        self.graph = graph
        self.session_factory = session_factory
        self.heartbeat_interval = max(0.01, float(heartbeat_interval))
        self.disconnect_check_interval = max(0.01, float(disconnect_check_interval))

    async def prepare_chat_turn(
        self,
        *,
        user_id: int,
        session_id: str,
        message: str,
        force_ticket: bool = False,
        trace_id: str = "",
    ) -> PreparedTurn:
        """Verify ownership, auto-cancel any pending order interrupt, and persist the user message."""
        async with self.session_factory() as db:
            repo = SessionRepository(db)
            cs_session = await repo.get_session_for_user(session_id, user_id)
            if cs_session is None:
                raise LookupError("会话不存在或无权访问")

            sid_str = str(cs_session.id)
            thread_id = build_thread_id(user_id, sid_str)
            config = {"configurable": {"thread_id": thread_id}}
            runtime_ctx = AgentRuntimeContext(
                user_id=user_id,
                session_id=sid_str,
                trace_id=trace_id or uuid.uuid4().hex,
            )

            snapshot = await self.graph.aget_state(config)
            has_graph_interrupt = bool(snapshot is not None and getattr(snapshot, "interrupts", None))
            if cs_session.has_pending_interrupt or has_graph_interrupt:
                if has_graph_interrupt:
                    await self.graph.ainvoke(
                        Command(resume={"action": "cancel", "reason": "superseded_by_new_message"}),
                        context=runtime_ctx,
                        config=config,
                    )
                await repo.set_pending_interrupt(cs_session.id, user_id, pending=False)

            if cs_session.title == "新会话":
                clean_title = message.strip().splitlines()[0][:40]
                if clean_title:
                    cs_session.title = clean_title

            await repo.append_message(
                cs_session.id,
                user_id,
                "user",
                message,
            )
            await db.commit()

            return PreparedTurn(
                session_id=cs_session.id,
                user_id=user_id,
                thread_id=thread_id,
                graph_input={
                    "messages": [HumanMessage(content=message)],
                    "force_ticket": force_ticket,
                },
                initial_intent=None,
            )

    async def prepare_resume_turn(
        self,
        *,
        user_id: int,
        session_id: str,
        action: str,
        address_id: int | None = None,
        reason: str | None = None,
    ) -> PreparedTurn:
        """Verify ownership and active order interrupt before resuming LangGraph execution."""
        async with self.session_factory() as db:
            repo = SessionRepository(db)
            cs_session = await repo.get_session_for_user(session_id, user_id)
            if cs_session is None:
                raise LookupError("会话不存在或无权访问")

            sid_str = str(cs_session.id)
            thread_id = build_thread_id(user_id, sid_str)
            config = {"configurable": {"thread_id": thread_id}}
            snapshot = await self.graph.aget_state(config)
            has_graph_interrupt = bool(snapshot is not None and getattr(snapshot, "interrupts", None))

            if not cs_session.has_pending_interrupt and not has_graph_interrupt:
                raise ValueError("当前会话没有待确认的下单中断")
            if not has_graph_interrupt:
                await repo.set_pending_interrupt(cs_session.id, user_id, pending=False)
                await db.commit()
                raise ValueError("当前下单中断已过期或已处理，请重新挑选商品下单")

            resume_payload: dict[str, Any] = {"action": action}
            if address_id is not None:
                resume_payload["address_id"] = address_id
            if reason is not None and reason.strip():
                resume_payload["reason"] = reason.strip()

            return PreparedTurn(
                session_id=cs_session.id,
                user_id=user_id,
                thread_id=thread_id,
                graph_input=Command(resume=resume_payload),
                initial_intent=cs_session.last_intent or "recommend_order",
            )

    async def _persist_assistant_turn(
        self,
        *,
        session_id: uuid.UUID,
        user_id: int,
        message_id: uuid.UUID,
        content: str,
        intent: str | None,
        cards: list[dict[str, Any]],
        citations: list[dict[str, Any]],
        interrupt_payload: dict[str, Any] | None,
    ) -> None:
        async with self.session_factory() as db:
            repo = SessionRepository(db)
            cs_session = await repo.get_session_for_user(session_id, user_id)
            if cs_session is None:
                return

            now = utcnow()
            assistant_msg = CsMessage(
                id=message_id,
                session_id=cs_session.id,
                user_id=user_id,
                role="assistant",
                content=content,
                intent=intent,
                cards_json=cards or None,
                citations_json=citations or None,
                interrupt_payload=interrupt_payload,
                created_at=now,
            )
            db.add(assistant_msg)
            cs_session.has_pending_interrupt = interrupt_payload is not None
            if intent is not None:
                cs_session.last_intent = intent
            cs_session.last_message_at = now
            cs_session.updated_at = now
            await db.commit()

    async def stream_turn(
        self,
        prepared: PreparedTurn,
        *,
        request: Request,
        trace_id: str = "",
    ) -> AsyncIterator[str]:
        """Stream SSE events (`meta`, `token`, `card`, `interrupt`, `done`, `error`) for one turn."""
        sid_str = str(prepared.session_id)
        message_id = uuid.uuid4()
        message_id_str = str(message_id)
        runtime_ctx = AgentRuntimeContext(
            user_id=prepared.user_id,
            session_id=sid_str,
            trace_id=trace_id or uuid.uuid4().hex,
        )
        config = {"configurable": {"thread_id": prepared.thread_id}}

        queue: asyncio.Queue[tuple[str, Any]] = asyncio.Queue(maxsize=1)
        resolved_intent: str | None = prepared.initial_intent

        async def _produce() -> None:
            nonlocal resolved_intent
            saw_inner_subgraph_events = False
            collected_citations: list[dict[str, Any]] = []
            pending_interrupt: dict[str, Any] | None = None
            fallback_ai_text: str | None = None

            def _record_citations(items: Any) -> None:
                if not isinstance(items, list):
                    return
                for item in items:
                    if isinstance(item, Mapping):
                        collected_citations.append(dict(item))

            try:
                async for ns, mode, data in self.graph.astream(
                    prepared.graph_input,
                    context=runtime_ctx,
                    config=config,
                    stream_mode=["messages", "updates"],
                    subgraphs=True,
                ):
                    if mode == "messages":
                        msg, meta = data
                        delta = _extract_user_facing_delta(msg, meta, ns)
                        if delta:
                            await queue.put(("token", delta))
                        continue

                    if mode != "updates" or not isinstance(data, Mapping):
                        continue

                    if ns == ():
                        if "intent_router" in data and isinstance(data["intent_router"], Mapping):
                            intent_val = data["intent_router"].get("intent")
                            if isinstance(intent_val, str) and intent_val:
                                resolved_intent = intent_val
                                await queue.put(("meta", intent_val))

                        if "__interrupt__" in data and data["__interrupt__"]:
                            pending_interrupt = _normalize_interrupt_payload(
                                data["__interrupt__"][0],
                                sid_str,
                            )

                        for node_name, node_upd in data.items():
                            if node_name in ("intent_router", "__interrupt__") or not isinstance(
                                node_upd, Mapping
                            ):
                                continue
                            if node_name in _SUBGRAPH_ROLLUP_KEYS and saw_inner_subgraph_events:
                                continue
                            _record_citations(node_upd.get("rag_citations"))
                            maybe_text = _extract_latest_ai_text(node_upd.get("messages"))
                            if maybe_text:
                                fallback_ai_text = maybe_text
                            for ev in node_upd.get("ui_events") or []:
                                if isinstance(ev, Mapping):
                                    await queue.put(("card", dict(ev)))
                    elif len(ns) == 1:
                        for node_name, node_upd in data.items():
                            if node_name == "__interrupt__" or not isinstance(node_upd, Mapping):
                                continue
                            if "ui_events" in node_upd or "rag_citations" in node_upd:
                                saw_inner_subgraph_events = True
                            _record_citations(node_upd.get("rag_citations"))
                            maybe_text = _extract_latest_ai_text(node_upd.get("messages"))
                            if maybe_text:
                                fallback_ai_text = maybe_text
                            for ev in node_upd.get("ui_events") or []:
                                if isinstance(ev, Mapping):
                                    await queue.put(("card", dict(ev)))

                await queue.put(
                    (
                        "graph_done",
                        {
                            "citations": collected_citations,
                            "interrupt": pending_interrupt,
                            "fallback_text": fallback_ai_text,
                        },
                    )
                )
            except asyncio.CancelledError:
                raise
            except Exception as exc:  # noqa: BLE001 - emit clean SSE error event to client
                logger.exception("SSE graph stream failed for session %s", sid_str)
                await queue.put(("error", exc))

        producer_task = asyncio.create_task(_produce())
        meta_emitted = False
        token_parts: list[str] = []
        stored_cards: list[dict[str, Any]] = []
        last_emit_at = time.monotonic()

        def _emit_meta(intent_val: str) -> str:
            nonlocal meta_emitted, resolved_intent, last_emit_at
            resolved_intent = intent_val
            meta_emitted = True
            last_emit_at = time.monotonic()
            return format_sse_event(
                "meta",
                {
                    "session_id": sid_str,
                    "message_id": message_id_str,
                    "intent": intent_val,
                },
            )

        try:
            if resolved_intent is not None:
                yield _emit_meta(resolved_intent)

            while True:
                if await request.is_disconnected():
                    break

                until_heartbeat = max(
                    0.01, self.heartbeat_interval - (time.monotonic() - last_emit_at)
                )
                wait_timeout = min(self.disconnect_check_interval, until_heartbeat)
                try:
                    kind, payload = await asyncio.wait_for(queue.get(), timeout=wait_timeout)
                except asyncio.TimeoutError:
                    if await request.is_disconnected():
                        break
                    if time.monotonic() - last_emit_at >= self.heartbeat_interval:
                        yield SSE_HEARTBEAT
                        last_emit_at = time.monotonic()
                    continue

                if await request.is_disconnected():
                    break

                if kind == "meta":
                    if not meta_emitted:
                        yield _emit_meta(str(payload))
                    continue

                if kind == "token":
                    if not meta_emitted:
                        yield _emit_meta(resolved_intent or "chitchat")
                    token_parts.append(str(payload))
                    yield format_sse_event("token", {"delta": str(payload)})
                    last_emit_at = time.monotonic()
                    continue

                if kind == "card":
                    if not meta_emitted:
                        yield _emit_meta(resolved_intent or "chitchat")
                    card_type = str(payload.get("card_type") or "")
                    card_payload = payload.get("payload")
                    normalized_card = {
                        "card_type": card_type,
                        "payload": card_payload if isinstance(card_payload, Mapping) else {},
                    }
                    if card_type != "citations":
                        stored_cards.append(normalized_card)
                    yield format_sse_event("card", normalized_card)
                    last_emit_at = time.monotonic()
                    continue

                if kind == "error":
                    yield format_sse_event(
                        "error",
                        {
                            "code": "AGENT_STREAM_ERROR",
                            "message": "服务暂时繁忙，请稍后再试或转人工工单",
                        },
                    )
                    break

                if kind == "graph_done":
                    if not meta_emitted:
                        yield _emit_meta(resolved_intent or "fallback")

                    if not token_parts and payload.get("fallback_text"):
                        fallback_text = str(payload["fallback_text"])
                        token_parts.append(fallback_text)
                        yield format_sse_event("token", {"delta": fallback_text})

                    pending_interrupt = payload.get("interrupt")
                    citations = payload.get("citations") or []

                    await self._persist_assistant_turn(
                        session_id=prepared.session_id,
                        user_id=prepared.user_id,
                        message_id=message_id,
                        content="".join(token_parts),
                        intent=resolved_intent,
                        cards=stored_cards,
                        citations=citations,
                        interrupt_payload=pending_interrupt,
                    )

                    if pending_interrupt is not None:
                        yield format_sse_event("interrupt", pending_interrupt)
                        yield format_sse_event(
                            "done",
                            {"message_id": message_id_str, "finish_reason": "interrupted"},
                        )
                    else:
                        yield format_sse_event(
                            "done",
                            {"message_id": message_id_str, "finish_reason": "stop"},
                        )
                    break
        finally:
            if not producer_task.done():
                producer_task.cancel()
                with suppress(asyncio.CancelledError, Exception):
                    await producer_task
