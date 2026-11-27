package com.sz.mq.strategy.impl;

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
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.MessageExt;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * RocketMQ 编程式 Push 消费容器注册器（含消费幂等与 ACK 记录）
 */
@Slf4j
public class RocketMqConsumerRegistrar implements MqConsumerContainerRegistrar {

    private final String nameServer;
    private final MqProperties mqProperties;
    private final MqJsonCodec mqJsonCodec;
    private final Supplier<MqMessageLogStore> logStoreSupplier;
    private final List<DefaultMQPushConsumer> consumers = new ArrayList<>();

    public RocketMqConsumerRegistrar(String nameServer, MqProperties mqProperties, MqJsonCodec mqJsonCodec) {
        this(nameServer, mqProperties, mqJsonCodec, (Supplier<MqMessageLogStore>) null);
    }

    public RocketMqConsumerRegistrar(String nameServer,
                                     MqProperties mqProperties,
                                     MqJsonCodec mqJsonCodec,
                                     MqMessageLogStore logStore) {
        this(nameServer, mqProperties, mqJsonCodec, logStore == null ? null : () -> logStore);
    }

    public RocketMqConsumerRegistrar(String nameServer,
                                     MqProperties mqProperties,
                                     MqJsonCodec mqJsonCodec,
                                     Supplier<MqMessageLogStore> logStoreSupplier) {
        this.nameServer = nameServer;
        this.mqProperties = mqProperties;
        this.mqJsonCodec = mqJsonCodec;
        this.logStoreSupplier = logStoreSupplier;
    }

    @Override
    public MqTypeEnum getType() {
        return MqTypeEnum.ROCKETMQ;
    }

    @Override
    public synchronized void start(List<MqConsumerHandler<?>> handlers) {
        if (nameServer == null || nameServer.isBlank()) {
            throw new IllegalStateException("未配置 rocketmq.name-server，无法启动 RocketMQ 消费者");
        }
        for (MqConsumerHandler<?> handler : handlers) {
            String consumerGroup = mqProperties.getRocketmq().getConsumerGroupPrefix() + handler.group();
            int concurrency = handler.concurrency() > 0
                    ? handler.concurrency()
                    : mqProperties.getConsumer().getConcurrency();
            int maxReconsumeTimes = Math.max(0, mqProperties.getRocketmq().getMaxReconsumeTimes());

            try {
                DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(consumerGroup);
                consumer.setNamesrvAddr(nameServer);
                consumer.subscribe(handler.topic(), handler.tag());
                consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
                consumer.setConsumeThreadMin(concurrency);
                consumer.setConsumeThreadMax(concurrency);
                consumer.setMaxReconsumeTimes(maxReconsumeTimes);
                consumer.registerMessageListener((MessageListenerConcurrently) (msgs, context) -> {
                    for (MessageExt msgExt : msgs) {
                        ConsumeConcurrentlyStatus status = handleMessage(handler, msgExt);
                        if (status == ConsumeConcurrentlyStatus.RECONSUME_LATER) {
                            return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                        }
                    }
                    return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
                });
                consumer.start();
                consumers.add(consumer);
                log.info("[SZ-MQ][RocketMQ] 消费者已启动, topic={}, tag={}, group={}, concurrency={}",
                        handler.topic(), handler.tag(), consumerGroup, concurrency);
            } catch (Exception e) {
                stop();
                throw new IllegalStateException("启动 RocketMQ 消费者失败, consumerGroup=" + consumerGroup, e);
            }
        }
    }

    @Override
    public synchronized void stop() {
        for (DefaultMQPushConsumer consumer : consumers) {
            try {
                consumer.shutdown();
            } catch (Exception e) {
                log.warn("[SZ-MQ][RocketMQ] 停止消费者异常: {}", e.getMessage());
            }
        }
        consumers.clear();
    }

