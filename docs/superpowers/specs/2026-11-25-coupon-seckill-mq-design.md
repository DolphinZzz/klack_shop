# 优惠券抢购消息队列（MQ）架构改造设计文档

> **日期**：2026-11-25  
> **目标模块**：`sz-boot-parent`（`sz-dependencies`、`sz-common-mq`、`sz-service-admin`、`config/{dev,local,preview,prod}/mq.yml`）  
> **技术基线**：Java 21 + Spring Boot 3.5.5（`sz-boot-parent` `1.3.0-beta-SNAPSHOT`）  
> **选定架构**：方案 A（三引擎真实客户端完整实现） + 架构一（全链路统一策略抽象） + 彻底移除 `local` 阻塞队列 + 方式二（瞬时 DB 异常进死信不回滚 Redis，留待人工重放补单；业务确定性失败回滚 Redis）

---

## 1. 背景与目标

当前图书商城系统的优惠券抢购（`ShopProductCouponServiceImpl`）采用 `Redis Lua 原子预扣库存 + JVM 内存 LinkedBlockingQueue + 单线程异步写 MySQL` 方案。存在重启丢任务、无法水平扩展、缺少可靠确认与死信治理的问题。

### 1.1 目标
1. 彻底移除 `ShopProductCouponServiceImpl` 中的 JVM 内存 `LinkedBlockingQueue` 与单线程 `ExecutorService`，改用消息队列解耦 Redis 预扣库存与 MySQL 异步落库。
2. 在已预留的 `sz-common-mq` 模块中构建通用 MQ 策略抽象层，适配 **RabbitMQ、RocketMQ、Kafka** 三种消息队列，业务模块 `sz-service-admin` 对具体 MQ API 与注解 **零依赖、零侵入**。
3. 遵循 Sz-Admin 外部配置拆分规范，在 `config/{dev,local,preview,prod}/mq.yml` 中仅修改 `sz.mq.type` 即可切换引擎，未选中的 MQ 自动配置与 Actuator 健康检查被自动屏蔽。
4. 优先完成本地 Docker `rabbitmq:3.13.7`（`127.0.0.1:5672`，`admin/admin`，vhost `/`）全链路端到端验证。

### 1.2 非目标与约束
- **不保留** JVM 阻塞队列作为 `local` 兜底策略（仅支持 `rabbitmq` / `rocketmq` / `kafka`）。
- **不修改**现有数据库表结构（`shop_product_coupon_user` 存在历史重复记录，不加 `(cid, uid)` 唯一索引）。
- **不改变**前端抢券接口协议（`POST /client/shopProductCoupon/grab/{couponId}` 仍即时返回 `ORDCP...` 订单号）及 `lua/coupon_seckill.lua`。
- 依赖严格兼容 **Spring Boot 3.5.5 / Java 21**：`spring-boot-starter-amqp`、`spring-kafka` 跟随 Spring Boot 3.5.5，`org.apache.rocketmq:rocketmq-spring-boot-starter` 使用 `2.3.4`。
- MQ 层使用独立纯净 `ObjectMapper`（`MqJsonCodec`），绕开 `sz-common-core` 全局 `null -> ""` 序列化修改器。
- 所有逻辑 `topic`、`group`、`tag` 命名必须匹配 `^[a-zA-Z0-9_-]+$`（兼容 RocketMQ 禁止 `.` 的校验规则）。

---

## 2. 失败补偿与死信策略（方式二）

1. **生产端发送失败**（Broker 不可达 / Publisher NACK / 路由不可达 Return / 发送超时）：`grabCoupon` 立即执行幂等安全 Lua 回滚 Redis，并向客户端抛出业务异常。
2. **消费端确定性业务失败（`MqNonRetryableException`）**（如券不存在、已禁用、已过期、DB 库存不足）：跳过重试，调用 `onConsumeFailed(message, cause, false)` 触发幂等安全 Lua 回滚 Redis（若库存 Key 已被禁用删除，则只清理已购 Set 不复活库存 Key），同时转入死信队列留痕。
3. **消费端瞬时/系统异常**（如 DB 连接池超时、锁等待超时）：执行有限次重试（`maxRetry`，默认 3 次）；若重试耗尽仍失败，调用 `onConsumeFailed(message, cause, true)`——**不回滚 Redis**（保留占坑库存与已购标记），打印带 `messageId`/`orderId`/`couponId`/`userId` 的高优告警日志，并将消息转入死信队列（RabbitMQ DLQ / Kafka DLT / RocketMQ `%DLQ%`）等待 DB 恢复后人工重放补单。

---

## 3. `sz-common-mq` 模块设计

