<template>
  <div class="home-workbench" v-loading="loading">
    <!-- 顶部工作台栏：紧凑、高信噪比，不占用首屏大面积视觉空间 -->
    <header class="workbench-header">
      <div class="header-title-group">
        <div class="header-eyebrow">
          <span class="status-dot" :class="{ 'is-warning': !!loadWarning }"></span>
          <span>商城运营工作台</span>
        </div>
        <h1>{{ greetingText }}</h1>
      </div>

      <div class="header-actions">
        <div class="sync-meta">
          <span class="sync-label">最近同步</span>
          <time class="sync-time">{{ lastUpdatedText }}</time>
        </div>
        <el-button type="primary" :icon="RefreshRight" :loading="loading" @click="loadDashboard">
          刷新数据
        </el-button>
      </div>
    </header>

    <el-alert
      v-if="loadWarning"
      class="warning-alert"
      title="部分模块数据加载失败"
      type="warning"
      :description="loadWarning"
      show-icon
      :closable="false"
    />

    <!-- 第一层：核心经营账本 (8 cols) + 待办与预警队列 (4 cols) -->
    <section class="bento-row bento-top">
      <article class="panel kpi-ledger">
        <div class="panel-head">
          <div>
            <h2>核心经营指标</h2>
            <p>基于实时业务接口聚合的成交、履约与商品规模</p>
          </div>
          <span class="period-chip">近 80 单 / 60 笔样本</span>
        </div>

        <div class="kpi-primary-grid">
          <div class="kpi-cell kpi-focal">
            <span class="kpi-label">近 80 单成交额</span>
            <strong class="kpi-value is-hero">{{ formatMoney(summary.recentOrderAmount) }}</strong>
            <span class="kpi-meta">累计订单 {{ formatCount(summary.totalOrders) }} 单</span>
          </div>

          <div class="kpi-cell">
            <span class="kpi-label">待发货订单</span>
            <strong class="kpi-value" :class="{ 'is-amber': summary.pendingShipment > 0 }">
              {{ formatCount(summary.pendingShipment) }}
            </strong>
            <span class="kpi-meta">已支付待履约</span>
          </div>

          <div class="kpi-cell">
            <span class="kpi-label">近 60 笔充值额</span>
            <strong class="kpi-value">{{ formatMoney(summary.recentRechargeAmount) }}</strong>
            <span class="kpi-meta">累计充值 {{ formatCount(summary.totalRecharges) }} 笔</span>
          </div>

          <div class="kpi-cell">
            <span class="kpi-label">在售商品</span>
            <strong class="kpi-value">{{ formatCount(summary.activeProducts) }}</strong>
            <span class="kpi-meta">商品总库 {{ formatCount(summary.totalProducts) }} 件</span>
          </div>
        </div>

        <div class="kpi-secondary-strip">
          <div class="strip-item">
            <span>会员总数</span>
            <strong>{{ formatCount(summary.totalUsers) }}</strong>
          </div>
          <div class="strip-divider"></div>
          <div class="strip-item">
            <span>近期待退款金额</span>
            <strong>{{ formatMoney(summary.recentAfterSaleAmount) }}</strong>
          </div>
          <div class="strip-divider"></div>
          <div class="strip-item">
            <span>充值赠送总额</span>
            <strong>{{ formatMoney(summary.recentRechargeGiftAmount) }}</strong>
          </div>
          <div class="strip-divider"></div>
          <div class="strip-item">
            <span>内容投放位</span>
            <strong>{{ formatCount(summary.totalBanners + summary.totalNews) }}</strong>
          </div>
        </div>
      </article>

      <article class="panel action-queue">
        <div class="panel-head">
          <div>
            <h2>运营待办</h2>
            <p>优先处理影响履约与客诉的事项</p>
          </div>
        </div>

        <div class="queue-list">
          <button
            v-for="item in actionItems"
            :key="item.label"
            type="button"
            class="queue-item"
            :class="{ 'is-clickable': canNavigate(item.moduleKey) }"
            :disabled="!canNavigate(item.moduleKey)"
            @click="navigateToModule(item.moduleKey)"
          >
            <div class="queue-item-left">
              <span class="queue-dot" :class="`is-${item.tone}`"></span>
              <div class="queue-text">
                <strong>{{ item.label }}</strong>
                <span>{{ item.desc }}</span>
              </div>
            </div>
            <div class="queue-item-right">
              <strong class="queue-count" :class="`is-${item.tone}`">{{ item.value }}</strong>
              <el-icon v-if="canNavigate(item.moduleKey)" class="queue-arrow"><ArrowRight /></el-icon>
            </div>
          </button>
        </div>
      </article>
    </section>

    <!-- 第二层：主趋势图表 (8 cols) + 售后状态分布 (4 cols) -->
    <section class="bento-row bento-charts-primary">
      <article class="panel chart-panel">
        <div class="panel-head">
          <div>
            <h2>订单与成交趋势</h2>
            <p>近 7 个活跃交易日支付金额与订单量走势</p>
          </div>
        </div>
        <div v-if="orderTrendData.length" ref="orderTrendRef" class="chart-box"></div>
        <el-empty v-else description="暂无订单趋势数据" :image-size="64">
          <el-button size="small" @click="loadDashboard">重新加载</el-button>
        </el-empty>
      </article>

      <article class="panel chart-panel">
        <div class="panel-head">
          <div>
            <h2>售后状态分布</h2>
            <p>各售后流程节点工单占比</p>
          </div>
          <span v-if="afterSaleTotal > 0" class="period-chip">共 {{ formatCount(afterSaleTotal) }} 单</span>
        </div>
        <div v-if="afterSaleStats.length" ref="afterSaleChartRef" class="chart-box"></div>
        <el-empty v-else description="暂无售后工单数据" :image-size="64">
          <el-button size="small" @click="loadDashboard">刷新状态</el-button>
        </el-empty>
      </article>
    </section>

    <!-- 第三层：对称分布图表 (6 cols + 6 cols)，消除孤立卡片空洞 -->
    <section class="bento-row bento-charts-secondary">
      <article class="panel chart-panel">
        <div class="panel-head">
          <div>
            <h2>分类商品分布</h2>
            <p>前 6 个业务分类下的商品规模对比</p>
          </div>
        </div>
        <div v-if="categoryStats.length" ref="categoryChartRef" class="chart-box chart-box-sm"></div>
        <el-empty v-else description="暂无分类商品数据" :image-size="64">
          <el-button v-if="canNavigate('shopCategory')" size="small" @click="navigateToModule('shopCategory')">
            前往分类管理
          </el-button>
        </el-empty>
      </article>

      <article class="panel chart-panel">
        <div class="panel-head">
          <div>
            <h2>充值渠道结构</h2>
            <p>基于最近 60 笔充值记录的金额构成</p>
          </div>
        </div>
        <div v-if="rechargeTypeStats.length" ref="rechargeChartRef" class="chart-box chart-box-sm"></div>
        <el-empty v-else description="暂无充值结构数据" :image-size="64">
          <el-button v-if="canNavigate('shopUserRecharge')" size="small" @click="navigateToModule('shopUserRecharge')">
            查看充值记录
          </el-button>
        </el-empty>
      </article>
    </section>

    <!-- 第四层：三列运营明细账本 (4 + 4 + 4) -->
    <section class="bento-row bento-feeds">
      <article class="panel feed-panel">
        <div class="panel-head">
          <div>
            <h2>最近订单</h2>
            <p>最新 6 笔成交与履约状态</p>
          </div>
          <el-button
            v-if="canNavigate('shopOrder')"
            link
            type="primary"
            size="small"
            @click="navigateToModule('shopOrder')"
          >
            全部订单
          </el-button>
        </div>

        <div v-if="recentOrders.length" class="feed-list">
          <div v-for="item in recentOrders" :key="item.id || item.orderId" class="order-row">
            <div class="order-row-main">
              <strong>{{ item.orderId || '-' }}</strong>
              <p>{{ item.realName || '匿名用户' }} · {{ item.userAddress || '地址待补充' }}</p>
            </div>
            <div class="order-row-side">
              <strong class="tabular-num">{{ formatMoney(item.payPrice || item.totalPrice) }}</strong>
              <el-tag size="small" effect="plain" :type="getOrderTagType(item.status)">
                {{ getOrderStatusText(item.status) }}
              </el-tag>
            </div>
          </div>
        </div>
        <el-empty v-else description="暂无最近订单" :image-size="64">
          <el-button v-if="canNavigate('shopOrder')" size="small" @click="navigateToModule('shopOrder')">
            前往订单管理
          </el-button>
        </el-empty>
      </article>

      <article class="panel feed-panel">
        <div class="panel-head">
          <div>
            <h2>热销商品</h2>
            <p>按销量排序前 5 位商品</p>
          </div>
          <el-button
            v-if="canNavigate('shopProduct')"
            link
            type="primary"
            size="small"
            @click="navigateToModule('shopProduct')"
          >
            商品库
          </el-button>
        </div>

        <div v-if="topProducts.length" class="feed-list">
          <div v-for="(item, index) in topProducts" :key="item.id || index" class="rank-row">
            <span class="rank-num" :class="{ 'is-top': index < 3 }">{{ index + 1 }}</span>
            <div class="rank-body">
              <div class="rank-title-line">
                <strong>{{ item.productName || '未命名商品' }}</strong>
                <span class="rank-sales tabular-num">{{ formatCount(item.sales) }}</span>
              </div>
              <div class="rank-meta-line">
                <span>库存 {{ formatCount(item.stock) }}</span>
                <div class="rank-bar-track">
                  <div class="rank-bar-fill" :style="{ width: `${getSalesPercent(item.sales)}%` }"></div>
                </div>
              </div>
            </div>
          </div>
        </div>
        <el-empty v-else description="暂无商品销量数据" :image-size="64">
          <el-button v-if="canNavigate('shopProduct')" size="small" @click="navigateToModule('shopProduct')">
            去上架商品
          </el-button>
        </el-empty>
      </article>

      <article class="panel feed-panel">
        <div class="panel-head">
          <div>
            <h2>内容运营</h2>
            <p>首页 Banner 与资讯发布动态</p>
          </div>
          <el-button
            v-if="canNavigate('shopHomeNews')"
            link
            type="primary"
            size="small"
            @click="navigateToModule('shopHomeNews')"
          >
            管理资讯
          </el-button>
        </div>

        <div class="content-summary-bar">
          <div class="content-stat">
            <span>Banner</span>
            <strong>{{ formatCount(summary.totalBanners) }}</strong>
          </div>
          <div class="content-stat">
            <span>资讯文章</span>
            <strong>{{ formatCount(summary.totalNews) }}</strong>
          </div>
        </div>

        <div v-if="latestNews.length" class="feed-list">
          <div v-for="item in latestNews" :key="item.id" class="news-row">
            <div class="news-row-main">
              <strong>{{ item.title || '未命名资讯' }}</strong>
              <span>{{ item.tagName || '未设置标签' }}</span>
            </div>
            <time class="news-date tabular-num">{{ formatShortDate(item.publishTime || item.createTime) }}</time>
          </div>
        </div>
        <el-empty v-else description="暂无资讯内容" :image-size="64">
          <el-button v-if="canNavigate('shopHomeNews')" size="small" @click="navigateToModule('shopHomeNews')">
            发布资讯
          </el-button>
        </el-empty>
      </article>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import * as echarts from 'echarts';
