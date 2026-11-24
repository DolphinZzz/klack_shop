<template>
  <div class="table-box">
    <ProTable
      ref="proTableRef"
      title="用户表"
      :indent="20"
      :columns="columns"
      :search-columns="searchColumns"
      :request-api="getTableList"
      row-key="uid"
    >
      <!-- 表格 header 按钮 -->
      <template #tableHeader="scope">
        <el-button type="primary" v-auth="'shop.user.create'" :icon="CirclePlus" @click="openAddEdit('新增用户表')">
          新增
        </el-button>
        <el-button
          v-auth="'shop.user.remove'"
          type="danger"
          :icon="Delete"
          plain
          :disabled="!scope.isSelected"
          @click="batchDelete(scope.selectedListIds)"
        >
          批量删除
        </el-button>
        <el-button v-auth="'shop.user.import'" type="primary" :icon="Upload" plain @click="importData"> 导入 </el-button>
        <el-button v-auth="'shop.user.export'" type="primary" :icon="Download" plain @click="downloadFile"> 导出 </el-button>
      </template>
      <template #operation="{ row }">
        <el-button v-auth="'shop.user.update'" type="primary" link :icon="EditPen" @click="openAddEdit('编辑用户表', row, false)">
          编辑
        </el-button>
        <el-button v-auth="'shop.user.remove'" type="primary" link :icon="Delete" @click="deleteInfo(row)"> 删除 </el-button>
      </template>
    </ProTable>
    <ShopUserForm ref="shopUserRef" />
    <ImportExcel ref="ImportExcelRef" />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { CirclePlus, Delete, EditPen, Upload, Download } from '@element-plus/icons-vue';
import ProTable from '@/components/ProTable/index.vue';
import {
  createShopUserApi,
  removeShopUserApi,
  updateShopUserApi,
  getShopUserListApi,
  getShopUserDetailApi,
  importShopUserExcelApi,
  exportShopUserExcelApi
} from '@/api/modules/shop_system/shopUser';
import { useHandleData } from '@/hooks/useHandleData';
import ShopUserForm from '@/views/shop_system/shopUser/components/ShopUserForm.vue';
import { useDictOptions } from '@/hooks/useDictOptions';
import type { ColumnProps, ProTableInstance, SearchProps } from '@/components/ProTable/interface';
import type { ShopUserQuery, ShopUserListRow } from '@/api/types/shop_system/shopUser';
import ImportExcel from '@/components/ImportExcel/index.vue';
import { downloadTemplate } from '@/api/modules/system/common';
import { useDownload } from '@/hooks/useDownload';
import { useDict } from '@/hooks/useDict';

defineOptions({
  name: 'ShopUserView'
});
// 使用useDict Hook 主动加载字典
useDict(['user_sex', 'biz_status', 'user_type']);
const proTableRef = ref<ProTableInstance>();
// 表格配置项
const columns: ColumnProps<ShopUserListRow>[] = [
  { type: 'selection', width: 80 },
  { prop: 'account', label: '用户账号' },
  { prop: 'nickname', label: '用户昵称' },
  { prop: 'phone', label: '手机号' },
  { prop: 'nowMoney', label: '用户余额' },
  { prop: 'integral', label: '用户积分' },
  { prop: 'level', label: '等级' },
  {
    prop: 'userType',
    label: '用户类型',
    tag: true,
    enum: useDictOptions('user_type'),
    fieldNames: {
      label: 'codeName',
      value: 'alias',
      tagType: 'callbackShowStyle'
    }
  },
  {
    prop: 'status',
    label: '状态',
    tag: true,
    enum: useDictOptions('biz_status'),
    fieldNames: {
      label: 'codeName',
      value: 'alias',
      tagType: 'callbackShowStyle'
    }
  },
  { prop: 'operation', label: '操作', width: 250, fixed: 'right' }
];
// 搜索条件项
const searchColumns: SearchProps[] = [
  { prop: 'account', label: '用户账号', el: 'input' },
  { prop: 'realName', label: '真实姓名', el: 'input' },
  {
    prop: 'sex',
    label: '性别',
    el: 'select',
    enum: useDictOptions('user_sex'),
    fieldNames: {
      label: 'codeName',
      value: 'alias',
      tagType: 'callbackShowStyle'
    }
  },
  { prop: 'cardId', label: '身份证', el: 'input' },
  { prop: 'nickname', label: '用户昵称', el: 'input' },
  { prop: 'phone', label: '手机号', el: 'input' },
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
// 获取table列表
const getTableList = (params: ShopUserQuery) => {
  let newParams = formatParams(params);
  return getShopUserListApi(newParams);
};
const formatParams = (params: ShopUserQuery) => {
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
const shopUserRef = ref<InstanceType<typeof ShopUserForm>>();
const openAddEdit = async (title: string, row: any = {}, isAdd = true) => {
  if (!isAdd) {
    const record = await getShopUserDetailApi({ id: row?.uid });
    row = record?.data;
  }
  const params = {
    title,
    row: { ...row },
    api: isAdd ? createShopUserApi : updateShopUserApi,
    getTableList: proTableRef.value?.getTableList
  };
  shopUserRef.value?.acceptParams(params);
};
// 删除信息
const deleteInfo = async (params: ShopUserListRow) => {
  await useHandleData(removeShopUserApi, { ids: [params.uid] }, `删除【${params.uid}】用户表`);
  proTableRef.value?.getTableList();
};
// 批量删除信息
const batchDelete = async (ids: (string | number)[]) => {
  await useHandleData(removeShopUserApi, { ids: ids }, '删除所选用户表');
  proTableRef.value?.clearSelection();
  proTableRef.value?.getTableList();
};
// 导入
const ImportExcelRef = ref<InstanceType<typeof ImportExcel>>();
const importData = () => {
  const params = {
    title: '用户表',
    tempApi: downloadTemplate,
    importApi: importShopUserExcelApi,
    getTableList: proTableRef.value?.getTableList,
    alias: '【标识字符】：例 jstj, 对应 [系统管理-模版文件管理] 中的标识',
    fileName: '【模板文件名称】：例 教师统计模板.xlsx, 对应 [系统管理-模版文件管理] 中的模版名'
  };
  ImportExcelRef.value?.acceptParams(params);
};
// 导出
const downloadFile = async () => {
  let newParams = formatParams(proTableRef.value?.searchParam as ShopUserQuery);
  useDownload(exportShopUserExcelApi, '用户表', newParams);
};
</script>
