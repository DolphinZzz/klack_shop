<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import {
  createFavoriteApi,
  createCartItemApi,
  createPreOrderApi,
  deleteFavoriteByProductApi,
  getCartListApi,
  getProductCommentListApi,
  getProductDetailApi,
  getFavoriteStatusApi,
  recordUserFootprintApi,
  reserveFlashSaleApi,
  type ProductCommentItem,
  type ProductDetailData,
  type ProductDetailSku,
  type ProductFlashSaleInfo
} from '@/api';
import SiteHeader from '@/components/SiteHeader.vue';
import SiteMainNav from '@/components/SiteMainNav.vue';
import SiteFooter from '@/components/SiteFooter.vue';
import { siteNavList } from '@/utils/userCenter';
import { useAuthStore } from '@/stores/modules/auth';
import { useCartStore } from '@/stores/modules/cart';

type SkuEntry = {
  key: string;
  normalizedKey: string;
  parts: string[];
  value: ProductDetailSku;
};

type SkuGroup = {
  index: number;
  label: string;
  options: string[];
};

type PurchasePayload = {
  productId: number;
  skuId: number;
  cartNum: number;
};

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const cartStore = useCartStore();

const loading = ref(false);
const buyNowLoading = ref(false);
const flashLoading = ref(false);
const quantity = ref(1);
const detailData = ref<ProductDetailData | null>(null);
const activeImage = ref('');
const failedImages = ref<Record<string, boolean>>({});

const hasValidImage = (url?: string) => Boolean(url && !failedImages.value[url]);
const markImageFailed = (url?: string) => {
  if (!url) return;
  failedImages.value[url] = true;
};
const selectedSkuKey = ref('');
const selectedSkuParts = ref<string[]>([]);
const activeContentTab = ref<'detail' | 'comment'>('detail');
const commentLoading = ref(false);
const commentList = ref<ProductCommentItem[]>([]);
const commentPage = ref(1);
const commentPageSize = ref(10);
const commentTotal = ref(0);
const flashNow = ref(0);
const favoriteLoading = ref(false);
const favoriteState = ref({
  collected: false,
  favoriteId: 0
});

let flashTimer: number | null = null;

const productInfo = computed(() => detailData.value?.productInfo || null);
const flashSale = computed<ProductFlashSaleInfo | null>(() => detailData.value?.flashSale || null);
const normalizeSkuPart = (part: string) => part.trim();
const normalizeSkuKey = (key: string) => key.split(',').map(normalizeSkuPart).filter(Boolean).join(',');
const buildSkuKey = (parts: string[]) => parts.map(normalizeSkuPart).filter(Boolean).join(',');

const skuEntries = computed<SkuEntry[]>(() => {
  const map = detailData.value?.productValue || {};
  return Object.keys(map).map(key => ({
    key,
    parts: key.split(',').map(part => part.trim()),
    normalizedKey: normalizeSkuKey(key),
    value: map[key]
  }));
});

const skuEntryMap = computed(() => {
  const map = new Map<string, SkuEntry>();
  skuEntries.value.forEach(entry => {
    map.set(entry.normalizedKey, entry);
  });
  return map;
});

const skuGroups = computed<SkuGroup[]>(() => {
  const attrGroups = (detailData.value?.productAttr || [])
    .map((attr, index) => {
      const options = (attr.optionList || [])
        .map(option => String(option.attrName || option.optionName || '').trim())
        .filter(Boolean);

      return {
        index,
        label: attr.attrName || `配置项 ${index + 1}`,
        options: Array.from(new Set(options))
      };
    })
    .filter(group => group.options.length > 0);
  if (attrGroups.length > 0) {
    return attrGroups;
  }

  if (!skuEntries.value.length) return [];
  const partCount = Math.max(...skuEntries.value.map(item => item.parts.length));
  const groups: SkuGroup[] = [];
  for (let i = 0; i < partCount; i++) {
    const optionMap = new Map<string, boolean>();
    skuEntries.value.forEach(item => {
      const option = item.parts[i];
      if (option) optionMap.set(option, true);
    });
    groups.push({
      index: i,
      label: `配置项 ${i + 1}`,
      options: Array.from(optionMap.keys())
    });
  }
  return groups;
});

const selectedSku = computed<ProductDetailSku | null>(() => {
  if (!selectedSkuKey.value) return null;
  return skuEntryMap.value.get(normalizeSkuKey(selectedSkuKey.value))?.value || null;
});

