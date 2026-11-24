<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import {
  createFavoriteApi,
  deleteCartItemApi,
  getCartListApi,
  updateCartItemApi,
  createPreOrderApi,
  type CartItem
} from '@/api';
import SiteHeader from '@/components/SiteHeader.vue';
import SiteMainNav from '@/components/SiteMainNav.vue';
import SiteFooter from '@/components/SiteFooter.vue';
import { siteNavList } from '@/utils/userCenter';
import { showConfirmDialog } from '@/utils/confirm';
import { useCartStore } from '@/stores/modules/cart';

const router = useRouter();
const cartStore = useCartStore();

const loading = ref(false);
const settleLoading = ref(false);
const cartList = ref<CartItem[]>([]);
const selectedIds = ref<number[]>([]);

const cartCount = computed(() => cartStore.cartCount);

const selectedRows = computed(() => {
  const ids = new Set(selectedIds.value);
  return cartList.value.filter(item => ids.has(item.id));
});

const isAllChecked = computed(() => {
  return cartList.value.length > 0 && selectedIds.value.length === cartList.value.length;
});

const totalAmount = computed(() => {
  return selectedRows.value.reduce((sum, item) => sum + Number(item.price || 0) * Number(item.cartNum || 0), 0);
});

const settleCount = computed(() => {
  return selectedRows.value.reduce((sum, item) => sum + Number(item.cartNum || 0), 0);
});

const toProductDetail = (id: number) => {
  router.push(`/product/${id}`);
};

const fetchCartList = async () => {
  loading.value = true;
  try {
    const res = await getCartListApi({ page: 1, limit: 200 });
    cartList.value = res.data?.rows || [];
    selectedIds.value = selectedIds.value.filter(id => cartList.value.some(item => item.id === id));
  } finally {
    loading.value = false;
  }
};

const updateCartNum = async (item: CartItem, nextNum: number) => {
  const maxStock = Number(item.stock || 0);
  const safeNum = Math.max(1, maxStock > 0 ? Math.min(nextNum, maxStock) : nextNum);
  if (safeNum === item.cartNum) return;
  const oldNum = item.cartNum;
  item.cartNum = safeNum;
  try {
    await updateCartItemApi({ id: item.id, cartNum: safeNum });
    await cartStore.fetchCount();
  } catch {
    item.cartNum = oldNum;
  }
};

const decreaseNum = (item: CartItem) => {
  updateCartNum(item, Number(item.cartNum || 1) - 1);
};

const increaseNum = (item: CartItem) => {
  updateCartNum(item, Number(item.cartNum || 1) + 1);
};

const toggleAll = (checked: boolean | string | number) => {
  if (checked) {
    selectedIds.value = cartList.value.map(item => item.id);
    return;
  }
  selectedIds.value = [];
};

const isRowChecked = (id: number) => selectedIds.value.includes(id);

const toggleRow = (id: number, checked: boolean | string | number) => {
  if (checked) {
    if (!selectedIds.value.includes(id)) {
      selectedIds.value = [...selectedIds.value, id];
    }
    return;
  }
  selectedIds.value = selectedIds.value.filter(item => item !== id);
};

const removeRows = async (ids: number[]) => {
  if (!ids.length) return;
  await deleteCartItemApi(ids);
  cartList.value = cartList.value.filter(item => !ids.includes(item.id));
  selectedIds.value = selectedIds.value.filter(id => !ids.includes(id));
  await cartStore.fetchCount();
};

const removeOne = async (id: number) => {
  try {
    await showConfirmDialog('确认从清单移除该外设吗？', '移除确认', { type: 'warning' });
    await removeRows([id]);
  } catch {
    // cancel
  }
};

const removeSelected = async () => {
  if (!selectedIds.value.length) {
    ElMessage.warning('请先选择外设商品');
    return;
  }
  try {
    await showConfirmDialog('确认移除选中的外设商品吗？', '批量移除', { type: 'warning' });
    await removeRows([...selectedIds.value]);
  } catch {
    // cancel
  }
};

const moveCollectRows = async (rows: CartItem[]) => {
  if (!rows.length) {
    ElMessage.warning('请先选择外设商品');
    return;
  }

  const productIds = Array.from(new Set(rows.map(item => Number(item.productId)).filter(Boolean)));
  for (const productId of productIds) {
    await createFavoriteApi(productId);
  }
  await removeRows(rows.map(item => item.id));
  ElMessage.success('已移入收藏清单');
};

const moveCollect = async () => {
  await moveCollectRows(selectedRows.value);
};

const moveCollectOne = async (item: CartItem) => {
  await moveCollectRows([item]);
};

