<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { shopUserLogoutApi } from '@/api';
import { useAuthStore } from '@/stores/modules/auth';
import { useCartStore } from '@/stores/modules/cart';

const props = withDefaults(
  defineProps<{
    modelValue?: string;
    placeholder?: string;
    mode?: 'default' | 'checkout';
    checkoutTitle?: string;
    checkoutStep?: 1 | 2 | 3;
    backLabel?: string;
  }>(),
  {
    modelValue: undefined,
    placeholder: '搜索机械键盘、客制化轴体、轻量化鼠标、HiFi 耳机...',
    mode: 'default',
    checkoutTitle: '结算流程',
    checkoutStep: 1,
    backLabel: '返回购物车'
  }
);

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void;
  (e: 'search', keyword: string): void;
  (e: 'back'): void;
}>();

const router = useRouter();
const authStore = useAuthStore();
const cartStore = useCartStore();

const innerKeyword = ref(props.modelValue ?? '');

watch(
  () => props.modelValue,
  val => {
    if (val !== undefined && val !== innerKeyword.value) {
      innerKeyword.value = val;
    }
  }
);

const keyword = computed({
  get: () => (props.modelValue !== undefined ? props.modelValue : innerKeyword.value),
  set: (val: string) => {
    innerKeyword.value = val;
    emit('update:modelValue', val);
  }
});

const cartCount = computed(() => cartStore.cartCount);
const displayName = computed(
  () => authStore.userInfo.nickname || authStore.userInfo.username || authStore.userInfo.account || '极客会员'
);

const checkoutSteps = [
  { num: 1, code: '01', label: '购物清单' },
  { num: 2, code: '02', label: '核对配置' },
  { num: 3, code: '03', label: '安全支付' }
];

const toHome = () => router.push('/home');
const toCart = () => router.push('/cart');
const toProfile = () => router.push('/user/account/profile');

const onSearch = () => {
  const value = keyword.value.trim();
  if (props.modelValue !== undefined) {
    emit('search', value);
    return;
  }
  if (!value) {
    router.push('/products');
    return;
  }
  router.push(`/products?keyword=${encodeURIComponent(value)}`);
};

const onBack = () => {
  emit('back');
};

const onAuthAction = async () => {
  if (!authStore.isLogin) {
    router.push('/login');
    return;
  }
  try {
    await shopUserLogoutApi();
  } finally {
    authStore.logout();
    cartStore.clear();
    ElMessage.success('已退出登录');
    router.replace('/home');
  }
};
</script>

<template>
  <header class="kc-header" :class="`mode-${mode}`">
    <div class="kc-header-inner">
      <button type="button" class="brand-lockup" @click="toHome" aria-label="返回咔嗒造物商城首页">
        <span class="brand-keycap" aria-hidden="true">
          <span class="keycap-top">
            <span class="keycap-letter">K</span>
            <span class="keycap-led" />
          </span>
        </span>
        <span class="brand-copy">
          <span class="brand-title-row">
            <strong>咔嗒造物</strong>
            <span class="brand-en">KlackCraft</span>
          </span>
          <span class="brand-sub">CUSTOM PERIPHERALS STUDIO</span>
        </span>
      </button>

      <template v-if="mode === 'default'">
        <div class="search-pill">
          <el-icon class="search-prefix"><Search /></el-icon>
          <input
            v-model="keyword"
            type="text"
            class="search-field"
            :placeholder="placeholder"
            @keyup.enter="onSearch"
          />
          <button type="button" class="search-submit" @click="onSearch">
            <span>搜索</span>
          </button>
        </div>

        <div class="utility-actions">
          <button type="button" class="utility-pill cart-trigger" @click="toCart">
            <el-icon><ShoppingTrolley /></el-icon>
            <span>购物车</span>
            <span class="count-pill kc-mono">{{ cartCount }}</span>
          </button>

          <button
            v-if="authStore.isLogin"
            type="button"
            class="utility-pill account-trigger"
            @click="toProfile"
            :title="displayName"
          >
            <el-avatar :size="24" :src="authStore.userInfo.avatar" class="mini-avatar">
              {{ displayName.slice(0, 1) }}
            </el-avatar>
            <span class="account-name">{{ displayName }}</span>
          </button>

          <button type="button" class="utility-pill auth-trigger" :class="{ primary: !authStore.isLogin }" @click="onAuthAction">
            {{ authStore.isLogin ? '退出' : '登录 / 注册' }}
          </button>
        </div>
      </template>

      <template v-else>
        <div class="checkout-track" aria-label="结算进度">
          <div
            v-for="step in checkoutSteps"
            :key="step.num"
            class="checkout-step"
            :class="{ active: checkoutStep === step.num, done: checkoutStep > step.num }"
          >
            <span class="step-code kc-mono">{{ step.code }}</span>
            <span class="step-label">{{ step.label }}</span>
          </div>
        </div>

        <div class="utility-actions">
          <button type="button" class="utility-pill" @click="onBack">
            <el-icon><ArrowLeft /></el-icon>
            <span>{{ backLabel }}</span>
          </button>
        </div>
      </template>
    </div>
  </header>
