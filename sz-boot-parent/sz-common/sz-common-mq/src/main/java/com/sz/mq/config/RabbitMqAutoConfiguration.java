package com.sz.mq.config;

import com.rabbitmq.client.Channel;
import com.sz.mq.consumer.MqConsumerHandler;
import com.sz.mq.consumer.MqConsumerLifecycle;
import com.sz.mq.core.MqJsonCodec;
import com.sz.mq.store.MqMessageLogStore;
import com.sz.mq.strategy.impl.RabbitMqConsumerRegistrar;
import com.sz.mq.strategy.impl.RabbitMqStrategy;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.List;

/**
 * RabbitMQ 引擎自动配置与拓扑声明（Direct Exchange + Business Queue + DLX + DLQ）
 */
@AutoConfiguration(after = RabbitAutoConfiguration.class)
@ConditionalOnClass({RabbitTemplate.class, Channel.class})
@ConditionalOnProperty(prefix = "sz.mq", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnProperty(prefix = "sz.mq", name = "type", havingValue = "rabbitmq", matchIfMissing = true)
public class RabbitMqAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(AmqpAdmin.class)
    public RabbitAdmin szMqRabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        admin.setAutoStartup(true);
        return admin;
    }

    @Bean
    @ConditionalOnMissingBean(name = "szMqRabbitDeclarables")
    public Declarables szMqRabbitDeclarables(MqProperties properties,
                                            ObjectProvider<List<MqConsumerHandler<?>>> handlersProvider) {
        List<MqConsumerHandler<?>> handlers = handlersProvider.getIfAvailable(List::of);
        MqConsumerLifecycle.validateHandlers(handlers);

        List<Declarable> declarables = new ArrayList<>();
        MqProperties.RabbitMq rabbitCfg = properties.getRabbitmq();
        for (MqConsumerHandler<?> handler : handlers) {
            if (handler == null) {
                continue;
            }
            String topic = handler.topic();
            String tag = handler.tag();
            String group = handler.group();

            String bizExchangeName = rabbitCfg.getExchangePrefix() + topic;
            String dlxExchangeName = rabbitCfg.getDlxPrefix() + topic;
            String bizQueueName = rabbitCfg.getQueuePrefix() + topic + "." + group;
            String dlqQueueName = rabbitCfg.getDlqPrefix() + topic + "." + group;
            String dlqRoutingKey = group + "." + tag + ".dlq";

            DirectExchange bizExchange = new DirectExchange(bizExchangeName, true, false);
            DirectExchange dlxExchange = new DirectExchange(dlxExchangeName, true, false);
            Queue bizQueue = QueueBuilder.durable(bizQueueName)
                    .withArgument("x-dead-letter-exchange", dlxExchangeName)
                    .withArgument("x-dead-letter-routing-key", dlqRoutingKey)
                    .build();
            Queue dlqQueue = QueueBuilder.durable(dlqQueueName).build();
            Binding bizBinding = BindingBuilder.bind(bizQueue).to(bizExchange).with(tag);
            Binding dlqBinding = BindingBuilder.bind(dlqQueue).to(dlxExchange).with(dlqRoutingKey);

            declarables.add(bizExchange);
            declarables.add(dlxExchange);
            declarables.add(bizQueue);
            declarables.add(dlqQueue);
            declarables.add(bizBinding);
            declarables.add(dlqBinding);
        }
        return new Declarables(declarables);
    }

    @Bean
    @ConditionalOnMissingBean
    public RabbitMqStrategy rabbitMqStrategy(RabbitTemplate rabbitTemplate,
                                             MqProperties mqProperties,
                                             MqJsonCodec mqJsonCodec) {
        return new RabbitMqStrategy(rabbitTemplate, mqProperties, mqJsonCodec);
    }

    @Bean
    @ConditionalOnMissingBean
    public RabbitMqConsumerRegistrar rabbitMqConsumerRegistrar(ConnectionFactory connectionFactory,
                                                               AmqpAdmin amqpAdmin,
                                                               MqProperties mqProperties,
                                                               MqJsonCodec mqJsonCodec,
                                                               ObjectProvider<MqMessageLogStore> logStoreProvider) {
        return new RabbitMqConsumerRegistrar(connectionFactory, amqpAdmin, mqProperties, mqJsonCodec, logStoreProvider::getIfAvailable);
    }
}
