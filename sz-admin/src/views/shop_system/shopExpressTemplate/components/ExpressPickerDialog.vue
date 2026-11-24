<template>
  <el-dialog v-model="innerVisible" title="选择快递类型" width="980px" draggable :destroy-on-close="true">
    <!-- 工具栏：查询 + 新增 + 批量删除 -->
    <div class="toolbar">
      <div class="left">
        <el-input v-model="query.expressName" placeholder="快递名" clearable />
        <el-input v-model="query.code" placeholder="编号" clearable />
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>

      <div class="right">
        <el-button type="primary" v-auth="'shop.express.create'" @click="openCreate"> 新增快递公司 </el-button>

        <el-button type="danger" plain v-auth="'shop.express.remove'" :disabled="selectedIds.length === 0" @click="batchRemove">
          批量删除
        </el-button>
      </div>
    </div>

    <el-table :data="list" height="500" border @row-dblclick="handlePick" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" />

      <el-table-column prop="id" label="ID" width="90" />
      <el-table-column prop="expressName" label="快递名" min-width="180" show-overflow-tooltip />
      <el-table-column prop="code" label="编号" width="120" />
      <el-table-column prop="sort" label="排序" width="90" />
      <el-table-column prop="createTime" label="创建时间" min-width="180" />
      <el-table-column prop="updateTime" label="更新时间" min-width="180">
        <template #default="{ row }">
          <span>{{ row.updateTime || '-' }}</span>
        </template>
      </el-table-column>

      <el-table-column label="操作" width="170" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="handlePick(row)">选择</el-button>
          <el-button type="danger" link v-auth="'shop.express.remove'" @click="removeOne(row.id)"> 删除 </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination
        v-model:current-page="query.current"
        v-model:page-size="query.limit"
        :total="total"
        layout="total, prev, pager, next, sizes"
        @current-change="load"
        @size-change="handleSizeChange"
      />
    </div>

    <template #footer>
      <el-button @click="innerVisible = false">关闭</el-button>
    </template>

    <!-- 新增快递公司弹窗 -->
    <ShopExpressFormDialog ref="expressFormRef" @success="afterCreateSuccess" />
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import ShopExpressFormDialog from './ShopExpressFormDialog.vue';
import { getShopExpressListApi, removeShopExpressApi } from '@/api/modules/shop_system/shopExpress'; // 你需要在 api 里提供 remove

export type ShopExpressRow = {
  id: number;
  code: string;
  expressName: string;
  sort: number;
  createTime: string;
  updateTime: string;
  isDel: number;
};

type Props = {
  modelValue: boolean;
};

const props = defineProps<Props>();
const emit = defineEmits<{
  (e: 'update:modelValue', v: boolean): void;
  (e: 'pick', row: ShopExpressRow): void;
}>();

const innerVisible = computed({
  get: () => props.modelValue,
  set: v => emit('update:modelValue', v)
});

const query = reactive({
  current: 1,
  limit: 10,
  expressName: '',
  code: ''
});

const list = ref<ShopExpressRow[]>([]);
const total = ref(0);

// 多选删除
const selectedIds = ref<(number | string)[]>([]);
const handleSelectionChange = (rows: ShopExpressRow[]) => {
  selectedIds.value = rows.map(r => r.id);
};

const load = async () => {
  const res: any = await getShopExpressListApi({ ...query });

  // 按你后端 PageResult: data.rows / data.total
  const page = res?.data ?? res?.data?.data ?? res;
  list.value = page?.rows ?? [];
  total.value = page?.total ?? 0;

  // 翻页后清空选择，避免误删
  selectedIds.value = [];
};

const handleSearch = async () => {
  query.current = 1;
  await load();
};

const handleReset = async () => {
  query.current = 1;
  query.limit = 10;
  query.expressName = '';
  query.code = '';
  await load();
};

const handleSizeChange = async () => {
  query.current = 1;
  await load();
};

const handlePick = (row: ShopExpressRow) => {
  emit('pick', row);
  innerVisible.value = false;
};

// 新增
const expressFormRef = ref<InstanceType<typeof ShopExpressFormDialog>>();
const openCreate = () => {
  expressFormRef.value?.open();
};
const afterCreateSuccess = async () => {
  // 新增成功后刷新列表
  query.current = 1;
  await load();
};

// 删除
const removeOne = async (id: number) => {
  await ElMessageBox.confirm('确认删除该快递公司吗？', '提示', { type: 'warning' });
  await removeShopExpressApi({ ids: [id] });
  ElMessage.success('删除成功');
  // 当前页如果被删空了可以回退一页（简单处理：不回退也行）
  await load();
};

const batchRemove = async () => {
  await ElMessageBox.confirm(`确认删除选中的 ${selectedIds.value.length} 条记录吗？`, '提示', {
    type: 'warning'
  });
  await removeShopExpressApi({ ids: selectedIds.value });
  ElMessage.success('删除成功');
  await load();
};

watch(
  () => props.modelValue,
  async v => {
    if (v) {
      await load();
    }
  }
);
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.left {
  display: flex;
  gap: 10px;
  align-items: center;
}

.right {
  display: flex;
  gap: 10px;
  align-items: center;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}
</style>
