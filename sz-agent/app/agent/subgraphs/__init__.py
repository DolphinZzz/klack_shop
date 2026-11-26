from __future__ import annotations

from collections.abc import Mapping, Sequence
import json
from typing import Any

from langchain_core.messages import AnyMessage, ToolMessage

from app.agent.llm import coerce_message_text


def slice_new_messages(
    result_messages: Sequence[AnyMessage],
    prior_count: int,
) -> list[AnyMessage]:
    """Return only the newly generated messages from an inner agent invocation."""
    if prior_count <= 0:
        return list(result_messages)
    return list(result_messages[prior_count:])


def parse_tool_message_payload(content: Any) -> dict[str, Any] | None:
    """Safely decode a ToolMessage payload dictionary from dict or JSON text."""
    if isinstance(content, Mapping):
        return dict(content)
    text = coerce_message_text(content).strip()
    if not text:
        return None
    try:
        parsed = json.loads(text)
    except (ValueError, TypeError):
        return None
    if isinstance(parsed, Mapping):
        return dict(parsed)
    return None


def collect_unique_citations(
    messages: Sequence[AnyMessage],
    tool_names: set[str],
) -> list[dict[str, Any]]:
    """Extract and deduplicate KB citations produced by *tool_names* in *messages*."""
    citations: list[dict[str, Any]] = []
    seen_keys: set[tuple[str, str, str]] = set()
    for msg in messages:
        if not isinstance(msg, ToolMessage) or msg.name not in tool_names:
            continue
        payload = parse_tool_message_payload(msg.content)
        if not payload or not payload.get("ok"):
            continue
        raw_citations = payload.get("citations")
        if not isinstance(raw_citations, list):
            continue
        for item in raw_citations:
            if not isinstance(item, Mapping):
                continue
            citation = dict(item)
            key = (
                str(citation.get("chunk_id") or ""),
                str(citation.get("doc_id") or ""),
                str(citation.get("section_path") or ""),
            )
            if key in seen_keys:
                continue
            seen_keys.add(key)
            citations.append(citation)
    return citations
