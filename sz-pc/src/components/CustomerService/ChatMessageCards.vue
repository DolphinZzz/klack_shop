<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import {
  ArrowDown,
  ArrowUp,
  Check,
  Document,
  Goods,
  Location,
  ShoppingBag,
  Tickets,
  Wallet
} from '@element-plus/icons-vue';
import type {
  CsCard,
  CsCitation,
  CsInterruptEnvelope,
  CsInterruptStatus,
  CsOrderCreatedPayload,
  CsOrderDraft,
  CsProductCardItem,
  CsTicketCreatedPayload
} from '@/api/types/cs';

const props = withDefaults(
  defineProps<{
    cards?: CsCard[] | null;
    citations?: CsCitation[] | null;
    interruptPayload?: CsInterruptEnvelope | Record<string, unknown> | null;
    interruptStatus?: CsInterruptStatus;
    resumingInterrupt?: boolean;
  }>(),
  {
    cards: () => [],
    citations: () => [],
    interruptPayload: null,
    interruptStatus: 'pending',
    resumingInterrupt: false
  }
);

const emit = defineEmits<{
  (e: 'view-product', productId: number): void;
  (e: 'quick-buy', product: CsProductCardItem): void;
  (e: 'confirm-order', payload: { addressId: number }): void;
  (e: 'cancel-order'): void;
  (e: 'go-payment', orderNo: string): void;
  (e: 'open-tickets', ticketId?: string | null): void;
  (e: 'manage-address'): void;
}>();

const citationsExpanded = ref(false);
const selectedAddressId = ref<number | null>(null);

const productListCards = computed<CsProductCardItem[][]>(() => {
  if (!props.cards?.length) return [];
  return props.cards
    .filter(card => card.card_type === 'product_list')
    .map(card => {
      const payload = card.payload as { products?: CsProductCardItem[] } | undefined;
      return Array.isArray(payload?.products) ? payload.products : [];
    })
    .filter(list => list.length > 0);
});

const orderCreatedCards = computed<CsOrderCreatedPayload[]>(() => {
  if (!props.cards?.length) return [];
  return props.cards
    .filter(card => card.card_type === 'order_created')
    .map(card => card.payload as CsOrderCreatedPayload)
    .filter(item => Boolean(item && item.order_no));
});

const ticketCreatedCards = computed<CsTicketCreatedPayload[]>(() => {
  if (!props.cards?.length) return [];
  return props.cards
    .filter(card => card.card_type === 'ticket_created')
    .map(card => card.payload as CsTicketCreatedPayload)
    .filter(item => Boolean(item && item.ticket_no));
});

const orderDraft = computed<CsOrderDraft | null>(() => {
  const raw = props.interruptPayload;
  if (!raw || typeof raw !== 'object') return null;
  const record = raw as Record<string, unknown>;
  if (record.payload && typeof record.payload === 'object') {
    return record.payload as CsOrderDraft;
  }
  if (record.draft && typeof record.draft === 'object') {
    return record.draft as CsOrderDraft;
  }
  if ('draft_id' in record && 'product_id' in record) {
    return record as unknown as CsOrderDraft;
  }
  return null;
});

watch(
  orderDraft,
  draft => {
    if (!draft) {
      selectedAddressId.value = null;
      return;
    }
    const available = Array.isArray(draft.available_addresses) ? draft.available_addresses : [];
    if (draft.address_id && available.some(a => a.id === draft.address_id)) {
      selectedAddressId.value = draft.address_id;
      return;
    }
    const defaultAddr = available.find(a => a.is_default === 1 && a.id != null);
    if (defaultAddr?.id != null) {
      selectedAddressId.value = defaultAddr.id;
      return;
    }
    const firstValid = available.find(a => a.id != null);
    selectedAddressId.value = firstValid?.id ?? draft.address_id ?? null;
  },
  { immediate: true }
);

const selectedAddress = computed(() => {
  const draft = orderDraft.value;
  if (!draft) return null;
  const available = Array.isArray(draft.available_addresses) ? draft.available_addresses : [];
  return available.find(addr => addr.id === selectedAddressId.value) ?? null;
});

