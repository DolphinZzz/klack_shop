<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import {
  ChatDotRound,
  Close,
  Headset,
  Plus,
  Promotion,
  Service,
  Tickets,
  VideoPause
} from '@element-plus/icons-vue';
import {
  createCsSessionApi,
  getCsSessionMessagesApi,
  listCsSessionsApi,
  streamCsChat,
  streamCsResume
} from '@/api/modules/cs';
import type {
  CsCard,
  CsCitation,
  CsMessage,
  CsProductCardItem,
  CsSession,
  CsStreamHandlers
} from '@/api/types/cs';
import { useAuthStore } from '@/stores/modules/auth';
import ChatMessageCards from './ChatMessageCards.vue';
import MyTicketPanel from './MyTicketPanel.vue';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

const drawerOpen = ref(false);
const activeTab = ref<'chat' | 'tickets'>('chat');
const focusTicketId = ref<string | null>(null);

const sessions = ref<CsSession[]>([]);
const currentSessionId = ref<string>('');
const messages = ref<CsMessage[]>([]);
const loadingSessions = ref(false);
const loadingMessages = ref(false);
const creatingSession = ref(false);
const isStreaming = ref(false);
const resumingInterrupt = ref(false);
const draftInput = ref('');
const messagesContainerRef = ref<HTMLElement | null>(null);
const ticketPanelRef = ref<InstanceType<typeof MyTicketPanel> | null>(null);

let activeAbortController: AbortController | null = null;
let sessionMessagesReqSeq = 0;

const isHidden = computed(() => route.path === '/login');

const quickPrompts = [
  '推荐一款适合打游戏的机械键盘',
  '帮我查一下最近一笔订单状态',
  '键盘 7 天无理由退换货怎么算？',
  '申请转人工客服处理'
];

const scrollToBottom = async () => {
  await nextTick();
  const el = messagesContainerRef.value;
  if (el) {
    el.scrollTop = el.scrollHeight;
  }
};

const abortActiveStream = () => {
  if (activeAbortController) {
    activeAbortController.abort();
    activeAbortController = null;
  }
  isStreaming.value = false;
  resumingInterrupt.value = false;
};

const resetWidgetState = () => {
  abortActiveStream();
  sessionMessagesReqSeq += 1;
  loadingSessions.value = false;
  loadingMessages.value = false;
  sessions.value = [];
  currentSessionId.value = '';
  messages.value = [];
  draftInput.value = '';
  focusTicketId.value = null;
  activeTab.value = 'chat';
  ticketPanelRef.value?.reset();
};

const hydrateMessages = (rawMessages: CsMessage[], hasPendingInterrupt: boolean) => {
  let lastInterruptIndex = -1;
  if (hasPendingInterrupt) {
    for (let i = rawMessages.length - 1; i >= 0; i -= 1) {
      if (rawMessages[i].interrupt_payload) {
        lastInterruptIndex = i;
        break;
      }
    }
  }
  return rawMessages.map((msg, idx) => ({
    ...msg,
    cards: Array.isArray(msg.cards) ? msg.cards : [],
    citations: Array.isArray(msg.citations) ? msg.citations : [],
    interruptStatus: msg.interrupt_payload
      ? idx === lastInterruptIndex
        ? ('pending' as const)
        : ('superseded' as const)
      : undefined
  }));
};

const loadSessionMessages = async (sessionId: string) => {
  if (!sessionId) return;
  const reqSeq = ++sessionMessagesReqSeq;
  loadingMessages.value = true;
  try {
    const res = await getCsSessionMessagesApi(sessionId);
    if (reqSeq !== sessionMessagesReqSeq || currentSessionId.value !== sessionId) return;
    const data = res.data;
    if (data) {
      const idx = sessions.value.findIndex(s => s.id === data.session.id);
      if (idx >= 0) {
        sessions.value[idx] = data.session;
      }
      messages.value = hydrateMessages(data.messages || [], Boolean(data.session.has_pending_interrupt));
      await scrollToBottom();
    }
  } catch {
    // http.ts handles toast
  } finally {
    if (reqSeq === sessionMessagesReqSeq) {
      loadingMessages.value = false;
    }
  }
};

