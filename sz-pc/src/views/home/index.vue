<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import {
  getHomeBannerApi,
  getHomeCategoryApi,
  getHomeNewsApi,
  getHomeNewBooksApi,
  getHomeGuessLikeApi,
  type HomeBannerItem,
  type HomeCategoryItem,
  type HomeNewsItem,
  type HomeNewBookItem
} from '@/api';
import { useAuthStore } from '@/stores/modules/auth';
import { useCartStore } from '@/stores/modules/cart';
import SiteHeader from '@/components/SiteHeader.vue';
import SiteMainNav from '@/components/SiteMainNav.vue';
import SiteFooter from '@/components/SiteFooter.vue';
import { siteNavList } from '@/utils/userCenter';

type CategoryDisplayItem = {
  id: number;
  name: string;
  previewChildren: string[];
  children: Array<{ id: number; name: string }>;
};

const router = useRouter();
const authStore = useAuthStore();
const cartStore = useCartStore();

const loading = ref(false);
const categoryList = ref<HomeCategoryItem[]>([]);
const bannerList = ref<HomeBannerItem[]>([]);
const newsList = ref<HomeNewsItem[]>([]);
const newBookList = ref<HomeNewBookItem[]>([]);
const guessLikeList = ref<HomeNewBookItem[]>([]);
const newBookPage = ref(0);
const activeCategoryId = ref<number | null>(null);
const failedImages = ref<Record<string, boolean>>({});

const hasValidImage = (url?: string) => Boolean(url && !failedImages.value[url]);
const markImageFailed = (url?: string) => {
  if (!url) return;
  failedImages.value[url] = true;
};

const NEW_DROP_PAGE_SIZE = 4;

const craftPillars = [
  {
    index: '01',
    icon: 'keyboard',
    code: 'KEYBOARDS',
    title: '客制化机械键盘',
    desc: 'CNC 阳极套件 · 热插拔轴体 · PBT 键帽',
    keyword: '键盘'
  },
  {
    index: '02',
    icon: 'mouse',
    code: 'MICE',
    title: '轻量化电竞鼠标',
    desc: '旗舰光学引擎 · 客制化微动与防滑贴',
    keyword: '鼠标'
  },
  {
    index: '03',
    icon: 'audio',
    code: 'ACOUSTICS',
    title: '桌面声学耳机',
    desc: '监听头戴 · 低延迟无线 · 桌面 DAC',
    keyword: '耳机'
  },
  {
    index: '04',
    icon: 'custom',
    code: 'BESPOKE LAB',
    title: '专属客制化定制',
    desc: '轴体搭配 · 配色方案 · 极客桌搭严选',
    keyword: ''
  }
];

const userCard = computed(() => {
  if (authStore.isLogin) {
    return {
      loggedIn: true,
      nickname: authStore.userInfo.nickname || authStore.userInfo.username || '极客会员'
    };
  }
  return {
    loggedIn: false,
    nickname: 'Hi, Maker'
  };
});

const displayCategoryList = computed<CategoryDisplayItem[]>(() => {
  return (categoryList.value || [])
    .filter(item => Number(item.pid) === 0)
    .map(item => {
      const children = (item.children || []).map(child => ({
        id: child.id,
        name: child.cateName
      }));
      return {
        id: item.id,
        name: item.cateName,
        previewChildren: children.slice(0, 2).map(child => child.name),
        children
      };
    });
});

const activeCategory = computed(() => {
  if (activeCategoryId.value === null) return null;
  return displayCategoryList.value.find(item => item.id === activeCategoryId.value) || null;
});

const displayBannerList = computed(() => {
  if (bannerList.value.length > 0) return bannerList.value;
  return [
    {
      id: 0,
      title: '咔嗒造物 · 客制化外设工坊',
      image: '',
      link: '',
      sort: 0
    }
  ] as HomeBannerItem[];
});

const displayNewsList = computed<HomeNewsItem[]>(() => {
  if (newsList.value.length > 0) return newsList.value;
  return [
    {
      id: 1,
      categoryId: 0,
      title: '咔嗒造物工坊全新视觉上线',
      tagName: '工坊',
      coverImage: '',
      summary: '',
      content: '',
      link: '',
      source: '',
      author: '',
      browseCount: 0,
      sort: 10,
      isTop: 1,
      status: 0,
      publishTime: '',
      startTime: '',
      endTime: '',
      remark: ''
    },
    {
      id: 2,
      categoryId: 0,
      title: '机械键盘客制化轴体与键帽上新补货',
      tagName: '新品',
      coverImage: '',
      summary: '',
      content: '',
      link: '',
      source: '',
      author: '',
      browseCount: 0,
      sort: 20,
      isTop: 0,
      status: 0,
      publishTime: '',
      startTime: '',
      endTime: '',
      remark: ''
    },
    {
      id: 3,
      categoryId: 0,
      title: '极简桌搭美学与外设调校指南',
      tagName: '极客志',
      coverImage: '',
      summary: '',
      content: '',
      link: '',
      source: '',
      author: '',
      browseCount: 0,
      sort: 30,
      isTop: 0,
      status: 0,
      publishTime: '',
      startTime: '',
      endTime: '',
      remark: ''
    }
  ];
});

