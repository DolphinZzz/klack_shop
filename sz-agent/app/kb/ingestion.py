from __future__ import annotations

import asyncio
import logging
from pathlib import Path
from typing import Any
from uuid import UUID

from sqlalchemy.ext.asyncio import AsyncSession

from app.db.models import KbDocument
from app.kb.chunker import chunk_markdown
from app.repositories.kb_repo import KbRepository

logger = logging.getLogger(__name__)


class KbIngestionService:
    """Orchestrates PDF parsing, markdown caching, chunking, embedding, and Milvus sync."""

    def __init__(
        self,
        session: AsyncSession,
        parser: Any,
        embeddings: Any,
        store: Any,
        storage_dir: str | Path | None = None,
        *,
        min_chars: int = 500,
        max_chars: int = 800,
        overlap: int = 90,
    ) -> None:
        self.session = session
        self.repo = KbRepository(session)
        self.parser = parser
        self.embeddings = embeddings
        self.store = store
        self.storage_dir = Path(storage_dir) if storage_dir is not None else None
        self.min_chars = min_chars
        self.max_chars = max_chars
        self.overlap = overlap

    def _markdown_cache_path(self, doc_id: UUID | str) -> Path | None:
        if self.storage_dir is None:
            return None
        return self.storage_dir / "markdown" / f"{doc_id}.md"

    async def ingest_document(
        self,
        doc_id: UUID | str,
        *,
        use_cached_markdown: bool = False,
    ) -> KbDocument:
        """Parse, chunk, embed, and atomically replace Milvus vectors for *doc_id*."""
        doc = await self.repo.get_document(doc_id)
        if doc is None:
            raise LookupError(f"KB document {doc_id} not found")

        await self.repo.update_status(doc.id, status="PARSING", error_msg=None)

        try:
            cache_path = self._markdown_cache_path(doc.id)
            if use_cached_markdown and cache_path is not None and cache_path.exists():
                markdown_text = cache_path.read_text(encoding="utf-8")
            else:
                if not doc.file_path:
                    raise FileNotFoundError(f"Document {doc.id} has no file_path configured")
                pdf_path = Path(doc.file_path)
                if not pdf_path.exists():
                    raise FileNotFoundError(f"PDF file not found: {pdf_path}")
                markdown_text = await self.parser.parse_pdf(pdf_path)
                if cache_path is not None:
                    cache_path.parent.mkdir(parents=True, exist_ok=True)
                    cache_path.write_text(markdown_text, encoding="utf-8")

            chunks = chunk_markdown(
                markdown_text,
                doc_id=str(doc.id),
                doc_title=doc.title,
                category=doc.category,
                min_chars=self.min_chars,
                max_chars=self.max_chars,
                overlap=self.overlap,
            )
            if not chunks:
                raise ValueError(f"Document {doc.id} produced zero chunks after parsing")

            texts = [c.content for c in chunks]
            if hasattr(self.embeddings, "aembed_documents"):
                vectors = await self.embeddings.aembed_documents(texts)
            else:
                vectors = await asyncio.to_thread(self.embeddings.embed_documents, texts)

            if len(vectors) != len(chunks):
                raise RuntimeError(
                    f"Embedding count ({len(vectors)}) does not match chunks ({len(chunks)})"
                )

            await self.store.delete_by_doc_id(str(doc.id))
            await self.store.upsert_chunks(chunks, vectors)

            updated = await self.repo.update_status(
                doc.id,
                status="INDEXED",
                chunk_count=len(chunks),
                error_msg=None,
            )
            assert updated is not None
            return updated
        except Exception as exc:
            logger.exception("Failed to ingest KB document %s", doc.id)
            failed = await self.repo.update_status(
                doc.id,
                status="FAILED",
                chunk_count=0,
                error_msg=str(exc)[:4000],
            )
            assert failed is not None
            return failed

    async def delete_document(self, doc_id: UUID | str) -> bool:
        """Remove Milvus vectors first, then cached markdown and the PostgreSQL record."""
        doc = await self.repo.get_document(doc_id)
        if doc is None:
            return False

        await self.store.delete_by_doc_id(str(doc.id))

        cache_path = self._markdown_cache_path(doc.id)
        if cache_path is not None and cache_path.exists():
            cache_path.unlink()

        return await self.repo.delete_document(doc.id)
