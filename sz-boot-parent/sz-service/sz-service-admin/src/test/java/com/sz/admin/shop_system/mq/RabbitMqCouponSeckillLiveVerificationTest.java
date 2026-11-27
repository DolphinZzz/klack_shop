package com.sz.admin.shop_system.mq;

import com.sz.mq.config.MqAutoConfiguration;
import com.sz.mq.config.RabbitMqAutoConfiguration;
import com.sz.mq.consumer.MqConsumerHandler;
import com.sz.mq.core.MqMessage;
import com.sz.mq.core.MqNonRetryableException;
import com.sz.mq.core.MqSendResult;
import com.sz.mq.core.MqTypeEnum;
import com.sz.mq.strategy.MqTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 本地 Docker RabbitMQ 端到端实测验证（需通过 RUN_LIVE_MQ_TEST=true 显式触发）
 */
@EnabledIfEnvironmentVariable(named = "RUN_LIVE_MQ_TEST", matches = "true")
class RabbitMqCouponSeckillLiveVerificationTest {

    private static final String LIVE_TOPIC = "coupon-seckill-live-test";
    private static final String LIVE_TAG = "grab";
    private static final String LIVE_GROUP = "coupon-seckill-live-test-group";
    private static final String MGMT_BASE = "http://127.0.0.1:15672/api";
    private static final String BASIC_AUTH = "Basic " + Base64.getEncoder()
            .encodeToString("admin:admin".getBytes(StandardCharsets.UTF_8));

