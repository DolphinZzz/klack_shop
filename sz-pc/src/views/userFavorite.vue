<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { deleteFavoriteApi, getFavoritePageApi, type FavoriteItem } from '@/api';
import ProductPager from '@/components/ProductPager.vue';
import UserCenterScaffold from '@/components/UserCenterScaffold.vue';
import { showConfirmDialog } from '@/utils/confirm';

const router = useRouter();

const PAGE_SIZE = 8;
const fallbackImage = 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="120" height="140" viewBox="0 0 120 140"%3E%3Crect width="120" height="140" rx="20" fill="%23f8fafc"/%3E%3Cpath d="M28 84h64" stroke="%23d7dee7" stroke-width="6" stroke-linecap="round"/%3E%3Cpath d="M36 60h48" stroke="%23d7dee7" stroke-width="6" stroke-linecap="round"/%3E%3C/svg%3E';

const loading = ref(false);
const page = ref(1);
const total = ref(0);
const rows = ref<FavoriteItem[]>([]);
const batchMode = ref(false);
const selectedIds = ref<number[]>([]);

const totalText = computed(() => `全部宝贝(${total.value})`);
const selectedCount = computed(() => selectedIds.value.length);
const isAllSelected = computed(() => rows.value.length > 0 && rows.value.every(item => selectedIds.value.includes(item.id)));

const formatMoney = (value?: string | number | null) => `￥${Number(value || 0).toFixed(2)}`;
const formatTime = (value?: string) => (value ? value.replace('T', ' ').slice(0, 16) : '刚刚收藏');

const fetchFavorites = async () => {
  loading.value = true;
  try {
    const res = await getFavoritePageApi({ page: page.value, limit: PAGE_SIZE });
    rows.value = res.data?.rows || [];
    total.value = Number(res.data?.total || 0);
    selectedIds.value = selectedIds.value.filter(id => rows.value.some(item => item.id === id));
  } finally {
    loading.value = false;
  }
};

const toProduct = (productId?: number) => {
  if (!productId) return;
  router.push(`/product/${productId}`);
};

const enterBatchMode = () => {
  batchMode.value = true;
  selectedIds.value = [];
};

const exitBatchMode = () => {
  batchMode.value = false;
  selectedIds.value = [];
};

const toggleSelection = (id: number) => {
  if (!batchMode.value) return;
  if (selectedIds.value.includes(id)) {
    selectedIds.value = selectedIds.value.filter(item => item !== id);
    return;
  }
  selectedIds.value = [...selectedIds.value, id];
};

const toggleAll = () => {
  if (isAllSelected.value) {
    selectedIds.value = [];
    return;
  }
  selectedIds.value = rows.value.map(item => item.id);
};

const removeFavorites = async (ids: number[], successMessage: string) => {
  if (!ids.length) return;
  await deleteFavoriteApi(ids);
  const nextTotal = Math.max(0, total.value - ids.length);
  const maxPage = Math.max(1, Math.ceil(nextTotal / PAGE_SIZE));
  if (page.value > maxPage) {
    page.value = maxPage;
  }
  selectedIds.value = selectedIds.value.filter(id => !ids.includes(id));
  await fetchFavorites();
  ElMessage.success(successMessage);
};

const removeOne = async (item: FavoriteItem) => {
  try {
    await showConfirmDialog(`确认移除“${item.productName || '该商品'}”吗？`, '取消收藏', { type: 'warning' });
    await removeFavorites([item.id], '已移除收藏商品');
  } catch (error) {
    // cancel
  }
};

const removeSelected = async () => {
  if (!selectedIds.value.length) {
    ElMessage.warning('请先选择要取消收藏的商品');
    return;
  }
  try {
    await showConfirmDialog(`确认取消收藏选中的 ${selectedIds.value.length} 件商品吗？`, '批量取消收藏', { type: 'warning' });
    await removeFavorites([...selectedIds.value], '已批量取消收藏');
    if (!rows.value.length) {
      exitBatchMode();
    }
  } catch (error) {
    // cancel
  }
};

