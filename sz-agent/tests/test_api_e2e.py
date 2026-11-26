from __future__ import annotations

import asyncio
from collections.abc import AsyncIterator
import json
from pathlib import Path
from typing import Any

import httpx
from httpx import ASGITransport
import pytest
import pytest_asyncio
from langchain_core.language_models.fake_chat_models import GenericFakeChatModel
from langchain_core.messages import AIMessage, AIMessageChunk
from langchain_core.outputs import ChatGenerationChunk
from langgraph.checkpoint.memory import InMemorySaver
from sqlalchemy import event, select
from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker, create_async_engine
from starlette.requests import Request

from app.agent.state import HandoffSummary, RouterDecision
from app.config import Settings
from app.db.base import Base
import app.db.models  # noqa: F401 - register ORM models
from app.db.models import CsMessage, CsSession, KbDocument
from tests.test_graph_flows import (
    FakeKbHit,
    FakeKbRetriever,
    FakeMallClient,
    StubChatModel,
    _make_llm_factory,
)

INTERNAL_SECRET = "e2e-internal-secret-2026"


class SlowStreamingChatModel(GenericFakeChatModel):
    """Fake streaming chat model with controllable delay between chunks for heartbeat/disconnect tests."""

    first_chunk_sent: asyncio.Event | None = None
    release_gate: asyncio.Event | None = None
    chunk_delay: float = 0.0
    second_chunk_reached: bool = False

    def bind_tools(self, tools, **kwargs):
        return self

    async def _astream(self, messages, stop=None, run_manager=None, **kwargs):
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
            yield ChatGenerationChunk(
                message=AIMessageChunk(content="", id=msg.id, tool_call_chunks=tool_call_chunks)
            )
            return
        parts = str(msg.content).split("|")
        for idx, part in enumerate(parts):
            if idx > 0:
                if self.release_gate is not None:
                    await self.release_gate.wait()
                elif self.chunk_delay > 0:
                    await asyncio.sleep(self.chunk_delay)
                self.second_chunk_reached = True
            chunk = ChatGenerationChunk(message=AIMessageChunk(content=part, id=msg.id))
            if run_manager:
                await run_manager.on_llm_new_token(part, chunk=chunk)
            yield chunk
            if idx == 0 and self.first_chunk_sent is not None:
                self.first_chunk_sent.set()


class FakeMilvusStore:
    """In-memory Milvus vector store stub recording upserts and deletions."""

    def __init__(self) -> None:
        self.ensure_calls = 0
        self.upserted: list[tuple[list[Any], list[list[float]]]] = []
        self.deleted_doc_ids: list[str] = []

    async def ensure_collection(self) -> None:
        self.ensure_calls += 1

    async def delete_by_doc_id(self, doc_id: Any) -> None:
        self.deleted_doc_ids.append(str(doc_id))

    async def upsert_chunks(self, chunks: list[Any], vectors: list[list[float]]) -> int:
        self.upserted.append((list(chunks), list(vectors)))
        return len(chunks)


class FakeKbParser:
    """Deterministic PDF-to-Markdown parser stub."""

    def __init__(self, markdown: str = "# 售后服务规范\n\n## 第一条\n现货 48 小时内发货，七天无理由退货需保持包装完好。") -> None:
        self.markdown = markdown
        self.parsed_paths: list[Path] = []

    async def parse_pdf(self, pdf_path: Path | str, **_kwargs: Any) -> str:
        path = Path(pdf_path)
        self.parsed_paths.append(path)
        return self.markdown


class FakeEmbeddings:
    """Deterministic 1024-dim embedding stub."""

    def __init__(self) -> None:
        self.calls: list[list[str]] = []

    async def aembed_documents(self, texts: list[str]) -> list[list[float]]:
        self.calls.append(list(texts))
        return [[0.25] * 1024 for _ in texts]

    async def aembed_query(self, text: str) -> list[float]:
        return [0.25] * 1024


def _client_headers(user_id: int) -> dict[str, str]:
    return {
        "X-Internal-Secret": INTERNAL_SECRET,
        "X-User-Id": str(user_id),
    }


