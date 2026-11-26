from __future__ import annotations

from datetime import datetime
from typing import Any, Literal

from pydantic import AliasChoices, BaseModel, Field, field_validator

from app.db.models import CsMessage, CsSession, CsTicket, CsTicketReply, KbDocument

VALID_SESSION_STATUSES: frozenset[str] = frozenset({"ACTIVE", "ARCHIVED"})
VALID_KB_CATEGORIES: frozenset[str] = frozenset(
    {"consumer_law", "mall_policy", "faq", "product_guide"}
)


def compute_total_pages(total: int, limit: int) -> int:
    """Compute total page count for 1-indexed pagination."""
    if total <= 0 or limit <= 0:
        return 0
    return (total + limit - 1) // limit


def _require_non_blank(value: str, label: str) -> str:
    cleaned = value.strip()
    if not cleaned:
        raise ValueError(f"{label}不能为空")
    return cleaned


class CreateSessionRequest(BaseModel):
    """Request body for creating a customer service session."""

    title: str | None = Field(default=None, max_length=128)


class ChatStreamRequest(BaseModel):
    """Request body for initiating an SSE customer service chat turn."""

    message: str = Field(..., min_length=1, max_length=4000)
    force_ticket: bool = Field(
        default=False,
        validation_alias=AliasChoices("force_ticket", "forceTicket"),
    )

    @field_validator("message")
    @classmethod
    def _validate_message(cls, value: str) -> str:
        return _require_non_blank(value, "消息内容")


class ResumeStreamRequest(BaseModel):
    """Request body for resuming an interrupted order confirmation turn."""

    action: Literal["confirm", "cancel"]
    address_id: int | None = Field(
        default=None,
        gt=0,
        validation_alias=AliasChoices("address_id", "addressId"),
    )
    reason: str | None = Field(default=None, max_length=256)


class TicketFollowupRequest(BaseModel):
    """Request body for appending a client follow-up reply to a ticket."""

    content: str = Field(..., min_length=1, max_length=4000)

    @field_validator("content")
    @classmethod
    def _validate_content(cls, value: str) -> str:
        return _require_non_blank(value, "追问内容")


class AdminTicketReplyRequest(BaseModel):
    """Request body for an admin reply to a support ticket."""

    content: str = Field(..., min_length=1, max_length=4000)
    close_ticket: bool = Field(
        default=False,
        validation_alias=AliasChoices("close_ticket", "closeTicket"),
    )

    @field_validator("content")
    @classmethod
    def _validate_content(cls, value: str) -> str:
        return _require_non_blank(value, "回复内容")


class AdminTicketCloseRequest(BaseModel):
    """Optional request body when closing a support ticket."""

    reason: str | None = Field(default=None, max_length=256)


class KbReindexRequest(BaseModel):
    """Optional request body when reindexing a knowledge base document."""

    use_cached_markdown: bool = Field(
        default=False,
        validation_alias=AliasChoices("use_cached_markdown", "useCachedMarkdown"),
    )


class SessionResponse(BaseModel):
    """Serialized customer service session metadata."""

    id: str
    user_id: int
    title: str | None = None
    status: str
    has_pending_interrupt: bool
    last_intent: str | None = None
    last_message_at: datetime | None = None
    created_at: datetime
    updated_at: datetime

    @classmethod
    def from_orm_model(cls, model: CsSession) -> SessionResponse:
        return cls(
            id=str(model.id),
            user_id=int(model.user_id),
            title=model.title,
            status=model.status,
            has_pending_interrupt=bool(model.has_pending_interrupt),
            last_intent=model.last_intent,
            last_message_at=model.last_message_at,
            created_at=model.created_at,
            updated_at=model.updated_at,
        )


class SessionListResponse(BaseModel):
    """List response for a user's customer service sessions."""

    items: list[SessionResponse]


class MessageResponse(BaseModel):
    """Serialized display message inside a customer service session."""

    id: str
    session_id: str
    role: str
    content: str
    intent: str | None = None
    cards: list[dict[str, Any]] | None = None
    citations: list[dict[str, Any]] | None = None
    interrupt_payload: dict[str, Any] | None = None
    created_at: datetime

    @classmethod
    def from_orm_model(cls, model: CsMessage) -> MessageResponse:
        cards = list(model.cards_json) if isinstance(model.cards_json, list) else None
        citations = (
            list(model.citations_json) if isinstance(model.citations_json, list) else None
        )
        return cls(
            id=str(model.id),
            session_id=str(model.session_id),
            role=model.role,
            content=model.content,
            intent=model.intent,
            cards=cards,
            citations=citations,
            interrupt_payload=model.interrupt_payload,
            created_at=model.created_at,
        )