const handlePageChange = async (nextPage: number) => {
  page.value = nextPage;
  selectedIds.value = [];
  await fetchFavorites();
  window.scrollTo({ top: 0, behavior: 'smooth' });
};

onMounted(() => {
  void fetchFavorites();
});
</script>

<template>
  <UserCenterScaffold
    active-menu="favorite"
    crumb-section="收藏夹"
    crumb-label="收藏的宝贝"
    :title="totalText"
  >
    <template #toolbar-extra>
      <div class="toolbar-extra">

        <template v-if="!batchMode">
          <el-button round class="batch-btn" @click="enterBatchMode">批量操作</el-button>
        </template>
        <template v-else>
          <el-button round @click="exitBatchMode">取消</el-button>
          <button type="button" class="inline-action" @click="toggleAll">
            {{ isAllSelected ? '取消全选' : '全选' }}
          </button>
          <button type="button" class="inline-action danger" :disabled="selectedCount === 0" @click="removeSelected">
            取消收藏<span v-if="selectedCount">({{ selectedCount }})</span>
          </button>
        </template>
      </div>
    </template>

    <div class="favorite-page" v-loading="loading">
      <div v-if="batchMode && rows.length" class="batch-banner">
        <div class="banner-copy">
          <strong>批量管理模式</strong>
          <span>已选择 {{ selectedCount }} 件商品，可一次性取消收藏。</span>
        </div>
        <div class="banner-meta">{{ isAllSelected ? '当前页已全选' : '当前页未全选' }}</div>
      </div>

      <section v-if="rows.length" class="favorite-grid">
        <article
          v-for="item in rows"
          :key="item.id"
          class="favorite-card"
          :class="{ selected: selectedIds.includes(item.id), batch: batchMode }"
        >
          <button
            type="button"
            class="select-dot"
            :class="{ visible: batchMode, active: selectedIds.includes(item.id) }"
            @click.stop="toggleSelection(item.id)"
          >
            <span />
          </button>

          <button
            v-if="!batchMode"
            type="button"
            class="remove-btn"
            @click.stop="removeOne(item)"
          >
            移除
          </button>

          <button type="button" class="cover-wrap" @click="toProduct(item.productId)">
            <img class="cover-image" :src="item.productImage || fallbackImage" :alt="item.productName || '商品图片'" />
          </button>

          <div class="card-body">
            <button type="button" class="product-name" @click="toProduct(item.productId)">
              {{ item.productName || '商品已下架' }}
            </button>
            <p class="product-info">{{ item.productInfo || '这件商品已被加入你的收藏夹，方便你随时回来查看。' }}</p>

            <div class="card-meta">
              <span class="meta-time">{{ formatTime(item.updateTime || item.createTime) }}</span>
              <span class="meta-badge">已收藏</span>
            </div>

            <div class="card-footer">
              <strong class="price">{{ formatMoney(item.productPrice) }}</strong>
              <button type="button" class="detail-link" @click="toProduct(item.productId)">查看详情</button>
            </div>
          </div>
        </article>
      </section>

      <el-empty v-else description="你还没有收藏商品，先去逛逛喜欢的宝贝吧~" />

      <ProductPager
        v-if="total > PAGE_SIZE"
        :current="page"
        :page-size="PAGE_SIZE"
        :total="total"
        @change="handlePageChange"
      />
    </div>
  </UserCenterScaffold>
</template>

<style scoped lang="scss">
.favorite-page {
  display: grid;
  gap: 18px;
  min-height: 260px;
}

.toolbar-extra {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
}

.batch-btn {
  min-width: 108px;
}

.inline-action {
  height: 36px;
  padding: 0 4px;
  border: none;
  background: transparent;
  color: #334155;
  font-size: 14px;
  cursor: pointer;
}

.inline-action.danger {
  color: var(--kc-accent);
}

.inline-action:disabled {
  color: #cbd5e1;
  cursor: not-allowed;
}

.batch-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 18px;
  border-radius: 22px;
  background:
    radial-gradient(circle at top left, rgba(251, 113, 133, 0.14), transparent 34%),
    var(--kc-surface-muted);
  border: 1px solid transparent;
  box-shadow: var(--kc-shadow-sm);
}