</template>

<style scoped lang="scss">
.kc-header {
  position: sticky;
  top: 0;
  z-index: 40;
  background: rgba(246, 246, 244, 0.84);
  backdrop-filter: blur(18px) saturate(160%);
  -webkit-backdrop-filter: blur(18px) saturate(160%);
  box-shadow: 0 1px 0 rgba(21, 22, 26, 0.04);
}

.kc-header-inner {
  width: min(var(--kc-container-width), calc(100% - 32px));
  margin: 0 auto;
  min-height: 74px;
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 28px;
  padding: 12px 0;
}

.brand-lockup {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  padding: 0;
  border: 0;
  background: transparent;
  cursor: pointer;
  text-align: left;
  color: inherit;
}

.brand-keycap {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: #15161a;
  padding: 3px 3px 6px;
  display: grid;
  place-items: stretch;
  box-shadow: var(--kc-shadow-sm);
  transition: transform 120ms ease-out;
}

.brand-lockup:active .brand-keycap {
  transform: scale(0.95);
}

.keycap-top {
  position: relative;
  border-radius: 8px;
  background: linear-gradient(180deg, #282a32 0%, #1d1f25 100%);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.12);
  display: grid;
  place-items: center;
}

.keycap-letter {
  font-family: var(--kc-font-mono);
  font-size: 15px;
  font-weight: 700;
  color: #ffffff;
  line-height: 1;
}

.keycap-led {
  position: absolute;
  top: 5px;
  right: 5px;
  width: 5px;
  height: 5px;
  border-radius: 999px;
  background: var(--kc-accent);
}

.brand-copy {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.brand-title-row {
  display: inline-flex;
  align-items: baseline;
  gap: 7px;
  line-height: 1;
}

.brand-title-row strong {
  font-size: 18px;
  font-weight: 700;
  color: var(--kc-text);
  letter-spacing: -0.02em;
}

.brand-en {
  font-family: var(--kc-font-mono);
  font-size: 13px;
  font-weight: 600;
  color: var(--kc-accent);
}

.brand-sub {
  font-family: var(--kc-font-mono);
  font-size: 10px;
  letter-spacing: 0.12em;
  color: var(--kc-text-muted);
  line-height: 1;
}

/* Soft Floating Capsule Search */
.search-pill {
  display: flex;
  align-items: center;
  height: 44px;
  max-width: 540px;
  width: 100%;
  justify-self: center;
  background: var(--kc-surface);
  border: 1px solid transparent;
  border-radius: 999px;
  padding: 4px 4px 4px 16px;
  box-shadow: var(--kc-shadow-sm);
  transition: box-shadow 180ms ease;
}

.search-pill:focus-within {
  box-shadow:
    0 0 0 2px rgba(21, 22, 26, 0.14),
    var(--kc-shadow-md);
}

.search-prefix {
  color: var(--kc-text-muted);
  font-size: 15px;
  flex-shrink: 0;
}

.search-field {
  flex: 1;
  min-width: 0;
  height: 100%;
  border: 0;
  outline: none;
  background: transparent;
  padding: 0 12px;
  font-size: 13px;
  color: var(--kc-text);
}

.search-field::placeholder {
  color: var(--kc-text-muted);
}

.search-submit {
  height: 36px;
  padding: 0 20px;
  border: 0;
  border-radius: 999px;
  background: var(--kc-carbon);
  color: #ffffff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 150ms ease, transform 100ms ease-out;
  flex-shrink: 0;
}

.search-submit:active {
  transform: scale(0.96);
}

.utility-actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  justify-self: end;
}

.utility-pill {
  height: 40px;
  padding: 0 14px 0 12px;
  border: 1px solid transparent;
  border-radius: 999px;
  background: var(--kc-surface);
  color: var(--kc-text);
  font-size: 13px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  cursor: pointer;
  box-shadow: var(--kc-shadow-sm);
  transition: transform 100ms ease-out, background-color 150ms ease, box-shadow 150ms ease;
  white-space: nowrap;
}

.utility-pill:active {
  transform: scale(0.97);
}

.utility-pill.primary {
  padding: 0 18px;
  background: var(--kc-carbon);
  color: #ffffff;
}

.count-pill {
  min-width: 22px;
  height: 22px;
  padding: 0 6px;
  border-radius: 999px;
  background: var(--kc-accent-soft);
  color: var(--kc-accent);
  font-size: 12px;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.account-trigger {
  padding-left: 7px;
}

.mini-avatar {
  background: var(--kc-carbon);
  color: #ffffff;
  font-size: 11px;
  font-weight: 700;
}

.account-name {
  max-width: 88px;
  overflow: hidden;
  text-overflow: ellipsis;
}

@media (hover: hover) {
  .search-submit:hover {
    background: var(--kc-accent);
  }

  .utility-pill:hover {
    box-shadow: var(--kc-shadow-md);
  }

  .utility-pill.primary:hover {
    background: var(--kc-accent);
  }
}

/* Checkout Progress Track */
.checkout-track {
  display: inline-flex;
  align-items: center;
  justify-self: center;
  gap: 10px;
  padding: 5px;
  border-radius: 999px;
  background: var(--kc-surface);
  box-shadow: var(--kc-shadow-sm);
}

.checkout-step {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 5px 12px 5px 7px;
  border-radius: 999px;
  color: var(--kc-text-muted);
  font-size: 13px;
  font-weight: 600;
}

.step-code {
  width: 22px;
  height: 22px;
  border-radius: 999px;
  background: var(--kc-surface-muted);
  display: grid;
  place-items: center;
  font-size: 11px;
}

.checkout-step.active {
  background: var(--kc-accent-soft);
  color: var(--kc-text);
}

.checkout-step.active .step-code {
  background: var(--kc-accent);
  color: #ffffff;
}

.checkout-step.done {
  color: var(--kc-text-secondary);
}

.checkout-step.done .step-code {
  background: var(--kc-carbon);
  color: #ffffff;
}

@media (max-width: 1024px) {
  .kc-header-inner {
    grid-template-columns: 1fr auto;
    gap: 12px;
  }

  .search-pill,
  .checkout-track {
    grid-column: 1 / -1;
    order: 3;
    max-width: none;
    justify-self: stretch;
  }

  .checkout-track {
    justify-content: flex-start;
    overflow-x: auto;
  }
}

@media (max-width: 640px) {
  .kc-header-inner {
    width: calc(100% - 24px);
  }

  .brand-sub,
  .account-name {
    display: none;
  }

  .utility-pill {
    padding: 0 11px;
    font-size: 12px;
  }
}
</style>
