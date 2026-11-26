import hashlib
import json
from pathlib import Path
from types import SimpleNamespace
import uuid

import httpx
import pytest
from pypdf import PdfWriter


def _write_blank_pdf(path: Path, pages: int) -> None:
    writer = PdfWriter()
    for _ in range(pages):
        writer.add_blank_page(width=200, height=200)
    with path.open("wb") as f:
        writer.write(f)


def test_chunk_markdown_preserves_law_articles_and_section_path():
    from app.kb.chunker import chunk_markdown

    md = (
        "# 第三章 经营者的义务\n\n## 第二十五条\n"
        + ("经营者采用网络方式销售商品，消费者有权自收到商品之日起七日内退货。" * 6)
        + "\n\n## 第二十六条\n格式条款规定。"
    )
    chunks = chunk_markdown(
        md,
        doc_id="d1",
        doc_title="消保法",
        category="consumer_law",
        min_chars=120,
        max_chars=260,
        overlap=40,
    )
    assert len(chunks) >= 2
    assert all(c.section_path.startswith("第三章 经营者的义务") for c in chunks)
    assert any("第二十六条" in c.content for c in chunks)
    assert [c.chunk_id for c in chunks] == [f"d1:{i}" for i in range(len(chunks))]
    assert all(c.scope == "public" and c.page_no == 0 for c in chunks)


def test_chunk_markdown_merges_short_articles_splits_long_text_and_preserves_tables():
    from app.kb.chunker import chunk_markdown

    short_articles = "\n\n".join(
        [
            "# 第二章 消费者权利",
            "第七条 消费者在购买商品时享有人身财产安全不受损害的权利。",
            "第八条 消费者享有知悉其购买商品真实情况的权利。",
            "第九条 消费者享有自主选择商品或者服务的权利。",
            "- 2 -",
            "第 2 页 / 共 10 页",
            "第十条 消费者享有公平交易的权利，有权拒绝强制交易行为。",
        ]
    )
    merged = chunk_markdown(
        short_articles,
        doc_id="d2",
        doc_title="消保法",
        category="consumer_law",
        min_chars=120,
        max_chars=260,
        overlap=40,
    )
    assert len(merged) >= 1
    assert all("- 2 -" not in c.content and "第 2 页 / 共 10 页" not in c.content for c in merged)
    assert all(c.section_path.startswith("第二章 消费者权利") for c in merged)

    long_article = (
        "# 第三章 细则\n\n## 第三十条\n"
        + "".join(f"第{i}款规定经营者应当如实告知售后服务、运费及退换货条件。" for i in range(1, 25))
    )
    split_chunks = chunk_markdown(
        long_article,
        doc_id="d3",
        doc_title="实施条例",
        category="consumer_law",
        min_chars=120,
        max_chars=220,
        overlap=40,
    )
    assert len(split_chunks) >= 3
    assert all(len(c.content) <= 220 for c in split_chunks)
    # Consecutive splits from the same long block share overlapping text
    tail_probe = split_chunks[0].content[-20:]
    assert tail_probe in split_chunks[1].content

    table_rows = "\n".join(f"| 轴体{i} | 线性 | {35 + i}g |" for i in range(1, 16))
    table_md = (
        "# 选购指南\n\n## 轴体参数表\n\n"
        "| 型号 | 类型 | 触发压力 |\n"
        "| --- | --- | --- |\n"
        f"{table_rows}\n"
    )
    table_chunks = chunk_markdown(
        table_md,
        doc_id="d4",
        doc_title="轴体指南",
        category="product_guide",
        min_chars=100,
        max_chars=240,
        overlap=30,
    )
    assert len(table_chunks) >= 2
    assert all("| 型号 | 类型 | 触发压力 |" in c.content for c in table_chunks)


