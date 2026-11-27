package com.sz.admin.shop_system.agent.service;

import com.sz.admin.shop_system.agent.config.AgentProperties;
import com.sz.core.common.enums.CommonResponseEnum;
import com.sz.core.common.exception.common.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.util.UriUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.SequenceInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

/**
 * 智能客服 Agent 内网 HTTP / Multipart / SSE 透传代理服务
 */
@Slf4j
@Service
public class AgentProxyService {

    public static final String HEADER_INTERNAL_SECRET = "X-Internal-Secret";
    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_ADMIN_ID = "X-Admin-Id";
    public static final String HEADER_ADMIN_NAME = "X-Admin-Name";
    public static final String HEADER_REQUEST_ID = "X-Request-Id";

    private static final Pattern SAFE_PATH_SEGMENT_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{1,64}$");
    private static final long DEFAULT_STREAM_TIMEOUT_MS = 300_000L;
    private static final Duration JSON_REQUEST_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration MULTIPART_REQUEST_TIMEOUT = Duration.ofSeconds(60);
    private static final int MAX_SSE_ERROR_BODY_BYTES = 8192;

    /**
     * 必须在转发前剥离/覆盖的头：防伪造内网身份头、公网凭证头及 HTTP/1.1 逐跳受限头
     */
    private static final Set<String> STRIPPED_REQUEST_HEADERS = Set.of(
            "x-internal-secret",
            "x-user-id",
            "x-admin-id",
            "x-admin-name",
            "x-request-id",
            "authorization",
            "cookie",
            "accept-encoding",
            "host",
            "content-length",
            "connection",
            "keep-alive",
            "upgrade",
            "transfer-encoding",
            "te",
            "trailer",
            "proxy-connection",
            "proxy-authorization",
            "proxy-authenticate",
            "expect",
            "http2-settings"
    );

    private static final Set<String> STRIPPED_RESPONSE_HEADERS = Set.of(
            "transfer-encoding",
            "connection",
            "content-length",
            "keep-alive",
            "proxy-connection"
    );

    private final AgentProperties agentProperties;
    private final HttpClient httpClient;

