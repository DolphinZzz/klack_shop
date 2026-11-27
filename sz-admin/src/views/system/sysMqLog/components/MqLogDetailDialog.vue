<template>
  <el-dialog v-model="visible" :title="dialogTitle" width="860px" destroy-on-close>
    <el-descriptions v-if="mode === 'send' && sendDetail" :column="2" border>
      <el-descriptions-item label="记录ID">{{ sendDetail.id }}</el-descriptions-item>
      <el-descriptions-item label="消息ID">{{ sendDetail.messageId }}</el-descriptions-item>
      <el-descriptions-item label="MQ引擎">
        <el-tag type="info">{{ sendDetail.mqType }}</el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="发送状态">
        <el-tag :type="sendStatusTagType(sendDetail.sendStatus)">{{ sendStatusLabel(sendDetail.sendStatus) }}</el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="Topic">{{ sendDetail.topic }}</el-descriptions-item>
      <el-descriptions-item label="Tag / 路由键">{{ sendDetail.tag || '-' }}</el-descriptions-item>
      <el-descriptions-item label="业务键">{{ sendDetail.bizKey || '-' }}</el-descriptions-item>
      <el-descriptions-item label="Broker消息ID">{{ sendDetail.rawMessageId || '-' }}</el-descriptions-item>
      <el-descriptions-item label="发送成功时间">{{ sendDetail.sentTime || '-' }}</el-descriptions-item>
      <el-descriptions-item label="创建时间">{{ sendDetail.createTime || '-' }}</el-descriptions-item>
      <el-descriptions-item :span="2" label="消息载荷 (Payload)">
        <pre class="mq-json-pre">{{ formatPayload(sendDetail.payload) }}</pre>
      </el-descriptions-item>
      <el-descriptions-item v-if="sendDetail.errorMsg" :span="2" label="错误信息">
        <div class="mq-error-box">{{ sendDetail.errorMsg }}</div>
      </el-descriptions-item>
    </el-descriptions>

    <el-descriptions v-else-if="mode === 'consume' && consumeDetail" :column="2" border>
      <el-descriptions-item label="记录ID">{{ consumeDetail.id }}</el-descriptions-item>
      <el-descriptions-item label="消息ID">{{ consumeDetail.messageId }}</el-descriptions-item>
      <el-descriptions-item label="MQ引擎">
        <el-tag type="info">{{ consumeDetail.mqType }}</el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="消费组">{{ consumeDetail.consumerGroup }}</el-descriptions-item>
      <el-descriptions-item label="Topic">{{ consumeDetail.topic }}</el-descriptions-item>
      <el-descriptions-item label="Tag / 路由键">{{ consumeDetail.tag || '-' }}</el-descriptions-item>
      <el-descriptions-item label="消费状态">
        <el-tag :type="consumeStatusTagType(consumeDetail.consumeStatus)">
          {{ consumeStatusLabel(consumeDetail.consumeStatus) }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="ACK确认状态">
        <el-tag :type="ackStatusTagType(consumeDetail.ackStatus)">
          {{ ackStatusLabel(consumeDetail.ackStatus) }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="业务键">{{ consumeDetail.bizKey || '-' }}</el-descriptions-item>
      <el-descriptions-item label="重试次数">{{ consumeDetail.retryCount ?? 0 }}</el-descriptions-item>
      <el-descriptions-item label="首次接收时间">{{ consumeDetail.receiveTime || '-' }}</el-descriptions-item>
      <el-descriptions-item label="消费成功时间">{{ consumeDetail.consumeTime || '-' }}</el-descriptions-item>
      <el-descriptions-item label="ACK确认时间">{{ consumeDetail.ackTime || '-' }}</el-descriptions-item>
      <el-descriptions-item label="创建时间">{{ consumeDetail.createTime || '-' }}</el-descriptions-item>
      <el-descriptions-item :span="2" label="消息载荷 (Payload)">
        <pre class="mq-json-pre">{{ formatPayload(consumeDetail.payload) }}</pre>
      </el-descriptions-item>
      <el-descriptions-item v-if="consumeDetail.errorMsg" :span="2" label="错误信息">
        <div class="mq-error-box">{{ consumeDetail.errorMsg }}</div>
      </el-descriptions-item>
    </el-descriptions>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { getSysMqConsumeLogDetailApi, getSysMqSendLogDetailApi } from '@/api/modules/system/sysMqLog';
import type { SysMqConsumeLogRow, SysMqSendLogRow } from '@/api/types/system/sysMqLog';

defineOptions({
  name: 'MqLogDetailDialog'
});

type LogMode = 'send' | 'consume';
type TagType = 'primary' | 'success' | 'warning' | 'info' | 'danger';

const visible = ref(false);
const mode = ref<LogMode>('send');
const sendDetail = ref<SysMqSendLogRow>();
const consumeDetail = ref<SysMqConsumeLogRow>();

const dialogTitle = computed(() => (mode.value === 'send' ? 'MQ 发送记录详情' : 'MQ 消费记录详情'));

const showSend = async (row: SysMqSendLogRow) => {
  mode.value = 'send';
  sendDetail.value = row;
  consumeDetail.value = undefined;
  visible.value = true;
  if (row?.id) {
    const res = await getSysMqSendLogDetailApi(row.id);
    sendDetail.value = res.data;
  }
};

const showConsume = async (row: SysMqConsumeLogRow) => {
  mode.value = 'consume';
  consumeDetail.value = row;
  sendDetail.value = undefined;
  visible.value = true;
  if (row?.id) {
    const res = await getSysMqConsumeLogDetailApi(row.id);
    consumeDetail.value = res.data;
  }
};

const formatPayload = (raw?: string) => {
  if (!raw) return '-';
  try {
    return JSON.stringify(JSON.parse(raw), null, 2);
  } catch {
    return raw;
  }
};

const sendStatusTagType = (status?: string): TagType => {
  if (status === 'SENT') return 'success';
  if (status === 'FAILED') return 'danger';
  return 'warning';
};

const sendStatusLabel = (status?: string) => {
  if (status === 'SENT') return '已发送';
  if (status === 'FAILED') return '发送失败';
  if (status === 'PENDING') return '待发送';
  return status || '-';
};

const consumeStatusTagType = (status?: string): TagType => {
  if (status === 'SUCCESS') return 'success';
  if (status === 'DEAD') return 'danger';
  if (status === 'RETRYING') return 'warning';
  return 'primary';
};

const consumeStatusLabel = (status?: string) => {
  if (status === 'SUCCESS') return '消费成功';
  if (status === 'DEAD') return '进入死信';
  if (status === 'RETRYING') return '重试中';
  if (status === 'CONSUMING') return '消费中';
  return status || '-';
};

const ackStatusTagType = (status?: string): TagType => {
  if (status === 'ACKED') return 'success';
  if (status === 'PENDING') return 'info';
  return 'danger';
};

const ackStatusLabel = (status?: string) => {
  if (status === 'ACKED') return '已ACK';
  if (status === 'PENDING') return '待确认';
  if (status === 'NACKED_DLQ') return '死信NACK';
  if (status === 'SENT_DLT') return '死信DLT';
  if (status === 'BROKER_DLQ') return 'Broker死信';
  if (status === 'NON_RETRYABLE_ACK') return '业务拒绝ACK';
  return status || '-';
};

defineExpose({
  showSend,
  showConsume
});
</script>

<style scoped lang="scss">
.mq-json-pre {
  margin: 0;
  padding: 8px 10px;
  max-height: 240px;
  overflow: auto;
  font-size: 12px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-all;
  background-color: var(--el-fill-color-light);
  border-radius: 4px;
}

.mq-error-box {
  max-height: 180px;
  overflow: auto;
  color: var(--el-color-danger);
  white-space: pre-wrap;
  word-break: break-all;
  font-size: 12px;
  line-height: 1.5;
}
</style>
