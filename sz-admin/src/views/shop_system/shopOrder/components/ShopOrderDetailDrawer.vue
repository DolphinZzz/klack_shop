<template>
  <el-drawer v-model="visible" :with-header="false" size="72%" destroy-on-close>
    <div v-loading="loading" class="order-detail-drawer">
      <el-button class="drawer-close" text :icon="Close" @click="visible = false" />

      <div class="summary-panel">
        <div class="summary-head">
          <div class="summary-icon">
            <el-icon><DocumentCopy /></el-icon>
          </div>
          <div class="summary-main">
            <div class="summary-title">{{ orderTypeLabel }}订单 - {{ deliveryModeLabel }}</div>
            <div class="summary-no">订单号：{{ valueOf(detail?.orderNo || detail?.orderId) }}</div>
          </div>
        </div>
        <div class="summary-metrics">
          <div class="metric-item">
            <div class="metric-label">订单状态</div>
            <div class="metric-value status">{{ orderStatusLabel }}</div>
          </div>
          <div class="metric-item">
            <div class="metric-label">实际支付</div>
            <div class="metric-value">{{ money(detail?.payFee ?? detail?.finalTotalFee) }}</div>
          </div>
          <div class="metric-item">
            <div class="metric-label">支付方式</div>
            <div class="metric-value">{{ payTypeLabel }}</div>
          </div>
          <div class="metric-item">
            <div class="metric-label">支付时间</div>
            <div class="metric-value">{{ valueOf(detail?.payTime) }}</div>
          </div>
        </div>
      </div>

      <el-tabs v-model="activeTab" class="detail-tabs">
        <el-tab-pane label="订单信息" name="orderInfo" />
        <el-tab-pane label="商品信息" name="goodsInfo" />
        <el-tab-pane label="发货记录" name="deliveryInfo" />
        <el-tab-pane label="订单记录" name="orderRecord" />
      </el-tabs>

      <div class="tab-body">
        <template v-if="activeTab === 'orderInfo'">
          <div class="section-card">
            <div class="section-title">用户信息</div>
            <div class="row two-col">
              <div>用户名：{{ valueOf(detail?.userName || detail?.realName) }}</div>
              <div>用户电话：{{ valueOf(detail?.userPhone) }}</div>
            </div>
          </div>

          <div class="section-card">
            <div class="section-title">收货信息</div>
            <div class="row three-col">
              <div>收货人：{{ valueOf(receiverName) }}</div>
              <div>收货电话：{{ valueOf(receiverPhone) }}</div>
              <div>收货地址：{{ valueOf(receiverAddress) }}</div>
            </div>
          </div>

          <div class="section-card">
            <div class="section-title">订单信息</div>
            <div class="row three-col">
              <div>商品总价：{{ money(goodsTotalAmount) }}</div>
              <div>商品总数：{{ valueOf(detail?.totalNum) }}</div>
              <div>平台优惠金额：{{ money(platformCouponAmount) }}</div>
              <div>支付状态：{{ orderStatusLabel }}</div>
              <div>实付支付：{{ money(detail?.payFee ?? detail?.finalTotalFee) }}</div>
              <div>商户优惠金额：{{ money(detail?.merchantCouponAmount) }}</div>
              <div>商户会员抵扣：{{ money(detail?.merchantVipDeductAmount) }}</div>
              <div>SVIP抵扣：{{ money(detail?.svipDeductAmount) }}</div>
              <div>支付邮费：{{ money(detail?.freightTotalFee) }}</div>
              <div>赠送积分：{{ valueOf(detail?.giftIntegral) }}</div>
              <div>积分抵扣金额：{{ money(detail?.integralDeductAmount) }}</div>
              <div>支付方式：{{ payTypeLabel }}</div>
              <div>支付时间：{{ valueOf(detail?.payTime) }}</div>
            </div>
          </div>

          <div class="section-card">
            <div class="section-title">买家留言</div>
            <div class="note-content">{{ valueOf(detail?.buyerRemark) }}</div>
          </div>

          <div class="section-card">
            <div class="section-title">商家备注</div>
            <div class="note-content">{{ valueOf(detail?.merchantRemark) }}</div>
          </div>
        </template>

        <template v-if="activeTab === 'goodsInfo'">
          <div class="table-panel">
            <div class="table-head goods-grid">
              <div>商品信息</div>
              <div>商品售价</div>
              <div>购买数量</div>
              <div>发货数量</div>
            </div>
            <div v-if="!goodsList.length" class="empty-line">-</div>
            <div v-for="(item, index) in goodsList" :key="index" class="table-row goods-grid">
              <div class="goods-cell">
                <el-image v-if="item.imageUrl" :src="item.imageUrl" fit="cover" class="goods-image" />
                <div v-else class="goods-image goods-empty">-</div>
                <div class="goods-meta">
                  <div class="goods-name">{{ valueOf(item.productName) }}</div>
                  <div class="goods-sku">规格：{{ valueOf(item.sku) }}</div>
                </div>
              </div>
              <div>{{ money(item.price) }}</div>
              <div>{{ valueOf(item.cartNum) }}</div>
              <div>{{ valueOf(item.deliveryNum ?? item.cartNum) }}</div>
            </div>
          </div>
        </template>

        <template v-if="activeTab === 'deliveryInfo'">
          <div class="table-panel">
            <div class="delivery-banner">
              <div>[{{ valueOf(detail?.expressName || '无需配送') }}] {{ valueOf(deliveryTime) }}</div>
              <div class="delivery-action">配送信息</div>
            </div>
            <div v-if="!goodsList.length" class="empty-line">-</div>
            <div v-for="(item, index) in goodsList" :key="index" class="table-row delivery-grid">
              <div class="goods-cell">
                <el-image v-if="item.imageUrl" :src="item.imageUrl" fit="cover" class="goods-image" />
                <div v-else class="goods-image goods-empty">-</div>
                <div class="goods-meta">
                  <div class="goods-name">{{ valueOf(item.productName) }}</div>
                  <div class="goods-sku">规格：{{ valueOf(item.sku) }}</div>
                </div>
              </div>
              <div>X {{ valueOf(item.cartNum) }}</div>
              <div>发货备注：{{ valueOf(detail?.deliveryRemark) }}</div>
            </div>
          </div>
        </template>

        <template v-if="activeTab === 'orderRecord'">
          <div class="table-panel">
            <div class="table-head record-grid">
              <div>操作类型</div>
              <div>操作记录</div>
              <div>操作时间</div>
            </div>
            <div v-if="!statusLogs.length" class="empty-line">-</div>
            <div v-for="(item, index) in statusLogs" :key="index" class="table-row record-grid">
              <div>{{ changeTypeLabel(item.changeType) }}</div>
              <div>{{ valueOf(item.changeMessage) }}</div>
              <div>{{ valueOf(item.createTime) }}</div>
            </div>
          </div>
        </template>
      </div>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { Close, DocumentCopy } from '@element-plus/icons-vue';
