--liquibase formatted sql

--changeset car:20261123_013_01
--comment: 删除 teacher 演示模块（关联数据、菜单、角色、生成器配置、业务表）
DELETE FROM `sys_data_role_menu` WHERE `role_id` IN (3,4,5,6,7) OR `menu_id` IN ('85b54322630f43a39296488a5e76ba16','7391f12ad51049c2b86d231d39708c71','73d312f4fa8949ddba3d9807c0c56f00','8061d8e79be744bf91b7b438f8e8e887','91ccb13b5c174583803a4c492a5dfdb6','ab1ac16c617d41979472ebe433c1f8e4','cb3500315dba4c2d83e4d92edf36dff7');
DELETE FROM `sys_data_role_relation` WHERE `role_id` IN (3,4,5,6,7) OR `menu_id` IN ('85b54322630f43a39296488a5e76ba16','7391f12ad51049c2b86d231d39708c71','73d312f4fa8949ddba3d9807c0c56f00','8061d8e79be744bf91b7b438f8e8e887','91ccb13b5c174583803a4c492a5dfdb6','ab1ac16c617d41979472ebe433c1f8e4','cb3500315dba4c2d83e4d92edf36dff7');
DELETE FROM `sys_role_menu` WHERE `role_id` IN (3,4,5,6,7) OR `menu_id` IN ('85b54322630f43a39296488a5e76ba16','7391f12ad51049c2b86d231d39708c71','73d312f4fa8949ddba3d9807c0c56f00','8061d8e79be744bf91b7b438f8e8e887','91ccb13b5c174583803a4c492a5dfdb6','ab1ac16c617d41979472ebe433c1f8e4','cb3500315dba4c2d83e4d92edf36dff7');
DELETE FROM `sys_dept_role` WHERE `role_id` IN (3,4,5,6,7);
DELETE FROM `sys_user_data_role` WHERE `role_id` IN (3,4,5,6,7);
DELETE FROM `sys_user_role` WHERE `role_id` IN (3,4,5,6,7);
DELETE FROM `sys_role` WHERE `id` IN (3,4,5,6,7) AND `permissions` LIKE 'teacher_statics_menu%';
DELETE FROM `sys_menu` WHERE `id` IN ('85b54322630f43a39296488a5e76ba16','7391f12ad51049c2b86d231d39708c71','73d312f4fa8949ddba3d9807c0c56f00','8061d8e79be744bf91b7b438f8e8e887','91ccb13b5c174583803a4c492a5dfdb6','ab1ac16c617d41979472ebe433c1f8e4','cb3500315dba4c2d83e4d92edf36dff7');
DELETE c FROM `generator_table_column` c JOIN `generator_table` t ON c.`table_id` = t.`table_id` WHERE t.`table_name` = 'teacher_statistics';
DELETE FROM `generator_table` WHERE `table_name` = 'teacher_statistics';
DROP TABLE IF EXISTS `teacher_statistics`;

