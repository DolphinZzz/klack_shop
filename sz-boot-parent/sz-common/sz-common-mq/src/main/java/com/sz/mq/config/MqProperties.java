package com.sz.mq.config;

import com.sz.mq.core.MqTypeEnum;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 统一消息队列配置属性
 */
@Data
@ConfigurationProperties(prefix = "sz.mq")
public class MqProperties {

    /**
     * 是否启用 MQ 模块
     */
    private boolean enabled = true;

    /**
     * 当前激活的消息队列策略类型：rabbitmq | rocketmq | kafka
     */
    private MqTypeEnum type = MqTypeEnum.RABBITMQ;

    /**
     * 生产端同步发送超时时间（毫秒）
     */
    private long sendTimeoutMs = 5000L;

    /**
     * 消费端通用配置
     */
    private Consumer consumer = new Consumer();

    /**
     * RabbitMQ 专属配置
     */
    private RabbitMq rabbitmq = new RabbitMq();

    /**
     * RocketMQ 专属配置
     */
    private RocketMq rocketmq = new RocketMq();

    /**
     * Kafka 专属配置
     */
    private Kafka kafka = new Kafka();

    @Data
    public static class Consumer {
        private int concurrency = 1;
        private int prefetch = 10;
        private int maxRetry = 3;
        private long retryInitialIntervalMs = 500L;
    }

    @Data
    public static class RabbitMq {
        private String exchangePrefix = "sz.mq.direct.";
        private String dlxPrefix = "sz.mq.dlx.";
        private String queuePrefix = "sz.mq.queue.";
        private String dlqPrefix = "sz.mq.dlq.";
        private boolean waitForConfirms = true;
        private long confirmTimeoutMs = 5000L;
    }

    @Data
    public static class RocketMq {
        private String consumerGroupPrefix = "sz-mq-consumer-";
        private int maxReconsumeTimes = 3;
    }

    @Data
    public static class Kafka {
        private String groupIdPrefix = "sz-mq-";
        private String dlqSuffix = "-dlt";
        private boolean autoCreateTopics = true;
        private int defaultPartitions = 3;
        private short defaultReplicas = 1;
    }
}
