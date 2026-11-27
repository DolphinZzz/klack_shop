from __future__ import annotations

import asyncio
from dataclasses import asdict, dataclass
import threading
from typing import Any
from uuid import UUID

from pymilvus import DataType, MilvusClient

from app.config import Settings
from app.kb.chunker import KbChunk

DEFAULT_COLLECTION_NAME = "kada_cs_kb_chunks"
DEFAULT_VECTOR_DIM = 1024
DEFAULT_MIN_SCORE = 0.35

_OUTPUT_FIELDS = [
    "chunk_id",
    "doc_id",
    "doc_title",
    "category",
    "section_path",
    "page_no",
    "scope",
    "content",
]


@dataclass(slots=True)
class KbSearchHit:
    """Single retrieved knowledge base chunk with cosine similarity score."""

    chunk_id: str
    doc_id: str
    doc_title: str
    category: str
    section_path: str
    page_no: int
    scope: str
    content: str
    score: float

    def to_dict(self) -> dict[str, Any]:
        return asdict(self)


def _truncate_utf8(value: str, max_bytes: int) -> str:
    """Truncate *value* cleanly on a UTF-8 character boundary within *max_bytes*."""
    encoded = value.encode("utf-8")
    if len(encoded) <= max_bytes:
        return value
    return encoded[:max_bytes].decode("utf-8", errors="ignore")


def _escape_milvus_str(value: str) -> str:
    return value.replace("\\", "\\\\").replace('"', '\\"')


def _normalize_categories(categories: list[str] | None) -> list[str] | None:
    if categories is None:
        return None
    return [c.strip() for c in categories if c and c.strip()]


def _build_search_filter(categories: list[str] | None) -> str:
    base = 'scope == "public"'
    cleaned = _normalize_categories(categories)
    if not cleaned:
        return base
    if len(cleaned) == 1:
        return f'{base} and category == "{_escape_milvus_str(cleaned[0])}"'
    quoted = ", ".join(f'"{_escape_milvus_str(c)}"' for c in cleaned)
    return f"{base} and category in [{quoted}]"