@pytest.mark.asyncio
async def test_mineru_flash_fallback_splits_oversized_pdf(tmp_path):
    from app.kb.mineru_parser import MinerUParser

    pdf_path = tmp_path / "big.pdf"
    _write_blank_pdf(pdf_path, pages=25)
    calls = []
    parser = MinerUParser(
        mode="cloud_api",
        api_token="",
        flash_max_pages=10,
        flash_runner=lambda p: (calls.append(p.name) or f"# part {len(calls)}"),
    )
    md = await parser.parse_pdf(pdf_path)
    assert len(calls) == 3 and "part 1" in md and "part 3" in md


@pytest.mark.asyncio
async def test_mineru_parser_modes_and_markdown_cache(tmp_path, monkeypatch):
    from app.kb.mineru_parser import MinerUParser

    pdf_path = tmp_path / "sample.pdf"
    _write_blank_pdf(pdf_path, pages=2)

    # 1. cloud_api precision mode via mineru.MinerU
    seen_calls = []

    class FakeMinerUClient:
        def __init__(self, token=None, base_url=None, flash_base_url=None):
            seen_calls.append(("init", token, base_url, flash_base_url))

        def extract(self, source, **kwargs):
            seen_calls.append(("extract", source))
            return SimpleNamespace(state="done", markdown="# 精确解析正文", error=None, err_code="")

        def close(self):
            seen_calls.append(("close",))

    monkeypatch.setattr("app.kb.mineru_parser.MinerU", FakeMinerUClient)
    precision_parser = MinerUParser(
        mode="cloud_api",
        api_token="token-xyz",
        storage_dir=tmp_path,
    )
    precision_md = await precision_parser.parse_pdf(pdf_path, doc_id="doc-100")
    assert precision_md == "# 精确解析正文"
    assert ("init", "token-xyz", "https://mineru.net/api/v4", None) in seen_calls
    assert ("close",) in seen_calls
    cached_file = tmp_path / "markdown" / "doc-100.md"
    assert cached_file.exists() and cached_file.read_text(encoding="utf-8") == "# 精确解析正文"

    # 2. local_http mode
    def _mock_transport(request: httpx.Request) -> httpx.Response:
        assert request.url.path.endswith("/file_parse")
        assert request.method == "POST"
        return httpx.Response(
            200,
            json={"results": {"sample": {"md_content": "# 本地 HTTP Markdown"}}},
        )

    async with httpx.AsyncClient(transport=httpx.MockTransport(_mock_transport)) as http_client:
        http_parser = MinerUParser(
            mode="local_http",
            base_url="http://127.0.0.1:18000",
            http_client=http_client,
        )
        http_md = await http_parser.parse_pdf(pdf_path)
    assert http_md == "# 本地 HTTP Markdown"

    # 3. local_cli mode
    cli_commands = []

    async def fake_cli_runner(cmd: list[str]) -> tuple[int, str, str]:
        cli_commands.append(cmd)
        out_dir = Path(cmd[cmd.index("-o") + 1])
        nested = out_dir / "sample" / "auto"
        nested.mkdir(parents=True, exist_ok=True)
        (nested / "sample.md").write_text("# 本地 CLI Markdown", encoding="utf-8")
        return 0, "ok", ""

    cli_parser = MinerUParser(mode="local_cli", cli_command="mineru", cli_runner=fake_cli_runner)
    cli_md = await cli_parser.parse_pdf(pdf_path)
    assert cli_md == "# 本地 CLI Markdown"
    assert cli_commands[0][0] == "mineru" and "-p" in cli_commands[0] and "-o" in cli_commands[0]

    with pytest.raises(ValueError, match="Unsupported MinerU mode"):
        MinerUParser(mode="unknown_mode")


def test_get_qwen_embeddings_configures_dashscope_and_dimensions():
    from app.config import Settings
    from app.kb.embeddings import get_qwen_embeddings

    settings = Settings(
        dashscope_api_key="sk-dashscope-test",
        dashscope_base_url="https://dashscope.aliyuncs.com/compatible-mode/v1",
        qwen_embedding_model="text-embedding-v4",
        qwen_embedding_dimensions=1024,
    )
    emb = get_qwen_embeddings(settings)
    assert emb.model == "text-embedding-v4"
    assert emb.dimensions == 1024
    assert emb.check_embedding_ctx_length is False
    assert emb.openai_api_base == "https://dashscope.aliyuncs.com/compatible-mode/v1"
    assert emb.model_kwargs.get("encoding_format") == "float"


