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
import com.sz.mq.store.MqMessageLogStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 基于 MyBatis-Flex 的 MQ 发送与消费记录持久化实现
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MybatisMqMessageLogStore implements MqMessageLogStore {

    public static final String SEND_STATUS_PENDING = "PENDING";
    public static final String SEND_STATUS_SENT = "SENT";
    public static final String SEND_STATUS_FAILED = "FAILED";

    public static final String CONSUME_STATUS_CONSUMING = "CONSUMING";
    public static final String CONSUME_STATUS_SUCCESS = "SUCCESS";
    public static final String CONSUME_STATUS_RETRYING = "RETRYING";
    public static final String CONSUME_STATUS_DEAD = "DEAD";

    public static final String ACK_STATUS_PENDING = "PENDING";

    private static final int MAX_ERROR_LEN = 1000;

    private final SysMqSendLogMapper sendLogMapper;
    private final SysMqConsumeLogMapper consumeLogMapper;

    @Override
    public void recordSendPending(MqMessage<?> message, MqTypeEnum mqType, String payloadJson) {
        LocalDateTime now = LocalDateTime.now();
        SysMqSendLog record = new SysMqSendLog()
                .setMessageId(message.getMessageId())
                .setMqType(mqType.getCode())
                .setTopic(message.getTopic())
                .setTag(message.getTag())
                .setBizKey(message.getBizKey())
                .setPayload(payloadJson)
                .setSendStatus(SEND_STATUS_PENDING)
                .setCreateTime(now)
                .setUpdateTime(now);
        sendLogMapper.insert(record);
    }

    @Override
    public void recordSendSuccess(String messageId, MqSendResult result) {
        LocalDateTime now = LocalDateTime.now();
        SysMqSendLog update = new SysMqSendLog()
                .setSendStatus(SEND_STATUS_SENT)
                .setRawMessageId(result == null ? null : result.getRawMessageId())
                .setErrorMsg("")
                .setSentTime(now)
                .setUpdateTime(now);
        sendLogMapper.updateByQuery(update, QueryWrapper.create().eq(SysMqSendLog::getMessageId, messageId));
    }

    @Override
    public void recordSendFailure(String messageId, Throwable cause) {
        SysMqSendLog update = new SysMqSendLog()
                .setSendStatus(SEND_STATUS_FAILED)
                .setErrorMsg(truncateError(cause))
                .setUpdateTime(LocalDateTime.now());
        sendLogMapper.updateByQuery(update, QueryWrapper.create().eq(SysMqSendLog::getMessageId, messageId));
    }

    @Override
    public MqConsumeAttemptDecision tryBeginConsume(MqMessage<?> message,
                                                    String consumerGroup,
                                                    MqTypeEnum mqType,
                                                    String payloadJson) {
        SysMqConsumeLog existing = findConsumeLog(message.getMessageId(), consumerGroup);
        if (existing != null) {
            return handleExistingConsumeLog(existing);
        }

        LocalDateTime now = LocalDateTime.now();
        SysMqConsumeLog insert = new SysMqConsumeLog()
                .setMessageId(message.getMessageId())
                .setConsumerGroup(consumerGroup)
                .setMqType(mqType.getCode())
                .setTopic(message.getTopic())
                .setTag(message.getTag())
                .setBizKey(message.getBizKey())
                .setPayload(payloadJson)
                .setConsumeStatus(CONSUME_STATUS_CONSUMING)
                .setAckStatus(ACK_STATUS_PENDING)
                .setRetryCount(0)
                .setReceiveTime(now)
                .setCreateTime(now)
                .setUpdateTime(now);
        try {
            consumeLogMapper.insert(insert);
            return MqConsumeAttemptDecision.PROCEED;
        } catch (DuplicateKeyException dupEx) {
            SysMqConsumeLog raced = findConsumeLog(message.getMessageId(), consumerGroup);
            if (raced != null) {
                return handleExistingConsumeLog(raced);
            }
            throw dupEx;
        }
    }

    @Override
    public void recordConsumeSuccess(String messageId, String consumerGroup) {
        LocalDateTime now = LocalDateTime.now();
        SysMqConsumeLog update = new SysMqConsumeLog()
                .setConsumeStatus(CONSUME_STATUS_SUCCESS)
                .setConsumeTime(now)
                .setErrorMsg("")
                .setUpdateTime(now);
        consumeLogMapper.updateByQuery(update, consumeKeyWrapper(messageId, consumerGroup));
    }

    @Override
    public void recordConsumeRetry(String messageId, String consumerGroup, int retryCount, Throwable cause) {
        SysMqConsumeLog update = new SysMqConsumeLog()
                .setConsumeStatus(CONSUME_STATUS_RETRYING)
                .setRetryCount(retryCount)
                .setErrorMsg(truncateError(cause))
                .setUpdateTime(LocalDateTime.now());
        consumeLogMapper.updateByQuery(update, consumeKeyWrapper(messageId, consumerGroup));
    }

    @Override
    public void recordConsumeDead(String messageId,
                                  String consumerGroup,
                                  int retryCount,
                                  String ackStatus,
                                  Throwable cause) {
        LocalDateTime now = LocalDateTime.now();
        SysMqConsumeLog update = new SysMqConsumeLog()
                .setConsumeStatus(CONSUME_STATUS_DEAD)
                .setAckStatus(ackStatus)
                .setRetryCount(retryCount)
                .setErrorMsg(truncateError(cause))
                .setAckTime(now)
                .setUpdateTime(now);
        consumeLogMapper.updateByQuery(update, consumeKeyWrapper(messageId, consumerGroup));
    }

    @Override
    public void recordAck(String messageId, String consumerGroup, String ackStatus) {
        LocalDateTime now = LocalDateTime.now();
        SysMqConsumeLog update = new SysMqConsumeLog()
                .setAckStatus(ackStatus)
                .setAckTime(now)
                .setUpdateTime(now);
        consumeLogMapper.updateByQuery(update, consumeKeyWrapper(messageId, consumerGroup));
    }

    private MqConsumeAttemptDecision handleExistingConsumeLog(SysMqConsumeLog existing) {
        if (CONSUME_STATUS_SUCCESS.equals(existing.getConsumeStatus())) {
            return MqConsumeAttemptDecision.ALREADY_CONSUMED;
        }
        // 对于 DEAD（死信人工重放）或 RETRYING / 未完成状态的重新投递，重置为 CONSUMING 允许继续消费
        LocalDateTime now = LocalDateTime.now();
        SysMqConsumeLog reset = new SysMqConsumeLog()
                .setId(existing.getId())
                .setConsumeStatus(CONSUME_STATUS_CONSUMING)
                .setAckStatus(ACK_STATUS_PENDING)
                .setReceiveTime(now)
                .setErrorMsg("")
                .setUpdateTime(now);
        consumeLogMapper.update(reset);
        return MqConsumeAttemptDecision.PROCEED;
    }

    private SysMqConsumeLog findConsumeLog(String messageId, String consumerGroup) {
        return consumeLogMapper.selectOneByQuery(consumeKeyWrapper(messageId, consumerGroup));
    }

    private QueryWrapper consumeKeyWrapper(String messageId, String consumerGroup) {
        return QueryWrapper.create()
                .eq(SysMqConsumeLog::getMessageId, messageId)
                .eq(SysMqConsumeLog::getConsumerGroup, consumerGroup);
    }

    static String truncateError(Throwable cause) {
        if (cause == null) {
            return "";
        }
        String raw = cause.getClass().getSimpleName()
                + (cause.getMessage() != null ? ": " + cause.getMessage() : "");
        if (raw.length() <= MAX_ERROR_LEN) {
            return raw;
        }
        return raw.substring(0, MAX_ERROR_LEN - 3) + "...";
    }
}
