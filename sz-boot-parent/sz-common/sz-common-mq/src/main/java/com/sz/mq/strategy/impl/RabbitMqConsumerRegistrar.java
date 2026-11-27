package com.sz.mq.strategy.impl;

import com.rabbitmq.client.Channel;
import com.sz.mq.config.MqProperties;
import com.sz.mq.consumer.MqConsumerContainerRegistrar;
import com.sz.mq.consumer.MqConsumerHandler;
import com.sz.mq.core.MqJsonCodec;
import com.sz.mq.core.MqMessage;
import com.sz.mq.core.MqNonRetryableException;
import com.sz.mq.core.MqTypeEnum;
import com.sz.mq.store.MqConsumeAttemptDecision;
import com.sz.mq.store.MqMessageLogStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * RabbitMQ 消费容器注册器（消费幂等落库 + 手动 ACK + 有限指数退避重试 + 死信路由）
 */
@Slf4j
public class RabbitMqConsumerRegistrar implements MqConsumerContainerRegistrar {

    private final ConnectionFactory connectionFactory;
    private final AmqpAdmin amqpAdmin;
    private final MqProperties mqProperties;
    private final MqJsonCodec mqJsonCodec;
    private final Supplier<MqMessageLogStore> logStoreSupplier;
    private final List<SimpleMessageListenerContainer> containers = new ArrayList<>();

    public RabbitMqConsumerRegistrar(ConnectionFactory connectionFactory,
                                     AmqpAdmin amqpAdmin,
                                     MqProperties mqProperties,
                                     MqJsonCodec mqJsonCodec) {
        this(connectionFactory, amqpAdmin, mqProperties, mqJsonCodec, (Supplier<MqMessageLogStore>) null);
    }

    public RabbitMqConsumerRegistrar(ConnectionFactory connectionFactory,
                                     AmqpAdmin amqpAdmin,
                                     MqProperties mqProperties,
                                     MqJsonCodec mqJsonCodec,
                                     MqMessageLogStore logStore) {
        this(connectionFactory, amqpAdmin, mqProperties, mqJsonCodec, logStore == null ? null : () -> logStore);
    }

    public RabbitMqConsumerRegistrar(ConnectionFactory connectionFactory,
                                     AmqpAdmin amqpAdmin,
                                     MqProperties mqProperties,
                                     MqJsonCodec mqJsonCodec,
                                     Supplier<MqMessageLogStore> logStoreSupplier) {
        this.connectionFactory = connectionFactory;
        this.amqpAdmin = amqpAdmin;
        this.mqProperties = mqProperties;
        this.mqJsonCodec = mqJsonCodec;
        this.logStoreSupplier = logStoreSupplier;
    }

    @Override
    public MqTypeEnum getType() {
        return MqTypeEnum.RABBITMQ;
    }

    @Override
    public synchronized void start(List<MqConsumerHandler<?>> handlers) {
        if (amqpAdmin instanceof RabbitAdmin rabbitAdmin) {
            try {
                rabbitAdmin.initialize();
            } catch (Exception e) {
                log.warn("[SZ-MQ][RabbitMQ] 启动初始化声明拓扑失败（将在连接恢复后自动重试）: {}", e.getMessage());
            }
        }

        for (MqConsumerHandler<?> handler : handlers) {
            String queueName = mqProperties.getRabbitmq().getQueuePrefix() + handler.topic() + "." + handler.group();
            int concurrency = handler.concurrency() > 0
                    ? handler.concurrency()
                    : mqProperties.getConsumer().getConcurrency();

            SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
            container.setQueueNames(queueName);
            container.setAcknowledgeMode(AcknowledgeMode.MANUAL);
            container.setPrefetchCount(mqProperties.getConsumer().getPrefetch());
            container.setConcurrentConsumers(concurrency);
            container.setMaxConcurrentConsumers(concurrency);
            container.setMissingQueuesFatal(false);
            container.setMessageListener((ChannelAwareMessageListener) (amqpMessage, channel) ->
                    handleDelivery(handler, amqpMessage, channel));
            container.start();
            containers.add(container);
            log.info("[SZ-MQ][RabbitMQ] 消费容器已启动, queue={}, concurrency={}", queueName, concurrency);
        }
    }

    @Override
    public synchronized void stop() {
        for (SimpleMessageListenerContainer container : containers) {
            try {
                container.stop();
            } catch (Exception e) {
                log.warn("[SZ-MQ][RabbitMQ] 停止消费容器异常: {}", e.getMessage());
            }
        }
        containers.clear();
    }

