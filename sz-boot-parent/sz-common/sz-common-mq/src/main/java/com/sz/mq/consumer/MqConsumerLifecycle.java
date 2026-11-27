package com.sz.mq.consumer;

import com.sz.mq.config.MqProperties;
import com.sz.mq.core.MqTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.SmartLifecycle;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 消费监听容器统一生命周期管理器
 */
@Slf4j
public class MqConsumerLifecycle implements SmartLifecycle {

    private static final Pattern VALID_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");

    private final MqProperties mqProperties;
    private final ObjectProvider<List<MqConsumerHandler<?>>> handlersProvider;
    private final ObjectProvider<List<MqConsumerContainerRegistrar>> registrarsProvider;

    private volatile boolean running = false;
    private volatile MqConsumerContainerRegistrar activeRegistrar;

    public MqConsumerLifecycle(MqProperties mqProperties,
                               ObjectProvider<List<MqConsumerHandler<?>>> handlersProvider,
                               ObjectProvider<List<MqConsumerContainerRegistrar>> registrarsProvider) {
        this.mqProperties = mqProperties;
        this.handlersProvider = handlersProvider;
        this.registrarsProvider = registrarsProvider;
    }

    public static void validateHandlers(List<MqConsumerHandler<?>> handlers) {
        if (handlers == null || handlers.isEmpty()) {
            return;
        }
        Set<String> seenTopicGroup = new HashSet<>();
        for (MqConsumerHandler<?> handler : handlers) {
            if (handler == null) {
                continue;
            }
            String topic = validateIdentifier("topic", handler.topic());
            String group = validateIdentifier("group", handler.group());
            validateIdentifier("tag", handler.tag());
            if (handler.payloadType() == null) {
                throw new IllegalArgumentException("MqConsumerHandler.payloadType() 不能为空: " + handler.getClass().getName());
            }
            String key = topic + "::" + group;
            if (!seenTopicGroup.add(key)) {
                throw new IllegalStateException("存在重复的 (topic, group) 消费者注册: (" + topic + ", " + group + ")");
            }
        }
    }

    private static String validateIdentifier(String fieldName, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("MQ " + fieldName + " 不能为空");
        }
        if (!VALID_NAME_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("MQ " + fieldName + " 非法（仅允许字母、数字、下划线、连字符）: " + value);
        }
        return value;
    }

    @Override
    public void start() {
        if (running) {
            return;
        }
        if (!mqProperties.isEnabled()) {
            log.info("[SZ-MQ] sz.mq.enabled=false，跳过消费容器启动");
            return;
        }
        List<MqConsumerHandler<?>> handlers = handlersProvider.getIfAvailable(List::of);
        if (handlers.isEmpty()) {
            log.info("[SZ-MQ] 未发现 MqConsumerHandler Bean，跳过消费容器启动");
            return;
        }
        validateHandlers(handlers);

        MqTypeEnum activeType = mqProperties.getType();
        List<MqConsumerContainerRegistrar> registrars = registrarsProvider.getIfAvailable(List::of);
        MqConsumerContainerRegistrar matched = registrars.stream()
                .filter(registrar -> registrar != null && registrar.getType() == activeType)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("未找到对应的 MQ 消费容器注册器: " + activeType));

        matched.start(handlers);
        this.activeRegistrar = matched;
        this.running = true;
        log.info("[SZ-MQ] MQ 消费容器已启动，type={}, handlerCount={}", activeType.getCode(), handlers.size());
    }

    @Override
    public void stop() {
        if (!running) {
            return;
        }
        MqConsumerContainerRegistrar registrar = this.activeRegistrar;
        if (registrar != null) {
            try {
                registrar.stop();
            } finally {
                this.activeRegistrar = null;
            }
        }
        this.running = false;
        log.info("[SZ-MQ] MQ 消费容器已停止");
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE - 100;
    }
}
