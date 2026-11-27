package com.sz.mq.core;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class MqJsonCodecTest {

    private final MqJsonCodec codec = new MqJsonCodec();

    @Test
    void shouldPreserveNullFieldsInsteadOfConvertingToEmptyStringAndRoundTripTypes() {
        LocalDateTime now = LocalDateTime.of(2026, 11, 25, 17, 30, 0);
        SamplePayload payload = new SamplePayload(9007199254740999L, null, now);
        MqMessage<SamplePayload> message = MqMessage.of("coupon-seckill", "grab", "ORD123", payload);

        String json = codec.toJson(message);

        assertTrue(json.contains("\"note\":null"), "null 字段应保留为 JSON null，而不是空字符串: " + json);
        assertFalse(json.contains("\"note\":\"\""));

        MqMessage<SamplePayload> parsed = codec.parseMessage(json, SamplePayload.class);
        assertEquals(message.getMessageId(), parsed.getMessageId());
        assertEquals("coupon-seckill", parsed.getTopic());
        assertEquals("grab", parsed.getTag());
        assertEquals("ORD123", parsed.getBizKey());
        assertNotNull(parsed.getPayload());
        assertEquals(9007199254740999L, parsed.getPayload().getId());
        assertNull(parsed.getPayload().getNote());
        assertEquals(now, parsed.getPayload().getCreatedAt());
    }

    @Test
    void shouldThrowIllegalArgumentExceptionOnInvalidJson() {
        assertThrows(IllegalArgumentException.class, () -> codec.parseMessage("{bad json", SamplePayload.class));
        assertThrows(IllegalArgumentException.class, () -> codec.parseMessage("", SamplePayload.class));
        assertThrows(IllegalArgumentException.class, () -> codec.parseMessage(new byte[0], SamplePayload.class));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class SamplePayload {
        private Long id;
        private String note;
        private LocalDateTime createdAt;
    }
}
