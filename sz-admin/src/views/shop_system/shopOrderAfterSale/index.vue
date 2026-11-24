<template>
  <div class="table-box">
    <ProTable
      ref="proTableRef"
      title="售后管理"
      :indent="20"
      :columns="columns"
      :search-columns="searchColumns"
      :request-api="getTableList"
      row-key="id"
      @reset="handleTableReset"
    >
      <template #tableHeader>
        <div class="after-sale-header">
          <el-tabs v-model="activeStatusTab" class="after-sale-tabs" @tab-change="handleStatusTabChange">
            <el-tab-pane v-for="item in statusTabs" :key="item.name" :name="item.name" :label="item.label" />
          </el-tabs>
        </div>
      </template>

      <template #operation="{ row }">
        <el-button v-auth="'shop.order.after.sale.query_table'" type="primary" link :icon="View" @click="openDetail(row)">详情</el-button>
        <el-button
          v-if="Number(row.status) === 0"
          v-auth="'shop.order.after.sale.update'"
          type="primary"
          link
          :icon="Select"
          @click="openAudit(row)"
        >
          审核
        </el-button>
        <el-button
          v-if="Number(row.status) === 2"
          v-auth="'shop.order.after.sale.update'"
          type="primary"
          link
          :icon="Box"
          @click="openReceive(row)"
        >
          确认收货退款
        </el-button>
      </template>
    </ProTable>

    <el-drawer v-model="detailVisible" title="售后详情" size="720px" destroy-on-close>
      <div v-loading="detailLoading" class="detail-drawer">
        <template v-if="detailRecord">
          <div class="summary-grid">
            <div class="summary-card">
              <span>售后单号</span>
              <strong>{{ detailRecord.serviceNo || '-' }}</strong>
            </div>
            <div class="summary-card">
              <span>订单号</span>
              <strong>{{ detailRecord.orderNo || '-' }}</strong>
            </div>
            <div class="summary-card">
              <span>售后类型</span>
              <strong>{{ detailRecord.afterSaleTypeText || '-' }}</strong>
            </div>
            <div class="summary-card">
              <span>当前状态</span>
              <strong>{{ detailRecord.statusText || '-' }}</strong>
            </div>
          </div>

          <section class="detail-section">
            <div class="section-title">用户信息</div>
            <div class="info-grid">
              <div><span>用户：</span>{{ detailRecord.realName || '-' }}</div>
              <div><span>手机号：</span>{{ detailRecord.userPhone || '-' }}</div>
              <div class="span-2"><span>收货地址：</span>{{ detailRecord.userAddress || '-' }}</div>
            </div>
          </section>

          <section class="detail-section">
            <div class="section-title">售后信息</div>
            <div class="info-grid">
              <div><span>申请金额：</span>{{ formatMoney(detailRecord.applyAmount) }}</div>
              <div><span>申请原因：</span>{{ detailRecord.reason || '-' }}</div>
              <div><span>用户备注：</span>{{ detailRecord.userRemark || '-' }}</div>
              <div><span>审核备注：</span>{{ detailRecord.auditRemark || '-' }}</div>
              <div><span>收货备注：</span>{{ detailRecord.receiveRemark || '-' }}</div>
              <div><span>退货备注：</span>{{ detailRecord.returnRemark || '-' }}</div>
              <div class="span-2">
                <span>退货物流：</span>
                {{ detailRecord.returnExpressNo ? `${detailRecord.returnExpressName || '-'} / ${detailRecord.returnExpressNo}` : '-' }}
              </div>
            </div>
          </section>

          <section class="detail-section">
            <div class="section-title">商品信息</div>
            <div v-if="detailRecord.orderInfoList?.length" class="goods-list">
              <div
                v-for="item in detailRecord.orderInfoList"
                :key="`${detailRecord.id}-${item.skuId}-${item.productId}`"
                class="goods-row"
              >
                <el-image class="goods-image" :src="item.image" fit="cover">
                  <template #error>
                    <div class="goods-image goods-empty">暂无图片</div>
                  </template>
                </el-image>
                <div class="goods-main">
                  <h4>{{ item.productName || '-' }}</h4>
                  <p>规格：{{ item.sku || '-' }}</p>
                  <p>{{ formatMoney(item.price) }} × {{ item.cartNum || 0 }}</p>
                </div>
              </div>
            </div>
            <el-empty v-else description="暂无商品信息" :image-size="72" />
          </section>

          <section class="detail-section">
            <div class="section-title">时间节点</div>
            <div class="timeline-grid">
              <div class="timeline-item">
                <span>申请时间</span>
                <strong>{{ formatDateTime(detailRecord.createTime) }}</strong>
              </div>
              <div class="timeline-item">
                <span>审核时间</span>
                <strong>{{ formatDateTime(detailRecord.auditTime) }}</strong>
              </div>
              <div class="timeline-item">
                <span>退货时间</span>
                <strong>{{ formatDateTime(detailRecord.returnTime) }}</strong>
              </div>
              <div class="timeline-item">
                <span>收货时间</span>
                <strong>{{ formatDateTime(detailRecord.receiveTime) }}</strong>
              </div>
              <div class="timeline-item">
                <span>退款时间</span>
                <strong>{{ formatDateTime(detailRecord.refundTime) }}</strong>
              </div>
            </div>
          </section>
        </template>
      </div>
    </el-drawer>

    <el-dialog v-model="auditVisible" title="售后审核" width="520px" destroy-on-close>
      <el-form label-width="96px">
        <el-form-item label="审核结果" required>
          <el-radio-group v-model="auditForm.approved">
            <el-radio :value="true">同意</el-radio>
            <el-radio :value="false">拒绝</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审核备注">
          <el-input
            v-model="auditForm.auditRemark"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="请输入审核说明"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="auditVisible = false">取消</el-button>
        <el-button type="primary" :loading="auditSubmitting" @click="submitAudit">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="receiveVisible" title="确认收货并退款" width="560px" destroy-on-close>
      <div class="receive-summary" v-if="receiveRecord">
        <p>售后单号：{{ receiveRecord.serviceNo || '-' }}</p>
        <p>订单号：{{ receiveRecord.orderNo || '-' }}</p>
        <p>退货物流：{{ receiveRecord.returnExpressNo ? `${receiveRecord.returnExpressName || '-'} / ${receiveRecord.returnExpressNo}` : '-' }}</p>
      </div>
      <el-form label-width="96px">
        <el-form-item label="收货备注">
          <el-input
            v-model="receiveForm.receiveRemark"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="请输入收货确认备注"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="receiveVisible = false">取消</el-button>
        <el-button type="primary" :loading="receiveSubmitting" @click="submitReceive">确认退款</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { Box, Select, View } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import ProTable from '@/components/ProTable/index.vue';
