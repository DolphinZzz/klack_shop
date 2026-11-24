<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { getOrderPaymentInfoApi, getPreOrderDetailApi, orderPaymentApi, type OrderPaymentInfo } from '@/api';
import SiteHeader from '@/components/SiteHeader.vue';
import SiteFooter from '@/components/SiteFooter.vue';

const route = useRoute();
const router = useRouter();

const loading = ref(false);
const paying = ref(false);
const paymentInfo = ref<OrderPaymentInfo | null>(null);
const selectedPayType = ref('yue');
const remainingSeconds = ref(0);

let countdownTimer: number | null = null;
let expiredSyncing = false;

const parseOrderNo = () => {
  const raw = route.query.orderNo;
  if (Array.isArray(raw)) return raw[0] || '';
  return typeof raw === 'string' ? raw : '';
};

const orderNo = computed(() => parseOrderNo());

const ensureOrderAddressReady = async (currentOrderNo: string) => {
  try {
    const res = await getPreOrderDetailApi(currentOrderNo);
    if (res.data?.addressId) {
      return true;
    }
    ElMessage.warning('当前订单缺少收货地址，请先补充后再支付');
    router.replace({ path: '/order/confirm', query: { orderNo: currentOrderNo } });
    return false;
  } catch {
    ElMessage.warning('订单信息获取失败，请稍后重试');
    router.replace('/user/order/list');
    return false;
  }
};

const availableMethods = computed(() => {
  const info = paymentInfo.value;
  if (!info) return [] as Array<{ key: string; title: string; desc: string; tag: string }>;

  const methods: Array<{ key: string; title: string; desc: string; tag: string }> = [];
  if (info.enableWxpay) {
    methods.push({ key: 'wxpay', title: '微信支付', desc: '微信扫码极速结账', tag: 'WECHAT' });
  }
  if (info.enableAlipay) {
    methods.push({ key: 'alipay', title: '支付宝支付', desc: '支付宝安全收银台', tag: 'ALIPAY' });
  }
  if (info.enableYue) {
    methods.push({ key: 'yue', title: '账户余额支付', desc: `可用余额 ¥${money(info.nowMoney)}`, tag: 'BALANCE' });
  }
  return methods;
});

const canPay = computed(() => Boolean(paymentInfo.value?.canPay) && remainingSeconds.value > 0);
const isPaid = computed(() => Number(paymentInfo.value?.paid ?? 0) === 1);
const isExpired = computed(() => Number(paymentInfo.value?.paid ?? 0) === 2 || (!isPaid.value && remainingSeconds.value <= 0));
const orderTypeLabel = computed(() => (Number(paymentInfo.value?.orderType || 0) === 1 ? '闪购订单' : '常规订单'));

const pageTitle = computed(() => {
  if (isPaid.value) return '订单支付完成';
  if (isExpired.value) return '订单已超时关闭';
  return '订单已提交，请完成支付';
});

const pageDesc = computed(() => {
  if (isPaid.value) return '订单已进入工坊履约队列，可随时前往订单中心追踪物流。';
  if (isExpired.value) return '未在规定时间内完成支付，系统已自动释放库存。';
  return '请在倒计时结束前完成付款，超时后订单将自动取消。';
});

const money = (value: unknown) => {
  const num = Number(value ?? 0);
  return Number.isFinite(num) ? num.toFixed(2) : '0.00';
};

const formatCountdown = (seconds: number) => {
  const safe = Math.max(0, seconds);
  const minutes = String(Math.floor(safe / 60)).padStart(2, '0');
  const secs = String(safe % 60).padStart(2, '0');
  return `${minutes}:${secs}`;
};

const stopCountdown = () => {
  if (countdownTimer !== null) {
    window.clearInterval(countdownTimer);
    countdownTimer = null;
  }
};

const syncExpiredState = async () => {
  if (expiredSyncing || isPaid.value) return;
  expiredSyncing = true;
  try {
    await loadPaymentInfo();
  } finally {
    expiredSyncing = false;
  }
};

const startCountdown = () => {
  stopCountdown();
  if (!canPay.value) return;
  countdownTimer = window.setInterval(() => {
    if (remainingSeconds.value <= 1) {
      remainingSeconds.value = 0;
      stopCountdown();
      void syncExpiredState();
      return;
    }
    remainingSeconds.value -= 1;
  }, 1000);
};

