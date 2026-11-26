<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { ArrowLeft, Refresh, Tickets } from '@element-plus/icons-vue';
import { getCsTicketDetailApi, listCsTicketsApi, replyCsTicketApi } from '@/api/modules/cs';
import type { CsTicketDetail, CsTicketSummary } from '@/api/types/cs';

const props = withDefaults(
  defineProps<{
    active: boolean;
    focusTicketId?: string | null;
  }>(),
  {
    focusTicketId: null
  }
);

const emit = defineEmits<{
  (e: 'consumed-focus'): void;
}>();

const loadingList = ref(false);
const loadingDetail = ref(false);
const submittingReply = ref(false);
const statusFilter = ref<string>('');
const tickets = ref<CsTicketSummary[]>([]);
const selectedDetail = ref<CsTicketDetail | null>(null);
const replyContent = ref('');

let listReqSeq = 0;
let detailReqSeq = 0;

const statusTabs = [
  { label: '全部', value: '' },
  { label: '待处理', value: 'OPEN' },
  { label: '已回复', value: 'REPLIED' },
  { label: '已关闭', value: 'CLOSED' }
];

const statusLabel = (status?: string) => {
  switch (status) {
    case 'OPEN':
      return '待处理';
    case 'REPLIED':
      return '已回复';
    case 'CLOSED':
      return '已关闭';
    default:
      return status || '未知';
  }
};

const categoryLabel = (category?: string) => {
  switch (category) {
    case 'order_issue':
      return '订单问题';
    case 'after_sale':
      return '售后退款';
    case 'product_consult':
      return '商品咨询';
    case 'complaint':
      return '服务投诉';
    default:
      return '其他咨询';
  }
};

const formatTime = (value?: string | null) => {
  if (!value) return '--';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
};

const canReply = computed(() => {
  const status = selectedDetail.value?.status;
  return status === 'OPEN' || status === 'REPLIED';
});

const fetchTickets = async () => {
  const reqSeq = ++listReqSeq;
  loadingList.value = true;
  try {
    const res = await listCsTicketsApi({
      status: statusFilter.value || undefined,
      page: 1,
      limit: 30
    });
    if (reqSeq !== listReqSeq) return;
    tickets.value = res.data?.items || [];
  } catch {
    // http.ts already handles error toast
  } finally {
    if (reqSeq === listReqSeq) {
      loadingList.value = false;
    }
  }
};

const openTicketDetail = async (ticketId: string) => {
  if (!ticketId) return;
  const reqSeq = ++detailReqSeq;
  loadingDetail.value = true;
  try {
    const res = await getCsTicketDetailApi(ticketId);
    if (reqSeq !== detailReqSeq) return;
    selectedDetail.value = res.data;
  } catch {
    // http.ts handles error toast
  } finally {
    if (reqSeq === detailReqSeq) {
      loadingDetail.value = false;
    }
  }
};

const clearDetail = () => {
  detailReqSeq += 1;
  loadingDetail.value = false;
  selectedDetail.value = null;
  replyContent.value = '';
};

const reset = () => {
  listReqSeq += 1;
  detailReqSeq += 1;
  loadingList.value = false;
  loadingDetail.value = false;
  submittingReply.value = false;
  statusFilter.value = '';
  tickets.value = [];
  selectedDetail.value = null;
  replyContent.value = '';
};

defineExpose({ reset, clearDetail });

const handleSelectStatus = (val: string) => {
  statusFilter.value = val;
  void fetchTickets();
};

const submitFollowup = async () => {
  const detail = selectedDetail.value;
  const trimmed = replyContent.value.trim();
  if (!detail || !trimmed || submittingReply.value) return;

  const targetTicketId = detail.id;
  submittingReply.value = true;
  try {
    const res = await replyCsTicketApi(targetTicketId, { content: trimmed });
    if (selectedDetail.value?.id === targetTicketId) {
      detailReqSeq += 1;
      selectedDetail.value = res.data;
      replyContent.value = '';
    }
    ElMessage.success('追问已提交');
    void fetchTickets();
  } catch {
    // http.ts handles error toast
  } finally {
    submittingReply.value = false;
  }
};