def _admin_headers(admin_id: int = 1, admin_name: str = "%E5%AE%A2%E6%9C%8D%E4%B8%BB%E7%AE%A1") -> dict[str, str]:
    return {
        "X-Internal-Secret": INTERNAL_SECRET,
        "X-Admin-Id": str(admin_id),
        "X-Admin-Name": admin_name,
    }


def _parse_sse_text(raw: str) -> list[dict[str, Any]]:
    """Parse raw SSE wire text into ordered event dicts and comment records."""
    events: list[dict[str, Any]] = []
    event_name: str | None = None
    data_lines: list[str] = []

    for line in raw.splitlines():
        if line == "":
            if event_name is not None or data_lines:
                raw_data = "\n".join(data_lines)
                try:
                    parsed_data = json.loads(raw_data)
                except ValueError:
                    parsed_data = raw_data
                events.append({"event": event_name or "message", "data": parsed_data})
                event_name = None
                data_lines = []
            continue
        if line.startswith(":"):
            events.append({"event": "comment", "comment": line[1:].strip()})
            continue
        if line.startswith("event:"):
            event_name = line[len("event:") :].strip()
        elif line.startswith("data:"):
            val = line[len("data:") :]
            if val.startswith(" "):
                val = val[1:]
            data_lines.append(val)

    if event_name is not None or data_lines:
        raw_data = "\n".join(data_lines)
        try:
            parsed_data = json.loads(raw_data)
        except ValueError:
            parsed_data = raw_data
        events.append({"event": event_name or "message", "data": parsed_data})
    return events


@pytest_asyncio.fixture
async def e2e_env(tmp_path: Path):
    """Build an isolated SQLite-backed FastAPI app environment with injected test doubles."""
    db_file = tmp_path / "cs_e2e.db"
    engine = create_async_engine(f"sqlite+aiosqlite:///{db_file}", echo=False)

    @event.listens_for(engine.sync_engine, "connect")
    def _enable_sqlite_fk(dbapi_connection, _connection_record) -> None:
        cursor = dbapi_connection.cursor()
        cursor.execute("PRAGMA foreign_keys=ON")
        cursor.close()

    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)

    session_factory = async_sessionmaker(
        engine,
        class_=AsyncSession,
        expire_on_commit=False,
        autoflush=False,
    )
    kb_dir = tmp_path / "kb_storage"
    kb_dir.mkdir(parents=True, exist_ok=True)
    settings = Settings(
        agent_internal_secret=INTERNAL_SECRET,
        kb_storage_dir=str(kb_dir),
        mineru_mode="cloud_api",
    )

    def _build_env(
        models: dict[str, Any] | None = None,
        *,
        mall_client: FakeMallClient | None = None,
        kb_retriever: FakeKbRetriever | None = None,
        kb_store: FakeMilvusStore | None = None,
        kb_parser: FakeKbParser | None = None,
        kb_embeddings: FakeEmbeddings | None = None,
        heartbeat_interval: float = 15.0,
        disconnect_check_interval: float = 0.05,
    ):
        from app.main import create_app

        resolved_mall = mall_client or FakeMallClient()
        resolved_store = kb_store or FakeMilvusStore()
        resolved_parser = kb_parser or FakeKbParser()
        resolved_embeddings = kb_embeddings or FakeEmbeddings()
        resolved_factory = _make_llm_factory(models or {})

        app = create_app(
            settings=settings,
            session_factory=session_factory,
            checkpointer=InMemorySaver(),
            mall_client=resolved_mall,
            kb_store=resolved_store,
            kb_embeddings=resolved_embeddings,
            kb_retriever=kb_retriever,
            kb_parser=resolved_parser,
            llm_factory=resolved_factory,
            sse_heartbeat_interval=heartbeat_interval,
            sse_disconnect_check_interval=disconnect_check_interval,
        )
        return {
            "app": app,
            "session_factory": session_factory,
            "mall_client": resolved_mall,
            "kb_store": resolved_store,
            "kb_parser": resolved_parser,
            "kb_embeddings": resolved_embeddings,
            "kb_dir": kb_dir,
            "settings": settings,
        }

    try:
        yield _build_env
    finally:
        await engine.dispose()


