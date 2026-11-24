<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import ProductPager from '@/components/ProductPager.vue';
import SiteHeader from '@/components/SiteHeader.vue';
import SiteMainNav from '@/components/SiteMainNav.vue';
import SiteFooter from '@/components/SiteFooter.vue';
import { siteNavList } from '@/utils/userCenter';
import {
  getNoticeCategoryApi,
  getNoticeHotApi,
  getNoticePageApi,
  type NoticeCategoryItem,
  type NoticeItem
} from '@/api';
import { useCartStore } from '@/stores/modules/cart';

const route = useRoute();
const router = useRouter();
const cartStore = useCartStore();

const loading = ref(false);
const hotLoading = ref(false);
const pageNo = ref(1);
const pageSize = 8;
const total = ref(0);
const categoryList = ref<NoticeCategoryItem[]>([]);
const noticeList = ref<NoticeItem[]>([]);
const hotList = ref<NoticeItem[]>([]);

const MAX_VISIBLE_CATEGORY = 7;

const activeCategoryId = computed<number | null>(() => {
  const raw = Number(route.query.categoryId || 0);
  return Number.isFinite(raw) && raw > 0 ? raw : null;
});
const visibleCategoryList = computed(() => categoryList.value.slice(0, MAX_VISIBLE_CATEGORY));
const overflowCategoryList = computed(() => categoryList.value.slice(MAX_VISIBLE_CATEGORY));
const activeCategoryName = computed(() => {
  if (!activeCategoryId.value) return '全部极客志';
  return categoryList.value.find(item => item.id === activeCategoryId.value)?.categoryName || '全部极客志';
});

const resolveText = (input?: string) => {
  if (!input) return '暂无简介';
  return input.replace(/<[^>]+>/g, '').replace(/\s+/g, ' ').trim();
};

const resolveSummary = (item: NoticeItem) => resolveText(item.summary || item.content);

const resolveTime = (value?: string) => {
  if (!value) return '-';
  return value.replace('T', ' ').slice(0, 16);
};

const openNoticeDetail = (item: NoticeItem) => {
  router.push(`/notice/${item.id}`);
};

const changeCategory = (categoryId?: number) => {
  router.push({
    path: '/notice',
    query: {
      ...(categoryId ? { categoryId: String(categoryId) } : {})
    }
  });
};

const fetchCategories = async () => {
  const res = await getNoticeCategoryApi();
  categoryList.value = res.data || [];
};

const fetchNoticeList = async (targetPage = 1) => {
  loading.value = true;
  pageNo.value = targetPage;
  try {
    const res = await getNoticePageApi({
      page: targetPage,
      limit: pageSize,
      categoryId: activeCategoryId.value || undefined
    });
    noticeList.value = res.data?.rows || [];
    total.value = Number(res.data?.total || 0);
  } finally {
    loading.value = false;
  }
};

const fetchHotList = async () => {
  hotLoading.value = true;
  try {
    const res = await getNoticeHotApi(5);
    hotList.value = res.data || [];
  } finally {
    hotLoading.value = false;
  }
};

const onPageChange = (page: number) => {
  fetchNoticeList(page);
  window.scrollTo({ top: 0, behavior: 'smooth' });
};

watch(
  () => route.query,
  () => {
    fetchNoticeList(1);
  },
  { immediate: true }
);

onMounted(async () => {
  await Promise.all([fetchCategories(), fetchHotList(), cartStore.fetchCount()]);
});
</script>