import type { ECharts, EChartsOption } from 'echarts';
import { ArrowRight, RefreshRight } from '@element-plus/icons-vue';
import type { IPage, IResultData } from '@/api/types';
import { getShopCategoryAllListApi } from '@/api/modules/shop_system/shopCategory';
import { getShopHomeBannerListApi } from '@/api/modules/shop_system/shopHomeBanner';
import { getShopHomeNewsListApi } from '@/api/modules/shop_system/shopHomeNews';
import { getShopOrderAfterSaleListApi } from '@/api/modules/shop_system/shopOrderAfterSale';
import { getShopOrderListApi } from '@/api/modules/shop_system/shopOrder';
import { getShopProductListApi } from '@/api/modules/shop_system/shopProduct';
import { getShopUserRechargeListApi } from '@/api/modules/shop_system/shopUserRecharge';
import { getShopUserListApi } from '@/api/modules/shop_system/shopUser';
import { useDict } from '@/hooks/useDict';
import { useDictOptions } from '@/hooks/useDictOptions';
import { useAppStore } from '@/stores/modules/app';
import { useAuthStore } from '@/stores/modules/auth';
import { useUserStore } from '@/stores/modules/user';
import type { ShopCategoryRow } from '@/api/types/shop_system/shopCategory';
import type { ShopHomeNewsRow } from '@/api/types/shop_system/shopHomeNews';
import type { ShopOrderRow } from '@/api/types/shop_system/shopOrder';
import type { ShopProductRow } from '@/api/types/shop_system/shopProduct';
import type { ShopUserRechargeRow } from '@/api/types/shop_system/shopUserRecharge';