    @Test
    void shouldVerifyTopologyConfirmManualAckNonRetryableAndTransientDlqOnLiveRabbitMq() throws Exception {
        CountDownLatch okLatch = new CountDownLatch(1);
        CountDownLatch bizErrFailedLatch = new CountDownLatch(1);
        CountDownLatch transientFailedLatch = new CountDownLatch(1);

        AtomicInteger transientAttempts = new AtomicInteger(0);
        AtomicInteger bizErrAttempts = new AtomicInteger(0);
        List<FailedEvent> failedEvents = new CopyOnWriteArrayList<>();
        List<CouponSeckillMessage> okMessages = new CopyOnWriteArrayList<>();

        MqConsumerHandler<CouponSeckillMessage> handler = new MqConsumerHandler<>() {
            @Override
            public String topic() {
                return LIVE_TOPIC;
            }

            @Override
            public String tag() {
                return LIVE_TAG;
            }

            @Override
            public String group() {
                return LIVE_GROUP;
            }

            @Override
            public Class<CouponSeckillMessage> payloadType() {
                return CouponSeckillMessage.class;
            }

            @Override
            public int concurrency() {
                return 1;
            }

            @Override
            public void onMessage(MqMessage<CouponSeckillMessage> message) {
                CouponSeckillMessage payload = message.getPayload();
                String orderId = payload.getOrderId();
                if (orderId.startsWith("LIVE_OK_")) {
                    okMessages.add(payload);
                    okLatch.countDown();
                } else if (orderId.startsWith("LIVE_BIZ_ERR_")) {
                    bizErrAttempts.incrementAndGet();
                    throw new MqNonRetryableException("coupon disabled");
                } else if (orderId.startsWith("LIVE_TRANSIENT_ERR_")) {
                    transientAttempts.incrementAndGet();
                    throw new RuntimeException("simulated mysql timeout");
                }
            }

            @Override
            public void onConsumeFailed(MqMessage<CouponSeckillMessage> message, Throwable cause, boolean retryable) {
                failedEvents.add(new FailedEvent(message.getPayload().getOrderId(), retryable));
                if (retryable) {
                    transientFailedLatch.countDown();
                } else {
                    bizErrFailedLatch.countDown();
                }
            }
        };

        try (ConfigurableApplicationContext ctx = new SpringApplicationBuilder(LiveRabbitBootApp.class)
                .web(WebApplicationType.NONE)
                .properties(
                        "spring.config.on-not-found=ignore",
                        "spring.config.import=",
                        "spring.jmx.enabled=false",
                        "sz.mq.enabled=true",
                        "sz.mq.type=rabbitmq",
                        "sz.mq.consumer.concurrency=1",
                        "sz.mq.consumer.prefetch=10",
                        "sz.mq.consumer.max-retry=2",
                        "sz.mq.consumer.retry-initial-interval-ms=50",
                        "spring.rabbitmq.host=127.0.0.1",
                        "spring.rabbitmq.port=5672",
                        "spring.rabbitmq.username=admin",
                        "spring.rabbitmq.password=admin",
                        "spring.rabbitmq.virtual-host=/",
                        "spring.rabbitmq.publisher-confirm-type=correlated",
                        "spring.rabbitmq.publisher-returns=true",
                        "spring.rabbitmq.template.mandatory=true"
                )
                .initializers(applicationContext -> applicationContext.getBeanFactory()
                        .registerSingleton("liveVerificationHandler", handler))
                .run()) {

            MqTemplate mqTemplate = ctx.getBean(MqTemplate.class);
            assertEquals(MqTypeEnum.RABBITMQ, mqTemplate.currentType());

            // 先清空可能存在的历史消息
            purgeQueue("sz.mq.queue." + LIVE_TOPIC + "." + LIVE_GROUP);
            purgeQueue("sz.mq.dlq." + LIVE_TOPIC + "." + LIVE_GROUP);

            // 1. 通过 Management API 验证拓扑自动声明
            String bizExchangeJson = getMgmt("/exchanges/%2F/sz.mq.direct." + LIVE_TOPIC);
            assertTrue(bizExchangeJson.contains("\"type\":\"direct\""), bizExchangeJson);
            assertTrue(bizExchangeJson.contains("\"durable\":true"), bizExchangeJson);

            String dlxExchangeJson = getMgmt("/exchanges/%2F/sz.mq.dlx." + LIVE_TOPIC);
            assertTrue(dlxExchangeJson.contains("\"type\":\"direct\""), dlxExchangeJson);

            String bizQueueJson = getMgmt("/queues/%2F/sz.mq.queue." + LIVE_TOPIC + "." + LIVE_GROUP);
            assertTrue(bizQueueJson.contains("\"x-dead-letter-exchange\":\"sz.mq.dlx." + LIVE_TOPIC + "\""), bizQueueJson);
            assertTrue(bizQueueJson.contains("\"x-dead-letter-routing-key\":\"" + LIVE_GROUP + "." + LIVE_TAG + ".dlq\""), bizQueueJson);

            String dlqQueueJson = getMgmt("/queues/%2F/sz.mq.dlq." + LIVE_TOPIC + "." + LIVE_GROUP);
            assertTrue(dlqQueueJson.contains("\"durable\":true"), dlqQueueJson);

            // 2. 正常抢券消息：Publish Confirm + 消费成功 + Manual ACK
            CouponSeckillMessage okPayload = new CouponSeckillMessage("LIVE_OK_1", 88L, 1001L, System.currentTimeMillis());
            MqSendResult okResult = mqTemplate.send(
                    LIVE_TOPIC, LIVE_TAG, okPayload.getOrderId(), okPayload);
            assertTrue(okResult.isSuccess());
            assertTrue(okLatch.await(5, TimeUnit.SECONDS), "正常消息应在5秒内完成消费");
            assertEquals(1, okMessages.size());
            assertEquals(88L, okMessages.get(0).getCouponId());

            // 3. 确定性业务失败（MqNonRetryableException）：0次重试 + retryable=false 回调 + 进 DLQ
            CouponSeckillMessage bizErrPayload = new CouponSeckillMessage("LIVE_BIZ_ERR_1", 88L, 1002L, System.currentTimeMillis());
            assertTrue(mqTemplate.send(
                    LIVE_TOPIC, LIVE_TAG, bizErrPayload.getOrderId(), bizErrPayload).isSuccess());
            assertTrue(bizErrFailedLatch.await(5, TimeUnit.SECONDS), "确定性业务异常应在5秒内进入失败回调");
            assertEquals(1, bizErrAttempts.get(), "MqNonRetryableException 不应重试");

            // 4. 瞬时异常（方式二）：重试 2 次（共执行 3 次）+ retryable=true 回调 + 进 DLQ
            CouponSeckillMessage transientPayload = new CouponSeckillMessage("LIVE_TRANSIENT_ERR_1", 88L, 1003L, System.currentTimeMillis());
            assertTrue(mqTemplate.send(
                    LIVE_TOPIC, LIVE_TAG, transientPayload.getOrderId(), transientPayload).isSuccess());
            assertTrue(transientFailedLatch.await(5, TimeUnit.SECONDS), "瞬时异常重试耗尽后应在5秒内进入失败回调");
            assertEquals(3, transientAttempts.get(), "maxRetry=2 应共消费 3 次");

            assertTrue(failedEvents.contains(new FailedEvent("LIVE_BIZ_ERR_1", false)));
            assertTrue(failedEvents.contains(new FailedEvent("LIVE_TRANSIENT_ERR_1", true)));

            // 5. 直接通过 Management API 读取 DLQ 验证死信队列确实收到 2 条死信（避免 stats 5秒刷新延迟）
            waitUntilQueueMessages("sz.mq.dlq." + LIVE_TOPIC + "." + LIVE_GROUP, 2);
        } finally {
            // 6. 清理临时测试队列/交换机，保持本地 RabbitMQ 干净
            deleteMgmt("/queues/%2F/sz.mq.queue." + LIVE_TOPIC + "." + LIVE_GROUP);
            deleteMgmt("/queues/%2F/sz.mq.dlq." + LIVE_TOPIC + "." + LIVE_GROUP);
            deleteMgmt("/exchanges/%2F/sz.mq.direct." + LIVE_TOPIC);
            deleteMgmt("/exchanges/%2F/sz.mq.dlx." + LIVE_TOPIC);
        }
    }

