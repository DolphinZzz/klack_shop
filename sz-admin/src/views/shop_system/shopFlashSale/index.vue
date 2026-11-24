<template>
  <div class="table-box">
    <ProTable
      ref="proTableRef"
      title="营销管理"
      :indent="20"
      :columns="columns"
      :search-columns="searchColumns"
      :request-api="getTableList"
      row-key="productId"
    >
      <template #image="{ row }">
        <el-image
          v-if="row?.image"
          :src="row.image"
          :preview-src-list="[row.image]"
          fit="cover"
          style="width: 60px; height: 60px; border-radius: 8px"
          preview-teleported
        />
        <el-tag v-else type="info" size="small">暂无图片</el-tag>
      </template>

      <template #productShowStatus="{ row }">
        <el-tag :type="Number(row?.productShowStatus) === 1 ? 'success' : 'info'">
          {{ Number(row?.productShowStatus) === 1 ? '已上架' : '已下架' }}
        </el-tag>
      </template>

      <template #isSeckill="{ row }">
        <el-tag :type="Number(row?.isSeckill) === 1 ? 'danger' : 'info'">
          {{ Number(row?.isSeckill) === 1 ? '是' : '否' }}
        </el-tag>
      </template>

      <template #flashStatus="{ row }">
        <el-tag :type="Number(row?.flashStatus) === 1 ? 'warning' : 'info'">
          {{ Number(row?.flashStatus) === 1 ? '上架中' : '已下架' }}
        </el-tag>
      </template>

      <template #flashActive="{ row }">
        <el-tag :type="Number(row?.flashActive) === 1 ? 'success' : 'info'">
          {{ Number(row?.flashActive) === 1 ? '进行中' : '未进行' }}
        </el-tag>
      </template>

      <template #operation="{ row }">
        <el-button v-auth="'shop.flash.sale.update'" type="primary" link :icon="EditPen" @click="openEdit(row)">编辑营销</el-button>
      </template>
    </ProTable>

    <el-dialog v-model="dialogVisible" title="编辑营销信息" width="920px" destroy-on-close>
      <div class="dialog-body" v-loading="detailLoading">
        <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
          <el-form-item label="商品名称">
            <el-input :model-value="currentProductName" disabled />
          </el-form-item>

          <div class="form-grid">
            <el-form-item label="浏览量" prop="browse">
              <el-input-number v-model="form.browse" :min="0" :step="100" controls-position="right" class="w-full" />
            </el-form-item>
            <el-form-item label="虚拟浏览量" prop="fictiBrowse">
              <el-input-number v-model="form.fictiBrowse" :min="0" :step="100" controls-position="right" class="w-full" />
            </el-form-item>
            <el-form-item label="销量" prop="sales">
              <el-input-number v-model="form.sales" :min="0" :step="10" controls-position="right" class="w-full" />
            </el-form-item>
            <el-form-item label="商品状态" prop="productShowStatus">
              <el-radio-group v-model="form.productShowStatus">
                <el-radio :label="1">上架</el-radio>
                <el-radio :label="0">下架</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="秒杀商品" prop="isSeckill">
              <el-radio-group v-model="form.isSeckill">
                <el-radio :label="1">是</el-radio>
                <el-radio :label="0">否</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="首页活动标记" prop="activity">
              <el-radio-group v-model="form.activity">
                <el-radio label="1">限时秒杀</el-radio>
                <el-radio label="0">普通商品</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="秒杀状态" prop="flashStatus">
              <el-radio-group v-model="form.flashStatus">
                <el-radio :label="1">上架</el-radio>
                <el-radio :label="0">下架</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="默认秒杀价" prop="flashPrice">
              <el-input-number v-model="form.flashPrice" :min="0.01" :precision="2" :step="1" controls-position="right" class="w-full" />
            </el-form-item>
            <el-form-item label="限购数量" prop="limitPerUser">
              <el-input-number v-model="form.limitPerUser" :min="1" :step="1" controls-position="right" class="w-full" />
            </el-form-item>
            <el-form-item label="保留秒数" prop="reserveSeconds">
              <el-input-number v-model="form.reserveSeconds" :min="60" :step="60" controls-position="right" class="w-full" />
            </el-form-item>
          </div>

          <el-form-item label="秒杀时间" prop="timeRange">
            <el-date-picker
              v-model="timeRange"
              type="datetimerange"
              range-separator="至"
              start-placeholder="开始时间"
              end-placeholder="结束时间"
              value-format="YYYY-MM-DD HH:mm:ss"
              class="w-full"
            />
          </el-form-item>

          <el-divider content-position="left">SKU 秒杀价</el-divider>
          <div class="sku-tips">默认秒杀价用于首页与未覆盖规格，下面可以按 SKU 单独设置秒杀价。</div>

          <el-table :data="skuList" border class="sku-table" empty-text="当前商品暂无 SKU 规格">
            <el-table-column label="规格" min-width="260">
              <template #default="{ row }">
                <div class="sku-name">
                  <span>{{ formatSkuAttrValue(row.attrValue, row.sku) }}</span>
                  <el-tag v-if="isSkuDefault(row)" size="small" type="success">默认</el-tag>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="库存" prop="stock" width="100" />
            <el-table-column label="原价" width="120">
              <template #default="{ row }">￥{{ formatMoney(row.price) }}</template>
            </el-table-column>
            <el-table-column label="秒杀价" min-width="180">
              <template #default="{ row }">
                <el-input-number v-model="row.flashPrice" :min="0.01" :precision="2" :step="1" controls-position="right" class="w-full" />
              </template>
            </el-table-column>
          </el-table>
        </el-form>
      </div>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { EditPen } from '@element-plus/icons-vue';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import ProTable from '@/components/ProTable/index.vue';
