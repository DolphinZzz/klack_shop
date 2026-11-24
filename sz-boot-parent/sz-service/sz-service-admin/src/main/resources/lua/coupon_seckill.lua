-- 优惠券秒杀资格校验与预扣减 Lua 脚本
-- KEYS[1]: 优惠券库存 Key（coupon:vid:{couponId}）
-- KEYS[2]: 已购用户 Set Key（order:vid:{couponId}）
-- ARGV[1]: 用户 ID
-- 返回值：
--   0: 满足条件，已预扣 Redis 库存并将用户 ID 存入 Set
--   1: 库存不足
--   2: 该优惠券此用户已下过单

local stockKey = KEYS[1]
local orderKey = KEYS[2]
local userId = ARGV[1]

-- 1. 判断库存是否充足
local stock = redis.call('get', stockKey)
if (not stock or tonumber(stock) <= 0) then
    return 1
end

-- 2. 判断该用户是否已下过单（一人一单）
if (redis.call('sismember', orderKey, userId) == 1) then
    return 2
end

-- 3. 预扣减 Redis 库存
redis.call('incrby', stockKey, -1)

-- 4. 将用户 ID 存入当前优惠券的 Set 集合
redis.call('sadd', orderKey, userId)

return 0
