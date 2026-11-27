package com.sz.admin.shop_system.agent;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.sz.admin.shop_system.agent.config.AgentProperties;
import com.sz.admin.shop_system.agent.controller.AdminCsController;
import com.sz.admin.shop_system.agent.controller.ClientCsController;
import com.sz.admin.shop_system.agent.service.AgentProxyService;
import com.sz.admin.shop_system.agent.service.AgentProxyService.ProxyIdentity;
import com.sz.core.common.annotation.DebounceIgnore;
import com.sz.core.common.entity.BaseUserInfo;
import com.sz.core.common.entity.LoginUser;
import com.sz.core.common.exception.GlobalExceptionHandler;
import com.sz.core.common.exception.common.BusinessException;
import com.sz.security.core.util.LoginUtils;
import com.sz.security.util.StpUserUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AgentProxyServiceTest {

    private final List<HttpServer> startedServers = new ArrayList<>();

    @AfterEach
    void tearDown() {
        for (HttpServer server : startedServers) {
            server.stop(0);
        }
        startedServers.clear();
    }

    private HttpServer startStubServer() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();
        startedServers.add(server);
        return server;
    }

    private AgentProperties createProperties(HttpServer server) {
        AgentProperties props = new AgentProperties();
        props.setEnabled(true);
        props.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        props.setInternalSecret("kada-trusted-secret-2026");
        props.setConnectTimeoutMs(3000);
        props.setStreamTimeoutMs(300_000L);
        return props;
    }

    @Test
    void forwardJson_stripsClientSpoofedHeadersAndInjectsTrustedIdentity() throws Exception {
        HttpServer server = startStubServer();
        AtomicReference<String> capturedMethod = new AtomicReference<>();
        AtomicReference<String> capturedUri = new AtomicReference<>();
        AtomicReference<String> capturedBody = new AtomicReference<>();
        Map<String, List<String>> capturedHeaders = new ConcurrentHashMap<>();

        server.createContext("/internal/cs/client/sessions", exchange -> {
            capturedMethod.set(exchange.getRequestMethod());
            capturedUri.set(exchange.getRequestURI().toString());
            capturedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            exchange.getRequestHeaders().forEach((k, v) -> capturedHeaders.put(k.toLowerCase(), List.copyOf(v)));
            byte[] resp = "{\"session_id\":\"s-100\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json;charset=UTF-8");
            exchange.sendResponseHeaders(200, resp.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resp);
            }
        });

        AgentProxyService proxyService = new AgentProxyService(createProperties(server));

        HttpHeaders spoofed = new HttpHeaders();
        spoofed.set("X-User-Id", "99999");
        spoofed.set("X-Internal-Secret", "fake");
        spoofed.set("X-Admin-Id", "1");
        spoofed.set("X-Admin-Name", "evil");
        spoofed.set("X-Request-Id", "spoofed-req-id");
        spoofed.set("Authorization", "Bearer public-jwt");
        spoofed.set("Cookie", "sid=abc");
        spoofed.set("Accept-Encoding", "gzip, deflate, br");
        spoofed.set("Accept", "application/json");
        spoofed.setContentType(MediaType.APPLICATION_JSON);

        byte[] payload = "{\"title\":\"新会话\"}".getBytes(StandardCharsets.UTF_8);
        ResponseEntity<byte[]> response = proxyService.forwardJson(
                HttpMethod.POST,
                "/internal/cs/client/sessions",
                "status=ACTIVE",
                spoofed,
                payload,
                ProxyIdentity.client(10086L));

        assertEquals(200, response.getStatusCode().value());
        assertEquals("{\"session_id\":\"s-100\"}", new String(response.getBody(), StandardCharsets.UTF_8));
        assertEquals("POST", capturedMethod.get());
        assertEquals("/internal/cs/client/sessions?status=ACTIVE", capturedUri.get());
        assertEquals("{\"title\":\"新会话\"}", capturedBody.get());

        assertEquals(List.of("10086"), capturedHeaders.get("x-user-id"));
        assertEquals(List.of("kada-trusted-secret-2026"), capturedHeaders.get("x-internal-secret"));
        assertNull(capturedHeaders.get("x-admin-id"));
        assertNull(capturedHeaders.get("x-admin-name"));
        assertNull(capturedHeaders.get("authorization"));
        assertNull(capturedHeaders.get("cookie"));
        assertNull(capturedHeaders.get("accept-encoding"));
        assertNotNull(capturedHeaders.get("x-request-id"));
        assertNotEquals("spoofed-req-id", capturedHeaders.get("x-request-id").get(0));
        assertEquals(List.of("application/json"), capturedHeaders.get("accept"));
    }

    @Test
    void forwardJson_encodesAdminNameWithRfc3986PercentEncodingAndStripsUserId() throws Exception {
        HttpServer server = startStubServer();
        Map<String, List<String>> capturedHeaders = new ConcurrentHashMap<>();

        server.createContext("/internal/cs/admin/tickets", exchange -> {
            exchange.getRequestHeaders().forEach((k, v) -> capturedHeaders.put(k.toLowerCase(), List.copyOf(v)));
            byte[] resp = "{\"items\":[]}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, resp.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resp);
            }
        });

        AgentProxyService proxyService = new AgentProxyService(createProperties(server));

        HttpHeaders spoofed = new HttpHeaders();
        spoofed.set("X-User-Id", "777");
        spoofed.set("X-Admin-Id", "1");
        spoofed.set("X-Admin-Name", "spoofed");
        spoofed.set("X-Internal-Secret", "fake");

        ResponseEntity<byte[]> response = proxyService.forwardJson(
                HttpMethod.GET,
                "/internal/cs/admin/tickets",
                "status=OPEN&page=1",
                spoofed,
                null,
                ProxyIdentity.admin(42L, "张 管理员+测试"));

        assertEquals(200, response.getStatusCode().value());
        assertNull(capturedHeaders.get("x-user-id"));
        assertEquals(List.of("42"), capturedHeaders.get("x-admin-id"));
        assertEquals(List.of("kada-trusted-secret-2026"), capturedHeaders.get("x-internal-secret"));

        String encodedName = capturedHeaders.get("x-admin-name").get(0);
        assertFalse(encodedName.contains("+"), "X-Admin-Name must not use form '+' encoding: " + encodedName);
        assertTrue(encodedName.contains("%20"), "Spaces in X-Admin-Name must be RFC 3986 '%20': " + encodedName);
        assertTrue(encodedName.contains("%2B"), "'+' in X-Admin-Name must be percent-encoded as '%2B': " + encodedName);
        assertEquals("张 管理员+测试", java.net.URLDecoder.decode(encodedName, StandardCharsets.UTF_8));
    }

    @Test
    void forwardMultipart_relaysPdfAndFormFieldsWithTrustedAdminIdentity() throws Exception {
        HttpServer server = startStubServer();
        Map<String, List<String>> capturedHeaders = new ConcurrentHashMap<>();
        AtomicReference<String> capturedBody = new AtomicReference<>();

        server.createContext("/internal/cs/admin/kb/documents/upload", exchange -> {
            exchange.getRequestHeaders().forEach((k, v) -> capturedHeaders.put(k.toLowerCase(), List.copyOf(v)));
            capturedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] resp = "{\"id\":\"doc-1\",\"status\":\"PENDING\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, resp.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resp);
            }
        });

        AgentProxyService proxyService = new AgentProxyService(createProperties(server));
        MockMultipartFile pdfFile = new MockMultipartFile(
                "file", "policy.pdf", "application/pdf", "%PDF-1.4 sample".getBytes(StandardCharsets.UTF_8));

        HttpHeaders spoofed = new HttpHeaders();
        spoofed.set("X-Internal-Secret", "fake");
        spoofed.set("X-Admin-Id", "1");
        spoofed.set("Content-Type", "multipart/form-data; boundary=client-boundary");

        ResponseEntity<byte[]> response = proxyService.forwardMultipart(
                "/internal/cs/admin/kb/documents/upload",
                null,
                pdfFile,
                Map.of("category", "mall_policy", "title", "售后规范"),
                spoofed,
                ProxyIdentity.admin(7L, "Admin User"));

        assertEquals(200, response.getStatusCode().value());
        assertEquals(List.of("7"), capturedHeaders.get("x-admin-id"));
        assertEquals(List.of("Admin%20User"), capturedHeaders.get("x-admin-name"));
        assertEquals(List.of("kada-trusted-secret-2026"), capturedHeaders.get("x-internal-secret"));

        String contentType = capturedHeaders.get("content-type").get(0);
        assertTrue(contentType.startsWith("multipart/form-data; boundary="));
        assertFalse(contentType.contains("client-boundary"));

        String body = capturedBody.get();
        assertTrue(body.contains("name=\"category\""));
        assertTrue(body.contains("mall_policy"));
        assertTrue(body.contains("name=\"title\""));
        assertTrue(body.contains("售后规范"));
        assertTrue(body.contains("name=\"file\"; filename=\"policy.pdf\""));
        assertTrue(body.contains("%PDF-1.4 sample"));
    }

    @Test
    void forwardMultipart_streamsInputStreamWithoutBufferingGetBytes() throws Exception {
        HttpServer server = startStubServer();
        AtomicReference<byte[]> capturedBody = new AtomicReference<>();

        server.createContext("/internal/cs/admin/kb/documents/upload", exchange -> {
            capturedBody.set(exchange.getRequestBody().readAllBytes());
            byte[] resp = "{\"id\":\"doc-stream\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, resp.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resp);
            }
        });

        byte[] payload = ("%PDF-1.4\n" + "A".repeat(256 * 1024)).getBytes(StandardCharsets.UTF_8);
        MockMultipartFile streamingOnlyFile = new MockMultipartFile(
                "file", "large.pdf", "application/pdf", payload) {
            @Override
            public byte[] getBytes() {
                throw new AssertionError("forwardMultipart must stream via getInputStream() instead of getBytes()");
            }
        };

        AgentProxyService proxyService = new AgentProxyService(createProperties(server));
        ResponseEntity<byte[]> response = proxyService.forwardMultipart(
                "/internal/cs/admin/kb/documents/upload",
                null,
                streamingOnlyFile,
                Map.of("category", "mall_policy"),
                new HttpHeaders(),
                ProxyIdentity.admin(7L, "Admin"));

        assertEquals(200, response.getStatusCode().value());
        String body = new String(capturedBody.get(), StandardCharsets.UTF_8);
        assertTrue(body.contains("filename=\"large.pdf\""));
        assertTrue(body.contains("%PDF-1.4\n" + "A".repeat(1024)));
    }

    @Test
    void forwardSse_relaysEventsAndCancelsUpstreamOnEmitterCompletion() throws Exception {
        HttpServer server = startStubServer();
        CountDownLatch eventsSent = new CountDownLatch(1);
        CountDownLatch upstreamClosed = new CountDownLatch(1);

        server.createContext("/internal/cs/client/sessions/s1/chat/stream", (HttpExchange exchange) -> {
            exchange.getRequestBody().readAllBytes();
            exchange.getResponseHeaders().set("Content-Type", "text/event-stream;charset=UTF-8");
            exchange.sendResponseHeaders(200, 0);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(("event: meta\ndata: {\"session_id\":\"s1\",\"intent\":\"recommend_order\"}\n\n"
                        + "event: token\ndata: {\"delta\":\"你好\"}\n\n").getBytes(StandardCharsets.UTF_8));
                os.flush();
                eventsSent.countDown();
                for (int i = 0; i < 200; i++) {
                    Thread.sleep(25);
                    os.write(": heartbeat\n\n".getBytes(StandardCharsets.UTF_8));
                    os.flush();
                }
            } catch (IOException | InterruptedException ignored) {
                upstreamClosed.countDown();
            }
        });

        AgentProxyService proxyService = new AgentProxyService(createProperties(server));
        HttpHeaders spoofed = new HttpHeaders();
        spoofed.set("X-User-Id", "99999");
        spoofed.set("X-Internal-Secret", "fake");

        SseEmitter emitter = proxyService.forwardSse(
                HttpMethod.POST,
                "/internal/cs/client/sessions/s1/chat/stream",
                null,
                spoofed,
                "{\"message\":\"你好\"}".getBytes(StandardCharsets.UTF_8),
                ProxyIdentity.client(10086L));

        assertEquals(300_000L, emitter.getTimeout());
        assertTrue(eventsSent.await(3, TimeUnit.SECONDS), "Stub server should emit initial SSE events");

        // Wait briefly for the virtual thread to parse both events into the emitter buffer
        long deadline = System.currentTimeMillis() + 2000;
        String combined = "";
        while (System.currentTimeMillis() < deadline) {
            combined = readEmitterBufferText(emitter);
            if (combined.contains("meta") && combined.contains("token") && combined.contains("你好")) {
                break;
            }
            Thread.sleep(10);
        }
        assertTrue(combined.contains("event:meta"), "Expected meta event in emitter buffer: " + combined);
        assertTrue(combined.contains("\"session_id\":\"s1\""), "Expected meta payload in emitter buffer: " + combined);
        assertTrue(combined.contains("event:token"), "Expected token event in emitter buffer: " + combined);
        assertTrue(combined.contains("你好"), "Expected token delta in emitter buffer: " + combined);

        // Trigger onCompletion callback and verify upstream stream closes immediately
        Runnable completionCallback = (Runnable) ReflectionTestUtils.getField(emitter, "completionCallback");
        assertNotNull(completionCallback);
        completionCallback.run();

        assertTrue(upstreamClosed.await(2, TimeUnit.SECONDS),
                "Upstream HTTP stream must close immediately when emitter completes");
    }

    private static String readEmitterBufferText(SseEmitter emitter) {
        StringBuilder sb = new StringBuilder();
        synchronized (emitter) {
            Set<?> earlyAttempts = (Set<?>) ReflectionTestUtils.getField(emitter, "earlySendAttempts");
            if (earlyAttempts != null) {
                for (Object item : earlyAttempts) {
                    Object data = ReflectionTestUtils.getField(item, "data");
                    if (data != null) {
                        sb.append(data);
                    }
                }
            }
        }
        return sb.toString();
    }

    @Test
    void forwardSse_cancelsUpstreamOnEmitterTimeout() throws Exception {
        HttpServer server = startStubServer();
        CountDownLatch eventsSent = new CountDownLatch(1);
        CountDownLatch upstreamClosed = new CountDownLatch(1);

        server.createContext("/internal/cs/client/sessions/s2/resume/stream", exchange -> {
            exchange.getRequestBody().readAllBytes();
            exchange.getResponseHeaders().set("Content-Type", "text/event-stream;charset=UTF-8");
            exchange.sendResponseHeaders(200, 0);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write("event: meta\ndata: {\"session_id\":\"s2\"}\n\n".getBytes(StandardCharsets.UTF_8));
                os.flush();
                eventsSent.countDown();
                for (int i = 0; i < 200; i++) {
                    Thread.sleep(25);
                    os.write("event: token\ndata: {\"delta\":\".\"}\n\n".getBytes(StandardCharsets.UTF_8));
                    os.flush();
                }
            } catch (IOException | InterruptedException ignored) {
                upstreamClosed.countDown();
            }
        });

        AgentProxyService proxyService = new AgentProxyService(createProperties(server));
        SseEmitter emitter = proxyService.forwardSse(
                HttpMethod.POST,
                "/internal/cs/client/sessions/s2/resume/stream",
                null,
                new HttpHeaders(),
                "{\"action\":\"confirm\"}".getBytes(StandardCharsets.UTF_8),
                ProxyIdentity.client(10086L));

        assertTrue(eventsSent.await(3, TimeUnit.SECONDS));
        Runnable timeoutCallback = (Runnable) ReflectionTestUtils.getField(emitter, "timeoutCallback");
        assertNotNull(timeoutCallback);
        timeoutCallback.run();

        assertTrue(upstreamClosed.await(2, TimeUnit.SECONDS),
                "Upstream HTTP stream must close immediately on emitter timeout");
    }

    @Test
    void clientCsController_setsSseAntiBufferingHeadersAndRelaysEvents() throws Exception {
        HttpServer server = startStubServer();
        Map<String, List<String>> capturedHeaders = new ConcurrentHashMap<>();
        server.createContext("/internal/cs/client/sessions/sess-1/chat/stream", exchange -> {
            exchange.getRequestBody().readAllBytes();
            exchange.getRequestHeaders().forEach((k, v) -> capturedHeaders.put(k.toLowerCase(), List.copyOf(v)));
            exchange.getResponseHeaders().set("Content-Type", "text/event-stream;charset=UTF-8");
            byte[] body = ("event: meta\ndata: {\"session_id\":\"sess-1\"}\n\n"
                    + "event: done\ndata: {\"finish_reason\":\"stop\"}\n\n").getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });

        AgentProxyService proxyService = new AgentProxyService(createProperties(server));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ClientCsController(proxyService)).build();

        try (MockedStatic<StpUserUtil> stpUserMock = mockStatic(StpUserUtil.class)) {
            stpUserMock.when(StpUserUtil::getLoginIdAsLong).thenReturn(10086L);

            MvcResult asyncResult = mockMvc.perform(post("/client/cs/sessions/sess-1/chat/stream")
                            .accept(MediaType.TEXT_EVENT_STREAM, MediaType.APPLICATION_JSON)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"message\":\"推荐键盘\"}"))
                    .andExpect(request().asyncStarted())
                    .andExpect(header().string("Cache-Control", "no-cache"))
                    .andExpect(header().string("X-Accel-Buffering", "no"))
                    .andReturn();

            MvcResult completed = mockMvc.perform(asyncDispatch(asyncResult))
                    .andExpect(status().isOk())
                    .andReturn();

            assertEquals(List.of(MediaType.TEXT_EVENT_STREAM_VALUE), capturedHeaders.get("accept"));
            String sseBody = completed.getResponse().getContentAsString(StandardCharsets.UTF_8);
            assertTrue(sseBody.contains("event:meta"), "SSE body missing meta event: " + sseBody);
            assertTrue(sseBody.contains("\"session_id\":\"sess-1\""), "SSE body missing session payload: " + sseBody);
            assertTrue(sseBody.contains("event:done"), "SSE body missing done event: " + sseBody);
        }
    }

    @Test
    void controllers_enforceExpectedAuthDebounceAndPermissionsAnnotations() {
        int clientEndpoints = 0;
        for (Method method : ClientCsController.class.getDeclaredMethods()) {
            boolean isEndpoint = method.isAnnotationPresent(GetMapping.class)
                    || method.isAnnotationPresent(PostMapping.class)
                    || method.isAnnotationPresent(RequestMapping.class);
            if (!isEndpoint) {
                continue;
            }
            clientEndpoints++;
            assertFalse(method.isAnnotationPresent(SaIgnore.class),
                    () -> method.getName() + " must NOT carry @SaIgnore");
            SaCheckLogin checkLogin = method.getAnnotation(SaCheckLogin.class);
            assertNotNull(checkLogin, () -> method.getName() + " must carry @SaCheckLogin");
            assertEquals("shop_user", checkLogin.type());
            assertTrue(method.isAnnotationPresent(DebounceIgnore.class),
                    () -> method.getName() + " must carry @DebounceIgnore");
        }
        assertEquals(8, clientEndpoints);

        Map<String, String> expectedAdminPermissions = Map.of(
                "listTickets", "cs.ticket.query_table",
                "getTicketDetail", "cs.ticket.query_table",
                "replyTicket", "cs.ticket.reply",
                "closeTicket", "cs.ticket.close",
                "listKbDocuments", "cs.kb.query_table",
                "uploadKbDocument", "cs.kb.upload",
                "reindexKbDocument", "cs.kb.upload",
                "deleteKbDocument", "cs.kb.delete");

        int adminEndpoints = 0;
        for (Method method : AdminCsController.class.getDeclaredMethods()) {
            boolean isEndpoint = method.isAnnotationPresent(GetMapping.class)
                    || method.isAnnotationPresent(PostMapping.class)
                    || method.isAnnotationPresent(DeleteMapping.class)
                    || method.isAnnotationPresent(RequestMapping.class);
            if (!isEndpoint) {
                continue;
            }
            adminEndpoints++;
            assertFalse(method.isAnnotationPresent(SaIgnore.class),
                    () -> method.getName() + " must NOT carry @SaIgnore");
            assertTrue(method.isAnnotationPresent(SaCheckLogin.class),
                    () -> method.getName() + " must carry @SaCheckLogin");
            assertTrue(method.isAnnotationPresent(DebounceIgnore.class),
                    () -> method.getName() + " must carry @DebounceIgnore");
            SaCheckPermission perm = method.getAnnotation(SaCheckPermission.class);
            assertNotNull(perm, () -> method.getName() + " must carry @SaCheckPermission");
            String expectedPerm = expectedAdminPermissions.get(method.getName());
            assertNotNull(expectedPerm, () -> "Unexpected admin endpoint method: " + method.getName());
            assertArrayEquals(new String[]{expectedPerm}, perm.value());
        }
        assertEquals(8, adminEndpoints);
    }

    @Test
    void adminCsController_usesLoginUtilsAndRfc3986AdminNameEncoding() throws Exception {
        HttpServer server = startStubServer();
        Map<String, List<String>> capturedHeaders = new ConcurrentHashMap<>();
        server.createContext("/internal/cs/admin/tickets/t-100/reply", exchange -> {
            exchange.getRequestBody().readAllBytes();
            exchange.getRequestHeaders().forEach((k, v) -> capturedHeaders.put(k.toLowerCase(), List.copyOf(v)));
            byte[] resp = "{\"status\":\"REPLIED\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, resp.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resp);
            }
        });

        AgentProxyService proxyService = new AgentProxyService(createProperties(server));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AdminCsController(proxyService)).build();

        LoginUser loginUser = new LoginUser();
        BaseUserInfo userInfo = new BaseUserInfo();
        userInfo.setId(9L);
        userInfo.setUsername("cs_admin");
        userInfo.setNickname("客服 主管");
        loginUser.setUserInfo(userInfo);

        try (MockedStatic<LoginUtils> loginUtilsMock = mockStatic(LoginUtils.class)) {
            loginUtilsMock.when(LoginUtils::getLoginUser).thenReturn(loginUser);

            mockMvc.perform(post("/admin/cs/tickets/t-100/reply")
                            .header("X-Admin-Id", "1")
                            .header("X-Internal-Secret", "fake")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"content\":\"已处理\"}"))
                    .andExpect(status().isOk());
        }

        assertEquals(List.of("9"), capturedHeaders.get("x-admin-id"));
        assertEquals(List.of("kada-trusted-secret-2026"), capturedHeaders.get("x-internal-secret"));
        String encodedAdminName = capturedHeaders.get("x-admin-name").get(0);
        assertFalse(encodedAdminName.contains("+"));
        assertTrue(encodedAdminName.contains("%20"));
        assertEquals("客服 主管", java.net.URLDecoder.decode(encodedAdminName, StandardCharsets.UTF_8));
    }

    @Test
    void controllers_rejectUnsafePathSegmentsAndHandleDisabledAgent() {
        AgentProperties disabledProps = new AgentProperties();
        disabledProps.setEnabled(false);
        disabledProps.setBaseUrl("http://127.0.0.1:1");
        disabledProps.setInternalSecret("kada-trusted-secret-2026");

        AgentProxyService disabledProxy = new AgentProxyService(disabledProps);
        ResponseEntity<byte[]> resp = disabledProxy.forwardJson(
                HttpMethod.GET, "/internal/cs/client/sessions", null, new HttpHeaders(), null, ProxyIdentity.client(1L));
        assertEquals(503, resp.getStatusCode().value());

        SseEmitter sse = disabledProxy.forwardSse(
                HttpMethod.POST, "/internal/cs/client/sessions/s1/chat/stream", null, new HttpHeaders(), null, ProxyIdentity.client(1L));
        String sseText = readEmitterBufferText(sse);
        assertTrue(sseText.contains("event:error"), "Expected SSE error event when disabled: " + sseText);

        ClientCsController clientController = new ClientCsController(disabledProxy);
        AdminCsController adminController = new AdminCsController(disabledProxy);
        try (MockedStatic<StpUserUtil> stpUserMock = mockStatic(StpUserUtil.class);
             MockedStatic<LoginUtils> loginUtilsMock = mockStatic(LoginUtils.class)) {
            stpUserMock.when(StpUserUtil::getLoginIdAsLong).thenReturn(1L);
            LoginUser loginUser = new LoginUser();
            BaseUserInfo info = new BaseUserInfo();
            info.setId(1L);
            info.setUsername("admin");
            loginUser.setUserInfo(info);
            loginUtilsMock.when(LoginUtils::getLoginUser).thenReturn(loginUser);

            assertThrows(BusinessException.class, () -> clientController.listMessages("../admin", null));
            assertThrows(BusinessException.class, () -> clientController.getTicketDetail("a?b=c", null));
            assertThrows(BusinessException.class, () -> adminController.deleteKbDocument("doc/../../x", null));
        }
    }

    @Test
    void clientCsController_returnsJsonBusinessErrorOnInvalidSessionIdWithAcceptJson() throws Exception {
        AgentProperties props = new AgentProperties();
        props.setEnabled(true);
        props.setBaseUrl("http://127.0.0.1:1");
        props.setInternalSecret("kada-trusted-secret-2026");

        AgentProxyService proxyService = new AgentProxyService(props);
        ObjectMapper mapper = new ObjectMapper().configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ClientCsController(proxyService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(
                        new ByteArrayHttpMessageConverter(),
                        new StringHttpMessageConverter(StandardCharsets.UTF_8),
                        new MappingJackson2HttpMessageConverter(mapper))
                .build();

        try (MockedStatic<StpUserUtil> stpUserMock = mockStatic(StpUserUtil.class)) {
            stpUserMock.when(StpUserUtil::getLoginIdAsLong).thenReturn(10086L);

            mockMvc.perform(post("/client/cs/sessions/bad$id/chat/stream")
                            .accept(MediaType.APPLICATION_JSON)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"message\":\"hi\"}"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.code").value("C1008"));

            mockMvc.perform(post("/client/cs/sessions/bad$id/resume/stream")
                            .accept(MediaType.APPLICATION_JSON)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"action\":\"confirm\"}"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.code").value("C1008"));

            mockMvc.perform(post("/client/cs/sessions/bad$id/chat/stream")
                            .accept(MediaType.TEXT_EVENT_STREAM, MediaType.APPLICATION_JSON)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"message\":\"hi\"}"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.code").value("C1008"));
        }
    }

    @Test
    void forwardMultipart_sanitizesUntrustedFilenamesWithoutPathExceptions() throws Exception {
        HttpServer server = startStubServer();
        List<String> capturedBodies = new ArrayList<>();
        server.createContext("/internal/cs/admin/kb/documents/upload", exchange -> {
            capturedBodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] resp = "{\"id\":\"doc-2\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, resp.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resp);
            }
        });

        AgentProxyService proxyService = new AgentProxyService(createProperties(server));
        Map<String, String> cases = Map.of(
                "/", "upload.pdf",
                "a\0b.pdf", "ab.pdf",
                "C:\\fakepath\\law.pdf", "law.pdf");

        for (Map.Entry<String, String> entry : cases.entrySet()) {
            assertEquals(entry.getValue(), AgentProxyService.sanitizeFilename(entry.getKey()));
            MockMultipartFile file = new MockMultipartFile(
                    "file", entry.getKey(), "application/pdf", "%PDF-1.4".getBytes(StandardCharsets.UTF_8));
            ResponseEntity<byte[]> resp = proxyService.forwardMultipart(
                    "/internal/cs/admin/kb/documents/upload",
                    null,
                    file,
                    Map.of("category", "consumer_law"),
                    new HttpHeaders(),
                    ProxyIdentity.admin(1L, "admin"));
            assertEquals(200, resp.getStatusCode().value());
        }

        assertEquals(3, capturedBodies.size());
        assertTrue(capturedBodies.stream().anyMatch(b -> b.contains("filename=\"upload.pdf\"")));
        assertTrue(capturedBodies.stream().anyMatch(b -> b.contains("filename=\"ab.pdf\"")));
        assertTrue(capturedBodies.stream().anyMatch(b -> b.contains("filename=\"law.pdf\"")));
        assertTrue(capturedBodies.stream().noneMatch(b -> b.contains("fakepath") || b.contains("\0")));
    }
}