import { useDict } from '@/hooks/useDict';
import { useDictOptions } from '@/hooks/useDictOptions';
import { filterEnum } from '@/utils';
import { getShopOrderDetailByOrderNoApi } from '@/api/modules/shop_system/shopOrder';
import type { ShopOrderDetail, ShopOrderDetailItem, ShopOrderStatusLog } from '@/api/types/shop_system/shopOrder';

defineOptions({
  name: 'ShopOrderDetailDrawer'
});

useDict(['order_status']);

const visible = ref(false);
const loading = ref(false);
const activeTab = ref<'orderInfo' | 'goodsInfo' | 'deliveryInfo' | 'orderRecord'>('orderInfo');
const detail = ref<ShopOrderDetail | null>(null);

const orderStatusOptions = useDictOptions('order_status');

const valueOf = (value: any) => {
  if (value === 0 || value === false) return String(value);
  return value ? String(value) : '-';
};

const money = (value: any) => {
  if (value === '' || value === null || value === undefined) return '-';
  const num = Number(value);
  if (Number.isNaN(num)) return String(value);
  return `¥ ${num.toFixed(2)}`;
};

const orderStatusLabel = computed(() => {
  if (!detail.value?.status && detail.value?.status !== 0) return '-';
  return filterEnum(detail.value.status, orderStatusOptions.value, { label: 'codeName', value: 'alias' });
});

