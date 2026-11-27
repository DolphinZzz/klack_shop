package com.sz.admin.shop_system;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sz.AdminApplication;
import com.sz.admin.shop_system.pojo.dto.ShopUserDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 批量登录商城测试账号（user0001 ~ user1000）并生成 JMeter 压测用 Token 文件的测试脚本。
 * <p>
 * 使用方式：
 * 1. 确保本地 MySQL 与 Redis 已启动，且数据库中已存在 user0001 ~ user1000（密码 123456）。
 * 2. 在 IDE 中直接运行 {@link #batchGenerateShopUserTokens()}，或通过 Maven 指定该单测执行：
 *    {@code mvn -pl sz-service/sz-service-admin -am test -DskipTests=false -Dtest=ShopUserTokenBatchGenerateTest -Dsurefire.failIfNoSpecifiedTests=false}
 * 3. 脚本会调用真实登录接口 {@code POST /client/shop/auth/login}，将有效 Token 写入 Redis 并导出至：
 *    - {@code target/jmeter-shop-user-tokens.csv}（含 account,uid,token,authorization 四列）
 *    - {@code target/jmeter-tokens.txt}（纯单列 Bearer Token，每行一个）
 * 4. 在 JMeter 中通过「CSV Data Set Config」读取文件，并在「HTTP Header Manager」配置：
 *    {@code Authorization: ${authorization}}（或 {@code Authorization: Bearer ${token}}）。
 */
@SpringBootTest(
        classes = AdminApplication.class,
        properties = {
                "sz.mq.enabled=false",
                "spring.config.on-not-found=ignore",
                "spring.config.import="
                        + "optional:file:config/${spring.profiles.active:dev}/knife4j.yml,"
                        + "optional:file:config/${spring.profiles.active:dev}/oss.yml,"
                        + "optional:file:config/${spring.profiles.active:dev}/mybatis-flex.yml,"
                        + "optional:file:config/${spring.profiles.active:dev}/mysql.yml,"
                        + "optional:file:config/${spring.profiles.active:dev}/page-helper.yml,"
                        + "optional:file:config/${spring.profiles.active:dev}/redis.yml,"
                        + "optional:file:config/${spring.profiles.active:dev}/sa-token.yml,"
                        + "optional:file:../../config/${spring.profiles.active:dev}/knife4j.yml,"
                        + "optional:file:../../config/${spring.profiles.active:dev}/oss.yml,"
                        + "optional:file:../../config/${spring.profiles.active:dev}/mybatis-flex.yml,"
                        + "optional:file:../../config/${spring.profiles.active:dev}/mysql.yml,"
                        + "optional:file:../../config/${spring.profiles.active:dev}/page-helper.yml,"
                        + "optional:file:../../config/${spring.profiles.active:dev}/redis.yml,"
                        + "optional:file:../../config/${spring.profiles.active:dev}/sa-token.yml"
        }
)
@AutoConfigureMockMvc
class ShopUserTokenBatchGenerateTest {

    /** 起始编号（user0001 -> 1） */
    private static final int START_INDEX = 1;

    /** 生成 Token 数量（user0001 ~ user1000 共 1000 个） */
    private static final int COUNT = 1000;

    /** 账号前缀 */
    private static final String ACCOUNT_PREFIX = "user";

    /** 账号数字位数（%04d -> user0001 ~ user1000） */
    private static final String ACCOUNT_FORMAT = "%s%04d";

    /** 统一登录密码 */
    private static final String PASSWORD = "123456";

    /** Sa-Token Header 前缀（与 sa-token.yml 中 token-prefix: Bearer 保持一致） */
    private static final String TOKEN_PREFIX = "Bearer ";

    /** 登录接口路径（MockMvc 跳过 context-path，直接匹配 Controller 映射） */
    private static final String LOGIN_URI = "/client/shop/auth/login";

    /** 导出给 JMeter 使用的 CSV 文件（带表头：account,uid,token,authorization） */
    private static final Path CSV_OUTPUT_PATH = Path.of("target", "jmeter-shop-user-tokens.csv");

    /** 导出给 JMeter 使用的纯单列 Token 文件（每行一个 Bearer Token，无表头） */
    private static final Path TXT_OUTPUT_PATH = Path.of("target", "jmeter-tokens.txt");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void batchGenerateShopUserTokens() throws Exception {
        List<GeneratedToken> generatedTokens = new ArrayList<>(COUNT);
        List<String> failedAccounts = new ArrayList<>();

        long startMillis = System.currentTimeMillis();
        for (int i = 0; i < COUNT; i++) {
            int seq = START_INDEX + i;
            String account = String.format(ACCOUNT_FORMAT, ACCOUNT_PREFIX, seq);

            ShopUserDTO.Login loginDto = new ShopUserDTO.Login();
            loginDto.setAccountOrPhone(account);
            loginDto.setPassword(PASSWORD);

            try {
                MvcResult mvcResult = mockMvc.perform(post(LOGIN_URI)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginDto)))
                        .andExpect(status().isOk())
                        .andReturn();

                String responseBody = mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
                JsonNode root = objectMapper.readTree(responseBody);
                String code = root.path("code").asText("");
                JsonNode data = root.path("data");
                String accessToken = data.path("accessToken").asText("");
                long uid = data.path("uid").asLong(-1L);

                if (!"0000".equals(code) || accessToken.isBlank()) {
                    failedAccounts.add(account + "(code=" + code + ", msg=" + root.path("message").asText("") + ")");
                    continue;
                }

                generatedTokens.add(new GeneratedToken(account, uid, accessToken, TOKEN_PREFIX + accessToken));
            } catch (Exception e) {
                failedAccounts.add(account + "(" + e.getMessage() + ")");
            }
        }

        writeTokensCsv(generatedTokens);
        writeTokensTxt(generatedTokens);

        long costMs = System.currentTimeMillis() - startMillis;
        System.out.printf("批量生成 Token 完成：计划=%d，成功=%d，失败=%d，耗时=%dms%n",
                COUNT, generatedTokens.size(), failedAccounts.size(), costMs);
        System.out.printf("JMeter CSV 文件已导出至：%s%n", CSV_OUTPUT_PATH.toAbsolutePath());
        System.out.printf("JMeter TXT 文件已导出至：%s%n", TXT_OUTPUT_PATH.toAbsolutePath());
        generatedTokens.stream().limit(5).forEach(item ->
                System.out.printf("  account=%s | uid=%d | authorization=%s%n",
                        item.account(), item.uid(), item.authorization())
        );
        if (!failedAccounts.isEmpty()) {
            System.out.println("失败账号示例（最多展示前 10 个）：");
            failedAccounts.stream().limit(10).forEach(msg -> System.out.println("  " + msg));
        }

        assertFalse(generatedTokens.isEmpty(), "未成功生成任何 Token，请检查数据库账号和 Redis 状态");
    }

    private void writeTokensCsv(List<GeneratedToken> tokens) throws IOException {
        Path parent = CSV_OUTPUT_PATH.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (BufferedWriter writer = Files.newBufferedWriter(CSV_OUTPUT_PATH, StandardCharsets.UTF_8)) {
            writer.write("account,uid,token,authorization");
            writer.newLine();
            for (GeneratedToken item : tokens) {
                writer.write(String.join(",",
                        item.account(),
                        String.valueOf(item.uid()),
                        item.token(),
                        item.authorization()));
                writer.newLine();
            }
        }
    }

    private void writeTokensTxt(List<GeneratedToken> tokens) throws IOException {
        Path parent = TXT_OUTPUT_PATH.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (BufferedWriter writer = Files.newBufferedWriter(TXT_OUTPUT_PATH, StandardCharsets.UTF_8)) {
            for (GeneratedToken item : tokens) {
                writer.write(item.authorization());
                writer.newLine();
            }
        }
    }

    private record GeneratedToken(String account, long uid, String token, String authorization) {
    }
}