class MilvusKbStore:
    """Milvus collection wrapper managing `kada_cs_kb_chunks` schema, upserts, and search."""

    def __init__(
        self,
        uri: str = "http://127.0.0.1:19530",
        collection_name: str = DEFAULT_COLLECTION_NAME,
        dim: int = DEFAULT_VECTOR_DIM,
        default_min_score: float = DEFAULT_MIN_SCORE,
        client: Any | None = None,
    ) -> None:
        self.uri = uri
        self.collection_name = collection_name
        self.dim = dim
        self.default_min_score = default_min_score
        self._client = client
        self._collection_ready = False
        self._init_lock = threading.Lock()

    @classmethod
    def from_settings(cls, settings: Settings, **overrides: Any) -> MilvusKbStore:
        """Instantiate MilvusKbStore from runtime application settings."""
        kwargs: dict[str, Any] = {
            "uri": settings.milvus_uri,
            "collection_name": settings.milvus_collection,
            "dim": settings.qwen_embedding_dimensions,
        }
        kwargs.update(overrides)
        return cls(**kwargs)

    def _get_client(self) -> Any:
        if self._client is None:
            self._client = MilvusClient(uri=self.uri)
        return self._client

    def _ensure_collection_sync(self) -> None:
        with self._init_lock:
            if self._collection_ready:
                return
            client = self._get_client()
            if client.has_collection(self.collection_name):
                client.load_collection(self.collection_name)
                self._collection_ready = True
                return

            schema = client.create_schema(auto_id=False, enable_dynamic_field=False)
            schema.add_field("chunk_id", DataType.VARCHAR, max_length=64, is_primary=True, auto_id=False)
            schema.add_field("doc_id", DataType.VARCHAR, max_length=36)
            schema.add_field("doc_title", DataType.VARCHAR, max_length=256)
            schema.add_field("category", DataType.VARCHAR, max_length=32)
            schema.add_field("section_path", DataType.VARCHAR, max_length=512)
            schema.add_field("page_no", DataType.INT32)
            schema.add_field("scope", DataType.VARCHAR, max_length=32)
            schema.add_field("content", DataType.VARCHAR, max_length=4096)
            schema.add_field("embedding", DataType.FLOAT_VECTOR, dim=self.dim)

            index_params = client.prepare_index_params()
            index_params.add_index(
                field_name="embedding",
                index_type="HNSW",
                metric_type="COSINE",
                params={"M": 16, "efConstruction": 200},
            )
            client.create_collection(
                collection_name=self.collection_name,
                schema=schema,
                index_params=index_params,
            )
            self._collection_ready = True

    async def ensure_collection(self) -> None:
        """Ensure the Milvus collection and HNSW cosine index exist and are loaded."""
        await asyncio.to_thread(self._ensure_collection_sync)

    def _upsert_chunks_sync(self, chunks: list[KbChunk], vectors: list[list[float]]) -> int:
        if not self._collection_ready:
            self._ensure_collection_sync()
        rows: list[dict[str, Any]] = []
        for chunk, vec in zip(chunks, vectors, strict=True):
            if len(vec) != self.dim:
                raise ValueError(f"Expected vector dimension {self.dim}, got {len(vec)}")
            rows.append(
                {
                    "chunk_id": _truncate_utf8(chunk.chunk_id, 64),
                    "doc_id": _truncate_utf8(chunk.doc_id, 36),
                    "doc_title": _truncate_utf8(chunk.doc_title, 256),
                    "category": _truncate_utf8(chunk.category, 32),
                    "section_path": _truncate_utf8(chunk.section_path, 512),
                    "page_no": int(chunk.page_no),
                    "scope": _truncate_utf8(chunk.scope, 32),
                    "content": _truncate_utf8(chunk.content, 4096),
                    "embedding": [float(v) for v in vec],
                }
            )
        self._get_client().upsert(collection_name=self.collection_name, data=rows)
        return len(rows)

    async def upsert_chunks(self, chunks: list[KbChunk], vectors: list[list[float]]) -> int:
        """Upsert chunk payloads and 1024-d vectors into Milvus."""
        if len(chunks) != len(vectors):
            raise ValueError(
                f"Chunk count ({len(chunks)}) does not match vector count ({len(vectors)})"
            )
        if not chunks:
            return 0
        return await asyncio.to_thread(self._upsert_chunks_sync, chunks, vectors)

    def _delete_by_doc_id_sync(self, doc_id: str) -> None:
        client = self._get_client()
        if not client.has_collection(self.collection_name):
            return
        escaped = _escape_milvus_str(doc_id)
        client.delete(collection_name=self.collection_name, filter=f'doc_id == "{escaped}"')

    async def delete_by_doc_id(self, doc_id: str | UUID) -> None:
        """Delete all vector chunks belonging to *doc_id*."""
        await asyncio.to_thread(self._delete_by_doc_id_sync, str(doc_id))

    def _search_sync(
        self,
        query_vector: list[float],
        categories: list[str] | None,
        top_k: int,
        min_score: float,
    ) -> list[KbSearchHit]:
        if not self._collection_ready:
            self._ensure_collection_sync()
        filter_expr = _build_search_filter(categories)
        results = self._get_client().search(
            collection_name=self.collection_name,
            data=[[float(v) for v in query_vector]],
            filter=filter_expr,
            limit=top_k,
            output_fields=_OUTPUT_FIELDS,
            search_params={"metric_type": "COSINE", "params": {"ef": max(64, top_k * 10)}},
        )
        raw_hits = results[0] if results else []
        hits: list[KbSearchHit] = []
        for raw in raw_hits:
            score = float(raw.get("distance", 0.0))
            if score < min_score:
                continue
            entity = raw.get("entity") if isinstance(raw.get("entity"), dict) else raw
            scope = str(entity.get("scope", "public"))
            if scope != "public":
                continue
            hits.append(
                KbSearchHit(
                    chunk_id=str(entity.get("chunk_id") or raw.get("chunk_id") or ""),
                    doc_id=str(entity.get("doc_id") or ""),
                    doc_title=str(entity.get("doc_title") or ""),
                    category=str(entity.get("category") or ""),
                    section_path=str(entity.get("section_path") or ""),
                    page_no=int(entity.get("page_no") or 0),
                    scope=scope,
                    content=str(entity.get("content") or ""),
                    score=score,
                )
            )
        hits.sort(key=lambda item: item.score, reverse=True)
        return hits[:top_k]

    async def search(
        self,
        query_vector: list[float],
        categories: list[str] | None = None,
        top_k: int = 5,
        min_score: float | None = None,
    ) -> list[KbSearchHit]:
        """Search public KB chunks filtered by optional categories and minimum cosine score."""
        normalized_categories = _normalize_categories(categories)
        if normalized_categories is not None and len(normalized_categories) == 0:
            return []
        if top_k < 1:
            raise ValueError("top_k must be >= 1")
        if len(query_vector) != self.dim:
            raise ValueError(f"Expected query vector dimension {self.dim}, got {len(query_vector)}")
        threshold = self.default_min_score if min_score is None else min_score
        return await asyncio.to_thread(
            self._search_sync, query_vector, normalized_categories, top_k, threshold
        )


class KbRetriever:
    """High-level async retriever combining Qwen embeddings with MilvusKbStore."""

    def __init__(self, embeddings: Any, milvus_store: MilvusKbStore) -> None:
        self.embeddings = embeddings
        self.milvus_store = milvus_store

    async def retrieve(
        self,
        query: str,
        categories: list[str] | None = None,
        top_k: int = 5,
        min_score: float | None = None,
    ) -> list[KbSearchHit]:
        """Embed *query* and return filtered KB chunks above the similarity threshold."""
        if not query or not query.strip():
            return []
        if hasattr(self.embeddings, "aembed_query"):
            query_vector = await self.embeddings.aembed_query(query)
        else:
            query_vector = await asyncio.to_thread(self.embeddings.embed_query, query)
        return await self.milvus_store.search(
            query_vector=query_vector,
            categories=categories,
            top_k=top_k,
            min_score=min_score,
        )
