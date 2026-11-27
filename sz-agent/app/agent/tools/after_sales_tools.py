from __future__ import annotations

from collections.abc import Mapping
from typing import Any, Literal

from langchain.tools import ToolRuntime, tool

from app.agent.context import AgentRuntimeContext
from app.agent.tools._common import (
    clamp_limit,
    format_optional_decimal,
    pick_field,
    search_kb,
    to_int,
    to_optional_int,
)
from app.clients.mall_client import MallClientError

ALLOWED_ORDER_TABS = frozenset(
    {"all", "unpaid", "wait_deliver", "wait_receive", "completed", "refund"}
)
ALLOWED_AFTER_SALES_KB_CATEGORIES = ("consumer_law", "mall_policy", "faq")


def _normalize_order_product(item: Mapping[str, Any]) -> dict[str, Any]:
    return {
        "product_id": to_optional_int(pick_field(item, "productId", "product_id")),
        "sku_id": to_optional_int(pick_field(item, "skuId", "sku_id")),
        "product_name": str(pick_field(item, "productName", "product_name", default="") or ""),
        "image": str(item.get("image") or ""),
        "sku": str(item.get("sku") or ""),
        "price": format_optional_decimal(item.get("price")),
        "quantity": to_int(pick_field(item, "cartNum", "quantity"), 0),
    }


def _normalize_order_summary(order: Mapping[str, Any]) -> dict[str, Any]:
    raw_items = pick_field(order, "orderInfoList", "order_info_list", default=[])
    items = [_normalize_order_product(it) for it in raw_items] if isinstance(raw_items, list) else []
    return {
        "order_no": str(pick_field(order, "orderNo", "order_no", default="") or ""),
        "paid": to_optional_int(order.get("paid")),
        "status": to_optional_int(order.get("status")),
        "display_tab": str(pick_field(order, "displayTab", "display_tab", default="") or ""),
        "display_status": str(pick_field(order, "displayStatus", "display_status", default="") or ""),
        "order_type": to_optional_int(pick_field(order, "orderType", "order_type")),
        "order_type_label": str(pick_field(order, "orderTypeLabel", "order_type_label", default="") or ""),
        "create_time": pick_field(order, "createTime", "create_time"),
        "pay_time": pick_field(order, "payTime", "pay_time"),
        "pay_price": format_optional_decimal(pick_field(order, "payPrice", "pay_price")),
        "total_num": to_int(pick_field(order, "totalNum", "total_num"), 0),
        "can_pay": bool(pick_field(order, "canPay", "can_pay", default=False)),
        "can_confirm_receipt": bool(
            pick_field(order, "canConfirmReceipt", "can_confirm_receipt", default=False)
        ),
        "can_evaluate": bool(pick_field(order, "canEvaluate", "can_evaluate", default=False)),
        "can_apply_after_sale": bool(
            pick_field(order, "canApplyAfterSale", "can_apply_after_sale", default=False)
        ),
        "after_sale_id": to_optional_int(pick_field(order, "afterSaleId", "after_sale_id")),
        "after_sale_status": to_optional_int(pick_field(order, "afterSaleStatus", "after_sale_status")),
        "after_sale_status_text": pick_field(order, "afterSaleStatusText", "after_sale_status_text"),
        "items": items,
    }


def _normalize_shipment(shipment: Any) -> dict[str, Any] | None:
    if not isinstance(shipment, Mapping):
        return None
    return {
        "express_no": pick_field(shipment, "expressNo", "express_no"),
        "express_tmp_id": to_optional_int(pick_field(shipment, "expressTmpId", "express_tmp_id")),
        "express_template_name": pick_field(
            shipment, "expressTemplateName", "express_template_name"
        ),
        "express_company_name": pick_field(
            shipment, "expressCompanyName", "express_company_name"
        ),
        "status": to_optional_int(shipment.get("status")),
        "status_text": pick_field(shipment, "statusText", "status_text"),
        "create_time": pick_field(shipment, "createTime", "create_time"),
        "real_name": pick_field(shipment, "realName", "real_name"),
        "user_phone": pick_field(shipment, "userPhone", "user_phone"),
        "user_address": pick_field(shipment, "userAddress", "user_address"),
    }


def _normalize_after_sale(item: Mapping[str, Any]) -> dict[str, Any]:
    return {
        "id": to_optional_int(item.get("id")),
        "service_no": str(pick_field(item, "serviceNo", "service_no", default="") or ""),
        "order_id": to_optional_int(pick_field(item, "orderId", "order_id")),
        "order_no": str(pick_field(item, "orderNo", "order_no", default="") or ""),
        "after_sale_type": to_optional_int(pick_field(item, "afterSaleType", "after_sale_type")),
        "after_sale_type_text": pick_field(item, "afterSaleTypeText", "after_sale_type_text"),
        "status": to_optional_int(item.get("status")),
        "status_text": pick_field(item, "statusText", "status_text"),
        "apply_amount": format_optional_decimal(pick_field(item, "applyAmount", "apply_amount")),
        "reason": item.get("reason"),
        "user_remark": pick_field(item, "userRemark", "user_remark"),
        "audit_remark": pick_field(item, "auditRemark", "audit_remark"),
        "receive_remark": pick_field(item, "receiveRemark", "receive_remark"),
        "return_express_name": pick_field(item, "returnExpressName", "return_express_name"),
        "return_express_no": pick_field(item, "returnExpressNo", "return_express_no"),
        "return_remark": pick_field(item, "returnRemark", "return_remark"),
        "audit_time": pick_field(item, "auditTime", "audit_time"),
        "return_time": pick_field(item, "returnTime", "return_time"),
        "receive_time": pick_field(item, "receiveTime", "receive_time"),
        "refund_time": pick_field(item, "refundTime", "refund_time"),
        "create_time": pick_field(item, "createTime", "create_time"),
    }