const refreshSessionsSilent = async () => {
  try {
    const res = await listCsSessionsApi({ limit: 20 });
    sessions.value = res.data?.items || [];
  } catch {
    // non-fatal background sync
  }
};

const createNewSession = async (): Promise<CsSession | null> => {
  if (creatingSession.value) return null;
  abortActiveStream();
  sessionMessagesReqSeq += 1;
  loadingMessages.value = false;
  creatingSession.value = true;
  try {
    const res = await createCsSessionApi({});
    const created = res.data;
    if (created) {
      sessions.value = [created, ...sessions.value.filter(s => s.id !== created.id)];
      currentSessionId.value = created.id;
      messages.value = [];
      return created;
    }
    return null;
  } catch {
    return null;
  } finally {
    creatingSession.value = false;
  }
};

const initializeChatIfNeeded = async () => {
  if (!authStore.isLogin) return;
  if (sessions.value.length > 0 && currentSessionId.value) return;

  loadingSessions.value = true;
  try {
    const res = await listCsSessionsApi({ limit: 20 });
    const items = res.data?.items || [];
    sessions.value = items;
    if (items.length > 0) {
      currentSessionId.value = items[0].id;
      await loadSessionMessages(items[0].id);
    } else {
      await createNewSession();
    }
  } catch {
    // http.ts handles toast
  } finally {
    loadingSessions.value = false;
  }
};

const handleToggleWidget = () => {
  if (!authStore.isLogin) {
    ElMessage.info('请先登录后使用智能客服');
    void router.push({
      path: '/login',
      query: { redirect: route.fullPath }
    });
    return;
  }
  drawerOpen.value = true;
  void initializeChatIfNeeded();
};

const handleSwitchSession = async (sessionId: string) => {
  if (!sessionId || sessionId === currentSessionId.value) return;
  abortActiveStream();
  currentSessionId.value = sessionId;
  await loadSessionMessages(sessionId);
};

const supersedePendingInterrupts = () => {
  for (const msg of messages.value) {
    if (msg.interrupt_payload && msg.interruptStatus === 'pending') {
      msg.interruptStatus = 'superseded';
    }
  }
};

const createStreamingAssistantMessage = (sessionId: string): CsMessage => {
  const placeholder: CsMessage = {
    id: `temp-ai-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`,
    session_id: sessionId,
    role: 'assistant',
    content: '',
    cards: [],
    citations: [],
    interrupt_payload: null,
    created_at: new Date().toISOString(),
    streaming: true
  };
  messages.value.push(placeholder);
  return messages.value[messages.value.length - 1];
};

const buildStreamHandlers = (
  assistantMsg: CsMessage,
  sessionId: string,
  options?: {
    onTerminalSuccess?: () => void;
    onTerminalError?: (code?: string) => void;
  }
): CsStreamHandlers => ({
  onMeta(meta) {
    if (meta.message_id) {
      assistantMsg.id = meta.message_id;
    }
    if (meta.intent) {
      assistantMsg.intent = meta.intent;
    }
  },
  onToken(token) {
    assistantMsg.content += token.delta || '';
    void scrollToBottom();
  },
  onCard(card: CsCard) {
    if (!assistantMsg.cards) {
      assistantMsg.cards = [];
    }
    if (card.card_type === 'citations') {
      const payload = card.payload as { citations?: CsCitation[] } | undefined;
      if (Array.isArray(payload?.citations)) {
        assistantMsg.citations = [...(assistantMsg.citations || []), ...payload.citations];
      }
    } else {
      assistantMsg.cards.push(card);
    }
    void scrollToBottom();
  },
  onInterrupt(interrupt) {
    assistantMsg.interrupt_payload = interrupt;
    assistantMsg.interruptStatus = 'pending';
    const session = sessions.value.find(s => s.id === sessionId);
    if (session) {
      session.has_pending_interrupt = true;
    }
    void scrollToBottom();
  },
  onDone() {
    assistantMsg.streaming = false;
    isStreaming.value = false;
    activeAbortController = null;
    options?.onTerminalSuccess?.();
    void refreshSessionsSilent();
    void scrollToBottom();
  },
  onError(err) {
    assistantMsg.streaming = false;
    isStreaming.value = false;
    activeAbortController = null;
    if (!assistantMsg.content.trim()) {
      assistantMsg.content = err.message || '服务暂时繁忙，请稍后再试或转人工工单。';
    }
    options?.onTerminalError?.(err.code);
    ElMessage.error(err.message || '客服回复异常');
    void scrollToBottom();
  },
  onAbort() {
    assistantMsg.streaming = false;
    isStreaming.value = false;
    activeAbortController = null;
    if (!assistantMsg.content.trim()) {
      assistantMsg.content = '已停止本次回复生成。';
    }
  },
  onAuthExpired() {
    drawerOpen.value = false;
    resetWidgetState();
    void router.replace({
      path: '/login',
      query: { redirect: route.fullPath }
    });
  }
});