defineOptions({
  name: 'HomeDashboard'
});

useDict(['order_status', 'recharge_type']);

type DashboardSummary = {
  totalUsers: number;
  totalProducts: number;
  activeProducts: number;
  totalOrders: number;
  pendingShipment: number;
  totalRecharges: number;
  pendingAfterSale: number;
  totalBanners: number;
  totalNews: number;
  recentOrderAmount: number;
  recentRechargeAmount: number;
  recentRechargeGiftAmount: number;
  recentAfterSaleAmount: number;
};

type TrendPoint = {
  date: string;
  orderCount: number;
  payAmount: number;
};

type NamedStat = {
  name: string;
  value: number;
};

type FlatCategory = {
  id: number;
  name: string;
  path: string;
};

const AFTER_SALE_STATUS_OPTIONS = [
  { label: '待审核', value: 0 },
  { label: '待寄回', value: 1 },
  { label: '待退款', value: 2 },
  { label: '已退款', value: 3 },
  { label: '已拒绝', value: 4 }
];

const router = useRouter();
const appStore = useAppStore();
const authStore = useAuthStore();
const userStore = useUserStore();
const orderStatusOptions = useDictOptions('order_status');
const rechargeTypeOptions = useDictOptions('recharge_type');

const loading = ref(false);
const loadWarning = ref('');
const lastUpdatedAt = ref('');
const summary = ref<DashboardSummary>(createDefaultSummary());
const orderTrendData = ref<TrendPoint[]>([]);
const categoryStats = ref<NamedStat[]>([]);
const afterSaleStats = ref<NamedStat[]>([]);
const rechargeTypeStats = ref<NamedStat[]>([]);
const recentOrders = ref<ShopOrderRow[]>([]);
const topProducts = ref<ShopProductRow[]>([]);
const latestNews = ref<ShopHomeNewsRow[]>([]);