.banner-copy {
  display: grid;
  gap: 6px;
}

.banner-copy strong {
  color: #111827;
  font-size: 16px;
}

.banner-copy span,
.banner-meta {
  color: #64748b;
  font-size: 13px;
}

.favorite-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}

.favorite-card {
  position: relative;
  min-width: 0;
  overflow: hidden;
  border-radius: 24px;
  border: 1px solid transparent;
  box-shadow: var(--kc-shadow-sm);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98) 0%, rgba(248, 250, 252, 0.98) 100%);
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.05);
  transition: transform 0.22s ease, border-color 0.22s ease, box-shadow 0.22s ease;
}

.favorite-card:hover {
  transform: translateY(-4px);
  border-color: #fecaca;
  box-shadow: 0 18px 40px rgba(239, 68, 68, 0.12);
}

.favorite-card.selected {
  border-color: #fb7185;
  box-shadow: 0 18px 40px rgba(251, 113, 133, 0.14);
}

.favorite-card.batch {
  cursor: default;
}

.select-dot,
.remove-btn {
  position: absolute;
  top: 12px;
  z-index: 2;
}

.select-dot {
  left: 12px;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.95);
  background: rgba(255, 255, 255, 0.86);
  display: grid;
  place-items: center;
  cursor: pointer;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.2s ease, transform 0.2s ease, background-color 0.2s ease;
}

.select-dot.visible {
  opacity: 1;
  pointer-events: auto;
}

.select-dot span {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  border: 2px solid #cbd5e1;
  transition: border-color 0.2s ease, background-color 0.2s ease;
}

.select-dot.active {
  background: rgba(239, 68, 68, 0.92);
}

.select-dot.active span {
  border-color: #fff;
  background: #fff;
}

.remove-btn {
  left: 12px;
  height: 34px;
  padding: 0 14px;
  border: none;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.78);
  color: #fff;
  font-size: 12px;
  cursor: pointer;
  opacity: 0;
  transform: translateY(-6px);
  transition: opacity 0.2s ease, transform 0.2s ease, background-color 0.2s ease;
}

.favorite-card:hover .remove-btn {
  opacity: 1;
  transform: translateY(0);
}

.remove-btn:hover {
  background: rgba(239, 68, 68, 0.9);
}

.cover-wrap {
  width: 100%;
  padding: 14px 14px 0;
  border: none;
  background: transparent;
  cursor: pointer;
}

.cover-image {
  display: block;
  width: 100%;
  aspect-ratio: 1 / 1.08;
  object-fit: cover;
  border-radius: 18px;
  background: #f8fafc;
}

.card-body {
  display: grid;
  gap: 12px;
  padding: 16px 16px 18px;
}

.product-name {
  padding: 0;
  border: none;
  background: transparent;
  color: #0f172a;
  font-size: 16px;
  font-weight: 700;
  line-height: 1.6;
  text-align: left;
  cursor: pointer;
  display: -webkit-box;
  overflow: hidden;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.product-name:hover,
.detail-link:hover {
  color: var(--kc-accent);
}

.product-info {
  min-height: 42px;
  margin: 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.7;
  display: -webkit-box;
  overflow: hidden;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.card-meta,
.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.meta-time {
  color: #94a3b8;
  font-size: 12px;
}

.meta-badge {
  display: inline-flex;
  align-items: center;
  height: 26px;
  padding: 0 10px;
  border-radius: 999px;
  background: var(--kc-accent-soft);
  color: #e11d48;
  font-size: 12px;
  font-weight: 600;
}

.price {
  color: #111827;
  font-size: 28px;
  line-height: 1;
}

.detail-link {
  padding: 0;
  border: none;
  background: transparent;
  color: #475569;
  font-size: 13px;
  cursor: pointer;
}

@media (max-width: 1200px) {
  .favorite-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .toolbar-extra,
  .batch-banner {
    justify-content: flex-start;
  }

  .favorite-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .favorite-grid {
    grid-template-columns: 1fr;
  }

  .price {
    font-size: 24px;
  }
}
</style>


