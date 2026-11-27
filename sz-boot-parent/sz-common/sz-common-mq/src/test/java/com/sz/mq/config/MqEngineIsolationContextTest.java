package com.sz.mq.config;

import com.sz.mq.core.MqSendException;
import com.sz.mq.core.MqTypeEnum;
import com.sz.mq.strategy.MqTemplate;
import com.sz.mq.strategy.impl.KafkaConsumerRegistrar;
import com.sz.mq.strategy.impl.KafkaMqStrategy;
import com.sz.mq.strategy.impl.RabbitMqConsumerRegistrar;
import com.sz.mq.strategy.impl.RabbitMqStrategy;
import com.sz.mq.strategy.impl.RocketMqConsumerRegistrar;
import com.sz.mq.strategy.impl.RocketMqStrategy;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证在真实 Spring Boot AutoConfiguration + MqAutoConfigurationImportFilter 下，
 * 三大 MQ 引擎与 Actuator 组件能够严格按 YAML 隔离。
 */
class MqEngineIsolationContextTest {

    @Test
    void shouldLoadOnlyRabbitMqBeansWhenTypeIsRabbitmq() {
        try (ConfigurableApplicationContext ctx = bootContext(
                "sz.mq.enabled=true",
                "sz.mq.type=rabbitmq",
                "spring.rabbitmq.host=127.0.0.1"
        )) {
            assertEquals(1, ctx.getBeansOfType(RabbitMqStrategy.class).size());
            assertEquals(1, ctx.getBeansOfType(RabbitMqConsumerRegistrar.class).size());
            assertTrue(ctx.getBeansOfType(KafkaMqStrategy.class).isEmpty());
            assertTrue(ctx.getBeansOfType(KafkaConsumerRegistrar.class).isEmpty());
            assertTrue(ctx.getBeansOfType(RocketMqStrategy.class).isEmpty());
            assertTrue(ctx.getBeansOfType(RocketMqConsumerRegistrar.class).isEmpty());
            assertEquals(MqTypeEnum.RABBITMQ, ctx.getBean(MqTemplate.class).currentType());
        }
    }

    @Test
    void shouldLoadOnlyKafkaAndExcludeRabbitHealthWhenTypeIsKafka() {
        try (ConfigurableApplicationContext ctx = bootContext(
                "sz.mq.enabled=true",
                "sz.mq.type=kafka",
                "spring.kafka.bootstrap-servers=127.0.0.1:9092"
        )) {
            assertEquals(1, ctx.getBeansOfType(KafkaMqStrategy.class).size());
            assertEquals(1, ctx.getBeansOfType(KafkaConsumerRegistrar.class).size());
            assertTrue(ctx.getBeansOfType(RabbitMqStrategy.class).isEmpty());
            assertTrue(ctx.getBeansOfType(RabbitMqConsumerRegistrar.class).isEmpty());
            assertTrue(ctx.getBeansOfType(RocketMqStrategy.class).isEmpty());
            assertFalse(ctx.containsBean("rabbitHealthContributor"));
            assertEquals(MqTypeEnum.KAFKA, ctx.getBean(MqTemplate.class).currentType());
        }
    }

    @Test
    void shouldBootWithNoEngineBeansWhenMqDisabled() {
        try (ConfigurableApplicationContext ctx = bootContext(
                "sz.mq.enabled=false",
                "sz.mq.type=rabbitmq"
        )) {
            assertTrue(ctx.getBeansOfType(RabbitMqStrategy.class).isEmpty());
            assertTrue(ctx.getBeansOfType(KafkaMqStrategy.class).isEmpty());
            assertTrue(ctx.getBeansOfType(RocketMqStrategy.class).isEmpty());
            MqTemplate template = ctx.getBean(MqTemplate.class);
            assertThrows(MqSendException.class, () -> template.send("coupon-seckill", "payload"));
        }
    }

    private ConfigurableApplicationContext bootContext(String... properties) {
        return new SpringApplicationBuilder(TestBootApp.class)
                .web(WebApplicationType.NONE)
                .properties("spring.jmx.enabled=false")
                .properties(properties)
                .run();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestBootApp {
    }
}