const orderTrendRef = ref<HTMLDivElement>();
const categoryChartRef = ref<HTMLDivElement>();
const afterSaleChartRef = ref<HTMLDivElement>();
const rechargeChartRef = ref<HTMLDivElement>();

let orderTrendChart: ECharts | null = null;
let categoryChart: ECharts | null = null;
let afterSaleChart: ECharts | null = null;
let rechargeChart: ECharts | null = null;

const greetingText = computed(() => {
  const username = userStore.userInfo?.username?.trim();
  return username ? `${username}，今日运营概览` : '今日运营概览';
});

const lastUpdatedText = computed(() => lastUpdatedAt.value || '尚未同步');

const afterSaleTotal = computed(() => afterSaleStats.value.reduce((acc, item) => acc + item.value, 0));

const actionItems = computed(() => [
  {
    label: '待发货订单',
    value: formatCount(summary.value.pendingShipment),
    desc: '已支付未发货，需及时安排履约',
    tone: summary.value.pendingShipment > 0 ? 'warning' : 'neutral',
    moduleKey: 'shopOrder'
  },
  {
    label: '待审核售后',
    value: formatCount(summary.value.pendingAfterSale),
    desc: '待介入处理，避免客诉升级',
    tone: summary.value.pendingAfterSale > 0 ? 'danger' : 'neutral',
    moduleKey: 'shopOrderAfterSale'
  },
  {
    label: '在售商品维护',
    value: formatCount(summary.value.activeProducts),
    desc: `总商品库 ${formatCount(summary.value.totalProducts)} 件`,
    tone: 'primary',
    moduleKey: 'shopProduct'
  },
  {
    label: '首页内容投放',
    value: formatCount(summary.value.totalBanners + summary.value.totalNews),
    desc: `Banner ${formatCount(summary.value.totalBanners)} · 资讯 ${formatCount(summary.value.totalNews)}`,
    tone: 'neutral',
    moduleKey: 'shopHomeBanner'
  }
]);

const findMenuPath = (keyword: string) => {
  const lower = keyword.toLowerCase();
  const target = authStore.flatMenuListGet.find(
    item =>
      item.path &&
      ((typeof item.component === 'string' && item.component.toLowerCase().includes(lower)) ||
        item.path.toLowerCase().includes(lower))
  );
  return target?.path || '';
};

const canNavigate = (keyword: string) => Boolean(findMenuPath(keyword));

