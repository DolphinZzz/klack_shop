package com.sz.mq.core;

/**
 * 消息队列同步发送失败异常
 */
public class MqSendException extends RuntimeException {

    public MqSendException(String message) {
        super(message);
    }

    public MqSendException(String message, Throwable cause) {
        super(message, cause);
    }
}
