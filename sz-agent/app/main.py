from __future__ import annotations

from collections.abc import AsyncIterator
from contextlib import AsyncExitStack, asynccontextmanager
import logging
from typing import Any

from fastapi import FastAPI
from langgraph.checkpoint.base import BaseCheckpointSaver
from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker

from app.agent.graph import build_customer_service_graph, open_postgres_checkpointer
from app.agent.llm import LlmFactory, create_default_llm_factory
from app.clients.mall_client import MallInternalClient
from app.config import Settings, get_settings
from app.db.session import dispose_engine, get_session_factory
from app.kb.embeddings import get_qwen_embeddings
from app.kb.milvus_store import KbRetriever, MilvusKbStore
from app.kb.mineru_parser import MinerUParser
from app.repositories.ticket_repo import TicketRepository
from app.routers.admin_cs import router as admin_router
from app.routers.client_cs import router as client_router
from app.services.chat_service import ChatService

logger = logging.getLogger(__name__)


def _build_ticket_repo_factory(session_factory: async_sessionmaker[AsyncSession]):
    @asynccontextmanager
    async def _ticket_repo_scope() -> AsyncIterator[TicketRepository]:
        async with session_factory() as session:
            try:
                yield TicketRepository(session)
                await session.commit()
            except Exception:
                await session.rollback()
                raise

    return _ticket_repo_scope


@asynccontextmanager
async def lifespan(application: FastAPI) -> AsyncIterator[None]:
    """Initialize PostgreSQL checkpointer, Milvus collection, mall client, and compiled graph."""
    settings: Settings = getattr(application.state, "settings", None) or get_settings()
    owns_engine = getattr(application.state, "session_factory", None) is None
    owns_mall_client = getattr(application.state, "mall_client", None) is None

    async with AsyncExitStack() as stack:
        session_factory = getattr(application.state, "session_factory", None)
        if session_factory is None:
            session_factory = get_session_factory()
            application.state.session_factory = session_factory

        checkpointer = getattr(application.state, "checkpointer", None)
        if checkpointer is None:
            checkpointer = await stack.enter_async_context(
                open_postgres_checkpointer(settings.postgres_dsn)
            )
            application.state.checkpointer = checkpointer

        mall_client = getattr(application.state, "mall_client", None)
        if mall_client is None:
            mall_client = MallInternalClient.from_settings(settings)
            application.state.mall_client = mall_client

        kb_store = getattr(application.state, "kb_store", None)
        if kb_store is None:
            kb_store = MilvusKbStore.from_settings(settings)
            await kb_store.ensure_collection()
            application.state.kb_store = kb_store

        kb_embeddings = getattr(application.state, "kb_embeddings", None)
        if kb_embeddings is None and settings.dashscope_api_key:
            kb_embeddings = get_qwen_embeddings(settings)
            application.state.kb_embeddings = kb_embeddings
        elif kb_embeddings is None:
            logger.warning("DASHSCOPE_API_KEY is not configured; KB embeddings/retriever disabled")

        kb_retriever = getattr(application.state, "kb_retriever", None)
        if kb_retriever is None and kb_embeddings is not None:
            kb_retriever = KbRetriever(kb_embeddings, kb_store)
            application.state.kb_retriever = kb_retriever

        if getattr(application.state, "kb_parser", None) is None:
            application.state.kb_parser = MinerUParser.from_settings(settings)

        graph = getattr(application.state, "graph", None)
        configured_llm_factory = getattr(application.state, "llm_factory", None)
        if graph is None:
            if configured_llm_factory is None and not settings.deepseek_api_key:
                logger.warning(
                    "DEEPSEEK_API_KEY is not configured; customer service graph disabled"
                )
            else:
                resolved_llm_factory = configured_llm_factory or create_default_llm_factory(
                    settings
                )
                graph = build_customer_service_graph(
                    checkpointer=checkpointer,
                    mall_client=mall_client,
                    kb_retriever=kb_retriever,
                    ticket_repo_factory=_build_ticket_repo_factory(session_factory),
                    llm_factory=resolved_llm_factory,
                )
                application.state.graph = graph

        if getattr(application.state, "chat_service", None) is None and graph is not None:
            application.state.chat_service = ChatService(
                graph=graph,
                session_factory=session_factory,
                heartbeat_interval=getattr(application.state, "sse_heartbeat_interval", 15.0),
                disconnect_check_interval=getattr(
                    application.state, "sse_disconnect_check_interval", 0.2
                ),
            )

        try:
            yield
        finally:
            if owns_mall_client and hasattr(mall_client, "aclose"):
                await mall_client.aclose()
            if owns_engine:
                await dispose_engine()


def create_app(
    *,
    settings: Settings | None = None,
    session_factory: async_sessionmaker[AsyncSession] | None = None,
    checkpointer: BaseCheckpointSaver[Any] | None = None,
    mall_client: Any = None,
    kb_store: Any = None,
    kb_embeddings: Any = None,
    kb_retriever: Any = None,
    kb_parser: Any = None,
    llm_factory: LlmFactory | None = None,
    graph: Any = None,
    chat_service: ChatService | None = None,
    sse_heartbeat_interval: float = 15.0,
    sse_disconnect_check_interval: float = 0.2,
) -> FastAPI:
    """Create and configure the FastAPI application for sz-agent."""
    application = FastAPI(title="sz-agent", version="0.1.0", lifespan=lifespan)

    if settings is not None:
        application.state.settings = settings
        application.dependency_overrides[get_settings] = lambda: settings
    if session_factory is not None:
        application.state.session_factory = session_factory
    if checkpointer is not None:
        application.state.checkpointer = checkpointer
    if mall_client is not None:
        application.state.mall_client = mall_client
    if kb_store is not None:
        application.state.kb_store = kb_store
    if kb_embeddings is not None:
        application.state.kb_embeddings = kb_embeddings
    if kb_retriever is not None:
        application.state.kb_retriever = kb_retriever
    if kb_parser is not None:
        application.state.kb_parser = kb_parser
    if llm_factory is not None:
        application.state.llm_factory = llm_factory
    application.state.sse_heartbeat_interval = sse_heartbeat_interval
    application.state.sse_disconnect_check_interval = sse_disconnect_check_interval

    # Support ASGI transport tests that bypass FastAPI lifespan startup
    if graph is not None:
        application.state.graph = graph
    elif checkpointer is not None and session_factory is not None:
        resolved_settings = settings or get_settings()
        application.state.graph = build_customer_service_graph(
            checkpointer=checkpointer,
            mall_client=mall_client,
            kb_retriever=kb_retriever,
            ticket_repo_factory=_build_ticket_repo_factory(session_factory),
            llm_factory=llm_factory or create_default_llm_factory(resolved_settings),
        )

    if chat_service is not None:
        application.state.chat_service = chat_service
    elif (
        getattr(application.state, "graph", None) is not None
        and session_factory is not None
    ):
        application.state.chat_service = ChatService(
            graph=application.state.graph,
            session_factory=session_factory,
            heartbeat_interval=sse_heartbeat_interval,
            disconnect_check_interval=sse_disconnect_check_interval,
        )

    @application.get("/health")
    def health() -> dict[str, str]:
        return {"status": "ok"}

    application.include_router(client_router)
    application.include_router(admin_router)
    return application


app = create_app()