watch(
  () => props.active,
  isActive => {
    if (isActive) {
      void fetchTickets();
      if (props.focusTicketId) {
        void openTicketDetail(props.focusTicketId);
        emit('consumed-focus');
      }
    }
  },
  { immediate: true }
);

watch(
  () => props.focusTicketId,
  ticketId => {
    if (props.active && ticketId) {
      void openTicketDetail(ticketId);
      emit('consumed-focus');
    }
  }
);
</script>

<template>
  <div class="cs-ticket-panel">
    <!-- 工单详情视图 -->
    <template v-if="selectedDetail">
      <div class="cs-detail-topbar">
        <button type="button" class="cs-back-btn" @click="clearDetail">
          <el-icon><ArrowLeft /></el-icon>
          <span>返回列表</span>
        </button>
        <button
          type="button"
          class="cs-refresh-btn"
          :disabled="loadingDetail"
          @click="openTicketDetail(selectedDetail.id)"
        >
          <el-icon><Refresh /></el-icon>
          <span>刷新</span>
        </button>
      </div>

      <div v-loading="loadingDetail" class="cs-detail-scroll">
        <section class="cs-detail-card">
          <div class="cs-detail-meta-row">
            <span class="kc-mono cs-ticket-no">{{ selectedDetail.ticket_no }}</span>
            <span class="cs-status-badge" :class="`status-${selectedDetail.status}`">
              {{ statusLabel(selectedDetail.status) }}
            </span>
          </div>
          <h4 class="cs-detail-title">{{ selectedDetail.title }}</h4>
          <div class="cs-detail-tags">
            <span class="cs-cat-pill">{{ categoryLabel(selectedDetail.category) }}</span>
            <span v-if="selectedDetail.related_order_no" class="kc-mono cs-order-ref">
              订单 {{ selectedDetail.related_order_no }}
            </span>
            <span class="cs-time-muted">{{ formatTime(selectedDetail.created_at) }}</span>
          </div>
          <p v-if="selectedDetail.summary" class="cs-detail-summary">{{ selectedDetail.summary }}</p>
        </section>

        <section class="cs-replies-section">
          <div class="cs-section-heading">沟通时间轴</div>
          <div v-if="!selectedDetail.replies?.length" class="cs-empty-replies">
            工单已进入人工客服处理队列，请耐心等待专员回复，您也可在下方补充说明。
          </div>
          <div v-else class="cs-timeline">
            <article
              v-for="reply in selectedDetail.replies"
              :key="reply.id"
              class="cs-reply-item"
              :class="`sender-${reply.sender_type}`"
            >
              <div class="cs-reply-header">
                <span class="cs-sender-badge">
                  {{
                    reply.sender_type === 'USER'
                      ? '我'
                      : reply.sender_type === 'ADMIN'
                        ? reply.sender_name || '售后专员'
                        : '系统通知'
                  }}
                </span>
                <span class="cs-reply-time kc-mono">{{ formatTime(reply.created_at) }}</span>
              </div>
              <div class="cs-reply-content">{{ reply.content }}</div>
            </article>
          </div>
        </section>
      </div>

      <div v-if="canReply" class="cs-followup-composer">
        <el-input
          v-model="replyContent"
          type="textarea"
          :rows="2"
          :maxlength="1000"
          resize="none"
          placeholder="补充说明或继续追问（Enter + Ctrl/Cmd 发送）..."
          @keydown.ctrl.enter.prevent="submitFollowup"
          @keydown.meta.enter.prevent="submitFollowup"
        />
        <div class="cs-followup-actions">
          <span class="cs-followup-hint">提交后将通知售后专员跟进</span>
          <el-button
            type="primary"
            size="small"
            round
            :loading="submittingReply"
            :disabled="!replyContent.trim() || submittingReply"
            @click="submitFollowup"
          >
            发送追问
          </el-button>
        </div>
      </div>
      <div v-else class="cs-closed-banner">
        该工单已关闭，如需继续咨询请在「在线咨询」发起新对话。
      </div>
    </template>

    <!-- 工单列表视图 -->
    <template v-else>
      <div class="cs-filter-bar">
        <div class="cs-filter-chips">
          <button
            v-for="tab in statusTabs"
            :key="tab.value"
            type="button"
            class="cs-filter-chip"
            :class="{ active: statusFilter === tab.value }"
            @click="handleSelectStatus(tab.value)"
          >
            {{ tab.label }}
          </button>
        </div>
        <button type="button" class="cs-refresh-btn" :disabled="loadingList" @click="fetchTickets">
          <el-icon><Refresh /></el-icon>
        </button>
      </div>

      <div v-loading="loadingList" class="cs-ticket-list-scroll">
        <div v-if="!loadingList && tickets.length === 0" class="cs-empty-tickets">
          <el-icon class="cs-empty-icon"><Tickets /></el-icon>
          <p>暂无符合条件的服务工单</p>
          <span>在「在线咨询」遇到复杂售后问题时可随时转接人工工单</span>
        </div>

        <button
          v-for="item in tickets"
          :key="item.id"
          type="button"
          class="cs-ticket-row"
          @click="openTicketDetail(item.id)"
        >
          <div class="cs-ticket-row-top">
            <span class="kc-mono cs-ticket-no">{{ item.ticket_no }}</span>
            <span class="cs-status-badge" :class="`status-${item.status}`">
              {{ statusLabel(item.status) }}
            </span>
          </div>
          <div class="cs-ticket-row-title">{{ item.title }}</div>
          <div class="cs-ticket-row-meta">
            <span class="cs-cat-pill">{{ categoryLabel(item.category) }}</span>
            <span v-if="item.related_order_no" class="kc-mono">订单 {{ item.related_order_no }}</span>
            <span class="cs-time-muted kc-mono">{{ formatTime(item.last_reply_at || item.created_at) }}</span>
          </div>
        </button>
      </div>
    </template>
  </div>
