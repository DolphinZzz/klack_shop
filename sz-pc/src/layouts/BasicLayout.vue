<script setup lang="ts">
import SiteHeader from '@/components/SiteHeader.vue';
import SiteFooter from '@/components/SiteFooter.vue';
</script>

<template>
  <div class="kc-page">
    <SiteHeader />
    <main class="kc-main">
      <router-view />
    </main>
    <SiteFooter />
  </div>
</template>
