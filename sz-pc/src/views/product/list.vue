<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import ProductPager from '@/components/ProductPager.vue';
import SiteHeader from '@/components/SiteHeader.vue';
import SiteMainNav from '@/components/SiteMainNav.vue';
import SiteFooter from '@/components/SiteFooter.vue';
import { siteNavList } from '@/utils/userCenter';
import {
  getHomeCategoryApi,
  getProductListApi,
  type HomeCategoryItem,
  type ProductListItem
} from '@/api';
import { useCartStore } from '@/stores/modules/cart';

type SortType = 'default' | 'sales' | 'price';
type SortOrder = 'asc' | 'desc';

const route = useRoute();
const router = useRouter();
const cartStore = useCartStore();

const loading = ref(false);
const categoryLoading = ref(false);
const keyword = ref('');
const minPrice = ref('');
const maxPrice = ref('');
const productList = ref<ProductListItem[]>([]);
const total = ref(0);
const pageNo = ref(1);
const pageSize = 20;
const sortType = ref<SortType>('default');
const salesOrder = ref<SortOrder>('desc');
const priceOrder = ref<SortOrder>('asc');
const categoryList = ref<HomeCategoryItem[]>([]);
const failedImages = ref<Record<string, boolean>>({});

const hasValidImage = (url?: string) => Boolean(url && !failedImages.value[url]);
const markImageFailed = (url?: string) => {
  if (!url) return;
  failedImages.value[url] = true;
};

const queryCid = computed(() => String(route.query.cid || '').trim());
const queryPid = computed(() => String(route.query.pid || '').trim());
const queryKeyword = computed(() => String(route.query.keyword || '').trim());

const activeCategoryInfo = computed(() => {
  const cname = String(route.query.cname || '').trim();
  const pname = String(route.query.pname || '').trim();
  if (cname || pname) {
    return { parentName: pname, childName: cname };
  }
  if (!queryCid.value) return { parentName: '', childName: '' };

  for (const parent of categoryList.value) {
    if (String(parent.id) === queryCid.value) {
      return { parentName: parent.cateName, childName: '' };
    }
    const child = (parent.children || []).find(item => String(item.id) === queryCid.value);
    if (child) {
      return { parentName: parent.cateName, childName: child.cateName };
    }
  }
  return { parentName: '', childName: '' };
});

const breadcrumbLabel = computed(() => {
  if (activeCategoryInfo.value.childName) return activeCategoryInfo.value.childName;
  if (activeCategoryInfo.value.parentName) return activeCategoryInfo.value.parentName;
  if (queryKeyword.value) return `“${queryKeyword.value}”`;
  return '全部外设';
});

const selectedConditions = computed(() => {
  const list: string[] = [];
  if (activeCategoryInfo.value.parentName) {
    list.push(`一级分类：${activeCategoryInfo.value.parentName}`);
  }
  if (activeCategoryInfo.value.childName) {
    list.push(`二级分类：${activeCategoryInfo.value.childName}`);
  }
  if (queryKeyword.value) {
    list.push(`关键词：${queryKeyword.value}`);
  }
  if (minPrice.value || maxPrice.value) {
    list.push(`价格：¥${minPrice.value || '0'} - ¥${maxPrice.value || '∞'}`);
  }
  if (sortType.value === 'sales') {
    list.push(`销量：${salesOrder.value === 'desc' ? '从高到低' : '从低到高'}`);
  }
  if (sortType.value === 'price') {
    list.push(`价格：${priceOrder.value === 'desc' ? '从高到低' : '从低到高'}`);
  }
  return list;
});

const toHome = () => router.push('/home');

const parseNonNegative = (value: string) => {
  if (!value.trim()) return undefined;
  const num = Number(value);
  if (!Number.isFinite(num) || num < 0) return undefined;
  return num;
};

const fetchProducts = async (targetPage = pageNo.value) => {
  loading.value = true;
  pageNo.value = targetPage;
  try {
    const request = {
      page: pageNo.value,
      limit: pageSize,
      keyword: queryKeyword.value || undefined,
      cid: queryCid.value || undefined,
      salesOrder: sortType.value === 'sales' ? salesOrder.value : '',
      priceOrder: sortType.value === 'price' ? priceOrder.value : '',
      minPrice: parseNonNegative(minPrice.value),
      maxPrice: parseNonNegative(maxPrice.value)
    };
    const res = await getProductListApi(request);
    productList.value = res.data?.rows || [];
    total.value = Number(res.data?.total || 0);
  } finally {
    loading.value = false;
  }
};

const fetchCategoryTree = async () => {
  categoryLoading.value = true;
  try {
    const res = await getHomeCategoryApi();
    categoryList.value = res.data || [];
  } finally {
    categoryLoading.value = false;
  }
};

