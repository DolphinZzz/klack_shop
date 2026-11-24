<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import {
  getRechargeActivityListApi,
  userRechargeApi,
  type RechargeActivityItem
} from '@/api';
import UserCenterScaffold from '@/components/UserCenterScaffold.vue';

const router = useRouter();
const loading = ref(false);
const activityLoadingId = ref<number | null>(null);
const customLoading = ref(false);
const activityList = ref<RechargeActivityItem[]>([]);
const customAmount = ref('');

const formatMoney = (value?: string | number | null) => `￥${Number(value || 0).toFixed(2)}`;
const customPreview = computed(() => Number(customAmount.value || 0));
const resolveReceiveAmount = (item: RechargeActivityItem) =>
  Number(item.price || 0) + Number(item.givePrice || 0);

const fetchActivityList = async () => {
  loading.value = true;
  try {
    const res = await getRechargeActivityListApi();
    activityList.value = res.data || [];
  } finally {
    loading.value = false;
  }
};

const rechargeByActivity = async (item: RechargeActivityItem) => {
  activityLoadingId.value = item.id;
  try {
    await userRechargeApi({
      from: 'yue',
      price: String(item.price || ''),
      activityId: String(item.id)
    });
    ElMessage.success('充值成功，余额已更新');
    router.push('/user/wallet/balance');
  } finally {
    activityLoadingId.value = null;
  }
};

const rechargeCustom = async () => {
  const value = customAmount.value.trim();
  if (!/^\d+$/.test(value)) {
    ElMessage.warning('自定义充值金额只能输入正整数');
    return;
  }
  if (Number(value) <= 0) {
    ElMessage.warning('充值金额必须大于 0');
    return;
  }

  customLoading.value = true;
  try {
    await userRechargeApi({
      from: 'yue',
      price: value
    });
    ElMessage.success('自定义充值成功，余额已更新');
    customAmount.value = '';
    router.push('/user/wallet/balance');
  } finally {
    customLoading.value = false;
  }
};

onMounted(() => {
  void fetchActivityList();
});
</script>

<template>
  <UserCenterScaffold
    active-menu="balance"
    crumb-section="我的钱包"
    crumb-label="余额充值"
    title="余额充值"
    description="选择充值活动即可直接入账，也支持自定义整数金额充值。"
  >
    <div class="recharge-page" v-loading="loading">
      <section class="activity-panel">
        <div class="section-head">
          <h3>充值活动</h3>
          <p>点击活动直接充值到当前登录用户余额，暂不接入第三方支付。</p>
        </div>

        <div v-if="activityList.length" class="activity-grid">
          <article v-for="item in activityList" :key="item.id" class="activity-card">
            <div class="price-block">
              <span class="price-label">充值金额</span>
              <strong>{{ formatMoney(item.price) }}</strong>
            </div>

            <div class="activity-copy">
              <h4>{{ item.activityName || '充值活动' }}</h4>
              <div class="benefit-row">
                <span>赠送金额</span>
                <strong>{{ formatMoney(item.givePrice) }}</strong>
              </div>
              <div class="benefit-row total">
                <span>实际到账</span>
                <strong>{{ formatMoney(resolveReceiveAmount(item)) }}</strong>
              </div>
            </div>

            <el-button
              type="danger"
              round
              class="recharge-btn"
              :loading="activityLoadingId === item.id"
              @click="rechargeByActivity(item)"
            >
              立即充值
            </el-button>
          </article>
        </div>
        <el-empty v-else description="当前暂无充值活动" />
      </section>

      <section class="custom-panel">
        <div class="section-head">
          <h3>自定义充值</h3>
          <p>自定义充值金额仅支持正整数，不参与赠送活动。</p>
        </div>

        <div class="custom-box">
          <el-input
            v-model="customAmount"
            placeholder="请输入整数金额，例如 50"
            maxlength="8"
            clearable
          >
            <template #prepend>￥</template>
          </el-input>
          <div class="custom-preview">预计到账：{{ formatMoney(customPreview) }}</div>
          <el-button type="danger" round :loading="customLoading" @click="rechargeCustom">确认充值</el-button>
        </div>
      </section>
    </div>
  </UserCenterScaffold>
</template>

<style scoped lang="scss">
.recharge-page {
  display: grid;
  gap: 18px;
}

.activity-panel,
.custom-panel {
  border: 1px solid transparent;
  box-shadow: var(--kc-shadow-sm);
  border-radius: 20px;
  padding: 20px;
  background: #fff;
}

.section-head h3 {
  margin: 0;
  color: #0f172a;
  font-size: 20px;
}

.section-head p {
  margin: 8px 0 0;
  color: #64748b;
  font-size: 13px;
}

.activity-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-top: 18px;
}

.activity-card {
  display: grid;
  gap: 16px;
  border: 1px solid transparent;
  box-shadow: var(--kc-shadow-sm);
  border-radius: 22px;
  padding: 20px;
  background: var(--kc-surface-muted);
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.04);
}

.price-block {
  border-radius: 18px;
  padding: 16px 18px;
  background: var(--kc-accent);
  color: #fff;
}

.price-label {
  display: block;
  font-size: 12px;
  opacity: 0.92;
}

.price-block strong {
  display: block;
  margin-top: 10px;
  font-size: 34px;
  line-height: 1;
}

.activity-copy h4 {
  margin: 0 0 12px;
  color: #0f172a;
  font-size: 18px;
  line-height: 1.5;
}

.benefit-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 0;
  color: #64748b;
  font-size: 14px;
  }

.benefit-row.total {
  color: #0f172a;
  font-weight: 600;
}

.benefit-row strong {
  color: var(--kc-accent);
  font-size: 16px;
}

.recharge-btn {
  width: 100%;
}

.custom-box {
  margin-top: 18px;
  max-width: 420px;
  display: grid;
  gap: 14px;
}

.custom-preview {
  color: #475569;
  font-size: 14px;
}



@media (max-width: 700px) {
  .activity-grid {
    grid-template-columns: 1fr;
  }
}
</style>

