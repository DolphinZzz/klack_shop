from __future__ import annotations

import uuid

from fastapi import APIRouter, Body, Depends, Header, HTTPException, Query, Request, status
from fastapi.responses import StreamingResponse
from sqlalchemy.ext.asyncio import AsyncSession

from app.repositories.session_repo import SessionRepository
from app.repositories.ticket_repo import TicketRepository, VALID_TICKET_STATUSES
from app.routers import (
    SSE_RESPONSE_HEADERS,
    get_request_db_session,
    normalize_optional_filter,
    parse_uuid_or_404,
    resolve_chat_service,
)
from app.schemas.cs import (
    ChatStreamRequest,
    CreateSessionRequest,
    MessageResponse,
    ResumeStreamRequest,
    SessionListResponse,
    SessionMessagesResponse,
    SessionResponse,
    TicketDetailResponse,
    TicketFollowupRequest,
    TicketPageResponse,
    TicketSummaryResponse,
    VALID_SESSION_STATUSES,
    compute_total_pages,
)
from app.security.auth import get_client_user_id, verify_internal_secret
from app.services.chat_service import ChatService

router = APIRouter(
    prefix="/internal/cs/client",
    dependencies=[Depends(verify_internal_secret)],
)


@router.get("/ping")
def client_ping(user_id: int = Depends(get_client_user_id)) -> dict[str, int | str]:
    """内部用户端请求的健康检查与鉴权校验路由。"""
    return {"status": "ok", "user_id": user_id}


@router.get("/sessions", response_model=SessionListResponse)
async def list_sessions(
    limit: int = Query(default=20, ge=1, le=100),
    session_status: str | None = Query(default=None, alias="status"),
    user_id: int = Depends(get_client_user_id),
    db: AsyncSession = Depends(get_request_db_session),
) -> SessionListResponse:
    """查询当前已认证用户拥有的客服会话列表。"""
    normalized_status = normalize_optional_filter(
        session_status,
        VALID_SESSION_STATUSES,
        field_name="status",
        uppercase=True,
    )
    sessions = await SessionRepository(db).list_sessions(
        user_id=user_id,
        limit=limit,
        status=normalized_status,
    )
    return SessionListResponse(
        items=[SessionResponse.from_orm_model(item) for item in sessions]
    )


@router.post("/sessions", response_model=SessionResponse)
async def create_session(
    payload: CreateSessionRequest | None = Body(default=None),
    user_id: int = Depends(get_client_user_id),
    db: AsyncSession = Depends(get_request_db_session),
) -> SessionResponse:
    """为当前已认证用户创建新的 ACTIVE 客服会话。"""
    title = payload.title if payload is not None else None
    created = await SessionRepository(db).create_session(user_id=user_id, title=title)
    return SessionResponse.from_orm_model(created)


@router.get("/sessions/{session_id}/messages", response_model=SessionMessagesResponse)
async def list_session_messages(
    session_id: str,
    user_id: int = Depends(get_client_user_id),
    db: AsyncSession = Depends(get_request_db_session),
) -> SessionMessagesResponse:
    """获取用户所属会话的有序历史消息及当前待处理的中断状态。"""
    sid = parse_uuid_or_404(session_id, "会话不存在或无权访问")
    repo = SessionRepository(db)
    cs_session = await repo.get_session_for_user(sid, user_id)
    if cs_session is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="会话不存在或无权访问",
        )
    messages = await repo.list_messages_for_user(cs_session.id, user_id)
    assert messages is not None

    pending_interrupt = None
    if cs_session.has_pending_interrupt:
        for msg in reversed(messages):
            if msg.interrupt_payload is not None:
                pending_interrupt = msg.interrupt_payload
                break

    return SessionMessagesResponse(
        session=SessionResponse.from_orm_model(cs_session),
        messages=[MessageResponse.from_orm_model(m) for m in messages],
        pending_interrupt=pending_interrupt,
    )


