package com.sz.mq.strategy.impl;

import com.sz.mq.config.MqProperties;
import com.sz.mq.core.MqJsonCodec;
import com.sz.mq.core.MqMessage;
import com.sz.mq.core.MqSendException;
import com.sz.mq.core.MqSendResult;
import com.sz.mq.core.MqTypeEnum;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaMqStrategyTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private MqProperties properties;
    private KafkaMqStrategy strategy;

    @BeforeEach
    void setUp() {
        properties = new MqProperties();
        properties.setType(MqTypeEnum.KAFKA);
        strategy = new KafkaMqStrategy(kafkaTemplate, properties, new MqJsonCodec());
    }

    @Test
    void shouldSendSynchronouslyWithPartitionKeyAndTagHeader() {
        ProducerRecord<String, String> rec = new ProducerRecord<>("coupon-seckill", "ORD300", "{}");
        RecordMetadata metadata = new RecordMetadata(new TopicPartition("coupon-seckill", 1), 42L, 0, 0L, 0, 0);
        SendResult<String, String> sendResult = new SendResult<>(rec, metadata);

        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(CompletableFuture.completedFuture(sendResult));

        MqMessage<String> msg = MqMessage.of("coupon-seckill", "grab", "ORD300", "hello");
        MqSendResult result = strategy.send(msg);

        assertTrue(result.isSuccess());
        assertEquals(MqTypeEnum.KAFKA, result.getMqType());
        assertEquals("1@42", result.getRawMessageId());

        ArgumentCaptor<ProducerRecord<String, String>> captor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(kafkaTemplate).send(captor.capture());
        assertEquals("coupon-seckill", captor.getValue().topic());
        assertEquals("ORD300", captor.getValue().key());
        assertNotNull(captor.getValue().headers().lastHeader("sz-mq-tag"));
    }

    @Test
    void shouldThrowMqSendExceptionOnSendFailure() {
        CompletableFuture<SendResult<String, String>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("broker unreachable"));
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(failed);

        MqMessage<String> msg = MqMessage.of("coupon-seckill", "grab", "ORD301", "hello");
        assertThrows(MqSendException.class, () -> strategy.send(msg));
    }
}
