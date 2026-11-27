from __future__ import annotations

from collections.abc import Mapping
from decimal import Decimal, InvalidOperation, ROUND_HALF_UP
import logging
from typing import Any

logger = logging.getLogger(__name__)

_TWO_PLACES = Decimal("0.01")


def pick_field(mapping: Mapping[str, Any], *keys: str, default: Any = None) -> Any:
    """返回 `mapping` 中按 `keys` 顺序找到的第一个非 `None` 值。"""
    for key in keys:
        if key in mapping and mapping[key] is not None:
            return mapping[key]
    return default


def to_int(value: Any, default: int = 0) -> int:
    """将 `value` 转换为整数，缺失或格式非法时回退为 `default`。"""
    if value is None or isinstance(value, bool):
        return default
    try:
        return int(value)
    except (ValueError, TypeError):
        return default


def to_optional_int(value: Any) -> int | None:
    """当 `value` 存在且合法时转换为整数，否则返回 `None`。"""
    if value is None or isinstance(value, bool):
        return None
    try:
        return int(value)
    except (ValueError, TypeError):
        return None


def parse_decimal(value: Any, default: str = "0.00") -> Decimal:
    """安全地将数值或字符串金额解析为有限的 `Decimal`。"""
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
    """使用 `ROUND_HALF_UP` 四舍五入将金额格式化为两位小数字符串。"""
    dec = parse_decimal(value, default=default).quantize(_TWO_PLACES, rounding=ROUND_HALF_UP)
    return f"{dec:.2f}"


def format_optional_decimal(value: Any) -> str | None:
    """将可选金额四舍五入格式化为两位小数字符串，缺失或非法时返回 `None`。"""
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
    """将列表查询条数限制收敛在 `[1, maximum]` 区间内。"""
    val = to_int(limit, default)
    if val < 1:
        return default
    return min(val, maximum)


def resolve_full_address(address: Mapping[str, Any]) -> str:
    """优先返回 `fullAddress`，否则拼接省、市、区与详细地址字段。"""
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
    """将 Java `AgentMallVO.UserAddress` 规范化为下划线命名（`snake_case`）的工具返回载荷。"""
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
    """将原始知识库检索命中项转换为 `(hits, citations)` 结构化字典列表。"""
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
    """执行限定分类的知识库检索，并妥善兜底空结果或检索器未就绪等情况。"""
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
    except Exception as exc:  # noqa: BLE001 - 保持工具边界容错，避免异常中断 LLM 调用
        logger.warning("KB retrieval failed for categories=%s: %s", categories, exc)
        return {
            "ok": False,
            "query": cleaned_query,
            "categories": categories,
            "hits": [],
            "citations": [],
            "found": False,
            "message": "知识库检索暂时不可用，请引导用户稍后重试或转人工客服确认。",
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