import { getShopFlashSaleDetailApi, getShopFlashSaleListApi, updateShopFlashSaleApi } from '@/api/modules/shop_system/shopFlashSale';
import { getShopProductListApi } from '@/api/modules/shop_system/shopProduct';
import type { ColumnProps, ProTableInstance, SearchProps } from '@/components/ProTable/interface';
import type { ShopFlashSaleDetailRow, ShopFlashSaleQuery, ShopFlashSaleRow, ShopFlashSaleSkuRow, ShopFlashSaleUpdateForm } from '@/api/types/shop_system/shopFlashSale';
import { useDict } from '@/hooks/useDict';

defineOptions({
  name: 'ShopFlashSaleView'
});

useDict(['is_show', 'is_seckill']);

const proTableRef = ref<ProTableInstance>();
const formRef = ref<FormInstance>();
const dialogVisible = ref(false);
const submitting = ref(false);
const detailLoading = ref(false);
const currentProductName = ref('');
const skuList = ref<ShopFlashSaleSkuRow[]>([]);

const form = reactive<ShopFlashSaleUpdateForm>({
  id: undefined,
  productId: undefined,
  browse: 0,
  fictiBrowse: 0,
  sales: 0,
  productShowStatus: 1,
  isSeckill: 1,
  activity: '1',
  flashStatus: 1,
  flashPrice: 0.01,
  limitPerUser: 1,
  reserveSeconds: 900,
  startTime: '',
  endTime: '',
  skuList: []
});

const timeRange = computed({
  get: () => {
    if (form.startTime && form.endTime) {
      return [form.startTime, form.endTime];
    }
    return [];
  },
  set: value => {
    const [startTime = '', endTime = ''] = (value || []) as string[];
    form.startTime = startTime;
    form.endTime = endTime;
  }
});

const rules: FormRules = {
  browse: [{ required: true, message: '请输入浏览量', trigger: 'blur' }],
  fictiBrowse: [{ required: true, message: '请输入虚拟浏览量', trigger: 'blur' }],
  sales: [{ required: true, message: '请输入销量', trigger: 'blur' }],
  productShowStatus: [{ required: true, message: '请选择商品状态', trigger: 'change' }],
  isSeckill: [{ required: true, message: '请选择是否秒杀商品', trigger: 'change' }],
  activity: [{ required: true, message: '请选择首页活动标记', trigger: 'change' }],
  flashStatus: [{ required: true, message: '请选择秒杀状态', trigger: 'change' }],
  flashPrice: [{ required: true, message: '请输入默认秒杀价', trigger: 'blur' }],
  limitPerUser: [{ required: true, message: '请输入限购数量', trigger: 'blur' }],
  reserveSeconds: [{ required: true, message: '请输入保留秒数', trigger: 'blur' }],
  timeRange: [{
    validator: (_rule, value, callback) => {
      const range = (value || timeRange.value) as string[];
      if (!Array.isArray(range) || range.length !== 2 || !range[0] || !range[1]) {
        callback(new Error('请选择秒杀时间'));
        return;
      }
      callback();
    },
    trigger: 'change'
  }]
};