    private String getMgmt(String path) throws Exception {
        HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(MGMT_BASE + path))
                .header("Authorization", BASIC_AUTH)
                .GET()
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resp.statusCode(), "RabbitMQ Management API 调用失败: " + path + " -> " + resp.body());
        return resp.body();
    }

    private void purgeQueue(String queueName) throws Exception {
        HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(MGMT_BASE + "/queues/%2F/" + queueName + "/contents"))
                .header("Authorization", BASIC_AUTH)
                .DELETE()
                .build();
        client.send(req, HttpResponse.BodyHandlers.ofString());
    }

    private void deleteMgmt(String path) {
        try {
            HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(MGMT_BASE + path))
                    .header("Authorization", BASIC_AUTH)
                    .DELETE()
                    .build();
            client.send(req, HttpResponse.BodyHandlers.ofString());
        } catch (Exception ignored) {
        }
    }

    private void waitUntilQueueMessages(String queueName, int expected) throws Exception {
        HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
        long deadline = System.currentTimeMillis() + 5000L;
        String lastBody = "";
        while (System.currentTimeMillis() < deadline) {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(MGMT_BASE + "/queues/%2F/" + queueName + "/get"))
                    .header("Authorization", BASIC_AUTH)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"count\":10,\"ackmode\":\"ack_requeue_true\",\"encoding\":\"auto\"}"))
                    .build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            lastBody = resp.body();
            if (resp.statusCode() == 200
                    && lastBody.contains("LIVE_BIZ_ERR_1")
                    && lastBody.contains("LIVE_TRANSIENT_ERR_1")) {
                return;
            }
            Thread.sleep(150L);
        }
        fail("死信队列未在超时内收到预期 " + expected + " 条死信消息: " + lastBody);
    }

    private record FailedEvent(String orderId, boolean retryable) {
    }

    @SpringBootConfiguration
    @ImportAutoConfiguration({
            RabbitAutoConfiguration.class,
            MqAutoConfiguration.class,
            RabbitMqAutoConfiguration.class
    })
    static class LiveRabbitBootApp {
    }
}
