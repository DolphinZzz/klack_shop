<template>
  <div class="table-box">
    <ProTable
      ref="proTableRef"
      title="订单查询"
      :indent="20"
      :columns="columns"
      :search-columns="searchColumns"
      :request-api="getTableList"
      row-key="id"
      @reset="handleTableReset"
    >
      <template #tableHeader>
        <div class="order-header">
          <el-tabs v-model="activeStatusTab" class="order-status-tabs" @tab-change="handleStatusTabChange">
            <el-tab-pane v-for="item in statusTabs" :key="item.name" :name="item.name" :label="item.label" />
          </el-tabs>
        </div>
      </template>

      <template #operation="{ row }">
        <el-button type="primary" link :icon="View" @click="openDetail(row)">详情</el-button>
        <el-button v-if="showDeliverAction(row)" type="primary" link :icon="Box" @click="openDeliverDialog(row)">发货</el-button>
      </template>
    </ProTable>

    <ShopOrderDetailDrawer ref="detailDrawerRef" />
    <el-dialog v-model="deliverDialogVisible" title="订单发货" width="420px" destroy-on-close>
      <el-form label-width="100px">
        <el-form-item label="订单号">
          <el-input :model-value="deliverForm.orderNo" disabled />
        </el-form-item>
        <el-form-item label="快递模板" required>
          <el-select v-model="deliverForm.expressTemplateId" class="deliver-select" clearable filterable placeholder="请选择快递模板">
            <el-option v-for="item in expressTemplateOptions" :key="item.id" :label="item.label" :value="item.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="deliverDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="deliverSubmitting" @click="submitDeliver">确定发货</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { Box, View } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import ProTable from '@/components/ProTable/index.vue';
import { getShopOrderListApi, deliverShopOrderApi } from '@/api/modules/shop_system/shopOrder';
import { getShopExpressTemplateListApi } from '@/api/modules/shop_system/shopExpressTemplate';
import ShopOrderDetailDrawer from '@/views/shop_system/shopOrder/components/ShopOrderDetailDrawer.vue';
import { useDictOptions } from '@/hooks/useDictOptions';
import type { ColumnProps, ProTableInstance, SearchProps } from '@/components/ProTable/interface';
import type { ShopOrderQuery, ShopOrderRow } from '@/api/types/shop_system/shopOrder';
import type { ShopExpressTemplateRow } from '@/api/types/shop_system/shopExpressTemplate';
import { useDict } from '@/hooks/useDict';

defineOptions({
  name: 'ShopOrderView'
});

useDict(['order_status']);

const proTableRef = ref<ProTableInstance>();
const activeStatusTab = ref('all');

const orderTypeOptions = [
  { label: '普通', value: 0 },
  { label: '秒杀', value: 1 },
  { label: '砍价', value: 2 },
  { label: '拼团', value: 3 }
];

const orderStatusOptions = useDictOptions('order_status');

const statusTabs = computed(() => {
  const dictTabs = (orderStatusOptions.value || [])
    .map(item => {
      const statusValue = Number(item?.alias);
      if (Number.isNaN(statusValue)) return null;
      return {
        name: String(statusValue),
        label: item?.codeName || String(statusValue)
      };
    })
    .filter(Boolean) as { name: string; label: string }[];
  return [{ name: 'all', label: '全部' }, ...dictTabs];
});

const columns: ColumnProps<ShopOrderRow>[] = [
  { prop: 'orderId', label: '订单号', minWidth: 220 },
  { prop: 'realName', label: '用户名', minWidth: 120 },
  { prop: 'userAddress', label: '收货地址', minWidth: 260, showOverflowTooltip: true },
  { prop: 'totalPrice', label: '订单总价', minWidth: 100 },
  { prop: 'payPrice', label: '实付金额', minWidth: 100 },
  { prop: 'payTime', label: '支付时间', minWidth: 180 },
  { prop: 'payType', label: '支付方式', minWidth: 100 },
  {
    prop: 'status',
    label: '订单状态',
    minWidth: 100,
    tag: true,
    enum: orderStatusOptions,
    fieldNames: {
      label: 'codeName',
      value: 'alias',
      tagType: 'callbackShowStyle'
    }
  },
  { prop: 'createTime', label: '创建时间', minWidth: 180 },
  { prop: 'orderType', label: '订单类型', minWidth: 100 },
  { prop: 'operation', label: '操作', width: 170, fixed: 'right' }
];

const searchColumns: SearchProps[] = [
  { prop: 'orderId', label: '订单号', el: 'input' },
  { prop: 'orderType', label: '订单类型', el: 'select', enum: orderTypeOptions },
  { prop: 'realName', label: '真实姓名', el: 'input' }
];

