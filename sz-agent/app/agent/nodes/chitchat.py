from __future__ import annotations

from app.agent.llm import LlmFactory
from app.agent.nodes import build_conversational_node
from app.agent.prompts import CHITCHAT_SYSTEM_PROMPT


def build_chitchat_node(*, llm_factory: LlmFactory):
    """Build the KlackCraft peripheral/shopping advisor chitchat node."""
    return build_conversational_node(
        llm_factory=llm_factory,
        role="chitchat",
        system_prompt=CHITCHAT_SYSTEM_PROMPT,
    )
