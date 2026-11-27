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
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.Acknowledgment;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Kafka 编程式并发消费容器注册器（消费幂等落库 + 手动提交位点 + 指数退避重试 + DLT 死信转发）
 */
@Slf4j
public class KafkaConsumerRegistrar implements MqConsumerContainerRegistrar {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ConsumerFactory<String, String> consumerFactory;
    private final KafkaAdmin kafkaAdmin;
    private final MqProperties mqProperties;
    private final MqJsonCodec mqJsonCodec;
    private final Supplier<MqMessageLogStore> logStoreSupplier;
    private final List<ConcurrentMessageListenerContainer<String, String>> containers = new ArrayList<>();

    public KafkaConsumerRegistrar(KafkaTemplate<String, String> kafkaTemplate,
                                  ConsumerFactory<String, String> consumerFactory,
                                  KafkaAdmin kafkaAdmin,
                                  MqProperties mqProperties,
                                  MqJsonCodec mqJsonCodec) {
        this(kafkaTemplate, consumerFactory, kafkaAdmin, mqProperties, mqJsonCodec, (Supplier<MqMessageLogStore>) null);
    }

    public KafkaConsumerRegistrar(KafkaTemplate<String, String> kafkaTemplate,
                                  ConsumerFactory<String, String> consumerFactory,
                                  KafkaAdmin kafkaAdmin,
                                  MqProperties mqProperties,
                                  MqJsonCodec mqJsonCodec,
                                  MqMessageLogStore logStore) {
        this(kafkaTemplate, consumerFactory, kafkaAdmin, mqProperties, mqJsonCodec, logStore == null ? null : () -> logStore);
    }

    public KafkaConsumerRegistrar(KafkaTemplate<String, String> kafkaTemplate,
                                  ConsumerFactory<String, String> consumerFactory,
                                  KafkaAdmin kafkaAdmin,
                                  MqProperties mqProperties,
                                  MqJsonCodec mqJsonCodec,
                                  Supplier<MqMessageLogStore> logStoreSupplier) {
        this.kafkaTemplate = kafkaTemplate;
        this.consumerFactory = consumerFactory;
        this.kafkaAdmin = kafkaAdmin;
        this.mqProperties = mqProperties;
        this.mqJsonCodec = mqJsonCodec;
        this.logStoreSupplier = logStoreSupplier;
    }

    @Override
    public MqTypeEnum getType() {
        return MqTypeEnum.KAFKA;
    }

    @Override
    public synchronized void start(List<MqConsumerHandler<?>> handlers) {
        ensureTopics(handlers);
        for (MqConsumerHandler<?> handler : handlers) {
            String groupId = mqProperties.getKafka().getGroupIdPrefix() + handler.group();
            int concurrency = handler.concurrency() > 0
                    ? handler.concurrency()
                    : mqProperties.getConsumer().getConcurrency();

            ContainerProperties containerProps = new ContainerProperties(handler.topic());
            containerProps.setGroupId(groupId);
            containerProps.setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
            containerProps.setMessageListener((AcknowledgingMessageListener<String, String>) (record, ack) ->
                    handleRecord(handler, record, ack));

            ConcurrentMessageListenerContainer<String, String> container =
                    new ConcurrentMessageListenerContainer<>(consumerFactory, containerProps);
            container.setConcurrency(concurrency);
            container.start();
            containers.add(container);
            log.info("[SZ-MQ][Kafka] 消费容器已启动, topic={}, groupId={}, concurrency={}",
                    handler.topic(), groupId, concurrency);
        }
    }

    @Override
    public synchronized void stop() {
        for (ConcurrentMessageListenerContainer<String, String> container : containers) {
            try {
                container.stop();
            } catch (Exception e) {
                log.warn("[SZ-MQ][Kafka] 停止消费容器异常: {}", e.getMessage());
            }
        }
        containers.clear();
    }