const handleStatusTabChange = (name: string | number) => {
  if (!proTableRef.value) return;

  const tabName = String(name);
  activeStatusTab.value = tabName;

  if (tabName === 'all') {
    delete (proTableRef.value.searchParam as ShopOrderQuery).status;
  } else {
    (proTableRef.value.searchParam as ShopOrderQuery).status = Number(tabName);
  }

  proTableRef.value.search();
};

const handleTableReset = () => {
  activeStatusTab.value = 'all';
};

const getTableList = (params: ShopOrderQuery) => {
  const newParams = formatParams(params);
  return getShopOrderListApi(newParams);
};

const formatParams = (params: ShopOrderQuery) => {
  const newParams = JSON.parse(JSON.stringify(params));

  if (newParams.payTime) {
    newParams.payTimeStart = newParams.payTime[0];
    newParams.payTimeEnd = newParams.payTime[1];
    delete newParams.payTime;
  }

  if (newParams.createTime) {
    newParams.createTimeStart = newParams.createTime[0];
    newParams.createTimeEnd = newParams.createTime[1];
    delete newParams.createTime;
  }

  return newParams;
};

const detailDrawerRef = ref<InstanceType<typeof ShopOrderDetailDrawer>>();
const deliverDialogVisible = ref(false);
const deliverSubmitting = ref(false);
const deliverForm = ref<{ orderNo: string; expressTemplateId: number | null }>({
  orderNo: '',
  expressTemplateId: null
});
const expressTemplateOptions = ref<{ id: number; label: string }[]>([]);

const openDetail = (row: ShopOrderRow) => {
  const orderNo = String(row?.orderId || '');
  detailDrawerRef.value?.open(orderNo);
};

const showDeliverAction = (row: ShopOrderRow) => {
  const orderNo = String(row?.orderId || '').trim();
  const isPendingDeliveryTab = activeStatusTab.value === '0';
  return isPendingDeliveryTab && Boolean(orderNo) && Number(row?.paid) === 1 && Number(row?.status) === 0;
};

const loadExpressTemplateOptions = async () => {
  const res = await getShopExpressTemplateListApi({ current: 1, limit: 500 });
  const rows: ShopExpressTemplateRow[] = res?.data?.rows || [];
  expressTemplateOptions.value = rows.map(item => ({
    id: item.id,
    label: `${item.name}（${item.expressName || '-'} / ￥${item.expressPrice ?? 0}）`
  }));
};

const openDeliverDialog = async (row: ShopOrderRow) => {
  const orderNo = String(row?.orderId || '').trim();
  if (!orderNo) {
    ElMessage.warning('订单号不能为空');
    return;
  }

  deliverForm.value = {
    orderNo,
    expressTemplateId: null
  };

  if (!expressTemplateOptions.value.length) {
    await loadExpressTemplateOptions();
  }

  deliverDialogVisible.value = true;
};

const submitDeliver = async () => {
  const { orderNo, expressTemplateId } = deliverForm.value;
  if (!orderNo) {
    ElMessage.warning('订单号不能为空');
    return;
  }
  if (!expressTemplateId) {
    ElMessage.warning('请选择快递模板');
    return;
  }

  deliverSubmitting.value = true;
  try {
    await deliverShopOrderApi({ orderNo, expressTemplateId });
    ElMessage.success('发货成功');
    deliverDialogVisible.value = false;
    proTableRef.value?.search();
  } finally {
    deliverSubmitting.value = false;
  }
};
</script>

<style scoped lang="scss">
.order-header {
  width: 100%;
  padding: 0 2px 8px;
}

.order-status-tabs {
  margin-bottom: 0;
}

:deep(.order-status-tabs .el-tabs__header) {
  margin: 0;
}

:deep(.order-status-tabs .el-tabs__nav-wrap::after) {
  height: 1px;
  background-color: var(--el-border-color-lighter);
}

:deep(.order-status-tabs .el-tabs__item) {
  height: 34px;
  line-height: 34px;
  padding: 0 18px;
  font-size: 14px;
  color: var(--el-text-color-regular);
}

:deep(.order-status-tabs .el-tabs__item + .el-tabs__item) {
  margin-left: 10px;
}

:deep(.order-status-tabs .el-tabs__item.is-active) {
  color: var(--el-color-primary);
  font-weight: 600;
}

:deep(.order-status-tabs .el-tabs__active-bar) {
  height: 3px;
  border-radius: 3px;
}

.deliver-select {
  width: 100%;
}
</style>

