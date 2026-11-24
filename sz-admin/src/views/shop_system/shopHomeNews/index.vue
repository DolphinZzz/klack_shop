<template>
  <div class="table-box">
    <ProTable
      ref="proTableRef"
      title="首页资讯管理"
      :indent="20"
      :columns="columns"
      :search-columns="searchColumns"
      :request-api="getTableList"
      row-key="id"
    >
      <template #tableHeader="scope">
        <el-button type="primary" v-auth="'lb.home.news.create'" :icon="CirclePlus" @click="openAddEdit('新增首页资讯')">
          新增
        </el-button>
        <el-button
          v-auth="'lb.home.news.remove'"
          type="danger"
          :icon="Delete"
          plain
          :disabled="!scope.isSelected"
          @click="batchDelete(scope.selectedListIds)"
        >
          批量删除
        </el-button>
      </template>
      <template #coverImage="{ row }">
        <file-download-list :files="toImageList(row?.coverImage)" :max-rows="1" />
      </template>
      <template #operation="{ row }">
        <el-button
          v-auth="'lb.home.news.update'"
          type="primary"
          link
          :icon="EditPen"
          @click="openAddEdit('编辑首页资讯', row, false)"
        >
          编辑
        </el-button>
        <el-button v-auth="'lb.home.news.remove'" type="primary" link :icon="Delete" @click="deleteInfo(row)"> 删除 </el-button>
      </template>
    </ProTable>
    <ShopHomeNewsForm ref="shopHomeNewsRef" />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { CirclePlus, Delete, EditPen } from '@element-plus/icons-vue';
import ProTable from '@/components/ProTable/index.vue';
import {
  createShopHomeNewsApi,
  getShopHomeNewsDetailApi,
  getShopHomeNewsListApi,
  removeShopHomeNewsApi,
  updateShopHomeNewsApi
} from '@/api/modules/shop_system/shopHomeNews';
import { useHandleData } from '@/hooks/useHandleData';
import ShopHomeNewsForm from '@/views/shop_system/shopHomeNews/components/ShopHomeNewsForm.vue';
import { useDict } from '@/hooks/useDict';
import { useDictOptions } from '@/hooks/useDictOptions';
import type { ColumnProps, ProTableInstance, SearchProps } from '@/components/ProTable/interface';
import type { ShopHomeNewsQuery, ShopHomeNewsRow } from '@/api/types/shop_system/shopHomeNews';
import FileDownloadList from '@/components/Upload/FileDownloadList.vue';
import { getShopHomeNewsCategoryAllApi } from '@/api/modules/shop_system/shopHomeNewsCategory';

defineOptions({
  name: 'ShopHomeNewsView'
});

useDict(['biz_status']);
const proTableRef = ref<ProTableInstance>();
const categoryOptions = ref<{ label: string; value: number }[]>([]);
const topOptions = [
  { codeName: '普通', alias: '0', callbackShowStyle: 'info' },
  { codeName: '置顶', alias: '1', callbackShowStyle: 'danger' }
];

const columns: ColumnProps<ShopHomeNewsRow>[] = [
  { type: 'selection', width: 80 },
  { prop: 'title', label: '标题', minWidth: 220 },
  {
    prop: 'categoryId',
    label: '分类',
    width: 120,
    enum: categoryOptions,
    fieldNames: {
      label: 'label',
      value: 'value'
    }
  },
  { prop: 'tagName', label: '标签', width: 100 },
  { prop: 'coverImage', label: '封面图', width: 180 },
  { prop: 'publishTime', label: '发布时间', width: 180 },
  { prop: 'source', label: '来源', width: 120 },
  { prop: 'author', label: '作者', width: 100 },
  { prop: 'browseCount', label: '浏览量', width: 100 },
  {
    prop: 'isTop',
    label: '置顶',
    width: 90,
    tag: true,
    enum: topOptions,
    fieldNames: {
      label: 'codeName',
      value: 'alias',
      tagType: 'callbackShowStyle'
    }
  },
  { prop: 'sort', label: '排序', width: 90 },
  {
    prop: 'status',
    label: '状态',
    width: 90,
    tag: true,
    enum: useDictOptions('biz_status'),
    fieldNames: {
      label: 'codeName',
      value: 'alias',
      tagType: 'callbackShowStyle'
    }
  },
  { prop: 'remark', label: '备注', minWidth: 160 },
  { prop: 'operation', label: '操作', width: 220, fixed: 'right' }
];

const searchColumns: SearchProps[] = [
  { prop: 'title', label: '标题', el: 'input' },
  {
    prop: 'categoryId',
    label: '分类',
    el: 'select',
    enum: categoryOptions,
    fieldNames: {
      label: 'label',
      value: 'value'
    }
  },
  {
    prop: 'isTop',
    label: '置顶',
    el: 'select',
    enum: topOptions,
    fieldNames: {
      label: 'codeName',
      value: 'alias',
      tagType: 'callbackShowStyle'
    }
  },
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

const getTableList = (params: ShopHomeNewsQuery) => {
  return getShopHomeNewsListApi(params);
};

const loadCategoryOptions = async () => {
  const res = await getShopHomeNewsCategoryAllApi();
  categoryOptions.value = (res.data || []).map(item => ({
    label: item.categoryName || '',
    value: Number(item.id)
  }));
};
void loadCategoryOptions();

const shopHomeNewsRef = ref<InstanceType<typeof ShopHomeNewsForm>>();
const openAddEdit = async (title: string, row: any = {}, isAdd = true) => {
  if (!isAdd) {
    const record = await getShopHomeNewsDetailApi({ id: row?.id });
    row = record?.data;
  }
  const params = {
    title,
    row: { ...row },
    api: isAdd ? createShopHomeNewsApi : updateShopHomeNewsApi,
    getTableList: proTableRef.value?.getTableList
  };
  shopHomeNewsRef.value?.acceptParams(params);
};

const deleteInfo = async (params: ShopHomeNewsRow) => {
  await useHandleData(removeShopHomeNewsApi, { ids: [params.id] }, `删除【${params.id}】首页资讯`);
  proTableRef.value?.getTableList();
};

const batchDelete = async (ids: (string | number)[]) => {
  await useHandleData(removeShopHomeNewsApi, { ids }, '删除所选首页资讯');
  proTableRef.value?.clearSelection();
  proTableRef.value?.getTableList();
};

const toImageList = (image?: string) => {
  if (!image) return [];
  return [image];
};
</script>