    @Autowired
    public AgentProxyService(AgentProperties agentProperties) {
        this(agentProperties, HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(Math.max(1000, agentProperties.getConnectTimeoutMs())))
                .version(HttpClient.Version.HTTP_1_1)
                .build());
    }

    public AgentProxyService(AgentProperties agentProperties, HttpClient httpClient) {
        this.agentProperties = agentProperties;
        this.httpClient = httpClient;
    }

    /**
     * 服务端可信身份上下文（用户端仅设置 userId，管理端仅设置 adminId + adminName）
     */
    public record ProxyIdentity(Long userId, Long adminId, String adminName) {

        public static ProxyIdentity client(long userId) {
            return new ProxyIdentity(userId, null, null);
        }

        public static ProxyIdentity admin(long adminId, String adminName) {
            return new ProxyIdentity(null, adminId, adminName);
        }
    }

    public ResponseEntity<byte[]> forwardJson(HttpMethod method,
                                              String upstreamPath,
                                              HttpServletRequest request,
                                              byte[] body,
                                              ProxyIdentity identity) {
        return forwardJson(
                method,
                upstreamPath,
                request == null ? null : request.getQueryString(),
                extractHeaders(request),
                body,
                identity);
    }

    public ResponseEntity<byte[]> forwardJson(HttpMethod method,
                                              String upstreamPath,
                                              String queryString,
                                              HttpHeaders incomingHeaders,
                                              byte[] body,
                                              ProxyIdentity identity) {
        if (!agentProperties.isEnabled()) {
            return jsonErrorResponse(HttpStatus.SERVICE_UNAVAILABLE, "C9999", "智能客服服务未启用");
        }
        String requestId = UUID.randomUUID().toString();
        try {
            HttpRequest upstreamRequest = buildUpstreamRequest(
                    method, upstreamPath, queryString, incomingHeaders, body, identity, requestId, JSON_REQUEST_TIMEOUT, false);

            HttpResponse<byte[]> upstreamResponse = httpClient.send(upstreamRequest, HttpResponse.BodyHandlers.ofByteArray());
            return toResponseEntity(upstreamResponse);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Agent proxy request interrupted [requestId={}, path={}]", requestId, upstreamPath);
            return jsonErrorResponse(HttpStatus.BAD_GATEWAY, "C9999", "智能客服服务响应中断，请稍后再试");
        } catch (Exception e) {
            log.error("Agent proxy request failed [requestId={}, path={}]", requestId, upstreamPath, e);
            return jsonErrorResponse(HttpStatus.BAD_GATEWAY, "C9999", "智能客服服务暂时不可用，请稍后再试");
        }
    }

    public ResponseEntity<byte[]> forwardMultipart(String upstreamPath,
                                                   String queryString,
                                                   MultipartFile file,
                                                   Map<String, String> formFields,
                                                   HttpHeaders incomingHeaders,
                                                   ProxyIdentity identity) {
        if (file == null) {
            throw new BusinessException(CommonResponseEnum.FILE_NOT_EXISTS);
        }
        if (!agentProperties.isEnabled()) {
            return jsonErrorResponse(HttpStatus.SERVICE_UNAVAILABLE, "C9999", "智能客服服务未启用");
        }
        String requestId = UUID.randomUUID().toString();
        String boundary = "----KadaAgentBoundary" + UUID.randomUUID().toString().replace("-", "");
        try (InputStream fileStream = file.getInputStream();
             InputStream multipartStream = buildMultipartStream(boundary, file, fileStream, formFields)) {
            URI targetUri = buildTargetUri(upstreamPath, queryString);
            AtomicBoolean supplied = new AtomicBoolean(false);
            HttpRequest.Builder builder = HttpRequest.newBuilder(targetUri)
                    .timeout(MULTIPART_REQUEST_TIMEOUT)
                    .POST(HttpRequest.BodyPublishers.ofInputStream(() -> {
                        if (!supplied.compareAndSet(false, true)) {
                            throw new IllegalStateException("Multipart body stream cannot be replayed");
                        }
                        return multipartStream;
                    }));

            applySanitizedHeaders(builder, incomingHeaders, identity, requestId, true);
            builder.header(HttpHeaders.CONTENT_TYPE, "multipart/form-data; boundary=" + boundary);

            HttpResponse<byte[]> upstreamResponse = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
            return toResponseEntity(upstreamResponse);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Agent multipart upload interrupted [requestId={}, path={}]", requestId, upstreamPath);
            return jsonErrorResponse(HttpStatus.BAD_GATEWAY, "C9999", "智能客服文档上传中断，请稍后再试");
        } catch (Exception e) {
            log.error("Agent multipart upload failed [requestId={}, path={}]", requestId, upstreamPath, e);
            return jsonErrorResponse(HttpStatus.BAD_GATEWAY, "C9999", "智能客服服务暂时不可用，请稍后再试");
        }
    }

    public SseEmitter forwardSse(HttpMethod method,
                                 String upstreamPath,
                                 HttpServletRequest request,
                                 byte[] body,
                                 ProxyIdentity identity) {
        return forwardSse(
                method,
                upstreamPath,
                request == null ? null : request.getQueryString(),
                extractHeaders(request),
                body,
                identity);
    }

    public SseEmitter forwardSse(HttpMethod method,
                                 String upstreamPath,
                                 String queryString,
                                 HttpHeaders incomingHeaders,
                                 byte[] body,
                                 ProxyIdentity identity) {
        long streamTimeoutMs = agentProperties.getStreamTimeoutMs() > 0
                ? agentProperties.getStreamTimeoutMs()
                : DEFAULT_STREAM_TIMEOUT_MS;

        SseEmitter emitter = new SseEmitter(streamTimeoutMs) {
            @Override
            protected void extendResponse(ServerHttpResponse outputMessage) {
                super.extendResponse(outputMessage);
                outputMessage.getHeaders().set(HttpHeaders.CACHE_CONTROL, "no-cache");
                outputMessage.getHeaders().set("X-Accel-Buffering", "no");
            }
        };

        if (!agentProperties.isEnabled()) {
            emitSseErrorAndComplete(emitter, "AGENT_DISABLED", "智能客服服务未启用");
            return emitter;
        }

        String requestId = UUID.randomUUID().toString();
        AtomicBoolean cancelled = new AtomicBoolean(false);
        AtomicReference<InputStream> upstreamStreamRef = new AtomicReference<>();
        AtomicReference<CompletableFuture<HttpResponse<InputStream>>> responseFutureRef = new AtomicReference<>();
        AtomicReference<Thread> workerThreadRef = new AtomicReference<>();

        Runnable cancelUpstream = () -> {
            if (cancelled.compareAndSet(false, true)) {
                CompletableFuture<HttpResponse<InputStream>> future = responseFutureRef.get();
                if (future != null) {
                    future.cancel(true);
                }
                closeQuietly(upstreamStreamRef.getAndSet(null));
                Thread worker = workerThreadRef.get();
                if (worker != null) {
                    worker.interrupt();
                }
            }
        };

        emitter.onCompletion(cancelUpstream);
        emitter.onTimeout(cancelUpstream);
        emitter.onError(ex -> cancelUpstream.run());

        Thread.startVirtualThread(() -> {
            workerThreadRef.set(Thread.currentThread());
            if (cancelled.get()) {
                return;
            }
            try {
                HttpRequest upstreamRequest = buildUpstreamRequest(
                        method, upstreamPath, queryString, incomingHeaders, body, identity, requestId, Duration.ofMillis(streamTimeoutMs), true);

                CompletableFuture<HttpResponse<InputStream>> responseFuture =
                        httpClient.sendAsync(upstreamRequest, HttpResponse.BodyHandlers.ofInputStream());
                responseFutureRef.set(responseFuture);
                if (cancelled.get()) {
                    responseFuture.cancel(true);
                    return;
                }

                HttpResponse<InputStream> upstreamResponse = responseFuture.get(streamTimeoutMs, TimeUnit.MILLISECONDS);
                InputStream bodyStream = upstreamResponse.body();
                upstreamStreamRef.set(bodyStream);
                if (cancelled.get()) {
                    closeQuietly(upstreamStreamRef.getAndSet(null));
                    return;
                }

                int status = upstreamResponse.statusCode();
                if (status < 200 || status >= 300) {
                    String errBody = readBoundedUtf8(bodyStream, MAX_SSE_ERROR_BODY_BYTES);
                    log.warn("Agent SSE upstream error [requestId={}, status={}, body={}]", requestId, status, errBody);
                    if (!cancelled.get()) {
                        emitSseErrorAndComplete(emitter, "UPSTREAM_" + status, "服务暂时繁忙，请稍后再试或转人工工单");
                    }
                    return;
                }

                relaySseStream(bodyStream, emitter, cancelled, cancelUpstream);
                if (!cancelled.get()) {
                    emitter.complete();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (CancellationException e) {
                log.debug("Agent SSE upstream cancelled [requestId={}]", requestId);
            } catch (Exception e) {
                if (!cancelled.get()) {
                    log.error("Agent SSE stream failed [requestId={}, path={}]", requestId, upstreamPath, e);
                    emitSseErrorAndComplete(emitter, "AGENT_STREAM_ERROR", "服务暂时繁忙，请稍后再试或转人工工单");
                }
            } finally {
                closeQuietly(upstreamStreamRef.getAndSet(null));
            }
        });

        return emitter;
    }

    public static HttpHeaders extractHeaders(HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        if (request == null) {
            return headers;
        }
        Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames == null) {
            return headers;
        }
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            Enumeration<String> values = request.getHeaders(name);
            if (values != null) {
                while (values.hasMoreElements()) {
                    headers.add(name, values.nextElement());
                }
            }
        }
        return headers;
    }

    /**
     * 校验动态路径参数为安全单段标识符，防止内网路径穿越或参数注入
     */
    public static String requireSafeSegment(String value) {
        CommonResponseEnum.INVALID.message("非法的资源标识").assertTrue(
                value == null || !SAFE_PATH_SEGMENT_PATTERN.matcher(value).matches());
        return value;
    }

    /**
     * 使用 RFC 3986 百分号编码（空格编为 %20），匹配 Python urllib.parse.unquote
     */
    public static String encodeAdminName(String adminName) {
        if (adminName == null || adminName.isEmpty()) {
            return "";
        }
        return UriUtils.encode(adminName, StandardCharsets.UTF_8);
    }

    private HttpRequest buildUpstreamRequest(HttpMethod method,
                                             String upstreamPath,
                                             String queryString,
                                             HttpHeaders incomingHeaders,
                                             byte[] body,
                                             ProxyIdentity identity,
                                             String requestId,
                                             Duration timeout,
                                             boolean sse) {
        URI targetUri = buildTargetUri(upstreamPath, queryString);
        HttpRequest.BodyPublisher publisher = (body != null && body.length > 0)
                ? HttpRequest.BodyPublishers.ofByteArray(body)
                : HttpRequest.BodyPublishers.noBody();

        HttpRequest.Builder builder = HttpRequest.newBuilder(targetUri)
                .timeout(timeout)
                .method(method.name(), publisher);

        applySanitizedHeaders(builder, incomingHeaders, identity, requestId, false);
        if (sse) {
            builder.setHeader(HttpHeaders.ACCEPT, MediaType.TEXT_EVENT_STREAM_VALUE);
        }
        if (body != null && body.length > 0 && !hasHeaderIgnoreCase(incomingHeaders, HttpHeaders.CONTENT_TYPE)) {
            builder.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        }
        return builder.build();
    }

    private void relaySseStream(InputStream bodyStream,
                                SseEmitter emitter,
                                AtomicBoolean cancelled,
                                Runnable cancelUpstream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(bodyStream, StandardCharsets.UTF_8))) {
            String eventName = null;
            String eventId = null;
            StringBuilder dataBuffer = new StringBuilder();

            String line;
            while (!cancelled.get() && (line = reader.readLine()) != null) {
                if (line.isEmpty()) {
                    if (dataBuffer.length() > 0 || eventName != null || eventId != null) {
                        sendSseEvent(emitter, eventName, eventId, dataBuffer.toString(), cancelUpstream);
                        eventName = null;
                        eventId = null;
                        dataBuffer.setLength(0);
                    }
                    continue;
                }
                if (line.startsWith(":")) {
                    String commentText = line.substring(1).stripLeading();
                    try {
                        emitter.send(SseEmitter.event().comment(commentText));
                    } catch (IOException | IllegalStateException e) {
                        cancelUpstream.run();
                        return;
                    }
                    continue;
                }
                int colonIdx = line.indexOf(':');
                String field;
                String value;
                if (colonIdx < 0) {
                    field = line;
                    value = "";
                } else {
                    field = line.substring(0, colonIdx);
                    value = line.substring(colonIdx + 1);
                    if (value.startsWith(" ")) {
                        value = value.substring(1);
                    }
                }
                switch (field) {
                    case "event" -> eventName = value;
                    case "data" -> {
                        if (dataBuffer.length() > 0) {
                            dataBuffer.append('\n');
                        }
                        dataBuffer.append(value);
                    }
                    case "id" -> eventId = value;
                    default -> {
                    }
                }
            }

            if (!cancelled.get() && (dataBuffer.length() > 0 || eventName != null || eventId != null)) {
                sendSseEvent(emitter, eventName, eventId, dataBuffer.toString(), cancelUpstream);
            }
        }
    }

    private void sendSseEvent(SseEmitter emitter,
                              String eventName,
                              String eventId,
                              String data,
                              Runnable cancelUpstream) {
        try {
            SseEmitter.SseEventBuilder builder = SseEmitter.event();
            if (eventId != null) {
                builder.id(eventId);
            }
            if (eventName != null) {
                builder.name(eventName);
            }
            builder.data(data, MediaType.TEXT_PLAIN);
            emitter.send(builder);
        } catch (IOException | IllegalStateException e) {
            cancelUpstream.run();
        }
    }

    private void emitSseErrorAndComplete(SseEmitter emitter, String code, String message) {
        try {
            String safeCode = escapeJson(code);
            String safeMsg = escapeJson(message);
            String json = "{\"code\":\"" + safeCode + "\",\"message\":\"" + safeMsg + "\"}";
            emitter.send(SseEmitter.event().name("error").data(json, MediaType.TEXT_PLAIN));
        } catch (IOException | IllegalStateException ignored) {
            // client already disconnected
        }
        try {
            emitter.complete();
        } catch (IllegalStateException ignored) {
            // emitter already completed
        }
    }

    private void applySanitizedHeaders(HttpRequest.Builder builder,
                                       HttpHeaders incomingHeaders,
                                       ProxyIdentity identity,
                                       String requestId,
                                       boolean stripContentType) {
        if (incomingHeaders != null) {
            incomingHeaders.forEach((name, values) -> {
                if (name == null || values == null) {
                    return;
                }
                String lower = name.toLowerCase(Locale.ROOT);
                if (STRIPPED_REQUEST_HEADERS.contains(lower)) {
                    return;
                }
                if (stripContentType && "content-type".equals(lower)) {
                    return;
                }
                for (String value : values) {
                    if (value != null && !containsControlChars(value)) {
                        builder.header(name, value);
                    }
                }
            });
        }

        String secret = agentProperties.getInternalSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("sz.agent.internal-secret must not be blank");
        }
        builder.header(HEADER_INTERNAL_SECRET, secret);
        builder.header(HEADER_REQUEST_ID, requestId);

        if (identity != null) {
            if (identity.userId() != null) {
                builder.header(HEADER_USER_ID, String.valueOf(identity.userId()));
            }
            if (identity.adminId() != null) {
                builder.header(HEADER_ADMIN_ID, String.valueOf(identity.adminId()));
                builder.header(HEADER_ADMIN_NAME, encodeAdminName(identity.adminName()));
            }
        }
    }

    private ResponseEntity<byte[]> toResponseEntity(HttpResponse<byte[]> upstreamResponse) {
        HttpHeaders responseHeaders = new HttpHeaders();
        upstreamResponse.headers().map().forEach((name, values) -> {
            if (name == null || values == null || name.startsWith(":")) {
                return;
            }
            if (STRIPPED_RESPONSE_HEADERS.contains(name.toLowerCase(Locale.ROOT))) {
                return;
            }
            for (String value : values) {
                responseHeaders.add(name, value);
            }
        });
        byte[] body = upstreamResponse.body();
        if (!responseHeaders.containsKey(HttpHeaders.CONTENT_TYPE) && body != null && body.length > 0) {
            responseHeaders.setContentType(MediaType.APPLICATION_JSON);
        }
        return ResponseEntity.status(upstreamResponse.statusCode())
                .headers(responseHeaders)
                .body(body);
    }

    private ResponseEntity<byte[]> jsonErrorResponse(HttpStatus status, String code, String message) {
        String payload = "{\"code\":\"" + escapeJson(code) + "\",\"message\":\"" + escapeJson(message) + "\"}";
        return ResponseEntity.status(status)
                .contentType(new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8))
                .body(payload.getBytes(StandardCharsets.UTF_8));
    }

    private URI buildTargetUri(String upstreamPath, String queryString) {
        String base = agentProperties.getBaseUrl();
        if (base == null || base.isBlank()) {
            throw new IllegalStateException("sz.agent.base-url must not be blank");
        }
        String normalizedBase = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        String normalizedPath = upstreamPath.startsWith("/") ? upstreamPath : "/" + upstreamPath;
        String full = normalizedBase + normalizedPath;
        if (queryString != null && !queryString.isBlank()) {
            full = full + "?" + queryString;
        }
        return URI.create(full);
    }

    private InputStream buildMultipartStream(String boundary,
                                             MultipartFile file,
                                             InputStream fileStream,
                                             Map<String, String> formFields) throws IOException {
        ByteArrayOutputStream prefix = new ByteArrayOutputStream();
        if (formFields != null) {
            for (Map.Entry<String, String> entry : formFields.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) {
                    continue;
                }
                String fieldName = sanitizeHeaderToken(entry.getKey());
                if (fieldName.isEmpty()) {
                    continue;
                }
                prefix.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
                prefix.write(("Content-Disposition: form-data; name=\"" + fieldName + "\"\r\n").getBytes(StandardCharsets.UTF_8));
                prefix.write("Content-Type: text/plain; charset=UTF-8\r\n\r\n".getBytes(StandardCharsets.UTF_8));
                prefix.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                prefix.write("\r\n".getBytes(StandardCharsets.UTF_8));
            }
        }

        String safeFilename = sanitizeFilename(file.getOriginalFilename());
        String fileContentType = file.getContentType();
        if (fileContentType == null || fileContentType.isBlank() || containsControlChars(fileContentType)) {
            fileContentType = MediaType.APPLICATION_PDF_VALUE;
        }

        prefix.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        prefix.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + safeFilename + "\"\r\n")
                .getBytes(StandardCharsets.UTF_8));
        prefix.write(("Content-Type: " + fileContentType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));

        byte[] suffix = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);

        return new SequenceInputStream(Collections.enumeration(List.of(
                new ByteArrayInputStream(prefix.toByteArray()),
                fileStream,
                new ByteArrayInputStream(suffix))));
    }

    /**
     * 安全提取上传文件 basename（兼容 / 与 \ 分隔符），剥离控制字符与引号，空串回退 upload.pdf
     */
    public static String sanitizeFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "upload.pdf";
        }
        int lastSep = Math.max(originalFilename.lastIndexOf('/'), originalFilename.lastIndexOf('\\'));
        String baseName = lastSep >= 0 ? originalFilename.substring(lastSep + 1) : originalFilename;
        String cleaned = sanitizeHeaderToken(baseName).trim();
        if (cleaned.isEmpty() || ".".equals(cleaned) || "..".equals(cleaned)) {
            return "upload.pdf";
        }
        return cleaned;
    }

    private static boolean hasHeaderIgnoreCase(HttpHeaders headers, String targetName) {
        if (headers == null) {
            return false;
        }
        for (String key : headers.keySet()) {
            if (targetName.equalsIgnoreCase(key)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsControlChars(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c < 0x20 || c == 0x7F) {
                return true;
            }
        }
        return false;
    }

    private static String sanitizeHeaderToken(String input) {
        StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c < 0x20 || c == 0x7F || c == '"') {
                continue;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private static String escapeJson(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String readBoundedUtf8(InputStream stream, int maxBytes) throws IOException {
        byte[] buf = new byte[Math.min(maxBytes, 4096)];
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int total = 0;
        int read;
        while (total < maxBytes && (read = stream.read(buf, 0, Math.min(buf.length, maxBytes - total))) != -1) {
            out.write(buf, 0, read);
            total += read;
        }
        return out.toString(StandardCharsets.UTF_8);
    }

    private static void closeQuietly(InputStream stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException ignored) {
                // ignore close errors during cancellation
            }
        }
    }
}