@pytest.mark.asyncio
async def test_milvus_store_schema_index_scope_filter_and_retriever():
    from app.kb.chunker import KbChunk
    from app.kb.milvus_store import KbRetriever, MilvusKbStore

    class FakeMilvusClient:
        def __init__(self):
            self.collections = {}
            self.upserted = []
            self.deleted = []
            self.search_calls = []

        @staticmethod
        def create_schema(**kwargs):
            from pymilvus import MilvusClient

            return MilvusClient.create_schema(**kwargs)

        @staticmethod
        def prepare_index_params(**kwargs):
            from pymilvus import MilvusClient

            return MilvusClient.prepare_index_params(**kwargs)

        def has_collection(self, collection_name: str) -> bool:
            return collection_name in self.collections

        def create_collection(self, collection_name: str, schema, index_params, **kwargs):
            self.collections[collection_name] = {"schema": schema, "index_params": index_params}

        def load_collection(self, collection_name: str, **kwargs):
            pass

        def upsert(self, collection_name: str, data: list[dict], **kwargs):
            self.upserted.append((collection_name, data))
            return {"upsert_count": len(data)}

        def delete(self, collection_name: str, filter: str, **kwargs):
            self.deleted.append((collection_name, filter))
            return {"delete_count": 1}

        def search(self, collection_name: str, data, filter: str, limit: int, output_fields, search_params, **kwargs):
            self.search_calls.append(
                {
                    "collection_name": collection_name,
                    "filter": filter,
                    "limit": limit,
                    "output_fields": output_fields,
                    "search_params": search_params,
                }
            )
            return [
                [
                    {
                        "chunk_id": "d1:0",
                        "distance": 0.88,
                        "entity": {
                            "chunk_id": "d1:0",
                            "doc_id": "d1",
                            "doc_title": "消保法",
                            "category": "consumer_law",
                            "section_path": "第三章 > 第二十五条",
                            "page_no": 2,
                            "scope": "public",
                            "content": "七日内退货",
                        },
                    },
                    {
                        "chunk_id": "d1:1",
                        "distance": 0.21,
                        "entity": {
                            "chunk_id": "d1:1",
                            "doc_id": "d1",
                            "doc_title": "消保法",
                            "category": "consumer_law",
                            "section_path": "第三章 > 第二十六条",
                            "page_no": 3,
                            "scope": "public",
                            "content": "弱相关片段",
                        },
                    },
                ]
            ]

    fake_client = FakeMilvusClient()
    store = MilvusKbStore(uri="http://127.0.0.1:19530", collection_name="kada_cs_kb_chunks", client=fake_client)
    await store.ensure_collection()

    created = fake_client.collections["kada_cs_kb_chunks"]
    field_map = {f.name: f for f in created["schema"].fields}
    assert list(field_map.keys()) == [
        "chunk_id",
        "doc_id",
        "doc_title",
        "category",
        "section_path",
        "page_no",
        "scope",
        "content",
        "embedding",
    ]
    assert field_map["chunk_id"].is_primary and not field_map["chunk_id"].auto_id
    assert field_map["chunk_id"].params["max_length"] == 64
    assert field_map["doc_id"].params["max_length"] == 36
    assert field_map["doc_title"].params["max_length"] == 256
    assert field_map["category"].params["max_length"] == 32
    assert field_map["section_path"].params["max_length"] == 512
    assert int(field_map["page_no"].dtype) == 4  # DataType.INT32
    assert field_map["scope"].params["max_length"] == 32
    assert field_map["content"].params["max_length"] == 4096
    assert field_map["embedding"].params["dim"] == 1024

    idx_cfg = created["index_params"][0].get_index_configs()
    assert idx_cfg["index_type"] == "HNSW"
    assert idx_cfg["metric_type"] == "COSINE"
    assert idx_cfg["M"] == 16 and idx_cfg["efConstruction"] == 200

    chunk = KbChunk(
        chunk_id="d1:0",
        doc_id="d1",
        doc_title="消保法",
        category="consumer_law",
        section_path="第三章 > 第二十五条",
        page_no=0,
        scope="public",
        content="七日内退货",
        chunk_index=0,
    )
    await store.upsert_chunks([chunk], [[0.1] * 1024])
    assert len(fake_client.upserted) == 1 and fake_client.upserted[0][1][0]["chunk_id"] == "d1:0"

    await store.delete_by_doc_id("d1")
    assert fake_client.deleted[-1] == ("kada_cs_kb_chunks", 'doc_id == "d1"')

    class FakeEmbed:
        async def aembed_query(self, text: str) -> list[float]:
            return [0.5] * 1024

    retriever = KbRetriever(FakeEmbed(), store)
    hits = await retriever.retrieve("七天无理由退货", categories=["consumer_law", "mall_policy"], top_k=5)
    assert len(hits) == 1
    assert hits[0].chunk_id == "d1:0" and hits[0].score == pytest.approx(0.88)
    sent_filter = fake_client.search_calls[-1]["filter"]
    assert 'scope == "public"' in sent_filter
    assert '"consumer_law"' in sent_filter and '"mall_policy"' in sent_filter


