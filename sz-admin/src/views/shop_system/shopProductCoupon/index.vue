<template>
  <div class="table-box">
    <ProTable
      ref="proTableRef"
      title="商品优惠券"
      :indent="20"
      :columns="columns"
      :search-columns="searchColumns"
      :request-api="getTableList"
      row-key="id"
    >
      <!-- 表格 header 按钮 -->
      <template #tableHeader="scope">
        <el-button type="primary" v-auth="'lb.product.coupon.create'" :icon="CirclePlus" @click="openAddEdit('新增商品优惠券')">
          新增
        </el-button>
        <el-button
          v-auth="'lb.product.coupon.remove'"
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
          v-auth="'lb.product.coupon.update'"
          type="primary"
          link
          :icon="EditPen"
          @click="openAddEdit('编辑商品优惠券', row, false)"
        >
          编辑
        </el-button>
        <el-button v-auth="'lb.product.coupon.remove'" type="primary" link :icon="Delete" @click="deleteInfo(row)">
          删除
        </el-button>
      </template>
    </ProTable>
    <ShopProductCouponForm ref="shopProductCouponRef" />
  </div>
</template>

<script setup lang="ts">
import { h, ref } from 'vue';
import { CirclePlus, Delete, EditPen } from '@element-plus/icons-vue';
import { ElTag } from 'element-plus';
import ProTable from '@/components/ProTable/index.vue';
import {
  createShopProductCouponApi,
  removeShopProductCouponApi,
  updateShopProductCouponApi,
  getShopProductCouponListApi,
  getShopProductCouponDetailApi
} from '@/api/modules/shop_system/shopProductCoupon';
import { useHandleData } from '@/hooks/useHandleData';
import ShopProductCouponForm from '@/views/shop_system/shopProductCoupon/components/ShopProductCouponForm.vue';
import { useDictOptions } from '@/hooks/useDictOptions';
import type { ColumnProps, ProTableInstance, SearchProps } from '@/components/ProTable/interface';
import type { ShopProductCouponQuery, ShopProductCouponRow } from '@/api/types/shop_system/shopProductCoupon';
import { useDict } from '@/hooks/useDict';

defineOptions({
  name: 'ShopProductCouponView'
});
// 使用useDict Hook 主动加载字典
useDict(['coupon_type']);
const proTableRef = ref<ProTableInstance>();
const formatMoney = (value: any) => {
  if (value === null || value === undefined || value === '') return '-';
  return `￥${value}`;
};

const formatDays = (value: any) => {
  if (value === null || value === undefined || value === '') return '-';
  const num = Number(value);
  return Number.isNaN(num) ? '-' : `${num}天`;
};
// 表格配置项
const columns: ColumnProps<ShopProductCouponRow>[] = [
  { type: 'selection', width: 80 },
  { prop: 'name', label: '优惠券名称' },
  {
    prop: 'money',
    label: '优惠券面值',
    render: ({ row }) => formatMoney(row?.money)
  },
  { prop: 'total', label: '发放总数' },
  { prop: 'lastTotal', label: '剩余数量' },
  {
    prop: 'type',
    label: '优惠券类型',
    tag: true,
    enum: useDictOptions('coupon_type'),
    fieldNames: {
      label: 'codeName',
      value: 'alias',
      tagType: 'callbackShowStyle'
    }
  },
  {
    prop: 'productNames',
    label: '所属商品',
    render: ({ row }) => {
      const names = String(row?.productNames || '')
        .split(',')
        .map(item => item.trim())
        .filter(Boolean);

      if (!names.length) return '-';

      return h(
        'div',
        { class: 'product-name-tags' },
        names.map(name => h(ElTag, { type: 'info', effect: 'light', size: 'small' }, () => name))
      );
    }
  },
  {
    prop: 'couponStartDays',
    label: '领取开始时间',
    render: ({ row }) => formatDays(row?.couponStartDays)
  },
  {
    prop: 'couponEndDays',
    label: '领取开结束时间',
    render: ({ row }) => formatDays(row?.couponEndDays)
  },
  { prop: 'consumeTimeRange', label: '消费时间范围' },
  {
    prop: 'useStartDays',
    label: '使用开始时间',
    render: ({ row }) => formatDays(row?.useStartDays)
  },
  {
    prop: 'useEndDays',
    label: '使用结束时间',
    render: ({ row }) => formatDays(row?.useEndDays)
  },
  { prop: 'useRangeTime', label: '使用时间范围' },
  { prop: 'operation', label: '操作', width: 250, fixed: 'right' }
];
// 搜索条件项
const searchColumns: SearchProps[] = [
  { prop: 'name', label: '优惠券名称', el: 'input' },
  {
    prop: 'type',
    label: '优惠券类型',
    el: 'select',
    enum: useDictOptions('coupon_type'),
    fieldNames: {
      label: 'codeName',
      value: 'id',
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
      value: 'id',
      tagType: 'callbackShowStyle'
    }
  }
];
// 获取table列表
const getTableList = (params: ShopProductCouponQuery) => {
  let newParams = formatParams(params);
  return getShopProductCouponListApi(newParams);
};
const formatParams = (params: ShopProductCouponQuery) => {
  let newParams = JSON.parse(JSON.stringify(params));
  if (newParams.couponStartTime) {
    newParams.couponStartTimeStart = newParams.couponStartTime[0];
    newParams.couponStartTimeEnd = newParams.couponStartTime[1];
    delete newParams.couponStartTime;
  }

  if (newParams.couponEndTime) {
    newParams.couponEndTimeStart = newParams.couponEndTime[0];
    newParams.couponEndTimeEnd = newParams.couponEndTime[1];
    delete newParams.couponEndTime;
  }

  if (newParams.useStartTime) {
    newParams.useStartTimeStart = newParams.useStartTime[0];
    newParams.useStartTimeEnd = newParams.useStartTime[1];
    delete newParams.useStartTime;
  }

  if (newParams.useEndTime) {
    newParams.useEndTimeStart = newParams.useEndTime[0];
    newParams.useEndTimeEnd = newParams.useEndTime[1];
    delete newParams.useEndTime;
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
const shopProductCouponRef = ref<InstanceType<typeof ShopProductCouponForm>>();
const openAddEdit = async (title: string, row: any = {}, isAdd = true) => {
  if (!isAdd) {
    const record = await getShopProductCouponDetailApi({ id: row?.id });
    row = record?.data;
  }
  const params = {
    title,
    row: { ...row },
    api: isAdd ? createShopProductCouponApi : updateShopProductCouponApi,
    getTableList: proTableRef.value?.getTableList
  };
  shopProductCouponRef.value?.acceptParams(params);
};
// 删除信息
const deleteInfo = async (params: ShopProductCouponRow) => {
  await useHandleData(removeShopProductCouponApi, { ids: [params.id] }, `删除【${params.id}】商品优惠券`);
  proTableRef.value?.getTableList();
};
// 批量删除信息
const batchDelete = async (ids: (string | number)[]) => {
  await useHandleData(removeShopProductCouponApi, { ids }, '删除所选商品优惠券');
  proTableRef.value?.clearSelection();
  proTableRef.value?.getTableList();
};
</script>

<style scoped>
.product-name-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
</style>
