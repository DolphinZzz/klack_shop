<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import {
  confirmUserOrderReceiptApi,
  createCartItemApi,
  deleteUserOrderApi,
  getUserOrderDetailApi,
  submitUserOrderEvaluateApi,
  type UserOrderDetail,
  type UserOrderEvaluateParams
} from '@/api';
import { useCartStore } from '@/stores/modules/cart';
import UserCenterScaffold from '@/components/UserCenterScaffold.vue';
import { showConfirmDialog } from '@/utils/confirm';

const route = useRoute();
const router = useRouter();
const cartStore = useCartStore();

const fallbackImage =
  'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="120" height="120" viewBox="0 0 120 120"%3E%3Crect width="120" height="120" rx="10" fill="%23f4f4f2"/%3E%3Cpath d="M34 72h52" stroke="%23c9c9c0" stroke-width="5" stroke-linecap="round"/%3E%3Cpath d="M40 52h40" stroke="%23c9c9c0" stroke-width="5" stroke-linecap="round"/%3E%3C/svg%3E';

const loading = ref(false);
const rebuying = ref(false);
const deleting = ref(false);
const detailOrder = ref<UserOrderDetail | null>(null);
const evaluateVisible = ref(false);
const evaluateLoading = ref(false);
const evaluateForm = ref<UserOrderEvaluateParams>({
  orderNo: '',
  score: 5,
  content: ''
});

const currentOrderNo = computed(() => {
  const raw = route.query.orderNo;
  if (Array.isArray(raw)) return raw[0] || '';
  return typeof raw === 'string' ? raw : '';
});

const productTotal = computed(() => {
  const list = detailOrder.value?.orderInfoList || [];
  return list.reduce((sum, item) => sum + calcItemTotal(item.price, item.cartNum), 0);
});

const statusDescription = computed(() => {
  const order = detailOrder.value;
  if (!order) return '-';
  if (order.canPay) return '订单已提交，请在有效时间内完成支付，超时后订单将自动取消。';
  if (order.canConfirmReceipt) return '外设包裹已发出，请留意物流动态，签收后可确认收货。';
  if (order.canEvaluate) return '订单已签收完成，分享你的真实使用体验与手感评测。';
  if (order.displayTab === 'pending_shipment') return '工坊正在为你质检备货，完成后将第一时间发出。';
  if (order.displayTab === 'cancelled') return '订单已取消，如需相同配置可直接点击再次购买。';
  return '订单已完成，相关配置与履约记录将持续保留。';
});

function calcItemTotal(price?: string | number | null, cartNum?: number | null) {
  return Number(price || 0) * Number(cartNum || 0);
}

function formatPrice(value?: string | number | null) {
  return Number(value || 0).toFixed(2);
}

function formatDateTime(value?: string) {
  if (!value) return '-';
  return value.replace('T', ' ');
}

function formatPayType(value?: string) {
  if (value === 'wxpay') return '微信支付';
  if (value === 'alipay') return '支付宝支付';
  if (value === 'yue') return '余额支付';
  return value || '-';
}

function toOrderList() {
  router.push('/user/order/list');
}

function toProduct(productId?: number) {
  if (!productId) return;
  router.push(`/product/${productId}`);
}

function openAfterSale() {
  const order = detailOrder.value;
  if (!order) return;
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

function goPay(orderNo: string) {
  router.push({ path: '/order_payment', query: { orderNo } });
}

async function loadOrderDetail() {
  const orderNo = currentOrderNo.value;
  if (!orderNo) {
    ElMessage.error('缺少订单号');
    router.replace('/user/order/list');
    return;
  }

  loading.value = true;
  try {
    const res = await getUserOrderDetailApi(orderNo);
    detailOrder.value = res.data || null;
  } finally {
    loading.value = false;
  }
}

async function buyAgain() {
  const order = detailOrder.value;
  if (!order) return;

  const items = (order.orderInfoList || []).filter(item => item.productId && item.skuId);
  if (!items.length) {
    ElMessage.warning('当前订单缺少可再次购买的商品');
    return;
  }

  rebuying.value = true;
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
    rebuying.value = false;
  }
}

