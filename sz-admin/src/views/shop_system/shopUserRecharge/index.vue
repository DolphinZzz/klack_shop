<template>
  <div class="table-box">
    <ProTable
      ref="proTableRef"
      title="用户充值表"
      :indent="20"
      :columns="columns"
      :search-columns="searchColumns"
      :request-api="getTableList"
      row-key="id"
    >
      <!-- 表格 header 按钮 -->
      <template #tableHeader="scope">
        <el-button type="primary" v-auth="'lb.user.recharge.create'" :icon="CirclePlus" @click="openAddEdit('新增用户充值表')">
          新增
        </el-button>
        <el-button
          v-auth="'lb.user.recharge.remove'"
          type="danger"
          :icon="Delete"
          plain
          :disabled="!scope.isSelected"
          @click="batchDelete(scope.selectedListIds)"
        >
          批量删除
        </el-button>
        <el-button v-auth="'lb.user.recharge.export'" type="primary" :icon="Download" plain @click="downloadFile">
          导出
        </el-button>
      </template>
      <template #operation="{ row }">
        <el-button
          v-auth="'lb.user.recharge.update'"
          type="primary"
          link
          :icon="EditPen"
          @click="openAddEdit('编辑用户充值表', row, false)"
        >
          编辑
        </el-button>
        <el-button v-auth="'lb.user.recharge.remove'" type="primary" link :icon="Delete" @click="deleteInfo(row)">
          删除
        </el-button>
      </template>
    </ProTable>
    <ShopUserRechargeForm ref="shopUserRechargeRef" />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { CirclePlus, Delete, EditPen, Download } from '@element-plus/icons-vue';
import ProTable from '@/components/ProTable/index.vue';
import {
  createShopUserRechargeApi,
  removeShopUserRechargeApi,
  updateShopUserRechargeApi,
  getShopUserRechargeListApi,
  getShopUserRechargeDetailApi,
  exportShopUserRechargeExcelApi
} from '@/api/modules/shop_system/shopUserRecharge';
import { useHandleData } from '@/hooks/useHandleData';
import ShopUserRechargeForm from '@/views/shop_system/shopUserRecharge/components/ShopUserRechargeForm.vue';
import { useDictOptions } from '@/hooks/useDictOptions';
import type { ColumnProps, ProTableInstance, SearchProps } from '@/components/ProTable/interface';
import type { ShopUserRechargeQuery, ShopUserRechargeRow } from '@/api/types/shop_system/shopUserRecharge';
import { useDownload } from '@/hooks/useDownload';
import { useDict } from '@/hooks/useDict';

defineOptions({
  name: 'ShopUserRechargeView'
});
// 使用useDict Hook 主动加载字典
useDict(['recharge_type']);
const proTableRef = ref<ProTableInstance>();
// 表格配置项
const columns: ColumnProps<ShopUserRechargeRow>[] = [
  { type: 'selection', width: 80 },
  { prop: 'uid', label: '充值用户id' },
  { prop: 'payActivityId', label: '充值活动id' },
  { prop: 'orderId', label: '充值订单id' },
  { prop: 'price', label: '充值金额' },
  { prop: 'givePrice', label: '赠送金额' },
  {
    prop: 'rechargeType',
    label: '充值类型',
    tag: true,
    enum: useDictOptions('recharge_type'),
    fieldNames: {
      label: 'codeName',
      value: 'id',
      tagType: 'callbackShowStyle'
    }
  },
  { prop: 'payTime', label: '充值时间' },
  { prop: 'refundPrice', label: '退款金额' },
  { prop: 'operation', label: '操作', width: 250, fixed: 'right' }
];
// 搜索条件项
const searchColumns: SearchProps[] = [
  { prop: 'uid', label: '充值用户id', el: 'input' },
  { prop: 'payActivityId', label: '充值活动id', el: 'input' },
  { prop: 'orderId', label: '充值订单id', el: 'input' },
  { prop: 'price', label: '充值金额', el: 'input' },
  { prop: 'givePrice', label: '赠送金额', el: 'input' },
  {
    prop: 'rechargeType',
    label: '充值类型',
    el: 'select',
    enum: useDictOptions('recharge_type'),
    fieldNames: {
      label: 'codeName',
      value: 'id',
      tagType: 'callbackShowStyle'
    }
  },
  {
    prop: 'payTime',
    label: '充值时间',
    el: 'date-picker',
    span: 2,
    props: {
      type: 'datetimerange',
      valueFormat: 'YYYY-MM-DD HH:mm:ss'
    }
  },
  { prop: 'refundPrice', label: '退款金额', el: 'input' },
  { prop: 'isDel', label: '是否删除 0-否 1-是', el: 'input' }
];
// 获取table列表
const getTableList = (params: ShopUserRechargeQuery) => {
  let newParams = formatParams(params);
  return getShopUserRechargeListApi(newParams);
};
const formatParams = (params: ShopUserRechargeQuery) => {
  let newParams = JSON.parse(JSON.stringify(params));
  if (newParams.payTime) {
    newParams.payTimeStart = newParams.payTime[0];
    newParams.payTimeEnd = newParams.payTime[1];
    delete newParams.payTime;
  }

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
const shopUserRechargeRef = ref<InstanceType<typeof ShopUserRechargeForm>>();
const openAddEdit = async (title: string, row: any = {}, isAdd = true) => {
  if (!isAdd) {
    const record = await getShopUserRechargeDetailApi({ id: row?.id });
    row = record?.data;
  }
  const params = {
    title,
    row: { ...row },
    api: isAdd ? createShopUserRechargeApi : updateShopUserRechargeApi,
    getTableList: proTableRef.value?.getTableList
  };
  shopUserRechargeRef.value?.acceptParams(params);
};
// 删除信息
const deleteInfo = async (params: ShopUserRechargeRow) => {
  await useHandleData(removeShopUserRechargeApi, { ids: [params.id] }, `删除【${params.id}】用户充值表`);
  proTableRef.value?.getTableList();
};
// 批量删除信息
const batchDelete = async (ids: (string | number)[]) => {
  await useHandleData(removeShopUserRechargeApi, { ids }, '删除所选用户充值表');
  proTableRef.value?.clearSelection();
  proTableRef.value?.getTableList();
};
// 导出
const downloadFile = async () => {
  let newParams = formatParams(proTableRef.value?.searchParam as ShopUserRechargeQuery);
  useDownload(exportShopUserRechargeExcelApi, '用户充值表', newParams);
};
</script>
