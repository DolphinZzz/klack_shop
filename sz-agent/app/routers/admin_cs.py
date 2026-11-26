from __future__ import annotations

import asyncio
import hashlib
import logging
from pathlib import Path
import re
import uuid

from fastapi import (
    APIRouter,
    BackgroundTasks,
    Body,
    Depends,
    File,
    Form,
    HTTPException,
    Query,
    Request,
    UploadFile,
    status,
)
from sqlalchemy.exc import IntegrityError
from sqlalchemy.ext.asyncio import AsyncSession

from app.agent.state import VALID_TICKET_CATEGORIES
from app.repositories.kb_repo import KbRepository, VALID_KB_STATUSES
from app.repositories.session_repo import SessionRepository
from app.repositories.ticket_repo import TicketRepository, VALID_TICKET_STATUSES
from app.routers import (
    build_kb_ingestion_service,
    get_request_db_session,
    normalize_optional_filter,
    resolve_session_factory,
    resolve_settings,
)
from app.schemas.cs import (
    AdminTicketCloseRequest,
    AdminTicketDetailResponse,
    AdminTicketReplyRequest,
    DeleteKbDocumentResponse,
    KbDocumentPageResponse,
    KbDocumentResponse,
    KbReindexRequest,
    TicketDetailResponse,
    TicketPageResponse,
    TicketSummaryResponse,
    VALID_KB_CATEGORIES,
    compute_total_pages,
)
from app.security.auth import AdminActor, get_admin_actor, verify_internal_secret

logger = logging.getLogger(__name__)

MAX_KB_PDF_BYTES = 50 * 1024 * 1024
_UNSAFE_FILENAME_CHARS_RE = re.compile(r"[\x00-\x1f\x7f\"'<>|?*:\\/]+")

router = APIRouter(
    prefix="/internal/cs/admin",
    dependencies=[Depends(verify_internal_secret)],
)


def _sanitize_upload_filename(raw_name: str | None) -> str:
    if not raw_name or not raw_name.strip():
        return "upload.pdf"
    base = raw_name.replace("\\", "/").rsplit("/", 1)[-1].strip()
    cleaned = _UNSAFE_FILENAME_CHARS_RE.sub("_", base).strip(". ")
    if not cleaned:
        return "upload.pdf"
    if not cleaned.lower().endswith(".pdf"):
        cleaned = f"{cleaned}.pdf"
    return cleaned[:180]


async def _run_kb_ingestion_task(
    app: object,
    doc_id: uuid.UUID,
    *,
    use_cached_markdown: bool = False,
) -> None:
    session_factory = getattr(app.state, "session_factory", None)
    if session_factory is None:
        from app.db.session import get_session_factory

        session_factory = get_session_factory()

    async with session_factory() as bg_session:
        try:
            service = build_kb_ingestion_service(app, bg_session, require_embeddings=True)
            await service.ingest_document(doc_id, use_cached_markdown=use_cached_markdown)
            await bg_session.commit()
        except Exception:
            await bg_session.rollback()
            logger.exception("Background KB ingestion task failed for doc %s", doc_id)


@router.get("/ping")
def admin_ping(actor: AdminActor = Depends(get_admin_actor)) -> dict[str, int | str]:
    """Health/auth verification route for internal admin requests."""
    return {"admin_id": actor.admin_id, "admin_name": actor.admin_name}


@router.get("/tickets", response_model=TicketPageResponse)
async def list_admin_tickets(
    ticket_status: str | None = Query(default=None, alias="status"),
    category: str | None = Query(default=None),
    related_order_no: str | None = Query(default=None),
    related_order_no_camel: str | None = Query(default=None, alias="relatedOrderNo"),
    keyword: str | None = Query(default=None),
    page: int = Query(default=1, ge=1),
    limit: int = Query(default=20, ge=1, le=100),
    _actor: AdminActor = Depends(get_admin_actor),
    db: AsyncSession = Depends(get_request_db_session),
) -> TicketPageResponse:
    """List support tickets for the admin console with filtering and pagination."""
    normalized_status = normalize_optional_filter(
        ticket_status,
        VALID_TICKET_STATUSES,
        field_name="status",
        uppercase=True,
    )
    normalized_category = normalize_optional_filter(
        category,
        VALID_TICKET_CATEGORIES,
        field_name="category",
    )
    normalized_order_no = normalize_optional_filter(
        related_order_no or related_order_no_camel
    )
    normalized_keyword = normalize_optional_filter(keyword)

    repo = TicketRepository(db)
    offset = (page - 1) * limit
    total = await repo.count_tickets_for_admin(
        status=normalized_status,
        category=normalized_category,
        related_order_no=normalized_order_no,
        keyword=normalized_keyword,
    )
    rows = await repo.list_tickets_for_admin(
        status=normalized_status,
        category=normalized_category,
        related_order_no=normalized_order_no,
        keyword=normalized_keyword,
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


@router.get("/tickets/{ticket_id}", response_model=AdminTicketDetailResponse)
async def get_admin_ticket_detail(
    ticket_id: str,
    _actor: AdminActor = Depends(get_admin_actor),
    db: AsyncSession = Depends(get_request_db_session),
) -> AdminTicketDetailResponse:
    """Return full ticket detail, reply timeline, and linked session message replay."""
    ticket = await TicketRepository(db).get_ticket_detail_for_admin(ticket_id)
    if ticket is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="工单不存在")

    session_messages = []
    if ticket.session_id is not None:
        session_messages = await SessionRepository(db).list_messages_for_admin(ticket.session_id)
    return AdminTicketDetailResponse.from_orm_with_messages(ticket, session_messages)


