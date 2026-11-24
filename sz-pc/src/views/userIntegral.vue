<script setup lang="ts">
import { onMounted, ref } from 'vue';
import {
  getUserIntegralBillApi,
  getUserIntegralSummaryApi,
  type UserIntegralBillItem,
  type UserIntegralSummary
} from '@/api';
import UserCenterScaffold from '@/components/UserCenterScaffold.vue';

const summaryLoading = ref(false);
const listLoading = ref(false);
const summary = ref<UserIntegralSummary>({
  currentIntegral: 0,
  totalGainIntegral: 0,
  totalUseIntegral: 0,
  freezeIntegral: 0
});
const rows = ref<UserIntegralBillItem[]>([]);
const page = ref(1);
const pageSize = ref(10);
const total = ref(0);

const formatDateTime = (value?: string) => (value ? value.replace('T', ' ') : '-');
const resolveBillTone = (item: UserIntegralBillItem) => (Number(item.pm || 0) === 1 ? 'income' : 'expense');
const resolveBillAmount = (item: UserIntegralBillItem) => {
  const prefix = Number(item.pm || 0) === 1 ? '+' : '-';
  return `${prefix}${Number(item.number || 0)}`;
};

const fetchSummary = async () => {
  summaryLoading.value = true;
  try {
    const res = await getUserIntegralSummaryApi();
    summary.value = res.data || summary.value;
  } finally {
    summaryLoading.value = false;
  }
};

const fetchBills = async () => {
  listLoading.value = true;
  try {
    const res = await getUserIntegralBillApi({ page: page.value, limit: pageSize.value });
    rows.value = res.data?.rows || [];
    total.value = Number(res.data?.total || 0);
  } finally {
    listLoading.value = false;
  }
};

const handlePageChange = async (nextPage: number) => {
  page.value = nextPage;
  await fetchBills();
};

onMounted(async () => {
  await Promise.all([fetchSummary(), fetchBills()]);
});
</script>

<template>
  <UserCenterScaffold
    active-menu="integral"
    crumb-section="我的钱包"
    crumb-label="我的积分"
    title="我的积分"
    description="积分会在订单确认收货后按商品金额 1:100 比例发放，并记录积分收支明细。"
  >
    <div class="integral-page">
      <section class="summary-grid" v-loading="summaryLoading">
        <article class="summary-card emphasis">
          <span>当前积分</span>
          <strong>{{ summary.currentIntegral }}</strong>
        </article>
        <article class="summary-card">
          <span>累计积分</span>
          <strong>{{ summary.totalGainIntegral }}</strong>
        </article>
        <article class="summary-card">
          <span>消耗积分</span>
          <strong>{{ summary.totalUseIntegral }}</strong>
        </article>
        <article class="summary-card muted">
          <span>冻结积分</span>
          <strong>{{ summary.freezeIntegral }}</strong>
        </article>
      </section>

      <section class="bill-panel" v-loading="listLoading">
        <div v-if="rows.length" class="bill-list">
          <article v-for="item in rows" :key="item.id" class="bill-item">
            <div class="bill-main">
              <h3>{{ item.title || '积分明细' }}</h3>
              <p class="bill-mark">{{ item.mark || '暂无备注' }}</p>
              <p class="bill-time">{{ formatDateTime(item.createTime) }}</p>
            </div>
            <div class="bill-side">
              <strong :class="resolveBillTone(item)">{{ resolveBillAmount(item) }}</strong>
              <span>剩余 {{ item.balance || 0 }} 积分</span>
            </div>
          </article>
        </div>
        <el-empty v-else description="暂无积分明细哟~" />

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
  </UserCenterScaffold>
</template>

<style scoped lang="scss">
.integral-page {
  display: grid;
  gap: 18px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.summary-card {
  border: 1px solid transparent;
  box-shadow: var(--kc-shadow-sm);
  border-radius: 18px;
  padding: 18px 20px;
  background: var(--kc-surface-muted);
}

.summary-card span {
  display: block;
  color: #64748b;
  font-size: 13px;
}

.summary-card strong {
  display: block;
  margin-top: 12px;
  color: #0f172a;
  font-size: 32px;
  line-height: 1;
}

.summary-card.emphasis strong {
  color: var(--kc-accent);
}

.summary-card.muted {
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
}

.bill-panel {
  border: 1px solid transparent;
  box-shadow: var(--kc-shadow-sm);
  border-radius: 20px;
  padding: 18px;
  background: #fff;
}

.bill-list {
  display: grid;
  gap: 12px;
}

.bill-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px;
  border: 1px solid transparent;
  box-shadow: var(--kc-shadow-sm);
  border-radius: 18px;
  background: var(--kc-surface-muted);
}

.bill-main h3 {
  margin: 0;
  color: #0f172a;
  font-size: 16px;
}

.bill-mark,
.bill-time {
  margin: 8px 0 0;
  color: #64748b;
  font-size: 13px;
}

.bill-side {
  min-width: 150px;
  text-align: right;
}

.bill-side strong {
  display: block;
  font-size: 24px;
  line-height: 1;
}

.bill-side strong.expense {
  color: var(--kc-accent);
}

.bill-side strong.income {
  color: #059669;
}

.bill-side span {
  display: block;
  margin-top: 10px;
  color: #94a3b8;
  font-size: 13px;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

@media (max-width: 1024px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .summary-grid {
    grid-template-columns: 1fr;
  }

  .bill-item {
    align-items: flex-start;
    flex-direction: column;
  }

  .bill-side {
    min-width: 0;
    text-align: left;
  }
}
</style>
