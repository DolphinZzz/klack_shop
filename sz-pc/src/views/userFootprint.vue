<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { getUserFootprintListApi, type UserFootprintItem } from '@/api';
import UserCenterScaffold from '@/components/UserCenterScaffold.vue';

const router = useRouter();
const loading = ref(false);
const rows = ref<UserFootprintItem[]>([]);

const fallbackImage = 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="120" height="120" viewBox="0 0 120 120"%3E%3Crect width="120" height="120" rx="18" fill="%23f4f4f5"/%3E%3Cpath d="M34 72h52" stroke="%23d4d4d8" stroke-width="6" stroke-linecap="round"/%3E%3Cpath d="M40 52h40" stroke="%23d4d4d8" stroke-width="6" stroke-linecap="round"/%3E%3C/svg%3E';

const visibleCount = computed(() => rows.value.length);
const formatMoney = (value?: string | number | null) => `￥${Number(value || 0).toFixed(2)}`;

const fetchFootprints = async () => {
  loading.value = true;
  try {
    const res = await getUserFootprintListApi({ limit: 20 });
    rows.value = res.data || [];
  } finally {
    loading.value = false;
  }
};

const toProduct = (productId?: number) => {
  if (!productId) return;
  router.push(`/product/${productId}`);
};

onMounted(() => {
  void fetchFootprints();
});
</script>

<template>
  <UserCenterScaffold
    active-menu="footprint"
    crumb-section="历史记录"
    crumb-label="我的足迹"
    title="我的足迹"
    description="这里会展示最近浏览过的商品，仅用于历史记录查看，默认显示最近 20 条。"
  >
    <div class="footprint-page" v-loading="loading">
      <div class="page-tip" v-if="rows.length">当前展示 {{ visibleCount }} 条足迹记录</div>

      <section v-if="rows.length" class="footprint-list">
        <article v-for="item in rows" :key="item.id" class="footprint-card">
          <button type="button" class="footprint-cover-wrap" @click="toProduct(item.productId)">
            <img class="footprint-cover" :src="item.productImage || fallbackImage" :alt="item.productName || '商品图片'" />
          </button>

          <div class="footprint-main">
            <button type="button" class="footprint-name" @click="toProduct(item.productId)">
              {{ item.productName || '商品已下架' }}
            </button>

            <p class="footprint-info">{{ item.productInfo || '这件商品曾出现在你的浏览记录里。' }}</p>

            <div class="footprint-meta-row">
              <div class="meta-pill price-pill">
                <span>浏览价格</span>
                <strong>{{ formatMoney(item.productPrice) }}</strong>
              </div>
              <div class="meta-pill id-pill">
                <span>商品编号</span>
                <strong>#{{ item.productId || '-' }}</strong>
              </div>
            </div>

            <div class="footprint-actions">
              <el-button round @click="toProduct(item.productId)">查看商品</el-button>
            </div>
          </div>
        </article>
      </section>

      <el-empty v-else description="你还没有浏览足迹，先去逛逛商品吧~" />
    </div>
  </UserCenterScaffold>
</template>

<style scoped lang="scss">
.footprint-page {
  display: grid;
  gap: 16px;
  min-height: 260px;
}

.page-tip {
  color: #64748b;
  font-size: 13px;
}

.footprint-list {
  display: grid;
  gap: 16px;
}

.footprint-card {
  display: grid;
  grid-template-columns: 120px minmax(0, 1fr);
  gap: 18px;
  padding: 18px;
  border-radius: 22px;
  border: 1px solid transparent;
  box-shadow: var(--kc-shadow-sm);
  background: #fff;
}

.footprint-cover-wrap {
  padding: 0;
  border: none;
  background: transparent;
  cursor: pointer;
}

.footprint-cover {
  display: block;
  width: 120px;
  height: 156px;
  object-fit: cover;
  border-radius: 18px;
  border: 1px solid transparent;
  box-shadow: var(--kc-shadow-sm);
  background: #f8fafc;
}

.footprint-main {
  min-width: 0;
  display: grid;
  gap: 14px;
}

.footprint-name {
  padding: 0;
  border: none;
  background: transparent;
  color: #0f172a;
  font-size: 24px;
  font-weight: 700;
  text-align: left;
  cursor: pointer;
}

.footprint-info {
  margin: 0;
  color: #64748b;
  font-size: 14px;
  line-height: 1.8;
}

.footprint-meta-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.meta-pill {
  padding: 12px 14px;
  border-radius: 16px;
  border: 1px solid transparent;
  box-shadow: var(--kc-shadow-sm);
}

.price-pill {
  background: var(--kc-surface-muted);
}

.id-pill {
  background: linear-gradient(180deg, #fff 0%, #f8fafc 100%);
}

.meta-pill span {
  display: block;
  color: #94a3b8;
  font-size: 12px;
}

.meta-pill strong {
  display: block;
  margin-top: 8px;
  color: #0f172a;
  font-size: 20px;
  line-height: 1.2;
}

.footprint-actions {
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 768px) {
  .footprint-card {
    grid-template-columns: 1fr;
  }

  .footprint-cover {
    width: 100%;
    max-width: 220px;
    height: auto;
    aspect-ratio: 3 / 4;
  }

  .footprint-meta-row {
    grid-template-columns: 1fr;
  }
}
</style>
