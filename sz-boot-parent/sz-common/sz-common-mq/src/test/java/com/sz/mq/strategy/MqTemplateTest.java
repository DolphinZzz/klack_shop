package com.sz.mq.strategy;

import com.sz.mq.config.MqProperties;
import com.sz.mq.core.MqJsonCodec;
import com.sz.mq.core.MqMessage;
import com.sz.mq.core.MqSendException;
import com.sz.mq.core.MqSendResult;
import com.sz.mq.core.MqTypeEnum;
import com.sz.mq.store.MqMessageLogStore;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class MqTemplateTest {

    @Test
    void shouldRouteToConfiguredStrategyAndNormalizeEnvelope() {
        MqProperties properties = new MqProperties();
        properties.setEnabled(true);
        properties.setType(MqTypeEnum.KAFKA);

        MqStrategy rabbitStrategy = mock(MqStrategy.class);
        when(rabbitStrategy.getType()).thenReturn(MqTypeEnum.RABBITMQ);

        MqStrategy kafkaStrategy = mock(MqStrategy.class);
        when(kafkaStrategy.getType()).thenReturn(MqTypeEnum.KAFKA);
        when(kafkaStrategy.send(any())).thenReturn(MqSendResult.builder()
                .success(true)
                .mqType(MqTypeEnum.KAFKA)
                .messageId("msg-1")
                .build());

        MqTemplate template = new MqTemplate(properties, List.of(rabbitStrategy, kafkaStrategy));
        MqSendResult result = template.send("coupon-seckill", "", "ORD1", "hello");

        assertTrue(result.isSuccess());
        assertEquals(MqTypeEnum.KAFKA, result.getMqType());

        ArgumentCaptor<MqMessage<String>> captor = ArgumentCaptor.forClass(MqMessage.class);
        verify(kafkaStrategy).send(captor.capture());
        verify(rabbitStrategy, never()).send(any());

        MqMessage<String> sent = captor.getValue();
        assertNotNull(sent.getMessageId());
        assertFalse(sent.getMessageId().isBlank());
        assertEquals("coupon-seckill", sent.getTopic());
        assertEquals("default", sent.getTag());
        assertEquals("ORD1", sent.getBizKey());
        assertEquals("hello", sent.getPayload());
        assertTrue(sent.getTimestamp() > 0);
    }

    @Test
    void shouldPersistPendingBeforeSendAndMarkSentAfterBrokerConfirm() {
        MqProperties properties = new MqProperties();
        properties.setEnabled(true);
        properties.setType(MqTypeEnum.RABBITMQ);

        MqStrategy rabbitStrategy = mock(MqStrategy.class);
        when(rabbitStrategy.getType()).thenReturn(MqTypeEnum.RABBITMQ);
        MqSendResult sendResult = MqSendResult.builder()
                .success(true)
                .mqType(MqTypeEnum.RABBITMQ)
                .messageId("m-1")
                .rawMessageId("raw-1")
                .build();
        when(rabbitStrategy.send(any())).thenReturn(sendResult);

        MqMessageLogStore logStore = mock(MqMessageLogStore.class);
        MqTemplate template = new MqTemplate(properties, List.of(rabbitStrategy), new MqJsonCodec(), logStore);

        MqMessage<String> msg = MqMessage.of("coupon-seckill", "grab", "ORD10", "body");
        MqSendResult actual = template.send(msg);

        assertTrue(actual.isSuccess());
        InOrder inOrder = inOrder(logStore, rabbitStrategy);
        inOrder.verify(logStore).recordSendPending(any(), eq(MqTypeEnum.RABBITMQ), eq("\"body\""));
        inOrder.verify(rabbitStrategy).send(any());
        inOrder.verify(logStore).recordSendSuccess(eq(msg.getMessageId()), eq(sendResult));
        verify(logStore, never()).recordSendFailure(anyString(), any());
    }

    @Test
    void shouldMarkFailedAndPropagateWhenBrokerSendFails() {
        MqProperties properties = new MqProperties();
        properties.setEnabled(true);
        properties.setType(MqTypeEnum.RABBITMQ);

        MqStrategy rabbitStrategy = mock(MqStrategy.class);
        when(rabbitStrategy.getType()).thenReturn(MqTypeEnum.RABBITMQ);
        when(rabbitStrategy.send(any())).thenThrow(new MqSendException("broker nack"));

        MqMessageLogStore logStore = mock(MqMessageLogStore.class);
        MqTemplate template = new MqTemplate(properties, List.of(rabbitStrategy), new MqJsonCodec(), logStore);

        MqMessage<String> msg = MqMessage.of("coupon-seckill", "grab", "ORD11", "body");
        assertThrows(MqSendException.class, () -> template.send(msg));

        verify(logStore).recordSendPending(any(), eq(MqTypeEnum.RABBITMQ), anyString());
        verify(logStore).recordSendFailure(eq(msg.getMessageId()), any(MqSendException.class));
        verify(logStore, never()).recordSendSuccess(anyString(), any());
    }

    @Test
    void shouldAbortSendWithoutCallingStrategyWhenPendingInsertFails() {
        MqProperties properties = new MqProperties();
        properties.setEnabled(true);
        properties.setType(MqTypeEnum.RABBITMQ);

        MqStrategy rabbitStrategy = mock(MqStrategy.class);
        when(rabbitStrategy.getType()).thenReturn(MqTypeEnum.RABBITMQ);

        MqMessageLogStore logStore = mock(MqMessageLogStore.class);
        doThrow(new RuntimeException("mysql down")).when(logStore).recordSendPending(any(), any(), anyString());

        MqTemplate template = new MqTemplate(properties, List.of(rabbitStrategy), new MqJsonCodec(), logStore);

        assertThrows(MqSendException.class, () -> template.send("coupon-seckill", "grab", "ORD12", "body"));
        verify(rabbitStrategy, never()).send(any());
    }

    @Test
    void shouldFailFastAtStartupWhenEnabledAndStrategyMissing() {
        MqProperties properties = new MqProperties();
        properties.setEnabled(true);
        properties.setType(MqTypeEnum.ROCKETMQ);

        assertThrows(IllegalStateException.class, () -> new MqTemplate(properties, List.of()));
    }

    @Test
    void shouldBootWhenDisabledAndThrowOnSend() {
        MqProperties properties = new MqProperties();
        properties.setEnabled(false);
        properties.setType(MqTypeEnum.RABBITMQ);

        MqTemplate template = new MqTemplate(properties, List.of());
        assertThrows(MqSendException.class, () -> template.send("coupon-seckill", "payload"));
        assertThrows(MqSendException.class, () -> template.sendAsync("coupon-seckill", "grab", "k1", "payload"));
    }
}
