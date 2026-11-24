<template>
  <div class="table-box">
    <ProTable
      ref="proTableRef"
      title="快递模板"
      :indent="20"
      :columns="columns"
      :search-columns="searchColumns"
      :request-api="getTableList"
      row-key="id"
    >
      <!-- 表格 header 按钮 -->
      <template #tableHeader="scope">
        <el-button type="primary" v-auth="'shop.express.template.create'" :icon="CirclePlus" @click="openAddEdit('新增快递模板')">
          新增
        </el-button>
        <el-button
          v-auth="'shop.express.template.remove'"
          type="danger"
          :icon="Delete"
          plain
          :disabled="!scope.isSelected"
          @click="batchDelete(scope.selectedListIds)"
        >
          批量删除
        </el-button>
        <el-button v-auth="'shop.express.template.export'" type="primary" :icon="Download" plain @click="downloadFile">
          导出
        </el-button>
      </template>
      <template #operation="{ row }">
        <el-button
          v-auth="'shop.express.template.update'"
          type="primary"
          link
          :icon="EditPen"
          @click="openAddEdit('编辑快递模板', row, false)"
        >
          编辑
        </el-button>
        <el-button v-auth="'shop.express.template.remove'" type="primary" link :icon="Delete" @click="deleteInfo(row)">
          删除
        </el-button>
      </template>
    </ProTable>
    <ShopExpressTemplateForm ref="shopExpressTemplateRef" />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { CirclePlus, Delete, EditPen, Download } from '@element-plus/icons-vue';
import ProTable from '@/components/ProTable/index.vue';
import {
  createShopExpressTemplateApi,
  removeShopExpressTemplateApi,
  updateShopExpressTemplateApi,
  getShopExpressTemplateListApi,
  getShopExpressTemplateDetailApi,
  exportShopExpressTemplateExcelApi
} from '@/api/modules/shop_system/shopExpressTemplate';
import { useHandleData } from '@/hooks/useHandleData';
import ShopExpressTemplateForm from '@/views/shop_system/shopExpressTemplate/components/ShopExpressTemplateForm.vue';
import type { ColumnProps, ProTableInstance, SearchProps } from '@/components/ProTable/interface';
import type { ShopExpressTemplateQuery, ShopExpressTemplateRow } from '@/api/types/shop_system/shopExpressTemplate';
import { useDownload } from '@/hooks/useDownload';

defineOptions({
  name: 'ShopExpressTemplateView'
});
const proTableRef = ref<ProTableInstance>();
// 表格配置项
const columns: ColumnProps<ShopExpressTemplateRow>[] = [
  { type: 'selection', width: 80 },

  { prop: 'name', label: '模板名称', minWidth: 140 },

  { prop: 'expressName', label: '快递公司', minWidth: 120 },
  { prop: 'code', label: '快递编号', width: 100 },

  { prop: 'expressPrice', label: '价格', width: 100 },

  { prop: 'sort', label: '排序', width: 80 },

  {
    prop: 'createTime',
    label: '创建时间',
    minWidth: 170,
    render: ({ row }) => row.createTime || '-'
  },
  {
    prop: 'updateTime',
    label: '更新时间',
    minWidth: 170,
    render: ({ row }) => row.updateTime || '-'
  },

  { prop: 'operation', label: '操作', width: 250, fixed: 'right' }
];

// 搜索条件项
const searchColumns: SearchProps[] = [
  { prop: 'name', label: '模板名称', el: 'input' },
  { prop: 'code', label: '快递编号', el: 'input' }
];
// 获取table列表
const getTableList = (params: ShopExpressTemplateQuery) => {
  let newParams = formatParams(params);
  return getShopExpressTemplateListApi(newParams);
};
const formatParams = (params: ShopExpressTemplateQuery) => {
  let newParams = JSON.parse(JSON.stringify(params));
  return newParams;
};
// 打开 drawer(新增、查看、编辑)
const shopExpressTemplateRef = ref<InstanceType<typeof ShopExpressTemplateForm>>();
const openAddEdit = async (title: string, row: any = {}, isAdd = true) => {
  if (!isAdd) {
    const record = await getShopExpressTemplateDetailApi({ id: row?.id });
    row = record?.data;
  }
  const params = {
    title,
    row: { ...row },
    api: isAdd ? createShopExpressTemplateApi : updateShopExpressTemplateApi,
    getTableList: proTableRef.value?.getTableList
  };
  shopExpressTemplateRef.value?.acceptParams(params);
};
// 删除信息
const deleteInfo = async (params: ShopExpressTemplateRow) => {
  await useHandleData(removeShopExpressTemplateApi, { ids: [params.id] }, `删除【${params.id}】快递模板`);
  proTableRef.value?.getTableList();
};
// 批量删除信息
const batchDelete = async (ids: (string | number)[]) => {
  await useHandleData(removeShopExpressTemplateApi, { ids }, '删除所选快递模板');
  proTableRef.value?.clearSelection();
  proTableRef.value?.getTableList();
};
// 导出
const downloadFile = async () => {
  let newParams = formatParams(proTableRef.value?.searchParam as ShopExpressTemplateQuery);
  useDownload(exportShopExpressTemplateExcelApi, '快递模板', newParams);
};
</script>