const columns: ColumnProps<ShopFlashSaleRow>[] = [
  { prop: 'image', label: '商品图片', width: 100 },
  { prop: 'productName', label: '商品名称', minWidth: 220 },
  { prop: 'price', label: '原价', minWidth: 90 },
  { prop: 'flashPrice', label: '秒杀价', minWidth: 90 },
  { prop: 'stock', label: '库存', minWidth: 80 },
  { prop: 'browse', label: '浏览量', minWidth: 100 },
  { prop: 'fictiBrowse', label: '虚拟浏览量', minWidth: 110 },
  { prop: 'sales', label: '销量', minWidth: 80 },
  { prop: 'productShowStatus', label: '商品状态', minWidth: 100 },
  { prop: 'isSeckill', label: '秒杀商品', minWidth: 100 },
  { prop: 'flashStatus', label: '秒杀上下架', minWidth: 110 },
  { prop: 'flashActive', label: '活动进行中', minWidth: 110 },
  { prop: 'startTime', label: '开始时间', minWidth: 170 },
  { prop: 'endTime', label: '结束时间', minWidth: 170 },
  { prop: 'operation', label: '操作', width: 120, fixed: 'right' }
];

const searchColumns: SearchProps[] = [
  { prop: 'productName', label: '商品名称', el: 'input' },
  {
    prop: 'productShowStatus',
    label: '商品状态',
    el: 'select',
    enum: [
      { label: '已上架', value: 1 },
      { label: '已下架', value: 0 }
    ]
  },
  {
    prop: 'flashStatus',
    label: '秒杀状态',
    el: 'select',
    enum: [
      { label: '已上架', value: 1 },
      { label: '已下架', value: 0 }
    ]
  }
];

const getTableList = (params: ShopFlashSaleQuery) => {
  return getShopFlashSaleListApi(params);
};

const resetForm = () => {
  form.id = undefined;
  form.productId = undefined;
  form.browse = 0;
  form.fictiBrowse = 0;
  form.sales = 0;
  form.productShowStatus = 1;
  form.isSeckill = 1;
  form.activity = '1';
  form.flashStatus = 1;
  form.flashPrice = 0.01;
  form.limitPerUser = 1;
  form.reserveSeconds = 900;
  form.startTime = '';
  form.endTime = '';
  form.skuList = [];
  currentProductName.value = '';
  skuList.value = [];
};

const applyDetailToForm = (detail: ShopFlashSaleDetailRow, row: ShopFlashSaleRow) => {
  currentProductName.value = detail.productName || row.productName || '';
  form.id = row.id;
  form.productId = detail.productId || row.productId;
  form.browse = Number(detail.browse ?? row.browse ?? 0);
  form.fictiBrowse = Number(detail.fictiBrowse ?? row.fictiBrowse ?? 0);
  form.sales = Number(detail.sales ?? row.sales ?? 0);
  form.productShowStatus = Number(detail.productShowStatus ?? row.productShowStatus ?? 1);
  form.isSeckill = Number(detail.isSeckill ?? row.isSeckill ?? 1);
  form.activity = detail.activity || row.activity || '1';
  form.flashStatus = Number(detail.flashStatus ?? row.flashStatus ?? 1);
  form.flashPrice = Number(detail.flashPrice ?? row.flashPrice ?? detail.price ?? row.price ?? 0.01);
  form.limitPerUser = Number(detail.limitPerUser ?? row.limitPerUser ?? 1);
  form.reserveSeconds = Number(detail.reserveSeconds ?? row.reserveSeconds ?? 900);
  form.startTime = detail.startTime || row.startTime || '';
  form.endTime = detail.endTime || row.endTime || '';
  skuList.value = (detail.skuList || []).map(item => ({
    ...item,
    flashPrice: Number(item.flashPrice ?? detail.flashPrice ?? item.price ?? form.flashPrice ?? 0.01)
  }));
};

