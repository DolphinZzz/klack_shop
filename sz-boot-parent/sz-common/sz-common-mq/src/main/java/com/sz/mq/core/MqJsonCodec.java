package com.sz.mq.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.TimeZone;

/**
 * MQ 专用独立 JSON 编解码器。
 * <p>
 * 刻意不复用 Spring 上下文中的 @Primary ObjectMapper，避免全局 null -> "" 序列化器污染消息结构。
 */
public class MqJsonCodec {

    private final ObjectMapper objectMapper;

    public MqJsonCodec() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        this.objectMapper = mapper;
    }

    public String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("MQ 消息序列化失败: " + e.getMessage(), e);
        }
    }

    public byte[] toJsonBytes(Object value) {
        return toJson(value).getBytes(StandardCharsets.UTF_8);
    }

    public <T> MqMessage<T> parseMessage(String json, Class<T> payloadType) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("MQ 消息 JSON 不能为空");
        }
        try {
            JavaType type = objectMapper.getTypeFactory().constructParametricType(MqMessage.class, payloadType);
            return objectMapper.readValue(json, type);
        } catch (IOException e) {
            throw new IllegalArgumentException("MQ 消息反序列化失败: " + e.getMessage(), e);
        }
    }

    public <T> MqMessage<T> parseMessage(byte[] bytes, Class<T> payloadType) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("MQ 消息字节数组不能为空");
        }
        return parseMessage(new String(bytes, StandardCharsets.UTF_8), payloadType);
    }
}
