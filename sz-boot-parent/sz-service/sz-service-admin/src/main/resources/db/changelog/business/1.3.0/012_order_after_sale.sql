--liquibase formatted sql

--changeset car:20260401_012_01
--comment: 新增订单售后表
CREATE TABLE IF NOT EXISTS `shop_order_after_sale` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '售后单ID',
    `service_no` varchar(40) NOT NULL COMMENT '售后单号',
    `order_id` bigint NOT NULL COMMENT '订单ID',
    `order_no` varchar(40) NOT NULL COMMENT '订单号',
    `uid` bigint NOT NULL COMMENT '用户ID',
    `after_sale_type` tinyint NOT NULL COMMENT '售后类型：1仅退款 2退货退款',
    `status` tinyint NOT NULL DEFAULT '0' COMMENT '售后状态：0待审核 1待买家寄回 2待商家收货 3已退款 4已拒绝',
    `apply_amount` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '申请退款金额',
    `reason` varchar(255) NOT NULL COMMENT '售后原因',
    `user_remark` varchar(500) DEFAULT NULL COMMENT '用户备注',
    `audit_remark` varchar(500) DEFAULT NULL COMMENT '审核备注',
    `receive_remark` varchar(500) DEFAULT NULL COMMENT '收货备注',
    `return_express_name` varchar(100) DEFAULT NULL COMMENT '退货快递公司',
    `return_express_no` varchar(80) DEFAULT NULL COMMENT '退货快递单号',
    `return_remark` varchar(500) DEFAULT NULL COMMENT '退货备注',
    `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
    `return_time` datetime DEFAULT NULL COMMENT '买家寄回时间',
    `receive_time` datetime DEFAULT NULL COMMENT '商家收货时间',
    `refund_time` datetime DEFAULT NULL COMMENT '退款完成时间',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_del` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
    `version` int NOT NULL DEFAULT '0' COMMENT '版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_shop_order_after_sale_service_no` (`service_no`),
    UNIQUE KEY `uk_shop_order_after_sale_order` (`order_id`, `is_del`),
    KEY `idx_shop_order_after_sale_uid` (`uid`),
    KEY `idx_shop_order_after_sale_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='订单售后表';

--changeset car:20260401_012_02
--comment: 初始化售后管理菜单
INSERT INTO `sys_menu` (`id`, `pid`, `path`, `name`, `title`, `icon`, `component`, `redirect`, `sort`, `deep`, `menu_type_cd`, `permissions`, `is_hidden`, `has_children`, `is_link`, `is_full`, `is_affix`, `is_keep_alive`, `create_time`, `update_time`, `create_id`, `update_id`, `del_flag`, `delete_id`, `delete_time`, `use_data_scope`)
SELECT '8a4b2dbf9fa54f3b8820c0b3cbd11001', '6288292f0b0e466382ababe6fefb1ed1', '/shop_system/shopOrderAfterSale', 'ShopOrderAfterSale', '售后管理', 'Money', '/shop_system/shopOrderAfterSale/index', '', 960, 2, '1002002', '', 'F', 'T', 'F', 'F', 'F', 'T', NOW(), NOW(), 1, 1, 'F', NULL, NULL, 'F'
FROM dual
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_menu` WHERE `id` = '8a4b2dbf9fa54f3b8820c0b3cbd11001'
);

INSERT INTO `sys_menu` (`id`, `pid`, `path`, `name`, `title`, `icon`, `component`, `redirect`, `sort`, `deep`, `menu_type_cd`, `permissions`, `is_hidden`, `has_children`, `is_link`, `is_full`, `is_affix`, `is_keep_alive`, `create_time`, `update_time`, `create_id`, `update_id`, `del_flag`, `delete_id`, `delete_time`, `use_data_scope`)
SELECT '8a4b2dbf9fa54f3b8820c0b3cbd11002', '8a4b2dbf9fa54f3b8820c0b3cbd11001', '', 'ShopOrderAfterSaleQuery', '查询', '', '', '', 100, 3, '1002003', 'lb.order.after.sale.query_table', 'F', 'F', 'F', 'F', 'F', 'F', NOW(), NOW(), 1, 1, 'F', NULL, NULL, 'F'
FROM dual
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_menu` WHERE `id` = '8a4b2dbf9fa54f3b8820c0b3cbd11002'
);

INSERT INTO `sys_menu` (`id`, `pid`, `path`, `name`, `title`, `icon`, `component`, `redirect`, `sort`, `deep`, `menu_type_cd`, `permissions`, `is_hidden`, `has_children`, `is_link`, `is_full`, `is_affix`, `is_keep_alive`, `create_time`, `update_time`, `create_id`, `update_id`, `del_flag`, `delete_id`, `delete_time`, `use_data_scope`)
SELECT '8a4b2dbf9fa54f3b8820c0b3cbd11003', '8a4b2dbf9fa54f3b8820c0b3cbd11001', '', 'ShopOrderAfterSaleUpdate', '处理', '', '', '', 300, 3, '1002003', 'lb.order.after.sale.update', 'F', 'F', 'F', 'F', 'F', 'F', NOW(), NOW(), 1, 1, 'F', NULL, NULL, 'F'
FROM dual
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_menu` WHERE `id` = '8a4b2dbf9fa54f3b8820c0b3cbd11003'
);