@pytest.mark.asyncio
async def test_cross_user_access_forbidden(e2e_env):
    env = e2e_env(
        {
            "router": StubChatModel(
                messages=iter([]),
                structured_outputs=[
                    RouterDecision(intent="handoff", confidence=0.96, reason="handoff"),
                ],
            ),
            "handoff_extractor": StubChatModel(
                messages=iter([]),
                structured_outputs=[
                    HandoffSummary(
                        category="order_issue",
                        title="订单 ORD100 催发货",
                        summary="用户反馈订单未发货",
                        related_order_no="ORD100",
                    )
                ],
            ),
        }
    )

    async with httpx.AsyncClient(
        transport=ASGITransport(app=env["app"]),
        base_url="http://testserver",
    ) as client:
        create_res = await client.post(
            "/internal/cs/client/sessions",
            headers=_client_headers(101),
            json={"title": "用户A会话"},
        )
        assert create_res.status_code == 200
        sid = create_res.json()["id"]

        # Create a ticket owned by user 101 via handoff stream
        stream_res = await client.post(
            f"/internal/cs/client/sessions/{sid}/chat/stream",
            headers=_client_headers(101),
            json={"message": "订单 ORD100 一直没发货，帮我转人工"},
        )
        assert stream_res.status_code == 200
        events = _parse_sse_text(stream_res.text)
        ticket_cards = [
            e["data"] for e in events if e["event"] == "card" and e["data"]["card_type"] == "ticket_created"
        ]
        assert len(ticket_cards) == 1
        tid = ticket_cards[0]["payload"]["id"]

        # User B (202) attempts cross-user access on User A's session & ticket -> 403 or 404
        res_msgs = await client.get(
            f"/internal/cs/client/sessions/{sid}/messages",
            headers=_client_headers(202),
        )
        assert res_msgs.status_code in (403, 404)

        res_chat = await client.post(
            f"/internal/cs/client/sessions/{sid}/chat/stream",
            headers=_client_headers(202),
            json={"message": "越权发消息"},
        )
        assert res_chat.status_code in (403, 404)

        res_resume = await client.post(
            f"/internal/cs/client/sessions/{sid}/resume/stream",
            headers=_client_headers(202),
            json={"action": "confirm"},
        )
        assert res_resume.status_code in (403, 404)

        res_ticket = await client.get(
            f"/internal/cs/client/tickets/{tid}",
            headers=_client_headers(202),
        )
        assert res_ticket.status_code in (403, 404)

        res_followup = await client.post(
            f"/internal/cs/client/tickets/{tid}/replies",
            headers=_client_headers(202),
            json={"content": "越权追问"},
        )
        assert res_followup.status_code in (403, 404)

        # User B's lists remain isolated
        list_s_b = await client.get("/internal/cs/client/sessions", headers=_client_headers(202))
        assert list_s_b.status_code == 200 and list_s_b.json()["items"] == []
        list_t_b = await client.get("/internal/cs/client/tickets", headers=_client_headers(202))
        assert list_t_b.status_code == 200 and list_t_b.json()["items"] == []