@router.post("/tickets/{ticket_id}/reply", response_model=TicketDetailResponse)
async def reply_admin_ticket(
    ticket_id: str,
    payload: AdminTicketReplyRequest,
    actor: AdminActor = Depends(get_admin_actor),
    db: AsyncSession = Depends(get_request_db_session),
) -> TicketDetailResponse:
    """Add an admin reply, update ticket status, and sync a system note to the linked session."""
    admin_name = actor.admin_name or f"管理员{actor.admin_id}"
    try:
        updated = await TicketRepository(db).add_admin_reply(
            ticket_id,
            admin_id=actor.admin_id,
            admin_name=admin_name,
            content=payload.content,
            close_ticket=payload.close_ticket,
        )
    except LookupError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(exc)) from exc
    except ValueError as exc:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(exc)) from exc
    return TicketDetailResponse.from_orm_model(updated)


@router.post("/tickets/{ticket_id}/close", response_model=TicketDetailResponse)
async def close_admin_ticket(
    ticket_id: str,
    _payload: AdminTicketCloseRequest | None = Body(default=None),
    _actor: AdminActor = Depends(get_admin_actor),
    db: AsyncSession = Depends(get_request_db_session),
) -> TicketDetailResponse:
    """Close a support ticket."""
    try:
        closed = await TicketRepository(db).close_ticket(ticket_id)
    except LookupError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(exc)) from exc
    return TicketDetailResponse.from_orm_model(closed)


@router.get("/kb/documents", response_model=KbDocumentPageResponse)
async def list_kb_documents(
    doc_status: str | None = Query(default=None, alias="status"),
    category: str | None = Query(default=None),
    page: int = Query(default=1, ge=1),
    limit: int = Query(default=50, ge=1, le=100),
    _actor: AdminActor = Depends(get_admin_actor),
    db: AsyncSession = Depends(get_request_db_session),
) -> KbDocumentPageResponse:
    """List knowledge base PDF documents with optional status/category filters."""
    normalized_status = normalize_optional_filter(
        doc_status,
        VALID_KB_STATUSES,
        field_name="status",
        uppercase=True,
    )
    normalized_category = normalize_optional_filter(
        category,
        VALID_KB_CATEGORIES,
        field_name="category",
    )
    repo = KbRepository(db)
    offset = (page - 1) * limit
    total = await repo.count_documents(
        status=normalized_status,
        category=normalized_category,
    )
    rows = await repo.list_documents(
        status=normalized_status,
        category=normalized_category,
        limit=limit,
        offset=offset,
    )
    return KbDocumentPageResponse(
        current=page,
        limit=limit,
        total=total,
        total_page=compute_total_pages(total, limit),
        items=[KbDocumentResponse.from_orm_model(doc) for doc in rows],
    )