    <T> void handleRecord(MqConsumerHandler<T> handler,
                          ConsumerRecord<String, String> record,
                          Acknowledgment acknowledgment) {
        String tagHeader = extractTagHeader(record);
        if (tagHeader != null && !tagHeader.equals(handler.tag())) {
            log.debug("[SZ-MQ][Kafka] 忽略不匹配 tag 的消息, expected={}, actual={}, topic={}",
                    handler.tag(), tagHeader, handler.topic());
            acknowledgeSafely(acknowledgment);
            return;
        }

        String rawPayloadJson = record.value();
        MqMessage<T> mqMessage;
        try {
            mqMessage = mqJsonCodec.parseMessage(rawPayloadJson, handler.payloadType());
        } catch (Exception parseEx) {
            log.error("[SZ-MQ][Kafka] 消息反序列化失败，转发 DLT 后提交位点, topic={}, offset={}",
                    handler.topic(), record.offset(), parseEx);
            publishToDltSafely(handler.topic(), record.key(), rawPayloadJson);
            acknowledgeSafely(acknowledgment);
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
                            mqMessage, handler.group(), MqTypeEnum.KAFKA, rawPayloadJson);
                    begunInStore = true;
                    if (decision == MqConsumeAttemptDecision.ALREADY_CONSUMED) {
                        log.info("[SZ-MQ][Kafka] 消费记录幂等命中已成功状态，跳过业务直接提交位点, messageId={}, group={}",
                                mqMessage.getMessageId(), handler.group());
                        acknowledgeSafely(acknowledgment);
                        safeRecordAck(logStore, mqMessage.getMessageId(), handler.group(), "ACKED");
                        return;
                    }
                }