@pytest.mark.asyncio
async def test_chat_stream_emits_meta_token_card_interrupt_done_and_persists_db(e2e_env):
    router_stub = StubChatModel(
        messages=iter([]),
        structured_outputs=[
            RouterDecision(intent="recommend_order", confidence=0.95, reason="buy_with_rec"),
        ],
    )
    shopping_stub = StubChatModel(
        messages=iter(
            [
                AIMessage(
                    content="",
                    tool_calls=[
                        {"name": "list_hot_and_popular_products", "args": {"sort_by": "hot"}, "id": "rec_1"},
                        {
                            "name": "prepare_order_draft",
                            "args": {"product_id": 10, "sku_id": 101, "quantity": 1},
                            "id": "draft_1",
                        },
                    ],
                ),
                AIMessage(content="为您推荐 K80 键盘，并已准备好下单确认卡片。"),
            ]
        )
    )
    mall = FakeMallClient()
    env = e2e_env({"router": router_stub, "shopping": shopping_stub}, mall_client=mall)

    async with httpx.AsyncClient(
        transport=ASGITransport(app=env["app"]),
        base_url="http://testserver",
    ) as client:
        s_resp = await client.post("/internal/cs/client/sessions", headers=_client_headers(777))
        assert s_resp.status_code == 200
        sid = s_resp.json()["id"]

        stream_resp = await client.post(
            f"/internal/cs/client/sessions/{sid}/chat/stream",
            headers=_client_headers(777),
            json={"message": "推荐热销键盘并帮我买 K80 红轴"},
        )
        assert stream_resp.status_code == 200
        assert stream_resp.headers["content-type"].startswith("text/event-stream")
        assert stream_resp.headers.get("cache-control") == "no-cache"
        assert stream_resp.headers.get("x-accel-buffering") == "no"

        events = _parse_sse_text(stream_resp.text)
        event_names = [e["event"] for e in events]
        assert event_names[0] == "meta"
        assert events[0]["data"]["session_id"] == sid
        assert events[0]["data"]["intent"] == "recommend_order"
        msg_id = events[0]["data"]["message_id"]

        token_deltas = [e["data"]["delta"] for e in events if e["event"] == "token"]
        assert token_deltas
        full_tokens = "".join(token_deltas)
        assert "K80" in full_tokens and "下单确认卡片" in full_tokens
        # Structured router / tool-call JSON fragments must not leak into token events
        assert "recommend_order" not in full_tokens
        assert "prepare_order_draft" not in full_tokens

        cards = [e["data"] for e in events if e["event"] == "card"]
        assert any(c["card_type"] == "product_list" and len(c["payload"]["products"]) == 1 for c in cards)

        interrupts = [e["data"] for e in events if e["event"] == "interrupt"]
        assert len(interrupts) == 1
        assert interrupts[0]["interrupt_type"] == "order_confirm"
        assert interrupts[0]["session_id"] == sid
        assert interrupts[0]["payload"]["product_id"] == 10
        assert interrupts[0]["payload"]["total_pay_price"] == "399.00"
        draft_id = interrupts[0]["payload"]["draft_id"]

        assert events[-1]["event"] == "done"
        assert events[-1]["data"] == {"message_id": msg_id, "finish_reason": "interrupted"}
        assert mall.direct_create_calls == []

        # Verify DB persistence & history replay
        hist_resp = await client.get(
            f"/internal/cs/client/sessions/{sid}/messages",
            headers=_client_headers(777),
        )
        assert hist_resp.status_code == 200
        hist = hist_resp.json()
        assert hist["session"]["has_pending_interrupt"] is True
        assert hist["session"]["last_intent"] == "recommend_order"
        assert hist["pending_interrupt"]["interrupt_type"] == "order_confirm"
        assert len(hist["messages"]) == 2
        assert hist["messages"][0]["role"] == "user"
        assert hist["messages"][1]["role"] == "assistant"
        assert hist["messages"][1]["id"] == msg_id
        assert hist["messages"][1]["cards"][0]["card_type"] == "product_list"
        assert hist["messages"][1]["interrupt_payload"]["payload"]["draft_id"] == draft_id

        # Resume with confirm + addressId (camelCase accepted) -> creates formal order
        resume_resp = await client.post(
            f"/internal/cs/client/sessions/{sid}/resume/stream",
            headers=_client_headers(777),
            json={"action": "confirm", "addressId": 88},
        )
        assert resume_resp.status_code == 200
        resume_events = _parse_sse_text(resume_resp.text)
        assert resume_events[0]["event"] == "meta"
        assert any(
            e["event"] == "card"
            and e["data"]["card_type"] == "order_created"
            and e["data"]["payload"]["order_no"] == "ORD202611268888"
            and "/order_payment?orderNo=ORD202611268888" in e["data"]["payload"]["payment_url"]
            for e in resume_events
        )
        assert resume_events[-1]["event"] == "done"
        assert resume_events[-1]["data"]["finish_reason"] == "stop"

        assert len(mall.direct_create_calls) == 1
        assert mall.direct_create_calls[0]["address_id"] == 88
        assert mall.direct_create_calls[0]["idempotency_key"] == draft_id

        hist_after = (
            await client.get(
                f"/internal/cs/client/sessions/{sid}/messages",
                headers=_client_headers(777),
            )
        ).json()
        assert hist_after["session"]["has_pending_interrupt"] is False
        assert hist_after["pending_interrupt"] is None


