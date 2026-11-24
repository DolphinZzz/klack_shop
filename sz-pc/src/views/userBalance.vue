<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import {
  getUserBalanceBillApi,
  getUserBalanceSummaryApi,
  type UserBalanceBillItem,
  type UserBalanceSummary
} from '@/api';
import UserCenterScaffold from '@/components/UserCenterScaffold.vue';

const router = useRouter();
const tabs = [
  { label: '全部', value: 'all' },
  { label: '支出', value: 'expend' },
  { label: '充值', value: 'recharge' },
  { label: '收入', value: 'income' }
] as const;

const summaryLoading = ref(false);
const listLoading = ref(false);
const activeTab = ref<(typeof tabs)[number]['value']>('all');
const summary = ref<UserBalanceSummary>({
  nowMoney: 0,
  rechargeAmount: 0,
  consumeAmount: 0
});
const rows = ref<UserBalanceBillItem[]>([]);
const page = ref(1);
const pageSize = ref(10);
const total = ref(0);

const formatMoney = (value?: string | number | null) => `￥${Number(value || 0).toFixed(2)}`;
const formatDateTime = (value?: string) => (value ? value.replace('T', ' ') : '-');
const resolveBillTone = (item: UserBalanceBillItem) => {
  if (item.type === 'recharge') return 'income';
  return Number(item.pm || 0) === 1 ? 'income' : 'expense';
};
const resolveBillTag = (item: UserBalanceBillItem) => {
  if (item.type === 'recharge') return '充值';
  return Number(item.pm || 0) === 1 ? '收入' : '支出';
};
const resolveBillAmount = (item: UserBalanceBillItem) => {
  const prefix = resolveBillTone(item) === 'expense' ? '-' : '+';
  return `${prefix}${formatMoney(item.number)}`;
};

const toRechargeList = () => router.push('/user/recharge/list');

const fetchSummary = async () => {
  summaryLoading.value = true;
  try {
    const res = await getUserBalanceSummaryApi();
    summary.value = res.data || summary.value;
  } finally {
    summaryLoading.value = false;
  }
};

const fetchBills = async () => {
  listLoading.value = true;
  try {
    const res = await getUserBalanceBillApi({
      page: page.value,
      limit: pageSize.value,
      tab: activeTab.value
    });
    rows.value = res.data?.rows || [];
    total.value = Number(res.data?.total || 0);
  } finally {
    listLoading.value = false;
  }
};

const changeTab = async (tab: (typeof tabs)[number]['value']) => {
  if (tab === activeTab.value) return;
  activeTab.value = tab;
  page.value = 1;
  await fetchBills();
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
    active-menu="balance"
    crumb-section="我的钱包"
    crumb-label="我的余额"
    title="我的余额"
    description="查看账户余额、累计充值、累计消费，以及余额账单明细。"
  >
    <template #toolbar-extra>
      <el-button type="danger" round @click="toRechargeList">充值</el-button>
    </template>

    <div class="wallet-balance-page">
      <section class="summary-grid" v-loading="summaryLoading">
        <article class="summary-card emphasis">
          <span>账户余额(元)</span>
          <strong>{{ formatMoney(summary.nowMoney) }}</strong>
        </article>
        <article class="summary-card">
          <span>累计充值(元)</span>
          <strong>{{ formatMoney(summary.rechargeAmount) }}</strong>
        </article>
        <article class="summary-card">
          <span>累计消费(元)</span>
          <strong>{{ formatMoney(summary.consumeAmount) }}</strong>
        </article>
      </section>

      <section class="bill-panel" v-loading="listLoading">
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

        <div v-if="rows.length" class="bill-list">
          <article v-for="item in rows" :key="item.id" class="bill-item">
            <div class="bill-main">
              <div class="bill-title-row">
                <h3>{{ item.title || '余额账单' }}</h3>
                <span class="bill-tag" :class="resolveBillTone(item)">{{ resolveBillTag(item) }}</span>
              </div>
              <p class="bill-mark">{{ item.mark || '暂无备注' }}</p>
              <p class="bill-time">{{ formatDateTime(item.createTime) }}</p>
            </div>
            <div class="bill-side">
              <strong :class="resolveBillTone(item)">{{ resolveBillAmount(item) }}</strong>
              <span>余额 {{ formatMoney(item.balance) }}</span>
            </div>
          </article>
        </div>
        <el-empty v-else description="暂无账单明细哟~" />

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
.wallet-balance-page {
  display: grid;
  gap: 18px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
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
  font-size: 30px;
  line-height: 1;
}

.summary-card.emphasis strong {
  color: var(--kc-accent);
}

.bill-panel {
  border: 1px solid transparent;
  box-shadow: var(--kc-shadow-sm);
  border-radius: 20px;
  padding: 18px;
  background: #fff;
}

.tab-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 18px;
}

.tab-btn {
  min-width: 84px;
  height: 40px;
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

.bill-main {
  min-width: 0;
}

.bill-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.bill-title-row h3 {
  margin: 0;
  color: #0f172a;
  font-size: 16px;
}

.bill-tag {
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
}

.bill-tag.expense {
  background: rgba(239, 68, 68, 0.1);
  color: var(--kc-accent);
}

.bill-tag.income {
  background: rgba(16, 185, 129, 0.12);
  color: #059669;
}

.bill-mark,
.bill-time {
  margin: 8px 0 0;
  color: #64748b;
  font-size: 13px;
}

.bill-side {
  min-width: 160px;
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