const orderTypeLabel = computed(() => {
  const map: Record<string, string> = {
    '0': '普通',
    '1': '秒杀',
    '2': '砍价',
    '3': '拼团'
  };
  return map[String(detail.value?.orderType ?? 0)] || '普通';
});

const payTypeLabel = computed(() => {
  const payType = detail.value?.payType;
  if (payType === null || payType === undefined || payType === '') return '-';
  return String(payType);
});

const deliveryModeLabel = computed(() => {
  const hasExpressInfo = !!(detail.value?.expressName || detail.value?.expressNo || detail.value?.expressCode);
  return hasExpressInfo ? '快递配送' : '商家配送';
});

const goodsList = computed<ShopOrderDetailItem[]>(() => {
  return detail.value?.orderInfoList || [];
});

const statusLogs = computed<ShopOrderStatusLog[]>(() => {
  return detail.value?.orderStatusList || [];
});

const goodsTotalAmount = computed(() => {
  const total = goodsList.value.reduce((sum, item) => {
    const price = Number(item?.price || 0);
    const count = Number(item?.cartNum || 0);
    return sum + price * count;
  }, 0);
  return total || 0;
});

const platformCouponAmount = computed(() => {
  const list = detail.value?.platCouponUserList || [];
  if (!Array.isArray(list)) return 0;
  return list.reduce((sum, item) => {
    const val = Number(item?.couponPrice ?? item?.discountAmount ?? item?.price ?? 0);
    return sum + (Number.isNaN(val) ? 0 : val);
  }, 0);
});

const receiverName = computed(() => {
  return detail.value?.receiverName || detail.value?.realName || detail.value?.userName || '-';
});

const receiverPhone = computed(() => {
  return detail.value?.receiverPhone || detail.value?.userPhone || '-';
});

const receiverAddress = computed(() => {
  return detail.value?.receiverAddress || detail.value?.userAddress || '-';
});

const deliveryTime = computed(() => {
  const deliveryLog = statusLogs.value.find(item => {
    const type = String(item.changeType || '').toLowerCase();
    return type.includes('delivery') || type.includes('deliver') || type.includes('send') || type.includes('shipping');
  });
  return deliveryLog?.createTime || '-';
});

const changeTypeLabel = (changeType?: string) => {
  const map: Record<string, string> = {
    create_order: '订单生成',
    pay_order: '订单支付',
    delivery_order: '订单发货',
    receive_order: '订单收货',
    finish_order: '订单完成',
    cancel_order: '订单取消',
    refund_order: '订单退款'
  };
  if (!changeType) return '-';
  return map[changeType] || changeType;
};

const normalizeGoodsImage = (item: ShopOrderDetailItem) => {
  const url = item?.productImage || item?.image;
  return url ? String(url) : '';
};

const open = async (orderNo: string) => {
  if (!orderNo) {
    ElMessage.warning('订单号为空，无法查看详情');
    return;
  }

  visible.value = true;
  loading.value = true;
  activeTab.value = 'orderInfo';
  detail.value = null;

  try {
    const res = await getShopOrderDetailByOrderNoApi({ orderNo });
    const data = res?.data || {};
    const list = Array.isArray(data.orderInfoList) ? data.orderInfoList : [];
    data.orderInfoList = list.map((item: ShopOrderDetailItem) => ({
      ...item,
      imageUrl: normalizeGoodsImage(item)
    }));
    detail.value = data;
  } catch {
    ElMessage.error('获取订单详情失败');
    visible.value = false;
  } finally {
    loading.value = false;
  }
};

