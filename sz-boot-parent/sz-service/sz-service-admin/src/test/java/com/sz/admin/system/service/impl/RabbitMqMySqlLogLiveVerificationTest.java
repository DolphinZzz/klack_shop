package com.sz.admin.system.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.sz.AdminApplication;
import com.sz.admin.system.mapper.SysMqConsumeLogMapper;
import com.sz.admin.system.mapper.SysMqSendLogMapper;
import com.sz.admin.system.pojo.dto.SysMqConsumeLogListDTO;
import com.sz.admin.system.pojo.dto.SysMqSendLogListDTO;
import com.sz.admin.system.pojo.po.SysMqConsumeLog;
import com.sz.admin.system.pojo.po.SysMqSendLog;
import com.sz.admin.system.pojo.vo.SysMqConsumeLogVO;
import com.sz.admin.system.pojo.vo.SysMqSendLogVO;
import com.sz.admin.system.service.SysMqLogService;
import com.sz.core.common.entity.PageResult;
import com.sz.mq.consumer.MqConsumerHandler;
import com.sz.mq.core.MqMessage;
import com.sz.mq.core.MqNonRetryableException;
import com.sz.mq.core.MqSendResult;
import com.sz.mq.strategy.MqTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 完整 Spring Boot 上下文 + 本地 MySQL + 本地 Docker RabbitMQ 联调验证：
 * 验证 Liquibase 建表、先入库再发送、消费成功与 ACK 入库、死信入库及查询服务，测试结束后清理测试数据。
 */
@EnabledIfEnvironmentVariable(named = "RUN_LIVE_MQ_TEST", matches = "true")
@SpringBootTest(
        classes = {AdminApplication.class, RabbitMqMySqlLogLiveVerificationTest.TestHandlerConfig.class},
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.config.on-not-found=ignore",
                "spring.config.import="
                        + "optional:file:../../config/dev/knife4j.yml,"
                        + "optional:file:../../config/dev/oss.yml,"
                        + "optional:file:../../config/dev/mybatis-flex.yml,"
                        + "optional:file:../../config/dev/mysql.yml,"
                        + "optional:file:../../config/dev/page-helper.yml,"
                        + "optional:file:../../config/dev/redis.yml,"
                        + "optional:file:../../config/dev/sa-token.yml,"
                        + "optional:file:../../config/dev/mq.yml",
                "sz.mq.consumer.max-retry=1",
                "sz.mq.consumer.retry-initial-interval-ms=30"
        }
)
class RabbitMqMySqlLogLiveVerificationTest {

    private static final String TEST_TOPIC = "mq-log-e2e-test";
    private static final String TEST_TAG = "verify";
    private static final String TEST_GROUP = "mq-log-e2e-group";
    private static final String MGMT_BASE = "http://127.0.0.1:15672/api";
    private static final String BASIC_AUTH = "Basic " + Base64.getEncoder()
            .encodeToString("admin:admin".getBytes(StandardCharsets.UTF_8));

    private static final AtomicReference<CountDownLatch> OK_LATCH = new AtomicReference<>(new CountDownLatch(1));
    private static final AtomicReference<CountDownLatch> DEAD_LATCH = new AtomicReference<>(new CountDownLatch(1));

    @Autowired
    private MqTemplate mqTemplate;
    @Autowired
    private SysMqLogService sysMqLogService;
    @Autowired
    private SysMqSendLogMapper sendLogMapper;
    @Autowired
    private SysMqConsumeLogMapper consumeLogMapper;

