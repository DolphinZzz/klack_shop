package com.sz.mq.consumer;

import com.sz.mq.core.MqMessage;

/**
 * 业务侧实现的统一 MQ 消费者接口（业务层无需感知 RabbitMQ / RocketMQ / Kafka API）
 *
 * @param <T> 消息载荷类型
 */
public interface MqConsumerHandler<T> {

    String topic();

    default String tag() {
        return "default";
    }

    String group();

    Class<T> payloadType();

    /**
     * 消费并发度；<= 0 时使用全局配置 sz.mq.consumer.concurrency
     */
    default int concurrency() {
        return -1;
    }

    void onMessage(MqMessage<T> message);

    /**
     * 消费最终失败回调：
     * <ul>
     *   <li>retryable = false：确定性业务失败（抛出了 MqNonRetryableException），应补偿回滚；</li>
     *   <li>retryable = true：瞬时系统异常重试耗尽，进入死信队列待人工重放补单。</li>
     * </ul>
     */
    default void onConsumeFailed(MqMessage<T> message, Throwable cause, boolean retryable) {
    }
}