const sendMessage = async (rawText?: string, forceTicket = false) => {
  if (isStreaming.value || resumingInterrupt.value) return;
  const text = (rawText ?? draftInput.value).trim();
  if (!text) return;

  let sessionId = currentSessionId.value;
  if (!sessionId) {
    const created = await createNewSession();
    if (!created) return;
    sessionId = created.id;
  }

  if (rawText === undefined) {
    draftInput.value = '';
  }

  activeTab.value = 'chat';
  supersedePendingInterrupts();

  messages.value.push({
    id: `temp-user-${Date.now()}`,
    session_id: sessionId,
    role: 'user',
    content: text,
    created_at: new Date().toISOString()
  });

  const assistantMsg = createStreamingAssistantMessage(sessionId);
  await scrollToBottom();

  const controller = new AbortController();
  activeAbortController = controller;
  isStreaming.value = true;

  await streamCsChat(
    sessionId,
    { message: text, force_ticket: forceTicket },
    buildStreamHandlers(assistantMsg, sessionId),
    controller.signal
  );
};

const handleHandoffClick = () => {
  if (isStreaming.value || resumingInterrupt.value) return;
  const text = draftInput.value.trim() || '请帮我转接人工客服处理';
  draftInput.value = '';
  void sendMessage(text, true);
};

const syncExpiredInterrupt = (sourceMsg: CsMessage, sessionId: string, code?: string) => {
  if (code === 'UPSTREAM_409' || code === 'HTTP_409') {
    sourceMsg.interruptStatus = 'superseded';
    void loadSessionMessages(sessionId);
  }
};

const handleConfirmOrder = async (sourceMsg: CsMessage, payload: { addressId: number }) => {
  if (resumingInterrupt.value || isStreaming.value || !currentSessionId.value) return;
  resumingInterrupt.value = true;
  const sessionId = currentSessionId.value;

  const assistantMsg = createStreamingAssistantMessage(sessionId);
  await scrollToBottom();

  const controller = new AbortController();
  activeAbortController = controller;
  isStreaming.value = true;

  try {
    await streamCsResume(
      sessionId,
      { action: 'confirm', address_id: payload.addressId },
      buildStreamHandlers(assistantMsg, sessionId, {
        onTerminalSuccess: () => {
          sourceMsg.interruptStatus = 'confirmed';
          const session = sessions.value.find(s => s.id === sessionId);
          if (session) {
            session.has_pending_interrupt = false;
          }
        },
        onTerminalError: code => syncExpiredInterrupt(sourceMsg, sessionId, code)
      }),
      controller.signal
    );
  } finally {
    resumingInterrupt.value = false;
  }
};

