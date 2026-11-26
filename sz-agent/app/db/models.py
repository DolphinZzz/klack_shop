import uuid
from datetime import datetime
from typing import Any

from sqlalchemy import (
    BigInteger,
    Boolean,
    DateTime,
    ForeignKey,
    Index,
    Integer,
    JSON,
    String,
    Text,
    UniqueConstraint,
    Uuid,
    func,
    text,
)
from sqlalchemy.dialects.postgresql import JSONB
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.db.base import Base, utcnow

JSON_VARIANT = JSONB().with_variant(JSON(), "sqlite")


class CsSession(Base):
    """Customer service conversation session belonging to a single user."""

    __tablename__ = "cs_sessions"

    id: Mapped[uuid.UUID] = mapped_column(
        Uuid(as_uuid=True), primary_key=True, default=uuid.uuid4
    )
    user_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    title: Mapped[str | None] = mapped_column(String(128), nullable=True)
    status: Mapped[str] = mapped_column(
        String(16), nullable=False, default="ACTIVE", server_default=text("'ACTIVE'")
    )
    has_pending_interrupt: Mapped[bool] = mapped_column(
        Boolean, nullable=False, default=False, server_default=text("false")
    )
    last_intent: Mapped[str | None] = mapped_column(String(32), nullable=True)
    last_message_at: Mapped[datetime | None] = mapped_column(
        DateTime(timezone=True), nullable=True
    )
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        nullable=False,
        default=utcnow,
        server_default=func.now(),
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        nullable=False,
        default=utcnow,
        onupdate=utcnow,
        server_default=func.now(),
    )

    messages: Mapped[list["CsMessage"]] = relationship(
        "CsMessage",
        back_populates="session",
        cascade="all, delete-orphan",
        passive_deletes=True,
        order_by="CsMessage.created_at.asc()",
    )


Index("idx_cs_sessions_user_time", CsSession.user_id, CsSession.last_message_at.desc())


class CsMessage(Base):
    """Display message log entry inside a customer service session."""

    __tablename__ = "cs_messages"

    id: Mapped[uuid.UUID] = mapped_column(
        Uuid(as_uuid=True), primary_key=True, default=uuid.uuid4
    )
    session_id: Mapped[uuid.UUID] = mapped_column(
        Uuid(as_uuid=True),
        ForeignKey("cs_sessions.id", ondelete="CASCADE"),
        nullable=False,
    )
    user_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    role: Mapped[str] = mapped_column(String(16), nullable=False)
    content: Mapped[str] = mapped_column(Text, nullable=False)
    intent: Mapped[str | None] = mapped_column(String(32), nullable=True)
    cards_json: Mapped[Any | None] = mapped_column(JSON_VARIANT, nullable=True)
    citations_json: Mapped[Any | None] = mapped_column(JSON_VARIANT, nullable=True)
    interrupt_payload: Mapped[dict[str, Any] | None] = mapped_column(
        JSON_VARIANT, nullable=True
    )
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        nullable=False,
        default=utcnow,
        server_default=func.now(),
    )

    session: Mapped[CsSession] = relationship("CsSession", back_populates="messages")


Index("idx_cs_messages_session_time", CsMessage.session_id, CsMessage.created_at.asc())


