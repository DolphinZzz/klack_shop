<template>
  <div class="table-box">
    <ProTable
      ref="proTableRef"
      title="商品表"
      :indent="20"
      :columns="columns"
      :search-columns="searchColumns"
      :request-api="getTableList"
      row-key="id"
    >
      <!-- 表格 header 按钮 -->
      <template #tableHeader="scope">
        <el-button type="primary" v-auth="'lb.product.create'" :icon="CirclePlus" @click="openCreateDialog"> 新增 </el-button>
        <el-button
          v-auth="'lb.product.remove'"
          type="danger"
          :icon="Delete"
          plain
          :disabled="!scope.isSelected"
          @click="batchDelete(scope.selectedListIds)"
        >
          批量删除
        </el-button>
        <el-button v-auth="'lb.product.export'" type="primary" :icon="Download" plain @click="downloadFile"> 导出 </el-button>
      </template>
      <template #image="{ row }">
        <el-image
          v-if="getImageUrl(row?.image)"
          :src="getImageUrl(row?.image)"
          :preview-src-list="getPreviewList(row?.image)"
          fit="cover"
          style="width: 60px; height: 60px; border-radius: 8px"
          preview-teleported
        />
        <el-tag v-else type="info" size="small">暂无图片</el-tag>
      </template>
      <template #operation="{ row }">
        <el-button link :icon="EditPen" type="primary" @click="openEdit(row)">编辑</el-button>

        <el-button v-auth="'lb.product.update'" type="primary" link :icon="List" @click="openDetail(row)"> 详情 </el-button>

        <el-button v-auth="'lb.product.remove'" type="primary" link :icon="Delete" @click="deleteInfo(row)"> 删除 </el-button>
      </template>
    </ProTable>
    <ShopProductCreateDialog ref="createDialogRef" @success="proTableRef?.getTableList()" />
    <ShopProductDetailDrawer ref="detailDrawerRef" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { CirclePlus, Delete, EditPen, Download, List } from '@element-plus/icons-vue';
import ProTable from '@/components/ProTable/index.vue';
import { removeShopProductApi, getShopProductListApi, exportShopProductExcelApi } from '@/api/modules/shop_system/shopProduct';
import { useHandleData } from '@/hooks/useHandleData';
import { useDictOptions } from '@/hooks/useDictOptions';
import type { ColumnProps, ProTableInstance, SearchProps } from '@/components/ProTable/interface';
import type { ShopProductQuery, ShopProductRow } from '@/api/types/shop_system/shopProduct';
import { useDownload } from '@/hooks/useDownload';
import { useDict } from '@/hooks/useDict';
import ShopProductDetailDrawer from '@/views/shop_system/shopProduct/components/ShopProductDetailDrawer.vue';
import ShopProductCreateDialog from '@/views/shop_system/shopProduct/components/ShopProductCreateDialog.vue';
import { getShopCategoryAllListApi } from '@/api/modules/shop_system/shopCategory';

// 使用useDict Hook 主动加载字典
useDict(['is_postage', 'is_bargain', 'is_seckill', 'is_new', 'is_hot', 'activity_type', 'logitech_del', 'is_best', 'is_show']);
const proTableRef = ref<ProTableInstance>();
const createDialogRef = ref<InstanceType<typeof ShopProductCreateDialog>>();

// 表格配置项
const columns: ColumnProps<ShopProductRow>[] = [
  { type: 'selection', width: 80 },
  { prop: 'image', label: '商品图片' },
  { prop: 'productName', label: '商品名称' },
  { prop: 'price', label: '商品价格' },
  { prop: 'sales', label: '销量' },
  { prop: 'stock', label: '库存' },
  {
    prop: 'isShow',
    label: '状态',
    tag: true,
    enum: useDictOptions('is_show'),
    fieldNames: {
      label: 'codeName',
      value: 'alias',
      tagType: 'callbackShowStyle'
    }
  },
  {
    prop: 'isSeckill',
    label: '开启秒杀',
    tag: true,
    enum: useDictOptions('is_seckill'),
    fieldNames: {
      label: 'codeName',
      value: 'alias',
      tagType: 'callbackShowStyle'
    }
  },
  { prop: 'createTime', label: '创建时间' },
  { prop: 'updateTime', label: '更新时间' },
  { prop: 'operation', label: '操作', width: 250, fixed: 'right' }
];

