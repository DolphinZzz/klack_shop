from __future__ import annotations

from typing import Any

from langchain_core.messages import AIMessage, SystemMessage

from app.agent.llm import (
    LlmFactory,
    LlmRole,
    coerce_message_text,
    extract_conversation_messages,
)
from app.agent.state import CustomerServiceState


def build_conversational_node(
    *,
    llm_factory: LlmFactory,
    role: LlmRole,
    system_prompt: str,
):
    """构建按角色与系统提示词参数化的单轮对话 LangGraph 节点。"""

    async def _node(state: CustomerServiceState) -> dict[str, Any]:
        llm = llm_factory(role)
        history = extract_conversation_messages(state["messages"], max_turns=6)
        response = await llm.ainvoke([SystemMessage(content=system_prompt), *history])
        if not isinstance(response, AIMessage):
            response = AIMessage(
                content=coerce_message_text(getattr(response, "content", response))
            )
        return {"messages": [response]}

    _node.__name__ = f"{role}_node"
    return _node


from app.agent.nodes.chitchat import build_chitchat_node  # noqa: E402
from app.agent.nodes.fallback import build_fallback_node  # noqa: E402
from app.agent.nodes.handoff import build_handoff_node  # noqa: E402

__all__ = [
    "build_chitchat_node",
    "build_conversational_node",
    "build_fallback_node",
    "build_handoff_node",
]
