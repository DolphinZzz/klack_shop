from __future__ import annotations

from collections.abc import AsyncIterator
import uuid

from fastapi import HTTPException, Request, status
from sqlalchemy.ext.asyncio import AsyncSession

from app.config import Settings, get_settings
from app.db.session import get_session_factory
from app.kb.embeddings import get_qwen_embeddings
from app.kb.ingestion import KbIngestionService
from app.kb.milvus_store import MilvusKbStore
from app.kb.mineru_parser import MinerUParser
from app.repositories._common import coerce_uuid
from app.services.chat_service import ChatService

SSE_RESPONSE_HEADERS = {
    "Cache-Control": "no-cache",
    "X-Accel-Buffering": "no",
}


def resolve_settings(request: Request) -> Settings:
    """解析应用级 `app.state` 中的 Settings 覆盖项，未设置时返回环境变量缓存配置。"""
    override = getattr(request.app.state, "settings", None)
    return override if isinstance(override, Settings) else get_settings()


def resolve_session_factory(request: Request):
    """解析应用级 `async_sessionmaker` 覆盖项，未设置时返回默认 PostgreSQL 会话工厂。"""
    override = getattr(request.app.state, "session_factory", None)
    return override if override is not None else get_session_factory()


async def get_request_db_session(request: Request) -> AsyncIterator[AsyncSession]:
    """生成绑定至 `app.state.session_factory` 的 AsyncSession，并自动处理提交与异常回滚。"""
    factory = resolve_session_factory(request)
    async with factory() as session:
        try:
            yield session
            await session.commit()
        except Exception:
            await session.rollback()
            raise


def resolve_chat_service(request: Request) -> ChatService:
    """从 `app.state` 获取已配置的 ChatService，尚未初始化时抛出 503 异常。"""
    service = getattr(request.app.state, "chat_service", None)
    if service is None:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="客服编排服务尚未就绪",
        )
    return service


def build_kb_ingestion_service(
    request_or_app: Request | object,
    db_session: AsyncSession,
    *,
    require_embeddings: bool = True,
) -> KbIngestionService:
    """优先使用 `app.state` 覆盖项或默认配置构建 KbIngestionService。"""
    app = request_or_app.app if isinstance(request_or_app, Request) else request_or_app
    settings = getattr(app.state, "settings", None)
    if not isinstance(settings, Settings):
        settings = get_settings()

    storage_dir = getattr(app.state, "kb_storage_dir", None) or settings.kb_storage_dir
    store = getattr(app.state, "kb_store", None) or MilvusKbStore.from_settings(settings)
    parser = getattr(app.state, "kb_parser", None) or MinerUParser.from_settings(settings)

    embeddings = getattr(app.state, "kb_embeddings", None)
    if embeddings is None and require_embeddings:
        embeddings = get_qwen_embeddings(settings)

    return KbIngestionService(
        session=db_session,
        parser=parser,
        embeddings=embeddings,
        store=store,
        storage_dir=storage_dir,
    )


def normalize_optional_filter(
    value: str | None,
    allowed: frozenset[str] | None = None,
    *,
    field_name: str = "filter",
    uppercase: bool = False,
) -> str | None:
    """将空白字符串规范化为 None，并在给定允许集合时校验取值合法性。"""
    if value is None:
        return None
    cleaned = value.strip()
    if not cleaned:
        return None
    if uppercase:
        cleaned = cleaned.upper()
    if allowed is not None and cleaned not in allowed:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"非法的 {field_name}: {cleaned}",
        )
    return cleaned


def parse_uuid_or_404(raw_id: str, detail: str) -> uuid.UUID:
    """校验路径参数字符串是否为合法 UUID，非法时抛出 HTTP 404 异常。"""
    parsed = coerce_uuid(raw_id)
    if parsed is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=detail,
        )
    return parsed
