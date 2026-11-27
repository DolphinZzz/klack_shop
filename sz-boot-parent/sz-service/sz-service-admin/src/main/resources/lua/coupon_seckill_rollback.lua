-- 优惠券秒杀 Redis 幂等安全回滚 Lua 脚本
-- KEYS[1]: 优惠券库存 Key（coupon:vid:{couponId}）
-- KEYS[2]: 已购用户 Set Key（order:vid:{couponId}）
-- ARGV[1]: 用户 ID
-- 返回值：
--   1: 已移出已购集合并原子恢复库存 +1
--   2: 已移出已购集合，但库存 Key 已不存在（优惠券已禁用/删除），跳过加库存防止复活
--   0: 用户不在已购集合中（已回滚过或未预扣），幂等跳过

local stockKey = KEYS[1]
local orderKey = KEYS[2]
local userId = ARGV[1]

if (redis.call('srem', orderKey, userId) == 1) then
    if (redis.call('exists', stockKey) == 1) then
        redis.call('incrby', stockKey, 1)
        return 1
    end
    return 2
end

return 0
