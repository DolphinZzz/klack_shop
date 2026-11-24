<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import {
  confirmUserOrderReceiptApi,
  createCartItemApi,
  deleteUserOrderApi,
  getUserOrderListApi,
  submitUserOrderEvaluateApi,
  type UserOrderEvaluateParams,
  type UserOrderItem
} from '@/api';
import { useCartStore } from '@/stores/modules/cart';
import UserCenterScaffold from '@/components/UserCenterScaffold.vue';
import ProductPager from '@/components/ProductPager.vue';
import { showConfirmDialog } from '@/utils/confirm';

const route = useRoute();
const router = useRouter();
const cartStore = useCartStore();

const fallbackImage =
  'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="120" height="120" viewBox="0 0 120 120"%3E%3Crect width="120" height="120" rx="10" fill="%23f4f4f2"/%3E%3Cpath d="M34 72h52" stroke="%23c9c9c0" stroke-width="5" stroke-linecap="round"/%3E%3Cpath d="M40 52h40" stroke="%23c9c9c0" stroke-width="5" stroke-linecap="round"/%3E%3C/svg%3E';

const orderTabs = [
  { label: '全部订单', value: 'all' },
  { label: '待付款', value: 'pending_payment' },
  { label: '待发货', value: 'pending_shipment' },
  { label: '待收货', value: 'pending_receipt' },
  { label: '已取消', value: 'cancelled' }
];

const loading = ref(false);
const activeTab = ref(resolveTab(route.query.tab));
const keyword = ref(typeof route.query.keyword === 'string' ? route.query.keyword : '');
const page = ref(resolvePage(route.query.page));
const pageSize = ref(5);
const total = ref(0);
const orderList = ref<UserOrderItem[]>([]);

const rebuyingOrderNo = ref('');
const deletingOrderNo = ref('');
const evaluateVisible = ref(false);
const evaluateLoading = ref(false);
const evaluateForm = ref<UserOrderEvaluateParams>({
  orderNo: '',
  score: 5,
  content: ''
});

function resolveTab(tab: unknown) {
  const current = typeof tab === 'string' ? tab : 'all';
  return orderTabs.some(item => item.value === current) ? current : 'all';
}

function resolvePage(value: unknown) {
  const next = Number(value || 1);
  return Number.isFinite(next) && next > 0 ? next : 1;
}

function syncRouteQuery() {
  const query: Record<string, string> = { tab: activeTab.value };
  if (keyword.value.trim()) {
    query.keyword = keyword.value.trim();
  }
  if (page.value > 1) {
    query.page = String(page.value);
  }
  router.replace({ path: '/user/order/list', query });
}

async function fetchOrders() {
  loading.value = true;
  try {
    const res = await getUserOrderListApi({
      page: page.value,
      limit: pageSize.value,
      tab: activeTab.value,
      keyword: keyword.value.trim() || undefined
    });
    const data = res.data;
    orderList.value = data?.rows || [];
    total.value = Number(data?.total || 0);
  } finally {
    loading.value = false;
  }
}

async function fetchPageData() {
  syncRouteQuery();
  await fetchOrders();
}

function formatPrice(value?: string | number | null) {
  return Number(value || 0).toFixed(2);
}

function formatDateTime(value?: string) {
  if (!value) return '-';
  return value.replace('T', ' ');
}

function toProduct(productId?: number) {
  if (!productId) return;
  router.push(`/product/${productId}`);
}

async function handleTabChange(name: string | number) {
  activeTab.value = String(name);
  page.value = 1;
  await fetchPageData();
}

async function onSearch() {
  page.value = 1;
  await fetchPageData();
}

