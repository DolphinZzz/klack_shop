<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import SiteHeader from '@/components/SiteHeader.vue';
import SiteMainNav from '@/components/SiteMainNav.vue';
import SiteFooter from '@/components/SiteFooter.vue';
import { siteNavList } from '@/utils/userCenter';
import {
  getNoticeDetailApi,
  getNoticeHotApi,
  type NoticeItem
} from '@/api';
import { useCartStore } from '@/stores/modules/cart';

const route = useRoute();
const router = useRouter();
const cartStore = useCartStore();

const loading = ref(false);
const hotLoading = ref(false);
const detail = ref<NoticeItem | null>(null);
const hotList = ref<NoticeItem[]>([]);

const noticeId = computed(() => Number(route.params.id || 0));

const toHome = () => router.push('/home');
const toNoticeList = () => router.push('/notice');

const resolveTime = (value?: string) => {
  if (!value) return '-';
  return value.replace('T', ' ').slice(0, 16);
};

const openNoticeDetail = (item: NoticeItem) => {
  router.push(`/notice/${item.id}`);
};

const fetchDetail = async () => {
  if (!noticeId.value) return;
  loading.value = true;
  try {
    const res = await getNoticeDetailApi(noticeId.value);
    detail.value = res.data || null;
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

watch(
  () => route.params.id,
  () => {
    fetchDetail();
    fetchHotList();
  },
  { immediate: true }
);

onMounted(() => {
  void cartStore.fetchCount();
});
</script>

<template>
  <div class="kc-page">
    <SiteHeader />
    <SiteMainNav :items="siteNavList" active-key="notice" auto-navigate />

    <main class="kc-main article-detail-main">
      <div class="crumbs">
        <button type="button" class="crumb-btn" @click="toHome">工坊首页</button>
        <span>/</span>
        <button type="button" class="crumb-btn" @click="toNoticeList">极客志</button>
        <span>/</span>
        <span class="crumb-current">正文</span>
      </div>

      <section class="detail-grid">
        <article class="article-sheet kc-surface" v-loading="loading">
          <template v-if="detail">
            <header class="article-head">
              <span class="detail-tag">{{ detail.tagName || '极客志' }}</span>
              <h1>{{ detail.title }}</h1>
              <div class="detail-meta kc-mono">
                <span><el-icon><Clock /></el-icon>{{ resolveTime(detail.publishTime) }}</span>
                <span><el-icon><View /></el-icon>{{ detail.browseCount || 0 }} READS</span>
                <span v-if="detail.source">来源：{{ detail.source }}</span>
                <span v-if="detail.author">作者：{{ detail.author }}</span>
              </div>
            </header>

            <div v-if="detail.coverImage" class="detail-cover">
              <img :src="detail.coverImage" :alt="detail.title" />
            </div>

            <div class="detail-prose" v-html="detail.content || detail.summary || '<p>暂无正文内容</p>'"></div>
          </template>
          <div v-else class="empty-note">文章不存在或已下线</div>
        </article>

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
.article-detail-main {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.crumbs {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--kc-text-muted);
}

.crumb-btn {
  border: 0;
  padding: 0;
  background: transparent;
  color: var(--kc-text-secondary);
  font-size: 13px;
  cursor: pointer;
}

.crumb-current {
  color: var(--kc-text);
  font-weight: 600;
}

.detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 22px;
  align-items: start;
}

.article-sheet {
  padding: 36px 40px;
  border-radius: 26px;
  border: 1px solid transparent;
  background: var(--kc-surface);
  box-shadow: var(--kc-shadow-md);
}

.detail-tag {
  display: inline-flex;
  padding: 4px 11px;
  border-radius: 999px;
  background: var(--kc-accent-soft);
  color: var(--kc-accent);
  font-size: 12px;
  font-weight: 700;
}

.article-head h1 {
  margin: 14px 0 0;
  font-size: 32px;
  line-height: 1.3;
  letter-spacing: -0.025em;
  color: var(--kc-text);
}

.detail-meta {
  margin-top: 16px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  font-size: 12px;
  color: var(--kc-text-muted);
}

.detail-meta span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 5px 11px;
  border-radius: 999px;
  background: var(--kc-surface-muted);
}

.detail-cover {
  margin-top: 24px;
  border-radius: 16px;
  overflow: hidden;
  outline: 1px solid oklch(0 0 0 / 0.08);
  outline-offset: -1px;
}

.detail-cover img {
  width: 100%;
  display: block;
  object-fit: cover;
}

.detail-prose {
  margin-top: 28px;
  color: var(--kc-text-secondary);
  font-size: 15px;
  line-height: 1.95;
}

.detail-prose :deep(img),
.detail-prose :deep(video) {
  max-width: 100%;
  border-radius: 14px;
}

.hot-col {
  padding: 24px;
  border-radius: 24px;
  border: 1px solid transparent;
  background: var(--kc-surface);
  box-shadow: var(--kc-shadow-md);
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
  .crumb-btn:hover {
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
  .detail-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .article-sheet {
    padding: 22px;
    border-radius: 20px;
  }

  .article-head h1 {
    font-size: 24px;
  }
}
</style>
