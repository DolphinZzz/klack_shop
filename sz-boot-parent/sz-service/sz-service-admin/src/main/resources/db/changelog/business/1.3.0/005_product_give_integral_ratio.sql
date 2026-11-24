--liquibase formatted sql

--changeset car:20260325_005
--comment: 调整商品赠送积分字段注释
ALTER TABLE `shop_product`
    MODIFY COLUMN `give_integral` decimal(8,2) NOT NULL DEFAULT '0.00' COMMENT '赠送积分，按商品金额x100计算';
