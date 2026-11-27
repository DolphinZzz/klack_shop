package com.sz.mq.strategy.impl;

import com.sz.mq.config.MqProperties;
import com.sz.mq.consumer.MqConsumerHandler;
import com.sz.mq.core.MqJsonCodec;
import com.sz.mq.core.MqMessage;
import com.sz.mq.core.MqNonRetryableException;
import com.sz.mq.core.MqTypeEnum;
import com.sz.mq.store.MqConsumeAttemptDecision;
import com.sz.mq.store.MqMessageLogStore;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaConsumerRegistrarTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;
    @Mock
    private ConsumerFactory<String, String> consumerFactory;
    @Mock
    private KafkaAdmin kafkaAdmin;
    @Mock
    private Acknowledgment acknowledgment;
    @Mock
    private MqMessageLogStore logStore;

    private MqJsonCodec codec;
    private MqProperties properties;
    private KafkaConsumerRegistrar registrar;

    @BeforeEach
    void setUp() {
        codec = new MqJsonCodec();
        properties = new MqProperties();
        properties.setType(MqTypeEnum.KAFKA);
        properties.getConsumer().setMaxRetry(2);
        properties.getConsumer().setRetryInitialIntervalMs(1L);
        lenient().when(logStore.tryBeginConsume(any(), anyString(), any(), anyString()))
                .thenReturn(MqConsumeAttemptDecision.PROCEED);
        registrar = new KafkaConsumerRegistrar(kafkaTemplate, consumerFactory, kafkaAdmin, properties, codec, logStore);
    }

    @Test
    void shouldAckOnSuccess() {
        MqConsumerHandler<String> handler = mockHandler();
        MqMessage<String> msg = MqMessage.of("coupon-seckill", "grab", "ORD1", "ok");
        ConsumerRecord<String, String> rec = buildRecord(msg);

        registrar.handleRecord(handler, rec, acknowledgment);

        verify(handler).onMessage(any());
        verify(logStore).recordConsumeSuccess(msg.getMessageId(), "coupon-seckill-group");
        verify(acknowledgment).acknowledge();
        verify(logStore).recordAck(msg.getMessageId(), "coupon-seckill-group", "ACKED");
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void shouldSkipIdempotentlyWhenAlreadyConsumed() {
        MqConsumerHandler<String> handler = mockHandler();
        MqMessage<String> msg = MqMessage.of("coupon-seckill", "grab", "ORD_DUP", "ok");
        when(logStore.tryBeginConsume(any(), anyString(), any(), anyString()))
                .thenReturn(MqConsumeAttemptDecision.ALREADY_CONSUMED);

        registrar.handleRecord(handler, buildRecord(msg), acknowledgment);

        verify(handler, never()).onMessage(any());
        verify(acknowledgment).acknowledge();
        verify(logStore).recordAck(msg.getMessageId(), "coupon-seckill-group", "ACKED");
    }

    @Test
    void shouldForwardToDltAndCallFailedWithRetryableFalseOnNonRetryableException() {
        MqConsumerHandler<String> handler = mockHandler();
        doThrow(new MqNonRetryableException("coupon expired")).when(handler).onMessage(any());
        when(kafkaTemplate.send(anyString(), any(), any())).thenReturn(CompletableFuture.completedFuture(null));

        ConsumerRecord<String, String> rec = buildRecord(MqMessage.of("coupon-seckill", "grab", "ORD2", "ok"));
        registrar.handleRecord(handler, rec, acknowledgment);

        verify(handler, times(1)).onMessage(any());
        verify(handler).onConsumeFailed(any(), any(MqNonRetryableException.class), eq(false));
        verify(kafkaTemplate).send(eq("coupon-seckill-dlt"), eq("ORD2"), anyString());
        verify(acknowledgment).acknowledge();
    }

    @Test
    void shouldExhaustRetriesAndForwardToDltWithRetryableTrue() {
        MqConsumerHandler<String> handler = mockHandler();
        doThrow(new RuntimeException("db timeout")).when(handler).onMessage(any());
        when(kafkaTemplate.send(anyString(), any(), any())).thenReturn(CompletableFuture.completedFuture(null));

        ConsumerRecord<String, String> rec = buildRecord(MqMessage.of("coupon-seckill", "grab", "ORD3", "ok"));
        registrar.handleRecord(handler, rec, acknowledgment);

        verify(handler, times(3)).onMessage(any());
        verify(handler).onConsumeFailed(any(), any(RuntimeException.class), eq(true));
        verify(kafkaTemplate).send(eq("coupon-seckill-dlt"), eq("ORD3"), anyString());
        verify(acknowledgment).acknowledge();
    }

    @SuppressWarnings("unchecked")
    private MqConsumerHandler<String> mockHandler() {
        MqConsumerHandler<String> handler = mock(MqConsumerHandler.class);
        lenient().when(handler.topic()).thenReturn("coupon-seckill");
        lenient().when(handler.tag()).thenReturn("grab");
        lenient().when(handler.group()).thenReturn("coupon-seckill-group");
        lenient().when(handler.payloadType()).thenReturn(String.class);
        return handler;
    }

    private ConsumerRecord<String, String> buildRecord(MqMessage<String> msg) {
        ConsumerRecord<String, String> rec = new ConsumerRecord<>("coupon-seckill", 0, 1L, msg.getBizKey(), codec.toJson(msg));
        rec.headers().add(new RecordHeader("sz-mq-tag", msg.getTag().getBytes(StandardCharsets.UTF_8)));
        return rec;
    }
}
