from __future__ import annotations

import math
from typing import Annotated, Any, Literal, NotRequired, TypedDict

from langchain_core.messages import AnyMessage
from langgraph.graph.message import add_messages
from pydantic import BaseModel, Field, field_validator

IntentType = Literal[
    "chitchat",
    "recommend_order",
    "consult_after_sales",
    "handoff",
    "fallback",
]

VALID_INTENTS: frozenset[str] = frozenset(
    {"chitchat", "recommend_order", "consult_after_sales", "handoff", "fallback"}
)

TicketCategoryType = Literal[
    "order_issue",
    "after_sale",
    "product_consult",
    "complaint",
    "other",
]

VALID_TICKET_CATEGORIES: frozenset[str] = frozenset(
    {"order_issue", "after_sale", "product_consult", "complaint", "other"}
)


class OrderDraft(TypedDict):
    """Structured pending order draft presented to the user via LangGraph interrupt."""

    draft_id: str
    product_id: int
    product_name: str
    product_image: str
    sku_id: int
    sku_name: str
    unit_price: str
    quantity: int
    freight_price: str
    total_pay_price: str
    address_id: int | None
    receiver_name: str | None
    receiver_phone: str | None
    receiver_address: str | None
    available_addresses: list[dict[str, Any]]


class RouterDecision(BaseModel):
    """Structured intent classification output produced by intent_router."""

    intent: IntentType = "fallback"
    confidence: float = Field(default=0.0, ge=0.0, le=1.0)
    reason: str = ""

    @field_validator("intent", mode="before")
    @classmethod
    def _normalize_intent(cls, value: Any) -> str:
        if isinstance(value, str):
            cleaned = value.strip()
            if cleaned in VALID_INTENTS:
                return cleaned
        return "fallback"

    @field_validator("confidence", mode="before")
    @classmethod
    def _clamp_confidence(cls, value: Any) -> float:
        if isinstance(value, bool) or value is None:
            return 0.0
        try:
            num = float(value)
        except (TypeError, ValueError):
            return 0.0
        if not math.isfinite(num):
            return 0.0
        return max(0.0, min(1.0, num))

    @field_validator("reason", mode="before")
    @classmethod
    def _normalize_reason(cls, value: Any) -> str:
        if value is None:
            return ""
        return str(value)


class HandoffSummary(BaseModel):
    """Structured support ticket summary extracted by handoff_node."""

    category: TicketCategoryType = "other"
    title: str = Field(default="人工客服工单", min_length=1, max_length=128)
    summary: str = ""
    related_order_no: str | None = None

    @field_validator("category", mode="before")
    @classmethod
    def _normalize_category(cls, value: Any) -> str:
        if isinstance(value, str):
            cleaned = value.strip()
            if cleaned in VALID_TICKET_CATEGORIES:
                return cleaned
        return "other"

    @field_validator("title", mode="before")
    @classmethod
    def _normalize_title(cls, value: Any) -> str:
        if value is None or isinstance(value, bool):
            return "人工客服工单"
        cleaned = str(value).strip()[:128].strip()
        return cleaned or "人工客服工单"

    @field_validator("summary", mode="before")
    @classmethod
    def _normalize_summary(cls, value: Any) -> str:
        if value is None:
            return ""
        return str(value).strip()

    @field_validator("related_order_no", mode="before")
    @classmethod
    def _normalize_related_order_no(cls, value: Any) -> str | None:
        if value is None or isinstance(value, bool):
            return None
        cleaned = str(value).strip()[:64].strip()
        return cleaned or None


class CustomerServiceState(TypedDict):
    """Root LangGraph state shared across the main router, nodes, and subgraphs."""

    messages: Annotated[list[AnyMessage], add_messages]
    intent: IntentType | None
    intent_confidence: float
    pending_order_draft: OrderDraft | None
    created_order: dict[str, Any] | None
    recommended_products: list[dict[str, Any]]
    created_ticket: dict[str, Any] | None
    rag_citations: list[dict[str, Any]]
    ui_events: list[dict[str, Any]]
    force_ticket: NotRequired[bool]
    order_resume_payload: NotRequired[dict[str, Any] | None]


def empty_turn_outputs() -> dict[str, Any]:
    """Return clean per-turn output defaults so prior turn cards never leak forward."""
    return {
        "recommended_products": [],
        "created_order": None,
        "created_ticket": None,
        "rag_citations": [],
        "ui_events": [],
    }
