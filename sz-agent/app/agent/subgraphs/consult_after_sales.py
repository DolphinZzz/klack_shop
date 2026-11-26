from __future__ import annotations

from typing import Any

from langchain.agents import create_agent
from langgraph.graph import END, START, StateGraph
from langgraph.graph.state import CompiledStateGraph
from langgraph.runtime import Runtime

from app.agent.context import AgentRuntimeContext
from app.agent.llm import LlmFactory
from app.agent.prompts import AFTER_SALES_ADVISOR_SYSTEM_PROMPT
from app.agent.state import CustomerServiceState
from app.agent.subgraphs import collect_unique_citations, slice_new_messages
from app.agent.tools.after_sales_tools import build_after_sales_tools


def build_consult_after_sales_subgraph(
    *,
    mall_client: Any,
    kb_retriever: Any,
    llm_factory: LlmFactory,
) -> CompiledStateGraph:
    """Build the order-tracking, after-sales, and policy/law RAG consultation subgraph."""
    after_sales_tools = build_after_sales_tools(mall_client, kb_retriever)
    after_sales_agent = create_agent(
        model=llm_factory("after_sales"),
        tools=after_sales_tools,
        system_prompt=AFTER_SALES_ADVISOR_SYSTEM_PROMPT,
        context_schema=AgentRuntimeContext,
    )

    async def after_sales_advisor(
        state: CustomerServiceState,
        runtime: Runtime[AgentRuntimeContext],
    ) -> dict[str, Any]:
        prior_count = len(state["messages"])
        result = await after_sales_agent.ainvoke(
            {"messages": state["messages"]},
            context=runtime.context,
        )
        new_messages = slice_new_messages(result["messages"], prior_count)
        rag_citations = collect_unique_citations(
            new_messages,
            {"search_policy_and_law_kb"},
        )
        ui_events: list[dict[str, Any]] = []
        if rag_citations:
            ui_events.append(
                {
                    "card_type": "citations",
                    "payload": {"citations": rag_citations},
                }
            )
        return {
            "messages": new_messages,
            "rag_citations": rag_citations,
            "ui_events": ui_events,
        }

    subgraph = StateGraph(CustomerServiceState, context_schema=AgentRuntimeContext)
    subgraph.add_node("after_sales_advisor", after_sales_advisor)
    subgraph.add_edge(START, "after_sales_advisor")
    subgraph.add_edge("after_sales_advisor", END)
    return subgraph.compile()