const mergedCitations = computed<CsCitation[]>(() => {
  const list: CsCitation[] = [];
  const seen = new Set<string>();
  const pushCitation = (item: CsCitation) => {
    if (!item) return;
    const key = `${item.doc_id || ''}:${item.chunk_id || ''}:${item.page_no || 0}:${item.section_path || ''}`;
    if (seen.has(key)) return;
    seen.add(key);
    list.push(item);
  };

  if (Array.isArray(props.citations)) {
    for (const item of props.citations) {
      pushCitation(item);
    }
  }
  if (Array.isArray(props.cards)) {
    for (const card of props.cards) {
      if (card.card_type === 'citations') {
        const payload = card.payload as { citations?: CsCitation[] } | undefined;
        if (Array.isArray(payload?.citations)) {
          for (const item of payload.citations) {
            pushCitation(item);
          }
        }
      }
    }
  }
  return list;
});

const interruptStatusLabel = computed(() => {
  switch (props.interruptStatus) {
    case 'confirmed':
      return '已确认提交';
    case 'cancelled':
      return '已取消下单';
    case 'superseded':
      return '已由新对话取消';
    default:
      return '待确认下单';
  }
});

const categoryLabel = (category?: string) => {
  switch (category) {
    case 'consumer_law':
      return '消法条款';
    case 'mall_policy':
      return '商城规则';
    case 'product_guide':
      return '选购指南';
    case 'faq':
      return '常见问答';
    case 'order_issue':
      return '订单问题';
    case 'after_sale':
      return '售后退款';
    case 'product_consult':
      return '商品咨询';
    case 'complaint':
      return '服务投诉';
    default:
      return '其他咨询';
  }
};

const formatRemainingHint = (seconds?: number) => {
  const safe = Number(seconds || 0);
  if (!Number.isFinite(safe) || safe <= 0) {
    return '请尽快前往收银台完成支付';
  }
  const mins = Math.floor(safe / 60);
  const secs = safe % 60;
  if (mins > 0) {
    return `支付剩余约 ${mins} 分 ${secs} 秒`;
  }
  return `支付剩余约 ${secs} 秒`;
};

const onConfirmOrder = () => {
  if (props.resumingInterrupt || props.interruptStatus !== 'pending' || !selectedAddressId.value) {
    return;
  }
  emit('confirm-order', { addressId: selectedAddressId.value });
};

const onCancelOrder = () => {
  if (props.resumingInterrupt || props.interruptStatus !== 'pending') {
    return;
  }
  emit('cancel-order');
};
</script>

