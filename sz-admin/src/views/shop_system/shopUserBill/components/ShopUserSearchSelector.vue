<template>
  <div class="user-search-selector">
    <el-input :model-value="selectedLabel" :placeholder="placeholder || '请选择用户'" readonly clearable @clear="clearSelected">
      <template #append>
        <el-button @click="dialogVisible = true">选择</el-button>
      </template>
    </el-input>

    <el-dialog v-model="dialogVisible" title="选择用户" width="920px" destroy-on-close>
      <div class="toolbar">
        <el-input v-model="query.account" placeholder="用户账号" clearable />
        <el-input v-model="query.nickname" placeholder="用户昵称" clearable />
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" border height="460" @row-dblclick="pickUser">
        <el-table-column prop="uid" label="用户ID" width="100" />
        <el-table-column prop="account" label="用户账号" min-width="180" />
        <el-table-column prop="nickname" label="用户昵称" min-width="140" />
        <el-table-column prop="phone" label="手机号" min-width="150" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="pickUser(row)">选择</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="query.current"
          v-model:page-size="query.limit"
          :total="total"
          layout="total, prev, pager, next, sizes"
          @current-change="loadUsers"
          @size-change="handleSizeChange"
        />
      </div>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import { getShopUserListApi } from '@/api/modules/shop_system/shopUser';
import type { ShopUserListRow } from '@/api/types/shop_system/shopUser';

defineOptions({
  name: 'ShopUserSearchSelector'
});

const props = defineProps<{
  modelValue?: number | string;
  placeholder?: string;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', value?: number | string): void;
}>();

const dialogVisible = ref(false);
const loading = ref(false);
const tableData = ref<ShopUserListRow[]>([]);
const total = ref(0);
const selectedUser = ref<ShopUserListRow | null>(null);

const query = reactive({
  current: 1,
  limit: 10,
  account: '',
  nickname: ''
});

const selectedLabel = computed(() => {
  if (selectedUser.value) {
    const uid = selectedUser.value.uid ?? '-';
    const account = selectedUser.value.account || '-';
    const nickname = selectedUser.value.nickname || '-';
    return `${uid} / ${account} / ${nickname}`;
  }
  if (props.modelValue === 0 || props.modelValue) return String(props.modelValue);
  return '';
});

const loadUsers = async () => {
  loading.value = true;
  try {
    const res = await getShopUserListApi({ ...query });
    const page = (res as any)?.data?.data ?? (res as any)?.data ?? {};
    tableData.value = page?.rows || [];
    total.value = page?.total || 0;
  } finally {
    loading.value = false;
  }
};

const handleSearch = async () => {
  query.current = 1;
  await loadUsers();
};

const handleReset = async () => {
  query.current = 1;
  query.limit = 10;
  query.account = '';
  query.nickname = '';
  await loadUsers();
};

const handleSizeChange = async () => {
  query.current = 1;
  await loadUsers();
};

const pickUser = (row: ShopUserListRow) => {
  selectedUser.value = row;
  emit('update:modelValue', row.uid);
  dialogVisible.value = false;
};

const clearSelected = () => {
  selectedUser.value = null;
  emit('update:modelValue', undefined);
};

watch(
  () => props.modelValue,
  value => {
    if (value !== 0 && !value) selectedUser.value = null;
  }
);

watch(
  () => dialogVisible.value,
  async visible => {
    if (visible) await loadUsers();
  }
);
</script>

<style scoped lang="scss">
.user-search-selector {
  width: 100%;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}
</style>
