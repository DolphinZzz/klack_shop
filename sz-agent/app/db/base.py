from datetime import UTC, datetime

from sqlalchemy.orm import DeclarativeBase


def utcnow() -> datetime:
    """返回当前带时区信息的 UTC 时间。"""
    return datetime.now(UTC)


class Base(DeclarativeBase):
    """所有 sz-agent PostgreSQL ORM 模型的声明式基类。"""
