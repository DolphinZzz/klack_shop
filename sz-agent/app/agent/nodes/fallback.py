from __future__ import annotations

from app.agent.llm import LlmFactory
from app.agent.nodes import build_conversational_node
from app.agent.prompts import FALLBACK_SYSTEM_PROMPT


def build_fallback_node(*, llm_factory: LlmFactory):
    """Build the fallback clarification node listing the 4 core customer service capabilities."""
    return build_conversational_node(
        llm_factory=llm_factory,
        role="fallback",
        system_prompt=FALLBACK_SYSTEM_PROMPT,
    )
