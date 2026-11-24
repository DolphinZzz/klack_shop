--liquibase formatted sql

--changeset car:20261124_014_01
--comment: 将 sys_menu 中 lb. 开头的权限标识统一替换为 shop.
UPDATE `sys_menu`
SET `permissions` = CONCAT('shop.', SUBSTRING(`permissions`, 4)),
    `update_time` = NOW()
WHERE `permissions` LIKE 'lb.%';
