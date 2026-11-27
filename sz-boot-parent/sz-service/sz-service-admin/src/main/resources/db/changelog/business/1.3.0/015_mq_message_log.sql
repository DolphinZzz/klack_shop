--liquibase formatted sql

--changeset car:20261125_015_01
--comment: 新增 MQ 消息发送记录表
CREATE TABLE IF NOT EXISTS `sys_mq_send_log` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `message_id` varchar(64) NOT NULL COMMENT '全局消息ID',
    `mq_type` varchar(32) NOT NULL COMMENT 'MQ引擎类型：rabbitmq / rocketmq / kafka',
    `topic` varchar(128) NOT NULL COMMENT '消息主题',
    `tag` varchar(128) DEFAULT 'default' COMMENT '消息标签/路由键',
    `biz_key` varchar(128) DEFAULT NULL COMMENT '业务键（如订单号）',
    `payload` text COMMENT '消息JSON载荷',
    `send_status` varchar(32) NOT NULL DEFAULT 'PENDING' COMMENT '发送状态：PENDING待发送 SENT已发送 FAILED发送失败',
    `raw_message_id` varchar(128) DEFAULT NULL COMMENT 'Broker返回原始消息ID或偏移量',
    `error_msg` varchar(1000) DEFAULT NULL COMMENT '发送失败错误信息',
    `sent_time` datetime DEFAULT NULL COMMENT 'Broker确认发送成功时间',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_mq_send_message_id` (`message_id`),
    KEY `idx_mq_send_biz_key` (`biz_key`),
    KEY `idx_mq_send_topic_status` (`topic`, `send_status`),
    KEY `idx_mq_send_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='MQ消息发送记录表';

--changeset car:20261125_015_02
--comment: 新增 MQ 消息消费与ACK记录表
CREATE TABLE IF NOT EXISTS `sys_mq_consume_log` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `message_id` varchar(64) NOT NULL COMMENT '全局消息ID',
    `consumer_group` varchar(128) NOT NULL COMMENT '消费组名称',
    `mq_type` varchar(32) NOT NULL COMMENT 'MQ引擎类型：rabbitmq / rocketmq / kafka',
    `topic` varchar(128) NOT NULL COMMENT '消息主题',
    `tag` varchar(128) DEFAULT 'default' COMMENT '消息标签/路由键',
    `biz_key` varchar(128) DEFAULT NULL COMMENT '业务键（如订单号）',
    `payload` text COMMENT '消息JSON载荷',
    `consume_status` varchar(32) NOT NULL DEFAULT 'CONSUMING' COMMENT '消费状态：CONSUMING消费中 SUCCESS消费成功 RETRYING重试中 DEAD进入死信',
    `ack_status` varchar(32) NOT NULL DEFAULT 'PENDING' COMMENT 'ACK确认状态：PENDING待确认 ACKED已ACK NACKED_DLQ死信NACK SENT_DLT死信DLT BROKER_DLQ Broker死信',
    `retry_count` int NOT NULL DEFAULT '0' COMMENT '已重试次数',
    `error_msg` varchar(1000) DEFAULT NULL COMMENT '消费异常信息',
    `receive_time` datetime DEFAULT NULL COMMENT '首次接收消息时间',
    `consume_time` datetime DEFAULT NULL COMMENT '业务消费完成时间',
    `ack_time` datetime DEFAULT NULL COMMENT 'ACK/死信确认时间',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_mq_consume_msg_group` (`message_id`, `consumer_group`),
    KEY `idx_mq_consume_biz_key` (`biz_key`),
    KEY `idx_mq_consume_topic_status` (`topic`, `consume_status`),
    KEY `idx_mq_consume_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='MQ消息消费记录表';

--changeset car:20261125_015_03
--comment: 初始化系统管理-MQ消息记录菜单及查询权限
INSERT INTO `sys_menu` (`id`, `pid`, `path`, `name`, `title`, `icon`, `component`, `redirect`, `sort`, `deep`, `menu_type_cd`, `permissions`, `is_hidden`, `has_children`, `is_link`, `is_full`, `is_affix`, `is_keep_alive`, `create_time`, `update_time`, `create_id`, `update_id`, `del_flag`, `delete_id`, `delete_time`, `use_data_scope`)
SELECT '9c5e7a2b4d8f4e1a9b3c6d7e8f0a1501', '88b2e5def2ff474fa8bf3537d4a2fe5b', '/system/sysMqLog', 'SysMqLogView', 'MQ消息记录', 'MessageBox', '/system/sysMqLog/index', '', 1200, 2, '1002002', '', 'F', 'T', 'F', 'F', 'F', 'T', NOW(), NOW(), 1, 1, 'F', NULL, NULL, 'F'
FROM dual
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_menu` WHERE `id` = '9c5e7a2b4d8f4e1a9b3c6d7e8f0a1501'
);

INSERT INTO `sys_menu` (`id`, `pid`, `path`, `name`, `title`, `icon`, `component`, `redirect`, `sort`, `deep`, `menu_type_cd`, `permissions`, `is_hidden`, `has_children`, `is_link`, `is_full`, `is_affix`, `is_keep_alive`, `create_time`, `update_time`, `create_id`, `update_id`, `del_flag`, `delete_id`, `delete_time`, `use_data_scope`)
SELECT '9c5e7a2b4d8f4e1a9b3c6d7e8f0a1502', '9c5e7a2b4d8f4e1a9b3c6d7e8f0a1501', '', 'SysMqLogQuery', '查询', '', '', '', 100, 3, '1002003', 'sys.mq.log.query_table', 'F', 'F', 'F', 'F', 'F', 'F', NOW(), NOW(), 1, 1, 'F', NULL, NULL, 'F'
FROM dual
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_menu` WHERE `id` = '9c5e7a2b4d8f4e1a9b3c6d7e8f0a1502'
);
