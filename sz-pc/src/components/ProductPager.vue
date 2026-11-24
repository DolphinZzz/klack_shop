<script setup lang="ts">
const props = withDefaults(
  defineProps<{
    current: number;
    pageSize: number;
    total: number;
    small?: boolean;
  }>(),
  {
    current: 1,
    pageSize: 20,
    total: 0,
    small: false
  }
);

const emit = defineEmits<{
  (e: 'change', page: number): void;
}>();

const onCurrentChange = (page: number) => {
  emit('change', page);
};
</script>

<template>
  <div class="product-pager">
    <el-pagination
      :small="props.small"
      background
      layout="prev, pager, next"
      :current-page="props.current"
      :page-size="props.pageSize"
      :total="props.total"
      @current-change="onCurrentChange"
    />
  </div>
</template>

<style scoped lang="scss">
.product-pager {
  display: flex;
  justify-content: center;
  padding: 28px 0 4px;
}

.product-pager :deep(.btn-prev),
.product-pager :deep(.btn-next),
.product-pager :deep(.el-pager li) {
  border-radius: 999px;
  min-width: 36px;
  height: 36px;
  line-height: 36px;
  font-family: var(--kc-font-mono);
  font-size: 13px;
  background-color: var(--kc-surface);
  color: var(--kc-text-secondary);
  border: 1px solid transparent;
  box-shadow: var(--kc-shadow-sm);
  transition: transform 100ms ease-out, background-color 150ms ease, color 150ms ease, box-shadow 150ms ease;
}

.product-pager :deep(.el-pager li.is-active) {
  background-color: var(--kc-carbon);
  color: #ffffff;
  box-shadow: var(--kc-shadow-md);
}
</style>
