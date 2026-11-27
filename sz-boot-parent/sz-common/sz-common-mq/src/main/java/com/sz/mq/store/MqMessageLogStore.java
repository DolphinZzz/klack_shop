package com.sz.mq.store;

import com.sz.mq.core.MqMessage;
import com.sz.mq.core.MqSendResult;
import com.sz.mq.core.MqTypeEnum;

/**
 * MQ 发送与消费记录持久化扩展接口（由业务服务实现，保持 sz-common-mq 零数据库依赖）
 */
public interface MqMessageLogStore {

    void recordSendPending(MqMessage<?> message, MqTypeEnum mqType, String payloadJson);

    void recordSendSuccess(String messageId, MqSendResult result);

    void recordSendFailure(String messageId, Throwable cause);

    MqConsumeAttemptDecision tryBeginConsume(MqMessage<?> message, String consumerGroup, MqTypeEnum mqType, String payloadJson);

    void recordConsumeSuccess(String messageId, String consumerGroup);

    void recordConsumeRetry(String messageId, String consumerGroup, int retryCount, Throwable cause);

    void recordConsumeDead(String messageId, String consumerGroup, int retryCount, String ackStatus, Throwable cause);

    void recordAck(String messageId, String consumerGroup, String ackStatus);
}