<template>
  <div class="kc-page">
    <SiteHeader />
    <SiteMainNav :items="siteNavList" active-key="notice" auto-navigate />

    <main class="kc-main journal-main">
      <section class="journal-top">
        <div>
          <span class="kc-kicker">STUDIO JOURNAL // 极客志</span>
          <h1>外设评测、桌搭美学与客制化笔记</h1>
        </div>

        <div class="category-bar kc-surface">
          <button
            type="button"
            class="cat-tab"
            :class="{ active: !activeCategoryId }"
            @click="changeCategory()"
          >
            全部
          </button>
          <button
            v-for="item in visibleCategoryList"
            :key="item.id"
            type="button"
            class="cat-tab"
            :class="{ active: activeCategoryId === item.id }"
            @click="changeCategory(item.id)"
          >
            {{ item.categoryName }}
          </button>
          <el-dropdown v-if="overflowCategoryList.length > 0" trigger="click">
            <button type="button" class="cat-tab">
              更多
              <el-icon><ArrowDown /></el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-for="item in overflowCategoryList" :key="item.id" @click="changeCategory(item.id)">
                  {{ item.categoryName }}
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </section>

      <section class="journal-layout">
        <div class="feed-col kc-surface" v-loading="loading">
          <div class="feed-head">
            <h2>{{ activeCategoryName }}</h2>
            <span class="kc-mono">{{ total }} ARTICLES</span>
          </div>

          <div v-if="noticeList.length > 0" class="article-list">
            <article v-for="item in noticeList" :key="item.id" class="article-row" @click="openNoticeDetail(item)">
              <div class="article-cover">
                <img v-if="item.coverImage" :src="item.coverImage" :alt="item.title" />
                <div v-else class="cover-fallback kc-mono">{{ item.tagName || 'JOURNAL' }}</div>
              </div>
              <div class="article-body">
                <div class="article-top">
                  <span class="article-tag">{{ item.tagName || '极客志' }}</span>
                  <h3>{{ item.title }}</h3>
                </div>
                <p class="article-summary">{{ resolveSummary(item) }}</p>
                <div class="article-meta kc-mono">
                  <span><el-icon><Clock /></el-icon>{{ resolveTime(item.publishTime) }}</span>
                  <span><el-icon><View /></el-icon>{{ item.browseCount || 0 }}</span>
                </div>
              </div>
            </article>
          </div>

          <div v-else class="empty-note">当前分类暂无文章</div>

          <ProductPager v-if="total > pageSize" :current="pageNo" :page-size="pageSize" :total="total" @change="onPageChange" />
        </div>

        <aside class="hot-col kc-surface" v-loading="hotLoading">
          <div class="hot-head">
            <span class="kc-kicker">TRENDING</span>
            <h3>热门阅读</h3>
          </div>

          <div v-if="hotList.length > 0" class="hot-list">
            <button v-for="(item, index) in hotList" :key="item.id" type="button" class="hot-row" @click="openNoticeDetail(item)">
              <span class="hot-index kc-mono">{{ String(index + 1).padStart(2, '0') }}</span>
              <div class="hot-copy">
                <p>{{ item.title }}</p>
                <span class="kc-mono">{{ resolveTime(item.publishTime) }}</span>
              </div>
            </button>
          </div>
          <div v-else class="empty-note">暂无热门文章</div>
        </aside>
      </section>
    </main>

    <SiteFooter />
  </div>
</template>

<style scoped lang="scss">
.journal-main {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.journal-top h1 {
  margin: 8px 0 0;
  font-size: 30px;
  letter-spacing: -0.025em;
  color: var(--kc-text);
}

.category-bar {
  margin-top: 16px;
  padding: 6px;
  border-radius: 999px;
  border: 1px solid transparent;
  background: var(--kc-surface);
  box-shadow: var(--kc-shadow-sm);
  display: inline-flex;
  align-items: center;
  gap: 4px;
  max-width: 100%;
  overflow-x: auto;
}

.cat-tab {
  height: 36px;
  padding: 0 16px;
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
  white-space: nowrap;
  transition: background-color 140ms ease, color 140ms ease, transform 100ms ease-out;
}

.cat-tab:active {
  transform: scale(0.97);
}

.cat-tab.active {
  background: var(--kc-carbon);
  color: #ffffff;
}

.journal-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 22px;
  align-items: start;
}

.feed-col,
.hot-col {
  padding: 24px;
  border-radius: 24px;
  border: 1px solid transparent;
  background: var(--kc-surface);
  box-shadow: var(--kc-shadow-md);
}