</template>

<style scoped lang="scss">
.cs-ticket-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
}

.cs-filter-bar,
.cs-detail-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 10px 14px;
  border-bottom: 1px solid var(--kc-divider);
  background: var(--kc-surface);
  flex-shrink: 0;
}

.cs-filter-chips {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.cs-filter-chip {
  border: 1px solid var(--kc-border);
  background: var(--kc-surface-muted);
  color: var(--kc-text-secondary);
  font-size: 12px;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: 999px;
  cursor: pointer;
  transition: all 140ms ease;
}

.cs-filter-chip.active {
  background: var(--kc-carbon);
  color: #ffffff;
  border-color: var(--kc-carbon);
}

.cs-back-btn,
.cs-refresh-btn {
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

.cs-back-btn:hover,
.cs-refresh-btn:hover:not(:disabled) {
  color: var(--kc-text);
  background: var(--kc-bg-subtle);
}

.cs-ticket-list-scroll,
.cs-detail-scroll {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.cs-empty-tickets {
  margin: auto 0;
  text-align: center;
  padding: 36px 16px;
  color: var(--kc-text-muted);
}

.cs-empty-icon {
  font-size: 28px;
  color: var(--kc-text-muted);
  margin-bottom: 8px;
}

.cs-empty-tickets p {
  margin: 0 0 4px;
  font-size: 14px;
  font-weight: 600;
  color: var(--kc-text-secondary);
}

.cs-empty-tickets span {
  font-size: 12px;
}

.cs-ticket-row {
  text-align: left;
  width: 100%;
  background: var(--kc-surface);
  border: 1px solid var(--kc-border);
  border-radius: var(--kc-radius-sm);
  padding: 12px;
  cursor: pointer;
  transition: border-color 140ms ease, box-shadow 140ms ease;
}

.cs-ticket-row:hover {
  border-color: var(--kc-border-strong);
  box-shadow: var(--kc-shadow-sm);
}

.cs-ticket-row-top,
.cs-detail-meta-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.cs-ticket-no {
  font-size: 11px;
  color: var(--kc-text-muted);
}

.cs-status-badge {
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 999px;
}

.status-OPEN {
  background: var(--kc-accent-soft);
  color: var(--kc-accent);
}

.status-REPLIED {
  background: rgba(22, 163, 74, 0.12);
  color: var(--kc-success);
}

.status-CLOSED {
  background: var(--kc-surface-muted);
  color: var(--kc-text-muted);
}

.cs-ticket-row-title {
  margin: 6px 0;
  font-size: 13px;
  font-weight: 600;
  color: var(--kc-text);
  line-height: 1.4;
}

.cs-ticket-row-meta,
.cs-detail-tags {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  font-size: 11px;
  color: var(--kc-text-muted);
}

.cs-cat-pill {
  padding: 2px 6px;
  border-radius: 4px;
  background: var(--kc-surface-muted);
  color: var(--kc-text-secondary);
  font-weight: 600;
}

.cs-time-muted {
  margin-left: auto;
}

.cs-detail-card {
  background: var(--kc-surface);
  border: 1px solid var(--kc-border);
  border-radius: var(--kc-radius-sm);
  padding: 12px;
}

.cs-detail-title {
  margin: 8px 0 6px;
  font-size: 15px;
  font-weight: 700;
  color: var(--kc-text);
}

.cs-detail-summary {
  margin: 10px 0 0;
  padding-top: 8px;
  border-top: 1px dashed var(--kc-divider);
  font-size: 12px;
  line-height: 1.55;
  color: var(--kc-text-secondary);
}

.cs-replies-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.cs-section-heading {
  font-size: 12px;
  font-weight: 700;
  color: var(--kc-text-secondary);
}

.cs-empty-replies {
  padding: 14px;
  border-radius: var(--kc-radius-sm);
  background: var(--kc-surface);
  border: 1px dashed var(--kc-border);
  font-size: 12px;
  color: var(--kc-text-muted);
  line-height: 1.5;
}

.cs-timeline {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.cs-reply-item {
  padding: 10px 12px;
  border-radius: 10px;
  border: 1px solid var(--kc-border);
  background: var(--kc-surface);
}

.cs-reply-item.sender-USER {
  background: var(--kc-surface-muted);
}

.cs-reply-item.sender-ADMIN {
  border-color: var(--kc-accent-border);
  background: rgba(255, 77, 0, 0.03);
}

.cs-reply-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 5px;
}

.cs-sender-badge {
  font-size: 11px;
  font-weight: 700;
  color: var(--kc-text);
}

.cs-reply-time {
  font-size: 10px;
  color: var(--kc-text-muted);
}

.cs-reply-content {
  font-size: 13px;
  line-height: 1.55;
  color: var(--kc-text);
  white-space: pre-wrap;
  word-break: break-word;
}

.cs-followup-composer {
  padding: 10px 14px 12px;
  border-top: 1px solid var(--kc-divider);
  background: var(--kc-surface);
  flex-shrink: 0;
}

.cs-followup-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
}

.cs-followup-hint {
  font-size: 11px;
  color: var(--kc-text-muted);
}

.cs-closed-banner {
  padding: 10px 14px;
  border-top: 1px solid var(--kc-divider);
  background: var(--kc-surface-muted);
  color: var(--kc-text-muted);
  font-size: 12px;
  text-align: center;
  flex-shrink: 0;
}
</style>
