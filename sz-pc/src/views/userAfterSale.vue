<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import {
  applyAfterSaleApi,
  getAfterSaleDetailApi,
  getAfterSalePageApi,
  getUserOrderDetailApi,
  submitAfterSaleReturnApi,
  type AfterSaleApplyParams,
  type AfterSaleItem,
  type AfterSaleReturnParams,
  type UserOrderDetail
} from '@/api';
import UserCenterScaffold from '@/components/UserCenterScaffold.vue';

const route = useRoute();
const router = useRouter();

const statusTabs = [
  { label: '全部', value: 'all' },
  { label: '待审核', value: '0' },
  { label: '待寄回', value: '1' },
  { label: '待退款', value: '2' },
  { label: '已退款', value: '3' },
  { label: '已拒绝', value: '4' }
] as const;

const fallbackImage =
  'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="120" height="120" viewBox="0 0 120 120"%3E%3Crect width="120" height="120" rx="18" fill="%23f4f4f4"/%3E%3Cpath d="M34 72h52" stroke="%23d0d0d0" stroke-width="6" stroke-linecap="round"/%3E%3Cpath d="M40 52h40" stroke="%23d0d0d0" stroke-width="6" stroke-linecap="round"/%3E%3C/svg%3E';

const listLoading = ref(false);
const activeTab = ref(resolveStatus(route.query.status));
const keyword = ref(typeof route.query.keyword === 'string' ? route.query.keyword : '');
const page = ref(resolvePage(route.query.page));
const pageSize = ref(6);
const total = ref(0);
const rows = ref<AfterSaleItem[]>([]);

const detailVisible = ref(false);
const detailLoading = ref(false);
const detailRecord = ref<AfterSaleItem | null>(null);

const applyVisible = ref(false);
const applySubmitting = ref(false);
const targetOrderLoading = ref(false);
const targetOrder = ref<UserOrderDetail | null>(null);
const applyForm = ref<AfterSaleApplyParams>({
  orderNo: '',
  afterSaleType: 1,
  applyAmount: undefined,
  reason: '',
  userRemark: ''
});

const returnVisible = ref(false);
const returnSubmitting = ref(false);
const returnForm = ref<AfterSaleReturnParams>({
  id: 0,
  returnExpressName: '',
  returnExpressNo: '',
  returnRemark: ''
});

const handledIntentKey = ref('');

const currentIntentOrderNo = computed(() => (typeof route.query.orderNo === 'string' ? route.query.orderNo.trim() : ''));
const currentIntentMode = computed(() => (route.query.mode === 'apply' ? 'apply' : ''));
const currentIntentAfterSaleId = computed(() => {
  const raw = Number(route.query.afterSaleId || 0);
  return Number.isFinite(raw) && raw > 0 ? raw : 0;
});
const targetOrderMaxRefund = computed(() => Number(targetOrder.value?.payPrice || 0));

function resolveStatus(value: unknown) {
  const current = typeof value === 'string' ? value : 'all';
  return statusTabs.some(item => item.value === current) ? current : 'all';
}

function resolvePage(value: unknown) {
  const next = Number(value || 1);
  return Number.isFinite(next) && next > 0 ? next : 1;
}

function syncRouteQuery() {
  const query: Record<string, string> = {};
  if (activeTab.value !== 'all') {
    query.status = activeTab.value;
  }
  if (keyword.value.trim()) {
    query.keyword = keyword.value.trim();
  }
  if (page.value > 1) {
    query.page = String(page.value);
  }
  if (currentIntentOrderNo.value) {
    query.orderNo = currentIntentOrderNo.value;
  }
  if (currentIntentMode.value) {
    query.mode = currentIntentMode.value;
  }
  if (currentIntentAfterSaleId.value) {
    query.afterSaleId = String(currentIntentAfterSaleId.value);
  }
  router.replace({ path: '/user/service/after-sale', query });
}

async function fetchAfterSaleList() {
  listLoading.value = true;
  try {
    const res = await getAfterSalePageApi({
      page: page.value,
      limit: pageSize.value,
      status: activeTab.value === 'all' ? undefined : Number(activeTab.value),
      keyword: keyword.value.trim() || undefined
    });
    rows.value = res.data?.rows || [];
    total.value = Number(res.data?.total || 0);
  } finally {
    listLoading.value = false;
  }
}

async function refreshList() {
  syncRouteQuery();
  await fetchAfterSaleList();
}

function formatMoney(value?: string | number | null) {
  return `￥${Number(value || 0).toFixed(2)}`;
}

