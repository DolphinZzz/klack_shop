import uuid

from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.db.base import utcnow
from app.db.models import KbDocument
from app.repositories._common import coerce_uuid

VALID_KB_STATUSES = frozenset({"PENDING", "PARSING", "INDEXED", "FAILED"})


class KbRepository:
    """知识库 PDF 元数据与入库状态的数据仓储。"""

    def __init__(self, session: AsyncSession) -> None:
        self.session = session

    async def get_by_sha256(self, file_sha256: str) -> KbDocument | None:
        """按 SHA-256 摘要查询知识库文档。"""
        stmt = select(KbDocument).where(KbDocument.file_sha256 == file_sha256)
        result = await self.session.execute(stmt)
        return result.scalar_one_or_none()

    async def create_document(
        self,
        title: str,
        category: str,
        file_name: str | None = None,
        file_path: str | None = None,
        file_sha256: str | None = None,
        file_size: int | None = None,
        mineru_mode: str | None = None,
        uploaded_by: int | None = None,
        status: str = "PENDING",
    ) -> KbDocument:
        """插入一条新的知识库文档记录。"""
        if status not in VALID_KB_STATUSES:
            raise ValueError(f"Unsupported KB document status: {status!r}")
        now = utcnow()
        doc = KbDocument(
            title=title[:256],
            category=category,
            file_name=file_name,
            file_path=file_path,
            file_sha256=file_sha256,
            file_size=file_size,
            mineru_mode=mineru_mode,
            status=status,
            chunk_count=0,
            uploaded_by=uploaded_by,
            created_at=now,
            updated_at=now,
        )
        self.session.add(doc)
        await self.session.flush()
        return doc

    async def update_status(
        self,
        doc_id: uuid.UUID | str,
        status: str,
        chunk_count: int | None = None,
        error_msg: str | None = None,
    ) -> KbDocument | None:
        """更新文档入库状态、可选分块数量及错误信息。"""
        if status not in VALID_KB_STATUSES:
            raise ValueError(f"Unsupported KB document status: {status!r}")
        doc = await self.get_document(doc_id)
        if doc is None:
            return None
        doc.status = status
        if chunk_count is not None:
            doc.chunk_count = chunk_count
        doc.error_msg = error_msg
        doc.updated_at = utcnow()
        await self.session.flush()
        return doc

    async def list_documents(
        self,
        status: str | None = None,
        category: str | None = None,
        limit: int = 50,
        offset: int = 0,
    ) -> list[KbDocument]:
        """按创建时间倒序查询知识库文档列表。"""
        stmt = select(KbDocument)
        if status is not None:
            stmt = stmt.where(KbDocument.status == status)
        if category is not None:
            stmt = stmt.where(KbDocument.category == category)
        stmt = stmt.order_by(KbDocument.created_at.desc()).offset(offset).limit(limit)
        result = await self.session.execute(stmt)
        return list(result.scalars().all())

    async def count_documents(
        self, status: str | None = None, category: str | None = None
    ) -> int:
        """统计匹配可选状态与分类筛选条件的知识库文档数量。"""
        stmt = select(func.count(KbDocument.id))
        if status is not None:
            stmt = stmt.where(KbDocument.status == status)
        if category is not None:
            stmt = stmt.where(KbDocument.category == category)
        return int((await self.session.execute(stmt)).scalar_one())

    async def get_document(self, doc_id: uuid.UUID | str) -> KbDocument | None:
        """按 ID 查询知识库文档，不存在时返回 None。"""
        did = coerce_uuid(doc_id)
        if did is None:
            return None
        stmt = select(KbDocument).where(KbDocument.id == did)
        result = await self.session.execute(stmt)
        return result.scalar_one_or_none()

    async def delete_document(self, doc_id: uuid.UUID | str) -> bool:
        """按 ID 删除知识库文档，成功删除时返回 True。"""
        doc = await self.get_document(doc_id)
        if doc is None:
            return False
        await self.session.delete(doc)
        await self.session.flush()
        return True