type Option = { label: string; value: number };
type CategoryNode = {
  id: number;
  pid: number;
  cateName: string;
  children?: CategoryNode[];
};

const cateOptions = ref<Option[]>([]);
const categoryTree = ref<CategoryNode[]>([]);
// 搜索条件项
const searchColumns = computed<SearchProps[]>(() => [
  { prop: 'productName', label: '商品名称', el: 'input' },
  {
    prop: 'cateId',
    label: '分类名称',
    el: 'select',
    enum: cateOptions.value, // ✅ 直接绑定
    fieldNames: { label: 'label', value: 'value' },
    props: { clearable: true, filterable: true }
  },
  {
    prop: 'isShow',
    label: '状态',
    el: 'select',
    enum: useDictOptions('is_show'),
    fieldNames: { label: 'codeName', value: 'alias', tagType: 'callbackShowStyle' }
  }
]);

const flattenCategories = (nodes: CategoryNode[] = [], prefix = ''): Option[] => {
  const res: Option[] = [];
  for (const n of nodes) {
    const label = prefix ? `${prefix} / ${n.cateName}` : n.cateName;
    res.push({ label, value: n.id });
    if (n.children?.length) res.push(...flattenCategories(n.children, label));
  }
  return res;
};

const loadCategoryTree = async () => {
  const res: any = await getShopCategoryAllListApi();

  const raw = res?.data ?? res;
  const tree: CategoryNode[] = Array.isArray(raw) ? raw : Array.isArray(raw?.data) ? raw.data : [];

  categoryTree.value = tree;
  cateOptions.value = flattenCategories(tree);

  // ✅ 如果你的 ProTable 不会 watch searchColumns 变化，可以强制刷新一次
  proTableRef.value?.reset?.();
};

onMounted(loadCategoryTree);

const openCreateDialog = () => {
  createDialogRef.value?.openCreate();
};
const openEdit = (row: any) => {
  createDialogRef.value?.openEdit(row.id);
};
const detailDrawerRef = ref<InstanceType<typeof ShopProductDetailDrawer>>();

// 兼容 image 是 string / array 两种情况
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

const openDetail = async (row: any) => {
  // 直接把 productId 传进去，让抽屉自己请求并渲染
  detailDrawerRef.value?.open(row?.id);
};

// 获取table列表
const getTableList = (params: ShopProductQuery) => {
  let newParams = formatParams(params);
  return getShopProductListApi(newParams);
};
const formatParams = (params: ShopProductQuery) => {
  let newParams = JSON.parse(JSON.stringify(params));
  if (newParams.createTime) {
    newParams.createTimeStart = newParams.createTime[0];
    newParams.createTimeEnd = newParams.createTime[1];
    delete newParams.createTime;
  }

  if (newParams.updateTime) {
    newParams.updateTimeStart = newParams.updateTime[0];
    newParams.updateTimeEnd = newParams.updateTime[1];
    delete newParams.updateTime;
  }

  return newParams;
};
// 删除信息
const deleteInfo = async (params: ShopProductRow) => {
  await useHandleData(removeShopProductApi, { ids: [params.id] }, `删除【${params.id}】商品表`);
  proTableRef.value?.getTableList();
};
// 批量删除信息
const batchDelete = async (ids: (string | number)[]) => {
  await useHandleData(removeShopProductApi, { ids }, '删除所选商品表');
  proTableRef.value?.clearSelection();
  proTableRef.value?.getTableList();
};
// 导出
const downloadFile = async () => {
  let newParams = formatParams(proTableRef.value?.searchParam as ShopProductQuery);
  useDownload(exportShopProductExcelApi, '商品表', newParams);
};
</script>
