package com.sz.mq.core;

import lombok.Getter;

import java.util.Arrays;

/**
 * 支持的消息队列引擎类型
 */
@Getter
public enum MqTypeEnum {

    RABBITMQ("rabbitmq"),
    ROCKETMQ("rocketmq"),
    KAFKA("kafka");

    private final String code;

    MqTypeEnum(String code) {
        this.code = code;
    }

    public static MqTypeEnum fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("MQ type 不能为空，合法值为: rabbitmq, rocketmq, kafka");
        }
        String normalized = code.trim().toLowerCase();
        return Arrays.stream(values())
                .filter(item -> item.code.equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "不支持的 MQ 类型: " + code + "，合法值为: rabbitmq, rocketmq, kafka"));
    }
}
