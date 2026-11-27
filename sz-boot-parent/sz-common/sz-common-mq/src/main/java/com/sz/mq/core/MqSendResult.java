package com.sz.mq.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一消息发送结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MqSendResult {

    private boolean success;

    private String messageId;

    private MqTypeEnum mqType;

    private String rawMessageId;
}