function formatDateTime(value?: string) {
  return value ? value.replace('T', ' ') : '-';
}

function buildTimeline(item: AfterSaleItem) {
  const timeline = [
    { label: '发起申请', time: item.createTime, description: `用户提交${item.afterSaleTypeText || '售后申请'}` },
    { label: '商家审核', time: item.auditTime, description: item.auditRemark || '商家已处理申请' },
    { label: '买家退回', time: item.returnTime, description: item.returnExpressNo ? `已填写物流：${item.returnExpressName || '-'} / ${item.returnExpressNo}` : '' },
    { label: '商家收货', time: item.receiveTime, description: item.receiveRemark || '' },
    { label: '退款完成', time: item.refundTime, description: item.status === 3 ? '退款已退回用户账户余额' : '' }
  ];
  if (item.status === 4) {
    timeline.push({
      label: '申请被拒绝',
      time: item.auditTime,
      description: item.auditRemark || '商家已拒绝当前售后申请'
    });
  }
  return timeline.filter(entry => entry.time);
}

async function openDetail(id?: number | null) {
  if (!id) return;
  detailLoading.value = true;
  detailVisible.value = true;
  try {
    const res = await getAfterSaleDetailApi(id);
    detailRecord.value = res.data || null;
  } finally {
    detailLoading.value = false;
  }
}

async function openApply(orderNo: string) {
  if (!orderNo) return;
  targetOrderLoading.value = true;
  try {
    const res = await getUserOrderDetailApi(orderNo);
    const data = res.data;
    if (!data) {
      ElMessage.warning('订单不存在，无法发起售后');
      return;
    }
    if (!data.canApplyAfterSale) {
      if (data.afterSaleId) {
        ElMessage.info('该订单已有售后申请，已为你打开详情');
        await openDetail(data.afterSaleId);
        return;
      }
      ElMessage.warning('当前订单暂不支持申请售后');
      return;
    }

    targetOrder.value = data;
    applyForm.value = {
      orderNo: data.orderNo,
      afterSaleType: 1,
      applyAmount: Number(data.payPrice || 0),
      reason: '',
      userRemark: ''
    };
    applyVisible.value = true;
  } finally {
    targetOrderLoading.value = false;
  }
}

function closeApplyDialog() {
  applyVisible.value = false;
  targetOrder.value = null;
}

async function submitApply() {
  const reason = applyForm.value.reason.trim();
  const amount = Number(applyForm.value.applyAmount || 0);

  if (!applyForm.value.orderNo) {
    ElMessage.warning('订单号不能为空');
    return;
  }
  if (!reason) {
    ElMessage.warning('请填写售后原因');
    return;
  }
  if (reason.length > 255) {
    ElMessage.warning('售后原因不能超过255字');
    return;
  }
  if (amount <= 0) {
    ElMessage.warning('退款金额必须大于0');
    return;
  }
  if (targetOrderMaxRefund.value > 0 && amount > targetOrderMaxRefund.value) {
    ElMessage.warning('退款金额不能超过订单实付金额');
    return;
  }

  applySubmitting.value = true;
  try {
    await applyAfterSaleApi({
      ...applyForm.value,
      reason,
      userRemark: applyForm.value.userRemark?.trim() || undefined,
      applyAmount: amount
    });
    ElMessage.success('售后申请已提交');
    closeApplyDialog();
    await refreshList();
    if (currentIntentOrderNo.value) {
      router.replace({
        path: '/user/service/after-sale',
        query: {
          ...(activeTab.value !== 'all' ? { status: activeTab.value } : {}),
          ...(keyword.value.trim() ? { keyword: keyword.value.trim() } : {}),
          ...(page.value > 1 ? { page: String(page.value) } : {})
        }
      });
    }
  } finally {
    applySubmitting.value = false;
  }
}

function openReturnDialog(item: AfterSaleItem) {
  returnForm.value = {
    id: item.id,
    returnExpressName: item.returnExpressName || '',
    returnExpressNo: item.returnExpressNo || '',
    returnRemark: item.returnRemark || ''
  };
  returnVisible.value = true;
}

