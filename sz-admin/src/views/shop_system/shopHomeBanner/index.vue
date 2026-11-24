<template>
  <div class="table-box">
    <ProTable
      ref="proTableRef"
      title="首页轮播配置"
      :indent="20"
      :columns="columns"
      :search-columns="searchColumns"
      :request-api="getTableList"
      row-key="id"
    >
      <!-- 表格 header 按钮 -->
      <template #tableHeader="scope">
        <el-button type="primary" v-auth="'shop.home.banner.create'" :icon="CirclePlus" @click="openAddEdit('新增首页轮播配置')">
          新增
        </el-button>
        <el-button
          v-auth="'shop.home.banner.remove'"
          type="danger"
          :icon="Delete"
          plain
          :disabled="!scope.isSelected"
          @click="batchDelete(scope.selectedListIds)"
        >
          批量删除
        </el-button>
      </template>
      <template #image="{ row }">
        <file-download-list :files="toImageList(row?.image)" :max-rows="1" />
      </template>
      <template #operation="{ row }">
        <el-button
          v-auth="'shop.home.banner.update'"
          type="primary"
          link
          :icon="EditPen"
          @click="openAddEdit('编辑首页轮播配置', row, false)"
        >
          编辑
        </el-button>
        <el-button v-auth="'shop.home.banner.remove'" type="primary" link :icon="Delete" @click="deleteInfo(row)"> 删除 </el-button>
      </template>
    </ProTable>
    <ShopHomeBannerForm ref="shopHomeBannerRef" />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { CirclePlus, Delete, EditPen } from '@element-plus/icons-vue';
import ProTable from '@/components/ProTable/index.vue';
import {
  createShopHomeBannerApi,
  removeShopHomeBannerApi,
  updateShopHomeBannerApi,
  getShopHomeBannerListApi,
  getShopHomeBannerDetailApi
} from '@/api/modules/shop_system/shopHomeBanner';
import { useHandleData } from '@/hooks/useHandleData';
import ShopHomeBannerForm from '@/views/shop_system/shopHomeBanner/components/ShopHomeBannerForm.vue';
import { useDict } from '@/hooks/useDict';
import { useDictOptions } from '@/hooks/useDictOptions';
import type { ColumnProps, ProTableInstance, SearchProps } from '@/components/ProTable/interface';
import type { ShopHomeBannerQuery, ShopHomeBannerRow } from '@/api/types/shop_system/shopHomeBanner';
import FileDownloadList from '@/components/Upload/FileDownloadList.vue';

defineOptions({
  name: 'ShopHomeBannerView'
});
useDict(['biz_status']);
const proTableRef = ref<ProTableInstance>();
const bannerStatusOptions = [
  { codeName: '正常', alias: '0', callbackShowStyle: 'success' },
  { codeName: '禁用', alias: '1', callbackShowStyle: 'danger' }
];
// 表格配置项
const columns: ColumnProps<ShopHomeBannerRow>[] = [
  { type: 'selection', width: 80 },
  { prop: 'title', label: '标题' },
  { prop: 'image', label: '图片' },
  { prop: 'link', label: '跳转链接' },
  { prop: 'sort', label: '排序' },
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
  { prop: 'startTime', label: '开始时间' },
  { prop: 'endTime', label: '结束时间' },
  { prop: 'remark', label: '备注' },
  { prop: 'operation', label: '操作', width: 250, fixed: 'right' }
];
// 搜索条件项
const searchColumns: SearchProps[] = [
  { prop: 'title', label: '标题', el: 'input' },
  {
    prop: 'status',
    label: '状态',
    el: 'select',
    enum: bannerStatusOptions,
    fieldNames: {
      label: 'codeName',
      value: 'alias',
      tagType: 'callbackShowStyle'
    }
  }
];
// 获取table列表
const getTableList = (params: ShopHomeBannerQuery) => {
  let newParams = formatParams(params);
  return getShopHomeBannerListApi(newParams);
};
const formatParams = (params: ShopHomeBannerQuery) => {
  let newParams = JSON.parse(JSON.stringify(params));
  if (newParams.startTime) {
    newParams.startTimeStart = newParams.startTime[0];
    newParams.startTimeEnd = newParams.startTime[1];
    delete newParams.startTime;
  }

  if (newParams.endTime) {
    newParams.endTimeStart = newParams.endTime[0];
    newParams.endTimeEnd = newParams.endTime[1];
    delete newParams.endTime;
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
const shopHomeBannerRef = ref<InstanceType<typeof ShopHomeBannerForm>>();
const openAddEdit = async (title: string, row: any = {}, isAdd = true) => {
  if (!isAdd) {
    const record = await getShopHomeBannerDetailApi({ id: row?.id });
    row = record?.data;
  }
  const params = {
    title,
    row: { ...row },
    api: isAdd ? createShopHomeBannerApi : updateShopHomeBannerApi,
    getTableList: proTableRef.value?.getTableList
  };
  shopHomeBannerRef.value?.acceptParams(params);
};
// 删除信息
const deleteInfo = async (params: ShopHomeBannerRow) => {
  await useHandleData(removeShopHomeBannerApi, { ids: [params.id] }, `删除【${params.id}】首页轮播配置`);
  proTableRef.value?.getTableList();
};
// 批量删除信息
const batchDelete = async (ids: (string | number)[]) => {
  await useHandleData(removeShopHomeBannerApi, { ids }, '删除所选首页轮播配置');
  proTableRef.value?.clearSelection();
  proTableRef.value?.getTableList();
};

const toImageList = (image?: string) => {
  if (!image) return [];
  return [image];
};
</script>