@router.post("/kb/documents/upload", response_model=KbDocumentResponse)
async def upload_kb_document(
    request: Request,
    background_tasks: BackgroundTasks,
    file: UploadFile = File(...),
    category: str = Form(default="mall_policy"),
    title: str | None = Form(default=None),
    actor: AdminActor = Depends(get_admin_actor),
    db: AsyncSession = Depends(get_request_db_session),
) -> KbDocumentResponse:
    """Upload a PDF document with SHA-256 deduplication and trigger async KB ingestion."""
    normalized_category = normalize_optional_filter(
        category,
        VALID_KB_CATEGORIES,
        field_name="category",
    )
    if normalized_category is None:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="文档分类不能为空")

    raw_bytes = await file.read(MAX_KB_PDF_BYTES + 1)
    if not raw_bytes:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="上传文件不能为空")
    if len(raw_bytes) > MAX_KB_PDF_BYTES:
        raise HTTPException(
            status_code=status.HTTP_413_REQUEST_ENTITY_TOO_LARGE,
            detail="PDF 文件大小不能超过 50MB",
        )

    raw_filename = file.filename or ""
    if not raw_filename.lower().endswith(".pdf") and not raw_bytes.startswith(b"%PDF"):
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="仅支持上传 PDF 格式文档",
        )

    file_sha256 = hashlib.sha256(raw_bytes).hexdigest()
    repo = KbRepository(db)
    existing = await repo.get_by_sha256(file_sha256)
    if existing is not None:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail=f"相同内容（SHA-256）的知识库文档已存在：{existing.title} ({existing.id})",
        )

    settings = resolve_settings(request)
    storage_root = Path(
        getattr(request.app.state, "kb_storage_dir", None) or settings.kb_storage_dir
    ).resolve()
    safe_filename = _sanitize_upload_filename(file.filename)
    target_path = storage_root / "pdfs" / f"{file_sha256}_{safe_filename}"

    def _write_pdf() -> None:
        target_path.parent.mkdir(parents=True, exist_ok=True)
        target_path.write_bytes(raw_bytes)

    await asyncio.to_thread(_write_pdf)

    resolved_title = (title or "").strip() or Path(safe_filename).stem or "未命名知识文档"
    try:
        doc = await repo.create_document(
            title=resolved_title[:256],
            category=normalized_category,
            file_name=safe_filename,
            file_path=str(target_path),
            file_sha256=file_sha256,
            file_size=len(raw_bytes),
            mineru_mode=settings.mineru_mode,
            uploaded_by=actor.admin_id,
            status="PENDING",
        )
        await db.commit()
    except IntegrityError as exc:
        await db.rollback()
        if target_path.exists():
            await asyncio.to_thread(target_path.unlink, missing_ok=True)
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="相同内容的知识库文档已存在",
        ) from exc

    # Ensure session_factory is resolvable even when app.state was partially configured
    if getattr(request.app.state, "session_factory", None) is None:
        request.app.state.session_factory = resolve_session_factory(request)

    background_tasks.add_task(
        _run_kb_ingestion_task,
        request.app,
        doc.id,
        use_cached_markdown=False,
    )
    return KbDocumentResponse.from_orm_model(doc)


@router.post("/kb/documents/{doc_id}/reindex", response_model=KbDocumentResponse)
async def reindex_kb_document(
    doc_id: str,
    request: Request,
    background_tasks: BackgroundTasks,
    payload: KbReindexRequest | None = Body(default=None),
    _actor: AdminActor = Depends(get_admin_actor),
    db: AsyncSession = Depends(get_request_db_session),
) -> KbDocumentResponse:
    """Re-queue an existing knowledge base document for asynchronous parsing and indexing."""
    repo = KbRepository(db)
    doc = await repo.get_document(doc_id)
    if doc is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="知识库文档不存在")

    updated = await repo.update_status(doc.id, status="PENDING", error_msg=None)
    assert updated is not None
    await db.commit()

    if getattr(request.app.state, "session_factory", None) is None:
        request.app.state.session_factory = resolve_session_factory(request)

    use_cached = payload.use_cached_markdown if payload is not None else False
    background_tasks.add_task(
        _run_kb_ingestion_task,
        request.app,
        updated.id,
        use_cached_markdown=use_cached,
    )
    return KbDocumentResponse.from_orm_model(updated)


@router.delete("/kb/documents/{doc_id}", response_model=DeleteKbDocumentResponse)
async def delete_kb_document(
    doc_id: str,
    request: Request,
    _actor: AdminActor = Depends(get_admin_actor),
    db: AsyncSession = Depends(get_request_db_session),
) -> DeleteKbDocumentResponse:
    """Delete Milvus vectors, cached markdown, PG row, and stored PDF under kb_storage_dir."""
    repo = KbRepository(db)
    doc = await repo.get_document(doc_id)
    if doc is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="知识库文档不存在")

    file_path_str = doc.file_path
    service = build_kb_ingestion_service(request, db, require_embeddings=False)
    deleted = await service.delete_document(doc.id)
    if not deleted:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="知识库文档不存在")
    await db.commit()

    if file_path_str:
        settings = resolve_settings(request)
        storage_root = Path(
            getattr(request.app.state, "kb_storage_dir", None) or settings.kb_storage_dir
        ).resolve()
        candidate = Path(file_path_str).resolve()
        if candidate.is_relative_to(storage_root) and candidate.is_file():
            await asyncio.to_thread(candidate.unlink, missing_ok=True)

    return DeleteKbDocumentResponse(deleted=True, id=str(doc.id))
