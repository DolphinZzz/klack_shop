import { ElMessage } from 'element-plus';
import router from '@/router';
import { http } from '@/request/http';
import { useAuthStore } from '@/stores/modules/auth';
import type {
  CreateCsSessionParams,
  CsChatStreamRequest,
  CsResumeStreamRequest,
  CsSession,
  CsSessionListResponse,
  CsSessionMessagesResponse,
  CsSseCardEvent,
  CsSseDoneEvent,
  CsSseErrorEvent,
  CsSseInterruptEvent,
  CsSseMetaEvent,
  CsSseTokenEvent,
  CsStreamHandlers,
  CsTicketDetail,
  CsTicketPageQuery,
  CsTicketPageResponse,
  CsTicketReplyRequest
} from '@/api/types/cs';

const CODE_TOKEN_FAIL = 'C105';

export const listCsSessionsApi = (params?: { limit?: number; status?: string }) => {
  return http.get<CsSessionListResponse>('/client/cs/sessions', params as Record<string, unknown>);
};

export const createCsSessionApi = (data?: CreateCsSessionParams) => {
  return http.post<CsSession>('/client/cs/sessions', data ?? {});
};

export const getCsSessionMessagesApi = (sessionId: string) => {
  return http.get<CsSessionMessagesResponse>(`/client/cs/sessions/${encodeURIComponent(sessionId)}/messages`);
};

export const listCsTicketsApi = (params?: CsTicketPageQuery) => {
  return http.get<CsTicketPageResponse>('/client/cs/tickets', params as Record<string, unknown>);
};

export const getCsTicketDetailApi = (ticketId: string) => {
  return http.get<CsTicketDetail>(`/client/cs/tickets/${encodeURIComponent(ticketId)}`);
};

export const replyCsTicketApi = (ticketId: string, data: CsTicketReplyRequest) => {
  return http.post<CsTicketDetail>(`/client/cs/tickets/${encodeURIComponent(ticketId)}/replies`, data);
};

function resolveApiUrl(path: string): string {
  const baseUrl = String(import.meta.env.VITE_API_URL || '').replace(/\/+$/, '');
  const normalizedPath = path.startsWith('/') ? path : `/${path}`;
  return `${baseUrl}${normalizedPath}`;
}

function handleAuthExpired(message: string, handlers: CsStreamHandlers) {
  const authStore = useAuthStore();
  authStore.logout();
  const displayMsg = message || '登录状态失效，请重新登录';
  ElMessage.error(displayMsg);
  if (handlers.onAuthExpired) {
    handlers.onAuthExpired(displayMsg);
  } else {
    const currentFullPath = router.currentRoute.value.fullPath;
    void router.replace({
      path: '/login',
      query: currentFullPath && currentFullPath !== '/login' ? { redirect: currentFullPath } : undefined
    });
  }
}

interface ParsedErrorJson {
  code?: string;
  message?: string;
  detail?: string;
}

function parseErrorJson(rawText: string): ParsedErrorJson | null {
  try {
    const parsed = JSON.parse(rawText);
    if (parsed && typeof parsed === 'object') {
      return parsed as ParsedErrorJson;
    }
  } catch {
    // ignore non-JSON body
  }
  return null;
}

function dispatchSseFrame(rawFrame: string, handlers: CsStreamHandlers, state: { sawTerminal: boolean }) {
  const lines = rawFrame.split(/\r?\n/);
  let eventName = 'message';
  const dataLines: string[] = [];

  for (const line of lines) {
    if (!line || line.startsWith(':')) {
      continue;
    }
    const colonIdx = line.indexOf(':');
    let field = line;
    let value = '';
    if (colonIdx >= 0) {
      field = line.slice(0, colonIdx);
      value = line.slice(colonIdx + 1);
      if (value.startsWith(' ')) {
        value = value.slice(1);
      }
    }
    if (field === 'event') {
      eventName = value || 'message';
    } else if (field === 'data') {
      dataLines.push(value);
    }
  }

  if (dataLines.length === 0) {
    return;
  }

  const rawData = dataLines.join('\n');
  let parsed: unknown = rawData;
  try {
    parsed = JSON.parse(rawData);
  } catch {
    // keep raw string fallback
  }

  switch (eventName) {
    case 'meta':
      if (parsed && typeof parsed === 'object') {
        handlers.onMeta?.(parsed as CsSseMetaEvent);
      }
      break;
    case 'token':
      if (parsed && typeof parsed === 'object') {
        handlers.onToken?.(parsed as CsSseTokenEvent);
      }
      break;
    case 'card':
      if (parsed && typeof parsed === 'object') {
        handlers.onCard?.(parsed as CsSseCardEvent);
      }
      break;
    case 'interrupt':
      if (parsed && typeof parsed === 'object') {
        handlers.onInterrupt?.(parsed as CsSseInterruptEvent);
      }
      break;
    case 'done':
      state.sawTerminal = true;
      if (parsed && typeof parsed === 'object') {
        handlers.onDone?.(parsed as CsSseDoneEvent);
      } else {
        handlers.onDone?.({ message_id: '', finish_reason: 'stop' });
      }
      break;
    case 'error':
      state.sawTerminal = true;
      if (parsed && typeof parsed === 'object') {
        const err = parsed as CsSseErrorEvent;
        handlers.onError?.({
          code: err.code,
          message: err.message || '服务暂时繁忙，请稍后再试或转人工工单'
        });
      } else {
        handlers.onError?.({
          code: 'AGENT_STREAM_ERROR',
          message: typeof parsed === 'string' && parsed ? parsed : '服务暂时繁忙，请稍后再试'
        });
      }
      break;
    default:
      break;
  }
}

