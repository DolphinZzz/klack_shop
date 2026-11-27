package com.sz.mq.strategy.impl;

import com.sz.mq.config.MqProperties;
import com.sz.mq.core.MqJsonCodec;
import com.sz.mq.core.MqMessage;
import com.sz.mq.core.MqSendException;
import com.sz.mq.core.MqSendResult;
import com.sz.mq.core.MqTypeEnum;
import com.sz.mq.strategy.MqStrategy;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

/**
 * RocketMQ 发送策略实现
 */
@Slf4j
public class RocketMqStrategy implements MqStrategy {

    private final RocketMQTemplate rocketMQTemplate;
    private final MqProperties mqProperties;
    private final MqJsonCodec mqJsonCodec;

    public RocketMqStrategy(RocketMQTemplate rocketMQTemplate,
                            MqProperties mqProperties,
                            MqJsonCodec mqJsonCodec) {
        this.rocketMQTemplate = rocketMQTemplate;
        this.mqProperties = mqProperties;
        this.mqJsonCodec = mqJsonCodec;
    }

    @Override
    public MqTypeEnum getType() {
        return MqTypeEnum.ROCKETMQ;
    }

    @Override
    public <T> MqSendResult send(MqMessage<T> message) {
        String destination = buildDestination(message);
        Message<String> springMessage = buildSpringMessage(message);
        try {
            SendResult sendResult = rocketMQTemplate.syncSend(destination, springMessage, mqProperties.getSendTimeoutMs());
            if (sendResult == null || sendResult.getSendStatus() != SendStatus.SEND_OK) {
                SendStatus status = sendResult == null ? null : sendResult.getSendStatus();
                throw new MqSendException("RocketMQ 发送状态异常, messageId=" + message.getMessageId() + ", status=" + status);
            }
            return MqSendResult.builder()
                    .success(true)
                    .mqType(MqTypeEnum.ROCKETMQ)
                    .messageId(message.getMessageId())
                    .rawMessageId(sendResult.getMsgId())
                    .build();
        } catch (MqSendException e) {
            throw e;
        } catch (Exception e) {
            throw new MqSendException("RocketMQ 发送失败, destination=" + destination + ", messageId=" + message.getMessageId(), e);
        }
    }

    @Override
    public <T> void sendAsync(MqMessage<T> message) {
        String destination = buildDestination(message);
        Message<String> springMessage = buildSpringMessage(message);
        rocketMQTemplate.asyncSend(destination, springMessage, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.debug("[SZ-MQ][RocketMQ] 异步发送成功, messageId={}, msgId={}",
                        message.getMessageId(), sendResult == null ? null : sendResult.getMsgId());
            }

            @Override
            public void onException(Throwable e) {
                log.error("[SZ-MQ][RocketMQ] 异步发送失败, messageId={}, destination={}",
                        message.getMessageId(), destination, e);
            }
        }, mqProperties.getSendTimeoutMs());
    }

    private <T> String buildDestination(MqMessage<T> message) {
        return message.getTopic() + ":" + message.getTag();
    }

    private <T> Message<String> buildSpringMessage(MqMessage<T> message) {
        MessageBuilder<String> builder = MessageBuilder.withPayload(mqJsonCodec.toJson(message))
                .setHeader("sz-mq-message-id", message.getMessageId())
                .setHeader("sz-mq-tag", message.getTag());
        if (message.getBizKey() != null && !message.getBizKey().isBlank()) {
            builder.setHeader(RocketMQHeaders.KEYS, message.getBizKey());
        }
        return builder.build();
    }
}