<template>
  <div class="cs-cards-stack">
    <!-- 推荐商品列表卡 -->
    <div v-for="(products, listIdx) in productListCards" :key="`plist-${listIdx}`" class="cs-card cs-product-list-card">
      <div class="cs-card-head">
        <span class="cs-card-title">
          <el-icon><Goods /></el-icon>
          <span>推荐外设装备</span>
        </span>
        <span class="cs-card-count kc-mono">{{ products.length }} ITEMS</span>
      </div>

      <div class="cs-product-items">
        <article v-for="(item, idx) in products" :key="item.id ?? `prod-${idx}`" class="cs-product-row">
          <img
            v-if="item.image"
            :src="item.image"
            :alt="item.product_name"
            class="cs-product-thumb"
            loading="lazy"
          />
          <div v-else class="cs-product-thumb cs-thumb-empty">KC</div>

          <div class="cs-product-body">
            <div class="cs-product-name" :title="item.product_name">{{ item.product_name }}</div>
            <div v-if="item.product_info" class="cs-product-sub" :title="item.product_info">
              {{ item.product_info }}
            </div>
            <div class="cs-product-meta-row">
              <span class="cs-price kc-mono">¥{{ item.price ?? '--' }}</span>
              <span v-if="item.ot_price" class="cs-ot-price kc-mono">¥{{ item.ot_price }}</span>
              <span class="cs-stock kc-mono">库存 {{ item.stock }} · 已售 {{ item.sales }}</span>
            </div>
            <div class="cs-product-actions">
              <button
                type="button"
                class="cs-btn cs-btn-ghost"
                :disabled="item.id == null"
                @click="item.id != null && emit('view-product', item.id)"
              >
                查看详情
              </button>
              <button
                type="button"
                class="cs-btn cs-btn-accent"
                :disabled="item.id == null || item.stock <= 0"
                @click="emit('quick-buy', item)"
              >
                <el-icon><ShoppingBag /></el-icon>
                <span>{{ item.stock <= 0 ? '暂时缺货' : '帮我买这件' }}</span>
              </button>
            </div>
          </div>
        </article>
      </div>
    </div>

    <!-- 下单确认中断卡 -->
    <section
      v-if="orderDraft"
      class="cs-card cs-order-confirm-card"
      :class="`status-${interruptStatus}`"
    >
      <div class="cs-card-head">
        <span class="cs-card-title">
          <el-icon><ShoppingBag /></el-icon>
          <span>核对订单信息</span>
        </span>
        <span class="cs-status-pill" :class="`pill-${interruptStatus}`">
          {{ interruptStatusLabel }}
        </span>
      </div>

      <div class="cs-draft-sku-row">
        <img
          v-if="orderDraft.product_image"
          :src="orderDraft.product_image"
          :alt="orderDraft.product_name"
          class="cs-draft-thumb"
        />
        <div v-else class="cs-draft-thumb cs-thumb-empty">KC</div>
        <div class="cs-draft-info">
          <div class="cs-draft-name">{{ orderDraft.product_name }}</div>
          <div class="cs-draft-sku">规格：{{ orderDraft.sku_name || '标准款' }} × {{ orderDraft.quantity }}</div>
          <div class="cs-draft-pricing kc-mono">
            <span>单价 ¥{{ orderDraft.unit_price }}</span>
            <span>运费 ¥{{ orderDraft.freight_price }}</span>
          </div>
        </div>
      </div>

      <div class="cs-address-block">
        <div class="cs-address-label">
          <el-icon><Location /></el-icon>
          <span>收货地址</span>
          <button type="button" class="cs-link-btn" @click="emit('manage-address')">管理地址</button>
        </div>

        <template v-if="orderDraft.available_addresses?.length">
          <el-select
            v-model="selectedAddressId"
            size="small"
            class="cs-address-select"
            :disabled="interruptStatus !== 'pending' || resumingInterrupt"
            placeholder="请选择收货地址"
          >
            <el-option
              v-for="addr in orderDraft.available_addresses"
              :key="addr.id ?? addr.phone"
              :label="`${addr.real_name} ${addr.phone} · ${addr.full_address}`"
              :value="addr.id as number"
            >
              <div class="cs-addr-opt">
                <span class="cs-addr-opt-top">
                  <strong>{{ addr.real_name }}</strong>
                  <span class="kc-mono">{{ addr.phone }}</span>
                  <span v-if="addr.is_default === 1" class="cs-default-tag">默认</span>
                </span>
                <span class="cs-addr-opt-detail">{{ addr.full_address }}</span>
              </div>
            </el-option>
          </el-select>

          <div v-if="selectedAddress" class="cs-selected-address-preview">
            <div>
              <strong>{{ selectedAddress.real_name }}</strong>
              <span class="kc-mono">{{ selectedAddress.phone }}</span>
            </div>
            <div class="cs-addr-line">{{ selectedAddress.full_address }}</div>
          </div>
        </template>
        <div v-else class="cs-empty-addr-tip">
          暂无可用收货地址，请前往地址管理新增后再下单。
        </div>
      </div>

      <div class="cs-draft-total-row">
        <span>应付总额</span>
        <strong class="cs-total-price kc-mono">¥{{ orderDraft.total_pay_price }}</strong>
      </div>

      <div v-if="interruptStatus === 'pending'" class="cs-confirm-actions">
        <button
          type="button"
          class="cs-btn cs-btn-ghost"
          :disabled="resumingInterrupt"
          @click="onCancelOrder"
        >
          暂不下单
        </button>
        <button
          type="button"
          class="cs-btn cs-btn-accent cs-btn-solid"
          :disabled="resumingInterrupt || !selectedAddressId"
          @click="onConfirmOrder"
        >
          <el-icon v-if="!resumingInterrupt"><Check /></el-icon>
          <span>{{ resumingInterrupt ? '正在提交订单...' : '确认提交订单' }}</span>
        </button>
      </div>
    </section>

    <!-- 待支付订单卡 -->
    <section
      v-for="(order, idx) in orderCreatedCards"
      :key="`${order.order_no}-${idx}`"
      class="cs-card cs-order-created-card"
    >
      <div class="cs-card-head">
        <span class="cs-card-title">
          <el-icon><Wallet /></el-icon>
          <span>订单已创建 · 待支付</span>
        </span>
        <span class="cs-order-no kc-mono">{{ order.order_no }}</span>
      </div>

      <div class="cs-created-body">
        <div v-if="order.product_name" class="cs-created-product">
          {{ order.product_name }}
          <span v-if="order.sku_name">（{{ order.sku_name }}）</span>
          <span v-if="order.quantity">× {{ order.quantity }}</span>
        </div>
        <div class="cs-created-meta">
          <span class="cs-pay-amount kc-mono">应付 ¥{{ order.pay_price }}</span>
          <span class="cs-countdown-hint">{{ formatRemainingHint(order.remaining_seconds) }}</span>
        </div>
      </div>

      <button
        type="button"
        class="cs-btn cs-btn-accent cs-btn-block"
        @click="emit('go-payment', order.order_no)"
      >
        <el-icon><Wallet /></el-icon>
        <span>立即去支付</span>
      </button>
    </section>

    <!-- 工单已生成卡 -->
    <section
      v-for="(ticket, idx) in ticketCreatedCards"
      :key="`${ticket.ticket_no}-${idx}`"
      class="cs-card cs-ticket-created-card"
    >
      <div class="cs-card-head">
        <span class="cs-card-title">
          <el-icon><Tickets /></el-icon>
          <span>人工工单已受理</span>
        </span>
        <span class="cs-order-no kc-mono">{{ ticket.ticket_no }}</span>
      </div>

      <div class="cs-ticket-body">
        <div class="cs-ticket-title-row">
          <span class="cs-cat-tag">{{ categoryLabel(ticket.category) }}</span>
          <strong>{{ ticket.title }}</strong>
        </div>
        <p v-if="ticket.summary" class="cs-ticket-summary">{{ ticket.summary }}</p>
        <div v-if="ticket.related_order_no" class="cs-ticket-order kc-mono">
          关联订单：{{ ticket.related_order_no }}
        </div>
      </div>

      <button
        type="button"
        class="cs-btn cs-btn-dark cs-btn-block"
        @click="emit('open-tickets', ticket.id)"
      >
        <el-icon><Tickets /></el-icon>
        <span>查看我的工单</span>
      </button>
    </section>

    <!-- 可折叠法规与知识引用条 -->
    <div v-if="mergedCitations.length > 0" class="cs-citations-bar">
      <button
        type="button"
        class="cs-citations-toggle"
        :aria-expanded="citationsExpanded"
        @click="citationsExpanded = !citationsExpanded"
      >
        <span class="cs-citations-toggle-left">
          <el-icon><Document /></el-icon>
          <span>参考规则与法规依据（{{ mergedCitations.length }} 条）</span>
        </span>
        <el-icon>
          <ArrowUp v-if="citationsExpanded" />
          <ArrowDown v-else />
        </el-icon>
      </button>

      <div v-if="citationsExpanded" class="cs-citations-list">
        <div
          v-for="(cite, idx) in mergedCitations"
          :key="`${cite.doc_id}-${cite.chunk_id}-${idx}`"
          class="cs-citation-item"
        >
          <div class="cs-citation-head">
            <span class="cs-cat-tag">{{ categoryLabel(cite.category) }}</span>
            <strong class="cs-citation-doc">{{ cite.doc_title || '知识库文档' }}</strong>
            <span v-if="cite.page_no > 0" class="cs-citation-page kc-mono">P.{{ cite.page_no }}</span>
          </div>
          <div v-if="cite.section_path" class="cs-citation-section">{{ cite.section_path }}</div>
          <p class="cs-citation-excerpt">{{ cite.excerpt || cite.content }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.cs-cards-stack {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 8px;
}

.cs-card {
  background: var(--kc-surface);
  border: 1px solid var(--kc-border-strong);
  border-radius: var(--kc-radius-sm);
  padding: 12px;
  box-shadow: var(--kc-shadow-sm);
}

.cs-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 10px;
}

