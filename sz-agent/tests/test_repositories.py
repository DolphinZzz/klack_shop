import uuid

import pytest
from sqlalchemy.exc import IntegrityError


@pytest.mark.asyncio
async def test_session_and_ticket_user_isolation(async_db_session):
    from app.repositories.session_repo import SessionRepository
    from app.repositories.ticket_repo import TicketRepository

    s_repo = SessionRepository(async_db_session)
    t_repo = TicketRepository(async_db_session)

    s_a = await s_repo.create_session(user_id=101, title="咨询键盘")
    await s_repo.append_message(s_a.id, 101, "user", "推荐一款机械键盘")
    assert await s_repo.get_session_for_user(s_a.id, user_id=202) is None
    assert await s_repo.list_messages_for_user(s_a.id, user_id=202) is None

    ticket = await t_repo.create_ticket(
        101, s_a.id, "order_issue", "催发货", "用户询问何时发货", "ORD1001"
    )
    assert ticket.status == "OPEN" and ticket.ticket_no.startswith("TK")
    assert await t_repo.get_ticket_detail_for_user(ticket.id, user_id=202) is None


@pytest.mark.asyncio
async def test_ticket_state_machine_and_session_system_message(async_db_session):
    from app.repositories.session_repo import SessionRepository
    from app.repositories.ticket_repo import TicketRepository

    s_repo = SessionRepository(async_db_session)
    t_repo = TicketRepository(async_db_session)

    s_a = await s_repo.create_session(user_id=101, title="退款问题")
    ticket = await t_repo.create_ticket(
        101, s_a.id, "after_sale", "未发货退款", "申请取消", "ORD1002"
    )

    replied = await t_repo.add_admin_reply(
        ticket.id,
        admin_id=1,
        admin_name="客服A",
        content="已为您安排拦截",
        close_ticket=False,
    )
    assert replied.status == "REPLIED"
    msgs = await s_repo.list_messages_for_user(s_a.id, 101)
    assert msgs is not None
    assert any(
        m.role == "system" and ticket.ticket_no in m.content and "已为您安排拦截" in m.content
        for m in msgs
    )

    reopened = await t_repo.add_user_followup(
        ticket.id, user_id=101, content="请问多久退回余额？"
    )
    assert reopened.status == "OPEN"

    closed = await t_repo.close_ticket(ticket.id)
    assert closed.status == "CLOSED"
    with pytest.raises(ValueError, match="已关闭"):
        await t_repo.add_user_followup(ticket.id, user_id=101, content="再问一句")

    msg_count_before = len(await s_repo.list_messages_for_user(s_a.id, 101) or [])
    with pytest.raises(ValueError, match="已关闭"):
        await t_repo.add_admin_reply(
            ticket.id, admin_id=1, admin_name="客服A", content="关闭后再回复"
        )
    detail_after = await t_repo.get_ticket_detail_for_admin(ticket.id)
    assert detail_after is not None and len(detail_after.replies) == 2
    assert len(await s_repo.list_messages_for_user(s_a.id, 101) or []) == msg_count_before


