<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { getHomeFlashSaleApi, type HomeFlashSaleItem } from '@/api';
import SiteHeader from '@/components/SiteHeader.vue';
import SiteMainNav from '@/components/SiteMainNav.vue';
import SiteFooter from '@/components/SiteFooter.vue';
import { siteNavList } from '@/utils/userCenter';
import { useCartStore } from '@/stores/modules/cart';

const router = useRouter();
const cartStore = useCartStore();

const loading = ref(false);
const flashSaleList = ref<HomeFlashSaleItem[]>([]);
const flashTick = ref(0);

let flashTimer: number | null = null;

const displayFlashSaleList = computed(() => {
  return (flashSaleList.value || []).slice(0, 8).map(item => {
    const remainingSeconds = Math.max(0, Number(item.remainingSeconds || 0) - flashTick.value);
    return {
      ...item,
      remainingSeconds,
      canRush: Boolean(item.canRush) && remainingSeconds > 0 && Number(item.stock || 0) > 0
    };
  });
});

const activeCount = computed(() => displayFlashSaleList.value.filter(item => item.canRush).length);
const totalStock = computed(() => displayFlashSaleList.value.reduce((sum, item) => sum + Number(item.stock || 0), 0));

const toProductDetail = (id: number) => {
  router.push(`/product/${id}`);
};

const formatCountdown = (seconds: number) => {
  const safe = Math.max(0, Number(seconds || 0));
  const hours = String(Math.floor(safe / 3600)).padStart(2, '0');
  const minutes = String(Math.floor((safe % 3600) / 60)).padStart(2, '0');
  const secs = String(safe % 60).padStart(2, '0');
  return `${hours}:${minutes}:${secs}`;
};

const stopFlashTicker = () => {
  if (flashTimer !== null) {
    window.clearInterval(flashTimer);
    flashTimer = null;
  }
};

const startFlashTicker = () => {
  stopFlashTicker();
  flashTimer = window.setInterval(() => {
    flashTick.value += 1;
  }, 1000);
};

const fetchFlashSaleList = async () => {
  loading.value = true;
  try {
    const res = await getHomeFlashSaleApi(8);
    flashSaleList.value = res.data || [];
    flashTick.value = 0;
  } finally {
    loading.value = false;
  }
};

onMounted(async () => {
  startFlashTicker();
  await Promise.all([fetchFlashSaleList(), cartStore.fetchCount()]);
});

onBeforeUnmount(() => {
  stopFlashTicker();
});
</script>