@pytest.mark.asyncio
async def test_ingestion_deletes_old_vectors_before_upsert_and_handles_failure(async_db_session, tmp_path):
    from app.kb.ingestion import KbIngestionService
    from app.repositories.kb_repo import KbRepository

    pdf_path = tmp_path / "law.pdf"
    pdf_path.write_bytes(b"%PDF-1.4 sample")
    repo = KbRepository(async_db_session)
    doc = await repo.create_document(
        title="消保法",
        category="consumer_law",
        file_name="law.pdf",
        file_path=str(pdf_path),
        file_sha256="abc123",
        file_size=15,
        mineru_mode="cloud_api",
        uploaded_by=1,
    )

    events = []

    class FakeParser:
        async def parse_pdf(self, path):
            return "# 第一条\n保护消费者合法权益。"

    class FakeEmbed:
        async def aembed_documents(self, texts):
            return [[0.1] * 1024 for _ in texts]

    class FakeStore:
        async def delete_by_doc_id(self, doc_id):
            events.append(("del", str(doc_id)))

        async def upsert_chunks(self, chunks, vectors):
            events.append(("upsert", len(chunks), len(vectors[0])))

    svc = KbIngestionService(
        session=async_db_session,
        parser=FakeParser(),
        embeddings=FakeEmbed(),
        store=FakeStore(),
        storage_dir=tmp_path,
    )
    await svc.ingest_document(doc.id)
    updated = await repo.get_document(doc.id)
    assert updated.status == "INDEXED" and updated.chunk_count >= 1
    assert events[0][0] == "del" and events[1][0] == "upsert" and events[1][2] == 1024
    assert (tmp_path / "markdown" / f"{doc.id}.md").exists()

    class BrokenParser:
        async def parse_pdf(self, path):
            raise RuntimeError("MinerU timeout")

    svc_fail = KbIngestionService(
        session=async_db_session,
        parser=BrokenParser(),
        embeddings=FakeEmbed(),
        store=FakeStore(),
        storage_dir=tmp_path,
    )
    await svc_fail.ingest_document(doc.id)
    failed = await repo.get_document(doc.id)
    assert failed.status == "FAILED" and "MinerU timeout" in (failed.error_msg or "")