@pytest.mark.asyncio
async def test_session_repository_isolation_interrupt_and_admin_view(async_db_session):
    from app.repositories.session_repo import SessionRepository

    repo = SessionRepository(async_db_session)
    s1 = await repo.create_session(user_id=101, title="第一会话")
    s2 = await repo.create_session(user_id=101, title="第二会话")
    await repo.create_session(user_id=202, title="他人会话")

    # 拒绝跨用户追加消息
    with pytest.raises(LookupError):
        await repo.append_message(s1.id, user_id=202, role="user", content="越权写入")

    # 跨用户更新中断状态应返回 None 且不修改原标志
    assert await repo.set_pending_interrupt(s1.id, user_id=202, pending=True, intent="recommend_order") is None
    owned = await repo.get_session_for_user(s1.id, user_id=101)
    assert owned is not None and owned.has_pending_interrupt is False

    # 会话归属人更新中断状态并追加结构化消息
    updated = await repo.set_pending_interrupt(s1.id, user_id=101, pending=True, intent="recommend_order")
    assert updated is not None and updated.has_pending_interrupt is True and updated.last_intent == "recommend_order"

    msg = await repo.append_message(
        s1.id,
        user_id=101,
        role="assistant",
        content="为您找到热销键盘",
        intent="recommend_order",
        cards_json=[{"card_type": "product_list", "items": [{"id": 1}]}],
        citations_json=[{"doc_title": "选轴指南", "section_path": "第一章"}],
        interrupt_payload={"type": "order_confirm", "draft_id": "d1"},
    )
    assert msg.cards_json == [{"card_type": "product_list", "items": [{"id": 1}]}]
    assert msg.citations_json == [{"doc_title": "选轴指南", "section_path": "第一章"}]
    assert msg.interrupt_payload == {"type": "order_confirm", "draft_id": "d1"}

    # 当 intent=None 时清除中断状态应保留原 last_intent
    cleared = await repo.set_pending_interrupt(s1.id, user_id=101, pending=False, intent=None)
    assert cleared is not None and cleared.has_pending_interrupt is False
    assert cleared.last_intent == "recommend_order"

    # list_sessions 仅返回当前用户的会话，并按 last_message_at 降序排列
    user_sessions = await repo.list_sessions(user_id=101, limit=10)
    assert [s.id for s in user_sessions] == [s1.id, s2.id]
    assert len(await repo.list_sessions(user_id=101, limit=1)) == 1

    # 管理端回放消息不受 user_id 限制
    admin_msgs = await repo.list_messages_for_admin(s1.id)
    assert len(admin_msgs) == 1 and admin_msgs[0].id == msg.id
    assert await repo.get_session_for_user("not-a-uuid", user_id=101) is None

    # 删除会话时通过 SQLite 外键级联删除子消息
    await async_db_session.delete(s1)
    await async_db_session.flush()
    assert await repo.list_messages_for_admin(s1.id) == []


@pytest.mark.asyncio
async def test_ticket_repository_isolation_filters_and_close_on_reply(async_db_session):
    from app.repositories.session_repo import SessionRepository
    from app.repositories.ticket_repo import TicketRepository

    s_repo = SessionRepository(async_db_session)
    t_repo = TicketRepository(async_db_session)

    s_a = await s_repo.create_session(user_id=101, title="售后咨询")

    # 禁止将工单关联到其他用户的会话
    with pytest.raises(LookupError):
        await t_repo.create_ticket(
            user_id=202,
            session_id=s_a.id,
            category="order_issue",
            title="越权关联",
            summary="恶意关联他人会话",
            related_order_no=None,
        )

    t1 = await t_repo.create_ticket(101, s_a.id, "order_issue", "催发货", "一直未发", "ORD2001")
    t2 = await t_repo.create_ticket(101, None, "complaint", "包装破损", "外盒压扁", "ORD2002")
    t3 = await t_repo.create_ticket(202, None, "after_sale", "他人退货", "申请退货", "ORD3001")

    # 拒绝跨用户追问工单
    with pytest.raises(LookupError):
        await t_repo.add_user_followup(t1.id, user_id=202, content="冒充追问")

    # 用户工单列表与数量按用户隔离
    user_tickets = await t_repo.list_tickets_for_user(101)
    assert {t.id for t in user_tickets} == {t1.id, t2.id}
    assert await t_repo.count_tickets_for_user(101) == 2

    # 无关联 session_id 时管理端回复并设置 close_ticket=True 可正常关闭工单
    closed_t2 = await t_repo.add_admin_reply(
        t2.id, admin_id=2, admin_name="客服B", content="已补发新包装盒", close_ticket=True
    )
    assert closed_t2.status == "CLOSED" and closed_t2.closed_at is not None

    # 管理端工单列表筛选条件
    closed_list = await t_repo.list_tickets_for_admin(status="CLOSED")
    assert [t.id for t in closed_list] == [t2.id]
    by_order = await t_repo.list_tickets_for_admin(related_order_no="ORD3001")
    assert [t.id for t in by_order] == [t3.id]
    by_kw = await t_repo.list_tickets_for_admin(keyword="压扁")
    assert [t.id for t in by_kw] == [t2.id]
    assert await t_repo.count_tickets_for_admin(status="OPEN") == 2

    detail_admin = await t_repo.get_ticket_detail_for_admin(t2.id)
    assert detail_admin is not None and len(detail_admin.replies) == 1
    assert detail_admin.replies[0].sender_type == "ADMIN"

    # 用户对 t1 追问后，详情接口应重新加载最新回复列表
    await t_repo.add_admin_reply(t1.id, admin_id=1, admin_name="客服A", content="明天发", close_ticket=False)
    await t_repo.add_user_followup(t1.id, user_id=101, content="麻烦加急")
    detail_user = await t_repo.get_ticket_detail_for_user(t1.id, user_id=101)
    assert detail_user is not None
    assert [r.sender_type for r in detail_user.replies] == ["ADMIN", "USER"]