const newBookPageCount = computed(() => {
  const total = newBookList.value.length;
  return total === 0 ? 1 : Math.ceil(total / NEW_DROP_PAGE_SIZE);
});

const canSwitchNewBookPage = computed(() => newBookList.value.length > NEW_DROP_PAGE_SIZE);

const displayNewBookList = computed(() => {
  const start = newBookPage.value * NEW_DROP_PAGE_SIZE;
  return newBookList.value.slice(start, start + NEW_DROP_PAGE_SIZE);
});

const curatedList = computed(() => guessLikeList.value.slice(0, 8));
const spotlightItems = computed(() => (curatedList.value.length >= 4 ? curatedList.value.slice(0, 2) : []));
const gridItems = computed(() => (curatedList.value.length >= 4 ? curatedList.value.slice(2, 8) : curatedList.value));

const toLogin = () => router.push('/login');
const toUserProfile = () => router.push('/user/account/profile');
const toUserFavorite = () => router.push('/user/favorite/list');
const toUserFootprint = () => router.push('/user/history/footprint');
const toUserOrders = () => router.push('/user/order/list');
const toAllProducts = () => router.push('/products');

const toPillar = (keyword: string) => {
  if (!keyword) {
    router.push('/products');
    return;
  }
  router.push(`/products?keyword=${encodeURIComponent(keyword)}`);
};

const onCategoryEnter = (id: number) => {
  activeCategoryId.value = id;
};

const clearActiveCategory = () => {
  activeCategoryId.value = null;
};

const toProductDetail = (id: number) => {
  router.push(`/product/${id}`);
};

const toCategoryProducts = (parent: CategoryDisplayItem, child?: { id: number; name: string }) => {
  if (!child) {
    router.push({
      path: '/products',
      query: {
        cid: String(parent.id),
        pname: parent.name
      }
    });
    return;
  }
  router.push({
    path: '/products',
    query: {
      cid: String(child.id),
      pid: String(parent.id),
      cname: child.name,
      pname: parent.name
    }
  });
};

const prevNewBookPage = () => {
  if (!canSwitchNewBookPage.value) return;
  newBookPage.value = (newBookPage.value - 1 + newBookPageCount.value) % newBookPageCount.value;
};

const nextNewBookPage = () => {
  if (!canSwitchNewBookPage.value) return;
  newBookPage.value = (newBookPage.value + 1) % newBookPageCount.value;
};

const openNews = (item: HomeNewsItem) => {
  if (item.link && /^https?:\/\//.test(item.link)) {
    window.open(item.link, '_blank', 'noopener,noreferrer');
    return;
  }
  router.push(`/notice/${item.id}`);
};

const openBanner = (item: HomeBannerItem) => {
  if (!item.link) return;
  if (/^https?:\/\//.test(item.link)) {
    window.open(item.link, '_blank', 'noopener,noreferrer');
    return;
  }
  router.push(item.link);
};

const resolveNewsTime = (value?: string) => {
  if (!value) return 'NEW';
  return value.replace('T', ' ').slice(0, 10);
};

const formatPrice = (value: unknown) => {
  const num = Number(value ?? 0);
  return Number.isFinite(num) ? num.toFixed(2) : '0.00';
};

const fetchHomeData = async () => {
  loading.value = true;
  try {
    const [categoryRes, bannerRes, newsRes, newBookRes, guessLikeRes] = await Promise.all([
      getHomeCategoryApi(),
      getHomeBannerApi(),
      getHomeNewsApi(3),
      getHomeNewBooksApi(12),
      getHomeGuessLikeApi(8)
    ]);

    categoryList.value = categoryRes.data || [];
    bannerList.value = bannerRes.data || [];
    newsList.value = newsRes.data || [];
    newBookList.value = newBookRes.data || [];
    guessLikeList.value = guessLikeRes.data || [];
    newBookPage.value = 0;
  } finally {
    loading.value = false;
  }
};

onMounted(async () => {
  await Promise.all([fetchHomeData(), cartStore.fetchCount()]);
});
</script>