const ensureSelectedMethod = () => {
  const methodKeys = availableMethods.value.map(item => item.key);
  if (!methodKeys.length) {
    selectedPayType.value = '';
    return;
  }
  if (!methodKeys.includes(selectedPayType.value)) {
    selectedPayType.value = methodKeys.includes('yue') ? 'yue' : methodKeys[0];
  }
};

const loadPaymentInfo = async () => {
  const currentOrderNo = orderNo.value;
  if (!currentOrderNo) {
    ElMessage.error('缺少订单号');
    router.replace('/cart');
    return;
  }

  loading.value = true;
  try {
    const addressReady = await ensureOrderAddressReady(currentOrderNo);
    if (!addressReady) return;
    const res = await getOrderPaymentInfoApi(currentOrderNo);
    paymentInfo.value = res.data;
    remainingSeconds.value = Math.max(0, Number(res.data?.remainingSeconds || 0));
    ensureSelectedMethod();
    startCountdown();
  } finally {
    loading.value = false;
  }
};

const submitPayment = async () => {
  const currentOrderNo = orderNo.value;
  if (!currentOrderNo) return;
  if (!selectedPayType.value) {
    ElMessage.warning('请选择支付方式');
    return;
  }
  if (!canPay.value) {
    ElMessage.warning('订单当前不可支付');
    return;
  }

  paying.value = true;
  try {
    const res = await orderPaymentApi({
      orderNo: currentOrderNo,
      payType: selectedPayType.value,
      payChannel: selectedPayType.value
    });
    ElMessage.success(`支付成功，支付方式：${res.data?.payType || selectedPayType.value}`);
    router.replace({
      path: '/user/order/detail',
      query: { orderNo: currentOrderNo }
    });
  } finally {
    paying.value = false;
  }
};

onMounted(() => {
  void loadPaymentInfo();
});

onBeforeUnmount(() => {
  stopCountdown();
});
</script>

<template>
  <div class="kc-page" v-loading="loading">
    <SiteHeader mode="checkout" :checkout-step="3" back-label="返回订单中心" @back="router.push('/user/order/list')" />

    <main class="kc-main pay-main">
      <section class="pay-hero">
        <div class="hero-left">
          <div class="hero-badge kc-mono">¥</div>
          <div class="hero-copy">
            <div class="chip-row kc-mono">
              <span class="type-chip">{{ orderTypeLabel }}</span>
              <span class="order-no">NO. {{ orderNo }}</span>
            </div>
            <h1>{{ pageTitle }}</h1>
            <p v-if="!isPaid && !isExpired" class="countdown-copy">
              请在 <strong class="kc-mono">{{ formatCountdown(remainingSeconds) }}</strong> 内完成支付，超时后订单将自动取消
            </p>
            <p v-else>{{ pageDesc }}</p>
          </div>
        </div>

        <div class="hero-right kc-mono">
          <span class="pay-caption">TOTAL PAYABLE</span>
          <strong class="pay-amount">¥{{ money(paymentInfo?.payPrice) }}</strong>
          <small>共 {{ paymentInfo?.totalNum || 0 }} 件外设</small>
        </div>
      </section>

      <section class="method-sheet kc-surface">
        <div class="sheet-head">
          <span class="kc-kicker">PAYMENT CHANNEL</span>
          <h2>选择支付方式</h2>
        </div>

        <div class="method-grid">
          <button
            v-for="item in availableMethods"
            :key="item.key"
            type="button"
            class="method-card"
            :class="{ active: selectedPayType === item.key }"
            :disabled="isPaid || isExpired"
            @click="selectedPayType = item.key"
          >
            <div class="method-icon" :class="item.key">{{ item.key === 'wxpay' ? '微' : item.key === 'alipay' ? '支' : '¥' }}</div>
            <div class="method-copy">
              <span class="kc-mono method-tag">{{ item.tag }}</span>
              <strong>{{ item.title }}</strong>
              <p>{{ item.desc }}</p>
            </div>
          </button>
        </div>

        <div class="pay-actions">
          <span v-if="isExpired" class="state-tip expired">订单已超时关闭，不可重复支付</span>
          <span v-else-if="isPaid" class="state-tip paid">订单已完成支付</span>
          <el-button
            type="danger"
            class="pay-submit"
            :loading="paying"
            :disabled="!canPay || isPaid || !selectedPayType"
            @click="submitPayment"
          >
            {{ isPaid ? '已完成支付' : isExpired ? '订单已失效' : `确认支付 ¥${money(paymentInfo?.payPrice)}` }}
          </el-button>
        </div>
      </section>
    </main>

    <SiteFooter />
  </div>
