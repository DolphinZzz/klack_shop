<template>
  <el-dialog v-model="innerVisible" title="商品列表" width="980px" draggable :destroy-on-close="true">
    <div class="toolbar">
      <div class="filters">
        <el-input v-model="query.productName" placeholder="请输入商品名称" clearable />
        <el-select v-model="query.cateId" clearable filterable placeholder="请选择平台商品分类">
          <el-option v-for="item in cateOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="query.isShow" clearable placeholder="请选择商品状态">
          <el-option
            v-for="item in statusOptions"
            :key="item.alias"
            :label="item.codeName"
            :value="Number(item.alias)"
          />
        </el-select>
      </div>
      <div class="actions">
        <div class="left-actions">
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </div>
        <div class="right-actions">
          <el-button type="primary" plain @click="handlePickAll">选择全部商品</el-button>
        </div>
      </div>
    </div>

    <el-table
      ref="tableRef"
      :data="list"
      height="500"
      border
      @row-dblclick="handlePick"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="55" />
      <el-table-column prop="id" label="ID" width="90" />
      <el-table-column prop="image" label="商品图" width="90">
        <template #default="{ row }">
          <el-image
            v-if="getImageUrl(row?.image)"
            :src="getImageUrl(row?.image)"
            :preview-src-list="getPreviewList(row?.image)"
            fit="cover"
            style="width: 60px; height: 60px; border-radius: 6px"
            preview-teleported
          />
          <el-tag v-else type="info" size="small">暂无图片</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="productName" label="商品名称" min-width="180" show-overflow-tooltip />
      <el-table-column prop="cateId" label="商品分类" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">
          <span>{{ cateLabelMap.get(row?.cateId) || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="isShow" label="商品状态" width="120">
        <template #default="{ row }">
          <span>{{ getStatusLabel(row?.isShow) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="handlePick(row)">选择</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.limit"
        :total="total"
        layout="total, prev, pager, next, sizes"
        @current-change="load"
        @size-change="handleSizeChange"
      />
    </div>

    <template #footer>
      <el-button @click="innerVisible = false">取消</el-button>
      <el-button type="primary" @click="handleConfirm">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { getShopProductListApi } from '@/api/modules/shop_system/shopProduct';
import { getShopCategoryAllListApi } from '@/api/modules/shop_system/shopCategory';
import { useDict } from '@/hooks/useDict';
import { useDictOptions } from '@/hooks/useDictOptions';
import type { ShopProductQuery, ShopProductRow } from '@/api/types/shop_system/shopProduct';

type Props = {
  modelValue: boolean;
};

type Option = { label: string; value: number };

type CategoryNode = {
  id: number;
  pid: number;
  cateName: string;
  children?: CategoryNode[];
};

const props = defineProps<Props>();
const emit = defineEmits<{
  (e: 'update:modelValue', v: boolean): void;
  (e: 'pick', row: ShopProductRow): void;
}>();

const innerVisible = computed({
  get: () => props.modelValue,
  set: v => emit('update:modelValue', v)
});

useDict(['is_show']);
const statusOptions = useDictOptions('is_show');

const query = reactive<ShopProductQuery>({
  page: 1,
  limit: 10,
  productName: '',
  cateId: undefined,
  isShow: undefined
});

const list = ref<ShopProductRow[]>([]);
const total = ref(0);
const tableRef = ref();
const selectedRow = ref<ShopProductRow | null>(null);

const cateOptions = ref<Option[]>([]);
const cateLabelMap = computed(() => new Map(cateOptions.value.map(item => [item.value, item.label])));
const cateLoaded = ref(false);

const flattenCategories = (nodes: CategoryNode[] = [], prefix = ''): Option[] => {
  const res: Option[] = [];
  for (const n of nodes) {
    const label = prefix ? `${prefix} / ${n.cateName}` : n.cateName;
    res.push({ label, value: n.id });
    if (n.children?.length) res.push(...flattenCategories(n.children, label));
  }
  return res;
};

const loadCategories = async () => {
  if (cateLoaded.value) return;
  const res: any = await getShopCategoryAllListApi();
  const raw = res?.data ?? res;
  const tree: CategoryNode[] = Array.isArray(raw) ? raw : Array.isArray(raw?.data) ? raw.data : [];
  cateOptions.value = flattenCategories(tree);
  cateLoaded.value = true;
};

const getStatusLabel = (value?: number) => {
  const item = statusOptions.value.find(option => Number(option.alias) === Number(value));
  return item?.codeName ?? '-';
};

const getImageUrl = (image: any): string => {
  if (!image) return '';
  if (Array.isArray(image)) {
    const first = image[0];
    return typeof first === 'string' ? first : first?.url || '';
  }
  return image;
};

const getPreviewList = (image: any): string[] => {
  if (!image) return [];
  if (Array.isArray(image)) {
    return image.map((item: any) => (typeof item === 'string' ? item : item?.url)).filter(Boolean);
  }
  return [image];
};

const load = async (page?: number) => {
  if (typeof page === 'number') query.page = page;
  const res: any = await getShopProductListApi({ ...query });
  const pageData = res?.data ?? res?.data?.data ?? res;
  list.value = pageData?.rows ?? [];
  total.value = pageData?.total ?? 0;
  if (typeof pageData?.current === 'number') query.page = pageData.current;
  selectedRow.value = null;
  tableRef.value?.clearSelection?.();
};

const handleSelectionChange = (rows: ShopProductRow[]) => {
  selectedRow.value = rows.length ? rows[rows.length - 1] : null;
};

const handleSearch = async () => {
  query.page = 1;
  await load();
};

const handleReset = async () => {
  query.page = 1;
  query.limit = 10;
  query.productName = '';
  query.cateId = undefined;
  query.isShow = undefined;
  await load();
};

const handleSizeChange = async () => {
  query.page = 1;
  await load();
};

const handlePick = (row: ShopProductRow) => {
  emit('pick', row);
  innerVisible.value = false;
};

const handleConfirm = () => {
  if (!selectedRow.value) {
    ElMessage.warning('请选择商品');
    return;
  }
  handlePick(selectedRow.value);
};

const handlePickAll = () => {
  handlePick({ id: 0, productName: '全部商品' } as ShopProductRow);
};

watch(
  () => props.modelValue,
  async value => {
    if (value) {
      await loadCategories();
      await load();
    }
  }
);

</script>

<style scoped>
.toolbar {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 12px;
}

.filters {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.left-actions,
.right-actions {
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