@pytest.mark.asyncio
async def test_chat_stream_auto_cancels_pending_interrupt(e2e_env):
    router_stub = StubChatModel(
        messages=iter([]),
        structured_outputs=[
            RouterDecision(intent="recommend_order", confidence=0.95, reason="buy"),
            RouterDecision(intent="consult_after_sales", confidence=0.93, reason="check_order"),
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
                AIMessage(content="请确认订单信息。"),
            ]
        )
    )
    after_sales_stub = StubChatModel(
        messages=iter(
            [
                AIMessage(
                    content="",
                    tool_calls=[
                        {
                            "name": "search_policy_and_law_kb",
                            "args": {"query": "七天无理由"},
                            "id": "kb_1",
                        }
                    ],
                ),
                AIMessage(content="根据《消保法》第二十五条，支持七日无理由退货。"),
            ]
        )
    )
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
            )
        ]
    )
    env = e2e_env(
        {
            "router": router_stub,
            "shopping": shopping_stub,
            "after_sales": after_sales_stub,
        },
        mall_client=mall,
        kb_retriever=retriever,
    )

    async with httpx.AsyncClient(
        transport=ASGITransport(app=env["app"]),
        base_url="http://testserver",
    ) as client:
        sid = (await client.post("/internal/cs/client/sessions", headers=_client_headers(777))).json()["id"]

        # Turn 1 creates pending order interrupt
        r1 = await client.post(
            f"/internal/cs/client/sessions/{sid}/chat/stream",
            headers=_client_headers(777),
            json={"message": "帮我买 K80 红轴"},
        )
        assert r1.status_code == 200
        e1 = _parse_sse_text(r1.text)
        assert e1[-1]["data"]["finish_reason"] == "interrupted"

        # Turn 2 sends free text (must auto-cancel old interrupt and NEVER confirm order)
        r2 = await client.post(
            f"/internal/cs/client/sessions/{sid}/chat/stream",
            headers=_client_headers(777),
            json={"message": "算了，先不买，查一下七天无理由退货政策"},
        )
        assert r2.status_code == 200
        e2 = _parse_sse_text(r2.text)

        assert mall.direct_create_calls == []
        assert e2[0]["event"] == "meta"
        assert e2[0]["data"]["intent"] == "consult_after_sales"
        assert any(
            ev["event"] == "card" and ev["data"]["card_type"] == "citations"
            for ev in e2
        )
        assert e2[-1]["event"] == "done"
        assert e2[-1]["data"]["finish_reason"] == "stop"

        hist = (
            await client.get(f"/internal/cs/client/sessions/{sid}/messages", headers=_client_headers(777))
        ).json()
        assert hist["session"]["has_pending_interrupt"] is False
        assert hist["pending_interrupt"] is None
        assert hist["messages"][-1]["citations"][0]["doc_title"] == "中华人民共和国消费者权益保护法"