const navigateToModule = (keyword: string) => {
  const path = findMenuPath(keyword);
  if (path) router.push(path);
};

const createEmptyPage = <T,>() =>
  ({
    current: 1,
    limit: 0,
    totalPage: 0,
    total: 0,
    rows: []
  }) satisfies IPage<T>;

function createDefaultSummary(): DashboardSummary {
  return {
    totalUsers: 0,
    totalProducts: 0,
    activeProducts: 0,
    totalOrders: 0,
    pendingShipment: 0,
    totalRecharges: 0,
    pendingAfterSale: 0,
    totalBanners: 0,
    totalNews: 0,
    recentOrderAmount: 0,
    recentRechargeAmount: 0,
    recentRechargeGiftAmount: 0,
    recentAfterSaleAmount: 0
  };
}

const getPageData = <T,>(response?: IResultData<IPage<T>> | null): IPage<T> => response?.data ?? createEmptyPage<T>();

const getCategoryRows = (response?: IResultData<any> | null): ShopCategoryRow[] => {
  const payload = response?.data;
  if (Array.isArray(payload)) return payload as ShopCategoryRow[];
  if (Array.isArray(payload?.data)) return payload.data as ShopCategoryRow[];
  return [];
};

const toNumber = (value: unknown) => {
  const num = Number(value ?? 0);
  return Number.isFinite(num) ? num : 0;
};

const sumBy = <T,>(rows: T[], getter: (row: T) => unknown) => rows.reduce((total, item) => total + toNumber(getter(item)), 0);

const formatCount = (value: unknown) => new Intl.NumberFormat('zh-CN').format(toNumber(value));

const formatMoney = (value: unknown) => `￥${toNumber(value).toFixed(2)}`;

const formatShortDate = (value?: string) => {
  if (!value) return '-';
  return value.replace('T', ' ').slice(5, 10);
};

const getDateKey = (value?: string) => {
  if (!value) return '';
  return value.replace('T', ' ').slice(0, 10);
};

const getOrderStatusText = (status?: number) => {
  const target = String(status ?? '');
  const matched = orderStatusOptions.value.find(item => String(item?.alias ?? item?.id) === target);
  return matched?.codeName || (target ? `状态 ${target}` : '未知状态');
};

const getOrderTagType = (status?: number): 'warning' | 'primary' | 'success' | 'info' | 'danger' => {
  if (status === 0) return 'warning';
  if (status === 1) return 'primary';
  if (status === 2 || status === 3) return 'success';
  if (status === -1 || status === 4) return 'danger';
  return 'info';
};

const getRechargeTypeText = (type?: string) => {
  const target = String(type ?? '');
  const matched = rechargeTypeOptions.value.find(item => String(item?.id ?? item?.alias) === target);
  return matched?.codeName || (target || '未分类');
};

const getSalesPercent = (sales?: number) => {
  const maxSales = Math.max(...topProducts.value.map(item => toNumber(item.sales)), 0);
  if (!maxSales) return 0;
  return Number(((toNumber(sales) / maxSales) * 100).toFixed(2));
};

const flattenCategoryTree = (rows: ShopCategoryRow[] = [], prefix = ''): FlatCategory[] => {
  return rows.flatMap(item => {
    const name = String(item.cateName || '未命名分类');
    const path = prefix ? `${prefix} / ${name}` : name;
    const current = Number.isFinite(Number(item.id))
      ? [
          {
            id: Number(item.id),
            name,
            path
          }
        ]
      : [];

    const children = Array.isArray(item.children) ? flattenCategoryTree(item.children, path) : [];
    return [...current, ...children];
  });
};

const buildOrderTrend = (rows: ShopOrderRow[]) => {
  const trendMap = new Map<string, TrendPoint>();

  rows.forEach(item => {
    const dateKey = getDateKey(item.payTime || item.createTime);
    if (!dateKey) return;

    if (!trendMap.has(dateKey)) {
      trendMap.set(dateKey, {
        date: dateKey.slice(5),
        orderCount: 0,
        payAmount: 0
      });
    }

    const current = trendMap.get(dateKey);
    if (!current) return;

    current.orderCount += 1;
    current.payAmount += toNumber(item.payPrice || item.totalPrice);
  });

  return [...trendMap.entries()]
    .sort(([prev], [next]) => prev.localeCompare(next))
    .slice(-7)
    .map(([, item]) => item);
};

