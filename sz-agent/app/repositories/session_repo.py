import uuid
from typing import Any

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.db.base import utcnow
from app.db.models import CsMessage, CsSession
from app.repositories._common import coerce_uuid

VALID_MESSAGE_ROLES = frozenset({"user", "assistant", "system"})


class SessionRepository:
    """按用户隔离的客服会话与消息数据仓储。"""

    def __init__(self, session: AsyncSession) -> None:
        self.session = session

    async def create_session(
        self, user_id: int, title: str | None = None
    ) -> CsSession:
        """创建归属于 user_id 的新 ACTIVE 客服会话。"""
        now = utcnow()
        clean_title = title.strip()[:128] if title and title.strip() else "新会话"
        cs_session = CsSession(
            user_id=user_id,
            title=clean_title,
            status="ACTIVE",
            has_pending_interrupt=False,
            last_message_at=now,
            created_at=now,
            updated_at=now,
        )
        self.session.add(cs_session)
        await self.session.flush()
        return cs_session

    async def list_sessions(
        self,
        user_id: int,
        limit: int = 20,
        status: str | None = None,
    ) -> list[CsSession]:
        """按最近消息时间倒序查询归属于 user_id 的会话列表。"""
        stmt = select(CsSession).where(CsSession.user_id == user_id)
        if status is not None:
            stmt = stmt.where(CsSession.status == status)
        stmt = stmt.order_by(
            CsSession.last_message_at.desc().nullslast(),
            CsSession.created_at.desc(),
        ).limit(limit)
        result = await self.session.execute(stmt)
        return list(result.scalars().all())

    async def get_session_for_user(
        self, session_id: uuid.UUID | str, user_id: int
    ) -> CsSession | None:
        """若会话存在且归属于 user_id 则返回该会话，否则返回 None。"""
        sid = coerce_uuid(session_id)
        if sid is None:
            return None
        stmt = select(CsSession).where(
            CsSession.id == sid,
            CsSession.user_id == user_id,
        )
        result = await self.session.execute(stmt)
        return result.scalar_one_or_none()

    async def append_message(
        self,
        session_id: uuid.UUID | str,
        user_id: int,
        role: str,
        content: str,
        intent: str | None = None,
        cards_json: Any | None = None,
        citations_json: Any | None = None,
        interrupt_payload: dict[str, Any] | None = None,
    ) -> CsMessage:
        """校验会话归属于 user_id 后追加一条消息记录。"""
        if role not in VALID_MESSAGE_ROLES:
            raise ValueError(f"Unsupported message role: {role!r}")

        cs_session = await self.get_session_for_user(session_id, user_id)
        if cs_session is None:
            raise LookupError("会话不存在或无权访问")

        now = utcnow()
        message = CsMessage(
            session_id=cs_session.id,
            user_id=user_id,
            role=role,
            content=content,
            intent=intent,
            cards_json=cards_json,
            citations_json=citations_json,
            interrupt_payload=interrupt_payload,
            created_at=now,
        )
        self.session.add(message)
        cs_session.last_message_at = now
        cs_session.updated_at = now
        if intent is not None:
            cs_session.last_intent = intent
        await self.session.flush()
        return message

    async def list_messages_for_user(
        self, session_id: uuid.UUID | str, user_id: int
    ) -> list[CsMessage] | None:
        """若会话归属于 user_id 则按时间升序返回消息列表，否则返回 None。"""
        cs_session = await self.get_session_for_user(session_id, user_id)
        if cs_session is None:
            return None
        stmt = (
            select(CsMessage)
            .where(
                CsMessage.session_id == cs_session.id,
                CsMessage.user_id == user_id,
            )
            .order_by(CsMessage.created_at.asc())
        )
        result = await self.session.execute(stmt)
        return list(result.scalars().all())

    async def list_messages_for_admin(
        self, session_id: uuid.UUID | str
    ) -> list[CsMessage]:
        """按时间升序返回指定会话的消息列表，供管理端工单上下文回放使用。"""
        sid = coerce_uuid(session_id)
        if sid is None:
            return []
        stmt = (
            select(CsMessage)
            .where(CsMessage.session_id == sid)
            .order_by(CsMessage.created_at.asc())
        )
        result = await self.session.execute(stmt)
        return list(result.scalars().all())

    async def set_pending_interrupt(
        self,
        session_id: uuid.UUID | str,
        user_id: int,
        pending: bool,
        intent: str | None = None,
    ) -> CsSession | None:
        """更新归属于 user_id 的会话中断状态，不存在或无权访问时返回 None。"""
        cs_session = await self.get_session_for_user(session_id, user_id)
        if cs_session is None:
            return None
        cs_session.has_pending_interrupt = pending
        if intent is not None:
            cs_session.last_intent = intent
        cs_session.updated_at = utcnow()
        await self.session.flush()
        return cs_session