const settle = async () => {
  if (!selectedIds.value.length) {
    ElMessage.warning('请先选择结算商品');
    return;
  }
  settleLoading.value = true;
  try {
    const res = await createPreOrderApi({
      shoppingCartIds: [...selectedIds.value],
      preOrderType: 'cart'
    });
    const orderNo = res.data?.orderNo;
    if (!orderNo) {
      ElMessage.error('预下单失败，缺少订单号');
      return;
    }
    router.push({ path: '/order/confirm', query: { orderNo } });
  } finally {
    settleLoading.value = false;
  }
};

onMounted(async () => {
  await Promise.all([fetchCartList(), cartStore.fetchCount()]);
});
</script>

<template>
  <div class="kc-page">
    <SiteHeader />
    <SiteMainNav :items="siteNavList" auto-navigate />

    <main class="kc-main cart-main">
      <div class="cart-head">
        <div>
          <span class="kc-kicker">BUILD CART // 购物清单</span>
          <h1>我的购物清单 <span class="kc-mono count-badge">{{ cartCount }}</span></h1>
        </div>
        <button type="button" class="continue-btn" @click="router.push('/products')">继续挑选外设 →</button>
      </div>

      <section class="cart-table kc-surface" v-loading="loading">
        <div class="table-head">
          <div class="col-check">
            <el-checkbox :model-value="isAllChecked" @change="toggleAll">全选</el-checkbox>
          </div>
          <div class="col-product">外设与配置规格</div>
          <div class="col-price">单价</div>
          <div class="col-qty">数量</div>
          <div class="col-amount">小计</div>
          <div class="col-op">操作</div>
        </div>

        <div v-if="cartList.length > 0" class="table-body">
          <div class="row-item" v-for="item in cartList" :key="item.id">
            <div class="col-check">
              <el-checkbox :model-value="isRowChecked(item.id)" @change="(checked) => toggleRow(item.id, checked)" />
            </div>

            <div class="col-product">
              <div class="product-info">
                <img :src="item.image" :alt="item.productName" @click="toProductDetail(item.productId)" />
                <div class="product-text">
                  <p class="name" :title="item.productName" @click="toProductDetail(item.productId)">
                    {{ item.productName }}
                  </p>
                  <span class="sku-pill kc-mono">{{ item.sku || '标准配置' }}</span>
                </div>
              </div>
            </div>

            <div class="col-price kc-mono">¥{{ Number(item.price || 0).toFixed(2) }}</div>

            <div class="col-qty">
              <div class="qty-stepper kc-mono">
                <button type="button" @click="decreaseNum(item)">-</button>
                <span>{{ item.cartNum }}</span>
                <button type="button" @click="increaseNum(item)">+</button>
              </div>
            </div>

            <div class="col-amount kc-mono">¥{{ (Number(item.price || 0) * Number(item.cartNum || 0)).toFixed(2) }}</div>

            <div class="col-op">
              <button type="button" class="op-btn" @click="moveCollectOne(item)">移入收藏</button>
              <button type="button" class="op-btn op-danger" @click="removeOne(item.id)">移除</button>
            </div>
          </div>
        </div>

        <div v-else class="empty-cart">
          <p>购物清单还是空的，去挑一套趁手的外设吧</p>
          <button type="button" class="empty-cta" @click="router.push('/products')">探索外设图鉴</button>
        </div>
      </section>

      <section class="cart-dock kc-surface">
        <div class="dock-left">
          <el-checkbox :model-value="isAllChecked" @change="toggleAll">全选</el-checkbox>
          <button type="button" class="dock-action" @click="removeSelected">移除选中</button>
          <button type="button" class="dock-action" @click="moveCollect">移入收藏清单</button>
        </div>
        <div class="dock-right">
          <div class="dock-total">
            <span class="total-label">已选 <strong class="kc-mono">{{ settleCount }}</strong> 件 · 合计</span>
            <strong class="total-amount kc-mono">¥{{ totalAmount.toFixed(2) }}</strong>
          </div>
          <el-button type="danger" class="settle-btn" :loading="settleLoading" @click="settle">
            去结算 ({{ settleCount }})
          </el-button>
        </div>
      </section>
    </main>

    <SiteFooter />
  </div>
</template>

<style scoped lang="scss">
.cart-main {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.cart-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
}