    <T> ConsumeConcurrentlyStatus handleMessage(MqConsumerHandler<T> handler, MessageExt msgExt) {
        String body = msgExt.getBody() == null ? "" : new String(msgExt.getBody(), StandardCharsets.UTF_8);
        MqMessage<T> mqMessage;
        try {
            mqMessage = mqJsonCodec.parseMessage(body, handler.payloadType());
        } catch (Exception parseEx) {
            log.error("[SZ-MQ][RocketMQ] 消息反序列化失败，跳过消费避免阻塞, topic={}, msgId={}",
                    handler.topic(), msgExt.getMsgId(), parseEx);
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        }

        MqMessageLogStore logStore = resolveLogStore();
        int reconsumeTimes = msgExt.getReconsumeTimes();
        int maxReconsumeTimes = Math.max(0, mqProperties.getRocketmq().getMaxReconsumeTimes());
        boolean begunInStore = false;

        try {
            if (logStore != null) {
                MqConsumeAttemptDecision decision = logStore.tryBeginConsume(
                        mqMessage, handler.group(), MqTypeEnum.ROCKETMQ, body);
                begunInStore = true;
                if (decision == MqConsumeAttemptDecision.ALREADY_CONSUMED) {
                    log.info("[SZ-MQ][RocketMQ] 消费记录幂等命中已成功状态，跳过业务直接ACK, messageId={}, group={}",
                            mqMessage.getMessageId(), handler.group());
                    safeRecordAck(logStore, mqMessage.getMessageId(), handler.group(), "ACKED");
                    return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
                }
            }

            handler.onMessage(mqMessage);
            if (logStore != null) {
                safeRecordConsumeSuccess(logStore, mqMessage.getMessageId(), handler.group());
                safeRecordAck(logStore, mqMessage.getMessageId(), handler.group(), "ACKED");
            }
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        } catch (MqNonRetryableException nonRetryableEx) {
            log.warn("[SZ-MQ][RocketMQ] 命中不可重试业务异常，直接回调失败处理, messageId={}, topic={}, reason={}",
                    mqMessage.getMessageId(), handler.topic(), nonRetryableEx.getMessage());
            safeOnConsumeFailed(handler, mqMessage, nonRetryableEx, false);
            if (logStore != null) {
                safeRecordConsumeDead(logStore, mqMessage.getMessageId(), handler.group(), reconsumeTimes, "NON_RETRYABLE_ACK", nonRetryableEx);
            }
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        } catch (Throwable ex) {
            if (reconsumeTimes >= maxReconsumeTimes) {
                log.error("[SZ-MQ][RocketMQ] 瞬时异常重试达到上限({}次)，触发失败回调并交由 Broker 转入 %DLQ%, messageId={}, topic={}",
                        maxReconsumeTimes, mqMessage.getMessageId(), handler.topic(), ex);
                safeOnConsumeFailed(handler, mqMessage, ex, true);
                if (logStore != null) {
                    safeRecordConsumeDead(logStore, mqMessage.getMessageId(), handler.group(), reconsumeTimes, "BROKER_DLQ", ex);
                }
            } else {
                log.warn("[SZ-MQ][RocketMQ] 消费异常等待 Broker 延迟重试, reconsumeTimes={}/{}, messageId={}, topic={}",
                        reconsumeTimes, maxReconsumeTimes, mqMessage.getMessageId(), handler.topic(), ex);
                if (logStore != null && begunInStore) {
                    safeRecordConsumeRetry(logStore, mqMessage.getMessageId(), handler.group(), reconsumeTimes + 1, ex);
                }
            }
            return ConsumeConcurrentlyStatus.RECONSUME_LATER;
        }
    }

    private MqMessageLogStore resolveLogStore() {
        return logStoreSupplier == null ? null : logStoreSupplier.get();
    }

    private void safeRecordConsumeSuccess(MqMessageLogStore logStore, String messageId, String group) {
        try {
            logStore.recordConsumeSuccess(messageId, group);
        } catch (Exception e) {
            log.warn("[SZ-MQ][RocketMQ] 更新消费成功记录失败, messageId={}, group={}", messageId, group, e);
        }
    }

    private void safeRecordConsumeRetry(MqMessageLogStore logStore, String messageId, String group, int retryCount, Throwable ex) {
        try {
            logStore.recordConsumeRetry(messageId, group, retryCount, ex);
        } catch (Exception e) {
            log.warn("[SZ-MQ][RocketMQ] 更新消费重试记录失败, messageId={}, group={}", messageId, group, e);
        }
    }

    private void safeRecordConsumeDead(MqMessageLogStore logStore, String messageId, String group, int retryCount, String ackStatus, Throwable ex) {
        try {
            logStore.recordConsumeDead(messageId, group, retryCount, ackStatus, ex);
        } catch (Exception e) {
            log.warn("[SZ-MQ][RocketMQ] 更新死信消费记录失败, messageId={}, group={}", messageId, group, e);
        }
    }

    private void safeRecordAck(MqMessageLogStore logStore, String messageId, String group, String ackStatus) {
        try {
            logStore.recordAck(messageId, group, ackStatus);
        } catch (Exception e) {
            log.warn("[SZ-MQ][RocketMQ] 更新ACK确认记录失败, messageId={}, group={}", messageId, group, e);
        }
    }

    private <T> void safeOnConsumeFailed(MqConsumerHandler<T> handler,
                                         MqMessage<T> mqMessage,
                                         Throwable cause,
                                         boolean retryable) {
        try {
            handler.onConsumeFailed(mqMessage, cause, retryable);
        } catch (Throwable callbackEx) {
            log.error("[SZ-MQ][RocketMQ] onConsumeFailed 回调执行异常, messageId={}",
                    mqMessage.getMessageId(), callbackEx);
        }
    }
}
