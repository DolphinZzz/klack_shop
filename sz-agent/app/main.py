from fastapi import APIRouter, Depends, FastAPI

from app.security.auth import (
    AdminActor,
    get_admin_actor,
    get_client_user_id,
    verify_internal_secret,
)


def create_app() -> FastAPI:
    """Create and configure the FastAPI application for sz-agent."""
    application = FastAPI(title="sz-agent", version="0.1.0")

    @application.get("/health")
    def health() -> dict[str, str]:
        return {"status": "ok"}

    client_router = APIRouter(
        prefix="/internal/cs/client",
        dependencies=[Depends(verify_internal_secret)],
    )

    @client_router.get("/ping")
    def client_ping(user_id: int = Depends(get_client_user_id)) -> dict[str, int | str]:
        return {"status": "ok", "user_id": user_id}

    admin_router = APIRouter(
        prefix="/internal/cs/admin",
        dependencies=[Depends(verify_internal_secret)],
    )

    @admin_router.get("/ping")
    def admin_ping(actor: AdminActor = Depends(get_admin_actor)) -> dict[str, int | str]:
        return {"admin_id": actor.admin_id, "admin_name": actor.admin_name}

    application.include_router(client_router)
    application.include_router(admin_router)
    return application


app = create_app()
