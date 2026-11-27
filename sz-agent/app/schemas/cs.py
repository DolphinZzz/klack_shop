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
    """计算从 1 开始计数的分页总页数。"""
    if total <= 0 or limit <= 0:
        return 0
    return (total + limit - 1) // limit


def _require_non_blank(value: str, label: str) -> str:
    cleaned = value.strip()
    if not cleaned:
        raise ValueError(f"{label}不能为空")
    return cleaned


class CreateSessionRequest(BaseModel):
    """创建客服会话的请求体。"""

    title: str | None = Field(default=None, max_length=128)


class ChatStreamRequest(BaseModel):
    """发起 SSE 客服流式对话轮次的请求体。"""

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
    """恢复下单确认中断轮次的请求体。"""

    action: Literal["confirm", "cancel"]
    address_id: int | None = Field(
        default=None,
        gt=0,
        validation_alias=AliasChoices("address_id", "addressId"),
    )
    reason: str | None = Field(default=None, max_length=256)


class TicketFollowupRequest(BaseModel):
    """客户端向工单追加追问回复的请求体。"""

    content: str = Field(..., min_length=1, max_length=4000)

    @field_validator("content")
    @classmethod
    def _validate_content(cls, value: str) -> str:
        return _require_non_blank(value, "追问内容")


class AdminTicketReplyRequest(BaseModel):
    """管理员回复客服工单的请求体。"""

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
    """关闭客服工单时的可选请求体。"""

    reason: str | None = Field(default=None, max_length=256)


class KbReindexRequest(BaseModel):
    """重建知识库文档索引时的可选请求体。"""

    use_cached_markdown: bool = Field(
        default=False,
        validation_alias=AliasChoices("use_cached_markdown", "useCachedMarkdown"),
    )


class SessionResponse(BaseModel):
    """序列化后的客服会话元数据。"""

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
    """用户客服会话列表响应。"""

    items: list[SessionResponse]


class MessageResponse(BaseModel):
    """客服会话内序列化后的展示消息。"""

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
    """会话历史消息列表及当前激活的待确认下单中断载荷。"""

    session: SessionResponse
    messages: list[MessageResponse]
    pending_interrupt: dict[str, Any] | None = None


class TicketReplyResponse(BaseModel):
    """序列化后的工单回复记录。"""

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
    """序列化后的客服工单摘要。"""

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
    """包含按时间正序排列回复列表的客服工单详情。"""

    replies: list[TicketReplyResponse] = Field(default_factory=list)

    @classmethod
    def from_orm_model(cls, model: CsTicket) -> TicketDetailResponse:
        summary = TicketSummaryResponse.from_orm_model(model)
        replies = [TicketReplyResponse.from_orm_model(r) for r in (model.replies or [])]
        return cls(**summary.model_dump(), replies=replies)


class AdminTicketDetailResponse(TicketDetailResponse):
    """包含关联会话历史消息回放的管理端工单详情。"""

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
    """分页客服工单列表响应。"""

    current: int
    limit: int
    total: int
    total_page: int
    items: list[TicketSummaryResponse]


class KbDocumentResponse(BaseModel):
    """序列化后的知识库 PDF 文档台账记录。"""

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
    """分页知识库文档列表响应。"""

    current: int
    limit: int
    total: int
    total_page: int
    items: list[KbDocumentResponse]


class DeleteKbDocumentResponse(BaseModel):
    """删除知识库文档后的结果响应。"""

    deleted: bool
    id: str