async function removeOrder() {
  const order = detailOrder.value;
  if (!order) return;

  try {
    await showConfirmDialog('确认删除该订单吗？删除后将不再显示。', '删除确认', { type: 'warning' });
  } catch {
    return;
  }

  deleting.value = true;
  try {
    await deleteUserOrderApi(order.orderNo);
    ElMessage.success('订单已删除');
    router.replace('/user/order/list');
  } finally {
    deleting.value = false;
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
    await showConfirmDialog('确认当前订单已签收吗？确认后可进入评价。', '签收确认', { type: 'warning' });
  } catch {
    return;
  }

  await confirmUserOrderReceiptApi(orderNo);
  ElMessage.success('已确认收货');
  await loadOrderDetail();
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
    await loadOrderDetail();
  } finally {
    evaluateLoading.value = false;
  }
}

watch(currentOrderNo, (next, prev) => {
  if (!next || next === prev) return;
  void loadOrderDetail();
});

onMounted(async () => {
  await Promise.all([loadOrderDetail(), cartStore.fetchCount()]);
});
</script>

<template>
  <UserCenterScaffold
    active-menu="orders"
    crumb-section="我的订单"
    crumb-label="订单详情"
    title="订单详情"
    :description="currentOrderNo ? `订单编号：${currentOrderNo}` : ''"
  >
    <template #toolbar-extra>
      <el-button @click="toOrderList">返回订单列表</el-button>
    </template>

    <div class="detail-body" v-loading="loading">
      <template v-if="detailOrder">
        <section class="status-card">
          <div>
            <span class="status-pill">{{ detailOrder.displayStatus }}</span>
            <p class="status-desc">{{ statusDescription }}</p>
          </div>
          <div class="status-pay kc-mono">
            <span>PAID AMOUNT</span>
            <strong>¥{{ formatPrice(detailOrder.payPrice) }}</strong>
          </div>
        </section>

        <section class="info-two-col">
          <div class="info-block">
            <h3>订单基础信息</h3>
            <div class="kv-row"><span>订单编号</span><strong class="kc-mono">{{ detailOrder.orderNo }}</strong></div>
            <div class="kv-row"><span>订单类型</span><strong>{{ detailOrder.orderTypeLabel || (Number(detailOrder.orderType || 0) === 1 ? '闪购订单' : '常规订单') }}</strong></div>
            <div class="kv-row"><span>下单时间</span><strong class="kc-mono">{{ formatDateTime(detailOrder.createTime) }}</strong></div>
            <div class="kv-row"><span>支付时间</span><strong class="kc-mono">{{ formatDateTime(detailOrder.payTime) }}</strong></div>
            <div class="kv-row"><span>支付方式</span><strong>{{ formatPayType(detailOrder.payType) }}</strong></div>
            <div class="kv-row"><span>外设件数</span><strong class="kc-mono">{{ detailOrder.totalNum || 0 }} 件</strong></div>
          </div>

          <div class="info-block">
            <h3>收货与物流</h3>
            <div class="kv-row"><span>收货人</span><strong>{{ detailOrder.realName || '-' }}</strong></div>
            <div class="kv-row"><span>联系电话</span><strong class="kc-mono">{{ detailOrder.userPhone || '-' }}</strong></div>
            <div class="kv-row"><span>收货地址</span><strong>{{ detailOrder.userAddress || '-' }}</strong></div>
            <div class="kv-row"><span>物流公司</span><strong>{{ detailOrder.expressName || '-' }}</strong></div>
            <div class="kv-row"><span>快递单号</span><strong class="kc-mono">{{ detailOrder.expressNo || '-' }}</strong></div>
          </div>
        </section>

        <section class="info-block">
          <h3>外设清单</h3>
          <div class="goods-table">
            <div class="goods-head goods-grid">
              <span>外设信息</span>
              <span>单价</span>
              <span>数量</span>
              <span>小计</span>
            </div>
            <div
              v-for="item in detailOrder.orderInfoList"
              :key="`${detailOrder.orderNo}-${item.skuId}-${item.productId}`"
              class="goods-row goods-grid"
            >
              <div class="goods-item-main">
                <img :src="item.image || fallbackImage" :alt="item.productName" @click="toProduct(item.productId)" />
                <div>
                  <button type="button" class="goods-name" @click="toProduct(item.productId)">{{ item.productName }}</button>
                  <p class="goods-sku kc-mono">{{ item.sku || '标准配置' }}</p>
                </div>
              </div>
              <span class="kc-mono">¥{{ formatPrice(item.price) }}</span>
              <span class="kc-mono">×{{ item.cartNum || 0 }}</span>
              <strong class="kc-mono goods-sub">¥{{ formatPrice(calcItemTotal(item.price, item.cartNum)) }}</strong>
            </div>
          </div>
        </section>

        <section class="info-two-col">
          <div class="info-block">
            <h3>费用明细</h3>
            <div class="kv-row"><span>外设总额</span><strong class="kc-mono">¥{{ formatPrice(productTotal) }}</strong></div>
            <div class="kv-row"><span>物流运费</span><strong class="kc-mono">¥{{ formatPrice(detailOrder.freightTotalFee) }}</strong></div>
            <div class="kv-row total"><span>实付款</span><strong class="kc-mono">¥{{ formatPrice(detailOrder.payPrice) }}</strong></div>
          </div>

          <div class="info-block">
            <h3>订单履约轨迹</h3>
            <div v-if="detailOrder.orderStatusList?.length" class="timeline-list">
              <div
                v-for="(item, index) in detailOrder.orderStatusList"
                :key="`${item.changeType}-${index}`"
                class="timeline-item"
              >
                <span class="tl-dot" />
                <div>
                  <div class="tl-msg">{{ item.changeMessage || item.changeType || '-' }}</div>
                  <div class="tl-time kc-mono">{{ formatDateTime(item.createTime) }}</div>
                </div>
              </div>
            </div>
            <el-empty v-else description="暂无履约轨迹" :image-size="68" />
          </div>
        </section>

        <section class="detail-actions">
          <div class="action-total">
            合计 <strong class="kc-mono">¥{{ formatPrice(detailOrder.payPrice) }}</strong>
          </div>
          <div class="action-btns">
            <el-button v-if="detailOrder.canDelete" :loading="deleting" @click="removeOrder">删除订单</el-button>
            <el-button v-if="detailOrder.canBuyAgain" :loading="rebuying" @click="buyAgain">再次购买</el-button>
            <el-button v-if="detailOrder.canApplyAfterSale" @click="openAfterSale">申请售后</el-button>
            <el-button v-else-if="detailOrder.afterSaleId" @click="openAfterSale">查看售后</el-button>
            <el-button v-if="detailOrder.canConfirmReceipt" type="danger" @click="confirmReceipt(detailOrder.orderNo)">确认收货</el-button>
            <el-button v-if="detailOrder.canEvaluate" type="danger" @click="openEvaluate(detailOrder.orderNo)">去评价</el-button>
            <el-button v-if="detailOrder.canPay" type="danger" @click="goPay(detailOrder.orderNo)">立即支付</el-button>
          </div>
        </section>
      </template>

      <el-empty v-else description="未找到该订单信息">
        <el-button type="primary" @click="toOrderList">返回订单列表</el-button>
      </el-empty>
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
            placeholder="分享手感、做工或桌搭体验..."
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
.detail-body {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.status-card {
  padding: 22px 24px;
  border-radius: 20px;
  background: var(--kc-carbon);
  color: #ffffff;
  box-shadow: var(--kc-shadow-md);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}

.status-pill {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 999px;
  background: var(--kc-accent);
  color: #ffffff;
  font-size: 12px;
  font-weight: 700;
}

.status-desc {
  margin: 10px 0 0;
  font-size: 13px;
  color: #a4a9b8;
  line-height: 1.6;
}

.status-pay {
  text-align: right;
  flex-shrink: 0;
}

.status-pay span {
  display: block;
  font-size: 10px;
  letter-spacing: 0.12em;
  color: #8c919f;
}

.status-pay strong {
  font-size: 28px;
  color: var(--kc-accent);
}

.info-two-col {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.info-block {
  padding: 20px 22px;
  border-radius: 18px;
  background: var(--kc-surface-muted);
}

.info-block h3 {
  margin: 0 0 14px;
  font-size: 15px;
  color: var(--kc-text);
}

.kv-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 6px 0;
  font-size: 13px;
}

.kv-row span {
  color: var(--kc-text-muted);
  flex-shrink: 0;
}

.kv-row strong {
  color: var(--kc-text);
  text-align: right;
  font-weight: 600;
}

.kv-row.total {
  margin-top: 10px;
  padding: 12px 14px;
  border-radius: 12px;
  background: var(--kc-surface);
  box-shadow: var(--kc-shadow-sm);
}

.kv-row.total strong {
  font-size: 20px;
  color: var(--kc-accent);
}

.goods-table {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.goods-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 96px 64px 104px;
  align-items: center;
  gap: 12px;
}

.goods-head {
  padding: 4px 12px;
  font-size: 12px;
  color: var(--kc-text-muted);
  font-weight: 600;
}

.goods-row {
  padding: 12px 14px;
  border-radius: 14px;
  background: var(--kc-surface);
  box-shadow: var(--kc-shadow-sm);
  font-size: 13px;
  color: var(--kc-text-secondary);
}

.goods-item-main {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.goods-item-main img {
  width: 60px;
  height: 60px;
  object-fit: cover;
  border-radius: 10px;
  outline: 1px solid oklch(0 0 0 / 0.08);
  outline-offset: -1px;
  cursor: pointer;
}

.goods-name {
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--kc-text);
  font-size: 14px;
  font-weight: 600;
  text-align: left;
  cursor: pointer;
}

.goods-sku {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--kc-text-muted);
}

