package com.sz.mq.strategy;

import com.sz.mq.core.MqMessage;
import com.sz.mq.core.MqSendResult;
import com.sz.mq.core.MqTypeEnum;

/**
 * 消息队列发送策略接口
 */
public interface MqStrategy {

    MqTypeEnum getType();

    <T> MqSendResult send(MqMessage<T> message);

    <T> void sendAsync(MqMessage<T> message);
}
