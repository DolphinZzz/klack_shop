package com.sz.mq.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.*;

class MqAutoConfigurationImportFilterTest {

    private static final String[] CANDIDATES = {
            "org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration",
            "org.springframework.boot.actuate.autoconfigure.amqp.RabbitHealthContributorAutoConfiguration",
            "org.springframework.boot.actuate.autoconfigure.metrics.amqp.RabbitMetricsAutoConfiguration",
            "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration",
            "org.apache.rocketmq.spring.autoconfigure.RocketMQAutoConfiguration",
            "com.sz.mq.config.MqAutoConfiguration"
    };

    @Test
    void shouldAllowOnlyRabbitMqWhenTypeIsRabbitmq() {
        boolean[] matches = runFilter("true", "rabbitmq");
        assertArrayEquals(new boolean[]{true, true, true, false, false, true}, matches);
    }

    @Test
    void shouldAllowOnlyKafkaAndSuppressRabbitActuatorWhenTypeIsKafka() {
        boolean[] matches = runFilter("true", "kafka");
        assertArrayEquals(new boolean[]{false, false, false, true, false, true}, matches);
    }

    @Test
    void shouldAllowOnlyRocketMqWhenTypeIsRocketmq() {
        boolean[] matches = runFilter("true", "rocketmq");
        assertArrayEquals(new boolean[]{false, false, false, false, true, true}, matches);
    }

    @Test
    void shouldSuppressAllThreeBrokersWhenDisabled() {
        boolean[] matches = runFilter("false", "rabbitmq");
        assertArrayEquals(new boolean[]{false, false, false, false, false, true}, matches);
    }

    @Test
    void shouldSuppressAllThreeBrokersWhenTypeIsUnknown() {
        boolean[] matches = runFilter("true", "local");
        assertArrayEquals(new boolean[]{false, false, false, false, false, true}, matches);
    }

    private boolean[] runFilter(String enabled, String type) {
        MockEnvironment env = new MockEnvironment()
                .withProperty("sz.mq.enabled", enabled)
                .withProperty("sz.mq.type", type);
        MqAutoConfigurationImportFilter filter = new MqAutoConfigurationImportFilter();
        filter.setEnvironment(env);
        return filter.match(CANDIDATES, null);
    }
}
