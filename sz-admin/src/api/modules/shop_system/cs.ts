import http from '@/api';
import { ADMIN_MODULE } from '@/api/helper/prefix';
import type { IPage, IResultData } from '@/api/types';
import type {
  CsAdminTicketCloseDTO,
  CsAdminTicketDetail,
  CsAdminTicketReplyDTO,
  CsTicketDetail,
  CsTicketPageRaw,
  CsTicketQuery,
  CsTicketSummary,
  KbDeleteResult,
  KbDocumentPageRaw,
  KbDocumentQuery,
  KbDocumentRow,
  KbReindexDTO,
  KbUploadDTO
} from '@/api/types/shop_system/cs';

export interface CsOptionItem<T extends string = string> {
  label: string;
  value: T;
  tagType: 'primary' | 'success' | 'warning' | 'info' | 'danger';
}

export const CS_TICKET_STATUS_OPTIONS: CsOptionItem[] = [
  { label: '待处理', value: 'OPEN', tagType: 'warning' },
  { label: '已回复', value: 'REPLIED', tagType: 'primary' },
  { label: '已关闭', value: 'CLOSED', tagType: 'info' }
];

export const CS_TICKET_CATEGORY_OPTIONS: CsOptionItem[] = [
  { label: '订单问题', value: 'order_issue', tagType: 'primary' },
  { label: '售后咨询', value: 'after_sale', tagType: 'warning' },
  { label: '商品咨询', value: 'product_consult', tagType: 'success' },
  { label: '投诉建议', value: 'complaint', tagType: 'danger' },
  { label: '其他', value: 'other', tagType: 'info' }
];

export const KB_DOCUMENT_STATUS_OPTIONS: CsOptionItem[] = [
  { label: '待解析', value: 'PENDING', tagType: 'info' },
  { label: '解析中', value: 'PARSING', tagType: 'warning' },
  { label: '已入库', value: 'INDEXED', tagType: 'success' },
  { label: '解析失败', value: 'FAILED', tagType: 'danger' }
];

export const KB_DOCUMENT_CATEGORY_OPTIONS: CsOptionItem[] = [
  { label: '消费者权益法', value: 'consumer_law', tagType: 'danger' },
  { label: '商城规则', value: 'mall_policy', tagType: 'primary' },
  { label: '常见问题 FAQ', value: 'faq', tagType: 'success' },
  { label: '商品指南', value: 'product_guide', tagType: 'warning' }
];

export const findCsOption = (options: CsOptionItem[], value?: string | null): CsOptionItem | undefined => {
  if (!value) return undefined;
  return options.find(item => item.value === value);
};