const onSearch = (nextKeyword?: string) => {
  const kw = (nextKeyword ?? keyword.value).trim();
  router.push({
    path: '/products',
    query: {
      ...(queryCid.value ? { cid: queryCid.value } : {}),
      ...(queryPid.value ? { pid: queryPid.value } : {}),
      ...(activeCategoryInfo.value.parentName ? { pname: activeCategoryInfo.value.parentName } : {}),
      ...(activeCategoryInfo.value.childName ? { cname: activeCategoryInfo.value.childName } : {}),
      ...(kw ? { keyword: kw } : {})
    }
  });
};

const resetFilters = () => {
  minPrice.value = '';
  maxPrice.value = '';
  sortType.value = 'default';
  keyword.value = '';
  router.push('/products');
};

const onSortClick = (nextType: SortType) => {
  if (nextType === 'default') {
    sortType.value = 'default';
    fetchProducts(1);
    return;
  }
  if (nextType === 'sales') {
    if (sortType.value === 'sales') {
      salesOrder.value = salesOrder.value === 'desc' ? 'asc' : 'desc';
    } else {
      sortType.value = 'sales';
      salesOrder.value = 'desc';
    }
    fetchProducts(1);
    return;
  }
  if (sortType.value === 'price') {
    priceOrder.value = priceOrder.value === 'asc' ? 'desc' : 'asc';
  } else {
    sortType.value = 'price';
    priceOrder.value = 'asc';
  }
  fetchProducts(1);
};

const onPriceConfirm = () => {
  const min = parseNonNegative(minPrice.value);
  const max = parseNonNegative(maxPrice.value);
  if (min != null && max != null && min > max) {
    const tmp = minPrice.value;
    minPrice.value = maxPrice.value;
    maxPrice.value = tmp;
  }
  fetchProducts(1);
};

const onPageChange = (page: number) => {
  fetchProducts(page);
  window.scrollTo({ top: 0, behavior: 'smooth' });
};

const toProductDetail = (id: number) => {
  router.push(`/product/${id}`);
};

const getProductTitle = (item: ProductListItem) => {
  return item.productName || item.storeName || '未命名外设';
};

watch(
  () => route.query,
  () => {
    keyword.value = queryKeyword.value;
    fetchProducts(1);
  },
  { immediate: true }
);

onMounted(() => {
  void fetchCategoryTree();
  void cartStore.fetchCount();
});
</script>

