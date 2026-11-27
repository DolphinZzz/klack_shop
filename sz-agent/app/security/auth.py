import hmac
import urllib.parse
from dataclasses import dataclass
from typing import Annotated

from fastapi import Depends, Header, HTTPException, status

from app.config import Settings, get_settings


@dataclass(frozen=True, slots=True)
class AdminActor:
    """从受信反向代理请求头中提取的已认证内部管理员身份。"""

    admin_id: int
    admin_name: str


def _unauthorized(detail: str) -> HTTPException:
    return HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail=detail,
    )


def _parse_positive_int(raw: str | None, header_name: str) -> int:
    if raw is None:
        raise _unauthorized(f"Missing {header_name}")
    stripped = raw.strip()
    if not stripped or not stripped.isascii() or not stripped.isdigit():
        raise _unauthorized(f"Invalid {header_name}")
    value = int(stripped)
    if value <= 0:
        raise _unauthorized(f"Invalid {header_name}")
    return value


def verify_internal_secret(
    x_internal_secret: Annotated[str | None, Header(alias="X-Internal-Secret")] = None,
    settings: Annotated[Settings | None, Depends(get_settings)] = None,
) -> None:
    """使用常量时间比较校验共享请求头 X-Internal-Secret。"""
    resolved_settings = settings if settings is not None else get_settings()
    expected = resolved_settings.agent_internal_secret
    if not expected or not x_internal_secret:
        raise _unauthorized("Invalid internal secret")
    if not hmac.compare_digest(x_internal_secret.encode("utf-8"), expected.encode("utf-8")):
        raise _unauthorized("Invalid internal secret")


def get_client_user_id(
    x_user_id: Annotated[str | None, Header(alias="X-User-Id")] = None,
) -> int:
    """从请求头 X-User-Id 中提取并校验正整数客户端用户 ID。"""
    return _parse_positive_int(x_user_id, "X-User-Id")


def get_admin_actor(
    x_admin_id: Annotated[str | None, Header(alias="X-Admin-Id")] = None,
    x_admin_name: Annotated[str | None, Header(alias="X-Admin-Name")] = None,
) -> AdminActor:
    """从代理请求头中提取并校验管理员 ID 及 URL 解码后的管理员名称。"""
    admin_id = _parse_positive_int(x_admin_id, "X-Admin-Id")
    admin_name = urllib.parse.unquote(x_admin_name).strip() if x_admin_name else ""
    return AdminActor(admin_id=admin_id, admin_name=admin_name)
