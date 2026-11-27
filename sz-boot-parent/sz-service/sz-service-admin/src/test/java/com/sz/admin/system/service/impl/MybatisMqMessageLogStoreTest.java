package com.sz.admin.system.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.sz.admin.system.mapper.SysMqConsumeLogMapper;
import com.sz.admin.system.mapper.SysMqSendLogMapper;
import com.sz.admin.system.pojo.po.SysMqConsumeLog;
import com.sz.admin.system.pojo.po.SysMqSendLog;
import com.sz.mq.core.MqMessage;
import com.sz.mq.core.MqSendResult;
import com.sz.mq.core.MqTypeEnum;
import com.sz.mq.store.MqConsumeAttemptDecision;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MybatisMqMessageLogStoreTest {

    @Mock
    private SysMqSendLogMapper sendLogMapper;
    @Mock
    private SysMqConsumeLogMapper consumeLogMapper;

    private MybatisMqMessageLogStore store;

    @BeforeEach
    void setUp() {
        store = new MybatisMqMessageLogStore(sendLogMapper, consumeLogMapper);
    }

    @Test
    void shouldInsertPendingThenUpdateSentAndFailed() {
        MqMessage<String> msg = MqMessage.of("coupon-seckill", "grab", "ORDCP1", "payload");
        store.recordSendPending(msg, MqTypeEnum.RABBITMQ, "{\"orderId\":\"ORDCP1\"}");

        ArgumentCaptor<SysMqSendLog> insertCaptor = ArgumentCaptor.forClass(SysMqSendLog.class);
        verify(sendLogMapper).insert(insertCaptor.capture());
        assertEquals(msg.getMessageId(), insertCaptor.getValue().getMessageId());
        assertEquals("PENDING", insertCaptor.getValue().getSendStatus());
        assertEquals("rabbitmq", insertCaptor.getValue().getMqType());

        store.recordSendSuccess(msg.getMessageId(), MqSendResult.builder()
                .success(true).messageId(msg.getMessageId()).mqType(MqTypeEnum.RABBITMQ).rawMessageId("raw-1").build());

        ArgumentCaptor<SysMqSendLog> sentCaptor = ArgumentCaptor.forClass(SysMqSendLog.class);
        verify(sendLogMapper).updateByQuery(sentCaptor.capture(), any(QueryWrapper.class));
        assertEquals("SENT", sentCaptor.getValue().getSendStatus());
        assertEquals("raw-1", sentCaptor.getValue().getRawMessageId());
        assertNotNull(sentCaptor.getValue().getSentTime());

        store.recordSendFailure(msg.getMessageId(), new RuntimeException("broker down"));
        verify(sendLogMapper, times(2)).updateByQuery(any(SysMqSendLog.class), any(QueryWrapper.class));
    }

    @Test
    void shouldInsertConsumingOnFirstReceiveAndReturnAlreadyConsumedWhenSuccess() {
        MqMessage<String> msg = MqMessage.of("coupon-seckill", "grab", "ORDCP2", "payload");

        when(consumeLogMapper.selectOneByQuery(any(QueryWrapper.class))).thenReturn(null);
        MqConsumeAttemptDecision first = store.tryBeginConsume(msg, "coupon-seckill-group", MqTypeEnum.RABBITMQ, "{}");
        assertEquals(MqConsumeAttemptDecision.PROCEED, first);
        verify(consumeLogMapper).insert(any(SysMqConsumeLog.class));

        SysMqConsumeLog succeeded = new SysMqConsumeLog().setId(10L).setConsumeStatus("SUCCESS");
        when(consumeLogMapper.selectOneByQuery(any(QueryWrapper.class))).thenReturn(succeeded);
        MqConsumeAttemptDecision second = store.tryBeginConsume(msg, "coupon-seckill-group", MqTypeEnum.RABBITMQ, "{}");
        assertEquals(MqConsumeAttemptDecision.ALREADY_CONSUMED, second);
    }

    @Test
    void shouldAllowReEntryWhenExistingRecordIsDeadForDlqReplay() {
        MqMessage<String> msg = MqMessage.of("coupon-seckill", "grab", "ORDCP3", "payload");
        SysMqConsumeLog dead = new SysMqConsumeLog().setId(11L).setConsumeStatus("DEAD");
        when(consumeLogMapper.selectOneByQuery(any(QueryWrapper.class))).thenReturn(dead);

        MqConsumeAttemptDecision decision = store.tryBeginConsume(msg, "coupon-seckill-group", MqTypeEnum.RABBITMQ, "{}");
        assertEquals(MqConsumeAttemptDecision.PROCEED, decision);

        ArgumentCaptor<SysMqConsumeLog> updateCaptor = ArgumentCaptor.forClass(SysMqConsumeLog.class);
        verify(consumeLogMapper).update(updateCaptor.capture());
        assertEquals(11L, updateCaptor.getValue().getId());
        assertEquals("CONSUMING", updateCaptor.getValue().getConsumeStatus());
        assertEquals("PENDING", updateCaptor.getValue().getAckStatus());
    }

    @Test
    void shouldHandleDuplicateKeyRaceDuringTryBeginConsume() {
        MqMessage<String> msg = MqMessage.of("coupon-seckill", "grab", "ORDCP4", "payload");
        SysMqConsumeLog racedSuccess = new SysMqConsumeLog().setId(12L).setConsumeStatus("SUCCESS");
        when(consumeLogMapper.selectOneByQuery(any(QueryWrapper.class))).thenReturn(null, racedSuccess);
        when(consumeLogMapper.insert(any(SysMqConsumeLog.class))).thenThrow(new DuplicateKeyException("dup"));

        MqConsumeAttemptDecision decision = store.tryBeginConsume(msg, "coupon-seckill-group", MqTypeEnum.RABBITMQ, "{}");
        assertEquals(MqConsumeAttemptDecision.ALREADY_CONSUMED, decision);
    }

    @Test
    void shouldRecordSuccessRetryDeadAckAndTruncateLongError() {
        store.recordConsumeSuccess("m1", "g1");
        store.recordConsumeRetry("m1", "g1", 1, new RuntimeException("timeout"));
        store.recordConsumeDead("m1", "g1", 3, "NACKED_DLQ", new RuntimeException("x".repeat(1500)));
        store.recordAck("m1", "g1", "ACKED");

        verify(consumeLogMapper, times(4)).updateByQuery(any(SysMqConsumeLog.class), any(QueryWrapper.class));
        String truncated = MybatisMqMessageLogStore.truncateError(new RuntimeException("a".repeat(1500)));
        assertEquals(1000, truncated.length());
        assertTrue(truncated.endsWith("..."));
    }
}