async function streamCsSse(
  path: string,
  body: unknown,
  handlers: CsStreamHandlers,
  signal?: AbortSignal
): Promise<void> {
  const authStore = useAuthStore();
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    Accept: 'text/event-stream'
  };
  if (authStore.token) {
    headers.Authorization = `Bearer ${authStore.token}`;
  }

  let response: Response;
  try {
    response = await fetch(resolveApiUrl(path), {
      method: 'POST',
      headers,
      body: JSON.stringify(body),
      signal
    });
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') {
      handlers.onAbort?.();
      return;
    }
    if (signal?.aborted) {
      handlers.onAbort?.();
      return;
    }
    handlers.onError?.({
      code: 'NETWORK_ERROR',
      message: error instanceof Error && error.message ? error.message : '网络异常，请检查连接后重试'
    });
    return;
  }

  const contentType = (response.headers.get('content-type') || '').toLowerCase();

  if (response.status === 401) {
    const rawText = await response.text().catch(() => '');
    const parsed = parseErrorJson(rawText);
    handleAuthExpired(parsed?.message || parsed?.detail || '登录状态失效，请重新登录', handlers);
    return;
  }

  if (!contentType.includes('text/event-stream')) {
    const rawText = await response.text().catch(() => '');
    const parsed = parseErrorJson(rawText);
    if (parsed?.code === CODE_TOKEN_FAIL) {
      handleAuthExpired(parsed.message || '登录状态失效，请重新登录', handlers);
      return;
    }
    const errMsg =
      parsed?.message ||
      parsed?.detail ||
      (!response.ok ? `请求失败 (${response.status})` : '服务未返回流式响应');
    handlers.onError?.({
      code: parsed?.code || `HTTP_${response.status}`,
      message: errMsg
    });
    return;
  }

  if (!response.ok || !response.body) {
    handlers.onError?.({
      code: `HTTP_${response.status}`,
      message: `流式连接失败 (${response.status})`
    });
    return;
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder('utf-8');
  let buffer = '';
  const state = { sawTerminal: false };

  try {
    while (true) {
      const { value, done } = await reader.read();
      if (done) {
        break;
      }
      buffer += decoder.decode(value, { stream: true });
      const frames = buffer.split(/\r?\n\r?\n/);
      buffer = frames.pop() ?? '';
      for (const frame of frames) {
        if (frame.trim()) {
          dispatchSseFrame(frame, handlers, state);
        }
      }
    }

    buffer += decoder.decode();
    if (buffer.trim()) {
      const tailFrames = buffer.split(/\r?\n\r?\n/);
      for (const frame of tailFrames) {
        if (frame.trim()) {
          dispatchSseFrame(frame, handlers, state);
        }
      }
    }

    if (!state.sawTerminal) {
      handlers.onDone?.({ message_id: '', finish_reason: 'stop' });
    }
  } catch (error) {
    if ((error instanceof DOMException && error.name === 'AbortError') || signal?.aborted) {
      handlers.onAbort?.();
      return;
    }
    handlers.onError?.({
      code: 'STREAM_READ_ERROR',
      message: error instanceof Error && error.message ? error.message : '流式响应读取中断'
    });
  } finally {
    reader.releaseLock();
  }
}

export const streamCsChat = (
  sessionId: string,
  payload: CsChatStreamRequest,
  handlers: CsStreamHandlers,
  signal?: AbortSignal
) => {
  return streamCsSse(`/client/cs/sessions/${encodeURIComponent(sessionId)}/chat/stream`, payload, handlers, signal);
};

export const streamCsResume = (
  sessionId: string,
  payload: CsResumeStreamRequest,
  handlers: CsStreamHandlers,
  signal?: AbortSignal
) => {
  return streamCsSse(
    `/client/cs/sessions/${encodeURIComponent(sessionId)}/resume/stream`,
    payload,
    handlers,
    signal
  );
};
