<template>
  <div class="footer flx-center">
    <a href="javascript:void(0)">
      shop_system v{{ version }} | Copyright © 2022-{{ new Date().getFullYear() }} shop_system
    </a>
  </div>
</template>
<script setup lang="ts">
const version: string = __APP_VERSION__;
</script>
<style scoped lang="scss">
@use './index';
</style>
