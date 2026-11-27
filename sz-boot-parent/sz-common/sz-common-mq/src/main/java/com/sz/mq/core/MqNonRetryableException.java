package com.sz.mq.core;

/**
 * 消费端不可重试的确定性业务异常（不触发重试，直接回调 onConsumeFailed(..., false) 并转入死信）
 */
public class MqNonRetryableException extends RuntimeException {

    public MqNonRetryableException(String message) {
        super(message);
    }

    public MqNonRetryableException(String message, Throwable cause) {
        super(message, cause);
    }
}
