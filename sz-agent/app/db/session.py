from collections.abc import AsyncIterator

from sqlalchemy.ext.asyncio import (
    AsyncEngine,
    AsyncSession,
    async_sessionmaker,
    create_async_engine,
)

from app.config import Settings, get_settings

_engine: AsyncEngine | None = None
_engine_dsn: str | None = None
_session_factory: async_sessionmaker[AsyncSession] | None = None


def create_engine_from_settings(settings: Settings | None = None) -> AsyncEngine:
    """使用已配置的 PostgreSQL DSN 创建 SQLAlchemy 异步引擎。"""
    cfg = settings or get_settings()
    return create_async_engine(cfg.postgres_dsn, pool_pre_ping=True)


def get_engine() -> AsyncEngine:
    """返回缓存的 AsyncEngine，若配置中的 DSN 变更则重新创建。"""
    global _engine, _engine_dsn, _session_factory
    dsn = get_settings().postgres_dsn
    if _engine is None or _engine_dsn != dsn:
        _engine = create_async_engine(dsn, pool_pre_ping=True)
        _engine_dsn = dsn
        _session_factory = async_sessionmaker(
            _engine,
            class_=AsyncSession,
            expire_on_commit=False,
            autoflush=False,
        )
    return _engine


def get_session_factory() -> async_sessionmaker[AsyncSession]:
    """返回绑定至当前活跃引擎的异步会话工厂。"""
    get_engine()
    assert _session_factory is not None
    return _session_factory


async def get_db_session() -> AsyncIterator[AsyncSession]:
    """生成 AsyncSession，正常退出时自动提交，发生异常时回滚。"""
    factory = get_session_factory()
    async with factory() as session:
        try:
            yield session
            await session.commit()
        except Exception:
            await session.rollback()
            raise


async def dispose_engine() -> None:
    """销毁缓存的 AsyncEngine 并重置会话工厂状态。"""
    global _engine, _engine_dsn, _session_factory
    if _engine is not None:
        await _engine.dispose()
    _engine = None
    _engine_dsn = None
    _session_factory = None