const handleCancelOrder = async (sourceMsg: CsMessage) => {
  if (resumingInterrupt.value || isStreaming.value || !currentSessionId.value) return;
  resumingInterrupt.value = true;
  const sessionId = currentSessionId.value;

  const assistantMsg = createStreamingAssistantMessage(sessionId);
  await scrollToBottom();

  const controller = new AbortController();
  activeAbortController = controller;
  isStreaming.value = true;

  try {
    await streamCsResume(
      sessionId,
      { action: 'cancel', reason: 'user_cancelled' },
      buildStreamHandlers(assistantMsg, sessionId, {
        onTerminalSuccess: () => {
          sourceMsg.interruptStatus = 'cancelled';
          const session = sessions.value.find(s => s.id === sessionId);
          if (session) {
            session.has_pending_interrupt = false;
          }
        },
        onTerminalError: code => syncExpiredInterrupt(sourceMsg, sessionId, code)
      }),
      controller.signal
    );
  } finally {
    resumingInterrupt.value = false;
  }
};

const handleQuickBuy = (product: CsProductCardItem) => {
  if (isStreaming.value || resumingInterrupt.value) return;
  void sendMessage(`帮我买这件「${product.product_name}」`);
};

const handleViewProduct = (productId: number) => {
  drawerOpen.value = false;
  void router.push(`/product/${productId}`);
};

const handleGoPayment = (orderNo: string) => {
  drawerOpen.value = false;
  void router.push({ path: '/order_payment', query: { orderNo } });
};

const handleManageAddress = () => {
  drawerOpen.value = false;
  void router.push('/user/account/address');
};

const handleOpenTickets = (ticketId?: string | null) => {
  if (ticketId) {
    focusTicketId.value = ticketId;
  } else {
    focusTicketId.value = null;
    ticketPanelRef.value?.clearDetail();
  }
  activeTab.value = 'tickets';
};

const handleInputKeydown = (event: Event | KeyboardEvent) => {
  const keyEvent = event as KeyboardEvent;
  if (keyEvent.key === 'Enter' && !keyEvent.shiftKey && !keyEvent.isComposing) {
    keyEvent.preventDefault();
    void sendMessage();
  }
};

watch(
  () => authStore.token,
  (newToken, oldToken) => {
    if (!newToken || (oldToken && newToken !== oldToken)) {
      drawerOpen.value = false;
      resetWidgetState();
    }
  }
);

onBeforeUnmount(() => {
  abortActiveStream();
});
</script>

