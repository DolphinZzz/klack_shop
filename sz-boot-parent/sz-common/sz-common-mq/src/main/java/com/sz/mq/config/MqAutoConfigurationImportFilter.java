package com.sz.mq.config;

import com.sz.mq.core.MqTypeEnum;
import org.springframework.boot.autoconfigure.AutoConfigurationImportFilter;
import org.springframework.boot.autoconfigure.AutoConfigurationMetadata;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

import java.util.Arrays;

/**
 * 根据 sz.mq.enabled 与 sz.mq.type 在自动装配阶段屏蔽未启用 MQ 的 Starter 与 Actuator 健康检查，
 * 避免未启动的中间件导致应用启动失败或健康检查报 DOWN。
 */
public class MqAutoConfigurationImportFilter implements AutoConfigurationImportFilter, EnvironmentAware {

    private static final String[] RABBIT_PREFIXES = {
            "org.springframework.boot.autoconfigure.amqp.",
            "org.springframework.boot.actuate.autoconfigure.amqp.",
            "org.springframework.boot.actuate.autoconfigure.metrics.amqp."
    };

    private static final String[] KAFKA_PREFIXES = {
            "org.springframework.boot.autoconfigure.kafka.",
            "org.springframework.boot.actuate.autoconfigure.kafka.",
            "org.springframework.boot.actuate.autoconfigure.metrics.kafka."
    };

    private static final String[] ROCKET_PREFIXES = {
            "org.apache.rocketmq.spring.autoconfigure."
    };

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public boolean[] match(String[] autoConfigurationClasses, AutoConfigurationMetadata autoConfigurationMetadata) {
        boolean[] matches = new boolean[autoConfigurationClasses.length];
        Arrays.fill(matches, true);

        if (environment == null) {
            return matches;
        }

        boolean enabled = environment.getProperty("sz.mq.enabled", Boolean.class, true);
        MqTypeEnum activeType = resolveTypeOrNull(environment.getProperty("sz.mq.type", "rabbitmq"));

        for (int i = 0; i < autoConfigurationClasses.length; i++) {
            String className = autoConfigurationClasses[i];
            if (className == null) {
                continue;
            }
            if (isRabbitAutoConfig(className)) {
                matches[i] = enabled && activeType == MqTypeEnum.RABBITMQ;
            } else if (isKafkaAutoConfig(className)) {
                matches[i] = enabled && activeType == MqTypeEnum.KAFKA;
            } else if (isRocketAutoConfig(className)) {
                matches[i] = enabled && activeType == MqTypeEnum.ROCKETMQ;
            }
        }
        return matches;
    }

    private MqTypeEnum resolveTypeOrNull(String rawType) {
        try {
            return MqTypeEnum.fromCode(rawType);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private boolean isRabbitAutoConfig(String className) {
        return startsWithAny(className, RABBIT_PREFIXES);
    }

    private boolean isKafkaAutoConfig(String className) {
        return startsWithAny(className, KAFKA_PREFIXES);
    }

    private boolean isRocketAutoConfig(String className) {
        return startsWithAny(className, ROCKET_PREFIXES);
    }

    private boolean startsWithAny(String className, String[] prefixes) {
        for (String prefix : prefixes) {
            if (className.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
