package com.sz.mq.config;

import com.sz.mq.core.MqJsonCodec;
import com.sz.mq.store.MqMessageLogStore;
import com.sz.mq.strategy.impl.RocketMqConsumerRegistrar;
import com.sz.mq.strategy.impl.RocketMqStrategy;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.spring.autoconfigure.RocketMQAutoConfiguration;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * RocketMQ 引擎自动配置
 */
@AutoConfiguration(after = RocketMQAutoConfiguration.class)
@ConditionalOnClass({RocketMQTemplate.class, DefaultMQPushConsumer.class})
@ConditionalOnProperty(prefix = "sz.mq", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnProperty(prefix = "sz.mq", name = "type", havingValue = "rocketmq")
public class RocketMqEngineAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RocketMqStrategy rocketMqStrategy(RocketMQTemplate rocketMQTemplate,
                                             MqProperties mqProperties,
                                             MqJsonCodec mqJsonCodec) {
        return new RocketMqStrategy(rocketMQTemplate, mqProperties, mqJsonCodec);
    }

    @Bean
    @ConditionalOnMissingBean
    public RocketMqConsumerRegistrar rocketMqConsumerRegistrar(Environment environment,
                                                               MqProperties mqProperties,
                                                               MqJsonCodec mqJsonCodec,
                                                               ObjectProvider<MqMessageLogStore> logStoreProvider) {
        String nameServer = environment.getProperty("rocketmq.name-server");
        return new RocketMqConsumerRegistrar(nameServer, mqProperties, mqJsonCodec, logStoreProvider::getIfAvailable);
    }
}