async function handlePageChange(nextPage: number) {
  page.value = nextPage;
  await fetchPageData();
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

function goPay(orderNo: string) {
  router.push({ path: '/order_payment', query: { orderNo } });
}

async function buyAgain(order: UserOrderItem) {
  const items = (order.orderInfoList || []).filter(item => item.productId && item.skuId);
  if (!items.length) {
    ElMessage.warning('当前订单缺少可再次购买的商品');
    return;
  }

  rebuyingOrderNo.value = order.orderNo;
  try {
    for (const item of items) {
      await createCartItemApi({
        productId: item.productId,
        skuId: item.skuId,
        cartNum: Math.max(1, Number(item.cartNum || 1))
      });
    }
    await cartStore.fetchCount();
    ElMessage.success('外设已重新加入购物车');
    router.push('/cart');
  } finally {
    rebuyingOrderNo.value = '';
  }
}

async function removeOrder(order: UserOrderItem) {
  try {
    await showConfirmDialog('确认删除该订单吗？删除后将不再显示。', '删除确认', { type: 'warning' });
  } catch {
    return;
  }

  deletingOrderNo.value = order.orderNo;
  try {
    await deleteUserOrderApi(order.orderNo);
    ElMessage.success('订单已删除');
    if (orderList.value.length === 1 && page.value > 1) {
      page.value -= 1;
    }
    await fetchPageData();
  } finally {
    deletingOrderNo.value = '';
  }
}

function openDetail(orderNo: string) {
  router.push({ path: '/user/order/detail', query: { orderNo } });
}

function openAfterSale(order: UserOrderItem) {
  if (order.canApplyAfterSale) {
    router.push({
      path: '/user/service/after-sale',
      query: { orderNo: order.orderNo, mode: 'apply' }
    });
    return;
  }
  if (order.afterSaleId) {
    router.push({
      path: '/user/service/after-sale',
      query: { afterSaleId: String(order.afterSaleId) }
    });
  }
}

function openEvaluate(orderNo: string) {
  evaluateForm.value = {
    orderNo,
    score: 5,
    content: ''
  };
  evaluateVisible.value = true;
}

async function confirmReceipt(orderNo: string) {
  try {
    await showConfirmDialog('确认当前订单已签收吗？确认后可进行评价。', '签收确认', { type: 'warning' });
  } catch {
    return;
  }

  await confirmUserOrderReceiptApi(orderNo);
  ElMessage.success('已确认收货');
  await fetchPageData();
  openEvaluate(orderNo);
}

async function submitEvaluate() {
  const content = evaluateForm.value.content.trim();
  if (!content) {
    ElMessage.warning('请输入评价内容');
    return;
  }

  evaluateLoading.value = true;
  try {
    await submitUserOrderEvaluateApi({
      orderNo: evaluateForm.value.orderNo,
      score: evaluateForm.value.score || 5,
      content
    });
    ElMessage.success('评价提交成功');
    evaluateVisible.value = false;
    await fetchPageData();
  } finally {
    evaluateLoading.value = false;
  }
}

onMounted(async () => {
  await Promise.all([fetchOrders(), cartStore.fetchCount()]);
});
</script>

<template>
  <UserCenterScaffold
    active-menu="orders"
    crumb-section="订单与评价"
    crumb-label="我的订单"
    title="我的订单"
    description="追踪你的外设定制订单状态、物流进度与售后服务。"
  >
    <template #toolbar-extra>
      <div class="inline-search">
        <el-input
          v-model="keyword"
          placeholder="搜索外设名称或订单号"
          clearable
          @keyup.enter="onSearch"
          @clear="onSearch"
        />
        <el-button type="primary" @click="onSearch">检索</el-button>
      </div>
    </template>

    <div v-loading="loading">
      <el-tabs v-model="activeTab" class="order-tabs" @tab-change="handleTabChange">
        <el-tab-pane v-for="item in orderTabs" :key="item.value" :label="item.label" :name="item.value" />
      </el-tabs>

      <div v-if="orderList.length" class="order-list">
        <article v-for="order in orderList" :key="order.orderNo" class="order-card">
          <div class="order-card-head">
            <div class="head-meta">
              <span class="status-badge" :class="`status-${order.displayTab}`">{{ order.displayStatus }}</span>
              <span class="type-badge kc-mono">
                {{ order.orderTypeLabel || (Number(order.orderType || 0) === 1 ? '闪购' : '常规') }}
              </span>
              <span class="meta-text kc-mono">{{ formatDateTime(order.createTime) }}</span>
              <span class="meta-text kc-mono">NO. {{ order.orderNo }}</span>
            </div>
            <strong class="head-total kc-mono">¥{{ formatPrice(order.payPrice) }}</strong>
          </div>

          <div class="order-card-body">
            <div class="product-list">
              <div
                v-for="item in order.orderInfoList"
                :key="`${order.orderNo}-${item.skuId}-${item.productId}`"
                class="product-row"
              >
                <img
                  class="product-cover"
                  :src="item.image || fallbackImage"
                  :alt="item.productName"
                  @click="toProduct(item.productId)"
                />
                <div class="product-copy">
                  <button type="button" class="product-name" @click="toProduct(item.productId)">
                    {{ item.productName }}
                  </button>
                  <p class="product-sku kc-mono">{{ item.sku || '标准配置' }}</p>
                  <p class="product-meta kc-mono">¥{{ formatPrice(item.price) }} × {{ item.cartNum || 0 }}</p>
                </div>
              </div>
            </div>

            <div class="order-actions">
              <el-button v-if="order.canApplyAfterSale" size="small" @click="openAfterSale(order)">申请售后</el-button>
              <el-button v-else-if="order.afterSaleId" size="small" @click="openAfterSale(order)">查看售后</el-button>
              <el-button v-if="order.canPay" type="danger" size="small" @click="goPay(order.orderNo)">立即支付</el-button>
              <el-button
                v-else-if="order.canConfirmReceipt"
                type="danger"
                size="small"
                @click="confirmReceipt(order.orderNo)"
              >
                确认收货
              </el-button>
              <el-button
                v-else-if="order.canEvaluate"
                type="danger"
                size="small"
                @click="openEvaluate(order.orderNo)"
              >
                去评价
              </el-button>
              <el-button
                v-else-if="order.canBuyAgain"
                type="primary"
                size="small"
                :loading="rebuyingOrderNo === order.orderNo"
                @click="buyAgain(order)"
              >
                再次购买
              </el-button>
              <el-button
                v-if="order.canDelete"
                size="small"
                :loading="deletingOrderNo === order.orderNo"
                @click="removeOrder(order)"
              >
                删除
              </el-button>
              <el-button size="small" @click="openDetail(order.orderNo)">详情</el-button>
            </div>
          </div>
        </article>
      </div>

      <el-empty v-else description="暂无符合条件的订单" />

      <ProductPager v-if="total > pageSize" :current="page" :page-size="pageSize" :total="total" @change="handlePageChange" />
    </div>

    <el-dialog v-model="evaluateVisible" title="订单评价" width="520px" destroy-on-close>
      <div class="evaluate-dialog">
        <p class="evaluate-order-no kc-mono">ORDER NO. {{ evaluateForm.orderNo || '-' }}</p>
        <div class="evaluate-field">
          <span class="evaluate-label">外设评分</span>
          <el-rate v-model="evaluateForm.score" />
        </div>
        <div class="evaluate-field block">
          <span class="evaluate-label">体验反馈</span>
          <el-input
            v-model="evaluateForm.content"
            type="textarea"
            :rows="5"
            maxlength="200"
            show-word-limit
            placeholder="分享手感、做工、音质或桌搭使用体验..."
          />
        </div>
      </div>
      <template #footer>
        <el-button @click="evaluateVisible = false">取消</el-button>
        <el-button type="danger" :loading="evaluateLoading" @click="submitEvaluate">提交评价</el-button>
      </template>
    </el-dialog>
  </UserCenterScaffold>
</template>

<style scoped lang="scss">
.inline-search {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 300px;
}

.order-tabs {
  margin-bottom: 16px;
}

.order-tabs :deep(.el-tabs__nav-wrap::after) {
  display: none;
}

.order-tabs :deep(.el-tabs__item.is-active) {
  color: var(--kc-text);
  font-weight: 700;
}

.order-tabs :deep(.el-tabs__active-bar) {
  background-color: var(--kc-accent);
  border-radius: 999px;
}

.order-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.order-card {
  padding: 16px 18px;
  border-radius: 18px;
  background: var(--kc-surface-muted);
  transition: box-shadow 160ms ease, background-color 160ms ease;
}

.order-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 14px;
}