@router.post("/sessions/{session_id}/chat/stream")
async def chat_stream(
    session_id: str,
    payload: ChatStreamRequest,
    request: Request,
    x_request_id: str | None = Header(default=None, alias="X-Request-Id"),
    user_id: int = Depends(get_client_user_id),
    chat_service: ChatService = Depends(resolve_chat_service),
) -> StreamingResponse:
    """在用户所属会话上发起新一轮客服对话并以 SSE 流式推送事件。"""
    sid = parse_uuid_or_404(session_id, "会话不存在或无权访问")
    trace_id = (x_request_id or "").strip() or uuid.uuid4().hex
    try:
        prepared = await chat_service.prepare_chat_turn(
            user_id=user_id,
            session_id=str(sid),
            message=payload.message,
            force_ticket=payload.force_ticket,
            trace_id=trace_id,
        )
    except LookupError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(exc)) from exc

    return StreamingResponse(
        chat_service.stream_turn(prepared, request=request, trace_id=trace_id),
        media_type="text/event-stream",
        headers=SSE_RESPONSE_HEADERS,
    )


@router.post("/sessions/{session_id}/resume/stream")
async def resume_stream(
    session_id: str,
    payload: ResumeStreamRequest,
    request: Request,
    x_request_id: str | None = Header(default=None, alias="X-Request-Id"),
    user_id: int = Depends(get_client_user_id),
    chat_service: ChatService = Depends(resolve_chat_service),
) -> StreamingResponse:
    """恢复待确认的下单中断流程并以 SSE 流式推送后续事件。"""
    sid = parse_uuid_or_404(session_id, "会话不存在或无权访问")
    trace_id = (x_request_id or "").strip() or uuid.uuid4().hex
    try:
        prepared = await chat_service.prepare_resume_turn(
            user_id=user_id,
            session_id=str(sid),
            action=payload.action,
            address_id=payload.address_id,
            reason=payload.reason,
        )
    except LookupError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(exc)) from exc
    except ValueError as exc:
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail=str(exc)) from exc

    return StreamingResponse(
        chat_service.stream_turn(prepared, request=request, trace_id=trace_id),
        media_type="text/event-stream",
        headers=SSE_RESPONSE_HEADERS,
    )


@router.get("/tickets", response_model=TicketPageResponse)
async def list_my_tickets(
    ticket_status: str | None = Query(default=None, alias="status"),
    page: int = Query(default=1, ge=1),
    limit: int = Query(default=20, ge=1, le=100),
    user_id: int = Depends(get_client_user_id),
    db: AsyncSession = Depends(get_request_db_session),
) -> TicketPageResponse:
    """按可选状态筛选并分页查询当前已认证用户的客服工单列表。"""
    normalized_status = normalize_optional_filter(
        ticket_status,
        VALID_TICKET_STATUSES,
        field_name="status",
        uppercase=True,
    )
    repo = TicketRepository(db)
    offset = (page - 1) * limit
    total = await repo.count_tickets_for_user(user_id=user_id, status=normalized_status)
    rows = await repo.list_tickets_for_user(
        user_id=user_id,
        status=normalized_status,
        limit=limit,
        offset=offset,
    )
    return TicketPageResponse(
        current=page,
        limit=limit,
        total=total,
        total_page=compute_total_pages(total, limit),
        items=[TicketSummaryResponse.from_orm_model(t) for t in rows],
    )


@router.get("/tickets/{ticket_id}", response_model=TicketDetailResponse)
async def get_my_ticket_detail(
    ticket_id: str,
    user_id: int = Depends(get_client_user_id),
    db: AsyncSession = Depends(get_request_db_session),
) -> TicketDetailResponse:
    """返回当前已认证用户所属工单的详情与回复时间线。"""
    tid = parse_uuid_or_404(ticket_id, "工单不存在或无权访问")
    ticket = await TicketRepository(db).get_ticket_detail_for_user(tid, user_id)
    if ticket is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="工单不存在或无权访问",
        )
    return TicketDetailResponse.from_orm_model(ticket)


@router.post("/tickets/{ticket_id}/replies", response_model=TicketDetailResponse)
async def reply_my_ticket(
    ticket_id: str,
    payload: TicketFollowupRequest,
    user_id: int = Depends(get_client_user_id),
    db: AsyncSession = Depends(get_request_db_session),
) -> TicketDetailResponse:
    """追加用户追问回复并将 OPEN/REPLIED 工单重新置为待处理状态。"""
    tid = parse_uuid_or_404(ticket_id, "工单不存在或无权访问")
    try:
        updated = await TicketRepository(db).add_user_followup(
            tid,
            user_id=user_id,
            content=payload.content,
        )
    except LookupError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(exc)) from exc
    except ValueError as exc:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(exc)) from exc
    return TicketDetailResponse.from_orm_model(updated)
