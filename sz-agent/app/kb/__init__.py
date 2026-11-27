from __future__ import annotations

from typing import TYPE_CHECKING, Any

from app.kb.chunker import KbChunk, chunk_markdown
from app.kb.embeddings import get_qwen_embeddings
from app.kb.ingestion import KbIngestionService
from app.kb.milvus_store import KbRetriever, KbSearchHit, MilvusKbStore
from app.kb.mineru_parser import MinerUParser

if TYPE_CHECKING:
    from app.kb.seed import SeedReport, infer_document_metadata, run_seed_cli, seed_knowledge_docs

_SEED_EXPORTS = frozenset(
    {"SeedReport", "infer_document_metadata", "run_seed_cli", "seed_knowledge_docs"}
)

__all__ = [
    "KbChunk",
    "KbIngestionService",
    "KbRetriever",
    "KbSearchHit",
    "MilvusKbStore",
    "MinerUParser",
    "SeedReport",
    "chunk_markdown",
    "get_qwen_embeddings",
    "infer_document_metadata",
    "run_seed_cli",
    "seed_knowledge_docs",
]


def __getattr__(name: str) -> Any:
    if name in _SEED_EXPORTS:
        from app.kb import seed as _seed

        return getattr(_seed, name)
    raise AttributeError(f"module {__name__!r} has no attribute {name!r}")
