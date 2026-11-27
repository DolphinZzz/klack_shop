from __future__ import annotations

from decimal import Decimal, InvalidOperation
import re
from typing import Any
import urllib.parse

import httpx

from app.config import Settings, get_settings

_INVALID_ORDER_NO_RE = re.compile(r"[/\\\x00-\x1f\x7f]|\.\.")


class MallClientError(RuntimeError):
    """Java `/internal/agent/**` 接口返回业务错误或 HTTP 异常时抛出的结构化错误。"""

    def __init__(
        self,
        message: str,
        *,
        code: str | None = None,
        status_code: int | None = None,
        payload: Any = None,
    ) -> None:
        super().__init__(message)
        self.message = message
        self.code = code
        self.status_code = status_code
        self.payload = payload


def _require_positive_int(value: Any, field_name: str) -> int:
    if isinstance(value, bool) or not isinstance(value, int) or value <= 0:
        raise MallClientError(f"Invalid {field_name}: must be a positive integer", code="INVALID_PARAM")
    return value


def _encode_order_no(order_no: str) -> str:
    if not isinstance(order_no, str):
        raise MallClientError("订单号不能为空", code="INVALID_PARAM")
    cleaned = order_no.strip()
    if not cleaned or _INVALID_ORDER_NO_RE.search(cleaned):
        raise MallClientError("订单号格式无效", code="INVALID_PARAM")
    return urllib.parse.quote(cleaned, safe="")


def _format_price_param(value: Decimal | float | int | str, field_name: str) -> str:
    if isinstance(value, bool):
        raise MallClientError(f"Invalid {field_name}", code="INVALID_PARAM")
    try:
        dec = Decimal(str(value).strip())
    except (InvalidOperation, ValueError) as exc:
        raise MallClientError(f"Invalid {field_name}", code="INVALID_PARAM") from exc
    if not dec.is_finite() or dec < Decimal("0"):
        raise MallClientError(f"Invalid {field_name}", code="INVALID_PARAM")
    return format(dec, "f")