<template>
  <div class="kc-page">
    <SiteHeader />
    <SiteMainNav :items="siteNavList" active-key="flash" auto-navigate />

    <main class="kc-main flash-main">
      <!-- Anodized Carbon Telemetry Hero -->
      <section class="flash-hero">
        <div class="hero-copy">
          <span class="hero-kicker kc-mono">LIMITED DROP // FLASH TELEMETRY</span>
          <h1>限时闪购</h1>
          <p>限量客制化外设与工坊特惠场次。倒计时归零或库存清零后，状态将实时同步更新。</p>
        </div>

        <div class="hero-metrics kc-mono">
          <div class="metric-box">
            <span>LISTED</span>
            <strong>{{ String(displayFlashSaleList.length).padStart(2, '0') }}</strong>
            <small>当前场次</small>
          </div>
          <div class="metric-box accent">
            <span>LIVE</span>
            <strong>{{ String(activeCount).padStart(2, '0') }}</strong>
            <small>可抢外设</small>
          </div>
          <div class="metric-box">
            <span>STOCK</span>
            <strong>{{ totalStock }}</strong>
            <small>剩余席位</small>
          </div>
        </div>
      </section>

      <section class="flash-board" v-loading="loading">
        <div class="board-head">
          <div>
            <span class="kc-kicker">DROP ROSTER</span>
            <h2>当前闪购清单</h2>
          </div>
          <span class="board-note kc-mono">REAL-TIME SYNC</span>
        </div>

        <div v-if="displayFlashSaleList.length > 0" class="flash-grid">
          <article v-for="item in displayFlashSaleList" :key="item.id" class="flash-card">
            <div class="flash-media-wrap">
              <button type="button" class="flash-media" @click="toProductDetail(item.productId)">
                <img v-if="item.image" :src="item.image" :alt="item.productName" />
                <div v-else class="media-fallback">{{ item.productName }}</div>
                <span class="flash-badge kc-mono">FLASH</span>
                <span class="flash-status kc-mono" :class="{ live: item.canRush }">
                  {{ item.canRush ? 'LIVE' : item.statusText || 'ENDED' }}
                </span>
              </button>
            </div>

            <div class="flash-body">
              <h3 :title="item.productName" @click="toProductDetail(item.productId)">{{ item.productName }}</h3>

              <div class="price-line kc-mono">
                <strong>¥{{ Number(item.flashPrice || 0).toFixed(2) }}</strong>
                <span>¥{{ Number(item.price || 0).toFixed(2) }}</span>
              </div>

              <div class="meta-grid kc-mono">
                <div><span>已售</span><strong>{{ item.sales || 0 }}</strong></div>
                <div><span>限购</span><strong>{{ item.limitPerUser || 1 }} 件</strong></div>
                <div><span>库存</span><strong>{{ item.stock || 0 }}</strong></div>
              </div>

              <div class="countdown-bar kc-mono" :class="{ muted: !item.canRush }">
                <span>REMAINING</span>
                <strong>{{ formatCountdown(item.remainingSeconds) }}</strong>
              </div>

              <el-button
                type="danger"
                class="rush-btn"
                :disabled="!item.canRush"
                @click="toProductDetail(item.productId)"
              >
                {{ item.canRush ? '立即抢购' : item.statusText || '已结束' }}
              </el-button>
            </div>
          </article>
        </div>

        <div v-else class="flash-empty">
          <p>当前暂无进行中的限时闪购外设</p>
          <button type="button" class="empty-btn" @click="router.push('/products')">浏览外设图鉴</button>
        </div>
      </section>
    </main>

    <SiteFooter />
  </div>
</template>

<style scoped lang="scss">
.flash-main {
  display: flex;
  flex-direction: column;
  gap: 28px;
}

.flash-hero {
  padding: 32px 36px;
  border-radius: 26px;
  background:
    radial-gradient(circle at 86% 22%, rgba(255, 77, 0, 0.2), transparent 44%),
    radial-gradient(rgba(255, 255, 255, 0.08) 1px, transparent 1px),
    var(--kc-carbon);
  background-size: auto, 22px 22px, auto;
  color: #ffffff;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.08),
    var(--kc-shadow-lg);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 28px;
}

.hero-kicker {
  display: inline-block;
  font-size: 11px;
  letter-spacing: 0.14em;
  color: var(--kc-accent);
}

.hero-copy h1 {
  margin: 10px 0 0;
  font-size: 34px;
  line-height: 1.1;
  letter-spacing: -0.025em;
}

.hero-copy p {
  margin: 10px 0 0;
  max-width: 540px;
  font-size: 14px;
  line-height: 1.7;
  color: #9ba0ae;
}

.hero-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(116px, 1fr));
  gap: 12px;
}

.metric-box {
  padding: 16px 18px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.05);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.07);
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.metric-box.accent {
  background: rgba(255, 77, 0, 0.16);
}

.metric-box span {
  font-size: 10px;
  letter-spacing: 0.1em;
  color: #8c919f;
}

.metric-box strong {
  font-size: 28px;
  line-height: 1;
  color: #ffffff;
}

.metric-box.accent strong {
  color: var(--kc-accent);
}

.metric-box small {
  font-family: var(--kc-font-sans);
  font-size: 12px;
  color: #b4b8c5;
}

