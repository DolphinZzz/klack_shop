import uuid
from datetime import datetime

from sqlalchemy import func, or_, select
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import selectinload

from app.db.base import utcnow
from app.db.models import CsMessage, CsSession, CsTicket, CsTicketReply
from app.repositories._common import coerce_uuid

VALID_TICKET_STATUSES = frozenset({"OPEN", "REPLIED", "CLOSED"})


def _generate_ticket_no(now: datetime) -> str:
    """Generate a unique TK-prefixed ticket number within 32 characters."""
    suffix = uuid.uuid4().int % 10**8
    return f"TK{now.strftime('%Y%m%d%H%M%S')}{suffix:08d}"


def _escape_like(term: str) -> str:
    """Escape SQL LIKE wildcard characters in user-supplied search strings."""
    return term.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")


class TicketRepository:
    """Repository for asynchronous human support tickets and replies."""

    def __init__(self, session: AsyncSession) -> None:
        self.session = session

    async def create_ticket(
        self,
        user_id: int,
        session_id: uuid.UUID | str | None,
        category: str,
        title: str,
        summary: str | None = None,
        related_order_no: str | None = None,
    ) -> CsTicket:
        """Create an OPEN ticket, validating session ownership if session_id is given."""
        linked_sid: uuid.UUID | None = None
        if session_id is not None:
            sid = coerce_uuid(session_id)
            if sid is None:
                raise LookupError("关联会话不存在或无权访问")
            owner_stmt = select(CsSession.id).where(
                CsSession.id == sid,
                CsSession.user_id == user_id,
            )
            owned_sid = (await self.session.execute(owner_stmt)).scalar_one_or_none()
            if owned_sid is None:
                raise LookupError("关联会话不存在或无权访问")
            linked_sid = owned_sid

        now = utcnow()
        ticket = CsTicket(
            ticket_no=_generate_ticket_no(now),
            user_id=user_id,
            session_id=linked_sid,
            category=category,
            title=title[:128],
            summary=summary,
            related_order_no=related_order_no,
            status="OPEN",
            created_at=now,
            updated_at=now,
        )
        self.session.add(ticket)
        await self.session.flush()
        return ticket

    async def list_tickets_for_user(
        self,
        user_id: int,
        status: str | None = None,
        limit: int = 20,
        offset: int = 0,
    ) -> list[CsTicket]:
        """List tickets owned by user_id ordered by newest first."""
        stmt = select(CsTicket).where(CsTicket.user_id == user_id)
        if status is not None:
            stmt = stmt.where(CsTicket.status == status)
        stmt = stmt.order_by(CsTicket.created_at.desc()).offset(offset).limit(limit)
        result = await self.session.execute(stmt)
        return list(result.scalars().all())

    async def count_tickets_for_user(
        self, user_id: int, status: str | None = None
    ) -> int:
        """Count tickets owned by user_id with optional status filter."""
        stmt = select(func.count(CsTicket.id)).where(CsTicket.user_id == user_id)
        if status is not None:
            stmt = stmt.where(CsTicket.status == status)
        return int((await self.session.execute(stmt)).scalar_one())

    async def get_ticket_detail_for_user(
        self, ticket_id: uuid.UUID | str, user_id: int
    ) -> CsTicket | None:
        """Return ticket with replies if owned by user_id, else None."""
        tid = coerce_uuid(ticket_id)
        if tid is None:
            return None
        stmt = (
            select(CsTicket)
            .options(selectinload(CsTicket.replies))
            .execution_options(populate_existing=True)
            .where(CsTicket.id == tid, CsTicket.user_id == user_id)
        )
        result = await self.session.execute(stmt)
        return result.scalar_one_or_none()

    async def add_user_followup(
        self, ticket_id: uuid.UUID | str, user_id: int, content: str
    ) -> CsTicket:
        """Append a USER reply and reopen an OPEN/REPLIED ticket owned by user_id."""
        if not content or not content.strip():
            raise ValueError("追问内容不能为空")

        ticket = await self.get_ticket_detail_for_user(ticket_id, user_id)
        if ticket is None:
            raise LookupError("工单不存在或无权访问")
        if ticket.status == "CLOSED":
            raise ValueError("工单已关闭，无法继续追问")
        if ticket.status not in VALID_TICKET_STATUSES:
            raise ValueError(f"当前工单状态不允许追问: {ticket.status}")

        now = utcnow()
        reply = CsTicketReply(
            ticket_id=ticket.id,
            sender_type="USER",
            sender_id=user_id,
            sender_name=f"用户{user_id}",
            content=content,
            created_at=now,
        )
        self.session.add(reply)
        ticket.status = "OPEN"
        ticket.last_reply_at = now
        ticket.updated_at = now
        await self.session.flush()
        return await self._reload_ticket_with_replies(ticket.id)

    def _build_admin_filter_clauses(
        self,
        status: str | None = None,
        category: str | None = None,
        related_order_no: str | None = None,
        keyword: str | None = None,
    ) -> list:
        clauses = []
        if status:
            clauses.append(CsTicket.status == status)
        if category:
            clauses.append(CsTicket.category == category)
        if related_order_no:
            clauses.append(CsTicket.related_order_no == related_order_no)
        if keyword and keyword.strip():
            pattern = f"%{_escape_like(keyword.strip())}%"
            clauses.append(
                or_(
                    CsTicket.ticket_no.ilike(pattern, escape="\\"),
                    CsTicket.title.ilike(pattern, escape="\\"),
                    CsTicket.summary.ilike(pattern, escape="\\"),
                    CsTicket.related_order_no.ilike(pattern, escape="\\"),
                )
            )
        return clauses

    async def list_tickets_for_admin(
        self,
        status: str | None = None,
        category: str | None = None,
        related_order_no: str | None = None,
        keyword: str | None = None,
        limit: int = 20,
        offset: int = 0,
    ) -> list[CsTicket]:
        """List tickets for admin console with optional filters and pagination."""
        clauses = self._build_admin_filter_clauses(
            status=status,
            category=category,
            related_order_no=related_order_no,
            keyword=keyword,
        )
        stmt = (
            select(CsTicket)
            .where(*clauses)
            .order_by(CsTicket.updated_at.desc(), CsTicket.created_at.desc())
            .offset(offset)
            .limit(limit)
        )
        result = await self.session.execute(stmt)
        return list(result.scalars().all())

    async def count_tickets_for_admin(
        self,
        status: str | None = None,
        category: str | None = None,
        related_order_no: str | None = None,
        keyword: str | None = None,
    ) -> int:
        """Count tickets matching admin filter criteria."""
        clauses = self._build_admin_filter_clauses(
            status=status,
            category=category,
            related_order_no=related_order_no,
            keyword=keyword,
        )
        stmt = select(func.count(CsTicket.id)).where(*clauses)
        return int((await self.session.execute(stmt)).scalar_one())

    async def get_ticket_detail_for_admin(
        self, ticket_id: uuid.UUID | str
    ) -> CsTicket | None:
        """Return ticket with replies for admin inspection, or None if not found."""
        tid = coerce_uuid(ticket_id)
        if tid is None:
            return None
        return await self._reload_ticket_with_replies_or_none(tid)

    async def add_admin_reply(
        self,
        ticket_id: uuid.UUID | str,
        admin_id: int,
        admin_name: str,
        content: str,
        close_ticket: bool = False,
    ) -> CsTicket:
        """Write an ADMIN reply, update status, and sync a system message to session."""
        if not content or not content.strip():
            raise ValueError("回复内容不能为空")

        ticket = await self.get_ticket_detail_for_admin(ticket_id)
        if ticket is None:
            raise LookupError("工单不存在")

        now = utcnow()
        reply = CsTicketReply(
            ticket_id=ticket.id,
            sender_type="ADMIN",
            sender_id=admin_id,
            sender_name=admin_name,
            content=content,
            created_at=now,
        )
        self.session.add(reply)

        ticket.status = "CLOSED" if close_ticket else "REPLIED"
        ticket.last_reply_at = now
        ticket.updated_at = now
        if close_ticket:
            ticket.closed_at = now

        if ticket.session_id is not None:
            session_stmt = select(CsSession).where(
                CsSession.id == ticket.session_id,
                CsSession.user_id == ticket.user_id,
            )
            linked_session = (
                await self.session.execute(session_stmt)
            ).scalar_one_or_none()
            if linked_session is not None:
                sys_msg = CsMessage(
                    session_id=linked_session.id,
                    user_id=ticket.user_id,
                    role="system",
                    content=(
                        f"【工单 {ticket.ticket_no} 客服回复】"
                        f"{admin_name}：{content}"
                    ),
                    created_at=now,
                )
                self.session.add(sys_msg)
                linked_session.last_message_at = now
                linked_session.updated_at = now

        await self.session.flush()
        return await self._reload_ticket_with_replies(ticket.id)

    async def close_ticket(self, ticket_id: uuid.UUID | str) -> CsTicket:
        """Transition a ticket to CLOSED status."""
        ticket = await self.get_ticket_detail_for_admin(ticket_id)
        if ticket is None:
            raise LookupError("工单不存在")
        now = utcnow()
        ticket.status = "CLOSED"
        if ticket.closed_at is None:
            ticket.closed_at = now
        ticket.updated_at = now
        await self.session.flush()
        return ticket

    async def _reload_ticket_with_replies_or_none(
        self, ticket_id: uuid.UUID
    ) -> CsTicket | None:
        stmt = (
            select(CsTicket)
            .options(selectinload(CsTicket.replies))
            .execution_options(populate_existing=True)
            .where(CsTicket.id == ticket_id)
        )
        result = await self.session.execute(stmt)
        return result.scalar_one_or_none()

    async def _reload_ticket_with_replies(self, ticket_id: uuid.UUID) -> CsTicket:
        ticket = await self._reload_ticket_with_replies_or_none(ticket_id)
        assert ticket is not None
        return ticket