import {
  auditShopOrderAfterSaleApi,
  getShopOrderAfterSaleDetailApi,
  getShopOrderAfterSaleListApi,
  receiveShopOrderAfterSaleApi
} from '@/api/modules/shop_system/shopOrderAfterSale';
import type { ColumnProps, ProTableInstance, SearchProps } from '@/components/ProTable/interface';
import type {
  ShopOrderAfterSaleAuditDTO,
  ShopOrderAfterSaleQuery,
  ShopOrderAfterSaleReceiveDTO,
  ShopOrderAfterSaleRow
} from '@/api/types/shop_system/shopOrderAfterSale';

defineOptions({
  name: 'ShopOrderAfterSaleView'
});

const proTableRef = ref<ProTableInstance>();
const activeStatusTab = ref('all');

const afterSaleTypeOptions = [
  { label: '仅退款', value: 1, tagType: 'warning' },
  { label: '退货退款', value: 2, tagType: 'danger' }
];

const afterSaleStatusOptions = [
  { label: '待审核', value: 0, tagType: 'warning' },
  { label: '待寄回', value: 1, tagType: 'primary' },
  { label: '待退款', value: 2, tagType: 'danger' },
  { label: '已退款', value: 3, tagType: 'success' },
  { label: '已拒绝', value: 4, tagType: 'info' }
];