async function submitReturnExpress() {
  const expressName = returnForm.value.returnExpressName.trim();
  const expressNo = returnForm.value.returnExpressNo.trim();
  if (!returnForm.value.id) {
    ElMessage.warning('售后单不存在');
    return;
  }
  if (!expressName) {
    ElMessage.warning('请填写退货快递公司');
    return;
  }
  if (!expressNo) {
    ElMessage.warning('请填写退货快递单号');
    return;
  }

  returnSubmitting.value = true;
  try {
    await submitAfterSaleReturnApi({
      id: returnForm.value.id,
      returnExpressName: expressName,
      returnExpressNo: expressNo,
      returnRemark: returnForm.value.returnRemark?.trim() || undefined
    });
    ElMessage.success('退货物流已提交');
    returnVisible.value = false;
    await refreshList();
    await openDetail(returnForm.value.id);
  } finally {
    returnSubmitting.value = false;
  }
}

async function changeTab(value: (typeof statusTabs)[number]['value']) {
  if (value === activeTab.value) return;
  activeTab.value = value;
  page.value = 1;
  await refreshList();
}

async function searchList() {
  page.value = 1;
  await refreshList();
}

async function handlePageChange(nextPage: number) {
  page.value = nextPage;
  await refreshList();
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

async function handleRouteIntent() {
  const intentKey = `${currentIntentOrderNo.value}|${currentIntentMode.value}|${currentIntentAfterSaleId.value}`;
  if (!intentKey || intentKey === '||0' || handledIntentKey.value === intentKey) {
    return;
  }
  handledIntentKey.value = intentKey;

  if (currentIntentAfterSaleId.value) {
    await openDetail(currentIntentAfterSaleId.value);
    return;
  }
  if (currentIntentOrderNo.value && currentIntentMode.value === 'apply') {
    await openApply(currentIntentOrderNo.value);
  }
}

watch(
  () => [route.query.orderNo, route.query.mode, route.query.afterSaleId],
  () => {
    void handleRouteIntent();
  }
);

onMounted(async () => {
  await fetchAfterSaleList();
  await handleRouteIntent();
});
</script>

<template>
  <UserCenterScaffold
    active-menu="afterSale"
    crumb-section="售后服务"
    crumb-label="售后/退款"
    title="售后 / 退款"
    description="收货完成后可申请仅退款或退货退款。退货退款需先填写退货物流，商家确认收货后再完成退款。"
  >
    <template #toolbar-extra>
      <div v-if="currentIntentOrderNo" class="intent-toolbar" v-loading="targetOrderLoading">
        <span class="intent-order">当前订单：{{ currentIntentOrderNo }}</span>
        <el-button type="danger" round @click="openApply(currentIntentOrderNo)">发起售后</el-button>
      </div>
    </template>

    <div class="after-sale-page">
      <section v-if="targetOrder && currentIntentMode === 'apply'" class="intent-card">
        <div>
          <p class="intent-card-label">待申请售后订单</p>
          <h3>{{ targetOrder.orderNo }}</h3>
          <p class="intent-card-desc">
            当前订单实付 {{ formatMoney(targetOrder.payPrice) }}，支持 {{ targetOrder.canApplyAfterSale ? '申请售后' : '查看现有售后进度' }}。
          </p>
        </div>
        <div class="intent-card-actions">
          <el-button round @click="router.push('/user/order/list')">返回订单中心</el-button>
          <el-button type="danger" round @click="openApply(targetOrder.orderNo)">立即申请</el-button>
        </div>
      </section>

      <section class="filters-card">
        <div class="tab-row">
          <button
            v-for="tab in statusTabs"
            :key="tab.value"
            type="button"
            class="tab-btn"
            :class="{ active: activeTab === tab.value }"
            @click="changeTab(tab.value)"
          >
            {{ tab.label }}
          </button>
        </div>
        <div class="search-row">
          <el-input
            v-model="keyword"
            clearable
            placeholder="搜索售后单号或订单号"
            class="search-input"
            @keyup.enter="searchList"
          />
          <el-button type="danger" @click="searchList">搜索</el-button>
        </div>
      </section>

      <section class="list-panel" v-loading="listLoading">
        <template v-if="rows.length">
          <article v-for="item in rows" :key="item.id" class="sale-card">
            <div class="sale-head">
              <div class="sale-head-main">
                <span class="status-chip" :class="`status-${item.status}`">{{ item.statusText || '-' }}</span>
                <span class="type-chip">{{ item.afterSaleTypeText || '-' }}</span>
                <span class="meta-text">售后单号：{{ item.serviceNo || '-' }}</span>
                <span class="meta-text">订单号：{{ item.orderNo || '-' }}</span>
              </div>
              <div class="sale-amount">{{ formatMoney(item.applyAmount) }}</div>
            </div>

            <div class="goods-list">
              <div
                v-for="product in item.orderInfoList || []"
                :key="`${item.id}-${product.skuId}-${product.productId}`"
                class="goods-row"
              >
                <img class="goods-cover" :src="product.image || fallbackImage" :alt="product.productName" />
                <div class="goods-copy">
                  <h4>{{ product.productName }}</h4>
                  <p>规格：{{ product.sku || '-' }}</p>
                  <p>{{ formatMoney(product.price) }} × {{ product.cartNum || 0 }}</p>
                </div>
              </div>
            </div>

            <div class="sale-grid">
              <div class="grid-item">
                <span>申请原因</span>
                <strong>{{ item.reason || '-' }}</strong>
              </div>
              <div class="grid-item">
                <span>申请时间</span>
                <strong>{{ formatDateTime(item.createTime) }}</strong>
              </div>
              <div class="grid-item">
                <span>审核备注</span>
                <strong>{{ item.auditRemark || '-' }}</strong>
              </div>
              <div class="grid-item">
                <span>退货物流</span>
                <strong>{{ item.returnExpressNo ? `${item.returnExpressName || '-'} / ${item.returnExpressNo}` : '-' }}</strong>
              </div>
            </div>

            <div class="sale-actions">
              <el-button round @click="openDetail(item.id)">查看详情</el-button>
              <el-button
                v-if="item.canFillReturnExpress"
                type="danger"
                round
                @click="openReturnDialog(item)"
              >
                填写退货物流
              </el-button>
            </div>
          </article>
        </template>

        <el-empty v-else description="暂时还没有售后记录" />

        <div v-if="total > 0" class="pagination-wrap">
          <el-pagination
            background
            layout="prev, pager, next"
            :current-page="page"
            :page-size="pageSize"
            :total="total"
            @current-change="handlePageChange"
          />
        </div>
      </section>
    </div>

    <el-dialog v-model="applyVisible" title="申请售后" width="640px" destroy-on-close @closed="targetOrder = null">
      <div class="dialog-wrap">
        <div class="dialog-order-card" v-if="targetOrder">
          <div>
            <p class="dialog-order-label">订单号</p>
            <strong>{{ targetOrder.orderNo }}</strong>
          </div>
          <div>
            <p class="dialog-order-label">实付金额</p>
            <strong>{{ formatMoney(targetOrder.payPrice) }}</strong>
          </div>
        </div>

        <el-form label-width="96px">
          <el-form-item label="售后类型" required>
            <el-radio-group v-model="applyForm.afterSaleType">
              <el-radio :value="1">仅退款</el-radio>
              <el-radio :value="2">退货退款</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="退款金额" required>
            <el-input-number
              v-model="applyForm.applyAmount"
              class="full-input"
              :min="0.01"
              :max="targetOrderMaxRefund || undefined"
              :precision="2"
              :step="1"
              :controls-position="'right'"
            />
          </el-form-item>
          <el-form-item label="售后原因" required>
            <el-input
              v-model="applyForm.reason"
              maxlength="255"
              show-word-limit
              placeholder="请填写售后原因，例如商品破损、发错货等"
            />
          </el-form-item>
          <el-form-item label="补充说明">
            <el-input
              v-model="applyForm.userRemark"
              type="textarea"
              :rows="4"
              maxlength="500"
              show-word-limit
              placeholder="可补充说明问题细节，帮助商家尽快处理"
            />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="closeApplyDialog">取消</el-button>
        <el-button type="danger" :loading="applySubmitting" @click="submitApply">提交申请</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="returnVisible" title="填写退货物流" width="560px" destroy-on-close>
      <el-form label-width="108px">
        <el-form-item label="快递公司" required>
          <el-input v-model="returnForm.returnExpressName" placeholder="例如：顺丰、京东、圆通" />
        </el-form-item>
        <el-form-item label="快递单号" required>
          <el-input v-model="returnForm.returnExpressNo" placeholder="请输入退货快递单号" />
        </el-form-item>
        <el-form-item label="退货备注">
          <el-input
            v-model="returnForm.returnRemark"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="可填写包裹说明、寄件时间等"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="returnVisible = false">取消</el-button>
        <el-button type="danger" :loading="returnSubmitting" @click="submitReturnExpress">提交物流</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="售后详情" width="760px" destroy-on-close>
      <div v-loading="detailLoading" class="detail-wrap">
        <template v-if="detailRecord">
          <div class="detail-summary">
            <div class="summary-item">
              <span>售后单号</span>
              <strong>{{ detailRecord.serviceNo || '-' }}</strong>
            </div>
            <div class="summary-item">
              <span>售后类型</span>
              <strong>{{ detailRecord.afterSaleTypeText || '-' }}</strong>
            </div>
            <div class="summary-item">
              <span>当前状态</span>
              <strong>{{ detailRecord.statusText || '-' }}</strong>
            </div>
            <div class="summary-item">
              <span>申请金额</span>
              <strong>{{ formatMoney(detailRecord.applyAmount) }}</strong>
            </div>
          </div>

          <div class="detail-section">
            <h4>商品信息</h4>
            <div class="detail-goods-list">
              <div
                v-for="product in detailRecord.orderInfoList || []"
                :key="`${detailRecord.id}-${product.skuId}-${product.productId}`"
                class="detail-goods-row"
              >
                <img class="goods-cover" :src="product.image || fallbackImage" :alt="product.productName" />
                <div class="goods-copy">
                  <h5>{{ product.productName }}</h5>
                  <p>规格：{{ product.sku || '-' }}</p>
                  <p>{{ formatMoney(product.price) }} × {{ product.cartNum || 0 }}</p>
                </div>
              </div>
            </div>
          </div>

          <div class="detail-section">
            <h4>处理信息</h4>
            <div class="detail-info-grid">
              <div><span>申请原因：</span>{{ detailRecord.reason || '-' }}</div>
              <div><span>补充说明：</span>{{ detailRecord.userRemark || '-' }}</div>
              <div><span>审核备注：</span>{{ detailRecord.auditRemark || '-' }}</div>
              <div><span>收货备注：</span>{{ detailRecord.receiveRemark || '-' }}</div>
              <div><span>退货物流：</span>{{ detailRecord.returnExpressNo ? `${detailRecord.returnExpressName || '-'} / ${detailRecord.returnExpressNo}` : '-' }}</div>
              <div><span>退货说明：</span>{{ detailRecord.returnRemark || '-' }}</div>
            </div>
          </div>

          <div class="detail-section">
            <h4>时间轨迹</h4>
            <div v-if="buildTimeline(detailRecord).length" class="timeline-list">
              <div v-for="entry in buildTimeline(detailRecord)" :key="`${entry.label}-${entry.time}`" class="timeline-item">
                <div class="timeline-dot"></div>
                <div class="timeline-copy">
                  <div class="timeline-label">{{ entry.label }}</div>
                  <div class="timeline-time">{{ formatDateTime(entry.time) }}</div>
                  <p v-if="entry.description" class="timeline-desc">{{ entry.description }}</p>
                </div>
              </div>
            </div>
            <el-empty v-else description="暂无处理轨迹" :image-size="72" />
          </div>
        </template>
      </div>
      <template #footer>
        <el-button v-if="detailRecord?.canFillReturnExpress" type="danger" @click="detailRecord && openReturnDialog(detailRecord)">填写退货物流</el-button>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </UserCenterScaffold>
</template>

<style scoped lang="scss">
.after-sale-page {
  display: grid;
  gap: 18px;
}

.intent-toolbar {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.intent-order {
  color: #475569;
  font-size: 13px;
}

.intent-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 18px 20px;
  border: 1px solid transparent;
  box-shadow: var(--kc-shadow-sm);
  border-radius: 20px;
  background: var(--kc-surface);
}

.intent-card-label {
  margin: 0;
  color: var(--kc-accent);
  font-size: 12px;
  letter-spacing: 0.08em;
}

.intent-card h3 {
  margin: 8px 0 0;
  color: #0f172a;
  font-size: 24px;
}

.intent-card-desc {
  margin: 10px 0 0;
  color: #64748b;
  font-size: 14px;
  line-height: 1.7;
}

.intent-card-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.filters-card,
.list-panel {
  border: 1px solid transparent;
  box-shadow: var(--kc-shadow-sm);
  border-radius: 20px;
  background: #fff;
}

.filters-card {
  padding: 18px;
}

.tab-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.tab-btn {
  min-width: 88px;
  height: 40px;
  border: 1px solid transparent;
  box-shadow: var(--kc-shadow-sm);
  border-radius: 999px;
  background: #fff;
  color: #475569;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.tab-btn.active {
  border-color: #fecaca;
  background: var(--kc-accent-soft);
  color: var(--kc-accent);
  font-weight: 600;
}

.search-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
  margin-top: 16px;
}

