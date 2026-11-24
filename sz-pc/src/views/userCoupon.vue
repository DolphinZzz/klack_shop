<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { getUserCouponPageApi, type UserCouponItem } from '@/api';
import UserCenterScaffold from '@/components/UserCenterScaffold.vue';

const router = useRouter();
const loading = ref(false);
const activeTab = ref<'all' | 'unused' | 'used_expired'>('all');
const page = ref(1);
const pageSize = ref(9);
const total = ref(0);
const rows = ref<UserCouponItem[]>([]);

const tabs = [
  { label: '全部', value: 'all' },
  { label: '未使用', value: 'unused' },
  { label: '已使用/失效', value: 'used_expired' }
] as const;

const formatMoney = (value?: string | number | null) => Number(value || 0).toFixed(0);
const formatDate = (value?: string) => (value ? value.slice(0, 10) : '-');
const resolveCouponType = (item: UserCouponItem) => {
  if (Number(item.useType || 0) === 2) return '商品券';
  if (Number(item.useType || 0) === 3) return '品类券';
  return '通用券';
};
const resolveStatusText = (item: UserCouponItem) => {
  if (Number(item.status || 0) === 1) return '已使用';
  if (Number(item.status || 0) === 2) return '已失效';
  return '去使用';
};
const resolveStatusClass = (item: UserCouponItem) => {
  return Number(item.status || 0) === 0 ? 'ready' : 'disabled';
};

const fetchCoupons = async () => {
  loading.value = true;
  try {
    const res = await getUserCouponPageApi({
      page: page.value,
      limit: pageSize.value,
      tab: activeTab.value
    });
    rows.value = res.data?.rows || [];
    total.value = Number(res.data?.total || 0);
  } finally {
    loading.value = false;
  }
};

const changeTab = async (tab: 'all' | 'unused' | 'used_expired') => {
  if (tab === activeTab.value) return;
  activeTab.value = tab;
  page.value = 1;
  await fetchCoupons();
};

const handlePageChange = async (nextPage: number) => {
  page.value = nextPage;
  await fetchCoupons();
};

const useCoupon = (item: UserCouponItem) => {
  if (Number(item.status || 0) !== 0) return;
  router.push('/products');
};

onMounted(() => {
  void fetchCoupons();
});
</script>

<template>
  <UserCenterScaffold
    active-menu="coupon"
    crumb-section="我的钱包"
    crumb-label="我的优惠券"
    title="我的优惠券"
    description="查看当前账户下可用、已使用和已失效的优惠券。"
  >
    <div class="coupon-page" v-loading="loading">
      <div class="tab-row">
        <button
          v-for="tab in tabs"
          :key="tab.value"
          type="button"
          class="tab-btn"
          :class="{ active: activeTab === tab.value }"
          @click="changeTab(tab.value)"
        >
          {{ tab.label }}
        </button>
      </div>

      <div v-if="rows.length" class="coupon-grid">
        <article v-for="item in rows" :key="item.id" class="coupon-card" :class="resolveStatusClass(item)">
          <div class="coupon-main">
            <div class="price-line">
              <small>￥</small>
              <strong>{{ formatMoney(item.money) }}</strong>
            </div>
            <div class="coupon-meta">
              <span class="coupon-type">{{ resolveCouponType(item) }}</span>
              <h3>{{ item.name || '优惠券' }}</h3>
              <p>满 {{ formatMoney(item.minMoney) }} 可用</p>
              <p>有效期至 {{ formatDate(item.useEndTime || item.couponEndTime) }}</p>
            </div>
          </div>
          <button type="button" class="coupon-side" :class="resolveStatusClass(item)" @click="useCoupon(item)">
            {{ resolveStatusText(item) }}
          </button>
        </article>
      </div>
      <el-empty v-else description="暂无优惠券哟~" />

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
    </div>
  </UserCenterScaffold>
</template>

<style scoped lang="scss">
.coupon-page {
  display: grid;
  gap: 18px;
}

.tab-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.tab-btn {
  min-width: 96px;
  height: 42px;
  border: 1px solid transparent;
  box-shadow: var(--kc-shadow-sm);
  border-radius: 999px;
  background: #fff;
  color: #475569;
  font-size: 14px;
  cursor: pointer;
}

.tab-btn.active {
  border-color: #fecaca;
  background: var(--kc-accent-soft);
  color: var(--kc-accent);
  font-weight: 600;
}

.coupon-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.coupon-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 86px;
  min-height: 168px;
  border-radius: 20px;
  overflow: hidden;
  border: 1px solid transparent;
  box-shadow: var(--kc-shadow-sm);
  background: var(--kc-surface);
}

.coupon-card.disabled {
  border-color: #e5e7eb;
  background: linear-gradient(135deg, #f8fafc 0%, #ffffff 100%);
}

.coupon-main {
  padding: 18px;
}

.price-line {
  display: flex;
  align-items: baseline;
  gap: 4px;
  color: var(--kc-accent);
}

.price-line small {
  font-size: 20px;
}

.price-line strong {
  font-size: 44px;
  line-height: 1;
}

.coupon-card.disabled .price-line {
  color: #94a3b8;
}

.coupon-meta {
  margin-top: 14px;
}

.coupon-type {
  display: inline-flex;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(239, 68, 68, 0.08);
  color: var(--kc-accent);
  font-size: 12px;
}

.coupon-card.disabled .coupon-type {
  background: rgba(148, 163, 184, 0.12);
  color: #94a3b8;
}

.coupon-meta h3 {
  margin: 10px 0 8px;
  color: #0f172a;
  font-size: 18px;
}

.coupon-meta p {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 13px;
}

.coupon-side {
  border: none;
  background: var(--kc-accent);
  color: #fff;
  font-size: 24px;
  font-weight: 700;
  letter-spacing: 2px;
  cursor: pointer;
  writing-mode: vertical-rl;
  text-orientation: mixed;
}

.coupon-side.disabled {
  background: #cbd5e1;
  cursor: default;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
}

@media (max-width: 1100px) {
  .coupon-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 700px) {
  .coupon-grid {
    grid-template-columns: 1fr;
  }
}
</style>