<template>
  <div class="kc-page">
    <SiteHeader />
    <SiteMainNav :items="siteNavList" active-key="home" auto-navigate />

    <main class="kc-main home-main">
      <!-- 00 / Soft Elevated Bento Hero -->
      <section class="hero-matrix" v-loading="loading">
        <aside class="category-directory" @mouseleave="clearActiveCategory">
          <div class="directory-scroll">
            <button
              v-for="(item, idx) in displayCategoryList"
              :key="item.id"
              type="button"
              class="directory-pill"
              :class="{ active: activeCategoryId === item.id }"
              @mouseenter="onCategoryEnter(item.id)"
              @click="toCategoryProducts(item)"
            >
              <div class="directory-top">
                <span class="directory-index kc-mono">{{ String(idx + 1).padStart(2, '0') }}</span>
                <span class="directory-name">{{ item.name }}</span>
                <span class="directory-arrow" aria-hidden="true">›</span>
              </div>
              <div class="directory-sub">{{ item.previewChildren.join(' · ') || '探索全部系列' }}</div>
            </button>

            <div v-if="!displayCategoryList.length" class="directory-empty">
              外设分类加载中
            </div>
          </div>

          <transition name="drawer-fade">
            <div
              v-if="activeCategory && activeCategory.children.length > 0"
              class="directory-drawer"
              @mouseenter="onCategoryEnter(activeCategory.id)"
            >
              <div class="drawer-head">
                <span class="kc-kicker">SUB-SERIES</span>
                <h3>{{ activeCategory.name }}</h3>
              </div>
              <div class="drawer-tags">
                <button
                  v-for="child in activeCategory.children"
                  :key="child.id"
                  type="button"
                  class="drawer-tag"
                  @click.stop="toCategoryProducts(activeCategory, child)"
                >
                  {{ child.name }}
                </button>
              </div>
            </div>
          </transition>
        </aside>

        <div class="hero-stage">
          <el-carousel height="100%" indicator-position="inside" :autoplay="true" :interval="5000">
            <el-carousel-item v-for="item in displayBannerList" :key="item.id">
              <div class="banner-slide" :class="{ clickable: Boolean(item.link) }" @click="openBanner(item)">
                <img
                  v-if="hasValidImage(item.image)"
                  :src="item.image"
                  :alt="item.title"
                  @error="markImageFailed(item.image)"
                />
                <div v-else class="banner-industrial-fallback">
                  <div class="fallback-glow" aria-hidden="true" />
                  <span class="kc-mono fallback-code">KLACKCRAFT // EDITION 2026</span>
                  <strong>{{ item.title }}</strong>
                  <p>机械键盘 · 轻量化鼠标 · 桌面声学 · 深度客制化定制</p>
                </div>
                <div class="banner-overlay">
                  <span class="banner-chip kc-mono">FEATURED DROP</span>
                  <div class="banner-caption">{{ item.title }}</div>
                </div>
              </div>
            </el-carousel-item>
          </el-carousel>
        </div>

        <aside class="hero-rail">
          <div class="member-card">
            <div class="member-top">
              <el-avatar
                :size="46"
                :src="authStore.userInfo.avatar"
                class="member-avatar"
                :class="{ clickable: userCard.loggedIn }"
                @click="userCard.loggedIn && toUserProfile()"
              >
                {{ userCard.nickname.slice(0, 1) }}
              </el-avatar>
              <div class="member-meta">
                <span class="kc-mono member-role">{{ userCard.loggedIn ? 'STUDIO MEMBER' : 'GUEST ACCESS' }}</span>
                <strong class="member-name" :class="{ clickable: userCard.loggedIn }" @click="userCard.loggedIn && toUserProfile()">
                  {{ userCard.nickname }}
                </strong>
              </div>
            </div>

            <div class="member-actions">
              <button v-if="!userCard.loggedIn" type="button" class="member-cta" @click="toLogin">
                登录 / 注册极客账号
              </button>
              <button v-else type="button" class="member-cta outline" @click="toUserProfile">
                进入个人工坊 →
              </button>
            </div>

            <div class="member-shortcuts">
              <button type="button" class="shortcut-pill" @click="toUserFavorite">收藏</button>
              <button type="button" class="shortcut-pill" @click="toUserFootprint">足迹</button>
              <button type="button" class="shortcut-pill" @click="toUserOrders">订单</button>
            </div>
          </div>

          <div class="dispatch-card">
            <div class="dispatch-head">
              <span class="kc-kicker">STUDIO DISPATCH</span>
              <button type="button" class="dispatch-more" @click="router.push('/notice')">全部 →</button>
            </div>
            <div class="dispatch-list">
              <button
                v-for="item in displayNewsList"
                :key="item.id"
                type="button"
                class="dispatch-pill"
                @click="openNews(item)"
              >
                <div class="dispatch-row">
                  <span class="dispatch-tag">{{ item.tagName || '快讯' }}</span>
                  <span class="dispatch-title">{{ item.title }}</span>
                </div>
                <span class="dispatch-date kc-mono">{{ resolveNewsTime(item.publishTime) }}</span>
              </button>
            </div>
          </div>
        </aside>
      </section>

      <!-- 4 Independent Floating Craft Bento Cards (No Divider Lines) -->
      <section class="pillars-grid" aria-label="核心品类与客制化服务">
        <button
          v-for="pillar in craftPillars"
          :key="pillar.index"
          type="button"
          class="pillar-card"
          @click="toPillar(pillar.keyword)"
        >
          <div class="pillar-head">
            <span class="pillar-icon-well" aria-hidden="true">
              <svg v-if="pillar.icon === 'keyboard'" viewBox="0 0 24 24" fill="none">
                <rect x="2.5" y="6" width="19" height="12" rx="3" stroke="currentColor" stroke-width="1.75" />
                <path d="M6.5 10h.01M10 10h.01M14 10h.01M17.5 10h.01M8 14h8" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" />
              </svg>
              <svg v-else-if="pillar.icon === 'mouse'" viewBox="0 0 24 24" fill="none">
                <rect x="6.5" y="3.5" width="11" height="17" rx="5.5" stroke="currentColor" stroke-width="1.75" />
                <path d="M12 7.5v3" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" />
              </svg>
              <svg v-else-if="pillar.icon === 'audio'" viewBox="0 0 24 24" fill="none">
                <path d="M4.5 13v-1a7.5 7.5 0 0 1 15 0v1" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" />
                <rect x="3.5" y="12.5" width="4" height="6.5" rx="2" stroke="currentColor" stroke-width="1.75" />
                <rect x="16.5" y="12.5" width="4" height="6.5" rx="2" stroke="currentColor" stroke-width="1.75" />
              </svg>
              <svg v-else viewBox="0 0 24 24" fill="none">
                <path d="M4 7h16M4 12h16M4 17h16" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" />
                <circle cx="9" cy="7" r="2" fill="currentColor" />
                <circle cx="15" cy="12" r="2" fill="currentColor" />
                <circle cx="10" cy="17" r="2" fill="currentColor" />
              </svg>
            </span>
            <span class="pillar-code kc-mono">{{ pillar.index }} · {{ pillar.code }}</span>
          </div>
          <h3>{{ pillar.title }}</h3>
          <p>{{ pillar.desc }}</p>
        </button>
      </section>

      <!-- 01 / New Drops (Concentric Inset Photography, Zero Hairline Cuts) -->
      <section class="editorial-section" v-loading="loading">
        <header class="section-header">
          <div class="section-title-group">
            <span class="kc-kicker">01 / NEW DROPS</span>
            <h2>新品首发</h2>
          </div>
          <div class="section-controls">
            <span class="pager-indicator kc-mono">
              {{ newBookList.length ? `${String(newBookPage + 1).padStart(2, '0')} / ${String(newBookPageCount).padStart(2, '0')}` : '00 / 00' }}
            </span>
            <div class="pager-arrows">
              <button
                type="button"
                class="arrow-btn"
                aria-label="上一页新品"
                :disabled="!canSwitchNewBookPage"
                @click="prevNewBookPage"
              >
                ‹
              </button>
              <button
                type="button"
                class="arrow-btn"
                aria-label="下一页新品"
                :disabled="!canSwitchNewBookPage"
                @click="nextNewBookPage"
              >
                ›
              </button>
            </div>
          </div>
        </header>

        <div v-if="displayNewBookList.length > 0" class="drops-grid">
          <article
            v-for="item in displayNewBookList"
            :key="item.id"
            class="drop-card"
            @click="toProductDetail(item.id)"
          >
            <div class="drop-media-wrap">
              <div class="drop-media">
                <img
                  v-if="hasValidImage(item.image)"
                  :src="item.image"
                  :alt="item.productName"
                  @error="markImageFailed(item.image)"
                />
                <div v-else class="media-fallback">{{ item.productName }}</div>
                <span class="media-tag kc-mono">NEW</span>
              </div>
            </div>
            <div class="drop-body">
              <h3 :title="item.productName">{{ item.productName }}</h3>
              <div class="drop-foot">
                <strong class="price-readout kc-mono">¥{{ formatPrice(item.price) }}</strong>
                <span class="spec-jump">选配 →</span>
              </div>
            </div>
          </article>
        </div>
        <div v-else class="section-empty">
          <span>暂无首发外设</span>
          <button type="button" class="empty-action" @click="toAllProducts">浏览全部图鉴</button>
        </div>
      </section>

      <!-- 02 / Curated Desk Setup (2 Horizontal Editorial Spotlights + 3-Col Gallery) -->
      <section class="editorial-section" v-loading="loading">
        <header class="section-header">
          <div class="section-title-group">
            <span class="kc-kicker">02 / CURATED DESK SETUP</span>
            <h2>桌搭精选</h2>
          </div>
          <button type="button" class="section-link" @click="toAllProducts">
            浏览全部图鉴 →
          </button>
        </header>

        <template v-if="curatedList.length > 0">
          <div v-if="spotlightItems.length > 0" class="spotlight-row">
            <article
              v-for="item in spotlightItems"
              :key="item.id"
              class="spotlight-card"
              @click="toProductDetail(item.id)"
            >
              <div class="spotlight-media">
                <img
                  v-if="hasValidImage(item.image)"
                  :src="item.image"
                  :alt="item.productName"
                  @error="markImageFailed(item.image)"
                />
                <div v-else class="media-fallback">{{ item.productName }}</div>
                <span v-if="Number(item.isBest) === 1" class="lab-badge kc-mono">LAB PICK</span>
              </div>
              <div class="spotlight-copy">
                <span class="kc-mono spotlight-kicker">FLAGSHIP HARDWARE</span>
                <h3 :title="item.productName">{{ item.productName }}</h3>
                <p class="spotlight-sales kc-mono">已售 {{ item.sales || 0 }} 件 · 支持客制化选配</p>
                <div class="spotlight-bottom">
                  <strong class="price-readout kc-mono">¥{{ formatPrice(item.price) }}</strong>
                  <span class="spotlight-cta">立即选配</span>
                </div>
              </div>
            </article>
          </div>

          <div v-if="gridItems.length > 0" class="curated-grid">
            <article
              v-for="item in gridItems"
              :key="item.id"
              class="curated-card"
              @click="toProductDetail(item.id)"
            >
              <div class="curated-media-wrap">
                <div class="curated-media">
                  <img
                    v-if="hasValidImage(item.image)"
                    :src="item.image"
                    :alt="item.productName"
                    @error="markImageFailed(item.image)"
                  />
                  <div v-else class="media-fallback">{{ item.productName }}</div>
                  <span v-if="Number(item.isBest) === 1" class="lab-badge kc-mono">LAB PICK</span>
                </div>
              </div>
              <div class="curated-body">
                <h3 :title="item.productName">{{ item.productName }}</h3>
                <div class="curated-foot">
                  <strong class="price-readout kc-mono">¥{{ formatPrice(item.price) }}</strong>
                  <span class="sales-meta kc-mono">售 {{ item.sales || 0 }}</span>
                </div>
              </div>
            </article>
          </div>
        </template>
        <div v-else class="section-empty">
          <span>暂无精选外设</span>
          <button type="button" class="empty-action" @click="toAllProducts">前往外设图鉴</button>
        </div>
      </section>
    </main>

    <SiteFooter />
  </div>