- **核心类型**：`MqTypeEnum`、`MqMessage<T>`、`MqSendResult`、`MqJsonCodec`、`MqSendException`、`MqNonRetryableException`
- **策略抽象**：`MqStrategy`、`MqTemplate`（`EnumMap` 路由；`sz.mq.enabled=false` 时允许启动并在 `send()` 时抛 `MqSendException`）
- **消费抽象**：`MqConsumerHandler<T>`（含 `onConsumeFailed(message, cause, retryable)`）、`MqConsumerContainerRegistrar`、`MqConsumerLifecycle`（`SmartLifecycle`，校验命名正则与 `(topic, group)` 全局唯一性）
- **隔离机制**：`MqAutoConfigurationImportFilter`（在 `META-INF/spring.factories` 注册）按 `sz.mq.enabled` 与 `sz.mq.type` 屏蔽未选中 MQ 的 Spring Boot 自动配置及 Actuator 健康/指标自动配置。
- **三引擎规格**：
  - **RabbitMQ**：`Declarables` 自动声明 `sz.mq.direct.{topic}`、`sz.mq.dlx.{topic}`、`sz.mq.queue.{topic}.{group}`（DLQ 路由键 `{group}.{tag}.dlq`）、`sz.mq.dlq.{topic}.{group}`；`PERSISTENT` + Publisher Confirms + Returns；`AcknowledgeMode.MANUAL` + 指数退避重试 + `basicNack(false, false)` 入 DLQ。
  - **RocketMQ**：`RocketMQTemplate.syncSend(topic:tag, msg, timeout)` + `KEYS`；`DefaultMQPushConsumer`（`sz-mq-consumer-{group}`，`maxReconsumeTimes=3`）；`MqNonRetryableException` 调 `onConsumeFailed(..., false)` 返回 `CONSUME_SUCCESS`；瞬时异常达上限调 `onConsumeFailed(..., true)` 后返回 `RECONSUME_LATER` 进 `%DLQ%`。
  - **Kafka**：`KafkaTemplate<String, String>` 同步发送 + `bizKey` 分区键 + Headers；`ConcurrentMessageListenerContainer`（`AckMode.MANUAL_IMMEDIATE`）+ `KafkaAdmin` 按需建 `{topic}` 与 `{topic}-dlt`；终态失败按 `retryable` 调 `onConsumeFailed` 并投递 DLT 后提交位点。

---

## 4. `sz-service-admin` 优惠券业务改造

- 新增 `com.sz.admin.shop_system.mq`：
  - `CouponSeckillMqConstants`（`TOPIC="coupon-seckill"`, `TAG_GRAB="grab"`, `GROUP="coupon-seckill-group"`）
  - `CouponSeckillMessage`（`orderId`, `couponId`, `userId`, `requestTimeMillis`）
  - `CouponSeckillRedisHelper`（封装 `lua/coupon_seckill.lua` 与 `lua/coupon_seckill_rollback.lua`：`SREM == 1` 且 `EXISTS stockKey == 1` 才 `INCRBY +1`）
  - `CouponSeckillOrderConsumer`（`concurrency=1`，事务内幂等校验 → 状态/窗口校验 → 原子扣库存 → 写领券记录；仅 `!retryable` 时回滚 Redis）
- 重构 `ShopProductCouponServiceImpl`：彻底删除 JVM `BlockingQueue`、`ExecutorService` 及消费线程代码，改为 `MqTemplate` 同步发送与失败即回滚。
- 更新 `ShopUserBatchGenerateTest` 与 `ShopUserTokenBatchGenerateTest`：补充 `sz.mq.enabled=false`。

---

## 5. MQ 发送/消费记录入库闭环与后台管理端页面（方案 2）

- **数据库表**（Liquibase `015_mq_message_log.sql`）：
  - `sys_mq_send_log`：`message_id`（唯一）、`mq_type`、`topic`、`tag`、`biz_key`、`payload`、`send_status`（`PENDING`/`SENT`/`FAILED`）、`raw_message_id`、`error_msg`、`sent_time`、`create_time`、`update_time`。
  - `sys_mq_consume_log`：`(message_id, consumer_group)` 唯一索引、`mq_type`、`topic`、`tag`、`biz_key`、`payload`、`consume_status`（`CONSUMING`/`SUCCESS`/`RETRYING`/`DEAD`）、`ack_status`（`PENDING`/`ACKED`/`NACKED_DLQ`/`SENT_DLT`/`BROKER_DLQ`）、`retry_count`、`error_msg`、`receive_time`、`consume_time`、`ack_time`、`create_time`、`update_time`。
- **SPI 扩展（保持 `sz-common-mq` 零 DB 依赖）**：
  - `com.sz.mq.store.MqMessageLogStore` + `MqConsumeAttemptDecision`（`PROCEED` / `ALREADY_CONSUMED`）；
  - 发送前先写 `PENDING`（写库失败则直接中止并回滚 Redis），Broker 确认后改 `SENT`，发送失败改 `FAILED` 并立即向上抛出异常触发 Redis 回滚（方案 2，无定时补发）；
  - 消费前调用 `tryBeginConsume` 按 `(message_id, consumer_group)` 去重：若已是 `SUCCESS` 直接返回 `ALREADY_CONSUMED` 并补发 Broker ACK（`ACKED`）；若之前为 `DEAD`（死信重放）则重置为 `CONSUMING` 允许重新消费；业务落库成功后先置 `SUCCESS` 再发 Broker ACK，最后更新 `ack_status = ACKED`；死信时更新为 `DEAD` 与对应死信 ACK 状态。
- **后台管理端查询模块**：
  - 菜单「系统管理 / MQ消息记录」（`/system/sysMqLog`，权限 `sys.mq.log.query_table`）；
  - 后端 `SysMqLogController`（`/sys-mq-log/send`、`/sys-mq-log/consume` 分页与详情）；
  - 前端 `sz-admin/src/views/system/sysMqLog/index.vue` 单模块双 Tab（发送记录 / 消费记录），支持详情弹窗与发送记录一键「追踪消费」。
