from __future__ import annotations

from collections.abc import Mapping
from decimal import Decimal, ROUND_HALF_UP
from typing import Any, Literal
import uuid

from langchain.tools import ToolRuntime, tool

from app.agent.context import AgentRuntimeContext
from app.agent.tools._common import (
    clamp_limit,
    format_decimal,
    format_optional_decimal,
    normalize_user_address,
    parse_decimal,
    pick_field,
    search_kb,
    to_int,
    to_optional_int,
)
from app.clients.mall_client import MallClientError

PRESALE_KB_CATEGORIES = ["product_guide", "faq"]


def _resolve_freight_decimal(product: Mapping[str, Any]) -> Decimal:
    is_postage = to_int(pick_field(product, "isPostage", "is_postage"), 0)
    if is_postage == 1:
        return Decimal("0.00")
    postage = parse_decimal(product.get("postage"))
    if postage <= Decimal("0"):
        return Decimal("0.00")
    return postage.quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)


def _normalize_product_summary(product: Mapping[str, Any]) -> dict[str, Any]:
    return {
        "id": to_optional_int(product.get("id")),
        "product_name": str(pick_field(product, "productName", "product_name", default="") or ""),
        "product_info": str(pick_field(product, "productInfo", "product_info", default="") or ""),
        "image": str(product.get("image") or ""),
        "cate_id": to_optional_int(pick_field(product, "cateId", "cate_id")),
        "price": format_optional_decimal(product.get("price")),
        "ot_price": format_optional_decimal(pick_field(product, "otPrice", "ot_price")),
        "stock": to_int(product.get("stock"), 0),
        "sales": to_int(product.get("sales"), 0),
        "browse": to_int(product.get("browse"), 0),
        "is_hot": to_int(pick_field(product, "isHot", "is_hot"), 0),
        "is_best": to_int(pick_field(product, "isBest", "is_best"), 0),
        "is_new": to_int(pick_field(product, "isNew", "is_new"), 0),
        "is_seckill": to_int(pick_field(product, "isSeckill", "is_seckill"), 0),
        "is_postage": to_int(pick_field(product, "isPostage", "is_postage"), 0),
        "postage": format_decimal(product.get("postage")),
        "unit_name": str(pick_field(product, "unitName", "unit_name", default="") or ""),
    }


def _normalize_sku(sku: Mapping[str, Any]) -> dict[str, Any]:
    return {
        "sku_id": to_optional_int(pick_field(sku, "skuId", "sku_id", "id")),
        "sku": str(sku.get("sku") or ""),
        "price": format_optional_decimal(sku.get("price")),
        "ot_price": format_optional_decimal(pick_field(sku, "otPrice", "ot_price")),
        "flash_price": format_optional_decimal(pick_field(sku, "flashPrice", "flash_price")),
        "stock": to_int(sku.get("stock"), 0),
        "image": str(sku.get("image") or ""),
        "is_default": to_int(pick_field(sku, "isDefault", "is_default"), 0),
    }


def _normalize_product_detail(detail: Mapping[str, Any]) -> dict[str, Any]:
    active_flash = bool(pick_field(detail, "activeFlashSale", "active_flash_sale", default=False))
    freight = _resolve_freight_decimal(detail)
    raw_skus = detail.get("skus")
    skus = [_normalize_sku(s) for s in raw_skus] if isinstance(raw_skus, list) else []
    summary = _normalize_product_summary(detail)
    summary.update(
        {
            "keyword": str(detail.get("keyword") or ""),
            "slider_image": str(pick_field(detail, "sliderImage", "slider_image", default="") or ""),
            "is_show": to_int(pick_field(detail, "isShow", "is_show"), 1),
            "is_del": to_int(pick_field(detail, "isDel", "is_del"), 0),
            "active_flash_sale": active_flash,
            "flash_sale_id": to_optional_int(pick_field(detail, "flashSaleId", "flash_sale_id")),
            "freight_price": f"{freight:.2f}",
            "desc": str(detail.get("desc") or ""),
            "skus": skus,
            "can_direct_order": not active_flash
            and to_int(pick_field(detail, "isShow", "is_show"), 1) == 1
            and to_int(pick_field(detail, "isDel", "is_del"), 0) != 1,
        }
    )
    return summary


