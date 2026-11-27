package com.sz.mq.strategy.impl;

import com.sz.mq.config.MqProperties;
import com.sz.mq.core.MqJsonCodec;
import com.sz.mq.core.MqMessage;
import com.sz.mq.core.MqSendException;
import com.sz.mq.core.MqSendResult;
import com.sz.mq.core.MqTypeEnum;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RocketMqStrategyTest {

    @Mock
    private RocketMQTemplate rocketMQTemplate;

    private MqProperties properties;
    private RocketMqStrategy strategy;

    @BeforeEach
    void setUp() {
        properties = new MqProperties();
        properties.setType(MqTypeEnum.ROCKETMQ);
        strategy = new RocketMqStrategy(rocketMQTemplate, properties, new MqJsonCodec());
    }

    @Test
    void shouldSendSynchronouslyWithTopicTagAndKeysHeader() {
        SendResult okResult = new SendResult();
        okResult.setSendStatus(SendStatus.SEND_OK);
        okResult.setMsgId("rmq-1");
        when(rocketMQTemplate.syncSend(eq("coupon-seckill:grab"), any(Message.class), eq(5000L))).thenReturn(okResult);

        MqMessage<String> msg = MqMessage.of("coupon-seckill", "grab", "ORD200", "hello");
        MqSendResult result = strategy.send(msg);

        assertTrue(result.isSuccess());
        assertEquals(MqTypeEnum.ROCKETMQ, result.getMqType());
        assertEquals("rmq-1", result.getRawMessageId());

        ArgumentCaptor<Message<String>> captor = ArgumentCaptor.forClass(Message.class);
        verify(rocketMQTemplate).syncSend(eq("coupon-seckill:grab"), captor.capture(), eq(5000L));
        assertEquals("ORD200", captor.getValue().getHeaders().get(RocketMQHeaders.KEYS));
    }

    @Test
    void shouldThrowMqSendExceptionWhenStatusIsNotSendOk() {
        SendResult failedResult = new SendResult();
        failedResult.setSendStatus(SendStatus.FLUSH_DISK_TIMEOUT);
        when(rocketMQTemplate.syncSend(anyString(), any(Message.class), anyLong())).thenReturn(failedResult);

        MqMessage<String> msg = MqMessage.of("coupon-seckill", "grab", "ORD201", "hello");
        assertThrows(MqSendException.class, () -> strategy.send(msg));
    }
}
