package com.sz.mq.strategy.impl;

import com.sz.mq.config.MqProperties;
import com.sz.mq.core.MqJsonCodec;
import com.sz.mq.core.MqMessage;
import com.sz.mq.core.MqSendException;
import com.sz.mq.core.MqSendResult;
import com.sz.mq.core.MqTypeEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RabbitMqStrategyTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private MqProperties properties;
    private RabbitMqStrategy strategy;

    @BeforeEach
    void setUp() {
        properties = new MqProperties();
        properties.getRabbitmq().setConfirmTimeoutMs(1000L);
        strategy = new RabbitMqStrategy(rabbitTemplate, properties, new MqJsonCodec());
    }

    @Test
    void shouldSendPersistentMessageAndReturnSuccessOnConfirmAck() {
        MqMessage<String> msg = MqMessage.of("coupon-seckill", "grab", "ORD100", "payload-body");

        doAnswer(invocation -> {
            CorrelationData cd = invocation.getArgument(3);
            cd.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbitTemplate).send(eq("sz.mq.direct.coupon-seckill"), eq("grab"), any(Message.class), any(CorrelationData.class));

        MqSendResult result = strategy.send(msg);

        assertTrue(result.isSuccess());
        assertEquals(MqTypeEnum.RABBITMQ, result.getMqType());
        assertEquals(msg.getMessageId(), result.getMessageId());

        ArgumentCaptor<Message> msgCaptor = ArgumentCaptor.forClass(Message.class);
        verify(rabbitTemplate).send(eq("sz.mq.direct.coupon-seckill"), eq("grab"), msgCaptor.capture(), any(CorrelationData.class));
        assertEquals(MessageDeliveryMode.PERSISTENT, msgCaptor.getValue().getMessageProperties().getDeliveryMode());
        assertEquals("ORD100", msgCaptor.getValue().getMessageProperties().getCorrelationId());
    }

    @Test
    void shouldThrowMqSendExceptionOnBrokerNack() {
        MqMessage<String> msg = MqMessage.of("coupon-seckill", "grab", "ORD100", "payload-body");

        doAnswer(invocation -> {
            CorrelationData cd = invocation.getArgument(3);
            cd.getFuture().complete(new CorrelationData.Confirm(false, "disk full"));
            return null;
        }).when(rabbitTemplate).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));

        MqSendException ex = assertThrows(MqSendException.class, () -> strategy.send(msg));
        assertTrue(ex.getMessage().contains("NACK"));
    }

    @Test
    void shouldThrowMqSendExceptionWhenMessageIsReturnedUnroutable() {
        MqMessage<String> msg = MqMessage.of("coupon-seckill", "grab", "ORD100", "payload-body");

        doAnswer(invocation -> {
            CorrelationData cd = invocation.getArgument(3);
            ReturnedMessage returned = new ReturnedMessage(invocation.getArgument(2), 312, "NO_ROUTE", "ex", "rk");
            cd.setReturned(returned);
            cd.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbitTemplate).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));

        MqSendException ex = assertThrows(MqSendException.class, () -> strategy.send(msg));
        assertTrue(ex.getMessage().contains("Returned"));
    }

    @Test
    void shouldThrowMqSendExceptionOnConfirmTimeout() {
        properties.getRabbitmq().setConfirmTimeoutMs(50L);
        MqMessage<String> msg = MqMessage.of("coupon-seckill", "grab", "ORD100", "payload-body");

        MqSendException ex = assertThrows(MqSendException.class, () -> strategy.send(msg));
        assertTrue(ex.getMessage().contains("超时"));
    }
}