@pytest.mark.asyncio
async def test_admin_reply_updates_session_and_user_followup_reopens_ticket(e2e_env):
    env = e2e_env(
        {
            "router": StubChatModel(
                messages=iter([]),
                structured_outputs=[
                    RouterDecision(intent="handoff", confidence=0.97, reason="handoff"),
                ],
            ),
            "handoff_extractor": StubChatModel(
                messages=iter([]),
                structured_outputs=[
                    HandoffSummary(
                        category="after_sale",
                        title="订单 ORD_PAID_1 待发货退款",
                        summary="订单已付款未发货，用户申请人工退款",
                        related_order_no="ORD_PAID_1",
                    )
                ],
            ),
        }
    )

    async with httpx.AsyncClient(
        transport=ASGITransport(app=env["app"]),
        base_url="http://testserver",
    ) as client:
        sid = (
            await client.post(
                "/internal/cs/client/sessions",
                headers=_client_headers(501),
                json={"title": "售后申请"},
            )
        ).json()["id"]

        r_stream = await client.post(
            f"/internal/cs/client/sessions/{sid}/chat/stream",
            headers=_client_headers(501),
            json={"message": "订单 ORD_PAID_1 已付款但一直没发货，转人工申请退款"},
        )
        assert r_stream.status_code == 200
        events = _parse_sse_text(r_stream.text)
        ticket_card = next(
            e["data"]["payload"]
            for e in events
            if e["event"] == "card" and e["data"]["card_type"] == "ticket_created"
        )
        tid = ticket_card["id"]
        ticket_no = ticket_card["ticket_no"]

        # Admin lists & inspects ticket detail with linked session replay
        admin_list = await client.get(
            "/internal/cs/admin/tickets",
            headers=_admin_headers(),
            params={"status": "OPEN", "keyword": "ORD_PAID_1"},
        )
        assert admin_list.status_code == 200
        assert admin_list.json()["total"] == 1
        assert admin_list.json()["items"][0]["id"] == tid

        admin_detail = await client.get(
            f"/internal/cs/admin/tickets/{tid}",
            headers=_admin_headers(),
        )
        assert admin_detail.status_code == 200
        detail_json = admin_detail.json()
        assert detail_json["status"] == "OPEN"
        assert len(detail_json["session_messages"]) >= 2
        assert detail_json["session_messages"][0]["role"] == "user"

        # Admin replies without closing
        reply_res = await client.post(
            f"/internal/cs/admin/tickets/{tid}/reply",
            headers=_admin_headers(admin_id=9, admin_name="%E5%AE%A2%E6%9C%8D%E5%B0%8F%E5%8D%A1"),
            json={"content": "已为您登记拦截退款，预计 1 个工作日内原路退回", "closeTicket": False},
        )
        assert reply_res.status_code == 200
        assert reply_res.json()["status"] == "REPLIED"
        assert reply_res.json()["replies"][0]["sender_name"] == "客服小卡"

        # User queries original session messages and sees system sync summary
        user_msgs = await client.get(
            f"/internal/cs/client/sessions/{sid}/messages",
            headers=_client_headers(501),
        )
        assert user_msgs.status_code == 200
        msgs_list = user_msgs.json()["messages"]
        assert any(
            m["role"] == "system"
            and ticket_no in m["content"]
            and "已为您登记拦截退款" in m["content"]
            for m in msgs_list
        )

        # User follows up -> ticket status reopens to OPEN
        followup_res = await client.post(
            f"/internal/cs/client/tickets/{tid}/replies",
            headers=_client_headers(501),
            json={"content": "好的，退款到账会有短信通知吗？"},
        )
        assert followup_res.status_code == 200
        assert followup_res.json()["status"] == "OPEN"
        assert [r["sender_type"] for r in followup_res.json()["replies"]] == ["ADMIN", "USER"]

        # Admin closes ticket -> user followup afterwards is rejected
        close_res = await client.post(
            f"/internal/cs/admin/tickets/{tid}/close",
            headers=_admin_headers(),
        )
        assert close_res.status_code == 200
        assert close_res.json()["status"] == "CLOSED"

        closed_followup = await client.post(
            f"/internal/cs/client/tickets/{tid}/replies",
            headers=_client_headers(501),
            json={"content": "再追问一句"},
        )
        assert closed_followup.status_code == 400


