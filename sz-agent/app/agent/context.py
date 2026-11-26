from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class AgentRuntimeContext:
    """Trusted per-turn runtime context injected by the FastAPI/LangGraph runtime."""

    user_id: int
    session_id: str
    trace_id: str