.cs-card-title {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 700;
  color: var(--kc-text);
}

.cs-card-count,
.cs-order-no {
  font-size: 11px;
  color: var(--kc-text-muted);
}

.cs-product-items {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.cs-product-row {
  display: flex;
  gap: 10px;
  padding-top: 10px;
  border-top: 1px dashed var(--kc-divider);
}

.cs-product-row:first-child {
  padding-top: 0;
  border-top: none;
}

.cs-product-thumb,
.cs-draft-thumb {
  width: 60px;
  height: 60px;
  border-radius: 8px;
  object-fit: cover;
  background: var(--kc-surface-muted);
  flex-shrink: 0;
}

.cs-thumb-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--kc-font-mono);
  font-weight: 700;
  font-size: 13px;
  color: var(--kc-text-muted);
}

.cs-product-body,
.cs-draft-info {
  min-width: 0;
  flex: 1;
}

.cs-product-name,
.cs-draft-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--kc-text);
  line-height: 1.35;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.cs-product-sub,
.cs-draft-sku {
  margin-top: 2px;
  font-size: 11px;
  color: var(--kc-text-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.cs-product-meta-row {
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 4px;
}

.cs-price,
.cs-pay-amount,
.cs-total-price {
  color: var(--kc-accent);
  font-weight: 700;
  font-size: 14px;
}

.cs-ot-price {
  font-size: 11px;
  color: var(--kc-text-muted);
  text-decoration: line-through;
}

.cs-stock {
  font-size: 11px;
  color: var(--kc-text-muted);
  margin-left: auto;
}

.cs-product-actions {
  display: flex;
  gap: 6px;
  margin-top: 8px;
}

.cs-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  border-radius: 999px;
  padding: 5px 11px;
  font-size: 12px;
  font-weight: 600;
  border: 1px solid transparent;
  cursor: pointer;
  transition: all 140ms ease;
}

.cs-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.cs-btn-ghost {
  background: var(--kc-surface-muted);
  color: var(--kc-text);
  border-color: var(--kc-border);
}

.cs-btn-ghost:hover:not(:disabled) {
  background: var(--kc-bg-subtle);
}

.cs-btn-accent {
  background: var(--kc-accent-soft);
  color: var(--kc-accent);
  border-color: var(--kc-accent-border);
}

.cs-btn-accent:hover:not(:disabled),
.cs-btn-solid,
.cs-btn-block.cs-btn-accent {
  background: var(--kc-accent);
  color: #ffffff;
  border-color: var(--kc-accent);
}

.cs-btn-accent.cs-btn-solid:hover:not(:disabled),
.cs-btn-block.cs-btn-accent:hover:not(:disabled) {
  background: var(--kc-accent-hover);
}

.cs-btn-dark {
  background: var(--kc-carbon);
  color: #ffffff;
  border-color: var(--kc-carbon);
}

.cs-btn-dark:hover:not(:disabled) {
  background: var(--kc-carbon-soft);
}

.cs-btn-block {
  width: 100%;
  margin-top: 10px;
  padding: 8px 14px;
}

.cs-order-confirm-card.status-pending {
  border-color: var(--kc-accent-border);
  background: linear-gradient(180deg, rgba(255, 77, 0, 0.03) 0%, #ffffff 42%);
}

.cs-status-pill {
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 999px;
}

.pill-pending {
  background: var(--kc-accent-soft);
  color: var(--kc-accent);
}

.pill-confirmed {
  background: rgba(22, 163, 74, 0.12);
  color: var(--kc-success);
}

.pill-cancelled,
.pill-superseded {
  background: var(--kc-surface-muted);
  color: var(--kc-text-muted);
}

.cs-draft-sku-row {
  display: flex;
  gap: 10px;
  padding-bottom: 10px;
  border-bottom: 1px dashed var(--kc-divider);
}

.cs-draft-pricing {
  display: flex;
  gap: 12px;
  margin-top: 6px;
  font-size: 11px;
  color: var(--kc-text-secondary);
}

.cs-address-block {
  margin-top: 10px;
}

.cs-address-label {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  font-weight: 600;
  color: var(--kc-text-secondary);
  margin-bottom: 6px;
}

.cs-link-btn {
  margin-left: auto;
  border: none;
  background: transparent;
  color: var(--kc-accent);
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
  padding: 0;
}

.cs-address-select {
  width: 100%;
}

.cs-addr-opt {
  display: flex;
  flex-direction: column;
  line-height: 1.35;
  padding: 3px 0;
}

.cs-addr-opt-top {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
}

.cs-addr-opt-detail {
  font-size: 11px;
  color: var(--kc-text-muted);
  white-space: normal;
}

.cs-default-tag {
  font-size: 10px;
  padding: 1px 5px;
  border-radius: 4px;
  background: var(--kc-accent-soft);
  color: var(--kc-accent);
}

.cs-selected-address-preview {
  margin-top: 6px;
  padding: 7px 9px;
  border-radius: 8px;
  background: var(--kc-surface-muted);
  font-size: 11px;
  color: var(--kc-text-secondary);
  line-height: 1.45;
}

.cs-selected-address-preview strong {
  color: var(--kc-text);
  margin-right: 6px;
}

.cs-addr-line {
  margin-top: 2px;
}

.cs-empty-addr-tip {
  font-size: 12px;
  color: var(--kc-warning);
  background: rgba(217, 119, 6, 0.08);
  padding: 8px 10px;
  border-radius: 8px;
}

.cs-draft-total-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-top: 10px;
  padding-top: 8px;
  border-top: 1px solid var(--kc-divider);
  font-size: 12px;
  color: var(--kc-text-secondary);
}

.cs-total-price {
  font-size: 16px;
}

.cs-confirm-actions {
  display: flex;
  gap: 8px;
  margin-top: 10px;
}

.cs-confirm-actions .cs-btn {
  flex: 1;
  padding: 8px 10px;
}

.cs-created-body,
.cs-ticket-body {
  font-size: 12px;
  color: var(--kc-text-secondary);
  line-height: 1.5;
}

.cs-created-product {
  color: var(--kc-text);
  font-weight: 600;
  margin-bottom: 4px;
}

.cs-created-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  flex-wrap: wrap;
}