class MallInternalClient:
    """用于访问 Java `/internal/agent/**` 商城数据与直接建单接口的异步 HTTP 客户端。"""

    def __init__(
        self,
        settings: Settings | None = None,
        *,
        base_url: str | None = None,
        internal_secret: str | None = None,
        timeout: float | None = None,
        client: httpx.AsyncClient | None = None,
    ) -> None:
        resolved = settings or get_settings()
        resolved_base = (base_url if base_url is not None else resolved.java_internal_base_url).rstrip("/")
        self._base_url = resolved_base
        self._internal_secret = (
            internal_secret if internal_secret is not None else resolved.agent_internal_secret
        )
        self._timeout = timeout if timeout is not None else resolved.java_internal_timeout_seconds
        self._owns_client = client is None
        self._client = client or httpx.AsyncClient(
            base_url=self._base_url,
            timeout=self._timeout,
        )

    @classmethod
    def from_settings(cls, settings: Settings, **overrides: Any) -> MallInternalClient:
        """根据应用 `Settings` 配置实例化 `MallInternalClient`。"""
        return cls(settings=settings, **overrides)

    async def aclose(self) -> None:
        """若底层 `httpx.AsyncClient` 由当前实例持有，则将其关闭。"""
        if self._owns_client:
            await self._client.aclose()

    async def __aenter__(self) -> MallInternalClient:
        return self

    async def __aexit__(self, *_exc: object) -> None:
        await self.aclose()

    async def _request(
        self,
        method: str,
        path: str,
        *,
        params: dict[str, Any] | None = None,
        json_body: dict[str, Any] | None = None,
    ) -> Any:
        headers = {"X-Internal-Secret": self._internal_secret}
        try:
            response = await self._client.request(
                method,
                path,
                params=params,
                json=json_body,
                headers=headers,
            )
        except httpx.HTTPError as exc:
            raise MallClientError(f"内网商城接口请求失败: {exc}") from exc

        payload: Any = None
        try:
            payload = response.json()
        except ValueError as exc:
            if response.status_code >= 400:
                raise MallClientError(
                    f"内网商城接口返回 HTTP {response.status_code}",
                    status_code=response.status_code,
                ) from exc
            raise MallClientError(
                "内网商城接口返回了非 JSON 响应",
                status_code=response.status_code,
            ) from exc

        if response.status_code >= 400:
            if isinstance(payload, dict):
                code = str(payload.get("code")) if payload.get("code") is not None else None
                message = str(payload.get("message") or f"内网商城接口返回 HTTP {response.status_code}")
                raise MallClientError(
                    message,
                    code=code,
                    status_code=response.status_code,
                    payload=payload,
                )
            raise MallClientError(
                f"内网商城接口返回 HTTP {response.status_code}",
                status_code=response.status_code,
                payload=payload,
            )

        if not isinstance(payload, dict) or "code" not in payload:
            raise MallClientError(
                "内网商城接口响应格式无效",
                status_code=response.status_code,
                payload=payload,
            )

        code = str(payload.get("code"))
        if code != "0000":
            message = str(payload.get("message") or f"内网商城接口返回错误码 {code}")
            raise MallClientError(
                message,
                code=code,
                status_code=response.status_code,
                payload=payload,
            )

        return payload.get("data")

    async def recommend_products(
        self,
        sort: str = "hot",
        category_id: int | None = None,
        limit: int = 5,
    ) -> list[dict[str, Any]]:
        """按热销（`hot`）、销量（`sales`）或浏览量（`browse`）排序查询推荐商品。"""
        params: dict[str, Any] = {
            "sort": sort,
            "limit": _require_positive_int(limit, "limit"),
        }
        if category_id is not None:
            params["categoryId"] = _require_positive_int(category_id, "category_id")
        data = await self._request("GET", "/internal/agent/products/recommend", params=params)
        return list(data) if isinstance(data, list) else []

    async def search_products(
        self,
        keyword: str | None = None,
        category_id: int | None = None,
        min_price: Decimal | float | int | str | None = None,
        max_price: Decimal | float | int | str | None = None,
        limit: int = 6,
    ) -> list[dict[str, Any]]:
        """按关键词、分类及价格区间检索在售商品。"""
        params: dict[str, Any] = {"limit": _require_positive_int(limit, "limit")}
        if keyword is not None and keyword.strip():
            params["keyword"] = keyword.strip()
        if category_id is not None:
            params["categoryId"] = _require_positive_int(category_id, "category_id")
        if min_price is not None:
            params["minPrice"] = _format_price_param(min_price, "min_price")
        if max_price is not None:
            params["maxPrice"] = _format_price_param(max_price, "max_price")
        data = await self._request("GET", "/internal/agent/products/search", params=params)
        return list(data) if isinstance(data, list) else []

    async def get_product_detail(self, product_id: int) -> dict[str, Any]:
        """获取商品详情、进行中的秒杀活动状态及完整 SKU 列表。"""
        pid = _require_positive_int(product_id, "product_id")
        data = await self._request("GET", f"/internal/agent/products/{pid}")
        return dict(data) if isinstance(data, dict) else {}

    async def list_user_addresses(self, user_id: int) -> list[dict[str, Any]]:
        """获取指定 `user_id` 名下的可用收货地址列表。"""
        uid = _require_positive_int(user_id, "user_id")
        data = await self._request("GET", f"/internal/agent/users/{uid}/addresses")
        return list(data) if isinstance(data, list) else []

    async def list_user_orders(
        self,
        user_id: int,
        tab: str = "all",
        keyword: str | None = None,
        page: int = 1,
        limit: int = 5,
    ) -> dict[str, Any]:
        """分页获取指定 `user_id` 名下的订单摘要列表。"""
        uid = _require_positive_int(user_id, "user_id")
        params: dict[str, Any] = {
            "tab": tab,
            "page": _require_positive_int(page, "page"),
            "limit": _require_positive_int(limit, "limit"),
        }
        if keyword is not None and keyword.strip():
            params["keyword"] = keyword.strip()
        data = await self._request("GET", f"/internal/agent/users/{uid}/orders", params=params)
        if isinstance(data, dict):
            return dict(data)
        return {"current": page, "limit": limit, "totalPage": 0, "total": 0, "rows": []}

    async def get_order_detail(self, user_id: int, order_no: str) -> dict[str, Any]:
        """获取订单详情、发货记录、状态时间轴及关联售后单信息。"""
        uid = _require_positive_int(user_id, "user_id")
        encoded_order_no = _encode_order_no(order_no)
        data = await self._request("GET", f"/internal/agent/users/{uid}/orders/{encoded_order_no}")
        return dict(data) if isinstance(data, dict) else {}

    async def list_user_after_sales(
        self,
        user_id: int,
        order_no: str | None = None,
        service_no: str | None = None,
        limit: int = 10,
    ) -> list[dict[str, Any]]:
        """获取指定 `user_id` 名下的售后申请记录列表。"""
        uid = _require_positive_int(user_id, "user_id")
        params: dict[str, Any] = {"limit": _require_positive_int(limit, "limit")}
        if order_no is not None and order_no.strip():
            params["orderNo"] = order_no.strip()
        if service_no is not None and service_no.strip():
            params["serviceNo"] = service_no.strip()
        data = await self._request("GET", f"/internal/agent/users/{uid}/after-sales", params=params)
        return list(data) if isinstance(data, list) else []

    async def direct_create_order(
        self,
        user_id: int,
        product_id: int,
        sku_id: int,
        quantity: int,
        address_id: int,
        idempotency_key: str,
    ) -> dict[str, Any]:
        """在 `interrupt` 确认后为指定 `user_id` 幂等创建待支付订单。"""
        uid = _require_positive_int(user_id, "user_id")
        pid = _require_positive_int(product_id, "product_id")
        sid = _require_positive_int(sku_id, "sku_id")
        qty = _require_positive_int(quantity, "quantity")
        aid = _require_positive_int(address_id, "address_id")
        if not isinstance(idempotency_key, str) or not idempotency_key.strip():
            raise MallClientError("幂等键不能为空", code="INVALID_PARAM")

        body = {
            "productId": pid,
            "skuId": sid,
            "quantity": qty,
            "addressId": aid,
            "idempotencyKey": idempotency_key.strip(),
        }
        data = await self._request(
            "POST",
            f"/internal/agent/users/{uid}/orders/direct-create",
            json_body=body,
        )
        return dict(data) if isinstance(data, dict) else {}
