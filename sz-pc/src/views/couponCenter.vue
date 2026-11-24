<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import {
  getCouponCenterPageApi,
  grabCouponApi,
  type CouponCenterItem
} from '@/api';
import SiteHeader from '@/components/SiteHeader.vue';
import SiteMainNav from '@/components/SiteMainNav.vue';
import SiteFooter from '@/components/SiteFooter.vue';
import ProductPager from '@/components/ProductPager.vue';
import { siteNavList } from '@/utils/userCenter';
import { useCartStore } from '@/stores/modules/cart';

const router = useRouter();
const cartStore = useCartStore();

const loading = ref(false);
const grabbingId = ref<number | null>(null);
const page = ref(1);
const pageSize = ref(9);
const total = ref(0);
const couponList = ref<CouponCenterItem[]>([]);

const availableCount = computed(() => couponList.value.filter(item => canReceive(item)).length);
const receivedCount = computed(() => couponList.value.filter(item => hasReceived(item)).length);

const toMineCoupon = () => router.push('/user/wallet/coupon');

const formatMoney = (value?: string | number | null) => Number(value || 0).toFixed(0);
const formatDate = (value?: string) => (value ? String(value).slice(0, 10) : '-');
const hasReceived = (item: CouponCenterItem) => Number(item.couponUserId || 0) > 0 || Number(item.received || 0) > 0;
const isGeneralCoupon = (item: CouponCenterItem) => {
  const raw = String(item.cateKey || '').trim();
  if (!raw) return true;
  const parts = raw.split(',').map(part => part.trim()).filter(Boolean);
  return parts.length === 0 || parts.includes('0');
};
const canReceive = (item: CouponCenterItem) => !hasReceived(item) && Number(item.lastTotal || 0) > 0;
const resolveCouponTag = (item: CouponCenterItem) => (isGeneralCoupon(item) ? '全场通用' : '指定外设');
const resolveThresholdText = (item: CouponCenterItem) => {
  const minMoney = Number(item.minMoney || 0);
  return minMoney > 0 ? `满 ¥${formatMoney(item.minMoney)} 可用` : '无门槛直减';
};
const resolveScopeText = (item: CouponCenterItem) => {
  if (isGeneralCoupon(item)) return '适用于咔嗒造物全场外设商品';
  return item.productNames?.trim() || '适用于指定客制化外设商品';
};
const resolveTimeText = (item: CouponCenterItem) => {
  const start = formatDate(item.useStartTime || item.couponStartTime);
  const end = formatDate(item.useEndTime || item.couponEndTime);
  if (start === '-' && end === '-') return '领取后尽快使用';
  if (start === '-') return `有效期至 ${end}`;
  if (end === '-') return `${start} 起可用`;
  return `${start} — ${end}`;
};
const resolveButtonText = (item: CouponCenterItem) => {
  if (hasReceived(item)) return '已领取';
  if (Number(item.lastTotal || 0) <= 0) return '已领完';
  return '立即领取';
};
const resolveCardClass = (item: CouponCenterItem) => {
  if (hasReceived(item)) return 'received';
  if (Number(item.lastTotal || 0) <= 0) return 'empty';
  return 'ready';
};

const fetchCoupons = async () => {
  loading.value = true;
  try {
    const res = await getCouponCenterPageApi({
      page: page.value,
      limit: pageSize.value
    });
    couponList.value = res.data?.rows || [];
    total.value = Number(res.data?.total || 0);
  } finally {
    loading.value = false;
  }
};

const grabCoupon = async (item: CouponCenterItem) => {
  if (!canReceive(item) || grabbingId.value === item.id) return;
  grabbingId.value = item.id;
  try {
    await grabCouponApi(item.id);
    ElMessage.success('领取成功，已存入我的优惠券');
    await fetchCoupons();
  } finally {
    grabbingId.value = null;
  }
};

const handlePageChange = async (nextPage: number) => {
  page.value = nextPage;
  await fetchCoupons();
  window.scrollTo({ top: 0, behavior: 'smooth' });
};

onMounted(async () => {
  await Promise.all([fetchCoupons(), cartStore.fetchCount()]);
});
</script>

