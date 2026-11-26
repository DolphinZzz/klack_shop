--liquibase formatted sql

--changeset car:20261126_016_01
--comment: 初始化智能客服工单管理与知识库管理菜单及按钮权限
INSERT INTO `sys_menu` (`id`, `pid`, `path`, `name`, `title`, `icon`, `component`, `redirect`, `sort`, `deep`, `menu_type_cd`, `permissions`, `is_hidden`, `has_children`, `is_link`, `is_full`, `is_affix`, `is_keep_alive`, `create_time`, `update_time`, `create_id`, `update_id`, `del_flag`, `delete_id`, `delete_time`, `use_data_scope`)
SELECT '7b1a9e4c8d204f11a000000000000001', '6288292f0b0e466382ababe6fefb1ed1', '/shop/cs-ticket', 'ShopCsTicket', '客服工单管理', 'Service', '/shop_system/csTicket/index', '', 970, 2, '1002002', '', 'F', 'T', 'F', 'F', 'F', 'T', NOW(), NOW(), 1, 1, 'F', NULL, NULL, 'F'
FROM dual
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_menu` WHERE `id` = '7b1a9e4c8d204f11a000000000000001'
);

INSERT INTO `sys_menu` (`id`, `pid`, `path`, `name`, `title`, `icon`, `component`, `redirect`, `sort`, `deep`, `menu_type_cd`, `permissions`, `is_hidden`, `has_children`, `is_link`, `is_full`, `is_affix`, `is_keep_alive`, `create_time`, `update_time`, `create_id`, `update_id`, `del_flag`, `delete_id`, `delete_time`, `use_data_scope`)
SELECT '7b1a9e4c8d204f11a000000000000002', '7b1a9e4c8d204f11a000000000000001', '', 'ShopCsTicketQuery', '查询', '', '', '', 100, 3, '1002003', 'cs.ticket.query_table', 'F', 'F', 'F', 'F', 'F', 'F', NOW(), NOW(), 1, 1, 'F', NULL, NULL, 'F'
FROM dual
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_menu` WHERE `id` = '7b1a9e4c8d204f11a000000000000002'
);

INSERT INTO `sys_menu` (`id`, `pid`, `path`, `name`, `title`, `icon`, `component`, `redirect`, `sort`, `deep`, `menu_type_cd`, `permissions`, `is_hidden`, `has_children`, `is_link`, `is_full`, `is_affix`, `is_keep_alive`, `create_time`, `update_time`, `create_id`, `update_id`, `del_flag`, `delete_id`, `delete_time`, `use_data_scope`)
SELECT '7b1a9e4c8d204f11a000000000000003', '7b1a9e4c8d204f11a000000000000001', '', 'ShopCsTicketReply', '回复', '', '', '', 200, 3, '1002003', 'cs.ticket.reply', 'F', 'F', 'F', 'F', 'F', 'F', NOW(), NOW(), 1, 1, 'F', NULL, NULL, 'F'
FROM dual
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_menu` WHERE `id` = '7b1a9e4c8d204f11a000000000000003'
);

INSERT INTO `sys_menu` (`id`, `pid`, `path`, `name`, `title`, `icon`, `component`, `redirect`, `sort`, `deep`, `menu_type_cd`, `permissions`, `is_hidden`, `has_children`, `is_link`, `is_full`, `is_affix`, `is_keep_alive`, `create_time`, `update_time`, `create_id`, `update_id`, `del_flag`, `delete_id`, `delete_time`, `use_data_scope`)
SELECT '7b1a9e4c8d204f11a000000000000004', '7b1a9e4c8d204f11a000000000000001', '', 'ShopCsTicketClose', '关闭', '', '', '', 300, 3, '1002003', 'cs.ticket.close', 'F', 'F', 'F', 'F', 'F', 'F', NOW(), NOW(), 1, 1, 'F', NULL, NULL, 'F'
FROM dual
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_menu` WHERE `id` = '7b1a9e4c8d204f11a000000000000004'
);

