package com.sz.mq.strategy.impl;

import com.rabbitmq.client.Channel;
import com.sz.mq.config.MqProperties;
import com.sz.mq.consumer.MqConsumerHandler;
import com.sz.mq.core.MqJsonCodec;
import com.sz.mq.core.MqMessage;
import com.sz.mq.core.MqNonRetryableException;
import com.sz.mq.core.MqTypeEnum;
import com.sz.mq.store.MqConsumeAttemptDecision;
import com.sz.mq.store.MqMessageLogStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RabbitMqConsumerRegistrarTest {

    @Mock
    private ConnectionFactory connectionFactory;
    @Mock
    private RabbitAdmin rabbitAdmin;
    @Mock
    private Channel channel;
    @Mock
    private MqMessageLogStore logStore;

    private MqJsonCodec codec;
    private MqProperties properties;
    private RabbitMqConsumerRegistrar registrar;

    @BeforeEach
    void setUp() {
        codec = new MqJsonCodec();
        properties = new MqProperties();
        properties.getConsumer().setMaxRetry(2);
        properties.getConsumer().setRetryInitialIntervalMs(1L);
        registrar = new RabbitMqConsumerRegistrar(connectionFactory, rabbitAdmin, properties, codec, logStore);
    }

    @Test
    void shouldRecordConsumeSuccessAndAckOnSuccess() throws Exception {
        MqConsumerHandler<String> handler = mockHandler();
        MqMessage<String> msg = MqMessage.of("coupon-seckill", "grab", "ORD1", "ok");
        when(logStore.tryBeginConsume(any(), eq("coupon-seckill-group"), eq(MqTypeEnum.RABBITMQ), anyString()))
                .thenReturn(MqConsumeAttemptDecision.PROCEED);

        registrar.handleDelivery(handler, buildAmqpMessage(msg, 11L), channel);

        InOrder inOrder = inOrder(logStore, handler, channel);
        inOrder.verify(logStore).tryBeginConsume(any(), eq("coupon-seckill-group"), eq(MqTypeEnum.RABBITMQ), anyString());
        inOrder.verify(handler).onMessage(any());
        inOrder.verify(logStore).recordConsumeSuccess(msg.getMessageId(), "coupon-seckill-group");
        inOrder.verify(channel).basicAck(11L, false);
        inOrder.verify(logStore).recordAck(msg.getMessageId(), "coupon-seckill-group", "ACKED");
        verify(handler, never()).onConsumeFailed(any(), any(), anyBoolean());
    }

    @Test
    void shouldSkipBusinessAndAckWhenAlreadyConsumedIdempotently() throws Exception {
        MqConsumerHandler<String> handler = mockHandler();
        MqMessage<String> msg = MqMessage.of("coupon-seckill", "grab", "ORD_DUP", "ok");
        when(logStore.tryBeginConsume(any(), eq("coupon-seckill-group"), eq(MqTypeEnum.RABBITMQ), anyString()))
                .thenReturn(MqConsumeAttemptDecision.ALREADY_CONSUMED);

        registrar.handleDelivery(handler, buildAmqpMessage(msg, 21L), channel);

        verify(handler, never()).onMessage(any());
        verify(channel).basicAck(21L, false);
        verify(logStore).recordAck(msg.getMessageId(), "coupon-seckill-group", "ACKED");
    }

    @Test
    void shouldNotRetryAndRecordDeadOnNonRetryableException() throws Exception {
        MqConsumerHandler<String> handler = mockHandler();
        MqMessage<String> msg = MqMessage.of("coupon-seckill", "grab", "ORD2", "ok");
        when(logStore.tryBeginConsume(any(), anyString(), any(), anyString())).thenReturn(MqConsumeAttemptDecision.PROCEED);
        doThrow(new MqNonRetryableException("coupon disabled")).when(handler).onMessage(any());

        registrar.handleDelivery(handler, buildAmqpMessage(msg, 12L), channel);

        verify(handler, times(1)).onMessage(any());
        verify(handler, times(1)).onConsumeFailed(any(), any(MqNonRetryableException.class), eq(false));
        verify(logStore).recordConsumeDead(eq(msg.getMessageId()), eq("coupon-seckill-group"), eq(0), eq("NACKED_DLQ"), any());
        verify(channel).basicNack(12L, false, false);
    }

    @Test
    void shouldRetryTransientExceptionAndAckWhenLaterAttemptSucceeds() throws Exception {
        MqConsumerHandler<String> handler = mockHandler();
        MqMessage<String> msg = MqMessage.of("coupon-seckill", "grab", "ORD3", "ok");
        when(logStore.tryBeginConsume(any(), anyString(), any(), anyString())).thenReturn(MqConsumeAttemptDecision.PROCEED);

        AtomicInteger calls = new AtomicInteger();
        doAnswer(invocation -> {
            if (calls.incrementAndGet() < 3) {
                throw new RuntimeException("transient db timeout");
            }
            return null;
        }).when(handler).onMessage(any());

        registrar.handleDelivery(handler, buildAmqpMessage(msg, 13L), channel);

        assertEquals(3, calls.get());
        verify(logStore).recordConsumeRetry(eq(msg.getMessageId()), eq("coupon-seckill-group"), eq(1), any());
        verify(logStore).recordConsumeRetry(eq(msg.getMessageId()), eq("coupon-seckill-group"), eq(2), any());
        verify(logStore).recordConsumeSuccess(msg.getMessageId(), "coupon-seckill-group");
        verify(logStore).recordAck(msg.getMessageId(), "coupon-seckill-group", "ACKED");
        verify(handler, never()).onConsumeFailed(any(), any(), anyBoolean());
        verify(channel).basicAck(13L, false);
    }

    @Test
    void shouldExhaustRetriesAndRecordDeadWithRetryableTrueThenNackToDlq() throws Exception {
        MqConsumerHandler<String> handler = mockHandler();
        MqMessage<String> msg = MqMessage.of("coupon-seckill", "grab", "ORD4", "ok");
        when(logStore.tryBeginConsume(any(), anyString(), any(), anyString())).thenReturn(MqConsumeAttemptDecision.PROCEED);
        doThrow(new RuntimeException("db still down")).when(handler).onMessage(any());

        registrar.handleDelivery(handler, buildAmqpMessage(msg, 14L), channel);

        verify(handler, times(3)).onMessage(any());
        verify(handler, times(1)).onConsumeFailed(any(), any(RuntimeException.class), eq(true));
        verify(logStore).recordConsumeDead(eq(msg.getMessageId()), eq("coupon-seckill-group"), eq(2), eq("NACKED_DLQ"), any());
        verify(channel).basicNack(14L, false, false);
    }

    @Test
    void shouldNackMalformedJsonDirectlyToDlq() throws Exception {
        MqConsumerHandler<String> handler = mockHandler();
        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(15L);
        Message malformed = new Message("{not-json".getBytes(StandardCharsets.UTF_8), props);

        registrar.handleDelivery(handler, malformed, channel);

        verify(handler, never()).onMessage(any());
        verifyNoInteractions(logStore);
        verify(channel).basicNack(15L, false, false);
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

    private Message buildAmqpMessage(MqMessage<String> msg, long deliveryTag) {
        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(deliveryTag);
        return new Message(codec.toJsonBytes(msg), props);
    }
}
