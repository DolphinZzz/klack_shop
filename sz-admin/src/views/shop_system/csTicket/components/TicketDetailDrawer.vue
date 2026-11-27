<template>
  <el-drawer v-model="visible" title="客服工单详情" size="760px" destroy-on-close @closed="handleDrawerClosed">
    <div v-loading="loading" class="ticket-drawer">
      <template v-if="detail">
        <!-- 1. 前情摘要区 -->
        <section class="drawer-section">
          <div class="section-header">
            <div class="ticket-heading">
              <span class="ticket-no">{{ detail.ticket_no }}</span>
              <el-tag :type="statusOption?.tagType || 'info'" size="small">
                {{ statusOption?.label || detail.status }}
              </el-tag>
              <el-tag :type="categoryOption?.tagType || 'info'" effect="plain" size="small">
                {{ categoryOption?.label || detail.category }}
              </el-tag>
            </div>
          </div>

          <div class="summary-grid">
            <div class="summary-item">
              <span>用户 ID</span>
              <strong>{{ detail.user_id }}</strong>
            </div>
            <div class="summary-item">
              <span>关联订单号</span>
              <strong>{{ detail.related_order_no || '-' }}</strong>
            </div>
            <div class="summary-item">
              <span>创建时间</span>
              <strong>{{ formatCsDateTime(detail.created_at) }}</strong>
            </div>
            <div class="summary-item">
              <span>最后回复</span>
              <strong>{{ formatCsDateTime(detail.last_reply_at) }}</strong>
            </div>
          </div>

          <div class="summary-box">
            <div class="summary-title">{{ detail.title || '-' }}</div>
            <div class="summary-desc">{{ detail.summary || '暂无前情摘要' }}</div>
            <div v-if="detail.closed_at" class="closed-meta">关闭时间：{{ formatCsDateTime(detail.closed_at) }}</div>
          </div>
        </section>

        <!-- 2. 折叠历史会话回放区 -->
        <section class="drawer-section">
          <el-collapse v-model="activeCollapseNames" class="session-collapse">
            <el-collapse-item name="sessionReplay">
              <template #title>
                <div class="collapse-title">
                  <span>历史会话回放</span>
                  <el-tag size="small" type="info" effect="plain">
                    {{ detail.session_messages?.length || 0 }} 条消息
                  </el-tag>
                </div>
              </template>

              <div v-if="detail.session_messages?.length" class="session-message-list">
                <div
                  v-for="msg in detail.session_messages"
                  :key="msg.id"
                  class="session-message-item"
                  :class="`is-${msg.role}`"
                >
                  <div class="message-meta">
                    <el-tag :type="roleTagType(msg.role)" size="small" effect="light">
                      {{ roleLabel(msg.role) }}
                    </el-tag>
                    <el-tag v-if="msg.intent" size="small" type="info" effect="plain">
                      {{ msg.intent }}
                    </el-tag>
                    <span class="message-time">{{ formatCsDateTime(msg.created_at) }}</span>
                  </div>
                  <div class="message-content">{{ msg.content }}</div>
                  <div v-if="(msg.cards && msg.cards.length) || (msg.citations && msg.citations.length)" class="message-extras">
                    <span v-if="msg.cards?.length">附带卡片 {{ msg.cards.length }} 项</span>
                    <span v-if="msg.citations?.length">知识引用 {{ msg.citations.length }} 条</span>
                  </div>
                </div>
              </div>
              <el-empty v-else description="暂无关联会话记录" :image-size="60" />
            </el-collapse-item>
          </el-collapse>
        </section>

        <!-- 3. 回复时间轴与操作区 -->
        <section class="drawer-section">
          <div class="section-title">沟通回复时间轴</div>
          <el-timeline v-if="detail.replies?.length" class="reply-timeline">
            <el-timeline-item
              v-for="reply in detail.replies"
              :key="reply.id"
              :timestamp="formatCsDateTime(reply.created_at)"
              :type="reply.sender_type === 'ADMIN' ? 'primary' : 'warning'"
              placement="top"
            >
              <div class="reply-card" :class="reply.sender_type === 'ADMIN' ? 'is-admin' : 'is-user'">
                <div class="reply-header">
                  <el-tag :type="reply.sender_type === 'ADMIN' ? 'primary' : 'warning'" size="small">
                    {{ reply.sender_type === 'ADMIN' ? '客服回复' : '用户追问' }}
                  </el-tag>
                  <span class="reply-sender">{{ reply.sender_name || (reply.sender_type === 'ADMIN' ? `客服#${reply.sender_id}` : `用户#${reply.sender_id}`) }}</span>
                </div>
                <div class="reply-content">{{ reply.content }}</div>
              </div>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无工单回复记录" :image-size="60" />
        </section>

        <section class="drawer-section">
          <div v-if="detail.status !== 'CLOSED'" v-auth="[{ type: 'or', conditions: ['cs.ticket.reply', 'cs.ticket.close'] }]">
            <div class="section-title">处理工单</div>
            <div v-auth="'cs.ticket.reply'">
              <el-input
                v-model="replyContent"
                type="textarea"
                :rows="4"
                maxlength="4000"
                show-word-limit
                placeholder="请输入回复内容，提交后将同步通知用户关联会话"
              />
            </div>
            <div class="action-bar">
              <el-button
                v-auth="'cs.ticket.reply'"
                type="primary"
                :loading="submittingAction === 'reply'"
                :disabled="Boolean(submittingAction)"
                @click="handleReply(false)"
              >
                发送回复
              </el-button>
              <el-button
                v-auth="[{ type: 'and', conditions: ['cs.ticket.reply', 'cs.ticket.close'] }]"
                type="success"
                :loading="submittingAction === 'replyAndClose'"
                :disabled="Boolean(submittingAction)"
                @click="handleReply(true)"
              >
                回复并关闭工单
              </el-button>
              <el-button
                v-auth="'cs.ticket.close'"
                type="danger"
                plain
                :loading="submittingAction === 'close'"
                :disabled="Boolean(submittingAction)"
                @click="handleCloseOnly"
              >
                关闭工单
              </el-button>
            </div>
          </div>
          <el-alert v-else title="该工单已关闭，当前仅可查看历史会话与回复记录" type="info" show-icon :closable="false" />
        </section>
      </template>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  CS_TICKET_CATEGORY_OPTIONS,
  CS_TICKET_STATUS_OPTIONS,
  closeCsTicketApi,
  extractApiErrorMessage,
  findCsOption,
  formatCsDateTime,
  getCsTicketDetailApi,
  replyCsTicketApi
} from '@/api/modules/shop_system/cs';
import type { CsAdminTicketDetail } from '@/api/types/shop_system/cs';