class CsTicket(Base):
    """Asynchronous human customer service support ticket."""

    __tablename__ = "cs_tickets"

    id: Mapped[uuid.UUID] = mapped_column(
        Uuid(as_uuid=True), primary_key=True, default=uuid.uuid4
    )
    ticket_no: Mapped[str] = mapped_column(String(32), unique=True, nullable=False)
    user_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    session_id: Mapped[uuid.UUID | None] = mapped_column(
        Uuid(as_uuid=True),
        ForeignKey("cs_sessions.id", ondelete="SET NULL"),
        nullable=True,
    )
    category: Mapped[str] = mapped_column(String(32), nullable=False)
    title: Mapped[str] = mapped_column(String(128), nullable=False)
    summary: Mapped[str | None] = mapped_column(Text, nullable=True)
    related_order_no: Mapped[str | None] = mapped_column(String(64), nullable=True)
    status: Mapped[str] = mapped_column(
        String(16), nullable=False, default="OPEN", server_default=text("'OPEN'")
    )
    last_reply_at: Mapped[datetime | None] = mapped_column(
        DateTime(timezone=True), nullable=True
    )
    closed_at: Mapped[datetime | None] = mapped_column(
        DateTime(timezone=True), nullable=True
    )
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        nullable=False,
        default=utcnow,
        server_default=func.now(),
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        nullable=False,
        default=utcnow,
        onupdate=utcnow,
        server_default=func.now(),
    )

    session: Mapped[CsSession | None] = relationship("CsSession")
    replies: Mapped[list["CsTicketReply"]] = relationship(
        "CsTicketReply",
        back_populates="ticket",
        cascade="all, delete-orphan",
        passive_deletes=True,
        order_by="CsTicketReply.created_at.asc()",
    )


Index("idx_cs_tickets_user", CsTicket.user_id, CsTicket.created_at.desc())
Index("idx_cs_tickets_status", CsTicket.status, CsTicket.updated_at.desc())


class CsTicketReply(Base):
    """Bidirectional reply entry on an asynchronous support ticket."""

    __tablename__ = "cs_ticket_replies"

    id: Mapped[uuid.UUID] = mapped_column(
        Uuid(as_uuid=True), primary_key=True, default=uuid.uuid4
    )
    ticket_id: Mapped[uuid.UUID] = mapped_column(
        Uuid(as_uuid=True),
        ForeignKey("cs_tickets.id", ondelete="CASCADE"),
        nullable=False,
    )
    sender_type: Mapped[str] = mapped_column(String(16), nullable=False)
    sender_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    sender_name: Mapped[str | None] = mapped_column(String(64), nullable=True)
    content: Mapped[str] = mapped_column(Text, nullable=False)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        nullable=False,
        default=utcnow,
        server_default=func.now(),
    )

    ticket: Mapped[CsTicket] = relationship("CsTicket", back_populates="replies")


Index(
    "idx_cs_ticket_replies_ticket",
    CsTicketReply.ticket_id,
    CsTicketReply.created_at.asc(),
)


class KbDocument(Base):
    """Knowledge base PDF document metadata and ingestion status ledger."""

    __tablename__ = "kb_documents"
    __table_args__ = (UniqueConstraint("file_sha256", name="uk_kb_doc_sha256"),)

    id: Mapped[uuid.UUID] = mapped_column(
        Uuid(as_uuid=True), primary_key=True, default=uuid.uuid4
    )
    title: Mapped[str] = mapped_column(String(256), nullable=False)
    category: Mapped[str] = mapped_column(String(32), nullable=False)
    file_name: Mapped[str | None] = mapped_column(String(256), nullable=True)
    file_path: Mapped[str | None] = mapped_column(String(512), nullable=True)
    file_sha256: Mapped[str | None] = mapped_column(String(64), nullable=True)
    file_size: Mapped[int | None] = mapped_column(BigInteger, nullable=True)
    mineru_mode: Mapped[str | None] = mapped_column(String(32), nullable=True)
    status: Mapped[str] = mapped_column(
        String(16), nullable=False, default="PENDING", server_default=text("'PENDING'")
    )
    chunk_count: Mapped[int] = mapped_column(
        Integer, nullable=False, default=0, server_default=text("0")
    )
    error_msg: Mapped[str | None] = mapped_column(Text, nullable=True)
    uploaded_by: Mapped[int | None] = mapped_column(BigInteger, nullable=True)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        nullable=False,
        default=utcnow,
        server_default=func.now(),
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        nullable=False,
        default=utcnow,
        onupdate=utcnow,
        server_default=func.now(),
    )


Index("idx_kb_doc_status", KbDocument.status)