@pytest.mark.asyncio
async def test_kb_upload_and_delete_lifecycle(e2e_env):
    store = FakeMilvusStore()
    parser = FakeKbParser()
    embeddings = FakeEmbeddings()
    env = e2e_env(kb_store=store, kb_parser=parser, kb_embeddings=embeddings)
    pdf_bytes = b"%PDF-1.4\n1 0 obj <<>> endobj\ntrailer <<>>\n%%EOF"

    async with httpx.AsyncClient(
        transport=ASGITransport(app=env["app"]),
        base_url="http://testserver",
    ) as client:
        upload_res = await client.post(
            "/internal/cs/admin/kb/documents/upload",
            headers=_admin_headers(),
            files={"file": ("mall_policy.pdf", pdf_bytes, "application/pdf")},
            data={"category": "mall_policy", "title": "咔嗒造物售后服务规范"},
        )
        assert upload_res.status_code == 200
        uploaded = upload_res.json()
        doc_id = uploaded["id"]
        assert uploaded["title"] == "咔嗒造物售后服务规范"
        assert uploaded["category"] == "mall_policy"
        assert Path(uploaded["file_path"]).exists()
        assert Path(uploaded["file_path"]).is_relative_to(env["kb_dir"].resolve())

        # Background task finishes ingestion -> INDEXED + chunks in Milvus
        list_res = await client.get(
            "/internal/cs/admin/kb/documents",
            headers=_admin_headers(),
            params={"category": "mall_policy"},
        )
        assert list_res.status_code == 200
        listed = list_res.json()
        assert listed["total"] == 1
        assert listed["items"][0]["id"] == doc_id
        assert listed["items"][0]["status"] == "INDEXED"
        assert listed["items"][0]["chunk_count"] >= 1
        assert len(parser.parsed_paths) == 1
        assert len(store.upserted) == 1

        # Duplicate upload with identical SHA-256 must be deduplicated cleanly
        dup_res = await client.post(
            "/internal/cs/admin/kb/documents/upload",
            headers=_admin_headers(),
            files={"file": ("duplicate_name.pdf", pdf_bytes, "application/pdf")},
            data={"category": "mall_policy", "title": "重复上传"},
        )
        assert dup_res.status_code == 409
        assert len(parser.parsed_paths) == 1

        # Reindex triggers background re-ingestion
        reindex_res = await client.post(
            f"/internal/cs/admin/kb/documents/{doc_id}/reindex",
            headers=_admin_headers(),
        )
        assert reindex_res.status_code == 200
        assert len(parser.parsed_paths) == 2

        # Delete removes Milvus vectors, PG row, cached markdown, and local stored PDF
        stored_pdf = Path(uploaded["file_path"])
        del_res = await client.delete(
            f"/internal/cs/admin/kb/documents/{doc_id}",
            headers=_admin_headers(),
        )
        assert del_res.status_code == 200
        assert del_res.json()["deleted"] is True
        assert doc_id in store.deleted_doc_ids
        assert not stored_pdf.exists()

        async with env["session_factory"]() as verify_session:
            row = (
                await verify_session.execute(select(KbDocument))
            ).scalar_one_or_none()
            assert row is None

        del_again = await client.delete(
            f"/internal/cs/admin/kb/documents/{doc_id}",
            headers=_admin_headers(),
        )
        assert del_again.status_code == 404


@pytest.mark.asyncio
async def test_sse_heartbeat_emits_when_idle(e2e_env):
    slow_heartbeat_model = SlowStreamingChatModel(
        messages=iter([AIMessage(content="第一句 |第二句")]),
        chunk_delay=0.14,
    )
    env_hb = e2e_env(
        {
            "router": StubChatModel(
                messages=iter([]),
                structured_outputs=[RouterDecision(intent="chitchat", confidence=0.95, reason="hi")],
            ),
            "chitchat": slow_heartbeat_model,
        },
        heartbeat_interval=0.04,
        disconnect_check_interval=0.02,
    )
    async with httpx.AsyncClient(
        transport=ASGITransport(app=env_hb["app"]),
        base_url="http://testserver",
    ) as client:
        sid_hb = (await client.post("/internal/cs/client/sessions", headers=_client_headers(601))).json()["id"]
        hb_resp = await client.post(
            f"/internal/cs/client/sessions/{sid_hb}/chat/stream",
            headers=_client_headers(601),
            json={"message": "你好"},
        )
        assert hb_resp.status_code == 200
        hb_events = _parse_sse_text(hb_resp.text)
        assert any(e["event"] == "comment" and e["comment"] == "heartbeat" for e in hb_events)
        assert hb_events[-1]["event"] == "done" and hb_events[-1]["data"]["finish_reason"] == "stop"