export const formatCsDateTime = (value?: string | null): string => {
  if (!value) return '-';
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return value.replace('T', ' ').replace(/\.\d+Z?$/, '');
  }
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${parsed.getFullYear()}-${pad(parsed.getMonth() + 1)}-${pad(parsed.getDate())} ${pad(parsed.getHours())}:${pad(parsed.getMinutes())}:${pad(parsed.getSeconds())}`;
};

export const formatCsFileSize = (bytes?: number | null): string => {
  if (bytes === null || bytes === undefined || Number.isNaN(Number(bytes))) return '-';
  const size = Number(bytes);
  if (size < 1024) return `${size} B`;
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
  return `${(size / (1024 * 1024)).toFixed(2)} MB`;
};

const extractValidationDetailMessage = (detail: unknown): string | undefined => {
  if (typeof detail === 'string' && detail.trim()) {
    return detail.trim();
  }
  if (Array.isArray(detail)) {
    for (const entry of detail) {
      if (entry && typeof entry === 'object' && 'msg' in entry) {
        const msg = (entry as { msg?: unknown }).msg;
        if (typeof msg === 'string' && msg.trim()) {
          return msg.trim();
        }
      }
    }
  }
  return undefined;
};

export const extractApiErrorMessage = (error: unknown, fallback = '操作失败，请稍后重试'): string => {
  if (error && typeof error === 'object') {
    const errObj = error as {
      response?: { data?: { detail?: unknown; message?: unknown } };
      detail?: unknown;
      message?: unknown;
    };
    const detailMsg = extractValidationDetailMessage(errObj.response?.data?.detail ?? errObj.detail);
    if (detailMsg) return detailMsg;
    const responseMsg = errObj.response?.data?.message;
    if (typeof responseMsg === 'string' && responseMsg.trim()) return responseMsg.trim();
    const rawMsg = errObj.message;
    if (typeof rawMsg === 'string' && rawMsg.trim() && !rawMsg.startsWith('Request failed with status code')) {
      return rawMsg.trim();
    }
  }
  return fallback;
};

/**
 * 兼容后端透传原始 FastAPI JSON（无 code/data 包装）与标准 IResultData 两种返回形态
 */
function unwrapRaw<T>(res: unknown): T {
  if (res !== null && typeof res === 'object' && 'code' in res && 'data' in res) {
    return (res as IResultData<T>).data;
  }
  return res as T;
}

interface RawPageShape<T> {
  current?: number;
  limit?: number;
  total?: number;
  total_page?: number;
  totalPage?: number;
  items?: T[];
  rows?: T[];
}

function toProTablePage<T>(res: unknown): IResultData<IPage<T>> {
  const raw = unwrapRaw<RawPageShape<T>>(res) || {};
  const rows = Array.isArray(raw.items) ? raw.items : Array.isArray(raw.rows) ? raw.rows : [];
  const current = Number(raw.current ?? 1);
  const limit = Number(raw.limit ?? 20);
  const total = Number(raw.total ?? rows.length);
  const totalPage = Number(raw.total_page ?? raw.totalPage ?? (limit > 0 ? Math.ceil(total / limit) : 0));

  return {
    code: '0000',
    message: 'OK',
    data: {
      current,
      limit,
      total,
      totalPage,
      rows
    }
  };
}

function compactQueryParams<T extends object>(params: T): Record<string, unknown> {
  const cleaned: Record<string, unknown> = {};
  for (const [key, value] of Object.entries(params)) {
    if (typeof value === 'string') {
      const trimmed = value.trim();
      if (trimmed !== '') {
        cleaned[key] = trimmed;
      }
    } else if (value !== undefined && value !== null) {
      cleaned[key] = value;
    }
  }
  return cleaned;
}

export const getCsTicketListApi = async (params: CsTicketQuery): Promise<IResultData<IPage<CsTicketSummary>>> => {
  const res = await http.get<CsTicketPageRaw>(ADMIN_MODULE + '/admin/cs/tickets', compactQueryParams(params));
  return toProTablePage<CsTicketSummary>(res);
};

export const getCsTicketDetailApi = async (ticketId: string): Promise<CsAdminTicketDetail> => {
  const res = await http.get<CsAdminTicketDetail>(ADMIN_MODULE + `/admin/cs/tickets/${encodeURIComponent(ticketId)}`);
  return unwrapRaw<CsAdminTicketDetail>(res);
};

export const replyCsTicketApi = async (ticketId: string, payload: CsAdminTicketReplyDTO): Promise<CsTicketDetail> => {
  const res = await http.post<CsTicketDetail>(
    ADMIN_MODULE + `/admin/cs/tickets/${encodeURIComponent(ticketId)}/reply`,
    payload
  );
  return unwrapRaw<CsTicketDetail>(res);
};

export const closeCsTicketApi = async (ticketId: string, payload?: CsAdminTicketCloseDTO): Promise<CsTicketDetail> => {
  const res = await http.post<CsTicketDetail>(
    ADMIN_MODULE + `/admin/cs/tickets/${encodeURIComponent(ticketId)}/close`,
    payload ?? {}
  );
  return unwrapRaw<CsTicketDetail>(res);
};

export const getKbDocumentListApi = async (params: KbDocumentQuery): Promise<IResultData<IPage<KbDocumentRow>>> => {
  const res = await http.get<KbDocumentPageRaw>(ADMIN_MODULE + '/admin/cs/kb/documents', compactQueryParams(params));
  return toProTablePage<KbDocumentRow>(res);
};

export const uploadKbDocumentApi = async (params: KbUploadDTO): Promise<KbDocumentRow> => {
  const formData = new FormData();
  formData.append('file', params.file);
  formData.append('category', params.category);
  const trimmedTitle = params.title?.trim();
  if (trimmedTitle) {
    formData.append('title', trimmedTitle);
  }
  const res = await http.upload<KbDocumentRow>(ADMIN_MODULE + '/admin/cs/kb/documents/upload', formData);
  return unwrapRaw<KbDocumentRow>(res);
};

export const reindexKbDocumentApi = async (docId: string, payload?: KbReindexDTO): Promise<KbDocumentRow> => {
  const res = await http.post<KbDocumentRow>(
    ADMIN_MODULE + `/admin/cs/kb/documents/${encodeURIComponent(docId)}/reindex`,
    payload ?? { use_cached_markdown: false }
  );
  return unwrapRaw<KbDocumentRow>(res);
};

export const deleteKbDocumentApi = async (docId: string): Promise<KbDeleteResult> => {
  const res = await http.delete<KbDeleteResult>(ADMIN_MODULE + `/admin/cs/kb/documents/${encodeURIComponent(docId)}`);
  return unwrapRaw<KbDeleteResult>(res);
};