defineOptions({
  name: 'TicketDetailDrawer'
});

const emit = defineEmits<{
  updated: [];
}>();

const visible = ref(false);
const loading = ref(false);
const submittingAction = ref<'reply' | 'replyAndClose' | 'close' | null>(null);
const detail = ref<CsAdminTicketDetail | null>(null);
const replyContent = ref('');
const activeCollapseNames = ref<string[]>([]);
let loadSeq = 0;

const statusOption = computed(() => findCsOption(CS_TICKET_STATUS_OPTIONS, detail.value?.status));
const categoryOption = computed(() => findCsOption(CS_TICKET_CATEGORY_OPTIONS, detail.value?.category));

const roleLabel = (role: string) => {
  const map: Record<string, string> = {
    user: '用户',
    assistant: 'AI 客服',
    system: '系统消息'
  };
  return map[role] || role;
};

const roleTagType = (role: string): 'primary' | 'success' | 'info' | 'warning' => {
  if (role === 'user') return 'warning';
  if (role === 'assistant') return 'primary';
  return 'info';
};

const loadDetail = async (ticketId: string) => {
  const seq = ++loadSeq;
  loading.value = true;
  try {
    const data = await getCsTicketDetailApi(ticketId);
    if (seq !== loadSeq || !visible.value) return;
    detail.value = data;
  } catch (error) {
    if (seq !== loadSeq || !visible.value) return;
    ElMessage.error(extractApiErrorMessage(error, '加载工单详情失败'));
    visible.value = false;
  } finally {
    if (seq === loadSeq) {
      loading.value = false;
    }
  }
};

const open = async (ticketId: string) => {
  if (!ticketId) return;
  visible.value = true;
  detail.value = null;
  replyContent.value = '';
  activeCollapseNames.value = [];
  await loadDetail(ticketId);
};

