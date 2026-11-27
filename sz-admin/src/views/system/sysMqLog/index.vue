<template>
  <div class="mq-log-page">
    <div class="card mq-log-tabs-card">
      <el-tabs v-model="activeTab" class="mq-log-tabs" @tab-change="handleTabChange">
        <el-tab-pane label="发送记录" name="send" />
        <el-tab-pane label="消费记录" name="consume" />
      </el-tabs>
    </div>

    <div v-show="activeTab === 'send'" class="table-box mq-log-table-box">
      <ProTable
        ref="sendTableRef"
        title="MQ发送记录"
        :indent="20"
        :columns="sendColumns"
        :search-columns="sendSearchColumns"
        :request-api="getSendTableList"
        row-key="id"
      >
        <template #operation="{ row }">
          <el-button type="primary" link :icon="View" @click="openSendDetail(row)">详情</el-button>
          <el-button type="success" link :icon="Position" @click="handleTraceConsume(row)">追踪消费</el-button>
        </template>
      </ProTable>
    </div>

    <div v-show="activeTab === 'consume'" class="table-box mq-log-table-box">
      <ProTable
        ref="consumeTableRef"
        title="MQ消费记录"
        :indent="20"
        :columns="consumeColumns"
        :search-columns="consumeSearchColumns"
        :request-api="getConsumeTableList"
        :request-auto="false"
        row-key="id"
      >
        <template #operation="{ row }">
          <el-button type="primary" link :icon="View" @click="openConsumeDetail(row)">详情</el-button>
        </template>
      </ProTable>
    </div>

    <MqLogDetailDialog ref="detailDialogRef" />
  </div>
</template>

<script setup lang="ts">
import { nextTick, ref } from 'vue';
import { Position, View } from '@element-plus/icons-vue';
import type { TabPaneName } from 'element-plus';
import ProTable from '@/components/ProTable/index.vue';
import type { ColumnProps, ProTableInstance, SearchProps } from '@/components/ProTable/interface';
import { getSysMqConsumeLogListApi, getSysMqSendLogListApi } from '@/api/modules/system/sysMqLog';
import type { SysMqConsumeLogQuery, SysMqConsumeLogRow, SysMqSendLogQuery, SysMqSendLogRow } from '@/api/types/system/sysMqLog';
import MqLogDetailDialog from './components/MqLogDetailDialog.vue';

defineOptions({
  name: 'SysMqLogView'
});

const activeTab = ref<'send' | 'consume'>('send');
const sendTableRef = ref<ProTableInstance>();
const consumeTableRef = ref<ProTableInstance>();
const detailDialogRef = ref<InstanceType<typeof MqLogDetailDialog>>();

const staticEnumFieldNames = {
  label: 'label',
  value: 'value',
  tagType: 'tagType'
};

const mqTypeOptions = [
  { label: 'RabbitMQ', value: 'rabbitmq', tagType: 'primary' },
  { label: 'RocketMQ', value: 'rocketmq', tagType: 'warning' },
  { label: 'Kafka', value: 'kafka', tagType: 'info' }
];

const sendStatusOptions = [
  { label: '待发送', value: 'PENDING', tagType: 'warning' },
  { label: '已发送', value: 'SENT', tagType: 'success' },
  { label: '发送失败', value: 'FAILED', tagType: 'danger' }
];

const consumeStatusOptions = [
  { label: '消费中', value: 'CONSUMING', tagType: 'primary' },
  { label: '消费成功', value: 'SUCCESS', tagType: 'success' },
  { label: '重试中', value: 'RETRYING', tagType: 'warning' },
  { label: '进入死信', value: 'DEAD', tagType: 'danger' }
];

const ackStatusOptions = [
  { label: '待确认', value: 'PENDING', tagType: 'info' },
  { label: '已ACK', value: 'ACKED', tagType: 'success' },
  { label: '死信NACK', value: 'NACKED_DLQ', tagType: 'danger' },
  { label: '死信DLT', value: 'SENT_DLT', tagType: 'danger' },
  { label: 'Broker死信', value: 'BROKER_DLQ', tagType: 'danger' },
  { label: '业务拒绝ACK', value: 'NON_RETRYABLE_ACK', tagType: 'warning' }
];

const sendColumns: ColumnProps<SysMqSendLogRow>[] = [
  { prop: 'id', label: 'ID', width: 80 },
  { prop: 'messageId', label: '消息ID', minWidth: 180 },
  { prop: 'mqType', label: 'MQ引擎', minWidth: 100, tag: true, enum: mqTypeOptions, fieldNames: staticEnumFieldNames },
  { prop: 'topic', label: 'Topic', minWidth: 130 },
  { prop: 'tag', label: 'Tag', minWidth: 90 },
  { prop: 'bizKey', label: '业务键(订单号)', minWidth: 150 },
  { prop: 'sendStatus', label: '发送状态', minWidth: 100, tag: true, enum: sendStatusOptions, fieldNames: staticEnumFieldNames },
  { prop: 'rawMessageId', label: 'Broker消息ID', minWidth: 150 },
  { prop: 'sentTime', label: '发送确认时间', minWidth: 160 },
  { prop: 'createTime', label: '创建时间', minWidth: 160 },
  { prop: 'operation', label: '操作', width: 165, fixed: 'right' }
];

