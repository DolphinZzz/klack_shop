from __future__ import annotations

from app.agent.llm import LlmFactory
from app.agent.nodes import build_conversational_node
from app.agent.prompts import FALLBACK_SYSTEM_PROMPT


def build_fallback_node(*, llm_factory: LlmFactory):
    """构建列出 4 项核心客服能力的兜底澄清节点。"""
    return build_conversational_node(
        llm_factory=llm_factory,
        role="fallback",
        system_prompt=FALLBACK_SYSTEM_PROMPT,
    )