</template>

<style scoped lang="scss">
.pay-main {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.pay-hero {
  padding: 32px 36px;
  border-radius: 26px;
  background: var(--kc-carbon);
  color: #ffffff;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.08),
    var(--kc-shadow-lg);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
}

.hero-left {
  display: flex;
  align-items: center;
  gap: 20px;
}

.hero-badge {
  width: 62px;
  height: 62px;
  border-radius: 18px;
  background: rgba(255, 77, 0, 0.16);
  color: var(--kc-accent);
  font-size: 28px;
  font-weight: 700;
  display: grid;
  place-items: center;
  flex-shrink: 0;
}

.chip-row {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 11px;
}

.type-chip {
  padding: 4px 10px;
  border-radius: 999px;
  background: var(--kc-accent);
  color: #ffffff;
  font-weight: 700;
}

.order-no {
  color: #8c919f;
}

.hero-copy h1 {
  margin: 10px 0 0;
  font-size: 25px;
  line-height: 1.2;
}

.hero-copy p {
  margin: 8px 0 0;
  font-size: 14px;
  color: #a4a9b8;
}

.countdown-copy strong {
  color: var(--kc-accent);
  font-size: 20px;
  margin: 0 4px;
}

.hero-right {
  text-align: right;
}

.pay-caption {
  display: block;
  font-size: 10px;
  letter-spacing: 0.12em;
  color: #8c919f;
}

.pay-amount {
  display: block;
  margin-top: 6px;
  font-size: 38px;
  line-height: 1;
  color: var(--kc-accent);
}

.hero-right small {
  display: block;
  margin-top: 8px;
  font-family: var(--kc-font-sans);
  font-size: 13px;
  color: #9ba0ae;
}

.method-sheet {
  padding: 28px 32px;
  border-radius: 26px;
  border: 1px solid transparent;
  background: var(--kc-surface);
  box-shadow: var(--kc-shadow-md);
}

.sheet-head h2 {
  margin: 6px 0 0;
  font-size: 22px;
  color: var(--kc-text);
}

.method-grid {
  margin-top: 22px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.method-card {
  padding: 20px;
  border-radius: 18px;
  border: 1px solid transparent;
  background: var(--kc-surface-muted);
  display: flex;
  align-items: center;
  gap: 16px;
  text-align: left;
  cursor: pointer;
  transition: box-shadow 150ms ease, background-color 150ms ease, transform 100ms ease-out;
}

.method-card:active:not(:disabled) {
  transform: scale(0.985);
}

.method-card.active {
  background: var(--kc-surface);
  box-shadow: 0 0 0 2px var(--kc-carbon), var(--kc-shadow-md);
}

.method-card:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.method-icon {
  width: 46px;
  height: 46px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  color: #ffffff;
  font-size: 18px;
  font-weight: 700;
  flex-shrink: 0;
}

.method-icon.wxpay {
  background: #16a34a;
}

.method-icon.alipay {
  background: #0284c7;
}

.method-icon.yue {
  background: var(--kc-carbon);
}

.method-tag {
  display: block;
  font-size: 10px;
  letter-spacing: 0.1em;
  color: var(--kc-text-muted);
}

.method-copy strong {
  display: block;
  margin-top: 4px;
  font-size: 15px;
  color: var(--kc-text);
}

.method-copy p {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--kc-text-secondary);
}

.pay-actions {
  margin-top: 28px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 16px;
}

.state-tip {
  font-size: 13px;
  font-weight: 600;
}

.state-tip.expired {
  color: var(--kc-danger);
}

.state-tip.paid {
  color: var(--kc-success);
}

.pay-submit {
  min-width: 210px;
  height: 48px;
  border-radius: 999px;
  font-size: 15px;
  font-weight: 700;
}

@media (max-width: 960px) {
  .pay-hero {
    flex-direction: column;
    align-items: flex-start;
  }

  .hero-right {
    text-align: left;
  }

  .method-grid {
    grid-template-columns: 1fr;
  }
}
</style>