INSERT INTO `sys_menu` (`id`, `pid`, `path`, `name`, `title`, `icon`, `component`, `redirect`, `sort`, `deep`, `menu_type_cd`, `permissions`, `is_hidden`, `has_children`, `is_link`, `is_full`, `is_affix`, `is_keep_alive`, `create_time`, `update_time`, `create_id`, `update_id`, `del_flag`, `delete_id`, `delete_time`, `use_data_scope`)
SELECT '7b1a9e4c8d204f11a000000000000011', '6288292f0b0e466382ababe6fefb1ed1', '/shop/cs-knowledge', 'ShopCsKnowledge', '客服知识库管理', 'Collection', '/shop_system/csKnowledge/index', '', 980, 2, '1002002', '', 'F', 'T', 'F', 'F', 'F', 'T', NOW(), NOW(), 1, 1, 'F', NULL, NULL, 'F'
FROM dual
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_menu` WHERE `id` = '7b1a9e4c8d204f11a000000000000011'
);

INSERT INTO `sys_menu` (`id`, `pid`, `path`, `name`, `title`, `icon`, `component`, `redirect`, `sort`, `deep`, `menu_type_cd`, `permissions`, `is_hidden`, `has_children`, `is_link`, `is_full`, `is_affix`, `is_keep_alive`, `create_time`, `update_time`, `create_id`, `update_id`, `del_flag`, `delete_id`, `delete_time`, `use_data_scope`)
SELECT '7b1a9e4c8d204f11a000000000000012', '7b1a9e4c8d204f11a000000000000011', '', 'ShopCsKnowledgeQuery', '查询', '', '', '', 100, 3, '1002003', 'cs.kb.query_table', 'F', 'F', 'F', 'F', 'F', 'F', NOW(), NOW(), 1, 1, 'F', NULL, NULL, 'F'
FROM dual
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_menu` WHERE `id` = '7b1a9e4c8d204f11a000000000000012'
);

INSERT INTO `sys_menu` (`id`, `pid`, `path`, `name`, `title`, `icon`, `component`, `redirect`, `sort`, `deep`, `menu_type_cd`, `permissions`, `is_hidden`, `has_children`, `is_link`, `is_full`, `is_affix`, `is_keep_alive`, `create_time`, `update_time`, `create_id`, `update_id`, `del_flag`, `delete_id`, `delete_time`, `use_data_scope`)
SELECT '7b1a9e4c8d204f11a000000000000013', '7b1a9e4c8d204f11a000000000000011', '', 'ShopCsKnowledgeUpload', '上传/重建', '', '', '', 200, 3, '1002003', 'cs.kb.upload', 'F', 'F', 'F', 'F', 'F', 'F', NOW(), NOW(), 1, 1, 'F', NULL, NULL, 'F'
FROM dual
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_menu` WHERE `id` = '7b1a9e4c8d204f11a000000000000013'
);

INSERT INTO `sys_menu` (`id`, `pid`, `path`, `name`, `title`, `icon`, `component`, `redirect`, `sort`, `deep`, `menu_type_cd`, `permissions`, `is_hidden`, `has_children`, `is_link`, `is_full`, `is_affix`, `is_keep_alive`, `create_time`, `update_time`, `create_id`, `update_id`, `del_flag`, `delete_id`, `delete_time`, `use_data_scope`)
SELECT '7b1a9e4c8d204f11a000000000000014', '7b1a9e4c8d204f11a000000000000011', '', 'ShopCsKnowledgeDelete', '删除', '', '', '', 300, 3, '1002003', 'cs.kb.delete', 'F', 'F', 'F', 'F', 'F', 'F', NOW(), NOW(), 1, 1, 'F', NULL, NULL, 'F'
FROM dual
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_menu` WHERE `id` = '7b1a9e4c8d204f11a000000000000014'
);

--changeset car:20261126_016_02
--comment: 为超级管理员角色幂等授权客服工单与知识库菜单权限
INSERT INTO `sys_role_menu` (`menu_id`, `role_id`)
SELECT m.`id`, 1
FROM `sys_menu` m
WHERE m.`id` IN (
    '7b1a9e4c8d204f11a000000000000001',
    '7b1a9e4c8d204f11a000000000000002',
    '7b1a9e4c8d204f11a000000000000003',
    '7b1a9e4c8d204f11a000000000000004',
    '7b1a9e4c8d204f11a000000000000011',
    '7b1a9e4c8d204f11a000000000000012',
    '7b1a9e4c8d204f11a000000000000013',
    '7b1a9e4c8d204f11a000000000000014'
)
  AND EXISTS (SELECT 1 FROM `sys_role` r WHERE r.`id` = 1)
  AND NOT EXISTS (
      SELECT 1 FROM `sys_role_menu` rm
      WHERE rm.`menu_id` = m.`id`
        AND rm.`role_id` = 1
        AND (rm.`permission_type` = 'menu' OR rm.`permission_type` IS NULL)
  );