@pytest.mark.asyncio
async def test_ingestion_delete_document_orders_milvus_before_pg(async_db_session, tmp_path):
    from app.kb.ingestion import KbIngestionService
    from app.repositories.kb_repo import KbRepository

    repo = KbRepository(async_db_session)
    doc = await repo.create_document(
        title="商城规范",
        category="mall_policy",
        file_name="policy.pdf",
        file_path=str(tmp_path / "policy.pdf"),
        file_sha256="f" * 64,
        file_size=10,
        mineru_mode="cloud_api",
        uploaded_by=1,
    )
    md_cache = tmp_path / "markdown" / f"{doc.id}.md"
    md_cache.parent.mkdir(parents=True, exist_ok=True)
    md_cache.write_text("# 规范", encoding="utf-8")

    class FailingDeleteStore:
        async def delete_by_doc_id(self, doc_id):
            raise RuntimeError("Milvus down")

    svc_broken = KbIngestionService(
        session=async_db_session,
        parser=object(),
        embeddings=object(),
        store=FailingDeleteStore(),
        storage_dir=tmp_path,
    )
    with pytest.raises(RuntimeError, match="Milvus down"):
        await svc_broken.delete_document(doc.id)
    assert await repo.get_document(doc.id) is not None

    deleted_ids = []

    class WorkingDeleteStore:
        async def delete_by_doc_id(self, doc_id):
            deleted_ids.append(str(doc_id))

    svc_ok = KbIngestionService(
        session=async_db_session,
        parser=object(),
        embeddings=object(),
        store=WorkingDeleteStore(),
        storage_dir=tmp_path,
    )
    assert await svc_ok.delete_document(doc.id) is True
    assert deleted_ids == [str(doc.id)]
    assert await repo.get_document(doc.id) is None
    assert not md_cache.exists()
    assert await svc_ok.delete_document(uuid.uuid4()) is False


@pytest.mark.asyncio
async def test_seed_knowledge_docs_is_idempotent_by_sha256(async_db_session, tmp_path):
    from app.kb.ingestion import KbIngestionService
    from app.kb.seed import seed_knowledge_docs
    from app.repositories.kb_repo import KbRepository

    docs_dir = tmp_path / "knowledge_docs"
    docs_dir.mkdir()
    law_pdf = docs_dir / "01_consumer_rights_protection_law.pdf"
    policy_pdf = docs_dir / "04_klackcraft_mall_service_policy.pdf"
    law_pdf.write_bytes(b"%PDF-1.4 law")
    policy_pdf.write_bytes(b"%PDF-1.4 policy")

    parsed_files = []

    class FakeParser:
        async def parse_pdf(self, path):
            parsed_files.append(Path(path).name)
            return f"# {Path(path).stem}\n第一条 售后保障规则。"

    class FakeEmbed:
        async def aembed_documents(self, texts):
            return [[0.2] * 1024 for _ in texts]

    class FakeStore:
        async def delete_by_doc_id(self, doc_id):
            pass

        async def upsert_chunks(self, chunks, vectors):
            pass

    svc = KbIngestionService(
        session=async_db_session,
        parser=FakeParser(),
        embeddings=FakeEmbed(),
        store=FakeStore(),
        storage_dir=tmp_path,
    )
    first = await seed_knowledge_docs(async_db_session, svc, docs_dir=docs_dir, mineru_mode="cloud_api")
    assert first.created == 2 and first.skipped == 0 and first.failed == 0
    assert set(parsed_files) == {
        "01_consumer_rights_protection_law.pdf",
        "04_klackcraft_mall_service_policy.pdf",
    }

    repo = KbRepository(async_db_session)
    law_sha = hashlib.sha256(law_pdf.read_bytes()).hexdigest()
    law_doc = await repo.get_by_sha256(law_sha)
    assert law_doc is not None and law_doc.category == "consumer_law" and law_doc.status == "INDEXED"

    # Second run with same SHA-256 skips already indexed docs without re-parsing
    parsed_files.clear()
    second = await seed_knowledge_docs(async_db_session, svc, docs_dir=docs_dir, mineru_mode="cloud_api")
    assert second.created == 0 and second.skipped == 2 and parsed_files == []

    # If a seeded doc previously failed, running seed again retries it idempotently
    await repo.update_status(law_doc.id, status="FAILED", chunk_count=0, error_msg="retry")
    third = await seed_knowledge_docs(async_db_session, svc, docs_dir=docs_dir, mineru_mode="cloud_api")
    assert third.reindexed == 1 and third.skipped == 1
    assert parsed_files == ["01_consumer_rights_protection_law.pdf"]
