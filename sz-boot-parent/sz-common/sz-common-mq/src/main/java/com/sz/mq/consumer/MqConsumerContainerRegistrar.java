package com.sz.mq.consumer;

import com.sz.mq.core.MqTypeEnum;

import java.util.List;

/**
 * 引擎消费容器注册器策略接口
 */
public interface MqConsumerContainerRegistrar {

    MqTypeEnum getType();

    void start(List<MqConsumerHandler<?>> handlers);

    void stop();
}
