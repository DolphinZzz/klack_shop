--liquibase formatted sql

--changeset car:20260328_0001
CREATE TABLE IF NOT EXISTS `shop_home_news` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '资讯id',
  `title` VARCHAR(160) NOT NULL DEFAULT '' COMMENT '标题',
  `tag_name` VARCHAR(32) NOT NULL DEFAULT '资讯' COMMENT '资讯标签',
  `cover_image` VARCHAR(500) NOT NULL DEFAULT '' COMMENT '封面图',
  `summary` VARCHAR(500) NOT NULL DEFAULT '' COMMENT '摘要',
  `content` LONGTEXT NULL COMMENT '正文内容',
  `link` VARCHAR(500) NOT NULL DEFAULT '' COMMENT '跳转链接',
  `source` VARCHAR(120) NOT NULL DEFAULT '' COMMENT '来源',
  `author` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '作者',
  `sort` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `is_top` TINYINT NOT NULL DEFAULT 0 COMMENT '是否置顶：0-否 1-是',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-正常 1-禁用',
  `publish_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  `start_time` DATETIME NULL DEFAULT NULL COMMENT '开始时间',
  `end_time` DATETIME NULL DEFAULT NULL COMMENT '结束时间',
  `remark` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '备注',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_del` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记：0-否 1-是',
  `version` INT NOT NULL DEFAULT 0 COMMENT '并发版本',
  PRIMARY KEY (`id`),
  KEY `idx_status_top_sort_publish` (`status`, `is_top`, `sort`, `publish_time`),
  KEY `idx_time_range` (`start_time`, `end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='首页资讯配置表';

INSERT INTO `shop_home_news`
(`title`, `tag_name`, `summary`, `content`, `link`, `source`, `author`, `sort`, `is_top`, `status`, `publish_time`, `remark`)
VALUES
('图书商城新版首页已上线', '资讯', '新版首页支持轮播、分类导航和新品推荐展示。', '<p>图书商城 PC 首页已完成新版升级，支持首页轮播、分类导航和新品推荐展示。</p>', '', '图书商城', '系统', 10, 1, 0, NOW(), '默认资讯'),
('热销图书持续补货中', '资讯', '热销图书正在持续补货，欢迎关注库存动态。', '<p>当前热门图书正在持续补货，库存会根据到货情况动态更新。</p>', '', '图书商城', '系统', 20, 0, 0, NOW(), '默认资讯'),
('阅读活动每周更新', '活动', '阅读打卡和主题书单活动会按周更新。', '<p>平台阅读活动将按周更新，欢迎持续关注并参与。</p>', '', '图书商城', '系统', 30, 0, 0, NOW(), '默认资讯');

--changeset car:20260328_0002
INSERT IGNORE INTO `sys_menu`
(`id`, `pid`, `path`, `name`, `title`, `icon`, `component`, `redirect`, `sort`, `deep`, `menu_type_cd`, `permissions`, `is_hidden`, `has_children`, `is_link`, `is_full`, `is_affix`, `is_keep_alive`, `del_flag`, `use_data_scope`)
VALUES
('7cfde0afb11b4e9ba98ad6adf7da44e2', '6288292f0b0e466382ababe6fefb1ed1', '/shop_system/shopHomeNews', 'ShopHomeNewsView', '首页资讯管理', 'Bell', '/shop_system/shopHomeNews/index', '', 1500, 2, '1002002', '', 'F', 'T', 'F', 'F', 'F', 'F', 'F', 'F'),
('22ebbcf061af4682a82f12b7d2cb31d9', '7cfde0afb11b4e9ba98ad6adf7da44e2', '', '', '查询', '', '', '', 100, 3, '1002003', 'lb.home.news.query_table', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F'),
('d0e92cb1f4cb4622989b9fc8d0a5d06d', '7cfde0afb11b4e9ba98ad6adf7da44e2', '', '', '新增', '', '', '', 200, 3, '1002003', 'lb.home.news.create', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F'),
('be55d315ba144239baf071e351e35ae6', '7cfde0afb11b4e9ba98ad6adf7da44e2', '', '', '修改', '', '', '', 300, 3, '1002003', 'lb.home.news.update', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F'),
('abf8b0e5ab4a4c7bb5c0566e425fedf9', '7cfde0afb11b4e9ba98ad6adf7da44e2', '', '', '删除', '', '', '', 400, 3, '1002003', 'lb.home.news.remove', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F');