const sendSearchColumns: SearchProps[] = [
  { prop: 'messageId', label: '消息ID', el: 'input' },
  { prop: 'bizKey', label: '业务键', el: 'input' },
  { prop: 'topic', label: 'Topic', el: 'input' },
  { prop: 'mqType', label: 'MQ引擎', el: 'select', enum: mqTypeOptions },
  { prop: 'sendStatus', label: '发送状态', el: 'select', enum: sendStatusOptions },
  {
    prop: 'createTime',
    label: '创建时间',
    el: 'date-picker',
    span: 2,
    props: {
      type: 'datetimerange',
      valueFormat: 'YYYY-MM-DD HH:mm:ss'
    }
  }
];

const consumeColumns: ColumnProps<SysMqConsumeLogRow>[] = [
  { prop: 'id', label: 'ID', width: 80 },
  { prop: 'messageId', label: '消息ID', minWidth: 180 },
  { prop: 'mqType', label: 'MQ引擎', minWidth: 100, tag: true, enum: mqTypeOptions, fieldNames: staticEnumFieldNames },
  { prop: 'topic', label: 'Topic', minWidth: 130 },
  { prop: 'consumerGroup', label: '消费组', minWidth: 150 },
  { prop: 'bizKey', label: '业务键(订单号)', minWidth: 150 },
  {
    prop: 'consumeStatus',
    label: '消费状态',
    minWidth: 100,
    tag: true,
    enum: consumeStatusOptions,
    fieldNames: staticEnumFieldNames
  },
  { prop: 'ackStatus', label: 'ACK状态', minWidth: 110, tag: true, enum: ackStatusOptions, fieldNames: staticEnumFieldNames },
  { prop: 'retryCount', label: '重试次数', minWidth: 90 },
  { prop: 'consumeTime', label: '消费完成时间', minWidth: 160 },
  { prop: 'ackTime', label: 'ACK时间', minWidth: 160 },
  { prop: 'operation', label: '操作', width: 100, fixed: 'right' }
];

const consumeSearchColumns: SearchProps[] = [
  { prop: 'messageId', label: '消息ID', el: 'input' },
  { prop: 'bizKey', label: '业务键', el: 'input' },
  { prop: 'topic', label: 'Topic', el: 'input' },
  { prop: 'consumerGroup', label: '消费组', el: 'input' },
  { prop: 'mqType', label: 'MQ引擎', el: 'select', enum: mqTypeOptions },
  { prop: 'consumeStatus', label: '消费状态', el: 'select', enum: consumeStatusOptions },
  { prop: 'ackStatus', label: 'ACK状态', el: 'select', enum: ackStatusOptions },
  {
    prop: 'createTime',
    label: '创建时间',
    el: 'date-picker',
    span: 2,
    props: {
      type: 'datetimerange',
      valueFormat: 'YYYY-MM-DD HH:mm:ss'
    }
  }
];

const formatDateRangeParams = <T extends Record<string, any>>(params: T) => {
  const query = JSON.parse(JSON.stringify(params));
  if (Array.isArray(query.createTime) && query.createTime.length === 2) {
    query.createTimeStart = query.createTime[0];
    query.createTimeEnd = query.createTime[1];
    delete query.createTime;
  }
  return query;
};

const getSendTableList = (params: SysMqSendLogQuery) => {
  return getSysMqSendLogListApi(formatDateRangeParams(params));
};

const getConsumeTableList = (params: SysMqConsumeLogQuery) => {
  return getSysMqConsumeLogListApi(formatDateRangeParams(params));
};

const openSendDetail = (row: SysMqSendLogRow) => {
  detailDialogRef.value?.showSend(row);
};

const openConsumeDetail = (row: SysMqConsumeLogRow) => {
  detailDialogRef.value?.showConsume(row);
};

let skipNextTabRefresh = false;

const handleTabChange = (tabName: TabPaneName) => {
  if (skipNextTabRefresh) {
    skipNextTabRefresh = false;
    return;
  }
  nextTick(() => {
    if (tabName === 'send') {
      sendTableRef.value?.element?.doLayout();
      sendTableRef.value?.getTableList();
    } else if (tabName === 'consume') {
      consumeTableRef.value?.element?.doLayout();
      consumeTableRef.value?.getTableList();
    }
  });
};

const handleTraceConsume = (row: SysMqSendLogRow) => {
  skipNextTabRefresh = true;
  activeTab.value = 'consume';
  nextTick(() => {
    if (consumeTableRef.value) {
      consumeTableRef.value.searchParam.messageId = row.messageId;
      consumeTableRef.value.search();
      consumeTableRef.value.element?.doLayout();
    }
  });
};
</script>

<style scoped lang="scss">
.mq-log-page {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
}

.mq-log-tabs-card {
  flex-shrink: 0;
  padding: 6px 20px 0;
  margin-bottom: 10px;
}

.mq-log-tabs {
  :deep(.el-tabs__header) {
    margin: 0;
  }

  :deep(.el-tabs__content) {
    display: none;
  }

  :deep(.el-tabs__nav-wrap::after) {
    height: 1px;
    background-color: transparent;
  }

  :deep(.el-tabs__item) {
    height: 38px;
    line-height: 38px;
    font-size: 14px;
    font-weight: 500;
  }

  :deep(.el-tabs__item.is-active) {
    font-weight: 600;
  }

  :deep(.el-tabs__active-bar) {
    height: 3px;
    border-radius: 3px;
  }
}

.mq-log-table-box {
  flex: 1;
  min-height: 0;
  overflow: hidden;

  :deep(.table-main) {
    min-height: 0;
  }
}
</style>
