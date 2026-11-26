package com.sz.admin.shop_system.agent;

import cn.dev33.satoken.annotation.SaIgnore;
import com.sz.admin.shop_system.agent.config.AgentInternalAuthInterceptor;
import com.sz.admin.shop_system.agent.config.AgentProperties;
import com.sz.admin.shop_system.agent.controller.AgentInternalController;
import com.sz.core.common.annotation.DebounceIgnore;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class AgentInternalAuthInterceptorTest {

    @Test
    void preHandle_rejectsMissingOrInvalidSecretWith401() throws Exception {
        AgentProperties properties = new AgentProperties();
        properties.setEnabled(true);
        properties.setInternalSecret("kada-test-secret-2026");
        AgentInternalAuthInterceptor interceptor = new AgentInternalAuthInterceptor(properties);

        MockHttpServletRequest missingReq = new MockHttpServletRequest("GET", "/internal/agent/products/recommend");
        MockHttpServletResponse missingRes = new MockHttpServletResponse();
        assertFalse(interceptor.preHandle(missingReq, missingRes, new Object()));
        assertEquals(401, missingRes.getStatus());
        assertTrue(missingRes.getContentAsString().contains("C105"));

        MockHttpServletRequest wrongReq = new MockHttpServletRequest("POST", "/internal/agent/users/101/orders/direct-create");
        wrongReq.addHeader(AgentInternalAuthInterceptor.HEADER_INTERNAL_SECRET, "wrong-secret");
        MockHttpServletResponse wrongRes = new MockHttpServletResponse();
        assertFalse(interceptor.preHandle(wrongReq, wrongRes, new Object()));
        assertEquals(401, wrongRes.getStatus());

        MockHttpServletRequest shortReq = new MockHttpServletRequest("GET", "/internal/agent/products/1");
        shortReq.addHeader(AgentInternalAuthInterceptor.HEADER_INTERNAL_SECRET, "kada-test-secret");
        MockHttpServletResponse shortRes = new MockHttpServletResponse();
        assertFalse(interceptor.preHandle(shortReq, shortRes, new Object()));
        assertEquals(401, shortRes.getStatus());

        properties.setEnabled(false);
        MockHttpServletRequest disabledReq = new MockHttpServletRequest("GET", "/internal/agent/products/1");
        disabledReq.addHeader(AgentInternalAuthInterceptor.HEADER_INTERNAL_SECRET, "kada-test-secret-2026");
        MockHttpServletResponse disabledRes = new MockHttpServletResponse();
        assertFalse(interceptor.preHandle(disabledReq, disabledRes, new Object()));
        assertEquals(401, disabledRes.getStatus());
    }

    @Test
    void preHandle_allowsValidConstantTimeSecret() throws Exception {
        AgentProperties properties = new AgentProperties();
        properties.setEnabled(true);
        properties.setInternalSecret("kada-test-secret-2026");
        AgentInternalAuthInterceptor interceptor = new AgentInternalAuthInterceptor(properties);

        MockHttpServletRequest validReq = new MockHttpServletRequest("GET", "/internal/agent/products/recommend");
        validReq.addHeader(AgentInternalAuthInterceptor.HEADER_INTERNAL_SECRET, "kada-test-secret-2026");
        MockHttpServletResponse validRes = new MockHttpServletResponse();

        assertTrue(interceptor.preHandle(validReq, validRes, new Object()));
        assertEquals(200, validRes.getStatus());
    }

    @Test
    void agentInternalController_marksAllEndpointsWithSaIgnoreAndDebounceIgnore() {
        int endpointCount = 0;
        for (Method method : AgentInternalController.class.getDeclaredMethods()) {
            boolean isEndpoint = method.isAnnotationPresent(GetMapping.class)
                    || method.isAnnotationPresent(PostMapping.class)
                    || method.isAnnotationPresent(RequestMapping.class);
            if (!isEndpoint) {
                continue;
            }
            endpointCount++;
            assertTrue(method.isAnnotationPresent(SaIgnore.class),
                    () -> method.getName() + " must carry @SaIgnore");
            assertTrue(method.isAnnotationPresent(DebounceIgnore.class),
                    () -> method.getName() + " must carry @DebounceIgnore");
        }
        assertEquals(8, endpointCount);
    }
}