.search-input {
  width: 100%;
}

.list-panel {
  padding: 18px;
}

.sale-card + .sale-card {
  margin-top: 14px;
}

.sale-card {
  border: 1px solid transparent;
  box-shadow: var(--kc-shadow-sm);
  border-radius: 20px;
  padding: 18px;
  background: var(--kc-surface-muted);
}

.sale-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 14px;
  }

.sale-head-main {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.status-chip,
.type-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 76px;
  height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.status-0 {
  color: #c26b00;
  background: rgba(255, 180, 70, 0.16);
}

.status-1,
.status-2 {
  color: var(--kc-accent);
  background: rgba(239, 68, 68, 0.1);
}

.status-3 {
  color: #059669;
  background: rgba(16, 185, 129, 0.12);
}

.status-4 {
  color: #6b7280;
  background: rgba(107, 114, 128, 0.12);
}

.type-chip {
  color: #7c3aed;
  background: rgba(124, 58, 237, 0.1);
}

.meta-text {
  color: #64748b;
  font-size: 13px;
}

.sale-amount {
  color: var(--kc-accent);
  font-size: 28px;
  font-weight: 700;
  white-space: nowrap;
}

.goods-list {
  display: grid;
  gap: 12px;
  margin-top: 16px;
}

.goods-row,
.detail-goods-row {
  display: flex;
  align-items: center;
  gap: 14px;
}

.goods-cover {
  width: 84px;
  height: 84px;
  border-radius: 16px;
  object-fit: cover;
  background: #f8fafc;
  border: 1px solid transparent;
  box-shadow: var(--kc-shadow-sm);
  flex-shrink: 0;
}

.goods-copy {
  min-width: 0;
}

.goods-copy h4,
.goods-copy h5 {
  margin: 0;
  color: #0f172a;
  font-size: 15px;
  line-height: 1.7;
}

.goods-copy p {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 13px;
}

.sale-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 16px;
}

