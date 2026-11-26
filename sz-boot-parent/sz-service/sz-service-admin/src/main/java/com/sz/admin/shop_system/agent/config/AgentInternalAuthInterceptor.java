package com.sz.admin.shop_system.agent.config;

import cn.hutool.json.JSONUtil;
import com.sz.core.common.entity.ApiResult;
import com.sz.core.common.enums.CommonResponseEnum;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * /internal/agent/** 内网接口鉴权拦截器（常量时间比对 X-Internal-Secret）
 */
@Component
@RequiredArgsConstructor
public class AgentInternalAuthInterceptor implements HandlerInterceptor {

    public static final String HEADER_INTERNAL_SECRET = "X-Internal-Secret";

    private final AgentProperties agentProperties;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) throws IOException {
        String expectedSecret = agentProperties.getInternalSecret();
        String actualSecret = request.getHeader(HEADER_INTERNAL_SECRET);
        if (!agentProperties.isEnabled()
                || expectedSecret == null || expectedSecret.isBlank()
                || actualSecret == null || actualSecret.isBlank()
                || !MessageDigest.isEqual(
                        expectedSecret.getBytes(StandardCharsets.UTF_8),
                        actualSecret.getBytes(StandardCharsets.UTF_8))) {
            writeUnauthorized(response);
            return false;
        }
        return true;
    }

    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        ApiResult<Void> body = ApiResult.error(CommonResponseEnum.INVALID_TOKEN);
        response.getWriter().write(JSONUtil.toJsonStr(body));
    }
}
