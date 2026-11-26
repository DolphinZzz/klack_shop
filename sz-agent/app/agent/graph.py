from __future__ import annotations

from collections.abc import AsyncIterator
from contextlib import asynccontextmanager
from typing import Any

from langchain_core.messages import SystemMessage
from langgraph.checkpoint.base import BaseCheckpointSaver
from langgraph.checkpoint.postgres.aio import AsyncPostgresSaver
from langgraph.graph import END, START, StateGraph
from langgraph.graph.state import CompiledStateGraph
from psycopg.rows import dict_row
from psycopg_pool import AsyncConnectionPool

from app.agent.context import AgentRuntimeContext
from app.agent.llm import (
    LlmFactory,
    create_default_llm_factory,
    extract_conversation_messages,
    invoke_structured_output,
)
from app.agent.nodes import build_chitchat_node, build_fallback_node, build_handoff_node
from app.agent.prompts import INTENT_ROUTER_SYSTEM_PROMPT
from app.agent.state import (
    CustomerServiceState,
    RouterDecision,
    VALID_INTENTS,
    empty_turn_outputs,
)
from app.agent.subgraphs.consult_after_sales import build_consult_after_sales_subgraph
from app.agent.subgraphs.recommend_order import build_recommend_order_subgraph
from app.config import get_settings

LOW_CONFIDENCE_THRESHOLD = 0.6


def to_psycopg_dsn(postgres_dsn: str) -> str:
    """Convert a SQLAlchemy asyncpg/psycopg DSN into a standard libpq PostgreSQL DSN."""
    for prefix in ("postgresql+asyncpg://", "postgresql+psycopg://", "postgres+asyncpg://"):
        if postgres_dsn.startswith(prefix):
            return "postgresql://" + postgres_dsn[len(prefix) :]
    return postgres_dsn


def build_thread_id(user_id: int, session_id: str) -> str:
    """Construct the user-isolated LangGraph thread identifier."""
    return f"u:{user_id}:s:{session_id}"


@asynccontextmanager
async def open_postgres_checkpointer(
    postgres_dsn: str,
    *,
    max_size: int = 10,
) -> AsyncIterator[AsyncPostgresSaver]:
    """Open an AsyncConnectionPool, initialize AsyncPostgresSaver tables, and yield it."""
    conninfo = to_psycopg_dsn(postgres_dsn)
    pool = AsyncConnectionPool(
        conninfo=conninfo,
        max_size=max_size,
        open=False,
        kwargs={
            "autocommit": True,
            "prepare_threshold": 0,
            "row_factory": dict_row,
        },
    )
    await pool.open()
    try:
        checkpointer = AsyncPostgresSaver(pool)
        await checkpointer.setup()
        yield checkpointer
    finally:
        await pool.close()


def _route_by_intent(state: CustomerServiceState) -> str:
    intent = state.get("intent")
    if intent in VALID_INTENTS:
        return intent
    return "fallback"


def build_customer_service_graph(
    checkpointer: BaseCheckpointSaver[Any] | None = None,
    mall_client: Any = None,
    kb_retriever: Any = None,
    ticket_repo_factory: Any = None,
    llm_factory: LlmFactory | None = None,
) -> CompiledStateGraph:
    """Compile the root KlackCraft customer service LangGraph with 5 routed branches."""
    resolved_llm_factory = llm_factory or create_default_llm_factory(get_settings())

    async def intent_router(state: CustomerServiceState) -> dict[str, Any]:
        clean_outputs = {
            **empty_turn_outputs(),
            "pending_order_draft": None,
            "order_resume_payload": None,
        }
        if state.get("force_ticket"):
            return {
                **clean_outputs,
                "intent": "handoff",
                "intent_confidence": 1.0,
            }

        recent_messages = extract_conversation_messages(state["messages"], max_turns=6)
        router_llm = resolved_llm_factory("router")
        decision = await invoke_structured_output(
            router_llm,
            RouterDecision,
            [SystemMessage(content=INTENT_ROUTER_SYSTEM_PROMPT), *recent_messages],
        )
        if decision is None:
            return {
                **clean_outputs,
                "intent": "fallback",
                "intent_confidence": 0.0,
                "force_ticket": False,
            }

        confidence = max(0.0, min(1.0, float(decision.confidence)))
        effective_intent = decision.intent
        if effective_intent not in VALID_INTENTS or confidence < LOW_CONFIDENCE_THRESHOLD:
            effective_intent = "fallback"

        return {
            **clean_outputs,
            "intent": effective_intent,
            "intent_confidence": confidence,
            "force_ticket": False,
        }

    chitchat_node = build_chitchat_node(llm_factory=resolved_llm_factory)
    handoff_node = build_handoff_node(
        llm_factory=resolved_llm_factory,
        ticket_repo_factory=ticket_repo_factory,
    )
    fallback_node = build_fallback_node(llm_factory=resolved_llm_factory)

    recommend_order_subgraph = build_recommend_order_subgraph(
        mall_client=mall_client,
        kb_retriever=kb_retriever,
        llm_factory=resolved_llm_factory,
    )
    consult_after_sales_subgraph = build_consult_after_sales_subgraph(
        mall_client=mall_client,
        kb_retriever=kb_retriever,
        llm_factory=resolved_llm_factory,
    )

    builder = StateGraph(CustomerServiceState, context_schema=AgentRuntimeContext)
    builder.add_node("intent_router", intent_router)
    builder.add_node("chitchat_node", chitchat_node)
    builder.add_node("recommend_order_subgraph", recommend_order_subgraph)
    builder.add_node("consult_after_sales_subgraph", consult_after_sales_subgraph)
    builder.add_node("handoff_node", handoff_node)
    builder.add_node("fallback_node", fallback_node)

    builder.add_edge(START, "intent_router")
    builder.add_conditional_edges(
        "intent_router",
        _route_by_intent,
        {
            "chitchat": "chitchat_node",
            "recommend_order": "recommend_order_subgraph",
            "consult_after_sales": "consult_after_sales_subgraph",
            "handoff": "handoff_node",
            "fallback": "fallback_node",
        },
    )
    builder.add_edge("chitchat_node", END)
    builder.add_edge("recommend_order_subgraph", END)
    builder.add_edge("consult_after_sales_subgraph", END)
    builder.add_edge("handoff_node", END)
    builder.add_edge("fallback_node", END)

    return builder.compile(checkpointer=checkpointer)
