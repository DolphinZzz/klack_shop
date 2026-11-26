import uuid
from typing import Any

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.db.base import utcnow
from app.db.models import CsMessage, CsSession
from app.repositories._common import coerce_uuid

VALID_MESSAGE_ROLES = frozenset({"user", "assistant", "system"})


class SessionRepository:
    """User-isolated repository for customer service sessions and messages."""

    def __init__(self, session: AsyncSession) -> None:
        self.session = session

    async def create_session(
        self, user_id: int, title: str | None = None
    ) -> CsSession:
        """Create a new ACTIVE customer service session owned by user_id."""
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
        """List sessions owned by user_id ordered by most recent message."""
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
        """Return the session if it exists and belongs to user_id, else None."""
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
        """Append a message after verifying session ownership by user_id."""
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
        """Return ordered messages if session belongs to user_id, else None."""
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
        """Return ordered messages of a session for admin ticket context replay."""
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
        """Update interrupt state on a session owned by user_id, else return None."""
        cs_session = await self.get_session_for_user(session_id, user_id)
        if cs_session is None:
            return None
        cs_session.has_pending_interrupt = pending
        if intent is not None:
            cs_session.last_intent = intent
        cs_session.updated_at = utcnow()
        await self.session.flush()
        return cs_session
