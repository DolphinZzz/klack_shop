package com.sz.admin.shop_system.agent.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.sz.admin.shop_system.agent.service.AgentProxyService;
import com.sz.admin.shop_system.agent.service.AgentProxyService.ProxyIdentity;
import com.sz.core.common.annotation.DebounceIgnore;
import com.sz.core.common.enums.CommonResponseEnum;
import com.sz.security.util.StpUserUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 用户端智能客服超薄鉴权透传 Controller（/client/cs/**）
 */
@Tag(name = "[客户端] 智能客服透传接口")
@RestController
@RequestMapping("/client/cs")
@RequiredArgsConstructor
public class ClientCsController {

    private final AgentProxyService agentProxyService;

    @Operation(summary = "获取当前用户客服会话列表")
    @SaCheckLogin(type = "shop_user")
    @DebounceIgnore
    @GetMapping("/sessions")
    public ResponseEntity<byte[]> listSessions(HttpServletRequest request) {
        return agentProxyService.forwardJson(
                HttpMethod.GET,
                "/internal/cs/client/sessions",
                request,
                null,
                currentClientIdentity());
    }

    @Operation(summary = "新建客服会话")
    @SaCheckLogin(type = "shop_user")
    @DebounceIgnore
    @PostMapping("/sessions")
    public ResponseEntity<byte[]> createSession(HttpServletRequest request,
                                                @RequestBody(required = false) byte[] body) {
        return agentProxyService.forwardJson(
                HttpMethod.POST,
                "/internal/cs/client/sessions",
                request,
                body,
                currentClientIdentity());
    }

    @Operation(summary = "拉取客服会话历史消息")
    @SaCheckLogin(type = "shop_user")
    @DebounceIgnore
    @GetMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<byte[]> listMessages(@PathVariable String sessionId,
                                               HttpServletRequest request) {
        String safeSessionId = requireSafeSegment(sessionId);
        return agentProxyService.forwardJson(
                HttpMethod.GET,
                "/internal/cs/client/sessions/" + safeSessionId + "/messages",
                request,
                null,
                currentClientIdentity());
    }

    @Operation(summary = "发起客服对话流式响应（SSE）")
    @SaCheckLogin(type = "shop_user")
    @DebounceIgnore
    @PostMapping("/sessions/{sessionId}/chat/stream")
    public SseEmitter chatStream(@PathVariable String sessionId,
                                 HttpServletRequest request,
                                 HttpServletResponse response,
                                 @RequestBody(required = false) byte[] body) {
        String safeSessionId = requireSafeSegment(sessionId);
        applySseHeaders(response);
        return agentProxyService.forwardSse(
                HttpMethod.POST,
                "/internal/cs/client/sessions/" + safeSessionId + "/chat/stream",
                request,
                body,
                currentClientIdentity());
    }

    @Operation(summary = "恢复下单中断确认流式响应（SSE）")
    @SaCheckLogin(type = "shop_user")
    @DebounceIgnore
    @PostMapping("/sessions/{sessionId}/resume/stream")
    public SseEmitter resumeStream(@PathVariable String sessionId,
                                   HttpServletRequest request,
                                   HttpServletResponse response,
                                   @RequestBody(required = false) byte[] body) {
        String safeSessionId = requireSafeSegment(sessionId);
        applySseHeaders(response);
        return agentProxyService.forwardSse(
                HttpMethod.POST,
                "/internal/cs/client/sessions/" + safeSessionId + "/resume/stream",
                request,
                body,
                currentClientIdentity());
    }

    @Operation(summary = "获取当前用户工单分页列表")
    @SaCheckLogin(type = "shop_user")
    @DebounceIgnore
    @GetMapping("/tickets")
    public ResponseEntity<byte[]> listTickets(HttpServletRequest request) {
        return agentProxyService.forwardJson(
                HttpMethod.GET,
                "/internal/cs/client/tickets",
                request,
                null,
                currentClientIdentity());
    }

    @Operation(summary = "获取用户工单详情与回复流水")
    @SaCheckLogin(type = "shop_user")
    @DebounceIgnore
    @GetMapping("/tickets/{ticketId}")
    public ResponseEntity<byte[]> getTicketDetail(@PathVariable String ticketId,
                                                  HttpServletRequest request) {
        String safeTicketId = requireSafeSegment(ticketId);
        return agentProxyService.forwardJson(
                HttpMethod.GET,
                "/internal/cs/client/tickets/" + safeTicketId,
                request,
                null,
                currentClientIdentity());
    }

    @Operation(summary = "用户对工单补充追问")
    @SaCheckLogin(type = "shop_user")
    @DebounceIgnore
    @PostMapping("/tickets/{ticketId}/replies")
    public ResponseEntity<byte[]> replyTicket(@PathVariable String ticketId,
                                              HttpServletRequest request,
                                              @RequestBody(required = false) byte[] body) {
        String safeTicketId = requireSafeSegment(ticketId);
        return agentProxyService.forwardJson(
                HttpMethod.POST,
                "/internal/cs/client/tickets/" + safeTicketId + "/replies",
                request,
                body,
                currentClientIdentity());
    }

    private ProxyIdentity currentClientIdentity() {
        long userId = StpUserUtil.getLoginIdAsLong();
        CommonResponseEnum.INVALID_TOKEN.assertTrue(userId <= 0);
        return ProxyIdentity.client(userId);
    }

    private static void applySseHeaders(HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache");
        response.setHeader("X-Accel-Buffering", "no");
    }

    private static String requireSafeSegment(String value) {
        return AgentProxyService.requireSafeSegment(value);
    }
}
