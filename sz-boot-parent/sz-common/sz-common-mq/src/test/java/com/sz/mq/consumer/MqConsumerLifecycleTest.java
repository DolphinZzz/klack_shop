package com.sz.mq.consumer;

import com.sz.mq.config.MqProperties;
import com.sz.mq.core.MqMessage;
import com.sz.mq.core.MqTypeEnum;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MqConsumerLifecycleTest {

    @Test
    void shouldRejectIllegalIdentifierWithDot() {
        MqConsumerHandler<String> badHandler = new TestHandler("coupon.seckill", "grab", "group-1");
        assertThrows(IllegalArgumentException.class, () -> MqConsumerLifecycle.validateHandlers(List.of(badHandler)));
    }

    @Test
    void shouldRejectDuplicateTopicAndGroup() {
        MqConsumerHandler<String> h1 = new TestHandler("coupon-seckill", "grab", "group-1");
        MqConsumerHandler<String> h2 = new TestHandler("coupon-seckill", "other", "group-1");
        assertThrows(IllegalStateException.class, () -> MqConsumerLifecycle.validateHandlers(List.of(h1, h2)));
    }

    @Test
    void shouldStartAndStopActiveRegistrar() {
        MqProperties properties = new MqProperties();
        properties.setEnabled(true);
        properties.setType(MqTypeEnum.RABBITMQ);

        MqConsumerHandler<String> handler = new TestHandler("coupon-seckill", "grab", "group-1");
        MqConsumerContainerRegistrar registrar = mock(MqConsumerContainerRegistrar.class);
        when(registrar.getType()).thenReturn(MqTypeEnum.RABBITMQ);

        MqConsumerLifecycle lifecycle = new MqConsumerLifecycle(
                properties,
                providerOf(List.of(handler)),
                providerOf(List.of(registrar))
        );

        lifecycle.start();
        assertTrue(lifecycle.isRunning());
        verify(registrar).start(List.of(handler));

        lifecycle.stop();
        assertFalse(lifecycle.isRunning());
        verify(registrar).stop();
    }

    @Test
    void shouldSkipStartWhenDisabled() {
        MqProperties properties = new MqProperties();
        properties.setEnabled(false);
        MqConsumerContainerRegistrar registrar = mock(MqConsumerContainerRegistrar.class);

        MqConsumerLifecycle lifecycle = new MqConsumerLifecycle(
                properties,
                providerOf(List.of(new TestHandler("coupon-seckill", "grab", "group-1"))),
                providerOf(List.of(registrar))
        );

        lifecycle.start();
        assertFalse(lifecycle.isRunning());
        verify(registrar, never()).start(anyList());
    }

    @SuppressWarnings("unchecked")
    private <T> ObjectProvider<T> providerOf(T value) {
        ObjectProvider<T> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable(any())).thenReturn(value);
        return provider;
    }

    private record TestHandler(String topic, String tag, String group) implements MqConsumerHandler<String> {
        @Override
        public Class<String> payloadType() {
            return String.class;
        }

        @Override
        public void onMessage(MqMessage<String> message) {
        }
    }
}
