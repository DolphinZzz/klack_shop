<template>
  <div class="table-box">
    <ProTable
      ref="proTableRef"
      title="分类管理"
      :indent="20"
      :columns="columns"
      :search-columns="searchColumns"
      :request-api="getTableList"
      row-key="id"
    >
      <!-- 表格 header 按钮 -->
      <template #tableHeader="scope">
        <el-button type="primary" v-auth="'lb.category.create'" :icon="CirclePlus" @click="openAddEdit('新增分类管理')">
          新增
        </el-button>
        <el-button
          v-auth="'lb.category.remove'"
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
          v-auth="'lb.category.update'"
          type="primary"
          link
          :icon="EditPen"
          @click="openAddEdit('编辑分类管理', row, false)"
        >
          编辑
        </el-button>
        <el-button v-auth="'lb.category.remove'" type="primary" link :icon="Delete" @click="deleteInfo(row)"> 删除 </el-button>
      </template>
    </ProTable>
    <ShopCategoryForm ref="shopCategoryRef" />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { CirclePlus, Delete, EditPen } from '@element-plus/icons-vue';
import ProTable from '@/components/ProTable/index.vue';
import {
  createShopCategoryApi,
  removeShopCategoryApi,
  updateShopCategoryApi,
  getShopCategoryListApi,
  getShopCategoryDetailApi
} from '@/api/modules/shop_system/shopCategory';
import { useHandleData } from '@/hooks/useHandleData';
import ShopCategoryForm from '@/views/shop_system/shopCategory/components/ShopCategoryForm.vue';
import type { ColumnProps, ProTableInstance, SearchProps } from '@/components/ProTable/interface';
import type { ShopCategoryQuery, ShopCategoryRow } from '@/api/types/shop_system/shopCategory';

defineOptions({
  name: 'ShopCategoryView'
});
const proTableRef = ref<ProTableInstance>();
// 表格配置项
const columns: ColumnProps<ShopCategoryRow>[] = [
  { type: 'selection', width: 80 },
  { prop: 'cateName', label: '分类名称' },
  { prop: 'sort', label: '排序' },
  { prop: 'operation', label: '操作', width: 250, fixed: 'right' }
];
// 搜索条件项
const searchColumns: SearchProps[] = [{ prop: 'cateName', label: '分类名称', el: 'input' }];
// 获取table列表
const getTableList = (params: ShopCategoryQuery) => {
  let newParams = formatParams(params);
  return getShopCategoryListApi(newParams);
};
const formatParams = (params: ShopCategoryQuery) => {
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
// 打开 drawer(新增、查看、编辑)
const shopCategoryRef = ref<InstanceType<typeof ShopCategoryForm>>();
const openAddEdit = async (title: string, row: any = {}, isAdd = true) => {
  if (!isAdd) {
    const record = await getShopCategoryDetailApi({ id: row?.id });
    row = record?.data;
    console.log('编辑时的数据:', row); // 调试日志
  }
  const params = {
    title,
    row: { ...row },
    api: isAdd ? createShopCategoryApi : updateShopCategoryApi,
    getTableList: proTableRef.value?.getTableList
  };
  shopCategoryRef.value?.acceptParams(params);
};
// 删除信息
const deleteInfo = async (params: ShopCategoryRow) => {
  await useHandleData(removeShopCategoryApi, { ids: [params.id] }, `删除【${params.cateName}】分类管理`);
  proTableRef.value?.getTableList();
};
// 批量删除信息
const batchDelete = async (ids: (string | number)[]) => {
  try {
    const selectedList = (proTableRef.value?.selectedList as ShopCategoryRow[]) || [];
    const cateNames = selectedList.map(row => row.cateName).join('、');
    const message = cateNames ? `删除【${cateNames}】分类管理` : '删除所选分类管理';
    await useHandleData(removeShopCategoryApi, { ids }, message);
    proTableRef.value?.clearSelection();
    proTableRef.value?.getTableList();
  } catch (error) {
    console.error('批量删除出错:', error);
    throw error;
  }
};
</script>
