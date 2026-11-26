from __future__ import annotations

from typing import Annotated, Any, Literal, NotRequired, TypedDict

from langchain_core.messages import AnyMessage
from langgraph.graph.message import add_messages
from pydantic import BaseModel, Field

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

    intent: IntentType
    confidence: float = Field(ge=0.0, le=1.0)
    reason: str = ""


class HandoffSummary(BaseModel):
    """Structured support ticket summary extracted by handoff_node."""

    category: TicketCategoryType = "other"
    title: str = Field(min_length=1, max_length=128)
    summary: str = ""
    related_order_no: str | None = None


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