    <T> void handleDelivery(MqConsumerHandler<T> handler, Message amqpMessage, Channel channel) throws IOException {
        long deliveryTag = amqpMessage.getMessageProperties().getDeliveryTag();
        String rawPayloadJson = amqpMessage.getBody() == null ? "" : new String(amqpMessage.getBody(), StandardCharsets.UTF_8);
        MqMessage<T> mqMessage;
        try {
            mqMessage = mqJsonCodec.parseMessage(rawPayloadJson, handler.payloadType());
        } catch (Exception parseEx) {
            log.error("[SZ-MQ][RabbitMQ] 消息反序列化失败，直接转入死信队列, topic={}, group={}, deliveryTag={}",
                    handler.topic(), handler.group(), deliveryTag, parseEx);
            channel.basicNack(deliveryTag, false, false);
            return;
        }

        MqMessageLogStore logStore = resolveLogStore();
        int maxRetry = Math.max(0, mqProperties.getConsumer().getMaxRetry());
        long baseIntervalMs = Math.max(0L, mqProperties.getConsumer().getRetryInitialIntervalMs());
        boolean begunInStore = false;

        for (int attempt = 0; attempt <= maxRetry; attempt++) {
            try {
                if (logStore != null && !begunInStore) {
                    MqConsumeAttemptDecision decision = logStore.tryBeginConsume(
                            mqMessage, handler.group(), MqTypeEnum.RABBITMQ, rawPayloadJson);
                    begunInStore = true;
                    if (decision == MqConsumeAttemptDecision.ALREADY_CONSUMED) {
                        log.info("[SZ-MQ][RabbitMQ] 消费记录幂等命中已成功状态，跳过业务直接ACK, messageId={}, group={}",
                                mqMessage.getMessageId(), handler.group());
                        channel.basicAck(deliveryTag, false);
                        safeRecordAck(logStore, mqMessage.getMessageId(), handler.group(), "ACKED");
                        return;
                    }
                }

                handler.onMessage(mqMessage);
                if (logStore != null) {
                    safeRecordConsumeSuccess(logStore, mqMessage.getMessageId(), handler.group());
                }
                channel.basicAck(deliveryTag, false);
                if (logStore != null) {
                    safeRecordAck(logStore, mqMessage.getMessageId(), handler.group(), "ACKED");
                }
                return;
            } catch (MqNonRetryableException nonRetryableEx) {
                log.warn("[SZ-MQ][RabbitMQ] 命中不可重试业务异常，直接回调失败处理并转入死信, messageId={}, topic={}, reason={}",
                        mqMessage.getMessageId(), handler.topic(), nonRetryableEx.getMessage());
                safeOnConsumeFailed(handler, mqMessage, nonRetryableEx, false);
                if (logStore != null) {
                    safeRecordConsumeDead(logStore, mqMessage.getMessageId(), handler.group(), attempt, "NACKED_DLQ", nonRetryableEx);
                }
                channel.basicNack(deliveryTag, false, false);
                return;
            } catch (Throwable ex) {
                if (attempt < maxRetry) {
                    long sleepMs = baseIntervalMs * (1L << Math.min(attempt, 10));
                    log.warn("[SZ-MQ][RabbitMQ] 消费异常将进行第 {}/{} 次重试, messageId={}, topic={}, waitMs={}",
                            attempt + 1, maxRetry, mqMessage.getMessageId(), handler.topic(), sleepMs, ex);
                    if (logStore != null && begunInStore) {
                        safeRecordConsumeRetry(logStore, mqMessage.getMessageId(), handler.group(), attempt + 1, ex);
                    }
                    if (sleepMs > 0) {
                        try {
                            Thread.sleep(sleepMs);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            log.warn("[SZ-MQ][RabbitMQ] 消费重试等待被中断，重新入队等待下次投递, messageId={}",
                                    mqMessage.getMessageId());
                            channel.basicNack(deliveryTag, false, true);
                            return;
                        }
                    }
                } else {
                    log.error("[SZ-MQ][RabbitMQ] 瞬时异常重试已耗尽({}次)，转入死信队列, messageId={}, topic={}",
                            maxRetry, mqMessage.getMessageId(), handler.topic(), ex);
                    safeOnConsumeFailed(handler, mqMessage, ex, true);
                    if (logStore != null) {
                        safeRecordConsumeDead(logStore, mqMessage.getMessageId(), handler.group(), maxRetry, "NACKED_DLQ", ex);
                    }
                    channel.basicNack(deliveryTag, false, false);
                    return;
                }
            }
        }
    }

    private MqMessageLogStore resolveLogStore() {
        return logStoreSupplier == null ? null : logStoreSupplier.get();
    }

    private void safeRecordConsumeSuccess(MqMessageLogStore logStore, String messageId, String group) {
        try {
            logStore.recordConsumeSuccess(messageId, group);
        } catch (Exception e) {
            log.warn("[SZ-MQ][RabbitMQ] 更新消费成功记录失败, messageId={}, group={}", messageId, group, e);
        }
    }

    private void safeRecordConsumeRetry(MqMessageLogStore logStore, String messageId, String group, int retryCount, Throwable ex) {
        try {
            logStore.recordConsumeRetry(messageId, group, retryCount, ex);
        } catch (Exception e) {
            log.warn("[SZ-MQ][RabbitMQ] 更新消费重试记录失败, messageId={}, group={}", messageId, group, e);
        }
    }

    private void safeRecordConsumeDead(MqMessageLogStore logStore, String messageId, String group, int retryCount, String ackStatus, Throwable ex) {
        try {
            logStore.recordConsumeDead(messageId, group, retryCount, ackStatus, ex);
        } catch (Exception e) {
            log.warn("[SZ-MQ][RabbitMQ] 更新死信消费记录失败, messageId={}, group={}", messageId, group, e);
        }
    }

    private void safeRecordAck(MqMessageLogStore logStore, String messageId, String group, String ackStatus) {
        try {
            logStore.recordAck(messageId, group, ackStatus);
        } catch (Exception e) {
            log.warn("[SZ-MQ][RabbitMQ] 更新ACK确认记录失败, messageId={}, group={}", messageId, group, e);
        }
    }

    private <T> void safeOnConsumeFailed(MqConsumerHandler<T> handler,
                                         MqMessage<T> mqMessage,
                                         Throwable cause,
                                         boolean retryable) {
        try {
            handler.onConsumeFailed(mqMessage, cause, retryable);
        } catch (Throwable callbackEx) {
            log.error("[SZ-MQ][RabbitMQ] onConsumeFailed 回调执行异常, messageId={}",
                    mqMessage.getMessageId(), callbackEx);
        }
    }
}
