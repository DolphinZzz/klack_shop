from fastapi.testclient import TestClient


def test_health_requires_no_secret_and_internal_routes_reject_invalid_secret(monkeypatch):
    monkeypatch.setenv("AGENT_INTERNAL_SECRET", "test-secret-xyz")
    from app.config import get_settings

    get_settings.cache_clear()
    from app.main import create_app

    client = TestClient(create_app())
    assert client.get("/health").status_code == 200
    assert client.get("/internal/cs/client/ping").status_code == 401
    assert client.get("/internal/cs/client/ping", headers={"X-Internal-Secret": "wrong"}).status_code == 401
    assert client.get("/internal/cs/client/ping", headers={"X-Internal-Secret": "test-secret-xyz"}).status_code == 401  # 缺少 X-User-Id
    res = client.get("/internal/cs/client/ping", headers={"X-Internal-Secret": "test-secret-xyz", "X-User-Id": "10086"})
    assert res.status_code == 200 and res.json()["user_id"] == 10086


def test_admin_actor_decodes_url_encoded_name(monkeypatch):
    monkeypatch.setenv("AGENT_INTERNAL_SECRET", "test-secret-xyz")
    from app.config import get_settings

    get_settings.cache_clear()
    from app.main import create_app

    client = TestClient(create_app())
    res = client.get(
        "/internal/cs/admin/ping",
        headers={
            "X-Internal-Secret": "test-secret-xyz",
            "X-Admin-Id": "1",
            "X-Admin-Name": "%E5%AE%A2%E6%9C%8D%E5%B0%8F%E5%8D%A1",
        },
    )
    assert res.status_code == 200 and res.json() == {"admin_id": 1, "admin_name": "客服小卡"}


def test_settings_defaults(monkeypatch):
    monkeypatch.delenv("APP_HOST", raising=False)
    monkeypatch.delenv("APP_PORT", raising=False)
    monkeypatch.delenv("JAVA_INTERNAL_BASE_URL", raising=False)
    monkeypatch.delenv("QWEN_EMBEDDING_DIMENSIONS", raising=False)
    monkeypatch.delenv("MINERU_MODE", raising=False)
    from app.config import get_settings

    get_settings.cache_clear()
    settings = get_settings()
    assert settings.app_host == "127.0.0.1"
    assert settings.app_port == 8000
    assert settings.java_internal_base_url == "http://127.0.0.1:9991/api"
    assert settings.qwen_embedding_dimensions == 1024
    assert settings.mineru_mode == "cloud_api"


def test_auth_rejects_empty_configured_secret_and_invalid_actor_ids(monkeypatch):
    from app.config import get_settings
    from app.main import create_app

    monkeypatch.setenv("AGENT_INTERNAL_SECRET", "")
    get_settings.cache_clear()
    client = TestClient(create_app())
    assert client.get("/internal/cs/client/ping", headers={"X-Internal-Secret": "", "X-User-Id": "1"}).status_code == 401

    monkeypatch.setenv("AGENT_INTERNAL_SECRET", "test-secret-xyz")
    get_settings.cache_clear()
    client = TestClient(create_app())

    for bad_uid in ("0", "-1", "abc", "", "1_0"):
        res = client.get(
            "/internal/cs/client/ping",
            headers={"X-Internal-Secret": "test-secret-xyz", "X-User-Id": bad_uid},
        )
        assert res.status_code == 401, f"expected 401 for X-User-Id={bad_uid!r}"

    assert client.get("/internal/cs/admin/ping", headers={"X-Internal-Secret": "wrong", "X-Admin-Id": "1"}).status_code == 401
    for bad_aid in ("0", "-5", "abc", ""):
        res = client.get(
            "/internal/cs/admin/ping",
            headers={"X-Internal-Secret": "test-secret-xyz", "X-Admin-Id": bad_aid},
        )
        assert res.status_code == 401, f"expected 401 for X-Admin-Id={bad_aid!r}"
