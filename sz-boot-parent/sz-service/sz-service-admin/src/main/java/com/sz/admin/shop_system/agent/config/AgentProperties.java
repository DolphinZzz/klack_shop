package com.sz.admin.shop_system.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 智能客服 Agent 内网通信与代理配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "sz.agent")
public class AgentProperties {

    /**
     * 是否启用 Agent 集成
     */
    private boolean enabled = true;

    /**
     * Python Agent 服务基础地址
     */
    private String baseUrl = "http://127.0.0.1:8000";

    /**
     * 内网共享鉴权密钥（X-Internal-Secret）
     */
    private String internalSecret = "kada-local-internal-secret";

    /**
     * 连接超时毫秒数
     */
    private int connectTimeoutMs = 5000;

    /**
     * 流式请求超时毫秒数
     */
    private long streamTimeoutMs = 300000L;
}