.grid-item {
  padding: 14px 16px;
  border-radius: 16px;
  background: #f8fafc;
}

.grid-item span {
  display: block;
  color: #94a3b8;
  font-size: 12px;
}

.grid-item strong {
  display: block;
  margin-top: 8px;
  color: #0f172a;
  font-size: 14px;
  line-height: 1.7;
  word-break: break-word;
}

.sale-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 16px;
  flex-wrap: wrap;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.dialog-wrap {
  display: grid;
  gap: 18px;
}

.dialog-order-card {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  padding: 16px;
  border-radius: 18px;
  background: #f8fafc;
}

.dialog-order-label {
  margin: 0;
  color: #94a3b8;
  font-size: 12px;
}

.dialog-order-card strong {
  display: block;
  margin-top: 8px;
  color: #0f172a;
  font-size: 18px;
}

.full-input {
  width: 100%;
}

.detail-wrap {
  min-height: 160px;
}

.detail-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.summary-item {
  padding: 16px;
  border-radius: 16px;
  background: #f8fafc;
}

.summary-item span {
  display: block;
  color: #94a3b8;
  font-size: 12px;
}

.summary-item strong {
  display: block;
  margin-top: 8px;
  color: #0f172a;
  font-size: 15px;
}

.detail-section {
  margin-top: 20px;
}

