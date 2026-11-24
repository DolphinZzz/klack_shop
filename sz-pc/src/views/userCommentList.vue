<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { getUserCommentListApi, type UserCommentItem } from '@/api';
import { useCartStore } from '@/stores/modules/cart';
import UserCenterScaffold from '@/components/UserCenterScaffold.vue';
import ProductPager from '@/components/ProductPager.vue';

const route = useRoute();
const router = useRouter();
const cartStore = useCartStore();

const fallbackImage =
  'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="120" height="120" viewBox="0 0 120 120"%3E%3Crect width="120" height="120" rx="10" fill="%23f4f4f2"/%3E%3Cpath d="M34 72h52" stroke="%23c9c9c0" stroke-width="5" stroke-linecap="round"/%3E%3Cpath d="M40 52h40" stroke="%23c9c9c0" stroke-width="5" stroke-linecap="round"/%3E%3C/svg%3E';

const loading = ref(false);
const keyword = ref(typeof route.query.keyword === 'string' ? route.query.keyword : '');
const page = ref(resolvePage(route.query.page));
const pageSize = ref(5);
const total = ref(0);
const commentList = ref<UserCommentItem[]>([]);

function resolvePage(value: unknown) {
  const next = Number(value || 1);
  return Number.isFinite(next) && next > 0 ? next : 1;
}

function syncRouteQuery() {
  const query: Record<string, string> = {};
  if (keyword.value.trim()) {
    query.keyword = keyword.value.trim();
  }
  if (page.value > 1) {
    query.page = String(page.value);
  }
  router.replace({ path: '/user/comment/list', query });
}

async function fetchComments() {
  loading.value = true;
  try {
    const res = await getUserCommentListApi({
      page: page.value,
      limit: pageSize.value,
      keyword: keyword.value.trim() || undefined
    });
    const data = res.data;
    commentList.value = data?.rows || [];
    total.value = Number(data?.total || 0);
  } finally {
    loading.value = false;
  }
}

async function fetchPageData() {
  syncRouteQuery();
  await fetchComments();
}

function formatPrice(value?: string | number | null) {
  return Number(value || 0).toFixed(2);
}

function formatDateTime(value?: string) {
  if (!value) return '-';
  return value.replace('T', ' ');
}

function getCommentPics(value?: string | null) {
  if (!value) return [] as string[];
  return value
    .split(/[,\n\r]+/)
    .map(item => item.trim())
    .filter(Boolean);
}

function toProduct(productId?: number) {
  if (!productId) return;
  router.push(`/product/${productId}`);
}

async function onSearch() {
  page.value = 1;
  await fetchPageData();
}

async function handlePageChange(nextPage: number) {
  page.value = nextPage;
  await fetchPageData();
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

onMounted(async () => {
  await Promise.all([fetchComments(), cartStore.fetchCount()]);
});
</script>

<template>
  <UserCenterScaffold
    active-menu="comments"
    crumb-section="订单与评价"
    crumb-label="评价晒单"
    title="我的评价晒单"
    description="回看你对已购外设的手感点评与桌搭晒单记录。"
  >
    <template #toolbar-extra>
      <div class="inline-search">
        <el-input
          v-model="keyword"
          placeholder="搜索外设名称或订单号"
          clearable
          @keyup.enter="onSearch"
          @clear="onSearch"
        />
        <el-button type="primary" @click="onSearch">检索</el-button>
      </div>
    </template>

    <div v-loading="loading">
      <div v-if="commentList.length" class="comment-list">
        <article v-for="item in commentList" :key="item.id" class="comment-card">
          <div class="comment-head">
            <div class="product-wrap">
              <img
                class="product-cover"
                :src="item.productImage || fallbackImage"
                :alt="item.productName || '外设图片'"
                @click="toProduct(item.productId)"
              />
              <div class="product-copy">
                <button type="button" class="product-name" @click="toProduct(item.productId)">
                  {{ item.productName || '外设已下架' }}
                </button>
                <p v-if="item.productInfo" class="product-info">{{ item.productInfo }}</p>
                <div class="product-meta kc-mono">
                  <span>NO. {{ item.orderNo || '-' }}</span>
                  <strong>¥{{ formatPrice(item.productPrice) }}</strong>
                </div>
              </div>
            </div>
            <el-button size="small" @click="toProduct(item.productId)">查看外设</el-button>
          </div>

          <div class="comment-body">
            <div class="score-row">
              <div class="score-item">
                <span>评分</span>
                <el-rate :model-value="Number(item.productScore || 0)" disabled />
              </div>
              <span class="comment-time kc-mono">{{ formatDateTime(item.createTime) }}</span>
            </div>

            <p class="comment-text">{{ item.comment || '暂无评价内容' }}</p>

            <div v-if="getCommentPics(item.pic).length" class="comment-pics">
              <el-image
                v-for="(pic, index) in getCommentPics(item.pic)"
                :key="`${item.id}-${index}`"
                class="comment-pic"
                :src="pic"
                :preview-src-list="getCommentPics(item.pic)"
                fit="cover"
                preview-teleported
              />
            </div>
          </div>
        </article>
      </div>

      <el-empty v-else description="暂无评价记录" />

      <ProductPager v-if="total > pageSize" :current="page" :page-size="pageSize" :total="total" @change="handlePageChange" />
    </div>
  </UserCenterScaffold>
</template>

<style scoped lang="scss">
.inline-search {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 300px;
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.comment-card {
  padding: 16px 18px;
  border-radius: 18px;
  background: var(--kc-surface-muted);
}

.comment-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
}

.product-wrap {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.product-cover {
  width: 64px;
  height: 64px;
  object-fit: cover;
  border-radius: 12px;
  outline: 1px solid oklch(0 0 0 / 0.08);
  outline-offset: -1px;
  cursor: pointer;
  flex-shrink: 0;
}

.product-copy {
  min-width: 0;
}

.product-name {
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--kc-text);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  text-align: left;
}

.product-info {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--kc-text-muted);
}

.product-meta {
  margin-top: 6px;
  display: flex;
  align-items: center;
  gap: 14px;
  font-size: 12px;
  color: var(--kc-text-muted);
}

.product-meta strong {
  color: var(--kc-accent);
}

.comment-body {
  padding: 14px 16px;
  border-radius: 14px;
  background: var(--kc-surface);
  box-shadow: var(--kc-shadow-sm);
}

.score-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.score-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--kc-text-muted);
}

.comment-time {
  font-size: 12px;
  color: var(--kc-text-muted);
}

.comment-text {
  margin: 12px 0 0;
  font-size: 14px;
  line-height: 1.75;
  color: var(--kc-text-secondary);
  white-space: pre-wrap;
}

.comment-pics {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.comment-pic {
  width: 84px;
  height: 84px;
  border-radius: 12px;
  outline: 1px solid oklch(0 0 0 / 0.08);
  outline-offset: -1px;
}

@media (hover: hover) {
  .product-name:hover {
    color: var(--kc-accent);
  }
}

@media (max-width: 768px) {
  .inline-search {
    width: 100%;
  }

  .comment-head {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
