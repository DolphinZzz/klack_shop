from __future__ import annotations

from collections.abc import Callable, Sequence
import json
import re
from typing import Any, Literal, TypeVar

from langchain_core.language_models.chat_models import BaseChatModel
from langchain_core.messages import AIMessage, AnyMessage, BaseMessage, HumanMessage
from langchain_deepseek import ChatDeepSeek
from pydantic import BaseModel

from app.config import Settings, get_settings

LlmRole = Literal[
    "router",
    "handoff_extractor",
    "chitchat",
    "shopping",
    "after_sales",
    "fallback",
]
LlmFactory = Callable[[LlmRole], BaseChatModel]
SchemaT = TypeVar("SchemaT", bound=BaseModel)

_ROLE_PRESETS: dict[LlmRole, tuple[float, bool, bool]] = {
    "router": (0.0, False, True),
    "handoff_extractor": (0.0, False, True),
    "chitchat": (0.5, True, False),
    "shopping": (0.3, True, False),
    "after_sales": (0.3, True, False),
    "fallback": (0.3, True, False),
}

_JSON_FENCE_RE = re.compile(r"^```(?:json)?\s*(.*?)\s*```$", re.DOTALL)


def build_chat_deepseek(
    settings: Settings,
    *,
    temperature: float,
    streaming: bool,
    disable_streaming: bool = False,
) -> ChatDeepSeek:
    """Instantiate a ChatDeepSeek model configured from application Settings."""
    return ChatDeepSeek(
        model=settings.deepseek_model,
        api_key=settings.deepseek_api_key,
        base_url=settings.deepseek_base_url,
        temperature=temperature,
        streaming=streaming,
        disable_streaming=disable_streaming,
    )


def create_default_llm_factory(settings: Settings | None = None) -> LlmFactory:
    """Return a role-keyed factory producing configured ChatDeepSeek instances."""
    resolved = settings or get_settings()

    def _factory(role: LlmRole) -> BaseChatModel:
        if role not in _ROLE_PRESETS:
            raise ValueError(f"Unsupported LLM role: {role!r}")
        temperature, streaming, disable_streaming = _ROLE_PRESETS[role]
        return build_chat_deepseek(
            resolved,
            temperature=temperature,
            streaming=streaming,
            disable_streaming=disable_streaming,
        )

    return _factory


def coerce_message_text(content: Any) -> str:
    """Extract plain text from string or structured-block LangChain message content."""
    if isinstance(content, str):
        return content
    if isinstance(content, list):
        parts: list[str] = []
        for block in content:
            if isinstance(block, str):
                parts.append(block)
            elif isinstance(block, dict) and block.get("type") == "text":
                text = block.get("text")
                if isinstance(text, str):
                    parts.append(text)
        return "".join(parts)
    return str(content or "")


def extract_conversation_messages(
    messages: Sequence[AnyMessage],
    *,
    max_turns: int = 6,
) -> list[BaseMessage]:
    """Extract clean recent Human/AI dialogue turns without orphaned ToolMessages."""
    cleaned: list[BaseMessage] = []
    for msg in messages:
        if isinstance(msg, HumanMessage):
            text = coerce_message_text(msg.content).strip()
            if text:
                cleaned.append(HumanMessage(content=text))
        elif isinstance(msg, AIMessage):
            text = coerce_message_text(msg.content).strip()
            if text:
                cleaned.append(AIMessage(content=text))
    limit = max(max_turns, 1) * 2
    return cleaned[-limit:]


async def invoke_structured_output(
    model: BaseChatModel,
    schema: type[SchemaT],
    messages: Sequence[BaseMessage],
) -> SchemaT:
    """Invoke *model* with Pydantic structured output, falling back to JSON parsing if needed."""
    msg_list = list(messages)
    try:
        structured = model.with_structured_output(schema, method="function_calling")
        result = await structured.ainvoke(msg_list)
    except NotImplementedError:
        raw_msg = await model.ainvoke(msg_list)
        raw_text = coerce_message_text(raw_msg.content).strip()
        match = _JSON_FENCE_RE.match(raw_text)
        if match:
            raw_text = match.group(1)
        data = json.loads(raw_text)
        return schema.model_validate(data)

    if isinstance(result, schema):
        return result
    if isinstance(result, BaseModel):
        return schema.model_validate(result.model_dump())
    return schema.model_validate(result)