<template>
  <div class="kc-page">
    <SiteHeader v-model="keyword" @search="onSearch" />
    <SiteMainNav :items="siteNavList" active-key="products" auto-navigate />

    <main class="kc-main catalog-main">
      <div class="catalog-top">
        <div class="crumb-line" :class="{ loading: categoryLoading }">
          <button type="button" class="crumb-btn" @click="toHome">工坊首页</button>
          <span>/</span>
          <button type="button" class="crumb-btn" @click="resetFilters">外设图鉴</button>
          <span>/</span>
          <span class="crumb-current">{{ breadcrumbLabel }}</span>
        </div>

        <div class="catalog-header">
          <div>
            <span class="kc-kicker">HARDWARE CATALOG</span>
            <h1>{{ breadcrumbLabel }}</h1>
          </div>
          <div class="catalog-count kc-mono">
            TOTAL <strong>{{ total }}</strong> ITEMS
          </div>
        </div>
      </div>

      <section class="filter-bar">
        <div class="sort-cluster">
          <div class="sort-segmented">
            <button
              type="button"
              class="sort-chip"
              :class="{ active: sortType === 'default' }"
              @click="onSortClick('default')"
            >
              综合排序
            </button>
            <button
              type="button"
              class="sort-chip"
              :class="{ active: sortType === 'sales' }"
              @click="onSortClick('sales')"
            >
              销量
              <span class="kc-mono arrow">{{ sortType === 'sales' && salesOrder === 'asc' ? '↑' : '↓' }}</span>
            </button>
            <button
              type="button"
              class="sort-chip"
              :class="{ active: sortType === 'price' }"
              @click="onSortClick('price')"
            >
              价格
              <span class="kc-mono arrow">{{ sortType === 'price' && priceOrder === 'desc' ? '↓' : '↑' }}</span>
            </button>
          </div>

          <div class="price-range">
            <span class="range-label kc-mono">¥</span>
            <input v-model="minPrice" type="text" placeholder="最低价" @keyup.enter="onPriceConfirm" />
            <span class="range-sep">—</span>
            <input v-model="maxPrice" type="text" placeholder="最高价" @keyup.enter="onPriceConfirm" />
            <button type="button" class="range-apply" @click="onPriceConfirm">筛选</button>
          </div>
        </div>

        <div v-if="selectedConditions.length > 0" class="active-filters">
          <div class="condition-chips">
            <span v-for="item in selectedConditions" :key="item" class="condition-chip">{{ item }}</span>
          </div>
          <button type="button" class="clear-all" @click="resetFilters">重置全部</button>
        </div>
      </section>

      <section class="catalog-grid-wrap" v-loading="loading">
        <div v-if="productList.length > 0" class="catalog-grid">
          <article
            v-for="item in productList"
            :key="item.id"
            class="spec-card"
            role="button"
            tabindex="0"
            @click="toProductDetail(item.id)"
            @keyup.enter="toProductDetail(item.id)"
          >
            <div class="spec-media-wrap">
              <div class="spec-media">
                <img
                  v-if="hasValidImage(item.image)"
                  :src="item.image"
                  :alt="getProductTitle(item)"
                  @error="markImageFailed(item.image)"
                />
                <div v-else class="spec-fallback">{{ getProductTitle(item) }}</div>
              </div>
            </div>
            <div class="spec-body">
              <div class="spec-price-row kc-mono">
                <strong class="spec-price">¥{{ Number(item.price || 0).toFixed(2) }}</strong>
                <span v-if="Number(item.otPrice || 0) > Number(item.price || 0)" class="spec-ot">
                  ¥{{ Number(item.otPrice || 0).toFixed(2) }}
                </span>
              </div>
              <h2 class="spec-title" :title="getProductTitle(item)">{{ getProductTitle(item) }}</h2>
              <div class="spec-telemetry kc-mono">
                <span>{{ item.replyNum || 0 }} 评价</span>
                <span>·</span>
                <span>已售 {{ item.sales || 0 }}{{ item.unitName || '件' }}</span>
              </div>
              <div class="spec-foot">
                <span class="stock-indicator kc-mono">
                  <i :class="{ low: Number(item.stock || 0) <= 0 }" />
                  库存 {{ item.stock || 0 }}
                </span>
                <span class="spec-link">选配 →</span>
              </div>
            </div>
          </article>
        </div>

        <div v-else class="empty-catalog">
          <p>当前筛选条件下暂无外设商品</p>
          <button type="button" class="empty-reset" @click="resetFilters">查看全部外设</button>
        </div>

        <ProductPager v-if="total > pageSize" :current="pageNo" :page-size="pageSize" :total="total" @change="onPageChange" />
      </section>
    </main>

    <SiteFooter />
  </div>
</template>

<style scoped lang="scss">
.catalog-main {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.crumb-line {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--kc-text-muted);
}

.crumb-btn {
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--kc-text-secondary);
  font-size: 13px;
  cursor: pointer;
}

.crumb-current {
  color: var(--kc-text);
  font-weight: 600;
}

.catalog-header {
  margin-top: 10px;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
}

.catalog-header h1 {
  margin: 6px 0 0;
  font-size: 28px;
  font-weight: 700;
  letter-spacing: -0.025em;
  color: var(--kc-text);
  line-height: 1.15;
}

.catalog-count {
  padding: 7px 14px;
  border-radius: 999px;
  background: var(--kc-surface);
  box-shadow: var(--kc-shadow-sm);
  font-size: 12px;
  color: var(--kc-text-muted);
  letter-spacing: 0.06em;
}

.catalog-count strong {
  color: var(--kc-text);
  font-size: 15px;
}