const statusTabs = [{ name: 'all', label: '全部' }, ...afterSaleStatusOptions.map(item => ({ name: String(item.value), label: item.label }))];

const columns: ColumnProps<ShopOrderAfterSaleRow>[] = [
  { prop: 'serviceNo', label: '售后单号', minWidth: 180 },
  { prop: 'orderNo', label: '订单号', minWidth: 180 },
  { prop: 'realName', label: '用户', minWidth: 120 },
  { prop: 'userPhone', label: '手机号', minWidth: 130 },
  {
    prop: 'afterSaleType',
    label: '售后类型',
    minWidth: 110,
    tag: true,
    enum: afterSaleTypeOptions,
    fieldNames: {
      label: 'label',
      value: 'value',
      tagType: 'tagType'
    }
  },
  {
    prop: 'status',
    label: '状态',
    minWidth: 110,
    tag: true,
    enum: afterSaleStatusOptions,
    fieldNames: {
      label: 'label',
      value: 'value',
      tagType: 'tagType'
    }
  },
  { prop: 'applyAmount', label: '申请金额', minWidth: 100 },
  { prop: 'reason', label: '申请原因', minWidth: 220, showOverflowTooltip: true },
  { prop: 'createTime', label: '申请时间', minWidth: 170 },
  { prop: 'operation', label: '操作', width: 220, fixed: 'right' }
];

const searchColumns: SearchProps[] = [
  { prop: 'serviceNo', label: '售后单号', el: 'input' },
  { prop: 'orderNo', label: '订单号', el: 'input' },
  {
    prop: 'afterSaleType',
    label: '售后类型',
    el: 'select',
    enum: afterSaleTypeOptions,
    fieldNames: {
      label: 'label',
      value: 'value',
      tagType: 'tagType'
    }
  },
  {
    prop: 'status',
    label: '售后状态',
    el: 'select',
    enum: afterSaleStatusOptions,
    fieldNames: {
      label: 'label',
      value: 'value',
      tagType: 'tagType'
    }
  }
];

const getTableList = (params: ShopOrderAfterSaleQuery) => {
  return getShopOrderAfterSaleListApi(params);
};

const handleStatusTabChange = (name: string | number) => {
  if (!proTableRef.value) return;

  const tabName = String(name);
  activeStatusTab.value = tabName;
  if (tabName === 'all') {
    delete (proTableRef.value.searchParam as ShopOrderAfterSaleQuery).status;
  } else {
    (proTableRef.value.searchParam as ShopOrderAfterSaleQuery).status = Number(tabName);
  }
  proTableRef.value.search();
};

const handleTableReset = () => {
  activeStatusTab.value = 'all';
};

const detailVisible = ref(false);
const detailLoading = ref(false);
const detailRecord = ref<ShopOrderAfterSaleRow | null>(null);

const auditVisible = ref(false);
const auditSubmitting = ref(false);
const auditForm = ref<ShopOrderAfterSaleAuditDTO>({
  id: undefined,
  approved: true,
  auditRemark: ''
});

const receiveVisible = ref(false);
const receiveSubmitting = ref(false);
const receiveRecord = ref<ShopOrderAfterSaleRow | null>(null);
const receiveForm = ref<ShopOrderAfterSaleReceiveDTO>({
  id: undefined,
  receiveRemark: ''
});

const formatMoney = (value?: string | number | null) => `￥${Number(value || 0).toFixed(2)}`;
const formatDateTime = (value?: string) => (value ? value.replace('T', ' ') : '-');

const openDetail = async (row: ShopOrderAfterSaleRow) => {
  if (!row.id) return;
  detailVisible.value = true;
  detailLoading.value = true;
  try {
    const res = await getShopOrderAfterSaleDetailApi(row.id);
    detailRecord.value = res.data || null;
  } finally {
    detailLoading.value = false;
  }
};

const openAudit = (row: ShopOrderAfterSaleRow) => {
  auditForm.value = {
    id: row.id,
    approved: true,
    auditRemark: row.auditRemark || ''
  };
  auditVisible.value = true;
};