<template>
  <template v-if="!isHidden">
    <!-- 右下角悬浮唤醒键 -->
    <button
      type="button"
      class="kc-cs-fab"
      aria-label="打开咔嗒智能客服"
      @click="handleToggleWidget"
    >
      <span class="kc-cs-fab-led" aria-hidden="true" />
      <el-icon class="kc-cs-fab-icon"><Headset /></el-icon>
      <span class="kc-cs-fab-label">智能客服</span>
    </button>

    <!-- 侧边抽屉 -->
    <el-drawer
      v-model="drawerOpen"
      direction="rtl"
      size="min(460px, 100vw)"
      :with-header="false"
      append-to-body
      class="kc-cs-drawer"
    >
      <div class="kc-cs-shell">
        <!-- 顶部标题与双 Tab -->
        <header class="kc-cs-header">
          <div class="kc-cs-brand-row">
            <div class="kc-cs-brand">
              <span class="kc-cs-keycap" aria-hidden="true">K</span>
              <div>
                <div class="kc-cs-title">咔嗒造物智能客服</div>
                <div class="kc-cs-subtitle kc-mono">KLACKCRAFT STUDIO ASSISTANT</div>
              </div>
            </div>
            <button type="button" class="kc-cs-close-btn" aria-label="关闭客服抽屉" @click="drawerOpen = false">
              <el-icon><Close /></el-icon>
            </button>
          </div>

          <div class="kc-cs-tabs" role="tablist">
            <button
              type="button"
              role="tab"
              :aria-selected="activeTab === 'chat'"
              class="kc-cs-tab"
              :class="{ active: activeTab === 'chat' }"
              @click="activeTab = 'chat'"
            >
              <el-icon><ChatDotRound /></el-icon>
              <span>在线咨询</span>
            </button>
            <button
              type="button"
              role="tab"
              :aria-selected="activeTab === 'tickets'"
              class="kc-cs-tab"
              :class="{ active: activeTab === 'tickets' }"
              @click="activeTab = 'tickets'"
            >
              <el-icon><Tickets /></el-icon>
              <span>我的工单</span>
            </button>
          </div>
        </header>

        <!-- Tab 1: 在线咨询 -->
        <div v-show="activeTab === 'chat'" class="kc-cs-chat-pane">
          <div class="kc-cs-session-bar">
            <el-select
              :model-value="currentSessionId"
              size="small"
              class="kc-cs-session-select"
              :loading="loadingSessions"
              placeholder="选择历史会话"
              @update:model-value="handleSwitchSession"
            >
              <el-option
                v-for="s in sessions"
                :key="s.id"
                :label="s.title || '新会话'"
                :value="s.id"
              />
            </el-select>
            <button
              type="button"
              class="kc-cs-new-session-btn"
              :disabled="creatingSession"
              @click="createNewSession"
            >
              <el-icon><Plus /></el-icon>
              <span>新会话</span>
            </button>
          </div>

          <div ref="messagesContainerRef" v-loading="loadingMessages" class="kc-cs-messages">
            <!-- 首屏空状态与快捷引导 -->
            <div v-if="!loadingMessages && messages.length === 0" class="kc-cs-welcome">
              <div class="kc-cs-welcome-badge kc-mono">ONLINE · READY</div>
              <h3>你好，我是咔嗒外设导购与售后顾问</h3>
              <p>可以帮你挑选键鼠装备、核对实时库存并直接下单，也支持查询订单物流与售后退换政策。</p>
              <div class="kc-cs-quick-chips">
                <button
                  v-for="prompt in quickPrompts"
                  :key="prompt"
                  type="button"
                  class="kc-cs-chip"
                  @click="sendMessage(prompt)"
                >
                  {{ prompt }}
                </button>
              </div>
            </div>

            <!-- 对话消息流 -->
            <div
              v-for="msg in messages"
              :key="msg.id"
              class="kc-cs-msg-row"
              :class="msg.role === 'user' ? 'is-user' : 'is-assistant'"
            >
              <div class="kc-cs-bubble">
                <div v-if="msg.content" class="kc-cs-msg-text">{{ msg.content }}</div>
                <div v-else-if="msg.streaming" class="kc-cs-typing">
                  <span /><span /><span />
                </div>

                <ChatMessageCards
                  v-if="
                    msg.role !== 'user' &&
                    ((msg.cards && msg.cards.length > 0) ||
                      (msg.citations && msg.citations.length > 0) ||
                      msg.interrupt_payload)
                  "
                  :cards="msg.cards"
                  :citations="msg.citations"
                  :interrupt-payload="msg.interrupt_payload"
                  :interrupt-status="msg.interruptStatus"
                  :resuming-interrupt="resumingInterrupt"
                  @view-product="handleViewProduct"
                  @quick-buy="handleQuickBuy"
                  @confirm-order="handleConfirmOrder(msg, $event)"
                  @cancel-order="handleCancelOrder(msg)"
                  @go-payment="handleGoPayment"
                  @open-tickets="handleOpenTickets"
                  @manage-address="handleManageAddress"
                />
              </div>
            </div>
          </div>

          <!-- 底部输入区 -->
          <footer class="kc-cs-composer">
            <el-input
              v-model="draftInput"
              type="textarea"
              :rows="2"
              :maxlength="2000"
              resize="none"
              :disabled="isStreaming || resumingInterrupt"
              placeholder="输入想咨询的型号、订单号或售后问题，Enter 发送..."
              @keydown="handleInputKeydown"
            />
            <div class="kc-cs-composer-actions">
              <button
                type="button"
                class="kc-cs-handoff-btn"
                :disabled="isStreaming || resumingInterrupt"
                @click="handleHandoffClick"
              >
                <el-icon><Service /></el-icon>
                <span>转人工工单</span>
              </button>

              <div class="kc-cs-send-group">
                <el-button
                  v-if="isStreaming"
                  size="small"
                  round
                  type="danger"
                  @click="abortActiveStream"
                >
                  <el-icon><VideoPause /></el-icon>
                  <span style="margin-left: 4px">停止生成</span>
                </el-button>
                <el-button
                  v-else
                  size="small"
                  round
                  type="primary"
                  :disabled="!draftInput.trim() || resumingInterrupt"
                  @click="sendMessage()"
                >
                  <el-icon><Promotion /></el-icon>
                  <span style="margin-left: 4px">发送</span>
                </el-button>
              </div>
            </div>
          </footer>
        </div>

        <!-- Tab 2: 我的工单 -->
        <div v-show="activeTab === 'tickets'" class="kc-cs-tickets-pane">
          <MyTicketPanel
            ref="ticketPanelRef"
            :active="drawerOpen && activeTab === 'tickets'"
            :focus-ticket-id="focusTicketId"
            @consumed-focus="focusTicketId = null"
          />
        </div>
      </div>
    </el-drawer>
  </template>