def _build_after_sale_guidance(order: Mapping[str, Any]) -> str | None:
    if order.get("after_sale") is not None:
        return None
    if order.get("can_apply_after_sale"):
        return "当前订单支持在订单详情或售后页面自助提交仅退款/退货退款申请。"
    paid = order.get("paid")
    status = order.get("status")
    status_desc = order.get("status_desc") or order.get("display_status") or ""
    if paid == 1 and (status == 0 or status_desc == "待发货"):
        return (
            "当前订单为已付款待发货状态，暂不支持前端自助售后申请通道；"
            "若需申请取消订单或退款，请引导用户转人工工单由后台客服核实并处理拦截退款。"
        )
    if paid == 1 and status == 1:
        return (
            "当前订单已发货待收货，暂不支持前端自助售后申请通道；"
            "如需拒收拦截或紧急处理，可引导用户转人工工单，或签收后在订单页申请售后。"
        )
    return None


def build_after_sales_tools(mall_client: Any, kb_retriever: Any) -> list:
    """构建订单查询、物流跟踪、售后进度查询及法规/商城规范 RAG 共 4 个售后工具。"""

    @tool
    async def list_my_orders(
        tab: Literal["all", "unpaid", "wait_deliver", "wait_receive", "completed", "refund"] = "all",
        keyword: str | None = None,
        limit: int = 5,
        *,
        runtime: ToolRuntime[AgentRuntimeContext],
    ) -> dict[str, Any]:
        """查询当前用户的订单列表摘要（支持按状态标签页与订单号/商品关键词筛选）。"""
        normalized_tab = tab if tab in ALLOWED_ORDER_TABS else "all"
        bounded_limit = clamp_limit(limit, default=5, maximum=20)
        user_id = runtime.context.user_id
        try:
            page_result = await mall_client.list_user_orders(
                user_id=user_id,
                tab=normalized_tab,
                keyword=keyword,
                page=1,
                limit=bounded_limit,
            )
        except MallClientError as exc:
            return {"ok": False, "message": exc.message, "orders": [], "total": 0}

        if isinstance(page_result, list):
            rows = page_result
            total = len(rows)
        elif isinstance(page_result, Mapping):
            raw_rows = page_result.get("rows")
            rows = raw_rows if isinstance(raw_rows, list) else []
            total = to_int(page_result.get("total"), len(rows))
        else:
            rows = []
            total = 0

        orders = [_normalize_order_summary(row) for row in rows]
        return {
            "ok": True,
            "tab": normalized_tab,
            "total": total,
            "orders": orders,
        }

    @tool
    async def get_my_order_detail(
        order_no: str,
        *,
        runtime: ToolRuntime[AgentRuntimeContext],
    ) -> dict[str, Any]:
        """查询当前用户指定订单的明细、中文状态、发货记录、快递单号与状态时间轴。"""
        cleaned_order_no = (order_no or "").strip()
        if not cleaned_order_no:
            return {"ok": False, "message": "请提供有效的订单号。"}

        user_id = runtime.context.user_id
        try:
            raw = await mall_client.get_order_detail(user_id, cleaned_order_no)
        except MallClientError as exc:
            return {"ok": False, "message": exc.message}

        raw_items = pick_field(raw, "orderInfoList", "order_info_list", default=[])
        raw_timeline = pick_field(raw, "orderStatusList", "order_status_list", default=[])
        raw_after_sale = pick_field(raw, "afterSale", "after_sale")
        shipment = _normalize_shipment(raw.get("shipment"))
        tracking_supported = bool(pick_field(raw, "trackingSupported", "tracking_supported", default=False))
        express_no = pick_field(raw, "expressNo", "express_no") or (
            shipment["express_no"] if shipment else None
        )

        order = {
            "order_no": str(pick_field(raw, "orderNo", "order_no", default=cleaned_order_no) or ""),
            "paid": to_optional_int(raw.get("paid")),
            "status": to_optional_int(raw.get("status")),
            "status_desc": str(
                pick_field(raw, "statusDesc", "status_desc", "displayStatus", "display_status", default="")
                or ""
            ),
            "display_tab": str(pick_field(raw, "displayTab", "display_tab", default="") or ""),
            "display_status": str(pick_field(raw, "displayStatus", "display_status", default="") or ""),
            "order_type": to_optional_int(pick_field(raw, "orderType", "order_type")),
            "order_type_label": str(
                pick_field(raw, "orderTypeLabel", "order_type_label", default="") or ""
            ),
            "create_time": pick_field(raw, "createTime", "create_time"),
            "pay_time": pick_field(raw, "payTime", "pay_time"),
            "pay_price": format_optional_decimal(pick_field(raw, "payPrice", "pay_price")),
            "freight_total_fee": format_optional_decimal(
                pick_field(raw, "freightTotalFee", "freight_total_fee")
            ),
            "total_num": to_int(pick_field(raw, "totalNum", "total_num"), 0),
            "real_name": pick_field(raw, "realName", "real_name"),
            "user_phone": pick_field(raw, "userPhone", "user_phone"),
            "user_address": pick_field(raw, "userAddress", "user_address"),
            "pay_type": pick_field(raw, "payType", "pay_type"),
            "express_no": express_no,
            "express_name": pick_field(raw, "expressName", "express_name")
            or (shipment["express_company_name"] if shipment else None),
            "can_pay": bool(pick_field(raw, "canPay", "can_pay", default=False)),
            "can_confirm_receipt": bool(
                pick_field(raw, "canConfirmReceipt", "can_confirm_receipt", default=False)
            ),
            "can_evaluate": bool(pick_field(raw, "canEvaluate", "can_evaluate", default=False)),
            "can_apply_after_sale": bool(
                pick_field(raw, "canApplyAfterSale", "can_apply_after_sale", default=False)
            ),
            "after_sale_id": to_optional_int(pick_field(raw, "afterSaleId", "after_sale_id")),
            "after_sale_status": to_optional_int(
                pick_field(raw, "afterSaleStatus", "after_sale_status")
            ),
            "after_sale_status_text": pick_field(
                raw, "afterSaleStatusText", "after_sale_status_text"
            ),
            "items": [_normalize_order_product(it) for it in raw_items]
            if isinstance(raw_items, list)
            else [],
            "timeline": [
                {
                    "change_type": pick_field(st, "changeType", "change_type"),
                    "change_message": pick_field(st, "changeMessage", "change_message"),
                    "create_time": pick_field(st, "createTime", "create_time"),
                }
                for st in raw_timeline
            ]
            if isinstance(raw_timeline, list)
            else [],
            "shipment": shipment,
            "after_sale": _normalize_after_sale(raw_after_sale)
            if isinstance(raw_after_sale, Mapping)
            else None,
            "tracking_supported": tracking_supported,
        }

        response: dict[str, Any] = {"ok": True, "order": order}
        if not tracking_supported and (express_no or shipment):
            response["tracking_notice"] = (
                "商城当前仅记录发货状态、发货时间与快递单号，暂不支持查询第三方快递实时在途路由节点，请勿编造在途物流轨迹。"
            )
        guidance = _build_after_sale_guidance(order)
        if guidance:
            response["after_sale_guidance"] = guidance
        return response

    @tool
    async def get_my_after_sale_status(
        order_no: str | None = None,
        service_no: str | None = None,
        *,
        runtime: ToolRuntime[AgentRuntimeContext],
    ) -> dict[str, Any]:
        """查询当前用户的售后申请单列表、审核状态、退货物流及退款进度。"""
        user_id = runtime.context.user_id
        try:
            raw_list = await mall_client.list_user_after_sales(
                user_id=user_id,
                order_no=order_no,
                service_no=service_no,
                limit=10,
            )
        except MallClientError as exc:
            return {"ok": False, "message": exc.message, "after_sales": [], "count": 0}

        after_sales = [_normalize_after_sale(item) for item in raw_list]
        return {
            "ok": True,
            "after_sales": after_sales,
            "count": len(after_sales),
        }

    @tool
    async def search_policy_and_law_kb(
        query: str,
        category: Literal["consumer_law", "mall_policy", "faq"] | None = None,
        top_k: int = 5,
    ) -> dict[str, Any]:
        """检索消费者权益保护法规、咔嗒造物商城服务规范及售后常见问答知识库，返回结构化条款出处与正文。"""
        if category is not None and category not in ALLOWED_AFTER_SALES_KB_CATEGORIES:
            return {
                "ok": False,
                "query": query,
                "categories": [],
                "hits": [],
                "citations": [],
                "found": False,
                "message": "售后知识库仅支持检索 consumer_law、mall_policy 或 faq 分类。",
            }
        categories = [category] if category is not None else list(ALLOWED_AFTER_SALES_KB_CATEGORIES)
        return await search_kb(
            kb_retriever,
            query=query,
            categories=categories,
            top_k=top_k,
            empty_message="未在消费法规或商城服务规范知识库中检索到相似度达标的条文，请勿编造法条，建议如实告知并引导转人工工单确认。",
        )

    return [
        list_my_orders,
        get_my_order_detail,
        get_my_after_sale_status,
        search_policy_and_law_kb,
    ]
