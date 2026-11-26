from __future__ import annotations

import asyncio
import hashlib
import re
from dataclasses import dataclass
from pathlib import Path

from sqlalchemy.ext.asyncio import AsyncSession

from app.config import get_settings
from app.db.session import dispose_engine, get_session_factory
from app.kb.embeddings import get_qwen_embeddings
from app.kb.ingestion import KbIngestionService
from app.kb.milvus_store import MilvusKbStore
from app.kb.mineru_parser import MinerUParser
from app.repositories.kb_repo import KbRepository

DEFAULT_DOCS_DIR = Path(__file__).resolve().parents[2] / "knowledge_docs"

_KNOWN_DOCUMENT_META: tuple[tuple[str, str, str], ...] = (
    (
        "consumer_rights_protection_law",
        "中华人民共和国消费者权益保护法",
        "consumer_law",
    ),
    (
        "consumer_rights_implementation_regulations",
        "中华人民共和国消费者权益保护法实施条例",
        "consumer_law",
    ),
    (
        "online_7day_no_reason_return_measures",
        "网络购买商品七日无理由退货暂行办法",
        "consumer_law",
    ),
    (
        "klackcraft_mall_service_policy",
        "咔嗒造物商城发货时效、运费与售后退换货服务规范",
        "mall_policy",
    ),
)


@dataclass(slots=True)
class SeedReport:
    """Summary counters for a knowledge base directory seed run."""

    total_files: int = 0
    created: int = 0
    reindexed: int = 0
    skipped: int = 0
    failed: int = 0


def infer_document_metadata(pdf_path: Path) -> tuple[str, str]:
    """Infer `(title, category)` from known built-in PDF names or filename hints."""
    stem_lower = pdf_path.stem.lower()
    for pattern, title, category in _KNOWN_DOCUMENT_META:
        if pattern in stem_lower:
            return title, category

    if any(token in stem_lower for token in ("policy", "mall", "service")):
        category = "mall_policy"
    elif "faq" in stem_lower:
        category = "faq"
    elif any(token in stem_lower for token in ("product", "guide")):
        category = "product_guide"
    else:
        category = "consumer_law"

    clean_title = re.sub(r"^\d+[_-]+", "", pdf_path.stem).replace("_", " ").strip()
    return clean_title or pdf_path.stem, category


async def seed_knowledge_docs(
    session: AsyncSession,
    ingestion_service: KbIngestionService,
    *,
    docs_dir: Path | None = None,
    mineru_mode: str = "cloud_api",
) -> SeedReport:
    """Idempotently scan *docs_dir* for PDFs and ingest new or non-indexed documents."""
    target_dir = docs_dir or DEFAULT_DOCS_DIR
    report = SeedReport()
    if not target_dir.exists():
        return report

    pdf_files = sorted(p for p in target_dir.iterdir() if p.is_file() and p.suffix.lower() == ".pdf")
    report.total_files = len(pdf_files)
    repo = KbRepository(session)

    for pdf_path in pdf_files:
        raw_bytes = pdf_path.read_bytes()
        sha256 = hashlib.sha256(raw_bytes).hexdigest()
        existing = await repo.get_by_sha256(sha256)

        if existing is not None:
            if existing.status == "INDEXED":
                report.skipped += 1
                continue
            result = await ingestion_service.ingest_document(existing.id)
            if result.status == "INDEXED":
                report.reindexed += 1
            else:
                report.failed += 1
            continue

        title, category = infer_document_metadata(pdf_path)
        doc = await repo.create_document(
            title=title,
            category=category,
            file_name=pdf_path.name,
            file_path=str(pdf_path.resolve()),
            file_sha256=sha256,
            file_size=len(raw_bytes),
            mineru_mode=mineru_mode,
            uploaded_by=None,
        )
        result = await ingestion_service.ingest_document(doc.id)
        if result.status == "INDEXED":
            report.created += 1
        else:
            report.failed += 1

    return report


async def run_seed_cli(docs_dir: Path | None = None) -> SeedReport:
    """CLI entrypoint wiring Settings, PostgreSQL, MinerU, Qwen embeddings, and Milvus."""
    settings = get_settings()
    parser = MinerUParser.from_settings(settings)
    embeddings = get_qwen_embeddings(settings)
    store = MilvusKbStore.from_settings(settings)
    await store.ensure_collection()

    session_factory = get_session_factory()
    try:
        async with session_factory() as session:
            service = KbIngestionService(
                session=session,
                parser=parser,
                embeddings=embeddings,
                store=store,
                storage_dir=settings.kb_storage_dir,
            )
            report = await seed_knowledge_docs(
                session,
                service,
                docs_dir=docs_dir,
                mineru_mode=settings.mineru_mode,
            )
            await session.commit()
            return report
    finally:
        await dispose_engine()


if __name__ == "__main__":
    summary = asyncio.run(run_seed_cli())
    print(summary)