.filter-bar {
  padding: 14px 16px;
  border-radius: 20px;
  border: 1px solid transparent;
  background: var(--kc-surface);
  box-shadow: var(--kc-shadow-sm);
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.sort-cluster {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.sort-segmented {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px;
  border-radius: 999px;
  background: var(--kc-surface-muted);
}

.sort-chip {
  height: 34px;
  padding: 0 15px;
  border-radius: 999px;
  border: 0;
  background: transparent;
  color: var(--kc-text-secondary);
  font-size: 13px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  cursor: pointer;
  transition: color 140ms ease, background-color 140ms ease, box-shadow 140ms ease, transform 100ms ease-out;
}

.sort-chip:active {
  transform: scale(0.97);
}

.sort-chip.active {
  background: var(--kc-surface);
  color: var(--kc-text);
  box-shadow: var(--kc-shadow-sm);
}

.sort-chip .arrow {
  font-size: 12px;
  color: var(--kc-accent);
}

.price-range {
  margin-left: auto;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 40px;
  padding: 4px 4px 4px 14px;
  border-radius: 999px;
  background: var(--kc-surface-muted);
}

.range-label,
.range-sep {
  font-size: 12px;
  color: var(--kc-text-muted);
}

.price-range input {
  width: 72px;
  height: 28px;
  border: 0;
  outline: none;
  background: transparent;
  font-family: var(--kc-font-mono);
  font-size: 12px;
  color: var(--kc-text);
}

.range-apply {
  height: 32px;
  padding: 0 16px;
  border: 0;
  border-radius: 999px;
  background: var(--kc-carbon);
  color: #ffffff;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 140ms ease, transform 100ms ease-out;
}

.range-apply:active {
  transform: scale(0.96);
}

.active-filters {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-top: 4px;
}

.condition-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.condition-chip {
  padding: 5px 12px;
  border-radius: 999px;
  background: var(--kc-accent-soft);
  color: var(--kc-accent);
  font-size: 12px;
  font-weight: 600;
}

.clear-all {
  padding: 5px 12px;
  border: 0;
  border-radius: 999px;
  background: var(--kc-surface-muted);
  color: var(--kc-text-secondary);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
}

/* 4-Column Concentric Catalog Grid (20px outer - 8px padding = 12px inner) */
.catalog-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}

.spec-card {
  border-radius: 20px;
  border: 1px solid transparent;
  background: var(--kc-surface);
  box-shadow: var(--kc-shadow-sm);
  cursor: pointer;
  display: flex;
  flex-direction: column;
  transition: transform 200ms ease-out, box-shadow 200ms ease;
}

.spec-card:active {
  transform: scale(0.985);
}

.spec-media-wrap {
  padding: 8px 8px 0;
}

.spec-media {
  aspect-ratio: 4 / 3;
  border-radius: 12px;
  background: var(--kc-surface-muted);
  overflow: hidden;
  outline: 1px solid oklch(0 0 0 / 0.08);
  outline-offset: -1px;
}

.spec-media img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  transition: transform 340ms cubic-bezier(0.22, 1, 0.36, 1);
}

.spec-fallback {
  width: 100%;
  height: 100%;
  display: grid;
  place-items: center;
  padding: 16px;
  text-align: center;
  color: var(--kc-text-secondary);
  font-size: 13px;
  font-weight: 600;
}

.spec-body {
  padding: 14px 16px 16px;
  display: flex;
  flex: 1;
  flex-direction: column;
}

.spec-price-row {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.spec-price {
  font-size: 20px;
  font-weight: 700;
  color: var(--kc-text);
  line-height: 1;
}

.spec-ot {
  font-size: 12px;
  color: var(--kc-text-muted);
  text-decoration: line-through;
}

.spec-title {
  margin: 10px 0 0;
  font-size: 14px;
  font-weight: 600;
  line-height: 1.45;
  color: var(--kc-text);
  min-height: 40px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.spec-telemetry {
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  color: var(--kc-text-muted);
}

.spec-foot {
  margin-top: auto;
  padding-top: 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.stock-indicator {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  color: var(--kc-text-secondary);
}

.stock-indicator i {
  width: 6px;
  height: 6px;
  border-radius: 999px;
  background: var(--kc-success);
}

.stock-indicator i.low {
  background: var(--kc-text-muted);
}

.spec-link {
  padding: 4px 10px;
  border-radius: 999px;
  background: var(--kc-surface-muted);
  font-size: 12px;
  font-weight: 600;
  color: var(--kc-text-secondary);
  transition: background-color 150ms ease, color 150ms ease;
}

.empty-catalog {
  min-height: 280px;
  border-radius: 22px;
  background: var(--kc-surface);
  box-shadow: var(--kc-shadow-sm);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 14px;
  color: var(--kc-text-muted);
}

.empty-catalog p {
  margin: 0;
}

.empty-reset {
  height: 38px;
  padding: 0 22px;
  border-radius: 999px;
  border: 0;
  background: var(--kc-carbon);
  color: #ffffff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

@media (hover: hover) {
  .crumb-btn:hover {
    color: var(--kc-accent);
  }

  .sort-chip:hover:not(.active) {
    color: var(--kc-text);
  }

  .range-apply:hover {
    background: var(--kc-accent);
  }

  .clear-all:hover {
    color: var(--kc-text);
  }

  .spec-card:hover {
    transform: translateY(-3px);
    box-shadow: var(--kc-shadow-md);
  }

  .spec-card:hover .spec-media img {
    transform: scale(1.04);
  }

  .spec-card:hover .spec-link {
    background: var(--kc-carbon);
    color: #ffffff;
  }
}

@media (max-width: 1080px) {
  .catalog-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .price-range {
    margin-left: 0;
  }
}

@media (max-width: 768px) {
  .catalog-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .catalog-header {
    flex-direction: column;
    align-items: flex-start;
  }
}

@media (max-width: 500px) {
  .catalog-grid {
    grid-template-columns: 1fr;
  }
}
</style>
