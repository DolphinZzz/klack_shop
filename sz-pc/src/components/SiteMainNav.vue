<template>
  <nav class="kc-nav-bar" aria-label="主导航">
    <div class="kc-nav-inner">
      <button
        type="button"
        class="nav-cate-pill"
        :class="{ clickable: categoryClickable }"
        @click="handleCategoryClick"
      >
        <svg class="cate-svg" viewBox="0 0 16 16" fill="none" aria-hidden="true">
          <rect x="2" y="2" width="5" height="5" rx="1.5" stroke="currentColor" stroke-width="1.6" />
          <rect x="9" y="2" width="5" height="5" rx="1.5" stroke="currentColor" stroke-width="1.6" />
          <rect x="2" y="9" width="5" height="5" rx="1.5" stroke="currentColor" stroke-width="1.6" />
          <rect x="9" y="9" width="5" height="5" rx="1.5" stroke="currentColor" stroke-width="1.6" />
        </svg>
        <span>{{ categoryLabel }}</span>
      </button>

      <div class="nav-pills">
        <button
          v-for="item in items"
          :key="item.key"
          type="button"
          class="nav-pill"
          :class="{ active: item.key === activeKey }"
          @click="handleSelect(item.key)"
        >
          <span>{{ item.name }}</span>
        </button>
      </div>

      <div class="nav-status kc-mono" aria-hidden="true">
        <span class="status-dot" />
        <span>CUSTOM STUDIO</span>
      </div>
    </div>
  </nav>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';

const props = withDefaults(
  defineProps<{
    items: Array<{ key: string; name: string }>;
    activeKey?: string;
    categoryLabel?: string;
    categoryClickable?: boolean;
    autoNavigate?: boolean;
  }>(),
  {
    activeKey: '',
    categoryLabel: '全部外设',
    categoryClickable: true,
    autoNavigate: false
  }
);

const emit = defineEmits<{
  (e: 'select', key: string): void;
  (e: 'category-click'): void;
}>();

const router = useRouter();

const handleCategoryClick = () => {
  if (props.autoNavigate) {
    router.push('/products');
    return;
  }
  if (!props.categoryClickable) return;
  emit('category-click');
};

const handleSelect = (key: string) => {
  if (props.autoNavigate) {
    if (key === 'home') {
      router.push('/home');
      return;
    }
    if (key === 'products') {
      router.push('/products');
      return;
    }
    if (key === 'flash') {
      router.push('/flash-sale');
      return;
    }
    if (key === 'coupon') {
      router.push('/coupon-center');
      return;
    }
    if (key === 'order') {
      router.push('/user/order/list');
      return;
    }
    if (key === 'mine-coupon') {
      router.push('/user/wallet/coupon');
      return;
    }
    if (key === 'notice') {
      router.push('/notice');
      return;
    }
    ElMessage.info('该频道即将开放');
    return;
  }
  emit('select', key);
};
</script>

<style scoped lang="scss">
.kc-nav-bar {
  padding-top: 12px;
}

.kc-nav-inner {
  width: min(var(--kc-container-width), calc(100% - 32px));
  margin: 0 auto;
  display: flex;
  align-items: center;
  gap: 10px;
}

.nav-cate-pill {
  height: 36px;
  padding: 0 16px 0 13px;
  border: 1px solid transparent;
  border-radius: 999px;
  background: var(--kc-carbon);
  color: #ffffff;
  font-size: 13px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
  box-shadow: var(--kc-shadow-sm);
  transition: background-color 150ms ease, transform 100ms ease-out;
}

.nav-cate-pill.clickable {
  cursor: pointer;
}

.nav-cate-pill:active {
  transform: scale(0.97);
}

.cate-svg {
  width: 14px;
  height: 14px;
}

.nav-pills {
  display: flex;
  align-items: center;
  gap: 4px;
  overflow-x: auto;
  scrollbar-width: none;
  padding: 2px 0;
}

.nav-pills::-webkit-scrollbar {
  display: none;
}

.nav-pill {
  height: 36px;
  padding: 0 15px;
  border: 1px solid transparent;
  border-radius: 999px;
  background: transparent;
  color: var(--kc-text-secondary);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  transition: color 150ms ease, background-color 150ms ease, box-shadow 150ms ease, transform 100ms ease-out;
}

.nav-pill:active {
  transform: scale(0.97);
}

.nav-pill.active {
  background: var(--kc-surface);
  color: var(--kc-text);
  box-shadow: var(--kc-shadow-sm);
}

.nav-status {
  margin-left: auto;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.72);
  font-size: 11px;
  color: var(--kc-text-muted);
  letter-spacing: 0.08em;
  flex-shrink: 0;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 999px;
  background: var(--kc-success);
}

@media (hover: hover) {
  .nav-cate-pill.clickable:hover {
    background: var(--kc-accent);
  }

  .nav-pill:hover:not(.active) {
    color: var(--kc-text);
    background: rgba(255, 255, 255, 0.72);
  }
}

@media (max-width: 1024px) {
  .nav-status {
    display: none;
  }
}

@media (max-width: 640px) {
  .kc-nav-inner {
    width: calc(100% - 24px);
  }
}
</style>
