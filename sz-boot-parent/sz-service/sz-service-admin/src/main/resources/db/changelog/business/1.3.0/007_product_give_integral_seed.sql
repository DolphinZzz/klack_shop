--liquibase formatted sql

--changeset car:20260325_007
--comment: 按商品价格回填赠送积分
UPDATE `shop_product`
SET `give_integral` = ROUND(`price` * 100, 2)
WHERE `is_del` = 0;
