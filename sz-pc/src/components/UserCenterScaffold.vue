<script setup lang="ts">
import { computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { useCartStore } from '@/stores/modules/cart';
import SiteHeader from '@/components/SiteHeader.vue';
import SiteMainNav from '@/components/SiteMainNav.vue';
import SiteFooter from '@/components/SiteFooter.vue';
import {
  createUserCenterSideGroups,
  siteNavList,
  userCenterRouteMap,
  type UserCenterMenuKey
} from '@/utils/userCenter';

const props = withDefaults(
  defineProps<{
    activeMenu: UserCenterMenuKey;
    crumbLabel: string;
    title: string;
    description?: string;
    crumbSection?: string;
    activeNavKey?: string;
    hideHeader?: boolean;
  }>(),
  {
    description: '',
    crumbSection: '会员工坊',
    activeNavKey: 'order',
    hideHeader: false
  }
);

const router = useRouter();
const cartStore = useCartStore();

const sideGroups = computed(() => createUserCenterSideGroups(props.activeMenu));
const flatSideItems = computed(() => sideGroups.value.flatMap(group => group.items));

const toHome = () => router.push('/home');

const handleSideItem = (item: { disabled?: boolean; key?: UserCenterMenuKey }) => {
  if (item.disabled) {
    ElMessage.info('该功能正在建设中');
    return;
  }
  if (!item.key) return;
  router.push(userCenterRouteMap[item.key]);
};

onMounted(() => {
  void cartStore.fetchCount();
});
</script>

<template>
  <div class="kc-page">
    <SiteHeader />
    <SiteMainNav :items="siteNavList" :active-key="activeNavKey" auto-navigate />

    <main class="kc-main user-center-main">
      <div class="mobile-nav-strip">
        <button
          v-for="item in flatSideItems"
          :key="item.key"
          type="button"
          class="mobile-nav-pill"
          :class="{ active: item.active }"
          @click="handleSideItem(item)"
        >
          {{ item.label }}
        </button>
      </div>

      <div class="user-center-grid">
        <aside class="side-rail">
          <div class="side-rail-head">
            <span class="kc-kicker">MEMBER CONSOLE</span>
            <strong>造物会员中心</strong>
          </div>

          <div class="side-groups">
            <div v-for="group in sideGroups" :key="group.title" class="side-group">
              <div class="side-title">{{ group.title }}</div>
              <button
                v-for="item in group.items"
                :key="item.label"
                type="button"
                class="side-link"
                :class="{ active: item.active }"
                @click="handleSideItem(item)"
              >
                <span>{{ item.label }}</span>
                <span v-if="item.active" class="active-dot" aria-hidden="true" />
              </button>
            </div>
          </div>
        </aside>

        <section class="content-column">
          <div class="crumb-bar">
            <button type="button" class="crumb-link" @click="toHome">工坊首页</button>
            <span class="crumb-sep">/</span>
            <span class="crumb-section">{{ crumbSection }}</span>
            <span class="crumb-sep">/</span>
            <span class="crumb-current">{{ crumbLabel }}</span>
          </div>

          <div class="content-surface">
            <div v-if="!hideHeader" class="panel-toolbar">
              <div>
                <h1 class="panel-title">{{ title }}</h1>
                <p v-if="description" class="panel-desc">{{ description }}</p>
              </div>
              <slot name="toolbar-extra" />
            </div>
            <slot />
          </div>
        </section>
      </div>
    </main>

    <SiteFooter />
  </div>
</template>

<style scoped lang="scss">
.user-center-main {
  padding-top: 18px;
}

.mobile-nav-strip {
  display: none;
  gap: 8px;
  overflow-x: auto;
  padding: 2px 2px 14px;
  margin-bottom: 8px;
  scrollbar-width: none;
}

.mobile-nav-strip::-webkit-scrollbar {
  display: none;
}

.mobile-nav-pill {
  padding: 8px 16px;
  border-radius: 999px;
  border: 1px solid transparent;
  background: var(--kc-surface);
  box-shadow: var(--kc-shadow-sm);
  color: var(--kc-text-secondary);
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
  cursor: pointer;
}

.mobile-nav-pill.active {
  background: var(--kc-carbon);
  color: #ffffff;
}

.user-center-grid {
  display: grid;
  grid-template-columns: 244px minmax(0, 1fr);
  gap: 22px;
  align-items: start;
}

.side-rail {
  border-radius: 22px;
  border: 1px solid transparent;
  background: var(--kc-surface);
  box-shadow: var(--kc-shadow-md);
  padding: 10px;
}

.side-rail-head {
  padding: 16px 16px 15px;
  border-radius: 14px;
  background: var(--kc-carbon);
  color: #ffffff;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.09);
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.side-rail-head strong {
  font-size: 16px;
  font-weight: 700;
}

.side-groups {
  padding: 12px 2px 4px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.side-group {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.side-title {
  padding: 4px 12px 6px;
  color: var(--kc-text-muted);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.06em;
}

.side-link {
  width: 100%;
  padding: 10px 12px;
  border: 0;
  border-radius: 12px;
  background: transparent;
  color: var(--kc-text-secondary);
  font-size: 14px;
  font-weight: 500;
  display: flex;
  align-items: center;
  justify-content: space-between;
  text-align: left;
  cursor: pointer;
  transition: color 140ms ease, background-color 140ms ease, transform 100ms ease-out;
}

.side-link:active {
  transform: scale(0.985);
}

.side-link.active {
  color: var(--kc-text);
  background: var(--kc-accent-soft);
  font-weight: 700;
}

.active-dot {
  width: 6px;
  height: 6px;
  border-radius: 999px;
  background: var(--kc-accent);
}

.content-column {
  min-width: 0;
}

.crumb-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
  padding-left: 4px;
  font-size: 13px;
  color: var(--kc-text-muted);
}

.crumb-link {
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--kc-text-secondary);
  font-size: 13px;
  cursor: pointer;
  transition: color 140ms ease;
}

.crumb-current {
  color: var(--kc-text);
  font-weight: 600;
}

.content-surface {
  padding: 28px;
  border-radius: 24px;
  border: 1px solid transparent;
  background: var(--kc-surface);
  box-shadow: var(--kc-shadow-md);
}

.panel-toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 24px;
}

.panel-title {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: var(--kc-text);
  line-height: 1.2;
}

.panel-desc {
  margin: 6px 0 0;
  color: var(--kc-text-muted);
  font-size: 13px;
  line-height: 1.6;
}

@media (hover: hover) {
  .side-link:hover:not(.active) {
    color: var(--kc-text);
    background: var(--kc-surface-muted);
  }

  .crumb-link:hover {
    color: var(--kc-accent);
  }
}

@media (max-width: 1024px) {
  .mobile-nav-strip {
    display: flex;
  }

  .user-center-grid {
    grid-template-columns: 1fr;
  }

  .side-rail {
    display: none;
  }
}

@media (max-width: 640px) {
  .content-surface {
    padding: 18px;
    border-radius: 20px;
  }

  .panel-toolbar {
    flex-direction: column;
  }
}
</style>
