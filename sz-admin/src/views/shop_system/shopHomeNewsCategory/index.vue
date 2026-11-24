<template>
  <div class="table-box">
    <ProTable
      ref="proTableRef"
      title="资讯分类管理"
      :indent="20"
      :columns="columns"
      :search-columns="searchColumns"
      :request-api="getTableList"
      row-key="id"
    >
      <template #tableHeader="scope">
        <el-button
          type="primary"
          v-auth="'shop.home.news.category.create'"
          :icon="CirclePlus"
          @click="openAddEdit('新增资讯分类')"
        >
          新增
        </el-button>
        <el-button
          v-auth="'shop.home.news.category.remove'"
          type="danger"
          :icon="Delete"
          plain
          :disabled="!scope.isSelected"
          @click="batchDelete(scope.selectedListIds)"
        >
          批量删除
        </el-button>
      </template>
      <template #operation="{ row }">
        <el-button
          v-auth="'shop.home.news.category.update'"
          type="primary"
          link
          :icon="EditPen"
          @click="openAddEdit('编辑资讯分类', row, false)"
        >
          编辑
        </el-button>
        <el-button
          v-auth="'shop.home.news.category.remove'"
          type="primary"
          link
          :icon="Delete"
          @click="deleteInfo(row)"
        >
          删除
        </el-button>
      </template>
    </ProTable>
    <ShopHomeNewsCategoryForm ref="shopHomeNewsCategoryRef" />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { CirclePlus, Delete, EditPen } from '@element-plus/icons-vue';
import ProTable from '@/components/ProTable/index.vue';
import {
  createShopHomeNewsCategoryApi,
  getShopHomeNewsCategoryDetailApi,
  getShopHomeNewsCategoryListApi,
  removeShopHomeNewsCategoryApi,
  updateShopHomeNewsCategoryApi
} from '@/api/modules/shop_system/shopHomeNewsCategory';
import { useHandleData } from '@/hooks/useHandleData';
import { useDict } from '@/hooks/useDict';
import { useDictOptions } from '@/hooks/useDictOptions';
import type { ColumnProps, ProTableInstance, SearchProps } from '@/components/ProTable/interface';
import type { ShopHomeNewsCategoryQuery, ShopHomeNewsCategoryRow } from '@/api/types/shop_system/shopHomeNewsCategory';
import ShopHomeNewsCategoryForm from '@/views/shop_system/shopHomeNewsCategory/components/ShopHomeNewsCategoryForm.vue';

defineOptions({
  name: 'ShopHomeNewsCategoryView'
});

useDict(['biz_status']);
const proTableRef = ref<ProTableInstance>();

const columns: ColumnProps<ShopHomeNewsCategoryRow>[] = [
  { type: 'selection', width: 80 },
  { prop: 'categoryName', label: '分类名称' },
  { prop: 'sort', label: '排序', width: 100 },
  {
    prop: 'status',
    label: '状态',
    width: 100,
    tag: true,
    enum: useDictOptions('biz_status'),
    fieldNames: {
      label: 'codeName',
      value: 'alias',
      tagType: 'callbackShowStyle'
    }
  },
  { prop: 'remark', label: '备注' },
  { prop: 'operation', label: '操作', width: 220, fixed: 'right' }
];

const searchColumns: SearchProps[] = [
  { prop: 'categoryName', label: '分类名称', el: 'input' },
  {
    prop: 'status',
    label: '状态',
    el: 'select',
    enum: useDictOptions('biz_status'),
    fieldNames: {
      label: 'codeName',
      value: 'alias',
      tagType: 'callbackShowStyle'
    }
  }
];

const getTableList = (params: ShopHomeNewsCategoryQuery) => {
  return getShopHomeNewsCategoryListApi(params);
};

const shopHomeNewsCategoryRef = ref<InstanceType<typeof ShopHomeNewsCategoryForm>>();
const openAddEdit = async (title: string, row: any = {}, isAdd = true) => {
  if (!isAdd) {
    const record = await getShopHomeNewsCategoryDetailApi({ id: row?.id });
    row = record?.data;
  }
  const params = {
    title,
    row: { ...row },
    api: isAdd ? createShopHomeNewsCategoryApi : updateShopHomeNewsCategoryApi,
    getTableList: proTableRef.value?.getTableList
  };
  shopHomeNewsCategoryRef.value?.acceptParams(params);
};

const deleteInfo = async (params: ShopHomeNewsCategoryRow) => {
  await useHandleData(removeShopHomeNewsCategoryApi, { ids: [params.id] }, `删除【${params.categoryName}】资讯分类`);
  proTableRef.value?.getTableList();
};

const batchDelete = async (ids: (string | number)[]) => {
  await useHandleData(removeShopHomeNewsCategoryApi, { ids }, '删除所选资讯分类');
  proTableRef.value?.clearSelection();
  proTableRef.value?.getTableList();
};
</script>
