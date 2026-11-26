export type CsSessionStatus = 'ACTIVE' | 'ARCHIVED' | string;

export interface CsSession {
  id: string;
  user_id: number;
  title: string | null;
  status: CsSessionStatus;
  has_pending_interrupt: boolean;
  last_intent: string | null;
  last_message_at: string | null;
  created_at: string;
  updated_at: string;
}

export interface CsSessionListResponse {
  items: CsSession[];
}

export interface CreateCsSessionParams {
  title?: string;
}

export interface CsProductCardItem {
  id: number | null;
  product_name: string;
  product_info?: string;
  image: string;
  cate_id?: number | null;
  price: string | null;
  ot_price?: string | null;
  stock: number;
  sales: number;
  browse?: number;
  is_hot?: number;
  is_best?: number;
  is_new?: number;
  is_seckill?: number;
  is_postage?: number;
  postage?: string;
  unit_name?: string;
}

export interface CsProductListCardPayload {
  products: CsProductCardItem[];
}

export interface CsAddressOption {
  id: number | null;
  real_name: string;
  phone: string;
  province?: string;
  city?: string;
  district?: string;
  detail?: string;
  full_address: string;
  post_code?: string;
  is_default: number;
}

export interface CsOrderDraft {
  draft_id: string;
  product_id: number;
  product_name: string;
  product_image: string;
  sku_id: number;
  sku_name: string;
  unit_price: string;
  quantity: number;
  freight_price: string;
  total_pay_price: string;
  address_id: number | null;
  receiver_name: string | null;
  receiver_phone: string | null;
  receiver_address: string | null;
  available_addresses: CsAddressOption[];
}

export interface CsInterruptEnvelope {
  interrupt_type: 'order_confirm' | string;
  session_id: string;
  payload: CsOrderDraft;
}

export interface CsOrderCreatedPayload {
  order_no: string;
  product_id?: number | null;
  product_name?: string;
  product_image?: string;
  sku_id?: number | null;
  sku_name?: string;
  quantity?: number;
  pay_price: string;
  remaining_seconds: number;
  payment_url: string;
  paid?: number;
  status?: number;
}

export interface CsTicketCreatedPayload {
  id: string | null;
  ticket_no: string;
  category: string;
  title: string;
  summary: string | null;
  related_order_no: string | null;
  status: string;
  action_url?: string;
  tab?: string;
}

export interface CsCitation {
  chunk_id: string;
  doc_id: string;
  doc_title: string;
  category: string;
  section_path: string;
  page_no: number;
  excerpt?: string;
  content?: string;
  score: number;
}

export interface CsCitationsCardPayload {
  citations: CsCitation[];
}

export type CsCard =
  | {
      card_type: 'product_list';
      payload: CsProductListCardPayload;
    }
  | {
      card_type: 'order_created';
      payload: CsOrderCreatedPayload;
    }
  | {
      card_type: 'ticket_created';
      payload: CsTicketCreatedPayload;
    }
  | {
      card_type: 'citations';
      payload: CsCitationsCardPayload;
    }
  | {
      card_type: string;
      payload: Record<string, unknown>;
    };

export type CsInterruptStatus = 'pending' | 'confirmed' | 'cancelled' | 'superseded';

export interface CsMessage {
  id: string;
  session_id: string;
  role: 'user' | 'assistant' | string;
  content: string;
  intent?: string | null;
  cards?: CsCard[] | null;
  citations?: CsCitation[] | null;
  interrupt_payload?: CsInterruptEnvelope | Record<string, unknown> | null;
  created_at: string;
  streaming?: boolean;
  interruptStatus?: CsInterruptStatus;
}

export interface CsSessionMessagesResponse {
  session: CsSession;
  messages: CsMessage[];
  pending_interrupt: CsInterruptEnvelope | Record<string, unknown> | null;
}

export interface CsChatStreamRequest {
  message: string;
  force_ticket?: boolean;
}

export interface CsResumeStreamRequest {
  action: 'confirm' | 'cancel';
  address_id?: number;
  reason?: string;
}

export interface CsSseMetaEvent {
  session_id: string;
  message_id: string;
  intent: string;
}

export interface CsSseTokenEvent {
  delta: string;
}

export type CsSseCardEvent = CsCard;

export type CsSseInterruptEvent = CsInterruptEnvelope;

export interface CsSseDoneEvent {
  message_id: string;
  finish_reason: 'stop' | 'interrupted' | string;
}

export interface CsSseErrorEvent {
  code?: string;
  message: string;
}

export interface CsStreamHandlers {
  onMeta?: (payload: CsSseMetaEvent) => void;
  onToken?: (payload: CsSseTokenEvent) => void;
  onCard?: (payload: CsSseCardEvent) => void;
  onInterrupt?: (payload: CsSseInterruptEvent) => void;
  onDone?: (payload: CsSseDoneEvent) => void;
  onError?: (payload: CsSseErrorEvent) => void;
  onAbort?: () => void;
  onAuthExpired?: (message: string) => void;
}

export type CsTicketStatus = 'OPEN' | 'REPLIED' | 'CLOSED' | string;

export type CsTicketCategory =
  | 'order_issue'
  | 'after_sale'
  | 'product_consult'
  | 'complaint'
  | 'other'
  | string;

export interface CsTicketReply {
  id: string;
  ticket_id: string;
  sender_type: 'USER' | 'ADMIN' | 'SYSTEM' | string;
  sender_id: number;
  sender_name: string | null;
  content: string;
  created_at: string;
}

export interface CsTicketSummary {
  id: string;
  ticket_no: string;
  user_id: number;
  session_id: string | null;
  category: CsTicketCategory;
  title: string;
  summary: string | null;
  related_order_no: string | null;
  status: CsTicketStatus;
  last_reply_at: string | null;
  closed_at: string | null;
  created_at: string;
  updated_at: string;
}

export interface CsTicketDetail extends CsTicketSummary {
  replies: CsTicketReply[];
}

export interface CsTicketPageQuery {
  status?: string;
  page?: number;
  limit?: number;
}

export interface CsTicketPageResponse {
  current: number;
  limit: number;
  total: number;
  total_page: number;
  items: CsTicketSummary[];
}

export interface CsTicketReplyRequest {
  content: string;
}