const galleryImages = computed<string[]>(() => {
  const slider = productInfo.value?.sliderImage || '';
  const sliderImages = slider
    .split(',')
    .map(item => item.trim())
    .filter(Boolean);
  if (sliderImages.length > 0) {
    return Array.from(new Set(sliderImages));
  }
  const baseImage = productInfo.value?.image || '';
  return baseImage ? [baseImage] : [];
});

const originPrice = computed(() => {
  if (selectedSku.value?.price != null) return Number(selectedSku.value.price);
  return Number(productInfo.value?.price || 0);
});

const resolvePositivePrice = (value: unknown) => {
  const price = Number(value);
  return Number.isFinite(price) && price > 0 ? price : null;
};

const flashDisplayPrice = computed(() => {
  const skuFlashPrice = resolvePositivePrice(selectedSku.value?.flashPrice);
  if (skuFlashPrice != null) return skuFlashPrice;

  const productFlashPrice = resolvePositivePrice(flashSale.value?.flashPrice);
  if (productFlashPrice != null) return productFlashPrice;

  return originPrice.value;
});

const displayOtPrice = computed(() => {
  if (selectedSku.value?.otPrice != null) return Number(selectedSku.value.otPrice);
  return Number(productInfo.value?.otPrice || 0);
});

const displayStock = computed(() => {
  if (selectedSku.value?.stock != null) return Number(selectedSku.value.stock);
  return Number(productInfo.value?.stock || 0);
});

const flashRemainingSeconds = computed(() => {
  if (!flashSale.value) return 0;
  return Math.max(0, Number(flashSale.value.remainingSeconds || 0) - flashNow.value);
});

const canFlashRush = computed(() => {
  if (!flashSale.value) return false;
  return Boolean(flashSale.value.canRush) && flashRemainingSeconds.value > 0 && displayStock.value > 0;
});

const flashActionText = computed(() => {
  if (!flashSale.value) return '闪购未开启';
  if (displayStock.value <= 0) return '已售罄';
  if (canFlashRush.value) return '立即闪购';
  return flashSale.value.statusText || '已结束';
});

const keywordList = computed(() =>
  (productInfo.value?.keyword || '')
    .split(',')
    .map(item => item.trim())
    .filter(Boolean)
);

const postageText = computed(() => {
  const postage = Number(productInfo.value?.postage || 0);
  return postage > 0 ? `¥${postage.toFixed(2)}` : '免运费';
});

const selectedSkuLabel = computed(() => selectedSkuParts.value.filter(Boolean).join(' / '));
const isCollected = computed(() => Boolean(favoriteState.value.collected));

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
    flashNow.value += 1;
  }, 1000);
};

const recordFootprint = async (productId: number) => {
  if (!authStore.isLogin || !productId) return;
  try {
    await recordUserFootprintApi(productId);
  } catch {
    // ignore
  }
};

const resetFavoriteState = () => {
  favoriteState.value = {
    collected: false,
    favoriteId: 0
  };
};

const loadFavoriteStatus = async (productId: number) => {
  if (!authStore.isLogin || !productId) {
    resetFavoriteState();
    return;
  }

  try {
    const res = await getFavoriteStatusApi(productId);
    favoriteState.value = {
      collected: Boolean(res.data?.collected),
      favoriteId: Number(res.data?.favoriteId || 0)
    };
  } catch {
    resetFavoriteState();
  }
};

const changeImage = (url: string) => {
  if (!url) return;
  activeImage.value = url;
};

const selectSku = (entry: SkuEntry) => {
  selectedSkuKey.value = entry.normalizedKey;
  selectedSkuParts.value = [...entry.parts];
  if (entry.value?.image) {
    activeImage.value = entry.value.image;
  }
};

const pickSkuOption = (groupIndex: number, option: string) => {
  if (!skuGroups.value.length) return;
  const nextParts = Array.from({ length: skuGroups.value.length }, (_, idx) => selectedSkuParts.value[idx] || '');
  nextParts[groupIndex] = option;

  const exact = skuEntryMap.value.get(buildSkuKey(nextParts));
  if (exact) {
    selectSku(exact);
    return;
  }

  const candidate = skuEntries.value.find(entry => {
    return entry.parts[groupIndex] === option && nextParts.every((part, idx) => !part || entry.parts[idx] === part);
  });
  if (candidate) {
    selectSku(candidate);
    return;
  }

  const looseCandidate = skuEntries.value.find(entry => entry.parts[groupIndex] === option);
  if (looseCandidate) {
    selectSku(looseCandidate);
  }
};

