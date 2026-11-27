<template>
  <div class="table-box">
    <ProTable
      ref="proTableRef"
      title="客服工单管理"
      :indent="20"
      :columns="columns"
      :search-columns="searchColumns"
      :request-api="getTableList"
      row-key="id"
      @reset="handleTableReset"
    >
      <template #tableHeader>
        <div class="ticket-header">
          <el-tabs v-model="activeStatusTab" class="ticket-tabs" @tab-change="handleStatusTabChange">
            <el-tab-pane v-for="item in statusTabs" :key="item.name" :name="item.name" :label="item.label" />
          </el-tabs>
        </div>
      </template>

      <template #operation="{ row }">
        <el-button v-auth="'cs.ticket.query_table'" type="primary" link :icon="View" @click="openDetail(row)">
          详情
        </el-button>
        <el-button
          v-if="row.status !== 'CLOSED'"
          v-auth="'cs.ticket.close'"
          type="danger"
          link
          :icon="CircleClose"
          :loading="closingId === row.id"
          :disabled="closingId === row.id"
          @click="handleCloseTicket(row)"
        >
          关闭
        </el-button>
      </template>
    </ProTable>

    <TicketDetailDrawer ref="detailDrawerRef" @updated="refreshTable" />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { CircleClose, View } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import ProTable from '@/components/ProTable/index.vue';
import {
  CS_TICKET_CATEGORY_OPTIONS,
  CS_TICKET_STATUS_OPTIONS,
  closeCsTicketApi,
  extractApiErrorMessage,
  formatCsDateTime,
  getCsTicketListApi
} from '@/api/modules/shop_system/cs';
import type { ColumnProps, ProTableInstance, SearchProps } from '@/components/ProTable/interface';
import type { CsTicketQuery, CsTicketSummary } from '@/api/types/shop_system/cs';
import TicketDetailDrawer from '@/views/shop_system/csTicket/components/TicketDetailDrawer.vue';

defineOptions({
  name: 'ShopCsTicketView'
});

const proTableRef = ref<ProTableInstance>();
const detailDrawerRef = ref<InstanceType<typeof TicketDetailDrawer>>();
const activeStatusTab = ref('all');
const closingId = ref<string | null>(null);

const statusTabs = [
  { name: 'all', label: '全部' },
  ...CS_TICKET_STATUS_OPTIONS.map(item => ({ name: item.value, label: item.label }))
];

const enumFieldNames = {
  label: 'label',
  value: 'value',
  tagType: 'tagType'
};

const columns: ColumnProps<CsTicketSummary>[] = [
  { prop: 'ticket_no', label: '工单编号', minWidth: 200 },
  { prop: 'title', label: '工单标题', minWidth: 200, showOverflowTooltip: true },
  {
    prop: 'category',
    label: '工单分类',
    minWidth: 120,
    tag: true,
    enum: CS_TICKET_CATEGORY_OPTIONS,
    fieldNames: enumFieldNames
  },
  {
    prop: 'status',
    label: '工单状态',
    minWidth: 110,
    tag: true,
    enum: CS_TICKET_STATUS_OPTIONS,
    fieldNames: enumFieldNames
  },
  {
    prop: 'related_order_no',
    label: '关联订单号',
    minWidth: 190,
    render: ({ row }) => row.related_order_no || '-'
  },
  { prop: 'user_id', label: '用户 ID', minWidth: 100 },
  {
    prop: 'last_reply_at',
    label: '最后回复时间',
    minWidth: 170,
    render: ({ row }) => formatCsDateTime(row.last_reply_at)
  },
  {
    prop: 'created_at',
    label: '创建时间',
    minWidth: 170,
    render: ({ row }) => formatCsDateTime(row.created_at)
  },
  { prop: 'operation', label: '操作', width: 150, fixed: 'right' }
];

const searchColumns: SearchProps[] = [
  {
    prop: 'keyword',
    label: '关键词',
    el: 'input',
    props: { placeholder: '工单号 / 标题 / 摘要 / 订单号' }
  },
  {
    prop: 'category',
    label: '工单分类',
    el: 'select',
    enum: CS_TICKET_CATEGORY_OPTIONS,
    fieldNames: enumFieldNames
  },
  {
    prop: 'status',
    label: '工单状态',
    el: 'select',
    enum: CS_TICKET_STATUS_OPTIONS,
    fieldNames: enumFieldNames
  },
  {
    prop: 'related_order_no',
    label: '关联订单号',
    el: 'input',
    props: { placeholder: '请输入关联订单号' }
  }
];

const getTableList = (params: CsTicketQuery) => {
  activeStatusTab.value = params.status || 'all';
  return getCsTicketListApi(params);
};

const handleStatusTabChange = (name: string | number) => {
  if (!proTableRef.value) return;
  const tabName = String(name);
  const searchParam = proTableRef.value.searchParam as CsTicketQuery;
  const currentStatus = searchParam.status || 'all';
  if (currentStatus === tabName) return;
  activeStatusTab.value = tabName;
  if (tabName === 'all') {
    delete searchParam.status;
  } else {
    searchParam.status = tabName as CsTicketQuery['status'];
  }
  proTableRef.value.search();
};

const handleTableReset = () => {
  activeStatusTab.value = 'all';
};

const refreshTable = () => {
  proTableRef.value?.getTableList();
};

const openDetail = (row: CsTicketSummary) => {
  if (!row.id) return;
  detailDrawerRef.value?.open(row.id);
};

const handleCloseTicket = async (row: CsTicketSummary) => {
  if (!row.id || closingId.value) return;
  const ticketLabel = row.ticket_no || row.id;
  try {
    await ElMessageBox.confirm(`确认关闭工单「${ticketLabel}」吗？关闭后用户将无法继续追问。`, '关闭确认', {
      confirmButtonText: '确认关闭',
      cancelButtonText: '取消',
      type: 'warning'
    });
  } catch {
    return;
  }

  closingId.value = row.id;
  try {
    await closeCsTicketApi(row.id);
    ElMessage.success('工单已关闭');
    refreshTable();
  } catch (error) {
    ElMessage.error(extractApiErrorMessage(error, '关闭工单失败'));
  } finally {
    closingId.value = null;
  }
};
</script>

<style scoped lang="scss">
.ticket-header {
  width: 100%;
  padding: 0 2px 8px;
}

.ticket-tabs {
  margin-bottom: 0;
}

:deep(.ticket-tabs .el-tabs__header) {
  margin: 0;
}

:deep(.ticket-tabs .el-tabs__nav-wrap::after) {
  height: 1px;
  background-color: var(--el-border-color-lighter);
}

:deep(.ticket-tabs .el-tabs__item) {
  height: 34px;
  line-height: 34px;
  padding: 0 18px;
  font-size: 14px;
}
</style>