                handler.onMessage(mqMessage);
                if (logStore != null) {
                    safeRecordConsumeSuccess(logStore, mqMessage.getMessageId(), handler.group());
                }
                acknowledgeSafely(acknowledgment);
                if (logStore != null) {
                    safeRecordAck(logStore, mqMessage.getMessageId(), handler.group(), "ACKED");
                }
                return;
            } catch (MqNonRetryableException nonRetryableEx) {
                log.warn("[SZ-MQ][Kafka] 命中不可重试业务异常，直接回调失败处理并转入 DLT, messageId={}, topic={}, reason={}",
                        mqMessage.getMessageId(), handler.topic(), nonRetryableEx.getMessage());
                safeOnConsumeFailed(handler, mqMessage, nonRetryableEx, false);
                publishToDltSafely(handler.topic(), record.key(), rawPayloadJson);
                if (logStore != null) {
                    safeRecordConsumeDead(logStore, mqMessage.getMessageId(), handler.group(), attempt, "SENT_DLT", nonRetryableEx);
                }
                acknowledgeSafely(acknowledgment);
                return;
            } catch (Throwable ex) {
                if (attempt < maxRetry) {
                    long sleepMs = baseIntervalMs * (1L << Math.min(attempt, 10));
                    log.warn("[SZ-MQ][Kafka] 消费异常将进行第 {}/{} 次重试, messageId={}, topic={}, waitMs={}",
                            attempt + 1, maxRetry, mqMessage.getMessageId(), handler.topic(), sleepMs, ex);
                    if (logStore != null && begunInStore) {
                        safeRecordConsumeRetry(logStore, mqMessage.getMessageId(), handler.group(), attempt + 1, ex);
                    }
                    if (sleepMs > 0) {
                        try {
                            Thread.sleep(sleepMs);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            log.warn("[SZ-MQ][Kafka] 消费重试等待被中断，跳过提交位点等待重新投递, messageId={}",
                                    mqMessage.getMessageId());
                            return;
                        }
                    }
                } else {
                    log.error("[SZ-MQ][Kafka] 瞬时异常重试已耗尽({}次)，转入 DLT, messageId={}, topic={}",
                            maxRetry, mqMessage.getMessageId(), handler.topic(), ex);
                    safeOnConsumeFailed(handler, mqMessage, ex, true);
                    publishToDltSafely(handler.topic(), record.key(), rawPayloadJson);
                    if (logStore != null) {
                        safeRecordConsumeDead(logStore, mqMessage.getMessageId(), handler.group(), maxRetry, "SENT_DLT", ex);
                    }
                    acknowledgeSafely(acknowledgment);
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
            log.warn("[SZ-MQ][Kafka] 更新消费成功记录失败, messageId={}, group={}", messageId, group, e);
        }
    }

    private void safeRecordConsumeRetry(MqMessageLogStore logStore, String messageId, String group, int retryCount, Throwable ex) {
        try {
            logStore.recordConsumeRetry(messageId, group, retryCount, ex);
        } catch (Exception e) {
            log.warn("[SZ-MQ][Kafka] 更新消费重试记录失败, messageId={}, group={}", messageId, group, e);
        }
    }

    private void safeRecordConsumeDead(MqMessageLogStore logStore, String messageId, String group, int retryCount, String ackStatus, Throwable ex) {
        try {
            logStore.recordConsumeDead(messageId, group, retryCount, ackStatus, ex);
        } catch (Exception e) {
            log.warn("[SZ-MQ][Kafka] 更新死信消费记录失败, messageId={}, group={}", messageId, group, e);
        }
    }

    private void safeRecordAck(MqMessageLogStore logStore, String messageId, String group, String ackStatus) {
        try {
            logStore.recordAck(messageId, group, ackStatus);
        } catch (Exception e) {
            log.warn("[SZ-MQ][Kafka] 更新ACK确认记录失败, messageId={}, group={}", messageId, group, e);
        }
    }

    private void ensureTopics(List<MqConsumerHandler<?>> handlers) {
        if (!mqProperties.getKafka().isAutoCreateTopics() || kafkaAdmin == null) {
            return;
        }
        try {
            int partitions = Math.max(1, mqProperties.getKafka().getDefaultPartitions());
            short replicas = (short) Math.max(1, mqProperties.getKafka().getDefaultReplicas());
            String dlqSuffix = mqProperties.getKafka().getDlqSuffix();

            List<NewTopic> topics = new ArrayList<>();
            for (MqConsumerHandler<?> handler : handlers) {
                topics.add(new NewTopic(handler.topic(), partitions, replicas));
                topics.add(new NewTopic(handler.topic() + dlqSuffix, partitions, replicas));
            }
            kafkaAdmin.createOrModifyTopics(topics.toArray(NewTopic[]::new));
        } catch (Exception e) {
            log.warn("[SZ-MQ][Kafka] 自动创建 Topic/DLT 失败（若 Broker 已自动/预先建好 Topic 可忽略）: {}", e.getMessage());
        }
    }

    private String extractTagHeader(ConsumerRecord<String, String> record) {
        Header header = record.headers().lastHeader("sz-mq-tag");
        if (header == null || header.value() == null) {
            return null;
        }
        return new String(header.value(), StandardCharsets.UTF_8);
    }

    private void publishToDltSafely(String topic, String key, String value) {
        String dltTopic = topic + mqProperties.getKafka().getDlqSuffix();
        try {
            kafkaTemplate.send(dltTopic, key, value)
                    .get(mqProperties.getSendTimeoutMs(), TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.error("[SZ-MQ][Kafka] 发送消息至 DLT 失败, dltTopic={}, key={}", dltTopic, key, e);
        }
    }

    private void acknowledgeSafely(Acknowledgment acknowledgment) {
        if (acknowledgment != null) {
            acknowledgment.acknowledge();
        }
    }

    private <T> void safeOnConsumeFailed(MqConsumerHandler<T> handler,
                                         MqMessage<T> mqMessage,
                                         Throwable cause,
                                         boolean retryable) {
        try {
            handler.onConsumeFailed(mqMessage, cause, retryable);
        } catch (Throwable callbackEx) {
            log.error("[SZ-MQ][Kafka] onConsumeFailed 回调执行异常, messageId={}",
                    mqMessage.getMessageId(), callbackEx);
        }
    }
}