const isOptionActive = (groupIndex: number, option: string) => {
  return selectedSkuParts.value[groupIndex] === option;
};

const decreaseQuantity = () => {
  quantity.value = Math.max(1, quantity.value - 1);
};

const increaseQuantity = () => {
  quantity.value = Math.min(displayStock.value || 1, quantity.value + 1);
};

const resolvePurchasePayload = (): PurchasePayload | null => {
  if (!authStore.isLogin) {
    ElMessage.warning('请先登录');
    router.push('/login');
    return null;
  }
  const currentProductId = Number(productInfo.value?.id || 0);
  const currentSkuId = Number(selectedSku.value?.id || 0);
  if (!currentProductId || !currentSkuId) {
    ElMessage.warning('请选择外设规格');
    return null;
  }
  if (displayStock.value <= 0) {
    ElMessage.warning('商品库存不足');
    return null;
  }
  return {
    productId: currentProductId,
    skuId: currentSkuId,
    cartNum: quantity.value
  };
};

const addToCart = async (payload: PurchasePayload, successMessage?: string) => {
  await createCartItemApi(payload);
  await cartStore.fetchCount();
  if (successMessage) {
    ElMessage.success(successMessage);
  }
};

const findCartItemId = async (payload: PurchasePayload) => {
  const res = await getCartListApi({ page: 1, limit: 500, status: 0 });
  const target = (res.data?.rows || []).find(item => {
    return Number(item.productId) === payload.productId && Number(item.skuId) === payload.skuId;
  });
  return Number(target?.id || 0);
};

const onAddCart = async () => {
  const payload = resolvePurchasePayload();
  if (!payload) return;
  await addToCart(payload, '已加入购物车');
};

const onBuyNow = async () => {
  if (flashSale.value && canFlashRush.value) {
    await onFlashRush();
    return;
  }

  const payload = resolvePurchasePayload();
  if (!payload || buyNowLoading.value) return;

  buyNowLoading.value = true;
  try {
    await addToCart(payload);
    const cartId = await findCartItemId(payload);
    if (!cartId) {
      ElMessage.error('购物车商品定位失败，请稍后重试');
      return;
    }

    const res = await createPreOrderApi({
      shoppingCartIds: [cartId],
      preOrderType: 'cart'
    });
    const orderNo = res.data?.orderNo;
    if (!orderNo) {
      ElMessage.error('立即购买失败，缺少订单号');
      return;
    }
    router.push({ path: '/order/confirm', query: { orderNo } });
  } catch {
    // handled by http interceptor
  } finally {
    buyNowLoading.value = false;
  }
};

const toggleFavorite = async () => {
  const productId = Number(productInfo.value?.id || 0);
  if (!productId || favoriteLoading.value) return;

  if (!authStore.isLogin) {
    ElMessage.warning('请先登录后再收藏外设');
    router.push({ path: '/login', query: { redirect: route.fullPath } });
    return;
  }

  favoriteLoading.value = true;
  try {
    if (isCollected.value) {
      await deleteFavoriteByProductApi(productId);
      favoriteState.value = { collected: false, favoriteId: 0 };
      ElMessage.success('已取消收藏');
    } else {
      await createFavoriteApi(productId);
      favoriteState.value = { collected: true, favoriteId: favoriteState.value.favoriteId };
      ElMessage.success('已加入收藏清单');
    }
    await loadFavoriteStatus(productId);
  } finally {
    favoriteLoading.value = false;
  }
};

const onFlashRush = async () => {
  const payload = resolvePurchasePayload();
  if (!payload || flashLoading.value) return;
  if (!flashSale.value) {
    ElMessage.warning('当前商品未开启闪购');
    return;
  }
  if (!canFlashRush.value) {
    ElMessage.warning(flashActionText.value);
    return;
  }

  flashLoading.value = true;
  try {
    const res = await reserveFlashSaleApi(payload);
    const orderNo = res.data?.orderNo;
    if (!orderNo) {
      ElMessage.error('闪购预下单失败，请稍后重试');
      return;
    }
    ElMessage.success('抢购成功，请尽快提交订单');
    router.push({ path: '/order/confirm', query: { orderNo } });
  } finally {
    flashLoading.value = false;
  }
};

