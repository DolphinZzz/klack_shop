CREATE TABLE IF NOT EXISTS `shop_user_favorite` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '收藏id',
  `uid` bigint NOT NULL COMMENT '用户id',
  `product_id` bigint NOT NULL COMMENT '商品id',
  `product_name` varchar(128) NOT NULL DEFAULT '' COMMENT '商品名称快照',
  `product_info` varchar(255) DEFAULT NULL COMMENT '商品简介快照',
  `product_image` varchar(255) DEFAULT NULL COMMENT '商品图片快照',
  `product_price` decimal(8,2) NOT NULL DEFAULT '0.00' COMMENT '商品价格快照',
  `give_integral` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '赠送积分快照',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `is_del` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除，0否，1是',
  `version` int DEFAULT '0' COMMENT '并发版本控制',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_shop_user_favorite_uid_product` (`uid`, `product_id`),
  KEY `idx_shop_user_favorite_uid_utime` (`uid`, `update_time` DESC),
  KEY `idx_shop_user_favorite_product` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户商品收藏表';
