package com.sz.mq.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 统一消息信封
 *
 * @param <T> 业务载荷类型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MqMessage<T> {

    private String messageId;

    private String topic;

    private String tag;

    private String bizKey;

    private T payload;

    private long timestamp;

    public static <T> MqMessage<T> of(String topic, String tag, String bizKey, T payload) {
        String resolvedTag = (tag == null || tag.isBlank()) ? "default" : tag;
        return MqMessage.<T>builder()
                .messageId(UUID.randomUUID().toString())
                .topic(topic)
                .tag(resolvedTag)
                .bizKey(bizKey)
                .payload(payload)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