    @Test
    void shouldPersistSendConsumeAndAckLogsEndToEndWithRabbitMqAndMysql() throws Exception {
        OK_LATCH.set(new CountDownLatch(1));
        DEAD_LATCH.set(new CountDownLatch(1));

        String okMsgId = "e2e-ok-" + UUID.randomUUID();
        String okBizKey = "ORD_E2E_OK_" + System.currentTimeMillis();
        String deadMsgId = "e2e-dead-" + UUID.randomUUID();
        String deadBizKey = "ORD_E2E_DEAD_" + System.currentTimeMillis();

        try {
            // 1. 发送正常消息：验证先入库 PENDING -> Broker Confirm -> SENT，消费完成 -> SUCCESS -> ACKED
            MqSendResult okSend = mqTemplate.send(MqMessage.<String>builder()
                    .messageId(okMsgId)
                    .topic(TEST_TOPIC)
                    .tag(TEST_TAG)
                    .bizKey(okBizKey)
                    .payload("ok-payload")
                    .timestamp(System.currentTimeMillis())
                    .build());
            assertTrue(okSend.isSuccess());
            assertTrue(OK_LATCH.get().await(5, TimeUnit.SECONDS), "正常测试消息应在5秒内完成消费");

            // 等待 ACK 状态落库完成（ACK 更新紧跟在 basicAck 后）
            SysMqConsumeLog okConsumeRow = waitForConsumeRow(okMsgId, "SUCCESS", "ACKED");
            assertNotNull(okConsumeRow);
            assertEquals(okBizKey, okConsumeRow.getBizKey());
            assertNotNull(okConsumeRow.getReceiveTime());
            assertNotNull(okConsumeRow.getConsumeTime());
            assertNotNull(okConsumeRow.getAckTime());

            PageResult<SysMqSendLogVO> sendPage = sysMqLogService.sendPage(new SysMqSendLogListDTO().setMessageId(okMsgId));
            assertEquals(1, sendPage.getTotal());
            SysMqSendLogVO sendVo = sendPage.getRows().get(0);
            assertEquals("SENT", sendVo.getSendStatus());
            assertNotNull(sendVo.getSentTime());

            PageResult<SysMqConsumeLogVO> consumePage = sysMqLogService.consumePage(new SysMqConsumeLogListDTO().setMessageId(okMsgId));
            assertEquals(1, consumePage.getTotal());
            assertEquals("ACKED", consumePage.getRows().get(0).getAckStatus());

            // 2. 发送不可重试业务异常消息：验证落库为 DEAD + NACKED_DLQ
            MqSendResult deadSend = mqTemplate.send(MqMessage.<String>builder()
                    .messageId(deadMsgId)
                    .topic(TEST_TOPIC)
                    .tag(TEST_TAG)
                    .bizKey(deadBizKey)
                    .payload("fail-non-retryable")
                    .timestamp(System.currentTimeMillis())
                    .build());
            assertTrue(deadSend.isSuccess());
            assertTrue(DEAD_LATCH.get().await(5, TimeUnit.SECONDS), "业务失败消息应在5秒内进入死信回调");

            SysMqConsumeLog deadConsumeRow = waitForConsumeRow(deadMsgId, "DEAD", "NACKED_DLQ");
            assertNotNull(deadConsumeRow);
            assertTrue(deadConsumeRow.getErrorMsg().contains("simulated non-retryable"));
        } finally {
            // 清理本次测试数据库记录和临时测试 RabbitMQ 队列/交换机
            sendLogMapper.deleteByQuery(QueryWrapper.create().in(SysMqSendLog::getMessageId, okMsgId, deadMsgId));
            consumeLogMapper.deleteByQuery(QueryWrapper.create().in(SysMqConsumeLog::getMessageId, okMsgId, deadMsgId));
            deleteMgmt("/queues/%2F/sz.mq.queue." + TEST_TOPIC + "." + TEST_GROUP);
            deleteMgmt("/queues/%2F/sz.mq.dlq." + TEST_TOPIC + "." + TEST_GROUP);
            deleteMgmt("/exchanges/%2F/sz.mq.direct." + TEST_TOPIC);
            deleteMgmt("/exchanges/%2F/sz.mq.dlx." + TEST_TOPIC);
        }
    }

    private SysMqConsumeLog waitForConsumeRow(String messageId, String expectedConsumeStatus, String expectedAckStatus)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5000L;
        SysMqConsumeLog latest = null;
        while (System.currentTimeMillis() < deadline) {
            latest = consumeLogMapper.selectOneByQuery(QueryWrapper.create()
                    .eq(SysMqConsumeLog::getMessageId, messageId)
                    .eq(SysMqConsumeLog::getConsumerGroup, TEST_GROUP));
            if (latest != null
                    && expectedConsumeStatus.equals(latest.getConsumeStatus())
                    && expectedAckStatus.equals(latest.getAckStatus())) {
                return latest;
            }
            Thread.sleep(100L);
        }
        fail("消费记录未达到预期状态 (" + expectedConsumeStatus + ", " + expectedAckStatus + "): " + latest);
        return null;
    }

    private void deleteMgmt(String path) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(MGMT_BASE + path))
                    .header("Authorization", BASIC_AUTH)
                    .DELETE()
                    .build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            assertTrue(resp.statusCode() < 500);
        } catch (Exception ignored) {
        }
    }

    @TestConfiguration
    static class TestHandlerConfig {

        @Bean
        public MqConsumerHandler<String> mqLogE2eTestHandler() {
            return new MqConsumerHandler<>() {
                @Override
                public String topic() {
                    return TEST_TOPIC;
                }

                @Override
                public String tag() {
                    return TEST_TAG;
                }

                @Override
                public String group() {
                    return TEST_GROUP;
                }

                @Override
                public Class<String> payloadType() {
                    return String.class;
                }

                @Override
                public void onMessage(MqMessage<String> message) {
                    if ("fail-non-retryable".equals(message.getPayload())) {
                        throw new MqNonRetryableException("simulated non-retryable");
                    }
                    OK_LATCH.get().countDown();
                }

                @Override
                public void onConsumeFailed(MqMessage<String> message, Throwable cause, boolean retryable) {
                    DEAD_LATCH.get().countDown();
                }
            };
        }
    }
}