class SessionMessagesResponse(BaseModel):
    """Session message history plus any active pending order interrupt payload."""

    session: SessionResponse
    messages: list[MessageResponse]
    pending_interrupt: dict[str, Any] | None = None


class TicketReplyResponse(BaseModel):
    """Serialized ticket reply entry."""

    id: str
    ticket_id: str
    sender_type: str
    sender_id: int
    sender_name: str | None = None
    content: str
    created_at: datetime

    @classmethod
    def from_orm_model(cls, model: CsTicketReply) -> TicketReplyResponse:
        return cls(
            id=str(model.id),
            ticket_id=str(model.ticket_id),
            sender_type=model.sender_type,
            sender_id=int(model.sender_id),
            sender_name=model.sender_name,
            content=model.content,
            created_at=model.created_at,
        )


class TicketSummaryResponse(BaseModel):
    """Serialized support ticket summary."""

    id: str
    ticket_no: str
    user_id: int
    session_id: str | None = None
    category: str
    title: str
    summary: str | None = None
    related_order_no: str | None = None
    status: str
    last_reply_at: datetime | None = None
    closed_at: datetime | None = None
    created_at: datetime
    updated_at: datetime

    @classmethod
    def from_orm_model(cls, model: CsTicket) -> TicketSummaryResponse:
        return cls(
            id=str(model.id),
            ticket_no=model.ticket_no,
            user_id=int(model.user_id),
            session_id=str(model.session_id) if model.session_id is not None else None,
            category=model.category,
            title=model.title,
            summary=model.summary,
            related_order_no=model.related_order_no,
            status=model.status,
            last_reply_at=model.last_reply_at,
            closed_at=model.closed_at,
            created_at=model.created_at,
            updated_at=model.updated_at,
        )


class TicketDetailResponse(TicketSummaryResponse):
    """Serialized support ticket with chronological replies."""

    replies: list[TicketReplyResponse] = Field(default_factory=list)

    @classmethod
    def from_orm_model(cls, model: CsTicket) -> TicketDetailResponse:
        summary = TicketSummaryResponse.from_orm_model(model)
        replies = [TicketReplyResponse.from_orm_model(r) for r in (model.replies or [])]
        return cls(**summary.model_dump(), replies=replies)


class AdminTicketDetailResponse(TicketDetailResponse):
    """Admin ticket detail including linked session message replay."""

    session_messages: list[MessageResponse] = Field(default_factory=list)

    @classmethod
    def from_orm_with_messages(
        cls,
        ticket: CsTicket,
        session_messages: list[CsMessage],
    ) -> AdminTicketDetailResponse:
        detail = TicketDetailResponse.from_orm_model(ticket)
        return cls(
            **detail.model_dump(),
            session_messages=[MessageResponse.from_orm_model(m) for m in session_messages],
        )


class TicketPageResponse(BaseModel):
    """Paginated support ticket list."""

    current: int
    limit: int
    total: int
    total_page: int
    items: list[TicketSummaryResponse]


class KbDocumentResponse(BaseModel):
    """Serialized knowledge base PDF document ledger row."""

    id: str
    title: str
    category: str
    file_name: str | None = None
    file_path: str | None = None
    file_sha256: str | None = None
    file_size: int | None = None
    mineru_mode: str | None = None
    status: str
    chunk_count: int
    error_msg: str | None = None
    uploaded_by: int | None = None
    created_at: datetime
    updated_at: datetime

    @classmethod
    def from_orm_model(cls, model: KbDocument) -> KbDocumentResponse:
        return cls(
            id=str(model.id),
            title=model.title,
            category=model.category,
            file_name=model.file_name,
            file_path=model.file_path,
            file_sha256=model.file_sha256,
            file_size=model.file_size,
            mineru_mode=model.mineru_mode,
            status=model.status,
            chunk_count=int(model.chunk_count or 0),
            error_msg=model.error_msg,
            uploaded_by=model.uploaded_by,
            created_at=model.created_at,
            updated_at=model.updated_at,
        )


class KbDocumentPageResponse(BaseModel):
    """Paginated knowledge base document list."""

    current: int
    limit: int
    total: int
    total_page: int
    items: list[KbDocumentResponse]


class DeleteKbDocumentResponse(BaseModel):
    """Result payload after deleting a knowledge base document."""

    deleted: bool
    id: str