.head-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.status-badge {
  padding: 4px 10px;
  border-radius: 999px;
  background: var(--kc-carbon);
  color: #ffffff;
  font-size: 11px;
  font-weight: 700;
}

.status-badge.status-pending_payment {
  background: var(--kc-accent);
}

.status-badge.status-cancelled {
  background: #8c919f;
}

.type-badge {
  padding: 3px 9px;
  border-radius: 999px;
  background: var(--kc-surface);
  color: var(--kc-text-secondary);
  font-size: 11px;
}

.meta-text {
  font-size: 12px;
  color: var(--kc-text-muted);
}

.head-total {
  font-size: 18px;
  color: var(--kc-accent);
}

.order-card-body {
  padding: 14px 16px;
  border-radius: 14px;
  background: var(--kc-surface);
  box-shadow: var(--kc-shadow-sm);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
}

.product-list {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.product-row {
  display: flex;
  align-items: center;
  gap: 14px;
}

.product-cover {
  width: 68px;
  height: 68px;
  object-fit: cover;
  border-radius: 12px;
  outline: 1px solid oklch(0 0 0 / 0.08);
  outline-offset: -1px;
  cursor: pointer;
  flex-shrink: 0;
}

.product-copy {
  min-width: 0;
}

.product-name {
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--kc-text);
  font-size: 14px;
  font-weight: 600;
  text-align: left;
  cursor: pointer;
}

.product-sku,
.product-meta {
  margin: 5px 0 0;
  font-size: 12px;
  color: var(--kc-text-muted);
}

.order-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
  flex-shrink: 0;
}

.order-actions :deep(.el-button) {
  border-radius: 999px;
}

.order-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.evaluate-order-no {
  margin: 0 0 14px;
  font-size: 12px;
  color: var(--kc-text-muted);
}

.evaluate-field {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}

.evaluate-field.block {
  flex-direction: column;
  align-items: stretch;
}

.evaluate-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--kc-text);
}

@media (hover: hover) {
  .product-name:hover {
    color: var(--kc-accent);
  }
}

@media (max-width: 768px) {
  .inline-search {
    width: 100%;
  }

  .order-card-body {
    flex-direction: column;
    align-items: stretch;
  }

  .order-actions {
    justify-content: flex-start;
  }
}
</style>
