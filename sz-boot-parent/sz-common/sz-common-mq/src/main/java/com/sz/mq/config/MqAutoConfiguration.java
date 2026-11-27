package com.sz.mq.config;

import com.sz.mq.consumer.MqConsumerContainerRegistrar;
import com.sz.mq.consumer.MqConsumerHandler;
import com.sz.mq.consumer.MqConsumerLifecycle;
import com.sz.mq.core.MqJsonCodec;
import com.sz.mq.store.MqMessageLogStore;
import com.sz.mq.strategy.MqStrategy;
import com.sz.mq.strategy.MqTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * sz-common-mq 核心自动配置入口
 */
@AutoConfiguration
@EnableConfigurationProperties(MqProperties.class)
public class MqAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MqJsonCodec mqJsonCodec() {
        return new MqJsonCodec();
    }

    @Bean
    @ConditionalOnMissingBean
    public MqTemplate mqTemplate(MqProperties mqProperties,
                                 ObjectProvider<List<MqStrategy>> strategiesProvider,
                                 MqJsonCodec mqJsonCodec,
                                 ObjectProvider<MqMessageLogStore> logStoreProvider) {
        return new MqTemplate(
                mqProperties,
                strategiesProvider.getIfAvailable(List::of),
                mqJsonCodec,
                logStoreProvider::getIfAvailable
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public MqConsumerLifecycle mqConsumerLifecycle(MqProperties mqProperties,
                                                   ObjectProvider<List<MqConsumerHandler<?>>> handlersProvider,
                                                   ObjectProvider<List<MqConsumerContainerRegistrar>> registrarsProvider) {
        return new MqConsumerLifecycle(mqProperties, handlersProvider, registrarsProvider);
    }
}