const loadProductDetail = async () => {
  const id = route.params.id as string;
  if (!id) return;

  loading.value = true;
  try {
    const res = await getProductDetailApi(id);
    detailData.value = res.data;
    flashNow.value = 0;

    const defaultSku = skuEntries.value.find(item => item.value.isDefault) || skuEntries.value[0];
    if (defaultSku) {
      selectSku(defaultSku);
    }

    activeImage.value = galleryImages.value[0] || '';
    quantity.value = 1;

    await loadFavoriteStatus(Number(detailData.value?.productInfo?.id || id));
    void recordFootprint(Number(detailData.value?.productInfo?.id || id));
  } catch {
    detailData.value = null;
    resetFavoriteState();
    ElMessage.error('外设详情获取失败');
  } finally {
    loading.value = false;
  }
};

const loadCommentList = async (resetPage = false) => {
  const id = route.params.id as string;
  if (!id) return;
  if (resetPage) {
    commentPage.value = 1;
  }

  commentLoading.value = true;
  try {
    const res = await getProductCommentListApi(id, {
      page: commentPage.value,
      limit: commentPageSize.value
    });
    const data = res.data;
    commentList.value = data?.rows || [];
    commentTotal.value = Number(data?.total || 0);
  } catch {
    commentList.value = [];
    commentTotal.value = 0;
    ElMessage.error('评价加载失败');
  } finally {
    commentLoading.value = false;
  }
};

const switchToCommentTab = async () => {
  activeContentTab.value = 'comment';
  if (!commentList.value.length && commentTotal.value === 0) {
    await loadCommentList();
  }
};

const handleCommentPageChange = async (nextPage: number) => {
  commentPage.value = nextPage;
  await loadCommentList();
  window.scrollTo({ top: 0, behavior: 'smooth' });
};

const getCommentPics = (value?: string | null) => {
  if (!value) return [] as string[];
  return value
    .split(/[\n\r,]+/)
    .map(item => item.trim())
    .filter(Boolean);
};

const getUserDisplayName = (item: ProductCommentItem) => item.userName || '匿名极客';

const getUserInitial = (item: ProductCommentItem) => {
  const value = getUserDisplayName(item).trim();
  return value ? value.slice(0, 1) : 'K';
};

const formatDateTime = (value?: string) => {
  if (!value) return '-';
  return value.replace('T', ' ');
};

watch(
  () => route.params.id,
  () => {
    activeContentTab.value = 'detail';
    loadProductDetail();
    loadCommentList(true);
  },
  { immediate: true }
);

watch(selectedSku, sku => {
  const stock = Number(sku?.stock ?? productInfo.value?.stock ?? 1);
  if (stock <= 0) {
    quantity.value = 1;
    return;
  }
  if (quantity.value > stock) quantity.value = stock;
  if (quantity.value < 1) quantity.value = 1;
});

onMounted(() => {
  startFlashTicker();
  void cartStore.fetchCount();
});

onBeforeUnmount(() => {
  stopFlashTicker();
});
</script>