.board-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 18px;
  padding: 0 2px;
}

.board-head h2 {
  margin: 6px 0 0;
  font-size: 24px;
  letter-spacing: -0.02em;
  color: var(--kc-text);
}

.board-note {
  padding: 6px 12px;
  border-radius: 999px;
  background: var(--kc-surface);
  box-shadow: var(--kc-shadow-sm);
  font-size: 11px;
  letter-spacing: 0.08em;
  color: var(--kc-text-muted);
}

.flash-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}

.flash-card {
  border-radius: 20px;
  border: 1px solid transparent;
  background: var(--kc-surface);
  box-shadow: var(--kc-shadow-sm);
  display: flex;
  flex-direction: column;
  transition: transform 200ms ease-out, box-shadow 200ms ease;
}

.flash-media-wrap {
  padding: 8px 8px 0;
}

.flash-media {
  position: relative;
  width: 100%;
  aspect-ratio: 1 / 1;
  border: 0;
  padding: 0;
  border-radius: 12px;
  background: var(--kc-surface-muted);
  outline: 1px solid oklch(0 0 0 / 0.08);
  outline-offset: -1px;
  overflow: hidden;
  cursor: pointer;
}

.flash-media img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.media-fallback {
  width: 100%;
  height: 100%;
  display: grid;
  place-items: center;
  padding: 18px;
  color: var(--kc-text-secondary);
  font-size: 13px;
  font-weight: 600;
}

.flash-badge {
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

.flash-status {
  position: absolute;
  top: 10px;
  right: 10px;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(21, 22, 26, 0.78);
  backdrop-filter: blur(8px);
  color: #d0d3dc;
  font-size: 10px;
  font-weight: 600;
}

.flash-status.live {
  color: #4ade80;
}

.flash-body {
  padding: 14px 16px 16px;
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 12px;
}

.flash-body h3 {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  line-height: 1.45;
  color: var(--kc-text);
  min-height: 40px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  cursor: pointer;
}

.price-line {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.price-line strong {
  font-size: 24px;
  color: var(--kc-accent);
  line-height: 1;
}

.price-line span {
  font-size: 13px;
  color: var(--kc-text-muted);
  text-decoration: line-through;
}

.meta-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 6px;
  font-size: 11px;
}

.meta-grid div {
  padding: 7px 8px;
  border-radius: 10px;
  background: var(--kc-surface-muted);
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.meta-grid span {
  color: var(--kc-text-muted);
}

.meta-grid strong {
  color: var(--kc-text);
  font-size: 12px;
}

.countdown-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 9px 12px;
  border-radius: 12px;
  background: var(--kc-carbon);
  color: #9ba0ae;
  font-size: 11px;
}

.countdown-bar strong {
  font-size: 14px;
  color: #ffffff;
}

.countdown-bar.muted {
  background: var(--kc-surface-muted);
  color: var(--kc-text-muted);
}

.countdown-bar.muted strong {
  color: var(--kc-text-muted);
}

.rush-btn {
  margin-top: auto;
  height: 40px;
  border-radius: 999px;
  font-weight: 700;
}

.flash-empty {
  padding: 56px 24px;
  border-radius: 22px;
  background: var(--kc-surface);
  box-shadow: var(--kc-shadow-sm);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
  color: var(--kc-text-muted);
}

.flash-empty p {
  margin: 0;
}

.empty-btn {
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

@media (hover: hover) {
  .flash-card:hover {
    transform: translateY(-3px);
    box-shadow: var(--kc-shadow-md);
  }
}

@media (max-width: 1080px) {
  .flash-hero {
    flex-direction: column;
    align-items: flex-start;
  }

  .hero-metrics {
    width: 100%;
  }

  .flash-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 600px) {
  .flash-hero {
    padding: 22px;
    border-radius: 20px;
  }

  .hero-metrics,
  .flash-grid {
    grid-template-columns: 1fr;
  }
}
</style>
