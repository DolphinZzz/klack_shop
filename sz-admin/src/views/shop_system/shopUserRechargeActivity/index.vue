<template>
  <div class="table-box">
    <ProTable
      ref="proTableRef"
      title="充值活动表"
      :indent="20"
      :columns="columns"
      :search-columns="searchColumns"
      :request-api="getTableList"
      row-key="id"
    >
      <!-- 表格 header 按钮 -->
      <template #tableHeader="scope">
        <el-button
          type="primary"
          v-auth="'shop.user.recharge.activity.create'"
          :icon="CirclePlus"
          @click="openAddEdit('新增充值活动表')"
        >
          新增
        </el-button>
        <el-button
          v-auth="'shop.user.recharge.activity.remove'"
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
          v-auth="'shop.user.recharge.activity.update'"
          type="primary"
          link
          :icon="EditPen"
          @click="openAddEdit('编辑充值活动表', row, false)"
        >
          编辑
        </el-button>
        <el-button v-auth="'shop.user.recharge.activity.remove'" type="primary" link :icon="Delete" @click="deleteInfo(row)">
          删除
        </el-button>
      </template>
    </ProTable>
    <ShopUserRechargeActivityForm ref="shopUserRechargeActivityRef" />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { CirclePlus, Delete, EditPen } from '@element-plus/icons-vue';
import ProTable from '@/components/ProTable/index.vue';
import {
  createShopUserRechargeActivityApi,
  removeShopUserRechargeActivityApi,
  updateShopUserRechargeActivityApi,
  getShopUserRechargeActivityListApi,
  getShopUserRechargeActivityDetailApi
} from '@/api/modules/shop_system/shopUserRechargeActivity';
import { useHandleData } from '@/hooks/useHandleData';
import ShopUserRechargeActivityForm from '@/views/shop_system/shopUserRechargeActivity/components/ShopUserRechargeActivityForm.vue';
import type { ColumnProps, ProTableInstance, SearchProps } from '@/components/ProTable/interface';
import type { ShopUserRechargeActivityQuery, ShopUserRechargeActivityRow } from '@/api/types/shop_system/shopUserRechargeActivity';

defineOptions({
  name: 'ShopUserRechargeActivityView'
});
const proTableRef = ref<ProTableInstance>();
// 表格配置项
const columns: ColumnProps<ShopUserRechargeActivityRow>[] = [
  { type: 'selection', width: 80 },
  { prop: 'activityName', label: '活动名称' },
  { prop: 'price', label: '充值金额' },
  { prop: 'givePrice', label: '赠送金额' },
  { prop: 'operation', label: '操作', width: 250, fixed: 'right' }
];
// 搜索条件项
const searchColumns: SearchProps[] = [{ prop: 'activityName', label: '活动名称', el: 'input' }];
// 获取table列表
const getTableList = (params: ShopUserRechargeActivityQuery) => {
  let newParams = formatParams(params);
  return getShopUserRechargeActivityListApi(newParams);
};
const formatParams = (params: ShopUserRechargeActivityQuery) => {
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
const shopUserRechargeActivityRef = ref<InstanceType<typeof ShopUserRechargeActivityForm>>();
const openAddEdit = async (title: string, row: any = {}, isAdd = true) => {
  if (!isAdd) {
    const record = await getShopUserRechargeActivityDetailApi({ id: row?.id });
    row = record?.data;
  }
  const params = {
    title,
    row: { ...row },
    api: isAdd ? createShopUserRechargeActivityApi : updateShopUserRechargeActivityApi,
    getTableList: proTableRef.value?.getTableList
  };
  shopUserRechargeActivityRef.value?.acceptParams(params);
};
// 删除信息
const deleteInfo = async (params: ShopUserRechargeActivityRow) => {
  await useHandleData(removeShopUserRechargeActivityApi, { ids: [params.id] }, `删除【${params.id}】充值活动表`);
  proTableRef.value?.getTableList();
};
// 批量删除信息
const batchDelete = async (ids: (string | number)[]) => {
  await useHandleData(removeShopUserRechargeActivityApi, { ids }, '删除所选充值活动表');
  proTableRef.value?.clearSelection();
  proTableRef.value?.getTableList();
};
</script>
