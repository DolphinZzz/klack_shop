-- 首页轮播配置表
-- 执行库：sz_service_admin 对应业务库

CREATE TABLE IF NOT EXISTS `shop_home_banner` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '轮播id',
  `title` VARCHAR(120) NOT NULL DEFAULT '' COMMENT '标题',
  `image` VARCHAR(500) NOT NULL DEFAULT '' COMMENT '图片',
  `link` VARCHAR(500) NOT NULL DEFAULT '' COMMENT '跳转链接',
  `sort` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-正常 1-禁用',
  `start_time` DATETIME NULL DEFAULT NULL COMMENT '开始时间',
  `end_time` DATETIME NULL DEFAULT NULL COMMENT '结束时间',
  `remark` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '备注',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_del` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记：0-否 1-是',
  `version` INT NOT NULL DEFAULT 0 COMMENT '并发版本',
  PRIMARY KEY (`id`),
  KEY `idx_status_sort` (`status`, `sort`),
  KEY `idx_time_range` (`start_time`, `end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='首页轮播配置表';

-- 示例数据（按需保留）
INSERT INTO `shop_home_banner` (`title`, `image`, `link`, `sort`, `status`, `remark`)
VALUES
('新品上架', '', '/home', 10, 0, '默认轮播图'),
('热销推荐', '', '/home', 20, 0, '默认轮播图');