</template>

<style scoped lang="scss">
.kc-cs-fab {
  position: fixed;
  right: 24px;
  bottom: 28px;
  z-index: 1900;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 11px 16px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  background: var(--kc-carbon);
  color: #ffffff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: var(--kc-shadow-lg);
  transition: transform 140ms ease, background-color 140ms ease;
}

.kc-cs-fab:hover {
  transform: translateY(-2px);
  background: var(--kc-carbon-soft);
}

.kc-cs-fab:active {
  transform: scale(0.97);
}

.kc-cs-fab-led {
  width: 7px;
  height: 7px;
  border-radius: 999px;
  background: var(--kc-accent);
  box-shadow: 0 0 8px rgba(255, 77, 0, 0.8);
}

.kc-cs-fab-icon {
  font-size: 16px;
}

.kc-cs-shell {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--kc-bg);
}

.kc-cs-header {
  background: var(--kc-carbon);
  color: #ffffff;
  padding: 14px 16px 12px;
  flex-shrink: 0;
}

.kc-cs-brand-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.kc-cs-brand {
  display: flex;
  align-items: center;
  gap: 10px;
}

.kc-cs-keycap {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: var(--kc-carbon-soft);
  border: 1px solid var(--kc-carbon-border);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-family: var(--kc-font-mono);
  font-weight: 700;
  font-size: 14px;
  color: var(--kc-accent);
}

.kc-cs-title {
  font-size: 15px;
  font-weight: 700;
  line-height: 1.2;
}

.kc-cs-subtitle {
  margin-top: 2px;
  font-size: 10px;
  letter-spacing: 0.08em;
  color: rgba(255, 255, 255, 0.6);
}

.kc-cs-close-btn {
  width: 30px;
  height: 30px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  background: rgba(255, 255, 255, 0.06);
  color: rgba(255, 255, 255, 0.85);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.kc-cs-close-btn:hover {
  background: rgba(255, 255, 255, 0.16);
}

.kc-cs-tabs {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  margin-top: 12px;
}

.kc-cs-tab {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 7px 10px;
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(255, 255, 255, 0.05);
  color: rgba(255, 255, 255, 0.72);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 140ms ease;
}

.kc-cs-tab.active {
  background: var(--kc-accent);
  border-color: var(--kc-accent);
  color: #ffffff;
}

.kc-cs-chat-pane,
.kc-cs-tickets-pane {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.kc-cs-session-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: var(--kc-surface);
  border-bottom: 1px solid var(--kc-divider);
  flex-shrink: 0;
}

.kc-cs-session-select {
  flex: 1;
  min-width: 0;
}

.kc-cs-new-session-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 5px 11px;
  border-radius: 999px;
  border: 1px solid var(--kc-border);
  background: var(--kc-surface-muted);
  color: var(--kc-text);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  flex-shrink: 0;
}

