--liquibase formatted sql

--changeset car:20260325_001
--comment: 新增用户积分流水表
CREATE TABLE IF NOT EXISTS `shop_user_integral_bill` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `uid` bigint NOT NULL COMMENT '用户id',
  `link_id` varchar(64) DEFAULT NULL COMMENT '关联id',
  `pm` tinyint NOT NULL DEFAULT '1' COMMENT '收支，0支出，1收入',
  `title` varchar(128) NOT NULL COMMENT '账单标题',
  `category` varchar(64) NOT NULL DEFAULT 'integral' COMMENT '明细种类',
  `type` varchar(64) DEFAULT NULL COMMENT '明细类型',
  `number` int NOT NULL DEFAULT '0' COMMENT '明细数字',
  `balance` int NOT NULL DEFAULT '0' COMMENT '剩余积分',
  `mark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态',
  PRIMARY KEY (`id`),
  KEY `idx_shop_user_integral_bill_uid` (`uid`),
  KEY `idx_shop_user_integral_bill_link_id` (`link_id`),
  KEY `idx_shop_user_integral_bill_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户积分流水表';