const submitAudit = async () => {
  if (!auditForm.value.id) {
    ElMessage.warning('售后单不存在');
    return;
  }
  if (auditForm.value.approved === undefined || auditForm.value.approved === null) {
    ElMessage.warning('请选择审核结果');
    return;
  }
  auditSubmitting.value = true;
  try {
    await auditShopOrderAfterSaleApi({
      id: auditForm.value.id,
      approved: auditForm.value.approved,
      auditRemark: auditForm.value.auditRemark?.trim() || undefined
    });
    ElMessage.success('售后审核已提交');
    auditVisible.value = false;
    proTableRef.value?.search();
  } finally {
    auditSubmitting.value = false;
  }
};

const openReceive = (row: ShopOrderAfterSaleRow) => {
  receiveRecord.value = row;
  receiveForm.value = {
    id: row.id,
    receiveRemark: row.receiveRemark || ''
  };
  receiveVisible.value = true;
};

const submitReceive = async () => {
  if (!receiveForm.value.id) {
    ElMessage.warning('售后单不存在');
    return;
  }
  receiveSubmitting.value = true;
  try {
    await receiveShopOrderAfterSaleApi({
      id: receiveForm.value.id,
      receiveRemark: receiveForm.value.receiveRemark?.trim() || undefined
    });
    ElMessage.success('退款处理完成');
    receiveVisible.value = false;
    proTableRef.value?.search();
  } finally {
    receiveSubmitting.value = false;
  }
};
</script>

<style scoped lang="scss">
.after-sale-header {
  width: 100%;
  padding: 0 2px 8px;
}

.after-sale-tabs {
  margin-bottom: 0;
}

:deep(.after-sale-tabs .el-tabs__header) {
  margin: 0;
}

:deep(.after-sale-tabs .el-tabs__nav-wrap::after) {
  height: 1px;
  background-color: var(--el-border-color-lighter);
}

:deep(.after-sale-tabs .el-tabs__item) {
  height: 34px;
  line-height: 34px;
  padding: 0 18px;
  font-size: 14px;
}

.detail-drawer {
  padding-right: 8px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.summary-card,
.timeline-item {
  padding: 16px;
  border-radius: 14px;
  background: #f6f8fb;
}

.summary-card span,
.timeline-item span {
  display: block;
  color: #909399;
  font-size: 12px;
}

.summary-card strong,
.timeline-item strong {
  display: block;
  margin-top: 8px;
  color: #303133;
  font-size: 15px;
}

.detail-section {
  margin-top: 18px;
}

.section-title {
  margin-bottom: 12px;
  color: #303133;
  font-size: 16px;
  font-weight: 600;
}

.info-grid,
.timeline-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.info-grid > div {
  padding: 14px 16px;
  border-radius: 14px;
  background: #f6f8fb;
  color: #606266;
  line-height: 1.8;
}

.info-grid span {
  color: #909399;
}

.span-2 {
  grid-column: span 2;
}

.goods-list {
  display: grid;
  gap: 12px;
}

.goods-row {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  border-radius: 14px;
  background: #f6f8fb;
}

.goods-image {
  width: 64px;
  height: 64px;
  border-radius: 10px;
  flex-shrink: 0;
  overflow: hidden;
}

.goods-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  background: #ebeef5;
}

.goods-main h4 {
  margin: 0;
  color: #303133;
  font-size: 15px;
}

.goods-main p {
  margin: 6px 0 0;
  color: #606266;
  font-size: 13px;
}

.receive-summary {
  margin-bottom: 16px;
  padding: 14px 16px;
  border-radius: 12px;
  background: #f6f8fb;
  color: #606266;
  line-height: 1.9;
}

.receive-summary p {
  margin: 0;
}

@media (max-width: 1200px) {
  .summary-grid,
  .info-grid,
  .timeline-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .summary-grid,
  .info-grid,
  .timeline-grid {
    grid-template-columns: 1fr;
  }

  .span-2 {
    grid-column: span 1;
  }

  .goods-row {
    align-items: flex-start;
  }
}
</style>