const buildRechargeStats = (rows: ShopUserRechargeRow[]) => {
  const statMap = new Map<string, number>();

  rows.forEach(item => {
    const key = getRechargeTypeText(item.rechargeType);
    statMap.set(key, (statMap.get(key) || 0) + toNumber(item.price));
  });

  return [...statMap.entries()]
    .map(([name, value]) => ({ name, value }))
    .sort((prev, next) => next.value - prev.value);
};

const safeRequest = async <T,>(label: string, request: () => Promise<T>, failedModules: string[]) => {
  try {
    return await request();
  } catch (error) {
    failedModules.push(label);
    console.error(`[dashboard] ${label} load failed`, error);
    return null;
  }
};

const getChartTheme = () => {
  const styles = getComputedStyle(document.documentElement);
  const primary = styles.getPropertyValue('--el-color-primary').trim() || '#009688';
  const surface = styles.getPropertyValue('--el-bg-color').trim() || (appStore.isDark ? '#141414' : '#ffffff');
  const textPrimary = styles.getPropertyValue('--el-text-color-primary').trim() || (appStore.isDark ? '#e5eaf3' : '#1e293b');
  const textSecondary = styles.getPropertyValue('--el-text-color-secondary').trim() || (appStore.isDark ? '#a3a6ad' : '#64748b');
  const borderLight = styles.getPropertyValue('--el-border-color-lighter').trim() || (appStore.isDark ? '#2b2b2c' : '#ebeef5');
  return { primary, surface, textPrimary, textSecondary, borderLight };
};

const buildOrderTrendOption = (): EChartsOption => {
  const theme = getChartTheme();
  return {
    color: [theme.primary, '#64748b'],
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      top: 0,
      right: 0,
      icon: 'roundRect',
      itemWidth: 12,
      itemHeight: 4,
      textStyle: {
        color: theme.textSecondary,
        fontSize: 12
      }
    },
    grid: {
      top: 40,
      right: 14,
      bottom: 12,
      left: 10,
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: orderTrendData.value.map(item => item.date),
      axisLine: {
        lineStyle: {
          color: theme.borderLight
        }
      },
      axisTick: { show: false },
      axisLabel: {
        color: theme.textSecondary,
        fontSize: 12
      }
    },
    yAxis: [
      {
        type: 'value',
        name: '金额',
        nameTextStyle: { color: theme.textSecondary, fontSize: 11 },
        axisLabel: {
          color: theme.textSecondary,
          fontSize: 12,
          formatter: (value: number) => `￥${value}`
        },
        splitLine: {
          lineStyle: {
            color: theme.borderLight,
            type: 'dashed'
          }
        }
      },
      {
        type: 'value',
        name: '订单量',
        nameTextStyle: { color: theme.textSecondary, fontSize: 11 },
        axisLabel: {
          color: theme.textSecondary,
          fontSize: 12
        },
        splitLine: {
          show: false
        }
      }
    ],
    series: [
      {
        name: '支付金额',
        type: 'line',
        smooth: 0.35,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: { width: 2.5 },
        data: orderTrendData.value.map(item => Number(item.payAmount.toFixed(2))),
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: `${theme.primary}29` },
            { offset: 1, color: `${theme.primary}00` }
          ])
        }
      },
      {
        name: '订单量',
        type: 'line',
        smooth: 0.35,
        yAxisIndex: 1,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: { width: 2, type: 'dashed' },
        data: orderTrendData.value.map(item => item.orderCount)
      }
    ]
  };
};

const buildCategoryOption = (): EChartsOption => {
  const theme = getChartTheme();
  return {
    color: [theme.primary],
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      }
    },
    grid: {
      top: 8,
      right: 28,
      bottom: 8,
      left: 8,
      containLabel: true
    },
    xAxis: {
      type: 'value',
      splitLine: {
        lineStyle: {
          color: theme.borderLight,
          type: 'dashed'
        }
      },
      axisLabel: {
        color: theme.textSecondary,
        fontSize: 12
      }
    },
    yAxis: {
      type: 'category',
      inverse: true,
      data: categoryStats.value.map(item => item.name),
      axisLabel: {
        color: theme.textPrimary,
        fontSize: 12
      },
      axisTick: { show: false },
      axisLine: { show: false }
    },
    series: [
      {
        type: 'bar',
        data: categoryStats.value.map(item => item.value),
        barWidth: 14,
        itemStyle: {
          borderRadius: [0, 6, 6, 0]
        },
        label: {
          show: true,
          position: 'right',
          color: theme.textSecondary,
          fontSize: 12
        }
      }
    ]
  };
};

