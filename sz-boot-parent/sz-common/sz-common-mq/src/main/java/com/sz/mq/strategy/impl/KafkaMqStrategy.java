package com.sz.mq.strategy.impl;

import com.sz.mq.config.MqProperties;
import com.sz.mq.core.MqJsonCodec;
import com.sz.mq.core.MqMessage;
import com.sz.mq.core.MqSendException;
import com.sz.mq.core.MqSendResult;
import com.sz.mq.core.MqTypeEnum;
import com.sz.mq.strategy.MqStrategy;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Kafka 发送策略实现
 */
@Slf4j
public class KafkaMqStrategy implements MqStrategy {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final MqProperties mqProperties;
    private final MqJsonCodec mqJsonCodec;

    public KafkaMqStrategy(KafkaTemplate<String, String> kafkaTemplate,
                           MqProperties mqProperties,
                           MqJsonCodec mqJsonCodec) {
        this.kafkaTemplate = kafkaTemplate;
        this.mqProperties = mqProperties;
        this.mqJsonCodec = mqJsonCodec;
    }

    @Override
    public MqTypeEnum getType() {
        return MqTypeEnum.KAFKA;
    }

    @Override
    public <T> MqSendResult send(MqMessage<T> message) {
        ProducerRecord<String, String> record = buildRecord(message);
        try {
            SendResult<String, String> result = kafkaTemplate.send(record)
                    .get(mqProperties.getSendTimeoutMs(), TimeUnit.MILLISECONDS);
            RecordMetadata metadata = result == null ? null : result.getRecordMetadata();
            String rawId = metadata == null ? null : metadata.partition() + "@" + metadata.offset();
            return MqSendResult.builder()
                    .success(true)
                    .mqType(MqTypeEnum.KAFKA)
                    .messageId(message.getMessageId())
                    .rawMessageId(rawId)
                    .build();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MqSendException("Kafka 发送等待被中断, messageId=" + message.getMessageId(), e);
        } catch (Exception e) {
            throw new MqSendException("Kafka 发送失败, topic=" + message.getTopic() + ", messageId=" + message.getMessageId(), e);
        }
    }

    @Override
    public <T> void sendAsync(MqMessage<T> message) {
        ProducerRecord<String, String> record = buildRecord(message);
        kafkaTemplate.send(record).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("[SZ-MQ][Kafka] 异步发送失败, messageId={}, topic={}",
                        message.getMessageId(), message.getTopic(), ex);
            } else {
                log.debug("[SZ-MQ][Kafka] 异步发送成功, messageId={}", message.getMessageId());
            }
        });
    }

    private <T> ProducerRecord<String, String> buildRecord(MqMessage<T> message) {
        ProducerRecord<String, String> record = new ProducerRecord<>(
                message.getTopic(),
                message.getBizKey(),
                mqJsonCodec.toJson(message)
        );
        record.headers().add(new RecordHeader("sz-mq-message-id", message.getMessageId().getBytes(StandardCharsets.UTF_8)));
        record.headers().add(new RecordHeader("sz-mq-tag", message.getTag().getBytes(StandardCharsets.UTF_8)));
        return record;
    }
}