<template>
  <div class="kc-page">
    <SiteHeader />
    <SiteMainNav :items="siteNavList" active-key="coupon" auto-navigate />

    <main class="kc-main voucher-main">
      <section class="voucher-hero kc-surface">
        <div class="hero-left">
          <span class="kc-kicker">VOUCHER WORKSHOP</span>
          <h1>礼券工坊</h1>
          <p>全场通用券与指定外设专属券集中领取，领取后自动同步至会员资产，结账时可直接抵扣。</p>
          <div class="hero-actions">
            <button type="button" class="hero-cta" @click="toMineCoupon">查看我的优惠券 →</button>
          </div>
        </div>

        <div class="hero-right kc-mono">
          <div class="stat-tile">
            <span>AVAILABLE</span>
            <strong>{{ String(availableCount).padStart(2, '0') }}</strong>
            <small>当前可领</small>
          </div>
          <div class="stat-tile dark">
            <span>CLAIMED</span>
            <strong>{{ String(receivedCount).padStart(2, '0') }}</strong>
            <small>已领券张</small>
          </div>
        </div>
      </section>

      <section class="voucher-list-wrap" v-loading="loading">
        <div class="section-bar">
          <div>
            <span class="kc-kicker">ACTIVE VOUCHERS</span>
            <h2>可领取优惠券</h2>
          </div>
          <button type="button" class="mine-link" @click="toMineCoupon">我的优惠券 →</button>
        </div>

        <div v-if="couponList.length" class="voucher-grid">
          <article v-for="item in couponList" :key="item.id" class="voucher-ticket" :class="resolveCardClass(item)">
            <div class="ticket-main">
              <div class="ticket-face">
                <div class="face-amount kc-mono">
                  <span class="yen">¥</span>
                  <strong>{{ formatMoney(item.money) }}</strong>
                </div>
                <span class="face-threshold">{{ resolveThresholdText(item) }}</span>
              </div>

              <div class="ticket-info">
                <div class="info-top">
                  <span class="scope-badge">{{ resolveCouponTag(item) }}</span>
                  <h3 :title="item.name">{{ item.name || '工坊优惠券' }}</h3>
                </div>
                <p class="scope-desc">{{ resolveScopeText(item) }}</p>
                <div class="info-foot kc-mono">
                  <span>{{ resolveTimeText(item) }}</span>
                  <span>余 {{ Number(item.lastTotal || 0) }}/{{ Number(item.total || 0) }}</span>
                </div>
              </div>
            </div>

            <button
              type="button"
              class="ticket-stub"
              :disabled="!canReceive(item) || grabbingId === item.id"
              @click="grabCoupon(item)"
            >
              <span>{{ grabbingId === item.id ? '领取中' : resolveButtonText(item) }}</span>
            </button>
          </article>
        </div>

        <div v-else class="voucher-empty kc-surface">
          <p>当前暂无可领取的工坊优惠券</p>
          <button type="button" class="hero-cta" @click="toMineCoupon">查看已有优惠券</button>
        </div>

        <ProductPager v-if="total > pageSize" :current="page" :page-size="pageSize" :total="total" @change="handlePageChange" />
      </section>
    </main>

    <SiteFooter />
  </div>
</template>

<style scoped lang="scss">
.voucher-main {
  display: flex;
  flex-direction: column;
  gap: 28px;
}

.voucher-hero {
  padding: 32px 36px;
  border-radius: 26px;
  border: 1px solid transparent;
  background: var(--kc-surface);
  box-shadow: var(--kc-shadow-md);
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 28px;
  align-items: center;
}

.hero-left h1 {
  margin: 8px 0 0;
  font-size: 32px;
  letter-spacing: -0.025em;
  color: var(--kc-text);
  line-height: 1.1;
}

.hero-left p {
  margin: 10px 0 0;
  max-width: 560px;
  font-size: 14px;
  color: var(--kc-text-secondary);
  line-height: 1.75;
}

.hero-actions {
  margin-top: 20px;
}

.hero-cta {
  height: 40px;
  padding: 0 22px;
  border-radius: 999px;
  border: 0;
  background: var(--kc-carbon);
  color: #ffffff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 150ms ease, transform 100ms ease-out;
}

.hero-cta:active {
  transform: scale(0.97);
}

.hero-right {
  display: grid;
  grid-template-columns: repeat(2, 136px);
  gap: 12px;
}

