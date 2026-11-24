--liquibase formatted sql

--changeset car:20260330_010_01
--comment: 新增秒杀配置表
CREATE TABLE IF NOT EXISTS `shop_flash_sale` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '秒杀活动id',
    `product_id` bigint NOT NULL COMMENT '商品id',
    `flash_price` decimal(8,2) NOT NULL DEFAULT '0.00' COMMENT '秒杀价格',
    `limit_per_user` int NOT NULL DEFAULT '1' COMMENT '单用户限购数量',
    `reserve_seconds` int NOT NULL DEFAULT '900' COMMENT '预下单保留秒数',
    `status` tinyint NOT NULL DEFAULT '0' COMMENT '秒杀状态，0下架，1上架',
    `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
    `start_time` datetime NOT NULL COMMENT '秒杀开始时间',
    `end_time` datetime NOT NULL COMMENT '秒杀结束时间',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_del` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
    `version` int NOT NULL DEFAULT '0' COMMENT '版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_shop_flash_sale_product` (`product_id`, `is_del`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='秒杀配置表';

--changeset car:20260330_010_02
--comment: 初始化营销管理菜单
INSERT INTO `sys_menu` (`id`, `pid`, `path`, `name`, `title`, `icon`, `component`, `redirect`, `sort`, `deep`, `menu_type_cd`, `permissions`, `is_hidden`, `has_children`, `is_link`, `is_full`, `is_affix`, `is_keep_alive`, `create_time`, `update_time`, `create_id`, `update_id`, `del_flag`, `delete_id`, `delete_time`, `use_data_scope`)
SELECT '5d7fa9e4f49e4ad0a1d3f6c8b58f6a10', '6288292f0b0e466382ababe6fefb1ed1', '/shop_system/shopFlashSale', 'ShopFlashSaleMarketing', '营销管理', 'Histogram', '/shop_system/shopFlashSale/index', '', 950, 2, '1002002', '', 'F', 'T', 'F', 'F', 'F', 'T', NOW(), NOW(), 1, 1, 'F', NULL, NULL, 'F'
FROM dual
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_menu` WHERE `id` = '5d7fa9e4f49e4ad0a1d3f6c8b58f6a10'
);

INSERT INTO `sys_menu` (`id`, `pid`, `path`, `name`, `title`, `icon`, `component`, `redirect`, `sort`, `deep`, `menu_type_cd`, `permissions`, `is_hidden`, `has_children`, `is_link`, `is_full`, `is_affix`, `is_keep_alive`, `create_time`, `update_time`, `create_id`, `update_id`, `del_flag`, `delete_id`, `delete_time`, `use_data_scope`)
SELECT '2ec2b0bc3e934a21a81631f132f41001', '5d7fa9e4f49e4ad0a1d3f6c8b58f6a10', '', 'ShopFlashSaleMarketingQuery', '查询', '', '', '', 100, 3, '1002003', 'lb.flash.sale.query_table', 'F', 'F', 'F', 'F', 'F', 'F', NOW(), NOW(), 1, 1, 'F', NULL, NULL, 'F'
FROM dual
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_menu` WHERE `id` = '2ec2b0bc3e934a21a81631f132f41001'
);

INSERT INTO `sys_menu` (`id`, `pid`, `path`, `name`, `title`, `icon`, `component`, `redirect`, `sort`, `deep`, `menu_type_cd`, `permissions`, `is_hidden`, `has_children`, `is_link`, `is_full`, `is_affix`, `is_keep_alive`, `create_time`, `update_time`, `create_id`, `update_id`, `del_flag`, `delete_id`, `delete_time`, `use_data_scope`)
SELECT '4d45c092a6d64d55a2af7dc6d78d1002', '5d7fa9e4f49e4ad0a1d3f6c8b58f6a10', '', 'ShopFlashSaleMarketingUpdate', '修改', '', '', '', 300, 3, '1002003', 'lb.flash.sale.update', 'F', 'F', 'F', 'F', 'F', 'F', NOW(), NOW(), 1, 1, 'F', NULL, NULL, 'F'
FROM dual
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_menu` WHERE `id` = '4d45c092a6d64d55a2af7dc6d78d1002'
);

--changeset car:20260330_010_03
--comment: 初始化秒杀演示数据
UPDATE `shop_product`
SET `is_seckill` = 1,
    `activity` = '1',
    `browse` = CASE `id`
        WHEN 6 THEN 8800
        WHEN 8 THEN 6200
        WHEN 10 THEN 9600
        WHEN 24 THEN 4200
        ELSE IFNULL(`browse`, 0)
    END,
    `ficti_browse` = CASE `id`
        WHEN 6 THEN 12800
        WHEN 8 THEN 9500
        WHEN 10 THEN 18200
        WHEN 24 THEN 7600
        ELSE IFNULL(`ficti_browse`, 0)
    END,
    `sales` = CASE `id`
        WHEN 6 THEN 560
        WHEN 8 THEN 268
        WHEN 10 THEN 388
        WHEN 24 THEN 132
        ELSE `sales`
    END,
    `is_show` = 1
WHERE `id` IN (6, 8, 10, 24);

INSERT INTO `shop_flash_sale` (`product_id`, `flash_price`, `limit_per_user`, `reserve_seconds`, `status`, `sort`, `start_time`, `end_time`, `create_time`, `update_time`, `is_del`, `version`)
SELECT src.product_id,
       src.flash_price,
       src.limit_per_user,
       src.reserve_seconds,
       src.status,
       src.sort,
       src.start_time,
       src.end_time,
       NOW(),
       NOW(),
       0,
       0
FROM (
         SELECT 6 AS product_id, 39.90 AS flash_price, 1 AS limit_per_user, 900 AS reserve_seconds, 1 AS status, 100 AS sort,
                DATE_ADD(NOW(), INTERVAL -1 DAY) AS start_time, DATE_ADD(NOW(), INTERVAL 10 DAY) AS end_time
         UNION ALL
         SELECT 8, 42.80, 1, 900, 1, 90, DATE_ADD(NOW(), INTERVAL -1 DAY), DATE_ADD(NOW(), INTERVAL 7 DAY)
         UNION ALL
         SELECT 10, 88.00, 2, 1200, 1, 80, DATE_ADD(NOW(), INTERVAL -1 DAY), DATE_ADD(NOW(), INTERVAL 5 DAY)
         UNION ALL
         SELECT 24, 45.00, 1, 900, 1, 70, DATE_ADD(NOW(), INTERVAL -1 DAY), DATE_ADD(NOW(), INTERVAL 15 DAY)
     ) src
WHERE EXISTS (
    SELECT 1 FROM `shop_product` p WHERE p.`id` = src.product_id
)
  AND NOT EXISTS (
    SELECT 1 FROM `shop_flash_sale` fs WHERE fs.`product_id` = src.product_id AND fs.`is_del` = 0
);

--changeset car:20260331_010_04
--comment: SKU维度支持秒杀价
ALTER TABLE shop_product_attr_value
    ADD COLUMN flash_price decimal(8,2) DEFAULT NULL COMMENT 'SKU秒杀价' AFTER price;

--changeset car:20260331_010_05
--comment: 回填SKU版本号，兼容秒杀乐观锁
UPDATE shop_product_attr_value
SET version = 0
WHERE version IS NULL;
