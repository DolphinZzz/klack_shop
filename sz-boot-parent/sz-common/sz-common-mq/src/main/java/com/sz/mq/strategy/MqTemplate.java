package com.sz.mq.strategy;

import com.sz.mq.config.MqProperties;
import com.sz.mq.core.MqJsonCodec;
import com.sz.mq.core.MqMessage;
import com.sz.mq.core.MqSendException;
import com.sz.mq.core.MqSendResult;
import com.sz.mq.core.MqTypeEnum;
import com.sz.mq.store.MqMessageLogStore;
import lombok.extern.slf4j.Slf4j;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * MQ 策略上下文与统一发送门面
 */
@Slf4j
public class MqTemplate {

    private final MqProperties mqProperties;
    private final Map<MqTypeEnum, MqStrategy> strategyMap;
    private final MqJsonCodec mqJsonCodec;
    private final Supplier<MqMessageLogStore> logStoreSupplier;

    public MqTemplate(MqProperties mqProperties, List<MqStrategy> strategies) {
        this(mqProperties, strategies, new MqJsonCodec(), (Supplier<MqMessageLogStore>) null);
    }

    public MqTemplate(MqProperties mqProperties,
                      List<MqStrategy> strategies,
                      MqJsonCodec mqJsonCodec,
                      MqMessageLogStore logStore) {
        this(mqProperties, strategies, mqJsonCodec, logStore == null ? null : () -> logStore);
    }

    public MqTemplate(MqProperties mqProperties,
                      List<MqStrategy> strategies,
                      MqJsonCodec mqJsonCodec,
                      Supplier<MqMessageLogStore> logStoreSupplier) {
        this.mqProperties = mqProperties;
        this.mqJsonCodec = mqJsonCodec == null ? new MqJsonCodec() : mqJsonCodec;
        this.logStoreSupplier = logStoreSupplier;
        this.strategyMap = new EnumMap<>(MqTypeEnum.class);
        if (strategies != null) {
            for (MqStrategy strategy : strategies) {
                if (strategy != null && strategy.getType() != null) {
                    this.strategyMap.put(strategy.getType(), strategy);
                }
            }
        }
        if (mqProperties.isEnabled()) {
            MqTypeEnum activeType = mqProperties.getType();
            if (activeType == null || !this.strategyMap.containsKey(activeType)) {
                throw new IllegalStateException("MQ 已启用但未找到对应的发送策略实现，sz.mq.type=" + activeType);
            }
            log.info("[SZ-MQ] MqTemplate 初始化完成，当前激活策略={}", activeType.getCode());
        } else {
            log.info("[SZ-MQ] MqTemplate 初始化完成（当前 sz.mq.enabled=false）");
        }
    }

    public <T> MqSendResult send(String topic, T payload) {
        return send(topic, "default", null, payload);
    }

    public <T> MqSendResult send(String topic, String tag, String bizKey, T payload) {
        return send(MqMessage.of(topic, tag, bizKey, payload));
    }

    public <T> MqSendResult send(MqMessage<T> message) {
        MqStrategy strategy = resolveActiveStrategy();
        MqMessage<T> normalized = normalizeMessage(message);
        MqMessageLogStore logStore = resolveLogStore();

        if (logStore != null) {
            String payloadJson = serializePayloadSafe(normalized);
            try {
                logStore.recordSendPending(normalized, strategy.getType(), payloadJson);
            } catch (Exception e) {
                throw new MqSendException("MQ 发送记录入库失败，中止发送, messageId=" + normalized.getMessageId(), e);
            }
        }

        try {
            MqSendResult result = strategy.send(normalized);
            if (logStore != null) {
                safeRecordSendSuccess(logStore, normalized.getMessageId(), result);
            }
            return result;
        } catch (Throwable ex) {
            if (logStore != null) {
                safeRecordSendFailure(logStore, normalized.getMessageId(), ex);
            }
            if (ex instanceof RuntimeException runtimeEx) {
                throw runtimeEx;
            }
            throw new MqSendException("MQ 发送失败, messageId=" + normalized.getMessageId(), ex);
        }
    }

    public <T> void sendAsync(String topic, String tag, String bizKey, T payload) {
        sendAsync(MqMessage.of(topic, tag, bizKey, payload));
    }

    public <T> void sendAsync(MqMessage<T> message) {
        MqStrategy strategy = resolveActiveStrategy();
        MqMessage<T> normalized = normalizeMessage(message);
        MqMessageLogStore logStore = resolveLogStore();

        if (logStore != null) {
            String payloadJson = serializePayloadSafe(normalized);
            try {
                logStore.recordSendPending(normalized, strategy.getType(), payloadJson);
            } catch (Exception e) {
                throw new MqSendException("MQ 异步发送记录入库失败，中止发送, messageId=" + normalized.getMessageId(), e);
            }
        }

        try {
            strategy.sendAsync(normalized);
        } catch (Throwable ex) {
            if (logStore != null) {
                safeRecordSendFailure(logStore, normalized.getMessageId(), ex);
            }
            throw ex;
        }
    }

    public MqTypeEnum currentType() {
        return mqProperties.getType();
    }

    private MqMessageLogStore resolveLogStore() {
        return logStoreSupplier == null ? null : logStoreSupplier.get();
    }

    private String serializePayloadSafe(MqMessage<?> message) {
        try {
            return mqJsonCodec.toJson(message.getPayload());
        } catch (Exception e) {
            log.warn("[SZ-MQ] 序列化消息载荷用于入库失败, messageId={}", message.getMessageId(), e);
            return String.valueOf(message.getPayload());
        }
    }

    private void safeRecordSendSuccess(MqMessageLogStore logStore, String messageId, MqSendResult result) {
        try {
            logStore.recordSendSuccess(messageId, result);
        } catch (Exception e) {
            log.warn("[SZ-MQ] 更新 MQ 发送成功状态失败, messageId={}", messageId, e);
        }
    }

    private void safeRecordSendFailure(MqMessageLogStore logStore, String messageId, Throwable cause) {
        try {
            logStore.recordSendFailure(messageId, cause);
        } catch (Exception e) {
            log.warn("[SZ-MQ] 更新 MQ 发送失败状态失败, messageId={}", messageId, e);
        }
    }

    private MqStrategy resolveActiveStrategy() {
        if (!mqProperties.isEnabled()) {
            throw new MqSendException("MQ 模块已禁用（sz.mq.enabled=false），无法发送消息");
        }
        MqTypeEnum activeType = mqProperties.getType();
        MqStrategy strategy = activeType == null ? null : strategyMap.get(activeType);
        if (strategy == null) {
            throw new MqSendException("未找到可用的 MQ 发送策略: " + activeType);
        }
        return strategy;
    }

    private <T> MqMessage<T> normalizeMessage(MqMessage<T> source) {
        if (source == null) {
            throw new IllegalArgumentException("MqMessage 不能为空");
        }
        if (source.getTopic() == null || source.getTopic().isBlank()) {
            throw new IllegalArgumentException("MqMessage.topic 不能为空");
        }
        String messageId = (source.getMessageId() == null || source.getMessageId().isBlank())
                ? UUID.randomUUID().toString()
                : source.getMessageId();
        String tag = (source.getTag() == null || source.getTag().isBlank())
                ? "default"
                : source.getTag();
        long timestamp = source.getTimestamp() > 0 ? source.getTimestamp() : System.currentTimeMillis();
        return MqMessage.<T>builder()
                .messageId(messageId)
                .topic(source.getTopic())
                .tag(tag)
                .bizKey(source.getBizKey())
                .payload(source.getPayload())
                .timestamp(timestamp)
                .build();
    }
}