def build_shopping_tools(mall_client: Any, kb_retriever: Any) -> list:
    """Build the 6 pre-sales recommendation and order-draft LangChain tools."""

    @tool
    async def list_hot_and_popular_products(
        sort_by: Literal["hot", "sales", "browse"] = "hot",
        category_id: int | None = None,
        limit: int = 5,
    ) -> dict[str, Any]:
        """查询商城当前上架的热销、高销量或高浏览量推荐商品列表。"""
        normalized_sort = sort_by if sort_by in ("hot", "sales", "browse") else "hot"
        bounded_limit = clamp_limit(limit, default=5, maximum=20)
        effective_category_id = category_id if (category_id is not None and category_id > 0) else None
        try:
            raw_items = await mall_client.recommend_products(
                sort=normalized_sort,
                category_id=effective_category_id,
                limit=bounded_limit,
            )
        except MallClientError as exc:
            return {"ok": False, "message": exc.message, "products": [], "count": 0}

        products = [_normalize_product_summary(item) for item in raw_items]
        return {
            "ok": True,
            "sort_by": normalized_sort,
            "products": products,
            "count": len(products),
        }

    @tool
    async def search_products(
        keyword: str | None = None,
        category_id: int | None = None,
        min_price: float | None = None,
        max_price: float | None = None,
        limit: int = 6,
    ) -> dict[str, Any]:
        """按关键词、分类 ID 与价格区间检索商城上架商品。"""
        bounded_limit = clamp_limit(limit, default=6, maximum=20)
        effective_category_id = category_id if (category_id is not None and category_id > 0) else None
        min_dec = parse_decimal(min_price, default="-1") if min_price is not None else None
        max_dec = parse_decimal(max_price, default="-1") if max_price is not None else None
        if (min_dec is not None and min_dec < Decimal("0")) or (
            max_dec is not None and max_dec < Decimal("0")
        ):
            return {"ok": False, "message": "价格区间必须为非负数字。", "products": [], "count": 0}
        try:
            raw_items = await mall_client.search_products(
                keyword=keyword,
                category_id=effective_category_id,
                min_price=min_dec,
                max_price=max_dec,
                limit=bounded_limit,
            )
        except MallClientError as exc:
            return {"ok": False, "message": exc.message, "products": [], "count": 0}

        products = [_normalize_product_summary(item) for item in raw_items]
        return {
            "ok": True,
            "products": products,
            "count": len(products),
        }

    @tool
    async def get_product_detail_and_skus(product_id: int) -> dict[str, Any]:
        """查询指定商品的详情、实时库存、全部 SKU 规格价格与是否处于秒杀活动。"""
        if product_id <= 0:
            return {"ok": False, "message": "商品 ID 必须为正整数。"}
        try:
            raw_detail = await mall_client.get_product_detail(product_id)
        except MallClientError as exc:
            return {"ok": False, "message": exc.message}

        product = _normalize_product_detail(raw_detail)
        result: dict[str, Any] = {"ok": True, "product": product}
        if product["active_flash_sale"]:
            result["flash_sale_notice"] = (
                "该商品当前正在参与限时秒杀活动，暂不支持客服直接下单，请引导用户前往商品秒杀页面抢购。"
            )
        return result

    @tool
    async def search_presale_kb(query: str, top_k: int = 4) -> dict[str, Any]:
        """检索售前导购知识库（仅限 product_guide 与 faq），用于解答商品选购、参数与兼容性问题。"""
        return await search_kb(
            kb_retriever,
            query=query,
            categories=list(PRESALE_KB_CATEGORIES),
            top_k=top_k,
            empty_message="未在商品选购指南与常见问答中检索到强相关条款，请勿编造规格参数，建议用户参考商品详情页或转人工咨询。",
        )

    @tool
    async def list_user_addresses(runtime: ToolRuntime[AgentRuntimeContext]) -> dict[str, Any]:
        """读取当前已登录用户的收货地址列表（默认地址排在最前）。"""
        user_id = runtime.context.user_id
        try:
            raw_addresses = await mall_client.list_user_addresses(user_id)
        except MallClientError as exc:
            return {"ok": False, "message": exc.message, "addresses": [], "has_address": False}

        addresses = [normalize_user_address(addr) for addr in raw_addresses]
        if not addresses:
            return {
                "ok": True,
                "addresses": [],
                "has_address": False,
                "redirect_url": "/user/account/address",
                "message": "您当前暂无收货地址，请前往 /user/account/address 新增收货地址。",
            }
        return {"ok": True, "addresses": addresses, "has_address": True}

    @tool
    async def prepare_order_draft(
        product_id: int,
        sku_id: int,
        quantity: int = 1,
        address_id: int | None = None,
        *,
        runtime: ToolRuntime[AgentRuntimeContext],
    ) -> dict[str, Any]:
        """校验商品、SKU、库存、秒杀状态与当前用户收货地址，生成待用户确认的下单草稿（不会直接建单）。"""
        if product_id <= 0 or sku_id <= 0:
            return {"ok": False, "reason": "invalid_params", "message": "商品或规格 ID 无效。"}
        if quantity is None or quantity <= 0:
            return {"ok": False, "reason": "invalid_quantity", "message": "购买数量必须大于 0。"}

        try:
            detail = await mall_client.get_product_detail(product_id)
        except MallClientError as exc:
            return {"ok": False, "reason": "mall_error", "message": exc.message}

        if not detail:
            return {"ok": False, "reason": "product_unavailable", "message": "未找到该商品信息。"}

        is_del = to_int(pick_field(detail, "isDel", "is_del"), 0)
        is_show = to_int(pick_field(detail, "isShow", "is_show"), 1)
        if is_del == 1 or is_show != 1:
            return {
                "ok": False,
                "reason": "product_unavailable",
                "message": "该商品已下架或不存在，暂不支持下单。",
            }

        if bool(pick_field(detail, "activeFlashSale", "active_flash_sale", default=False)):
            return {
                "ok": False,
                "reason": "active_flash_sale",
                "message": "该商品当前正在参与限时秒杀活动，暂不支持客服直接下单，请前往商品秒杀页面参与抢购。",
            }

        raw_skus = detail.get("skus")
        skus = raw_skus if isinstance(raw_skus, list) else []
        matched_sku = next(
            (s for s in skus if to_optional_int(pick_field(s, "skuId", "sku_id", "id")) == sku_id),
            None,
        )
        if matched_sku is None:
            return {
                "ok": False,
                "reason": "sku_not_found",
                "message": f"未找到规格 ID 为 {sku_id} 的有效商品规格，请先确认具体规格。",
            }

        sku_stock = to_int(matched_sku.get("stock"), 0)
        product_stock = to_optional_int(detail.get("stock"))
        if sku_stock < quantity or (product_stock is not None and product_stock < quantity):
            return {
                "ok": False,
                "reason": "insufficient_stock",
                "message": f"当前所选规格库存不足（剩余 {sku_stock} 件），暂无法下单，建议更换规格或浏览同类热销商品。",
            }

        raw_unit_price = matched_sku.get("price")
        if raw_unit_price in (None, ""):
            raw_unit_price = detail.get("price")
        unit_price = parse_decimal(raw_unit_price, default="0.00").quantize(
            Decimal("0.01"), rounding=ROUND_HALF_UP
        )
        if unit_price <= Decimal("0"):
            return {"ok": False, "reason": "invalid_price", "message": "商品价格异常，暂无法下单。"}

        goods_total = (unit_price * quantity).quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)
        freight_price = _resolve_freight_decimal(detail)
        total_pay_price = (goods_total + freight_price).quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)

        user_id = runtime.context.user_id
        try:
            raw_addresses = await mall_client.list_user_addresses(user_id)
        except MallClientError as exc:
            return {"ok": False, "reason": "mall_error", "message": exc.message}

        addresses = [normalize_user_address(addr) for addr in raw_addresses]
        if not addresses:
            return {
                "ok": False,
                "reason": "no_address",
                "redirect_url": "/user/account/address",
                "message": "您当前还没有收货地址，请先前往个人中心地址管理（/user/account/address）新增收货地址后再下单。",
            }

        if address_id is not None:
            selected_address = next((addr for addr in addresses if addr["id"] == address_id), None)
            if selected_address is None:
                return {
                    "ok": False,
                    "reason": "address_not_found",
                    "available_addresses": addresses,
                    "message": f"未找到 ID 为 {address_id} 的可用收货地址，请从当前账户的收货地址列表中重新选择。",
                }
        else:
            selected_address = next((addr for addr in addresses if addr["is_default"] == 1), addresses[0])

        draft = {
            "draft_id": uuid.uuid4().hex,
            "product_id": to_int(detail.get("id"), product_id),
            "product_name": str(pick_field(detail, "productName", "product_name", default="") or ""),
            "product_image": str(matched_sku.get("image") or detail.get("image") or ""),
            "sku_id": to_int(pick_field(matched_sku, "skuId", "sku_id", "id"), sku_id),
            "sku_name": str(matched_sku.get("sku") or ""),
            "unit_price": f"{unit_price:.2f}",
            "quantity": int(quantity),
            "freight_price": f"{freight_price:.2f}",
            "total_pay_price": f"{total_pay_price:.2f}",
            "address_id": selected_address["id"],
            "receiver_name": selected_address["real_name"],
            "receiver_phone": selected_address["phone"],
            "receiver_address": selected_address["full_address"],
            "available_addresses": addresses,
        }
        return {
            "ok": True,
            "draft": draft,
            "message": "已生成待确认下单草稿，请用户在确认卡片中核对商品规格、金额和收货地址后确认提交。",
        }

    return [
        list_hot_and_popular_products,
        search_products,
        get_product_detail_and_skus,
        search_presale_kb,
        list_user_addresses,
        prepare_order_draft,
    ]
