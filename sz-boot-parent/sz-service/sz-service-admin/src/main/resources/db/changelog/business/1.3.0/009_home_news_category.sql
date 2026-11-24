--liquibase formatted sql

--changeset car:20260328_0003
CREATE TABLE IF NOT EXISTS `shop_home_news_category` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '分类id',
  `category_name` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '分类名称',
  `sort` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-正常 1-禁用',
  `remark` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '备注',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_del` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记：0-否 1-是',
  `version` INT NOT NULL DEFAULT 0 COMMENT '并发版本',
  PRIMARY KEY (`id`),
  KEY `idx_status_sort` (`status`, `sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='首页资讯分类表';

INSERT INTO `shop_home_news_category` (`category_name`, `sort`, `status`, `remark`)
VALUES
('技术贴', 10, 0, '默认分类'),
('供应链', 20, 0, '默认分类'),
('新闻报道', 30, 0, '默认分类')
ON DUPLICATE KEY UPDATE `category_name` = VALUES(`category_name`);

--changeset car:20260328_0004
ALTER TABLE `shop_home_news`
  ADD COLUMN `category_id` BIGINT NULL DEFAULT NULL COMMENT '分类id' AFTER `id`,
  ADD COLUMN `browse_count` INT NOT NULL DEFAULT 0 COMMENT '浏览量' AFTER `author`;

CREATE INDEX `idx_category_publish` ON `shop_home_news` (`category_id`, `publish_time`, `id`);
CREATE INDEX `idx_browse_count` ON `shop_home_news` (`browse_count`, `id`);

UPDATE `shop_home_news`
SET `category_id` = (
  SELECT `id` FROM `shop_home_news_category`
  WHERE `category_name` = CASE
    WHEN `shop_home_news`.`tag_name` = '活动' THEN '新闻报道'
    ELSE '技术贴'
  END
  LIMIT 1
)
WHERE `category_id` IS NULL;

UPDATE `shop_home_news`
SET `tag_name` = (
  SELECT `category_name` FROM `shop_home_news_category`
  WHERE `id` = `shop_home_news`.`category_id`
  LIMIT 1
)
WHERE `category_id` IS NOT NULL;

--changeset car:20260328_0005
INSERT IGNORE INTO `sys_menu`
(`id`, `pid`, `path`, `name`, `title`, `icon`, `component`, `redirect`, `sort`, `deep`, `menu_type_cd`, `permissions`, `is_hidden`, `has_children`, `is_link`, `is_full`, `is_affix`, `is_keep_alive`, `del_flag`, `use_data_scope`)
VALUES
('67d872040e1b4770b4e1c6ee833ce66c', '6288292f0b0e466382ababe6fefb1ed1', '/shop_system/shopHomeNewsCategory', 'ShopHomeNewsCategoryView', '资讯分类管理', 'CollectionTag', '/shop_system/shopHomeNewsCategory/index', '', 1490, 2, '1002002', '', 'F', 'T', 'F', 'F', 'F', 'F', 'F', 'F'),
('f2f2d47ce5dd459fa20dd5cad4d6eae1', '67d872040e1b4770b4e1c6ee833ce66c', '', '', '查询', '', '', '', 100, 3, '1002003', 'lb.home.news.category.query_table', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F'),
('2662805d9ab74e2f9c9af6327af65bf3', '67d872040e1b4770b4e1c6ee833ce66c', '', '', '新增', '', '', '', 200, 3, '1002003', 'lb.home.news.category.create', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F'),
('660d55d387754a43a8ebc0d639a9efd2', '67d872040e1b4770b4e1c6ee833ce66c', '', '', '修改', '', '', '', 300, 3, '1002003', 'lb.home.news.category.update', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F'),
('cfeef29fd59f4d33b85bb04572225532', '67d872040e1b4770b4e1c6ee833ce66c', '', '', '删除', '', '', '', 400, 3, '1002003', 'lb.home.news.category.remove', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F');