<template>
  <div class="kc-page">
    <SiteHeader />
    <SiteMainNav :items="siteNavList" active-key="products" auto-navigate />

    <main class="kc-main detail-main-wrap">
      <div class="detail-crumbs">
        <button type="button" class="crumb-btn" @click="router.push('/home')">工坊首页</button>
        <span>/</span>
        <button type="button" class="crumb-btn" @click="router.push('/products')">外设图鉴</button>
        <span>/</span>
        <span class="crumb-current">{{ productInfo?.productName || '外设规格' }}</span>
      </div>

      <section class="spec-sheet kc-surface" v-loading="loading">
        <template v-if="productInfo">
          <div class="gallery-col">
            <div class="gallery-viewport">
              <img
                v-if="hasValidImage(activeImage)"
                :src="activeImage"
                :alt="productInfo.productName"
                @error="markImageFailed(activeImage)"
              />
              <div v-else class="img-fallback">{{ productInfo.productName }}</div>
            </div>

            <div class="gallery-thumbs" v-if="galleryImages.length > 1">
              <button
                v-for="img in galleryImages"
                :key="img"
                type="button"
                class="thumb-btn"
                :class="{ active: img === activeImage }"
                @click="changeImage(img)"
              >
                <img :src="img" alt="缩略图" />
              </button>
            </div>
          </div>

          <div class="configurator-col">
            <div class="info-header">
              <div class="badge-row">
                <span class="studio-badge kc-mono">KLACKCRAFT 严选</span>
                <span class="spec-tag" v-for="item in keywordList.slice(0, 3)" :key="item">{{ item }}</span>
              </div>
              <h1 class="product-title">{{ productInfo.productName }}</h1>
              <p v-if="productInfo.productInfo" class="product-desc">{{ productInfo.productInfo }}</p>
            </div>

            <div v-if="flashSale" class="flash-telemetry" :class="{ disabled: !canFlashRush }">
              <div class="flash-left">
                <span class="flash-tag kc-mono">FLASH DROP</span>
                <strong class="kc-mono">¥{{ flashDisplayPrice.toFixed(2) }}</strong>
                <span class="flash-origin kc-mono">¥{{ originPrice.toFixed(2) }}</span>
              </div>
              <div class="flash-right kc-mono">
                <span>{{ flashSale.statusText || '闪购进行中' }}</span>
                <strong>{{ formatCountdown(flashRemainingSeconds) }}</strong>
              </div>
            </div>

            <div v-else class="price-telemetry">
              <div class="price-primary">
                <span class="price-label kc-mono">STUDIO PRICE</span>
                <div class="price-numbers kc-mono">
                  <strong>¥{{ originPrice.toFixed(2) }}</strong>
                  <span v-if="displayOtPrice > originPrice" class="ot-price">¥{{ displayOtPrice.toFixed(2) }}</span>
                </div>
              </div>
              <div class="price-stats kc-mono">
                <div>
                  <span>累计销量</span>
                  <strong>{{ productInfo.sales || 0 }}</strong>
                </div>
                <div>
                  <span>当前库存</span>
                  <strong>{{ displayStock }}{{ productInfo.unitName || '件' }}</strong>
                </div>
              </div>
            </div>

            <div class="spec-meta-grid">
              <div class="spec-meta-cell">
                <span>承运快递</span>
                <strong>{{ productInfo.expressName || '官方优选物流' }}</strong>
              </div>
              <div class="spec-meta-cell">
                <span>运费标准</span>
                <strong class="kc-mono">{{ postageText }}</strong>
              </div>
              <div class="spec-meta-cell">
                <span>计量单位</span>
                <strong>{{ productInfo.unitName || '件' }}</strong>
              </div>
              <div class="spec-meta-cell">
                <span>赠送积分</span>
                <strong class="kc-mono">{{ Number(productInfo.giveIntegral || 0) }} PTS</strong>
              </div>
            </div>

            <div class="sku-configurator" v-if="skuGroups.length > 0">
              <div class="sku-head">
                <span class="kc-kicker">CUSTOM CONFIGURATION // 客制化规格配置</span>
                <span v-if="selectedSkuLabel" class="sku-selected">{{ selectedSkuLabel }}</span>
              </div>

              <div class="sku-group" v-for="group in skuGroups" :key="group.index">
                <p class="sku-group-label">{{ group.label }}</p>
                <div class="sku-options">
                  <button
                    v-for="option in group.options"
                    :key="option"
                    type="button"
                    class="sku-chip"
                    :class="{ active: isOptionActive(group.index, option) }"
                    @click="pickSkuOption(group.index, option)"
                  >
                    {{ option }}
                  </button>
                </div>
              </div>
            </div>

            <div class="qty-row">
              <span class="qty-label">选购数量</span>
              <div class="qty-stepper kc-mono">
                <button type="button" @click="decreaseQuantity">-</button>
                <span>{{ quantity }}</span>
                <button type="button" @click="increaseQuantity">+</button>
              </div>
              <span class="qty-stock kc-mono">可售库存 {{ displayStock }} {{ productInfo.unitName || '件' }}</span>
            </div>

            <div class="cta-bar">
              <el-button
                type="danger"
                class="cta-primary"
                :loading="flashSale && canFlashRush ? flashLoading : buyNowLoading"
                @click="onBuyNow"
              >
                {{ flashSale && canFlashRush ? flashActionText : '立即购买' }}
              </el-button>
              <el-button class="cta-secondary" @click="onAddCart">加入购物车</el-button>
              <el-button
                class="cta-collect"
                :class="{ active: isCollected }"
                :loading="favoriteLoading"
                @click="toggleFavorite"
              >
                {{ isCollected ? '已收藏' : '收藏' }}
              </el-button>
            </div>
          </div>
        </template>

        <div v-else class="empty-box">暂无外设详情信息</div>
      </section>

      <section class="content-sheet kc-surface" v-if="productInfo">
        <div class="sheet-tabs">
          <button
            type="button"
            class="sheet-tab"
            :class="{ active: activeContentTab === 'detail' }"
            @click="activeContentTab = 'detail'"
          >
            规格图文详情
          </button>
          <button
            type="button"
            class="sheet-tab"
            :class="{ active: activeContentTab === 'comment' }"
            @click="switchToCommentTab"
          >
            极客评价 <span v-if="commentTotal > 0" class="kc-mono">({{ commentTotal }})</span>
          </button>
        </div>

        <div v-if="activeContentTab === 'detail'" class="desc-pane">
          <div v-if="productInfo.desc" class="desc-content" v-html="productInfo.desc"></div>
          <div v-else class="empty-box">暂无更多图文参数</div>
        </div>

        <div v-else class="comment-pane" v-loading="commentLoading">
          <div v-if="commentList.length" class="comment-list">
            <article v-for="item in commentList" :key="item.id" class="comment-item">
              <div class="comment-head">
                <div class="comment-author">
                  <img v-if="item.userAvatar" class="author-avatar" :src="item.userAvatar" :alt="getUserDisplayName(item)" />
                  <span v-else class="author-avatar fallback kc-mono">{{ getUserInitial(item) }}</span>
                  <div class="author-meta">
                    <strong>{{ getUserDisplayName(item) }}</strong>
                    <span class="kc-mono">{{ formatDateTime(item.createTime) }}</span>
                  </div>
                </div>
                <div class="comment-score">
                  <span>评分</span>
                  <el-rate :model-value="Number(item.productScore || 0)" disabled />
                </div>
              </div>
              <p class="comment-body">{{ item.comment || '暂无评价内容' }}</p>
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
            </article>
          </div>
          <div v-else class="empty-box">暂无评价记录</div>

          <div v-if="commentTotal > commentPageSize" class="comment-pagination">
            <el-pagination
              background
              layout="prev, pager, next"
              :current-page="commentPage"
              :page-size="commentPageSize"
              :total="commentTotal"
              @current-change="handleCommentPageChange"
            />
          </div>
        </div>
      </section>
    </main>

    <SiteFooter />
  </div>
