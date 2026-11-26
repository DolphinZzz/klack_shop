from __future__ import annotations

from collections.abc import Mapping
from decimal import Decimal, InvalidOperation, ROUND_HALF_UP
import logging
from typing import Any

logger = logging.getLogger(__name__)

_TWO_PLACES = Decimal("0.01")


def pick_field(mapping: Mapping[str, Any], *keys: str, default: Any = None) -> Any:
    """Return the first present non-None value among *keys* in *mapping*."""
    for key in keys:
        if key in mapping and mapping[key] is not None:
            return mapping[key]
    return default


def to_int(value: Any, default: int = 0) -> int:
    """Convert *value* to int, falling back to *default* when missing or malformed."""
    if value is None or isinstance(value, bool):
        return default
    try:
        return int(value)
    except (ValueError, TypeError):
        return default


def to_optional_int(value: Any) -> int | None:
    """Convert *value* to int if present and valid, else None."""
    if value is None or isinstance(value, bool):
        return None
    try:
        return int(value)
    except (ValueError, TypeError):
        return None


def parse_decimal(value: Any, default: str = "0.00") -> Decimal:
    """Safely parse numeric or string currency values into a finite Decimal."""
    if value is None or isinstance(value, bool):
        return Decimal(default)
    try:
        dec = Decimal(str(value).strip())
    except (InvalidOperation, ValueError):
        return Decimal(default)
    if not dec.is_finite():
        return Decimal(default)
    return dec


def format_decimal(value: Any, default: str = "0.00") -> str:
    """Quantize a currency value to two decimal places using ROUND_HALF_UP."""
    dec = parse_decimal(value, default=default).quantize(_TWO_PLACES, rounding=ROUND_HALF_UP)
    return f"{dec:.2f}"


def format_optional_decimal(value: Any) -> str | None:
    """Quantize an optional currency value to two decimal places or return None."""
    if value is None or isinstance(value, bool):
        return None
    try:
        dec = Decimal(str(value).strip())
    except (InvalidOperation, ValueError):
        return None
    if not dec.is_finite():
        return None
    return f"{dec.quantize(_TWO_PLACES, rounding=ROUND_HALF_UP):.2f}"


def clamp_limit(limit: int, default: int, *, maximum: int = 20) -> int:
    """Clamp list query limits to `[1, maximum]`."""
    val = to_int(limit, default)
    if val < 1:
        return default
    return min(val, maximum)


def resolve_full_address(address: Mapping[str, Any]) -> str:
    """Return `fullAddress` or join province/city/district/detail fields."""
    explicit = pick_field(address, "fullAddress", "full_address", default="")
    if isinstance(explicit, str) and explicit.strip():
        return explicit.strip()
    parts = [
        str(address.get(k) or "").strip()
        for k in ("province", "city", "district", "detail")
        if str(address.get(k) or "").strip()
    ]
    return " ".join(parts)


def normalize_user_address(address: Mapping[str, Any]) -> dict[str, Any]:
    """Normalize Java AgentMallVO.UserAddress into snake_case tool payload."""
    return {
        "id": to_optional_int(address.get("id")),
        "real_name": str(pick_field(address, "realName", "real_name", default="") or ""),
        "phone": str(address.get("phone") or ""),
        "province": str(address.get("province") or ""),
        "city": str(address.get("city") or ""),
        "district": str(address.get("district") or ""),
        "detail": str(address.get("detail") or ""),
        "full_address": resolve_full_address(address),
        "post_code": str(pick_field(address, "postCode", "post_code", default="") or ""),
        "is_default": to_int(pick_field(address, "isDefault", "is_default"), 0),
    }


def _hit_to_dict(hit: Any) -> dict[str, Any]:
    if isinstance(hit, Mapping):
        raw = hit
    elif hasattr(hit, "to_dict") and callable(hit.to_dict):
        raw = hit.to_dict()
    else:
        raw = {
            "chunk_id": getattr(hit, "chunk_id", ""),
            "doc_id": getattr(hit, "doc_id", ""),
            "doc_title": getattr(hit, "doc_title", ""),
            "category": getattr(hit, "category", ""),
            "section_path": getattr(hit, "section_path", ""),
            "page_no": getattr(hit, "page_no", 0),
            "scope": getattr(hit, "scope", "public"),
            "content": getattr(hit, "content", ""),
            "score": getattr(hit, "score", 0.0),
        }
    return {
        "chunk_id": str(raw.get("chunk_id") or ""),
        "doc_id": str(raw.get("doc_id") or ""),
        "doc_title": str(raw.get("doc_title") or ""),
        "category": str(raw.get("category") or ""),
        "section_path": str(raw.get("section_path") or ""),
        "page_no": to_int(raw.get("page_no"), 0),
        "content": str(raw.get("content") or ""),
        "score": round(float(raw.get("score") or 0.0), 4),
    }


def format_kb_results(hits: list[Any]) -> tuple[list[dict[str, Any]], list[dict[str, Any]]]:
    """Convert raw KB hits into `(hits, citations)` structured dictionaries."""
    formatted_hits = [_hit_to_dict(hit) for hit in hits]
    citations = [
        {
            "chunk_id": item["chunk_id"],
            "doc_id": item["doc_id"],
            "doc_title": item["doc_title"],
            "category": item["category"],
            "section_path": item["section_path"],
            "page_no": item["page_no"],
            "excerpt": item["content"][:200],
            "content": item["content"],
            "score": item["score"],
        }
        for item in formatted_hits
    ]
    return formatted_hits, citations


async def search_kb(
    kb_retriever: Any,
    *,
    query: str,
    categories: list[str],
    top_k: int,
    empty_message: str,
) -> dict[str, Any]:
    """Run a category-scoped KB retrieval with graceful handling of empty or missing stores."""
    cleaned_query = (query or "").strip()
    if not cleaned_query:
        return {
            "ok": False,
            "query": query,
            "categories": categories,
            "hits": [],
            "citations": [],
            "found": False,
            "message": "请提供有效的检索问题或关键词。",
        }

    if kb_retriever is None:
        return {
            "ok": True,
            "query": cleaned_query,
            "categories": categories,
            "hits": [],
            "citations": [],
            "found": False,
            "message": "知识库检索器暂未启用，请结合常识提示用户或引导转人工工单。",
        }

    bounded_top_k = clamp_limit(top_k, default=5, maximum=10)
    try:
        raw_hits = await kb_retriever.retrieve(
            query=cleaned_query,
            categories=categories,
            top_k=bounded_top_k,
        )
    except Exception as exc:  # noqa: BLE001 - keep tool boundary resilient for LLM
        logger.warning("KB retrieval failed for categories=%s: %s", categories, exc)
        return {
            "ok": False,
            "query": cleaned_query,
            "categories": categories,
            "hits": [],
            "citations": [],
            "found": False,
            "message": f"知识库检索暂时不可用，请引导用户稍后重试或转人工客服确认（{exc}）。",
        }

    hits, citations = format_kb_results(list(raw_hits or []))
    if not hits:
        return {
            "ok": True,
            "query": cleaned_query,
            "categories": categories,
            "hits": [],
            "citations": [],
            "found": False,
            "message": empty_message,
        }

    return {
        "ok": True,
        "query": cleaned_query,
        "categories": categories,
        "hits": hits,
        "citations": citations,
        "found": True,
    }