@pytest.mark.asyncio
async def test_sse_disconnect_stops_generation(e2e_env, monkeypatch):
    first_chunk_sent = asyncio.Event()
    release_gate = asyncio.Event()
    slow_disconnect_model = SlowStreamingChatModel(
        messages=iter(
            [
                AIMessage(
                    content="",
                    tool_calls=[
                        {
                            "name": "prepare_order_draft",
                            "args": {"product_id": 10, "sku_id": 101, "quantity": 1},
                            "id": "disc_draft_1",
                        }
                    ],
                ),
                AIMessage(content="半截内容 |后面绝不应到达"),
            ]
        ),
        first_chunk_sent=first_chunk_sent,
        release_gate=release_gate,
    )
    env_disc = e2e_env(
        {
            "router": StubChatModel(
                messages=iter([]),
                structured_outputs=[
                    RouterDecision(intent="recommend_order", confidence=0.96, reason="buy")
                ],
            ),
            "shopping": slow_disconnect_model,
        },
        heartbeat_interval=5.0,
        disconnect_check_interval=0.02,
    )

    async def fake_is_disconnected(self: Request) -> bool:
        return first_chunk_sent.is_set()

    monkeypatch.setattr(Request, "is_disconnected", fake_is_disconnected)

    async with httpx.AsyncClient(
        transport=ASGITransport(app=env_disc["app"]),
        base_url="http://testserver",
    ) as client:
        sid = (await client.post("/internal/cs/client/sessions", headers=_client_headers(602))).json()["id"]
        disc_resp = await asyncio.wait_for(
            client.post(
                f"/internal/cs/client/sessions/{sid}/chat/stream",
                headers=_client_headers(602),
                json={"message": "帮我买键盘"},
            ),
            timeout=2.0,
        )
        assert disc_resp.status_code == 200
        disc_events = _parse_sse_text(disc_resp.text)
        assert slow_disconnect_model.second_chunk_reached is False
        assert not any(e["event"] in ("interrupt", "done") for e in disc_events)

    async with env_disc["session_factory"]() as check_session:
        cs_sess = (
            await check_session.execute(select(CsSession).where(CsSession.user_id == 602))
        ).scalar_one()
        assert cs_sess.has_pending_interrupt is False
        msgs = list(
            (
                await check_session.execute(
                    select(CsMessage).where(CsMessage.session_id == cs_sess.id)
                )
            )
            .scalars()
            .all()
        )
        assert [m.role for m in msgs] == ["user"]


@pytest.mark.asyncio
async def test_lifespan_initializes_dependencies_cleanly(monkeypatch, tmp_path):
    from contextlib import asynccontextmanager
    from app import main as main_module

    events: list[str] = []
    db_file = tmp_path / "lifespan.db"
    engine = create_async_engine(f"sqlite+aiosqlite:///{db_file}", echo=False)
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
    session_factory = async_sessionmaker(engine, class_=AsyncSession, expire_on_commit=False)

    @asynccontextmanager
    async def fake_open_checkpointer(dsn: str):
        events.append(f"checkpointer_open:{dsn}")
        yield InMemorySaver()
        events.append("checkpointer_close")

    class FakeLifecycleMilvus:
        @classmethod
        def from_settings(cls, settings):
            return cls()

        async def ensure_collection(self):
            events.append("milvus_ensure")

    class FakeLifecycleMall:
        @classmethod
        def from_settings(cls, settings):
            events.append("mall_init")
            return cls()

        async def aclose(self):
            events.append("mall_close")

    async def fake_dispose_engine():
        events.append("dispose_engine")

    monkeypatch.setattr(main_module, "open_postgres_checkpointer", fake_open_checkpointer)
    monkeypatch.setattr(main_module, "MilvusKbStore", FakeLifecycleMilvus)
    monkeypatch.setattr(main_module, "MallInternalClient", FakeLifecycleMall)
    monkeypatch.setattr(main_module, "get_session_factory", lambda: session_factory)
    monkeypatch.setattr(main_module, "dispose_engine", fake_dispose_engine)

    cfg = Settings(
        agent_internal_secret=INTERNAL_SECRET,
        postgres_dsn="postgresql+asyncpg://kada:pass@127.0.0.1:5432/kada_cs",
        deepseek_api_key="sk-test",
        dashscope_api_key="",
        kb_storage_dir=str(tmp_path / "kb"),
    )
    app = main_module.create_app(settings=cfg)
    async with app.router.lifespan_context(app):
        assert hasattr(app.state, "graph")
        assert hasattr(app.state, "chat_service")
        assert "milvus_ensure" in events
        assert "mall_init" in events
        assert any(e.startswith("checkpointer_open:") for e in events)

    assert "mall_close" in events
    assert "checkpointer_close" in events
    assert "dispose_engine" in events
    await engine.dispose()