const handleDrawerClosed = () => {
  loadSeq++;
  loading.value = false;
  detail.value = null;
  replyContent.value = '';
  activeCollapseNames.value = [];
};

const handleReply = async (closeAfterReply: boolean) => {
  const targetId = detail.value?.id;
  if (!targetId || submittingAction.value) return;
  const content = replyContent.value.trim();
  if (!content) {
    ElMessage.warning('请输入回复内容');
    return;
  }

  submittingAction.value = closeAfterReply ? 'replyAndClose' : 'reply';
  try {
    await replyCsTicketApi(targetId, {
      content,
      close_ticket: closeAfterReply
    });
    ElMessage.success(closeAfterReply ? '已回复并关闭工单' : '回复已发送');
    if (visible.value && detail.value?.id === targetId) {
      replyContent.value = '';
      await loadDetail(targetId);
    }
    emit('updated');
  } catch (error) {
    ElMessage.error(extractApiErrorMessage(error, '发送回复失败'));
  } finally {
    submittingAction.value = null;
  }
};

const handleCloseOnly = async () => {
  const current = detail.value;
  if (!current?.id || submittingAction.value) return;
  const targetId = current.id;
  try {
    await ElMessageBox.confirm(`确认关闭工单「${current.ticket_no || targetId}」吗？关闭后用户将无法继续追问。`, '关闭确认', {
      confirmButtonText: '确认关闭',
      cancelButtonText: '取消',
      type: 'warning'
    });
  } catch {
    return;
  }

  submittingAction.value = 'close';
  try {
    await closeCsTicketApi(targetId);
    ElMessage.success('工单已关闭');
    if (visible.value && detail.value?.id === targetId) {
      await loadDetail(targetId);
    }
    emit('updated');
  } catch (error) {
    ElMessage.error(extractApiErrorMessage(error, '关闭工单失败'));
  } finally {
    submittingAction.value = null;
  }
};

defineExpose({
  open
});
</script>

<style scoped lang="scss">
.ticket-drawer {
  min-height: 240px;
  padding-right: 4px;
}

.drawer-section {
  margin-bottom: 20px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.ticket-heading {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.ticket-no {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.section-title {
  margin-bottom: 12px;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 12px;
}

.summary-item {
  padding: 12px 14px;
  border-radius: 10px;
  background: #f6f8fb;

  span {
    display: block;
    font-size: 12px;
    color: #909399;
  }

  strong {
    display: block;
    margin-top: 6px;
    font-size: 14px;
    color: #303133;
    word-break: break-all;
  }
}

.summary-box {
  padding: 14px 16px;
  border-radius: 10px;
  background: #f6f8fb;
  border: 1px solid var(--el-border-color-lighter);
}

.summary-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.summary-desc {
  margin-top: 8px;
  font-size: 13px;
  line-height: 1.7;
  color: #606266;
  white-space: pre-wrap;
  word-break: break-word;
}

.closed-meta {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}

.session-collapse {
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid var(--el-border-color-lighter);
}

.collapse-title {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-left: 12px;
  font-weight: 600;
}

.session-message-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 360px;
  overflow-y: auto;
  padding: 4px 12px 12px;
}

.session-message-item {
  padding: 10px 12px;
  border-radius: 8px;
  background: #f8fafc;
  border-left: 3px solid #dcdfe6;

  &.is-user {
    border-left-color: var(--el-color-warning);
  }

  &.is-assistant {
    border-left-color: var(--el-color-primary);
  }
}

.message-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.message-time {
  margin-left: auto;
  font-size: 12px;
  color: #909399;
}

.message-content {
  font-size: 13px;
  line-height: 1.65;
  color: #303133;
  white-space: pre-wrap;
  word-break: break-word;
}

.message-extras {
  display: flex;
  gap: 12px;
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
}

.reply-timeline {
  padding-left: 4px;
}

.reply-card {
  padding: 12px 14px;
  border-radius: 10px;
  background: #f6f8fb;

  &.is-admin {
    background: #ecf5ff;
  }
}

.reply-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.reply-sender {
  font-size: 13px;
  font-weight: 500;
  color: #606266;
}

.reply-content {
  font-size: 13px;
  line-height: 1.65;
  color: #303133;
  white-space: pre-wrap;
  word-break: break-word;
}

.action-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 12px;
}
</style>