</template>

<style scoped lang="scss">
.detail-main-wrap {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.detail-crumbs {
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
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.spec-sheet {
  padding: 26px;
  border-radius: 26px;
  border: 1px solid transparent;
  background: var(--kc-surface);
  box-shadow: var(--kc-shadow-md);
  display: grid;
  grid-template-columns: 492px minmax(0, 1fr);
  gap: 34px;
  align-items: start;
}

.gallery-col {
  position: sticky;
  top: 94px;
}

.gallery-viewport {
  aspect-ratio: 1 / 1;
  border-radius: 18px;
  overflow: hidden;
  background: var(--kc-surface-muted);
  outline: 1px solid oklch(0 0 0 / 0.08);
  outline-offset: -1px;
}

.gallery-viewport img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.img-fallback {
  width: 100%;
  height: 100%;
  display: grid;
  place-items: center;
  padding: 24px;
  text-align: center;
  color: var(--kc-text-secondary);
  font-weight: 600;
}

.gallery-thumbs {
  margin-top: 12px;
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 10px;
}

.thumb-btn {
  padding: 0;
  aspect-ratio: 1 / 1;
  border-radius: 12px;
  border: 0;
  overflow: hidden;
  background: var(--kc-surface-muted);
  cursor: pointer;
  outline: 1px solid oklch(0 0 0 / 0.08);
  outline-offset: -1px;
  transition: transform 120ms ease-out, box-shadow 150ms ease;
}

.thumb-btn:active {
  transform: scale(0.95);
}

.thumb-btn img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.thumb-btn.active {
  box-shadow: 0 0 0 2px var(--kc-accent);
}

/* Right Configurator Panel */
.configurator-col {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.badge-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.studio-badge {
  padding: 5px 11px;
  border-radius: 999px;
  background: var(--kc-carbon);
  color: #ffffff;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.06em;
}

.spec-tag {
  padding: 5px 11px;
  border-radius: 999px;
  background: var(--kc-surface-muted);
  color: var(--kc-text-secondary);
  font-size: 12px;
  font-weight: 500;
}

.product-title {
  margin: 12px 0 0;
  font-size: 28px;
  font-weight: 700;
  line-height: 1.25;
  letter-spacing: -0.025em;
  color: var(--kc-text);
}

.product-desc {
  margin: 10px 0 0;
  font-size: 14px;
  color: var(--kc-text-secondary);
  line-height: 1.75;
}

/* Price & Flash Telemetry (Tonal, Borderless) */
.price-telemetry {
  padding: 18px 22px;
  border-radius: 18px;
  background: var(--kc-surface-muted);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.price-label {
  font-size: 10px;
  letter-spacing: 0.12em;
  color: var(--kc-text-muted);
}

.price-numbers {
  margin-top: 6px;
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.price-numbers strong {
  font-size: 36px;
  font-weight: 700;
  color: var(--kc-accent);
  line-height: 1;
}

.ot-price {
  font-size: 15px;
  color: var(--kc-text-muted);
  text-decoration: line-through;
}

.price-stats {
  display: flex;
  gap: 12px;
}

.price-stats div {
  padding: 10px 14px;
  border-radius: 12px;
  background: var(--kc-surface);
  box-shadow: var(--kc-shadow-sm);
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.price-stats span {
  font-size: 11px;
  color: var(--kc-text-muted);
}

.price-stats strong {
  font-size: 15px;
  color: var(--kc-text);
}

.flash-telemetry {
  padding: 18px 22px;
  border-radius: 18px;
  background: var(--kc-carbon);
  color: #ffffff;
  box-shadow: var(--kc-shadow-md);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.flash-telemetry.disabled {
  opacity: 0.65;
}

.flash-left {
  display: flex;
  align-items: baseline;
  gap: 12px;
  flex-wrap: wrap;
}

.flash-tag {
  padding: 4px 10px;
  border-radius: 999px;
  background: var(--kc-accent);
  color: #ffffff;
  font-size: 11px;
  font-weight: 700;
}

.flash-left strong {
  font-size: 32px;
  line-height: 1;
}

.flash-origin {
  font-size: 14px;
  color: #9ba0ae;
  text-decoration: line-through;
}

.flash-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
  font-size: 12px;
  color: #b4b8c5;
}

.flash-right strong {
  font-size: 20px;
  color: var(--kc-accent);
}

/* 4 Independent Soft Spec Tiles (No Box Grid Lines) */
.spec-meta-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.spec-meta-cell {
  padding: 12px 14px;
  border-radius: 14px;
  background: var(--kc-surface-muted);
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.spec-meta-cell span {
  font-size: 11px;
  color: var(--kc-text-muted);
}

.spec-meta-cell strong {
  font-size: 13px;
  color: var(--kc-text);
}

/* Soft Keycap-Style SKU Configurator */
.sku-configurator {
  padding: 18px 20px;
  border-radius: 18px;
  background: var(--kc-surface-muted);
}

.sku-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.sku-selected {
  padding: 4px 10px;
  border-radius: 999px;
  background: var(--kc-surface);
  font-size: 12px;
  font-weight: 700;
  color: var(--kc-accent);
}

.sku-group + .sku-group {
  margin-top: 14px;
}

.sku-group-label {
  margin: 0 0 8px;
  font-size: 13px;
  font-weight: 600;
  color: var(--kc-text);
}

.sku-options {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.sku-chip {
  padding: 8px 16px;
  border-radius: 999px;
  border: 1px solid transparent;
  background: var(--kc-surface);
  color: var(--kc-text);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: var(--kc-shadow-sm);
  transition: transform 100ms ease-out, background-color 140ms ease, color 140ms ease, box-shadow 140ms ease;
}

.sku-chip:active {
  transform: scale(0.96);
}

.sku-chip.active {
  background: var(--kc-carbon);
  color: #ffffff;
  box-shadow: 0 0 0 2px rgba(255, 77, 0, 0.28), var(--kc-shadow-sm);
}

/* Quantity & CTA */
.qty-row {
  display: flex;
  align-items: center;
  gap: 14px;
}

.qty-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--kc-text);
}

.qty-stepper {
  display: inline-flex;
  align-items: center;
  padding: 3px;
  border-radius: 999px;
  background: var(--kc-surface-muted);
}

.qty-stepper button {
  width: 32px;
  height: 32px;
  border: 0;
  border-radius: 999px;
  background: var(--kc-surface);
  color: var(--kc-text);
  font-size: 16px;
  cursor: pointer;
  box-shadow: var(--kc-shadow-sm);
}

.qty-stepper span {
  width: 44px;
  text-align: center;
  font-size: 14px;
  font-weight: 600;
}

.qty-stock {
  font-size: 12px;
  color: var(--kc-text-muted);
}

.cta-bar {
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) minmax(0, 1fr) auto;
  gap: 10px;
  padding-top: 6px;
}

.cta-primary,
.cta-secondary,
.cta-collect {
  height: 48px;
  margin-left: 0 !important;
  border-radius: 999px;
  font-size: 15px;
  font-weight: 700;
}

.cta-secondary {
  border-color: transparent;
  background: var(--kc-carbon);
  color: #ffffff;
}

.cta-collect {
  min-width: 96px;
  border-color: transparent;
  background: var(--kc-surface-muted);
  color: var(--kc-text);
}

.cta-collect.active {
  color: var(--kc-accent);
  background: var(--kc-accent-soft);
}

/* Bottom Content Sheet */
.content-sheet {
  padding: 24px 28px 28px;
  border-radius: 26px;
  border: 1px solid transparent;
  background: var(--kc-surface);
  box-shadow: var(--kc-shadow-md);
}

.sheet-tabs {
  display: inline-flex;
  padding: 4px;
  border-radius: 999px;
  background: var(--kc-surface-muted);
  gap: 4px;
}

.sheet-tab {
  border: 0;
  border-radius: 999px;
  background: transparent;
  color: var(--kc-text-secondary);
  font-size: 14px;
  font-weight: 700;
  padding: 9px 20px;
  cursor: pointer;
  transition: background-color 150ms ease, color 150ms ease, box-shadow 150ms ease;
}

.sheet-tab.active {
  background: var(--kc-surface);
  color: var(--kc-text);
  box-shadow: var(--kc-shadow-sm);
}

.desc-pane,
.comment-pane {
  padding: 24px 4px 4px;
}

.desc-content {
  color: var(--kc-text-secondary);
  font-size: 14px;
  line-height: 1.9;
}

.desc-content :deep(img) {
  max-width: 100%;
  border-radius: 14px;
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.comment-item {
  padding: 18px 20px;
  border-radius: 18px;
  background: var(--kc-surface-muted);
}

.comment-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.comment-author {
  display: flex;
  align-items: center;
  gap: 12px;
}

.author-avatar {
  width: 40px;
  height: 40px;
  border-radius: 999px;
  object-fit: cover;
  background: var(--kc-carbon);
  color: #ffffff;
}

.author-avatar.fallback {
  display: grid;
  place-items: center;
  font-weight: 700;
}

.author-meta {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.author-meta strong {
  font-size: 14px;
  color: var(--kc-text);
}

.author-meta span {
  font-size: 11px;
  color: var(--kc-text-muted);
}

.comment-score {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--kc-text-muted);
}

.comment-body {
  margin: 14px 0 0;
  color: var(--kc-text-secondary);
  font-size: 14px;
  line-height: 1.75;
  white-space: pre-wrap;
}

.comment-pics {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.comment-pic {
  width: 88px;
  height: 88px;
  border-radius: 12px;
  outline: 1px solid oklch(0 0 0 / 0.08);
  outline-offset: -1px;
}

.comment-pagination {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.empty-box {
  min-height: 220px;
  display: grid;
  place-items: center;
  color: var(--kc-text-muted);
  font-size: 14px;
}

@media (hover: hover) {
  .crumb-btn:hover {
    color: var(--kc-accent);
  }

  .sku-chip:hover:not(.active) {
    box-shadow: var(--kc-shadow-md);
  }

  .cta-secondary:hover {
    background: #2b2e37;
    color: #ffffff;
  }
}

@media (max-width: 1080px) {
  .spec-sheet {
    grid-template-columns: 1fr;
  }

  .gallery-col {
    position: static;
  }
}

@media (max-width: 640px) {
  .spec-sheet,
  .content-sheet {
    padding: 18px;
    border-radius: 20px;
  }

  .spec-meta-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .price-telemetry {
    flex-direction: column;
    align-items: flex-start;
  }

  .cta-bar {
    grid-template-columns: 1fr;
  }

  .comment-head {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