@pytest.mark.asyncio
async def test_kb_repository_lifecycle_and_sha256_uniqueness(async_db_session):
    from app.repositories.kb_repo import KbRepository

    repo = KbRepository(async_db_session)
    doc = await repo.create_document(
        title="消费者权益保护法",
        category="consumer_law",
        file_name="law.pdf",
        file_path="/data/kb/law.pdf",
        file_sha256="deadbeef" * 8,
        file_size=12345,
        mineru_mode="cloud_api",
        uploaded_by=1,
    )
    assert doc.status == "PENDING" and doc.chunk_count == 0

    found = await repo.get_by_sha256("deadbeef" * 8)
    assert found is not None and found.id == doc.id

    # 重复的 sha256 违反唯一约束 uk_kb_doc_sha256
    with pytest.raises(IntegrityError):
        await repo.create_document(
            title="重复文件",
            category="consumer_law",
            file_name="dup.pdf",
            file_path="/data/kb/dup.pdf",
            file_sha256="deadbeef" * 8,
            file_size=12345,
            mineru_mode="cloud_api",
            uploaded_by=1,
        )
    await async_db_session.rollback()

    # 重新创建文档并验证状态流转
    doc = await repo.create_document(
        title="商城售后规范",
        category="mall_policy",
        file_name="policy.pdf",
        file_path="/data/kb/policy.pdf",
        file_sha256="cafebabe" * 8,
        file_size=2048,
        mineru_mode="cloud_api",
        uploaded_by=1,
    )
    await repo.update_status(doc.id, status="FAILED", chunk_count=0, error_msg="MinerU timeout")
    failed = await repo.get_document(doc.id)
    assert failed is not None and failed.status == "FAILED" and failed.error_msg == "MinerU timeout"

    await repo.update_status(doc.id, status="INDEXED", chunk_count=12, error_msg=None)
    indexed = await repo.get_document(doc.id)
    assert indexed is not None and indexed.status == "INDEXED" and indexed.chunk_count == 12 and indexed.error_msg is None

    assert len(await repo.list_documents(status="INDEXED", category="mall_policy")) == 1
    assert await repo.count_documents(status="INDEXED") == 1
    assert await repo.delete_document(doc.id) is True
    assert await repo.delete_document(doc.id) is False
    assert await repo.get_document(uuid.uuid4()) is None


def test_orm_metadata_defines_all_5_tables_and_indexes():
    from app.db.base import Base
    from app.db.models import CsTicket, KbDocument

    assert set(Base.metadata.tables.keys()) == {
        "cs_sessions",
        "cs_messages",
        "cs_tickets",
        "cs_ticket_replies",
        "kb_documents",
    }
    all_indexes = {
        idx.name
        for table in Base.metadata.tables.values()
        for idx in table.indexes
    }
    assert {
        "idx_cs_sessions_user_time",
        "idx_cs_messages_session_time",
        "idx_cs_tickets_user",
        "idx_cs_tickets_status",
        "idx_cs_ticket_replies_ticket",
        "idx_kb_doc_status",
    }.issubset(all_indexes)
    ticket_constraints = {c.name for c in CsTicket.__table__.constraints if c.name}
    kb_constraints = {c.name for c in KbDocument.__table__.constraints if c.name}
    assert "uq_cs_tickets_ticket_no" in ticket_constraints
    assert "uk_kb_doc_sha256" in kb_constraints
