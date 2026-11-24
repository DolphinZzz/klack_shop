package com.sz.oss;

import com.sz.core.common.entity.UploadResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 模拟图片上传到 OSS 的集成测试（默认读取 config/dev/oss.yml，可通过 -Doss.profile=prod 切换环境）
 */
@SpringBootTest(classes = {OssClientUploadTest.TestConfig.class, OssProperties.class, S3Configuration.class, OssClient.class})
@ContextConfiguration(initializers = OssClientUploadTest.OssConfigInitializer.class)
class OssClientUploadTest {

    @Configuration
    @EnableConfigurationProperties
    static class TestConfig {
    }

    @Autowired
    private OssClient ossClient;

    @Autowired
    private OssProperties ossProperties;

    static class OssConfigInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            String profile = System.getProperty("oss.profile", "dev");
            Path ossYamlPath = resolveOssConfigPath(profile);
            try {
                YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
                List<PropertySource<?>> sources = loader.load("external-oss-config", new FileSystemResource(ossYamlPath));
                for (PropertySource<?> source : sources) {
                    applicationContext.getEnvironment().getPropertySources().addFirst(source);
                }
            } catch (IOException e) {
                throw new IllegalStateException("加载 OSS 配置文件失败: " + ossYamlPath, e);
            }
        }

        private Path resolveOssConfigPath(String profile) {
            Path current = Path.of("").toAbsolutePath();
            for (int i = 0; i < 6 && current != null; i++) {
                Path candidateYml = current.resolve("config").resolve(profile).resolve("oss.yml");
                if (Files.exists(candidateYml)) {
                    return candidateYml;
                }
                Path candidateYaml = current.resolve("config").resolve(profile).resolve("oss.yaml");
                if (Files.exists(candidateYaml)) {
                    return candidateYaml;
                }
                current = current.getParent();
            }
            throw new IllegalStateException("未找到 config/" + profile + "/oss.yml 或 oss.yaml，当前目录: " + Path.of("").toAbsolutePath());
        }
    }

    @Test
    void testUploadImage() throws IOException {
        System.setProperty("java.awt.headless", "true");

        byte[] imageBytes = createTestPngBytes();
        MockMultipartFile mockImage = new MockMultipartFile("file", "test-upload.png", "image/png", imageBytes);

        System.out.println("========== OSS 配置信息 ==========");
        System.out.println("provider   : " + ossProperties.getProvider());
        System.out.println("endpoint   : " + ossProperties.getEndpoint());
        System.out.println("bucketName : " + ossProperties.getBucketName());
        System.out.println("domain     : " + ossProperties.getDomain());
        System.out.println("naming     : " + ossProperties.getNaming());
        System.out.println("scheme     : " + ossProperties.getScheme());

        UploadResult result = ossClient.upload(mockImage, "test");

        System.out.println("========== 图片上传结果 ==========");
        System.out.println("url        : " + result.getUrl());
        System.out.println("objectName : " + result.getObjectName());
        System.out.println("filename   : " + result.getFilename());
        System.out.println("dirTag     : " + result.getDirTag());
        System.out.println("eTag       : " + result.getETag());
        System.out.println("size       : " + result.getSize());

        assertNotNull(result, "上传结果不能为空");
        assertNotNull(result.getUrl(), "访问 URL 不能为空");
        assertFalse(result.getUrl().isBlank(), "访问 URL 不能为空白");
        assertNotNull(result.getObjectName(), "objectName 不能为空");
        assertEquals((long) imageBytes.length, result.getSize(), "上传文件大小应一致");
        assertTrue(ossClient.isFileExists(result.getObjectName()), "上传后应能在 OSS 中检测到该文件");
    }

    private byte[] createTestPngBytes() throws IOException {
        BufferedImage image = new BufferedImage(200, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        try {
            g2d.setColor(new Color(45, 136, 255));
            g2d.fillRect(0, 0, 200, 100);
            g2d.setColor(Color.WHITE);
            g2d.drawString("OSS Upload Test", 45, 55);
        } finally {
            g2d.dispose();
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return baos.toByteArray();
    }
}