.cs-countdown-hint {
  font-size: 11px;
  color: var(--kc-text-muted);
}

.cs-ticket-title-row {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  color: var(--kc-text);
}

.cs-cat-tag {
  font-size: 10px;
  font-weight: 600;
  padding: 2px 6px;
  border-radius: 4px;
  background: var(--kc-surface-muted);
  color: var(--kc-text-secondary);
  flex-shrink: 0;
}

.cs-ticket-summary {
  margin: 6px 0 0;
  font-size: 12px;
  color: var(--kc-text-secondary);
}

.cs-ticket-order {
  margin-top: 4px;
  font-size: 11px;
  color: var(--kc-text-muted);
}

.cs-citations-bar {
  background: var(--kc-surface-elevated);
  border: 1px solid var(--kc-border);
  border-radius: 10px;
  overflow: hidden;
}

.cs-citations-toggle {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 7px 10px;
  background: transparent;
  border: none;
  cursor: pointer;
  font-size: 11px;
  font-weight: 600;
  color: var(--kc-text-secondary);
}

.cs-citations-toggle:hover {
  color: var(--kc-text);
  background: var(--kc-surface-muted);
}

.cs-citations-toggle-left {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.cs-citations-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 8px 10px 10px;
  border-top: 1px solid var(--kc-divider);
}

.cs-citation-item {
  font-size: 11px;
  line-height: 1.5;
  color: var(--kc-text-secondary);
}

.cs-citation-head {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.cs-citation-doc {
  color: var(--kc-text);
  font-size: 11px;
}

.cs-citation-page {
  margin-left: auto;
  color: var(--kc-text-muted);
}

.cs-citation-section {
  margin-top: 2px;
  color: var(--kc-text-muted);
  font-size: 10px;
}

.cs-citation-excerpt {
  margin: 3px 0 0;
  color: var(--kc-text-secondary);
}
</style>