.feed-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 16px;
  padding: 0 4px;
}

.feed-head h2 {
  margin: 0;
  font-size: 20px;
  color: var(--kc-text);
}

.feed-head span {
  padding: 4px 10px;
  border-radius: 999px;
  background: var(--kc-surface-muted);
  font-size: 11px;
  color: var(--kc-text-muted);
  letter-spacing: 0.06em;
}

.article-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.article-row {
  display: grid;
  grid-template-columns: 216px minmax(0, 1fr);
  gap: 20px;
  padding: 12px;
  border-radius: 18px;
  background: transparent;
  cursor: pointer;
  transition: background-color 160ms ease, transform 160ms ease-out;
}

.article-cover {
  height: 142px;
  border-radius: 12px;
  overflow: hidden;
  background: var(--kc-surface-muted);
  outline: 1px solid oklch(0 0 0 / 0.08);
  outline-offset: -1px;
}

.article-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  transition: transform 340ms cubic-bezier(0.22, 1, 0.36, 1);
}

.cover-fallback {
  width: 100%;
  height: 100%;
  display: grid;
  place-items: center;
  background: var(--kc-carbon);
  color: var(--kc-accent);
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 0.12em;
}

.article-body {
  min-width: 0;
  padding: 4px 4px 4px 0;
  display: flex;
  flex-direction: column;
}

.article-top {
  display: flex;
  align-items: center;
  gap: 10px;
}

.article-tag {
  padding: 3px 9px;
  border-radius: 999px;
  background: var(--kc-accent-soft);
  color: var(--kc-accent);
  font-size: 11px;
  font-weight: 700;
  flex-shrink: 0;
}

.article-top h3 {
  margin: 0;
  font-size: 17px;
  color: var(--kc-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: color 140ms ease;
}

.article-summary {
  margin: 10px 0 0;
  font-size: 14px;
  color: var(--kc-text-secondary);
  line-height: 1.75;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.article-meta {
  margin-top: auto;
  padding-top: 12px;
  display: flex;
  align-items: center;
  gap: 18px;
  font-size: 12px;
  color: var(--kc-text-muted);
}

.article-meta span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.hot-head {
  padding: 0 6px;
}

.hot-head h3 {
  margin: 6px 0 0;
  font-size: 18px;
  color: var(--kc-text);
}

.hot-list {
  margin-top: 14px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.hot-row {
  width: 100%;
  padding: 11px 10px;
  border: 0;
  border-radius: 14px;
  background: transparent;
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr);
  gap: 10px;
  text-align: left;
  cursor: pointer;
  transition: background-color 140ms ease;
}

.hot-index {
  font-size: 13px;
  font-weight: 700;
  color: var(--kc-accent);
}

.hot-copy p {
  margin: 0;
  font-size: 14px;
  line-height: 1.5;
  color: var(--kc-text);
  font-weight: 500;
}

.hot-copy span {
  display: block;
  margin-top: 6px;
  font-size: 11px;
  color: var(--kc-text-muted);
}

.empty-note {
  padding: 40px 0;
  text-align: center;
  color: var(--kc-text-muted);
  font-size: 14px;
}

@media (hover: hover) {
  .cat-tab:hover:not(.active) {
    background: var(--kc-surface-muted);
    color: var(--kc-text);
  }

  .article-row:hover {
    background: var(--kc-surface-muted);
  }

  .article-row:hover .article-cover img {
    transform: scale(1.04);
  }

  .article-row:hover .article-top h3 {
    color: var(--kc-accent);
  }

  .hot-row:hover {
    background: var(--kc-surface-muted);
  }

  .hot-row:hover .hot-copy p {
    color: var(--kc-accent);
  }
}

@media (max-width: 1024px) {
  .journal-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 680px) {
  .article-row {
    grid-template-columns: 1fr;
  }

  .article-cover {
    height: 180px;
  }
}
</style>