.goods-sub {
  color: var(--kc-text);
}

.timeline-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.timeline-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 12px;
  background: var(--kc-surface);
}

.tl-dot {
  width: 8px;
  height: 8px;
  margin-top: 5px;
  border-radius: 999px;
  background: var(--kc-accent);
  flex-shrink: 0;
}

.tl-msg {
  font-size: 13px;
  color: var(--kc-text);
}

.tl-time {
  margin-top: 3px;
  font-size: 11px;
  color: var(--kc-text-muted);
}

.detail-actions {
  padding: 16px 20px;
  border-radius: 18px;
  background: var(--kc-surface-muted);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.action-total {
  font-size: 14px;
  color: var(--kc-text-secondary);
}

.action-total strong {
  margin-left: 6px;
  font-size: 22px;
  color: var(--kc-accent);
}

.action-btns {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.action-btns :deep(.el-button) {
  border-radius: 999px;
}

.action-btns :deep(.el-button + .el-button) {
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
  .goods-name:hover {
    color: var(--kc-accent);
  }
}

@media (max-width: 860px) {
  .status-card,
  .detail-actions {
    flex-direction: column;
    align-items: flex-start;
  }

  .status-pay {
    text-align: left;
  }

  .info-two-col {
    grid-template-columns: 1fr;
  }

  .goods-head {
    display: none;
  }

  .goods-grid {
    grid-template-columns: 1fr;
  }
}
</style>
