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
    """Create a SQLAlchemy async engine using the configured PostgreSQL DSN."""
    cfg = settings or get_settings()
    return create_async_engine(cfg.postgres_dsn, pool_pre_ping=True)


def get_engine() -> AsyncEngine:
    """Return a cached AsyncEngine, recreating it if settings DSN changes."""
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
    """Return the async session factory bound to the active engine."""
    get_engine()
    assert _session_factory is not None
    return _session_factory


async def get_db_session() -> AsyncIterator[AsyncSession]:
    """Yield an AsyncSession that commits on clean exit and rolls back on error."""
    factory = get_session_factory()
    async with factory() as session:
        try:
            yield session
            await session.commit()
        except Exception:
            await session.rollback()
            raise


async def dispose_engine() -> None:
    """Dispose the cached AsyncEngine and reset session factory state."""
    global _engine, _engine_dsn, _session_factory
    if _engine is not None:
        await _engine.dispose()
    _engine = None
    _engine_dsn = None
    _session_factory = None