</template>

<style scoped lang="scss">
.home-main {
  padding-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 36px;
}

/* 00 / Soft Elevated Bento Hero */
.hero-matrix {
  position: relative;
  display: grid;
  grid-template-columns: 236px minmax(0, 1fr) 268px;
  gap: 18px;
  align-items: stretch;
}

.category-directory {
  position: relative;
  z-index: 15;
  height: 452px;
  border-radius: 22px;
  border: 1px solid transparent;
  background: var(--kc-surface);
  box-shadow: var(--kc-shadow-md);
  padding: 10px;
  display: flex;
  flex-direction: column;
}

.directory-scroll {
  height: 100%;
  overflow-y: auto;
  overscroll-behavior: contain;
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding-bottom: 16px;
  mask-image: linear-gradient(to bottom, #000 calc(100% - 20px), transparent 100%);
  -webkit-mask-image: linear-gradient(to bottom, #000 calc(100% - 20px), transparent 100%);
  scrollbar-width: none;
}

.directory-scroll::-webkit-scrollbar {
  display: none;
}

.directory-pill {
  width: 100%;
  padding: 9px 12px;
  border: 0;
  border-radius: 12px;
  background: transparent;
  text-align: left;
  cursor: pointer;
  transition: background-color 140ms ease, transform 100ms ease-out;
}

.directory-pill:active {
  transform: scale(0.985);
}

.directory-pill.active {
  background: var(--kc-surface-muted);
}

.directory-top {
  display: flex;
  align-items: center;
  gap: 8px;
}

.directory-index {
  font-size: 11px;
  color: var(--kc-text-muted);
  transition: color 140ms ease;
}

.directory-pill.active .directory-index {
  color: var(--kc-accent);
}

.directory-name {
  flex: 1;
  font-size: 14px;
  font-weight: 600;
  color: var(--kc-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.directory-arrow {
  color: var(--kc-text-muted);
  font-size: 15px;
  transition: transform 140ms ease, color 140ms ease;
}

.directory-pill.active .directory-arrow {
  color: var(--kc-accent);
  transform: translateX(2px);
}

.directory-sub {
  margin-top: 3px;
  padding-left: 22px;
  font-size: 12px;
  color: var(--kc-text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.directory-empty {
  height: 100%;
  display: grid;
  place-items: center;
  color: var(--kc-text-muted);
  font-size: 13px;
}

.directory-drawer {
  position: absolute;
  top: 0;
  bottom: 0;
  left: calc(100% + 10px);
  width: 320px;
  padding: 22px;
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(18px);
  border: 1px solid transparent;
  border-radius: 22px;
  box-shadow: var(--kc-shadow-lg);
  z-index: 25;
  overflow-y: auto;
  overscroll-behavior: contain;
}

.drawer-head h3 {
  margin: 8px 0 18px;
  font-size: 18px;
  color: var(--kc-text);
}

.drawer-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.drawer-tag {
  padding: 8px 14px;
  border-radius: 999px;
  border: 0;
  background: var(--kc-surface-muted);
  color: var(--kc-text);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: color 140ms ease, background-color 140ms ease, transform 100ms ease-out;
}

.drawer-tag:active {
  transform: scale(0.96);
}

.drawer-fade-enter-active,
.drawer-fade-leave-active {
  transition: opacity 160ms ease, transform 160ms ease;
}

.drawer-fade-enter-from,
.drawer-fade-leave-to {
  opacity: 0;
  transform: translateX(-8px);
}

/* Center Hero Carousel */
.hero-stage {
  height: 452px;
  border-radius: 24px;
  border: 1px solid transparent;
  overflow: hidden;
  background: var(--kc-carbon);
  box-shadow: var(--kc-shadow-lg);
}

:deep(.hero-stage .el-carousel),
:deep(.hero-stage .el-carousel__container) {
  height: 100%;
}

:deep(.hero-stage .el-carousel__button) {
  width: 22px;
  height: 4px;
  border-radius: 999px;
}

.banner-slide {
  position: relative;
  width: 100%;
  height: 100%;
}

.banner-slide.clickable {
  cursor: pointer;
}

.banner-slide img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.banner-industrial-fallback {
  position: relative;
  width: 100%;
  height: 100%;
  padding: 44px;
  background:
    radial-gradient(circle at 82% 24%, rgba(255, 77, 0, 0.2), transparent 46%),
    radial-gradient(rgba(255, 255, 255, 0.08) 1px, transparent 1px),
    #15161b;
  background-size: auto, 24px 24px, auto;
  color: #ffffff;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 14px;
}

.fallback-glow {
  position: absolute;
  inset: 0;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.08);
  pointer-events: none;
}

.fallback-code {
  font-size: 11px;
  letter-spacing: 0.14em;
  color: var(--kc-accent);
}

.banner-industrial-fallback strong {
  font-size: 36px;
  line-height: 1.15;
  letter-spacing: -0.02em;
}

.banner-industrial-fallback p {
  margin: 0;
  color: #a4a8b6;
  font-size: 14px;
}

.banner-overlay {
  position: absolute;
  left: 22px;
  right: 22px;
  bottom: 24px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
  pointer-events: none;
}

.banner-chip {
  padding: 5px 11px;
  border-radius: 999px;
  background: var(--kc-accent);
  color: #ffffff;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.12em;
}

.banner-caption {
  padding: 9px 16px;
  border-radius: 12px;
  background: rgba(21, 22, 26, 0.72);
  color: #ffffff;
  font-size: 16px;
  font-weight: 700;
  backdrop-filter: blur(12px);
}

/* Right Member & Dispatch Rail */
.hero-rail {
  height: 452px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.member-card {
  padding: 18px;
  border-radius: 22px;
  background: var(--kc-carbon);
  color: #ffffff;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.09),
    var(--kc-shadow-md);
}

.member-top {
  display: flex;
  align-items: center;
  gap: 12px;
}

.member-avatar {
  background: #272a33;
  color: #ffffff;
  font-weight: 700;
  flex-shrink: 0;
}

.member-meta {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.member-role {
  font-size: 10px;
  letter-spacing: 0.12em;
  color: var(--kc-accent);
}

.member-name {
  font-size: 15px;
  color: #ffffff;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.clickable {
  cursor: pointer;
}

.member-actions {
  margin-top: 14px;
}

.member-cta {
  width: 100%;
  height: 38px;
  border-radius: 999px;
  border: 0;
  background: var(--kc-accent);
  color: #ffffff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 150ms ease, transform 100ms ease-out;
}

.member-cta:active {
  transform: scale(0.97);
}

.member-cta.outline {
  background: rgba(255, 255, 255, 0.1);
}

.member-shortcuts {
  margin-top: 12px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 6px;
}

.shortcut-pill {
  height: 30px;
  border: 0;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.06);
  color: #c6cad6;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 140ms ease, color 140ms ease, transform 100ms ease-out;
}

.shortcut-pill:active {
  transform: scale(0.96);
}

.dispatch-card {
  flex: 1;
  min-height: 0;
  padding: 16px 14px 12px;
  border-radius: 22px;
  border: 1px solid transparent;
  background: var(--kc-surface);
  box-shadow: var(--kc-shadow-md);
  display: flex;
  flex-direction: column;
}

.dispatch-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 6px;
  margin-bottom: 8px;
}

.dispatch-more {
  border: 0;
  background: transparent;
  color: var(--kc-text-muted);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}

.dispatch-list {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 4px;
}

.dispatch-pill {
  width: 100%;
  padding: 8px 8px;
  border: 0;
  border-radius: 12px;
  background: transparent;
  text-align: left;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 3px;
  transition: background-color 140ms ease;
}

.dispatch-row {
  display: flex;
  align-items: center;
  gap: 7px;
  min-width: 0;
}

.dispatch-tag {
  padding: 2px 7px;
  border-radius: 999px;
  background: var(--kc-surface-muted);
  color: var(--kc-text-secondary);
  font-size: 11px;
  font-weight: 600;
  flex-shrink: 0;
}

.dispatch-title {
  font-size: 13px;
  color: var(--kc-text);
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dispatch-date {
  padding-left: 2px;
  font-size: 11px;
  color: var(--kc-text-muted);
}

/* 4 Independent Floating Craft Bento Cards */
.pillars-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.pillar-card {
  padding: 20px;
  border: 1px solid transparent;
  border-radius: 20px;
  background: var(--kc-surface);
  box-shadow: var(--kc-shadow-sm);
  text-align: left;
  cursor: pointer;
  transition: transform 180ms ease-out, box-shadow 180ms ease;
}

.pillar-card:active {
  transform: scale(0.98);
}

.pillar-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.pillar-icon-well {
  width: 38px;
  height: 38px;
  border-radius: 12px;
  background: var(--kc-surface-muted);
  color: var(--kc-text);
  display: grid;
  place-items: center;
  transition: background-color 160ms ease, color 160ms ease;
}

.pillar-icon-well svg {
  width: 20px;
  height: 20px;
}

.pillar-code {
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.1em;
  color: var(--kc-text-muted);
}

.pillar-card h3 {
  margin: 14px 0 0;
  font-size: 15px;
  font-weight: 700;
  color: var(--kc-text);
}

.pillar-card p {
  margin: 6px 0 0;
  font-size: 12px;
  color: var(--kc-text-muted);
  line-height: 1.55;
}

/* Editorial Sections (Whitespace-driven, Zero Horizontal Rule Lines) */
.editorial-section {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.section-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  padding: 0 2px;
}

.section-title-group h2 {
  margin: 6px 0 0;
  font-size: 26px;
  font-weight: 700;
  letter-spacing: -0.025em;
  color: var(--kc-text);
  line-height: 1.1;
}

.section-controls {
  display: inline-flex;
  align-items: center;
  gap: 12px;
}

.pager-indicator {
  font-size: 12px;
  color: var(--kc-text-muted);
}

.pager-arrows {
  display: inline-flex;
  gap: 8px;
}

.arrow-btn {
  width: 36px;
  height: 36px;
  border-radius: 999px;
  border: 1px solid transparent;
  background: var(--kc-surface);
  color: var(--kc-text);
  font-size: 18px;
  line-height: 1;
  cursor: pointer;
  box-shadow: var(--kc-shadow-sm);
  transition: transform 100ms ease-out, background-color 150ms ease, color 150ms ease, box-shadow 150ms ease;
}

.arrow-btn:active:not(:disabled) {
  transform: scale(0.93);
}

.arrow-btn:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

.section-link {
  height: 36px;
  padding: 0 16px;
  border: 1px solid transparent;
  border-radius: 999px;
  background: var(--kc-surface);
  color: var(--kc-text);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: var(--kc-shadow-sm);
  transition: transform 100ms ease-out, color 150ms ease, box-shadow 150ms ease;
}

.section-link:active {
  transform: scale(0.97);
}

/* 01 / New Drops 4-Col Grid (Concentric Radii: 20px outer - 8px padding = 12px inner) */
.drops-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}

.drop-card {
  border-radius: 20px;
  border: 1px solid transparent;
  background: var(--kc-surface);
  box-shadow: var(--kc-shadow-sm);
  cursor: pointer;
  display: flex;
  flex-direction: column;
  transition: transform 200ms ease-out, box-shadow 200ms ease;
}

.drop-card:active {
  transform: scale(0.985);
}

.drop-media-wrap,
.curated-media-wrap {
  padding: 8px 8px 0;
}

.drop-media {
  position: relative;
  aspect-ratio: 4 / 3;
  border-radius: 12px;
  background: var(--kc-surface-muted);
  overflow: hidden;
  outline: 1px solid oklch(0 0 0 / 0.08);
  outline-offset: -1px;
}

.drop-media img,
.spotlight-media img,
.curated-media img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  transition: transform 360ms cubic-bezier(0.22, 1, 0.36, 1);
}

.media-fallback {
  width: 100%;
  height: 100%;
  display: grid;
  place-items: center;
  padding: 18px;
  text-align: center;
  background: var(--kc-surface-muted);
  color: var(--kc-text-secondary);
  font-size: 13px;
  font-weight: 600;
}

.media-tag {
  position: absolute;
  top: 10px;
  left: 10px;
  padding: 4px 9px;
  border-radius: 999px;
  background: rgba(21, 22, 26, 0.82);
  backdrop-filter: blur(8px);
  color: #ffffff;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.drop-body {
  padding: 14px 16px 16px;
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 12px;
}

.drop-body h3,
.curated-body h3 {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  line-height: 1.45;
  color: var(--kc-text);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 40px;
}

.drop-foot,
.curated-foot {
  margin-top: auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.price-readout {
  font-size: 18px;
  font-weight: 700;
  color: var(--kc-text);
}

.spec-jump {
  padding: 4px 10px;
  border-radius: 999px;
  background: var(--kc-surface-muted);
  font-size: 12px;
  font-weight: 600;
  color: var(--kc-text-secondary);
  transition: background-color 150ms ease, color 150ms ease;
}

/* 02 / Curated Spotlight (24px outer - 10px inset = 14px inner) + 3-Col Gallery */
.spotlight-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.spotlight-card {
  padding: 10px;
  border-radius: 24px;
  border: 1px solid transparent;
  background: var(--kc-surface);
  box-shadow: var(--kc-shadow-sm);
  display: grid;
  grid-template-columns: 236px minmax(0, 1fr);
  gap: 18px;
  cursor: pointer;
  transition: transform 200ms ease-out, box-shadow 200ms ease;
}

.spotlight-card:active {
  transform: scale(0.99);
}

.spotlight-media {
  position: relative;
  min-height: 204px;
  border-radius: 14px;
  background: var(--kc-surface-muted);
  overflow: hidden;
  outline: 1px solid oklch(0 0 0 / 0.08);
  outline-offset: -1px;
}

.spotlight-copy {
  padding: 12px 12px 10px 4px;
  display: flex;
  flex-direction: column;
}

.spotlight-kicker {
  font-size: 10px;
  letter-spacing: 0.12em;
  color: var(--kc-accent);
  font-weight: 700;
}

.spotlight-copy h3 {
  margin: 8px 0 0;
  font-size: 18px;
  font-weight: 700;
  line-height: 1.35;
  color: var(--kc-text);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.spotlight-sales {
  margin: 10px 0 0;
  font-size: 12px;
  color: var(--kc-text-muted);
}

.spotlight-bottom {
  margin-top: auto;
  padding-top: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.spotlight-bottom .price-readout {
  font-size: 22px;
}

.spotlight-cta {
  padding: 7px 14px;
  border-radius: 999px;
  background: var(--kc-surface-muted);
  font-size: 12px;
  font-weight: 600;
  color: var(--kc-text);
  transition: background-color 150ms ease, color 150ms ease;
}

.lab-badge {
  position: absolute;
  top: 10px;
  left: 10px;
  padding: 4px 10px;
  border-radius: 999px;
  background: var(--kc-accent);
  color: #ffffff;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.curated-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.curated-card {
  border-radius: 20px;
  border: 1px solid transparent;
  background: var(--kc-surface);
  box-shadow: var(--kc-shadow-sm);
  cursor: pointer;
  display: flex;
  flex-direction: column;
  transition: transform 200ms ease-out, box-shadow 200ms ease;
}

.curated-card:active {
  transform: scale(0.985);
}

.curated-media {
  position: relative;
  aspect-ratio: 16 / 10;
  border-radius: 12px;
  background: var(--kc-surface-muted);
  overflow: hidden;
  outline: 1px solid oklch(0 0 0 / 0.08);
  outline-offset: -1px;
}

.curated-body {
  padding: 14px 16px 16px;
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 10px;
}

.sales-meta {
  font-size: 12px;
  color: var(--kc-text-muted);
}

.section-empty {
  padding: 44px 24px;
  border-radius: 20px;
  background: var(--kc-surface);
  box-shadow: var(--kc-shadow-sm);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
  color: var(--kc-text-muted);
  font-size: 14px;
}

.empty-action {
  height: 38px;
  padding: 0 20px;
  border-radius: 999px;
  border: 0;
  background: var(--kc-carbon);
  color: #ffffff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

/* Hover States Scoped to Hover-Capable Pointers (ui-skills: jakubkrehel/better-ui) */
@media (hover: hover) {
  .directory-pill:hover {
    background: var(--kc-surface-muted);
  }

  .drawer-tag:hover {
    background: var(--kc-accent-soft);
    color: var(--kc-accent);
  }

  .member-cta:hover {
    background: var(--kc-accent-hover);
  }

  .member-cta.outline:hover {
    background: rgba(255, 77, 0, 0.24);
  }

  .shortcut-pill:hover {
    background: rgba(255, 255, 255, 0.14);
    color: #ffffff;
  }

  .dispatch-more:hover {
    color: var(--kc-accent);
  }

  .dispatch-pill:hover {
    background: var(--kc-surface-muted);
  }

  .dispatch-pill:hover .dispatch-title {
    color: var(--kc-accent);
  }

  .pillar-card:hover {
    transform: translateY(-2px);
    box-shadow: var(--kc-shadow-md);
  }

  .pillar-card:hover .pillar-icon-well {
    background: var(--kc-accent-soft);
    color: var(--kc-accent);
  }

  .arrow-btn:hover:not(:disabled) {
    background: var(--kc-carbon);
    color: #ffffff;
    box-shadow: var(--kc-shadow-md);
  }

  .section-link:hover {
    color: var(--kc-accent);
    box-shadow: var(--kc-shadow-md);
  }

  .drop-card:hover,
  .spotlight-card:hover,
  .curated-card:hover {
    transform: translateY(-3px);
    box-shadow: var(--kc-shadow-md);
  }

  .drop-card:hover .drop-media img,
  .spotlight-card:hover .spotlight-media img,
  .curated-card:hover .curated-media img {
    transform: scale(1.04);
  }

  .drop-card:hover .spec-jump,
  .spotlight-card:hover .spotlight-cta {
    background: var(--kc-carbon);
    color: #ffffff;
  }
}

@media (max-width: 1120px) {
  .hero-matrix {
    grid-template-columns: 220px minmax(0, 1fr);
  }

  .hero-rail {
    grid-column: 1 / -1;
    height: auto;
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .pillars-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .drops-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .spotlight-row {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .hero-matrix {
    grid-template-columns: 1fr;
  }

  .category-directory {
    height: auto;
    max-height: 280px;
  }

  .directory-drawer {
    display: none;
  }

  .hero-stage {
    height: 290px;
  }

  .hero-rail,
  .pillars-grid,
  .curated-grid {
    grid-template-columns: 1fr;
  }

  .spotlight-card {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 560px) {
  .drops-grid {
    grid-template-columns: 1fr;
  }
}
</style>