const resolveProductId = async (row: ShopFlashSaleRow) => {
  const rawProductId = Number(row.productId ?? (row as ShopFlashSaleRow & { product_id?: number }).product_id ?? 0);
  if (rawProductId > 0) {
    return rawProductId;
  }
  const productName = String(row.productName || '').trim();
  if (!productName) {
    return 0;
  }

  const res = await getShopProductListApi({
    page: 1,
    limit: 20,
    productName
  });
  const productRows = Array.isArray(res.data?.rows) ? res.data.rows : [];
  const matched = productRows.find(item => item.productName === productName && (!row.image || item.image === row.image))
    || productRows.find(item => item.productName === productName)
    || (productRows.length === 1 ? productRows[0] : undefined);

  return Number(matched?.id || 0);
};

const openEdit = async (row: ShopFlashSaleRow) => {
  resetForm();
  dialogVisible.value = true;
  detailLoading.value = true;
  try {
    const productId = await resolveProductId(row);
    if (productId <= 0) {
      throw new Error('missing product id');
    }
    const res = await getShopFlashSaleDetailApi(productId);
    applyDetailToForm(res.data || { productId }, { ...row, productId });
  } catch (error) {
    dialogVisible.value = false;
    ElMessage.error('未找到对应商品，营销详情加载失败');
  } finally {
    detailLoading.value = false;
  }
};

const formatMoney = (value?: number | string) => Number(value || 0).toFixed(2);

const formatSkuAttrValue = (raw?: string, fallback?: string) => {
  if (!raw) return fallback || '-';
  try {
    const data = JSON.parse(raw) as Record<string, string>;
    const entries = Object.entries(data || {});
    if (!entries.length) return fallback || raw;
    return entries.map(([key, value]) => `${key}：${value}`).join(' / ');
  } catch (_error) {
    return raw || fallback || '-';
  }
};

const isSkuDefault = (row: ShopFlashSaleSkuRow) => Number(row.isDefault || 0) === 1 || row.isDefault === true;

const submitForm = async () => {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;
  if (skuList.value.some(item => Number(item.flashPrice || 0) <= 0)) {
    ElMessage.warning('请为每个 SKU 设置大于 0 的秒杀价');
    return;
  }

  const productId = Number(form.productId || 0);
  if (productId <= 0) {
    ElMessage.error('商品ID缺失，无法保存营销信息');
    return;
  }

  submitting.value = true;
  try {
    await updateShopFlashSaleApi({
      ...form,
      productId,
      browse: Number(form.browse || 0),
      fictiBrowse: Number(form.fictiBrowse || 0),
      sales: Number(form.sales || 0),
      productShowStatus: Number(form.productShowStatus ?? 1),
      isSeckill: Number(form.isSeckill ?? 1),
      flashStatus: Number(form.flashStatus ?? 1),
      flashPrice: Number(form.flashPrice || 0),
      limitPerUser: Number(form.limitPerUser || 1),
      reserveSeconds: Number(form.reserveSeconds || 900),
      activity: form.activity || '1',
      skuList: skuList.value.map(item => ({
        skuId: Number(item.skuId || 0),
        flashPrice: Number(item.flashPrice || 0)
      }))
    });
    ElMessage.success('营销信息已更新');
    dialogVisible.value = false;
    proTableRef.value?.search();
  } finally {
    submitting.value = false;
  }
};
</script>

<style scoped lang="scss">
.dialog-body {
  min-height: 240px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 14px;
}

.w-full {
  width: 100%;
}

.sku-tips {
  margin: -6px 0 14px;
  color: #909399;
  font-size: 13px;
}

.sku-table {
  width: 100%;
}

.sku-name {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

:deep(.sku-table .el-input-number) {
  width: 100%;
}

@media (max-width: 768px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
