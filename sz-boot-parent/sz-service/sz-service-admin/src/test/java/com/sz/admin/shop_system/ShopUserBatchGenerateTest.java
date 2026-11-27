package com.sz.admin.shop_system;

import cn.dev33.satoken.secure.SaSecureUtil;
import com.sz.AdminApplication;
import com.sz.admin.shop_system.mapper.ShopUserMapper;
import com.sz.admin.shop_system.pojo.po.ShopUser;
import com.sz.admin.shop_system.service.ShopUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * 批量生成商城用户（shop_user）账号与密码的测试脚本。
 * <p>
 * 使用方式：
 * 1. 按需调整下方配置常量（生成数量、起始序号、前缀、随机密码/固定密码、初始余额等）。
 * 2. 在 IDE 中直接运行 {@link #batchGenerateShopUsers()}，或通过 Maven 指定单测执行。
 * 3. 脚本会自动跳过已存在的账号/手机号，并将生成的明文账号密码导出到 CSV 文件。
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
class ShopUserBatchGenerateTest {

    /** 生成账号数量 */
    private static final int COUNT = 1000;

    /** 起始编号（分批生成时可调大，避免与旧批次重复） */
    private static final int START_INDEX = 1;

    /** 账号前缀，例如 user0001、user0002 */
    private static final String ACCOUNT_PREFIX = "user";

    /** 昵称前缀 */
    private static final String NICKNAME_PREFIX = "测试用户";

    /** 起始手机号（确保为 11 位合法手机号） */
    private static final long PHONE_START = 13900000000L;

    /** true：每个账号生成独立随机密码；false：全部使用 FIXED_PASSWORD */
    private static final boolean USE_RANDOM_PASSWORD = false;

    /** 随机密码长度（至少 6 位） */
    private static final int PASSWORD_LENGTH = 10;

    /** 固定密码（仅 USE_RANDOM_PASSWORD = false 时生效） */
    private static final String FIXED_PASSWORD = "123456";

    /** 初始余额 */
    private static final BigDecimal INITIAL_MONEY = BigDecimal.ZERO;

    /** 初始积分 */
    private static final int INITIAL_INTEGRAL = 0;

    /** 用户类型 */
    private static final String USER_TYPE = "h5";

    /** 每批入库条数 */
    private static final int BATCH_SIZE = 500;

    /** 导出明文账号密码的 CSV 文件路径 */
    private static final Path CSV_OUTPUT_PATH = Path.of("target", "generated-shop-users.csv");

    private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijkmnpqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private static final String ALL_CHARS = UPPER + LOWER + DIGITS;
    private static final SecureRandom RANDOM = new SecureRandom();

    @Autowired
    private ShopUserService shopUserService;

    @Autowired
    private ShopUserMapper shopUserMapper;

    @Test
    void batchGenerateShopUsers() throws IOException {
        List<ShopUser> batchUsers = new ArrayList<>(Math.min(COUNT, BATCH_SIZE));
        List<GeneratedCredential> credentials = new ArrayList<>(COUNT);
        int insertedCount = 0;
        int skippedCount = 0;

        for (int i = 0; i < COUNT; i++) {
            int seq = START_INDEX + i;
            String account = String.format("%s%04d", ACCOUNT_PREFIX, seq);
            String phone = String.valueOf(PHONE_START + seq);

            if (shopUserMapper.countByAccount(account, null) > 0
                    || shopUserMapper.countByPhone(phone, null) > 0) {
                skippedCount++;
                continue;
            }

            String rawPassword = USE_RANDOM_PASSWORD ? generateRandomPassword(PASSWORD_LENGTH) : FIXED_PASSWORD;
            // 与 ShopUserServiceImpl#userRegister / buildLoginUser 保持一致，使用 MD5 加密入库
            String md5Password = SaSecureUtil.md5(rawPassword);

            ShopUser user = new ShopUser();
            user.setAccount(account);
            user.setPassword(md5Password);
            user.setPhone(phone);
            user.setNickname(NICKNAME_PREFIX + seq);
            user.setRealName(NICKNAME_PREFIX + seq);
            user.setSex(0);
            user.setStatus(0);
            user.setLevel(1);
            user.setIntegral(INITIAL_INTEGRAL);
            user.setExperience(0);
            user.setSignNum(0);
            user.setNowMoney(INITIAL_MONEY);
            user.setUserType(USER_TYPE);

            batchUsers.add(user);
            credentials.add(new GeneratedCredential(account, phone, rawPassword, md5Password));

            if (batchUsers.size() >= BATCH_SIZE) {
                shopUserService.saveBatch(batchUsers);
                insertedCount += batchUsers.size();
                batchUsers.clear();
            }
        }

        if (!batchUsers.isEmpty()) {
            shopUserService.saveBatch(batchUsers);
            insertedCount += batchUsers.size();
            batchUsers.clear();
        }

        Path csvFile = writeCredentialsCsv(credentials);

        System.out.printf("批量生成完成：计划=%d，成功入库=%d，跳过已存在=%d%n", COUNT, insertedCount, skippedCount);
        System.out.printf("明文账号密码已导出至：%s%n", csvFile.toAbsolutePath());
        credentials.stream().limit(10).forEach(item ->
                System.out.printf("  account=%s | phone=%s | password=%s%n",
                        item.account(), item.phone(), item.rawPassword())
        );
    }

    private String generateRandomPassword(int length) {
        int actualLength = Math.max(length, 6);
        char[] chars = new char[actualLength];
        // 至少包含一个大写字母、一个小写字母和一个数字
        chars[0] = UPPER.charAt(RANDOM.nextInt(UPPER.length()));
        chars[1] = LOWER.charAt(RANDOM.nextInt(LOWER.length()));
        chars[2] = DIGITS.charAt(RANDOM.nextInt(DIGITS.length()));
        for (int i = 3; i < actualLength; i++) {
            chars[i] = ALL_CHARS.charAt(RANDOM.nextInt(ALL_CHARS.length()));
        }
        // Fisher-Yates 洗牌打乱顺序
        for (int i = actualLength - 1; i > 0; i--) {
            int j = RANDOM.nextInt(i + 1);
            char tmp = chars[i];
            chars[i] = chars[j];
            chars[j] = tmp;
        }
        return new String(chars);
    }

    private Path writeCredentialsCsv(List<GeneratedCredential> credentials) throws IOException {
        Path parent = CSV_OUTPUT_PATH.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (BufferedWriter writer = Files.newBufferedWriter(CSV_OUTPUT_PATH, StandardCharsets.UTF_8)) {
            writer.write("account,phone,password,md5Password");
            writer.newLine();
            for (GeneratedCredential item : credentials) {
                writer.write(String.join(",", item.account(), item.phone(), item.rawPassword(), item.md5Password()));
                writer.newLine();
            }
        }
        return CSV_OUTPUT_PATH;
    }

    private record GeneratedCredential(String account, String phone, String rawPassword, String md5Password) {
    }
}