.stat-tile {
  padding: 18px;
  border-radius: 18px;
  background: var(--kc-surface-muted);
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.stat-tile.dark {
  background: var(--kc-carbon);
  color: #ffffff;
  box-shadow: var(--kc-shadow-sm);
}

.stat-tile span {
  font-size: 10px;
  letter-spacing: 0.1em;
  color: var(--kc-text-muted);
}

.stat-tile strong {
  font-size: 30px;
  line-height: 1;
  color: var(--kc-accent);
}

.stat-tile.dark strong {
  color: #ffffff;
}

.stat-tile small {
  font-family: var(--kc-font-sans);
  font-size: 12px;
  color: var(--kc-text-secondary);
}

.stat-tile.dark small {
  color: #9ba0ae;
}

.section-bar {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 18px;
  padding: 0 2px;
}

.section-bar h2 {
  margin: 6px 0 0;
  font-size: 24px;
  letter-spacing: -0.02em;
  color: var(--kc-text);
}

.mine-link {
  height: 36px;
  padding: 0 16px;
  border: 0;
  border-radius: 999px;
  background: var(--kc-surface);
  box-shadow: var(--kc-shadow-sm);
  color: var(--kc-text);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.voucher-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.voucher-ticket {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 88px;
  min-height: 158px;
  border-radius: 22px;
  border: 1px solid transparent;
  background: var(--kc-surface);
  box-shadow: var(--kc-shadow-sm);
  overflow: hidden;
  transition: transform 200ms ease-out, box-shadow 200ms ease;
}

.voucher-ticket::before,
.voucher-ticket::after {
  content: '';
  position: absolute;
  right: 80px;
  width: 16px;
  height: 16px;
  border-radius: 999px;
  background: var(--kc-bg);
  z-index: 2;
}

.voucher-ticket::before {
  top: -8px;
}

.voucher-ticket::after {
  bottom: -8px;
}

.ticket-main {
  display: grid;
  grid-template-columns: 136px minmax(0, 1fr);
}

.ticket-face {
  padding: 18px 14px;
  background: var(--kc-surface-muted);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  gap: 8px;
}

.face-amount {
  color: var(--kc-accent);
  display: inline-flex;
  align-items: baseline;
  gap: 2px;
}

.face-amount .yen {
  font-size: 16px;
  font-weight: 700;
}

.face-amount strong {
  font-size: 38px;
  line-height: 1;
}

.face-threshold {
  font-size: 12px;
  color: var(--kc-text-secondary);
  font-weight: 600;
}

.ticket-info {
  padding: 18px 20px;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.info-top {
  display: flex;
  align-items: center;
  gap: 8px;
}

.scope-badge {
  padding: 3px 9px;
  border-radius: 999px;
  background: var(--kc-carbon);
  color: #ffffff;
  font-size: 11px;
  font-weight: 600;
  white-space: nowrap;
}

.info-top h3 {
  margin: 0;
  font-size: 16px;
  color: var(--kc-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.scope-desc {
  margin: 10px 0 0;
  font-size: 13px;
  color: var(--kc-text-secondary);
  line-height: 1.5;
}

.info-foot {
  margin-top: auto;
  padding-top: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  font-size: 11px;
  color: var(--kc-text-muted);
}

.ticket-stub {
  border: 0;
  background: var(--kc-accent);
  color: #ffffff;
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 0.16em;
  writing-mode: vertical-rl;
  display: grid;
  place-items: center;
  cursor: pointer;
  transition: background-color 150ms ease;
}

.voucher-ticket.received .face-amount,
.voucher-ticket.empty .face-amount {
  color: var(--kc-text-muted);
}

.voucher-ticket.received .scope-badge,
.voucher-ticket.empty .scope-badge {
  background: var(--kc-bg-subtle);
  color: var(--kc-text-muted);
}

.ticket-stub:disabled {
  background: #c8cacf;
  color: #ffffff;
  cursor: not-allowed;
}

.voucher-empty {
  padding: 56px 24px;
  border-radius: 22px;
  background: var(--kc-surface);
  box-shadow: var(--kc-shadow-sm);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
  color: var(--kc-text-muted);
}

.voucher-empty p {
  margin: 0;
}

@media (hover: hover) {
  .hero-cta:hover {
    background: var(--kc-accent);
  }

  .mine-link:hover {
    color: var(--kc-accent);
    box-shadow: var(--kc-shadow-md);
  }

  .voucher-ticket.ready:hover {
    transform: translateY(-2px);
    box-shadow: var(--kc-shadow-md);
  }

  .ticket-stub:hover:not(:disabled) {
    background: var(--kc-accent-hover);
  }
}

@media (max-width: 1024px) {
  .voucher-hero,
  .voucher-grid {
    grid-template-columns: 1fr;
  }

  .hero-right {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .voucher-hero {
    padding: 22px;
    border-radius: 20px;
  }

  .voucher-ticket {
    grid-template-columns: 1fr;
  }

  .voucher-ticket::before,
  .voucher-ticket::after {
    display: none;
  }

  .ticket-main {
    grid-template-columns: 1fr;
  }

  .ticket-stub {
    height: 46px;
    writing-mode: horizontal-tb;
    letter-spacing: 0.08em;
  }
}
</style>
