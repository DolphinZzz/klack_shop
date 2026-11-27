package com.sz.mq.strategy.impl;

import com.sz.mq.config.MqProperties;
import com.sz.mq.consumer.MqConsumerHandler;
import com.sz.mq.core.MqJsonCodec;
import com.sz.mq.core.MqMessage;
import com.sz.mq.core.MqNonRetryableException;
import com.sz.mq.core.MqTypeEnum;
import com.sz.mq.store.MqConsumeAttemptDecision;
import com.sz.mq.store.MqMessageLogStore;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.common.message.MessageExt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RocketMqConsumerRegistrarTest {

    @Mock
    private MqMessageLogStore logStore;

    private MqJsonCodec codec;
    private MqProperties properties;
    private RocketMqConsumerRegistrar registrar;

    @BeforeEach
    void setUp() {
        codec = new MqJsonCodec();
        properties = new MqProperties();
        properties.setType(MqTypeEnum.ROCKETMQ);
        properties.getRocketmq().setMaxReconsumeTimes(3);
        lenient().when(logStore.tryBeginConsume(any(), anyString(), any(), anyString()))
                .thenReturn(MqConsumeAttemptDecision.PROCEED);
        registrar = new RocketMqConsumerRegistrar("127.0.0.1:9876", properties, codec, logStore);
    }

    @Test
    void shouldReturnConsumeSuccessOnNormalMessage() {
        MqConsumerHandler<String> handler = mockHandler();
        MqMessage<String> msg = MqMessage.of("coupon-seckill", "grab", "ORD1", "ok");
        MessageExt msgExt = buildMessageExt(msg, 0);

        ConsumeConcurrentlyStatus status = registrar.handleMessage(handler, msgExt);

        assertEquals(ConsumeConcurrentlyStatus.CONSUME_SUCCESS, status);
        verify(handler).onMessage(any());
        verify(logStore).recordConsumeSuccess(msg.getMessageId(), "coupon-seckill-group");
        verify(logStore).recordAck(msg.getMessageId(), "coupon-seckill-group", "ACKED");
        verify(handler, never()).onConsumeFailed(any(), any(), anyBoolean());
    }

    @Test
    void shouldSkipIdempotentlyWhenAlreadyConsumed() {
        MqConsumerHandler<String> handler = mockHandler();
        MqMessage<String> msg = MqMessage.of("coupon-seckill", "grab", "ORD_DUP", "ok");
        when(logStore.tryBeginConsume(any(), anyString(), any(), anyString()))
                .thenReturn(MqConsumeAttemptDecision.ALREADY_CONSUMED);

        ConsumeConcurrentlyStatus status = registrar.handleMessage(handler, buildMessageExt(msg, 0));

        assertEquals(ConsumeConcurrentlyStatus.CONSUME_SUCCESS, status);
        verify(handler, never()).onMessage(any());
        verify(logStore).recordAck(msg.getMessageId(), "coupon-seckill-group", "ACKED");
    }

    @Test
    void shouldCallOnConsumeFailedWithRetryableFalseAndFinishOnNonRetryableException() {
        MqConsumerHandler<String> handler = mockHandler();
        doThrow(new MqNonRetryableException("invalid coupon")).when(handler).onMessage(any());
        MessageExt msgExt = buildMessageExt(MqMessage.of("coupon-seckill", "grab", "ORD2", "ok"), 0);

        ConsumeConcurrentlyStatus status = registrar.handleMessage(handler, msgExt);

        assertEquals(ConsumeConcurrentlyStatus.CONSUME_SUCCESS, status);
        verify(handler).onConsumeFailed(any(), any(MqNonRetryableException.class), eq(false));
    }

    @Test
    void shouldReturnReconsumeLaterWithoutCallbackBeforeMaxReconsumeTimes() {
        MqConsumerHandler<String> handler = mockHandler();
        doThrow(new RuntimeException("db timeout")).when(handler).onMessage(any());
        MessageExt msgExt = buildMessageExt(MqMessage.of("coupon-seckill", "grab", "ORD3", "ok"), 1);

        ConsumeConcurrentlyStatus status = registrar.handleMessage(handler, msgExt);

        assertEquals(ConsumeConcurrentlyStatus.RECONSUME_LATER, status);
        verify(handler, never()).onConsumeFailed(any(), any(), anyBoolean());
    }

    @Test
    void shouldInvokeOnConsumeFailedWithRetryableTrueAndReturnReconsumeLaterAtLimitForDlq() {
        MqConsumerHandler<String> handler = mockHandler();
        doThrow(new RuntimeException("db timeout")).when(handler).onMessage(any());
        MessageExt msgExt = buildMessageExt(MqMessage.of("coupon-seckill", "grab", "ORD4", "ok"), 3);

        ConsumeConcurrentlyStatus status = registrar.handleMessage(handler, msgExt);

        assertEquals(ConsumeConcurrentlyStatus.RECONSUME_LATER, status);
        verify(handler).onConsumeFailed(any(), any(RuntimeException.class), eq(true));
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

    private MessageExt buildMessageExt(MqMessage<String> msg, int reconsumeTimes) {
        MessageExt ext = new MessageExt();
        ext.setMsgId("msg-" + reconsumeTimes);
        ext.setReconsumeTimes(reconsumeTimes);
        ext.setBody(codec.toJsonBytes(msg));
        return ext;
    }
}
