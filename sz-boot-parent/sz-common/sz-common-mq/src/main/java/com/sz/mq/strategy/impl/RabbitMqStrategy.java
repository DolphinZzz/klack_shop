package com.sz.mq.strategy.impl;

import com.sz.mq.config.MqProperties;
import com.sz.mq.core.MqJsonCodec;
import com.sz.mq.core.MqMessage;
import com.sz.mq.core.MqSendException;
import com.sz.mq.core.MqSendResult;
import com.sz.mq.core.MqTypeEnum;
import com.sz.mq.strategy.MqStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * RabbitMQ 发送策略实现（持久化投递 + Publisher Confirms + Mandatory Returns 同步确认）
 */
@Slf4j
public class RabbitMqStrategy implements MqStrategy {

    private final RabbitTemplate rabbitTemplate;
    private final MqProperties mqProperties;
    private final MqJsonCodec mqJsonCodec;

    public RabbitMqStrategy(RabbitTemplate rabbitTemplate, MqProperties mqProperties, MqJsonCodec mqJsonCodec) {
        this.rabbitTemplate = rabbitTemplate;
        this.mqProperties = mqProperties;
        this.mqJsonCodec = mqJsonCodec;
        if (mqProperties.getRabbitmq().isWaitForConfirms()) {
            this.rabbitTemplate.setMandatory(true);
        }
    }

    @Override
    public MqTypeEnum getType() {
        return MqTypeEnum.RABBITMQ;
    }

    @Override
    public <T> MqSendResult send(MqMessage<T> message) {
        String exchange = buildExchangeName(message.getTopic());
        String routingKey = message.getTag();
        Message amqpMessage = buildAmqpMessage(message);
        CorrelationData correlationData = new CorrelationData(message.getMessageId());

        try {
            rabbitTemplate.send(exchange, routingKey, amqpMessage, correlationData);
            if (mqProperties.getRabbitmq().isWaitForConfirms()) {
                long timeoutMs = mqProperties.getRabbitmq().getConfirmTimeoutMs();
                CorrelationData.Confirm confirm = correlationData.getFuture().get(timeoutMs, TimeUnit.MILLISECONDS);
                if (confirm == null || !confirm.isAck()) {
                    String reason = confirm == null ? "confirm=null" : confirm.getReason();
                    throw new MqSendException("RabbitMQ Broker NACK, messageId=" + message.getMessageId() + ", reason=" + reason);
                }
                if (correlationData.getReturned() != null) {
                    throw new MqSendException("RabbitMQ 消息路由不可达（Returned）, exchange=" + exchange
                            + ", routingKey=" + routingKey + ", replyText=" + correlationData.getReturned().getReplyText());
                }
            }
            return MqSendResult.builder()
                    .success(true)
                    .mqType(MqTypeEnum.RABBITMQ)
                    .messageId(message.getMessageId())
                    .rawMessageId(message.getMessageId())
                    .build();
        } catch (MqSendException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MqSendException("RabbitMQ 发送等待确认被中断, messageId=" + message.getMessageId(), e);
        } catch (TimeoutException e) {
            throw new MqSendException("RabbitMQ 发送等待 Broker Confirm 超时, messageId=" + message.getMessageId(), e);
        } catch (AmqpException e) {
            throw new MqSendException("RabbitMQ 发送失败, messageId=" + message.getMessageId(), e);
        } catch (Exception e) {
            throw new MqSendException("RabbitMQ 发送确认异常, messageId=" + message.getMessageId(), e);
        }
    }

    @Override
    public <T> void sendAsync(MqMessage<T> message) {
        String exchange = buildExchangeName(message.getTopic());
        String routingKey = message.getTag();
        Message amqpMessage = buildAmqpMessage(message);
        CorrelationData correlationData = new CorrelationData(message.getMessageId());

        rabbitTemplate.send(exchange, routingKey, amqpMessage, correlationData);
        correlationData.getFuture().whenComplete((confirm, ex) -> {
            if (ex != null) {
                log.error("[SZ-MQ][RabbitMQ] 异步发送确认异常, messageId={}", message.getMessageId(), ex);
            } else if (confirm == null || !confirm.isAck()) {
                log.error("[SZ-MQ][RabbitMQ] 异步发送被 NACK, messageId={}, reason={}",
                        message.getMessageId(), confirm == null ? "null" : confirm.getReason());
            } else if (correlationData.getReturned() != null) {
                log.error("[SZ-MQ][RabbitMQ] 异步发送路由不可达, messageId={}, replyText={}",
                        message.getMessageId(), correlationData.getReturned().getReplyText());
            }
        });
    }

    private String buildExchangeName(String topic) {
        return mqProperties.getRabbitmq().getExchangePrefix() + topic;
    }

    private <T> Message buildAmqpMessage(MqMessage<T> message) {
        byte[] body = mqJsonCodec.toJsonBytes(message);
        return MessageBuilder.withBody(body)
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setContentEncoding(StandardCharsets.UTF_8.name())
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                .setMessageId(message.getMessageId())
                .setCorrelationId(message.getBizKey())
                .setHeader("sz-mq-topic", message.getTopic())
                .setHeader("sz-mq-tag", message.getTag())
                .build();
    }
}