.detail-section h4 {
  margin: 0 0 12px;
  color: #0f172a;
  font-size: 16px;
}

.detail-goods-list {
  display: grid;
  gap: 12px;
}

.detail-info-grid {
  display: grid;
  gap: 10px;
  padding: 16px;
  border-radius: 18px;
  background: #f8fafc;
  color: #334155;
  font-size: 14px;
}

.detail-info-grid span {
  color: #94a3b8;
}

.timeline-list {
  display: grid;
  gap: 12px;
}

.timeline-item {
  display: flex;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 16px;
  background: #f8fafc;
}

.timeline-dot {
  width: 10px;
  height: 10px;
  margin-top: 6px;
  border-radius: 50%;
  background: var(--kc-accent);
  flex-shrink: 0;
}

.timeline-copy {
  min-width: 0;
}

.timeline-label {
  color: #0f172a;
  font-size: 14px;
  font-weight: 600;
}

.timeline-time,
.timeline-desc {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.7;
}

@media (max-width: 900px) {
  .intent-card,
  .sale-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .sale-grid,
  .dialog-order-card,
  .detail-summary {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .search-row {
    grid-template-columns: 1fr;
  }

  .goods-row,
  .detail-goods-row {
    align-items: flex-start;
  }

  .sale-card,
  .filters-card,
  .list-panel {
    padding: 14px;
  }
}
</style>
