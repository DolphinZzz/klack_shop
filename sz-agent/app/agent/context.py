from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class AgentRuntimeContext:
    """由 FastAPI/LangGraph 运行时按轮注入的可信上下文。"""

    user_id: int
    session_id: str
    trace_id: str