.cart-head h1 {
  margin: 6px 0 0;
  font-size: 28px;
  letter-spacing: -0.025em;
  color: var(--kc-text);
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.count-badge {
  padding: 4px 12px;
  border-radius: 999px;
  background: var(--kc-carbon);
  color: #ffffff;
  font-size: 13px;
}

.continue-btn {
  height: 36px;
  padding: 0 16px;
  border: 0;
  border-radius: 999px;
  background: var(--kc-surface);
  box-shadow: var(--kc-shadow-sm);
  color: var(--kc-text-secondary);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.cart-table {
  padding: 12px;
  border-radius: 24px;
  border: 1px solid transparent;
  background: var(--kc-surface);
  box-shadow: var(--kc-shadow-md);
}

.table-head,
.row-item {
  display: grid;
  grid-template-columns: 92px minmax(0, 1fr) 128px 156px 136px 112px;
  align-items: center;
}

.table-head {
  min-height: 44px;
  border-radius: 14px;
  background: var(--kc-surface-muted);
  color: var(--kc-text-muted);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.04em;
  margin-bottom: 8px;
}

.table-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.row-item {
  min-height: 112px;
  border-radius: 16px;
  transition: background-color 150ms ease;
}

.col-check {
  padding-left: 18px;
}

.col-product {
  padding: 12px;
}

.product-info {
  display: flex;
  align-items: center;
  gap: 16px;
  min-width: 0;
}

.product-info img {
  width: 82px;
  height: 82px;
  object-fit: cover;
  border-radius: 12px;
  outline: 1px solid oklch(0 0 0 / 0.08);
  outline-offset: -1px;
  cursor: pointer;
  flex-shrink: 0;
}

.product-text {
  min-width: 0;
}

.product-text .name {
  margin: 0;
  color: var(--kc-text);
  font-size: 15px;
  font-weight: 600;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
}

.sku-pill {
  display: inline-block;
  margin-top: 8px;
  padding: 4px 10px;
  border-radius: 999px;
  background: var(--kc-surface-muted);
  color: var(--kc-text-secondary);
  font-size: 12px;
}

.col-price {
  color: var(--kc-text);
  font-size: 14px;
  font-weight: 600;
}

.col-amount {
  color: var(--kc-accent);
  font-size: 16px;
  font-weight: 700;
}

.qty-stepper {
  display: inline-flex;
  align-items: center;
  padding: 3px;
  border-radius: 999px;
  background: var(--kc-surface-muted);
}

.qty-stepper button {
  width: 30px;
  height: 30px;
  border: 0;
  border-radius: 999px;
  background: var(--kc-surface);
  color: var(--kc-text);
  box-shadow: var(--kc-shadow-sm);
  cursor: pointer;
}

.qty-stepper span {
  width: 40px;
  text-align: center;
  font-size: 13px;
  font-weight: 600;
}

.col-op {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.op-btn {
  border: 0;
  padding: 0;
  background: transparent;
  color: var(--kc-text-secondary);
  text-align: left;
  cursor: pointer;
  font-size: 13px;
}

.op-danger {
  color: var(--kc-accent);
}

.empty-cart {
  min-height: 240px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 14px;
  color: var(--kc-text-muted);
}

.empty-cart p {
  margin: 0;
}

.empty-cta {
  height: 40px;
  padding: 0 22px;
  border-radius: 999px;
  border: 0;
  background: var(--kc-carbon);
  color: #ffffff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.cart-dock {
  padding: 18px 24px;
  border-radius: 22px;
  border: 1px solid transparent;
  background: var(--kc-surface);
  box-shadow: var(--kc-shadow-md);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.dock-left {
  display: flex;
  align-items: center;
  gap: 18px;
}

.dock-action {
  padding: 6px 12px;
  border: 0;
  border-radius: 999px;
  background: var(--kc-surface-muted);
  color: var(--kc-text-secondary);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.dock-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.dock-total {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.total-label {
  font-size: 13px;
  color: var(--kc-text-secondary);
}

.total-amount {
  font-size: 28px;
  color: var(--kc-accent);
  line-height: 1;
}

.settle-btn {
  min-width: 164px;
  height: 46px;
  border-radius: 999px;
  font-weight: 700;
}

@media (hover: hover) {
  .continue-btn:hover {
    color: var(--kc-accent);
    box-shadow: var(--kc-shadow-md);
  }

  .row-item:hover {
    background: var(--kc-surface-muted);
  }

  .product-text .name:hover {
    color: var(--kc-accent);
  }

  .op-btn:hover {
    color: var(--kc-text);
  }

  .dock-action:hover {
    color: var(--kc-accent);
  }
}

@media (max-width: 960px) {
  .table-head {
    display: none;
  }

  .row-item {
    grid-template-columns: 1fr;
    padding: 16px;
    gap: 12px;
    background: var(--kc-surface-muted);
  }

  .col-check,
  .col-product {
    padding: 0;
  }

  .cart-dock {
    flex-direction: column;
    align-items: stretch;
  }

  .dock-right {
    justify-content: space-between;
  }
}
</style>
