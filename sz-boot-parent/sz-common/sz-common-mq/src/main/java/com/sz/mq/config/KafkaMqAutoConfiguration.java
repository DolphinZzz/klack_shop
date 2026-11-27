package com.sz.mq.config;

import com.sz.mq.core.MqJsonCodec;
import com.sz.mq.store.MqMessageLogStore;
import com.sz.mq.strategy.impl.KafkaConsumerRegistrar;
import com.sz.mq.strategy.impl.KafkaMqStrategy;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;

/**
 * Kafka 引擎自动配置
 */
@AutoConfiguration(after = KafkaAutoConfiguration.class)
@ConditionalOnClass({KafkaTemplate.class, ConcurrentMessageListenerContainer.class})
@ConditionalOnProperty(prefix = "sz.mq", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnProperty(prefix = "sz.mq", name = "type", havingValue = "kafka")
public class KafkaMqAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public KafkaMqStrategy kafkaMqStrategy(KafkaTemplate<String, String> kafkaTemplate,
                                           MqProperties mqProperties,
                                           MqJsonCodec mqJsonCodec) {
        return new KafkaMqStrategy(kafkaTemplate, mqProperties, mqJsonCodec);
    }

    @Bean
    @ConditionalOnMissingBean
    public KafkaConsumerRegistrar kafkaConsumerRegistrar(KafkaTemplate<String, String> kafkaTemplate,
                                                         ConsumerFactory<String, String> consumerFactory,
                                                         ObjectProvider<KafkaAdmin> kafkaAdminProvider,
                                                         MqProperties mqProperties,
                                                         MqJsonCodec mqJsonCodec,
                                                         ObjectProvider<MqMessageLogStore> logStoreProvider) {
        return new KafkaConsumerRegistrar(
                kafkaTemplate,
                consumerFactory,
                kafkaAdminProvider.getIfAvailable(),
                mqProperties,
                mqJsonCodec,
                logStoreProvider::getIfAvailable
        );
    }
}
