<template>
  <div class="table-box">
    <ProTable
      ref="proTableRef"
      title="用户帐单表"
      :indent="20"
      :columns="columns"
      :search-columns="searchColumns"
      :request-api="getTableList"
      row-key="id"
    />
  </div>
</template>

<script setup lang="ts">
import { h, ref } from 'vue';
import ProTable from '@/components/ProTable/index.vue';
import { getShopUserBillListApi } from '@/api/modules/shop_system/shopUserBill';
import { useDictOptions } from '@/hooks/useDictOptions';
import type { ColumnProps, ProTableInstance, SearchProps } from '@/components/ProTable/interface';
import type { ShopUserBillQuery, ShopUserBillRow } from '@/api/types/shop_system/shopUserBill';
import { useDict } from '@/hooks/useDict';
import ShopUserSearchSelector from '@/views/shop_system/shopUserBill/components/ShopUserSearchSelector.vue';

defineOptions({
  name: 'ShopUserBillView'
});

useDict(['bill_budget']);
const proTableRef = ref<ProTableInstance>();
const billBudgetOptions = useDictOptions('bill_budget');

const columns: ColumnProps<ShopUserBillRow>[] = [
  { prop: 'id', label: 'ID', width: 90 },
  { prop: 'account', label: '用户账号', minWidth: 140 },
  { prop: 'realName', label: '用户姓名', minWidth: 120 },
  {
    prop: 'pm',
    label: '收支',
    tag: true,
    enum: billBudgetOptions,
    fieldNames: {
      label: 'codeName',
      value: 'alias',
      tagType: 'callbackShowStyle'
    }
  },
  { prop: 'title', label: '账单标题', minWidth: 160 },
  { prop: 'category', label: '明细种类', minWidth: 130 },
  { prop: 'type', label: '明细类型', minWidth: 130 },
  { prop: 'number', label: '明细数字', minWidth: 120 },
  { prop: 'balance', label: '剩余', minWidth: 120 },
  { prop: 'mark', label: '备注', minWidth: 220, showOverflowTooltip: true },
  { prop: 'createTime', label: '创建时间', minWidth: 170 },
  { prop: 'updateTime', label: '更新时间', minWidth: 170 }
];

const searchColumns: SearchProps[] = [
  {
    prop: 'uid',
    label: '用户',
    el: 'input',
    render: (params: any) => {
      return h(ShopUserSearchSelector, {
        modelValue: params.modelValue,
        'onUpdate:modelValue': params['onUpdate:modelValue'],
        placeholder: '请选择用户后查询'
      });
    }
  },
  {
    prop: 'pm',
    label: '收支',
    el: 'select',
    enum: billBudgetOptions,
    fieldNames: {
      label: 'codeName',
      value: 'alias',
      tagType: 'callbackShowStyle'
    },
    props: { clearable: true, filterable: true }
  }
];

const getTableList = (params: ShopUserBillQuery) => {
  return getShopUserBillListApi(params);
};
</script>