defineExpose({
  open
});
</script>

<style scoped lang="scss">
.order-detail-drawer {
  position: relative;
  height: 100%;
  overflow-y: auto;
  background: #f6f8fb;
}

.drawer-close {
  position: absolute;
  top: 8px;
  right: 10px;
  z-index: 9;
  font-size: 22px;
}

.summary-panel {
  background: #fff;
  padding: 16px 24px;
}

.summary-head {
  display: flex;
  align-items: center;
  gap: 14px;
}

.summary-icon {
  width: 76px;
  height: 76px;
  border-radius: 8px;
  background: #3f7cff;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
}

.summary-main {
  min-width: 0;
}

.summary-title {
  color: #303133;
  font-size: 30px;
  font-weight: 600;
}

.summary-no {
  margin-top: 6px;
  color: #606266;
  font-size: 22px;
}

.summary-metrics {
  margin-top: 16px;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.metric-item {
  min-width: 0;
}

.metric-label {
  color: #909399;
  font-size: 14px;
}

.metric-value {
  margin-top: 4px;
  color: #303133;
  font-size: 16px;
  font-weight: 600;
}

.metric-value.status {
  color: #ff7d00;
}

.detail-tabs {
  background: #eef2f7;
  padding: 0 8px;
}

.tab-body {
  padding: 14px 22px 24px;
}

.section-card,
.table-panel {
  background: #fff;
  border-radius: 6px;
  margin-bottom: 12px;
  padding: 14px 16px;
}

.section-title {
  position: relative;
  color: #303133;
  font-size: 18px;
  font-weight: 600;
  padding-left: 10px;
  margin-bottom: 12px;
}

.section-title::before {
  content: '';
  position: absolute;
  left: 0;
  top: 5px;
  width: 3px;
  height: 16px;
  border-radius: 2px;
  background: #3f7cff;
}

.row {
  display: grid;
  gap: 10px 22px;
  color: #606266;
  font-size: 15px;
}

.row.two-col {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.row.three-col {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.note-content {
  color: #606266;
  font-size: 15px;
  line-height: 24px;
  word-break: break-word;
}

.table-head,
.table-row {
  display: grid;
  align-items: center;
  gap: 12px;
}

.table-head {
  padding: 12px 14px;
  border-radius: 6px;
  color: #303133;
  background: #eef3fb;
  font-weight: 600;
}

.table-row {
  padding: 12px 14px;
  border-bottom: 1px solid #ebeef5;
  color: #303133;
}

.goods-grid {
  grid-template-columns: 2.5fr 0.9fr 0.9fr 0.9fr;
}

.delivery-grid {
  grid-template-columns: 2.5fr 0.8fr 1.8fr;
}

.record-grid {
  grid-template-columns: 1fr 2fr 1.3fr;
}

.goods-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.goods-image {
  width: 52px;
  height: 52px;
  border-radius: 6px;
  flex-shrink: 0;
  background: #f2f3f5;
}

.goods-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
}

.goods-meta {
  min-width: 0;
}

.goods-name {
  font-size: 15px;
  color: #303133;
}

.goods-sku {
  margin-top: 3px;
  color: #909399;
  font-size: 13px;
}

.delivery-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #eef3fb;
  color: #606266;
  border-radius: 6px;
  padding: 12px 14px;
  margin-bottom: 6px;
}

.delivery-action {
  color: #3f7cff;
}

.empty-line {
  padding: 16px 8px;
  color: #909399;
}

@media (max-width: 1200px) {
  .summary-title {
    font-size: 20px;
  }

  .summary-no {
    font-size: 14px;
  }

  .summary-metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .row.three-col {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