.kc-cs-new-session-btn:hover:not(:disabled) {
  background: var(--kc-bg-subtle);
}

.kc-cs-messages {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.kc-cs-welcome {
  background: var(--kc-surface);
  border: 1px solid var(--kc-border);
  border-radius: var(--kc-radius-md);
  padding: 16px;
  box-shadow: var(--kc-shadow-sm);
}

.kc-cs-welcome-badge {
  display: inline-block;
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.08em;
  color: var(--kc-accent);
  background: var(--kc-accent-soft);
  padding: 2px 8px;
  border-radius: 999px;
  margin-bottom: 8px;
}

.kc-cs-welcome h3 {
  margin: 0 0 6px;
  font-size: 15px;
  font-weight: 700;
  color: var(--kc-text);
}

.kc-cs-welcome p {
  margin: 0 0 12px;
  font-size: 12px;
  line-height: 1.55;
  color: var(--kc-text-secondary);
}

.kc-cs-quick-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
}

.kc-cs-chip {
  text-align: left;
  border: 1px solid var(--kc-border);
  background: var(--kc-surface-muted);
  color: var(--kc-text);
  font-size: 12px;
  padding: 6px 10px;
  border-radius: 999px;
  cursor: pointer;
  transition: border-color 140ms ease, background-color 140ms ease;
}

.kc-cs-chip:hover {
  border-color: var(--kc-accent-border);
  background: var(--kc-accent-soft);
  color: var(--kc-accent);
}

.kc-cs-msg-row {
  display: flex;
}

.kc-cs-msg-row.is-user {
  justify-content: flex-end;
}

.kc-cs-msg-row.is-assistant {
  justify-content: flex-start;
}

.kc-cs-bubble {
  max-width: 92%;
  border-radius: 14px;
  padding: 10px 12px;
  font-size: 13px;
  line-height: 1.6;
  word-break: break-word;
}

.is-user .kc-cs-bubble {
  background: var(--kc-carbon);
  color: #ffffff;
  border-bottom-right-radius: 4px;
}

.is-assistant .kc-cs-bubble {
  background: var(--kc-surface);
  color: var(--kc-text);
  border: 1px solid var(--kc-border);
  border-bottom-left-radius: 4px;
  box-shadow: var(--kc-shadow-sm);
}

.kc-cs-msg-text {
  white-space: pre-wrap;
}

.kc-cs-typing {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 2px;
}

.kc-cs-typing span {
  width: 6px;
  height: 6px;
  border-radius: 999px;
  background: var(--kc-text-muted);
  animation: kc-cs-bounce 1s infinite ease-in-out;
}

.kc-cs-typing span:nth-child(2) {
  animation-delay: 0.16s;
}

.kc-cs-typing span:nth-child(3) {
  animation-delay: 0.32s;
}

@keyframes kc-cs-bounce {
  0%,
  80%,
  100% {
    opacity: 0.35;
    transform: translateY(0);
  }
  40% {
    opacity: 1;
    transform: translateY(-3px);
  }
}

.kc-cs-composer {
  padding: 10px 14px 12px;
  background: var(--kc-surface);
  border-top: 1px solid var(--kc-divider);
  flex-shrink: 0;
}

.kc-cs-composer-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
}

.kc-cs-handoff-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  border: 1px solid var(--kc-border);
  background: var(--kc-surface-muted);
  color: var(--kc-text-secondary);
  font-size: 12px;
  font-weight: 600;
  padding: 5px 10px;
  border-radius: 999px;
  cursor: pointer;
}

.kc-cs-handoff-btn:hover:not(:disabled) {
  color: var(--kc-accent);
  border-color: var(--kc-accent-border);
  background: var(--kc-accent-soft);
}

.kc-cs-handoff-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

@media (max-width: 640px) {
  .kc-cs-fab {
    right: 16px;
    bottom: 20px;
    padding: 10px 14px;
  }
}
</style>

<style>
.kc-cs-drawer .el-drawer__body {
  padding: 0 !important;
  overflow: hidden;
}
</style>
