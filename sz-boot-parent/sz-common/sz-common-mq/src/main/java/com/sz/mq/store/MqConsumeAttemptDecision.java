package com.sz.mq.store;

/**
 * 消费准入幂等判定结果
 */
public enum MqConsumeAttemptDecision {

    /**
     * 允许执行本次业务消费
     */
    PROCEED,

    /**
     * 该消息已被同一消费组成功消费，跳过业务直接发送 Broker ACK
     */
    ALREADY_CONSUMED
}
