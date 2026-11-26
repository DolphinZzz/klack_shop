package com.sz.admin.shop_system.agent.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.sz.admin.shop_system.agent.service.AgentProxyService;
import com.sz.admin.shop_system.agent.service.AgentProxyService.ProxyIdentity;
import com.sz.core.common.annotation.DebounceIgnore;
import com.sz.core.common.entity.BaseUserInfo;
import com.sz.core.common.entity.LoginUser;
import com.sz.core.common.enums.CommonResponseEnum;
import com.sz.security.core.util.LoginUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 管理端智能客服工单与知识库超薄鉴权透传 Controller（/admin/cs/**）
 */
@Tag(name = "[管理端] 智能客服工单与知识库接口")
@RestController
@RequestMapping("/admin/cs")
@RequiredArgsConstructor
public class AdminCsController {

    private final AgentProxyService agentProxyService;

    @Operation(summary = "管理端客服工单分页查询")
    @SaCheckLogin
    @SaCheckPermission("cs.ticket.query_table")
    @DebounceIgnore
    @GetMapping("/tickets")
    public ResponseEntity<byte[]> listTickets(HttpServletRequest request) {
        return agentProxyService.forwardJson(
                HttpMethod.GET,
                "/internal/cs/admin/tickets",
                request,
                null,
                currentAdminIdentity());
    }

    @Operation(summary = "管理端客服工单详情与会话回放")
    @SaCheckLogin
    @SaCheckPermission("cs.ticket.query_table")
    @DebounceIgnore
    @GetMapping("/tickets/{ticketId}")
    public ResponseEntity<byte[]> getTicketDetail(@PathVariable String ticketId,
                                                  HttpServletRequest request) {
        String safeTicketId = requireSafeSegment(ticketId);
        return agentProxyService.forwardJson(
                HttpMethod.GET,
                "/internal/cs/admin/tickets/" + safeTicketId,
                request,
                null,
                currentAdminIdentity());
    }

    @Operation(summary = "管理端回复客服工单")
    @SaCheckLogin
    @SaCheckPermission("cs.ticket.reply")
    @DebounceIgnore
    @PostMapping("/tickets/{ticketId}/reply")
    public ResponseEntity<byte[]> replyTicket(@PathVariable String ticketId,
                                              HttpServletRequest request,
                                              @RequestBody(required = false) byte[] body) {
        String safeTicketId = requireSafeSegment(ticketId);
        return agentProxyService.forwardJson(
                HttpMethod.POST,
                "/internal/cs/admin/tickets/" + safeTicketId + "/reply",
                request,
                body,
                currentAdminIdentity());
    }

    @Operation(summary = "管理端关闭客服工单")
    @SaCheckLogin
    @SaCheckPermission("cs.ticket.close")
    @DebounceIgnore
    @PostMapping("/tickets/{ticketId}/close")
    public ResponseEntity<byte[]> closeTicket(@PathVariable String ticketId,
                                              HttpServletRequest request,
                                              @RequestBody(required = false) byte[] body) {
        String safeTicketId = requireSafeSegment(ticketId);
        return agentProxyService.forwardJson(
                HttpMethod.POST,
                "/internal/cs/admin/tickets/" + safeTicketId + "/close",
                request,
                body,
                currentAdminIdentity());
    }

    @Operation(summary = "管理端知识库文档分页列表")
    @SaCheckLogin
    @SaCheckPermission("cs.kb.query_table")
    @DebounceIgnore
    @GetMapping("/kb/documents")
    public ResponseEntity<byte[]> listKbDocuments(HttpServletRequest request) {
        return agentProxyService.forwardJson(
                HttpMethod.GET,
                "/internal/cs/admin/kb/documents",
                request,
                null,
                currentAdminIdentity());
    }

    @Operation(summary = "管理端上传知识库 PDF 文档并触发异步解析入库")
    @SaCheckLogin
    @SaCheckPermission("cs.kb.upload")
    @DebounceIgnore
    @PostMapping(value = "/kb/documents/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> uploadKbDocument(@RequestParam("file") MultipartFile file,
                                                   @RequestParam(value = "category", required = false) String category,
                                                   @RequestParam(value = "title", required = false) String title,
                                                   HttpServletRequest request) {
        Map<String, String> formFields = new LinkedHashMap<>();
        if (category != null) {
            formFields.put("category", category);
        }
        if (title != null) {
            formFields.put("title", title);
        }
        if (request != null) {
            request.getParameterMap().forEach((key, values) -> {
                if (key == null || "file".equals(key) || "category".equals(key) || "title".equals(key)) {
                    return;
                }
                if (values != null && values.length > 0 && values[0] != null) {
                    formFields.put(key, values[0]);
                }
            });
        }
        return agentProxyService.forwardMultipart(
                "/internal/cs/admin/kb/documents/upload",
                request == null ? null : request.getQueryString(),
                file,
                formFields,
                AgentProxyService.extractHeaders(request),
                currentAdminIdentity());
    }

    @Operation(summary = "管理端重新解析入库知识库文档")
    @SaCheckLogin
    @SaCheckPermission("cs.kb.upload")
    @DebounceIgnore
    @PostMapping("/kb/documents/{docId}/reindex")
    public ResponseEntity<byte[]> reindexKbDocument(@PathVariable String docId,
                                                    HttpServletRequest request,
                                                    @RequestBody(required = false) byte[] body) {
        String safeDocId = requireSafeSegment(docId);
        return agentProxyService.forwardJson(
                HttpMethod.POST,
                "/internal/cs/admin/kb/documents/" + safeDocId + "/reindex",
                request,
                body,
                currentAdminIdentity());
    }

    @Operation(summary = "管理端删除知识库文档")
    @SaCheckLogin
    @SaCheckPermission("cs.kb.delete")
    @DebounceIgnore
    @DeleteMapping("/kb/documents/{docId}")
    public ResponseEntity<byte[]> deleteKbDocument(@PathVariable String docId,
                                                   HttpServletRequest request) {
        String safeDocId = requireSafeSegment(docId);
        return agentProxyService.forwardJson(
                HttpMethod.DELETE,
                "/internal/cs/admin/kb/documents/" + safeDocId,
                request,
                null,
                currentAdminIdentity());
    }

    private ProxyIdentity currentAdminIdentity() {
        LoginUser loginUser = LoginUtils.getLoginUser();
        CommonResponseEnum.INVALID_TOKEN.assertNull(loginUser);
        BaseUserInfo userInfo = loginUser.getUserInfo();
        CommonResponseEnum.INVALID_TOKEN.assertNull(userInfo);
        Long adminId = userInfo.getId();
        CommonResponseEnum.INVALID_TOKEN.assertTrue(adminId == null || adminId <= 0);

        String adminName = userInfo.getNickname();
        if (adminName == null || adminName.isBlank()) {
            adminName = userInfo.getUsername();
        }
        if (adminName == null || adminName.isBlank()) {
            adminName = "admin-" + adminId;
        }
        return ProxyIdentity.admin(adminId, adminName);
    }

    private static String requireSafeSegment(String value) {
        return AgentProxyService.requireSafeSegment(value);
    }
}
