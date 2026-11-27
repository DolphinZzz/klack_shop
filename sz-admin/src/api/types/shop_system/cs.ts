import type { IPageQuery } from '@/api/types';

export type CsTicketStatus = 'OPEN' | 'REPLIED' | 'CLOSED';

export type CsTicketCategory = 'order_issue' | 'after_sale' | 'product_consult' | 'complaint' | 'other';

export interface CsTicketQuery extends IPageQuery {
  status?: CsTicketStatus | '';
  category?: CsTicketCategory | '';
  related_order_no?: string;
  keyword?: string;
}

export interface CsTicketSummary {
  id: string;
  ticket_no: string;
  user_id: number;
  session_id?: string | null;
  category: CsTicketCategory | string;
  title: string;
  summary?: string | null;
  related_order_no?: string | null;
  status: CsTicketStatus | string;
  last_reply_at?: string | null;
  closed_at?: string | null;
  created_at: string;
  updated_at: string;
}

export interface CsTicketReply {
  id: string;
  ticket_id: string;
  sender_type: 'USER' | 'ADMIN' | string;
  sender_id: number;
  sender_name?: string | null;
  content: string;
  created_at: string;
}

export interface CsSessionMessage {
  id: string;
  session_id: string;
  role: 'user' | 'assistant' | 'system' | string;
  content: string;
  intent?: string | null;
  cards?: Record<string, unknown>[] | null;
  citations?: Record<string, unknown>[] | null;
  interrupt_payload?: Record<string, unknown> | null;
  created_at: string;
}

export interface CsTicketDetail extends CsTicketSummary {
  replies: CsTicketReply[];
}

export interface CsAdminTicketDetail extends CsTicketDetail {
  session_messages: CsSessionMessage[];
}

export interface CsTicketPageRaw {
  current: number;
  limit: number;
  total: number;
  total_page: number;
  items: CsTicketSummary[];
}

export interface CsAdminTicketReplyDTO {
  content: string;
  close_ticket?: boolean;
}

export interface CsAdminTicketCloseDTO {
  reason?: string;
}

export type KbDocumentStatus = 'PENDING' | 'PARSING' | 'INDEXED' | 'FAILED';

export type KbDocumentCategory = 'consumer_law' | 'mall_policy' | 'faq' | 'product_guide';

export interface KbDocumentQuery extends IPageQuery {
  status?: KbDocumentStatus | '';
  category?: KbDocumentCategory | '';
}

export interface KbDocumentRow {
  id: string;
  title: string;
  category: KbDocumentCategory | string;
  file_name?: string | null;
  file_path?: string | null;
  file_sha256?: string | null;
  file_size?: number | null;
  mineru_mode?: string | null;
  status: KbDocumentStatus | string;
  chunk_count: number;
  error_msg?: string | null;
  uploaded_by?: number | null;
  created_at: string;
  updated_at: string;
}

export interface KbDocumentPageRaw {
  current: number;
  limit: number;
  total: number;
  total_page: number;
  items: KbDocumentRow[];
}

export interface KbUploadDTO {
  file: File;
  category: KbDocumentCategory;
  title?: string;
}

export interface KbReindexDTO {
  use_cached_markdown?: boolean;
}

export interface KbDeleteResult {
  deleted: boolean;
  id: string;
}
