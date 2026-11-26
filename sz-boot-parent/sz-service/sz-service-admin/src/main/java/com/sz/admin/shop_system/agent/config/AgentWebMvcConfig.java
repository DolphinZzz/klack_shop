package com.sz.admin.shop_system.agent.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 注册 /internal/agent/** 内网拦截器
 */
@Configuration
@RequiredArgsConstructor
public class AgentWebMvcConfig implements WebMvcConfigurer {

    private final AgentInternalAuthInterceptor agentInternalAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(agentInternalAuthInterceptor)
                .addPathPatterns("/internal/agent/**");
    }
}