const buildAfterSaleOption = (): EChartsOption => {
  const theme = getChartTheme();
  return {
    color: ['#d97706', theme.primary, '#dc2626', '#059669', '#94a3b8'],
    tooltip: {
      trigger: 'item'
    },
    legend: {
      bottom: 0,
      icon: 'circle',
      itemWidth: 8,
      itemHeight: 8,
      textStyle: {
        color: theme.textSecondary,
        fontSize: 12
      }
    },
    series: [
      {
        name: '售后状态',
        type: 'pie',
        radius: ['56%', '76%'],
        center: ['50%', '43%'],
        itemStyle: {
          borderColor: theme.surface,
          borderWidth: 2
        },
        label: {
          show: false
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 13,
            fontWeight: 600,
            color: theme.textPrimary
          }
        },
        data: afterSaleStats.value
      }
    ]
  };
};

const buildRechargeOption = (): EChartsOption => {
  const theme = getChartTheme();
  return {
    color: [theme.primary, '#0284c7', '#475569', '#d97706', '#64748b'],
    tooltip: {
      trigger: 'item',
      formatter: (params: any) => `${params.name}<br/>${formatMoney(params.value)} (${params.percent}%)`
    },
    legend: {
      orient: 'vertical',
      right: 8,
      top: 'middle',
      icon: 'circle',
      itemWidth: 8,
      itemHeight: 8,
      textStyle: {
        color: theme.textSecondary,
        fontSize: 12
      }
    },
    series: [
      {
        name: '充值金额',
        type: 'pie',
        radius: ['52%', '76%'],
        center: ['36%', '50%'],
        itemStyle: {
          borderColor: theme.surface,
          borderWidth: 2
        },
        label: {
          show: false
        },
        data: rechargeTypeStats.value
      }
    ]
  };
};

const initChart = (instance: ECharts | null, el?: HTMLDivElement) => {
  if (!el) return null;
  return instance ?? echarts.init(el);
};

const renderCharts = () => {
  if (orderTrendData.value.length) {
    orderTrendChart = initChart(orderTrendChart, orderTrendRef.value);
    orderTrendChart?.setOption(buildOrderTrendOption(), true);
  } else {
    orderTrendChart?.dispose();
    orderTrendChart = null;
  }

  if (categoryStats.value.length) {
    categoryChart = initChart(categoryChart, categoryChartRef.value);
    categoryChart?.setOption(buildCategoryOption(), true);
  } else {
    categoryChart?.dispose();
    categoryChart = null;
  }

  if (afterSaleStats.value.length) {
    afterSaleChart = initChart(afterSaleChart, afterSaleChartRef.value);
    afterSaleChart?.setOption(buildAfterSaleOption(), true);
  } else {
    afterSaleChart?.dispose();
    afterSaleChart = null;
  }

  if (rechargeTypeStats.value.length) {
    rechargeChart = initChart(rechargeChart, rechargeChartRef.value);
    rechargeChart?.setOption(buildRechargeOption(), true);
  } else {
    rechargeChart?.dispose();
    rechargeChart = null;
  }
};

const resizeCharts = () => {
  orderTrendChart?.resize();
  categoryChart?.resize();
  afterSaleChart?.resize();
  rechargeChart?.resize();
};

const loadDashboard = async () => {
  loading.value = true;
  const failedModules: string[] = [];

  try {
    const [
      productRes,
      activeProductRes,
      userRes,
      orderRes,
      pendingOrderRes,
      rechargeRes,
      afterSaleRes,
      pendingAfterSaleRes,
      bannerRes,
      newsRes,
      categoryRes
    ] = await Promise.all([
      safeRequest('商品总览', () => getShopProductListApi({ page: 1, limit: 20 }), failedModules),
      safeRequest('在售商品', () => getShopProductListApi({ page: 1, limit: 1, isShow: 1 }), failedModules),
      safeRequest('会员总览', () => getShopUserListApi({ page: 1, limit: 1 }), failedModules),
      safeRequest('订单总览', () => getShopOrderListApi({ page: 1, limit: 80 }), failedModules),
      safeRequest('待发货订单', () => getShopOrderListApi({ page: 1, limit: 1, status: 0 }), failedModules),
      safeRequest('充值记录', () => getShopUserRechargeListApi({ page: 1, limit: 60 }), failedModules),
      safeRequest('售后记录', () => getShopOrderAfterSaleListApi({ page: 1, limit: 40 }), failedModules),
      safeRequest('待处理售后', () => getShopOrderAfterSaleListApi({ page: 1, limit: 1, status: 0 }), failedModules),
      safeRequest('Banner', () => getShopHomeBannerListApi({ page: 1, limit: 1 }), failedModules),
      safeRequest('资讯', () => getShopHomeNewsListApi({ page: 1, limit: 5 }), failedModules),
      safeRequest('商品分类', () => getShopCategoryAllListApi(), failedModules)
    ]);

    const productPage = getPageData(productRes);
    const activeProductPage = getPageData(activeProductRes);
    const userPage = getPageData(userRes);
    const orderPage = getPageData(orderRes);
    const pendingOrderPage = getPageData(pendingOrderRes);
    const rechargePage = getPageData(rechargeRes);
    const afterSalePage = getPageData(afterSaleRes);
    const pendingAfterSalePage = getPageData(pendingAfterSaleRes);
    const bannerPage = getPageData(bannerRes);
    const newsPage = getPageData(newsRes);
    const categoryTree = getCategoryRows(categoryRes);

    const flatCategories = flattenCategoryTree(categoryTree).slice(0, 6);
    const categoryCountResponses = await Promise.all(
      flatCategories.map(item =>
        safeRequest(`分类-${item.name}`, () => getShopProductListApi({ page: 1, limit: 1, cateId: item.id }), failedModules)
      )
    );

    const afterSaleStatusResponses = await Promise.all(
      AFTER_SALE_STATUS_OPTIONS.map(item =>
        safeRequest(`售后状态-${item.label}`, () => getShopOrderAfterSaleListApi({ page: 1, limit: 1, status: item.value }), failedModules)
      )
    );

    summary.value = {
      totalUsers: userPage.total,
      totalProducts: productPage.total,
      activeProducts: activeProductPage.total,
      totalOrders: orderPage.total,
      pendingShipment: pendingOrderPage.total,
      totalRecharges: rechargePage.total,
      pendingAfterSale: pendingAfterSalePage.total,
      totalBanners: bannerPage.total,
      totalNews: newsPage.total,
      recentOrderAmount: sumBy(orderPage.rows, item => item.payPrice || item.totalPrice),
      recentRechargeAmount: sumBy(rechargePage.rows, item => item.price),
      recentRechargeGiftAmount: sumBy(rechargePage.rows, item => item.givePrice),
      recentAfterSaleAmount: sumBy(afterSalePage.rows, item => item.applyAmount)
    };

    orderTrendData.value = buildOrderTrend(orderPage.rows);
    rechargeTypeStats.value = buildRechargeStats(rechargePage.rows);
    categoryStats.value = flatCategories
      .map((item, index) => ({
        name: item.name,
        value: getPageData(categoryCountResponses[index]).total
      }))
      .sort((prev, next) => next.value - prev.value);
    afterSaleStats.value = AFTER_SALE_STATUS_OPTIONS.map((item, index) => ({
      name: item.label,
      value: getPageData(afterSaleStatusResponses[index]).total
    })).filter(item => item.value > 0);

    recentOrders.value = orderPage.rows.slice(0, 6);
    topProducts.value = [...productPage.rows].sort((prev, next) => toNumber(next.sales) - toNumber(prev.sales)).slice(0, 5);
    latestNews.value = newsPage.rows.slice(0, 5);

    loadWarning.value = failedModules.length
      ? `已展示可用数据，以下模块暂时未返回成功：${[...new Set(failedModules)].slice(0, 6).join('、')}`
      : '';
    lastUpdatedAt.value = new Date().toLocaleString('zh-CN', { hour12: false });

    await nextTick();
    renderCharts();
  } finally {
    loading.value = false;
  }
};

watch([orderStatusOptions, rechargeTypeOptions, () => appStore.isDark, () => appStore.primary], async () => {
  await nextTick();
  renderCharts();
});

onMounted(() => {
  loadDashboard();
  window.addEventListener('resize', resizeCharts);
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts);
  orderTrendChart?.dispose();
  categoryChart?.dispose();
  afterSaleChart?.dispose();
  rechargeChart?.dispose();
});
</script>

<style scoped lang="scss">
@use './index';
</style>
