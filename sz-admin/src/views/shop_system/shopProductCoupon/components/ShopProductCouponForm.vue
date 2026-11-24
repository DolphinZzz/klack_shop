<template>
  <el-dialog v-model="visible" :title="`${paramsProps.title}`" :destroy-on-close="true" width="580px" draggable>
    <el-form
      ref="ruleFormRef"
      label-width="140px"
      label-suffix=" :"
      :rules="rules"
      :model="paramsProps.row"
      @submit.enter.prevent="handleSubmit"
    >
      <el-form-item label="优惠券名称" prop="name">
        <el-input v-model="paramsProps.row.name" placeholder="请填写优惠券名称" clearable></el-input>
      </el-form-item>
      <el-form-item label="优惠券面值" prop="money">
        <el-input-number v-model="paramsProps.row.money" :precision="0" :min="1" :max="999999" />
      </el-form-item>
      <el-form-item label="发放总数" prop="total">
        <el-input-number v-model="paramsProps.row.total" :precision="0" :min="1" :max="999999" />
      </el-form-item>
      <el-form-item label="领取时间范围" prop="couponTimeRange">
        <el-date-picker
          clearable
          v-model="paramsProps.row.couponTimeRange"
          type="datetimerange"
          value-format="YYYY-MM-DD HH:mm:ss"
          range-separator="-"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          placeholder="请选择领取时间范围"
          :disabled="paramsProps.row.couponTimeUnlimited"
        />
        <el-checkbox v-model="paramsProps.row.couponTimeUnlimited" style="margin-left: 12px">无限期</el-checkbox>
      </el-form-item>
      <el-form-item label="可使用时间范围" prop="useTimeRange">
        <el-date-picker
          clearable
          v-model="paramsProps.row.useTimeRange"
          type="datetimerange"
          value-format="YYYY-MM-DD HH:mm:ss"
          range-separator="-"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          placeholder="请选择可使用时间范围"
          :disabled="paramsProps.row.useTimeUnlimited"
        />
        <el-checkbox v-model="paramsProps.row.useTimeUnlimited" style="margin-left: 12px">无限期</el-checkbox>
      </el-form-item>
      <el-form-item label="优惠券类型" prop="type">
        <el-select v-model="paramsProps.row.type" clearable placeholder="请选择优惠券类型">
          <el-option
            v-for="item in optionsStore.getDictOptions('coupon_type')"
            :key="item.alias"
            :label="item.codeName"
            :value="Number(item.alias)"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="所属商品" prop="cateKey">
        <el-input v-model="selectedProductLabel" placeholder="请选择商品" readonly>
          <template #append>
            <el-button @click="openProductPicker">选择</el-button>
          </template>
        </el-input>
      </el-form-item>
      <el-form-item label="最低消费" prop="minMoney">
        <el-input-number v-model="paramsProps.row.minMoney" :precision="0" :min="1" :max="999999" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false"> 取消</el-button>
      <el-button type="primary" @click="handleSubmit"> 确定</el-button>
    </template>
  </el-dialog>
  <ShopProductPickerDialog v-model="productPickerVisible" @pick="handleProductPick" />
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue';
import { type ElForm, ElMessage } from 'element-plus';
import { useOptionsStore } from '@/stores/modules/options';
import { getShopProductDetailApi } from '@/api/modules/shop_system/shopProduct';
import ShopProductPickerDialog from '@/views/shop_system/shopProductCoupon/components/ShopProductPickerDialog.vue';

defineOptions({
  name: 'ShopProductCouponForm'
});

const optionsStore = useOptionsStore();
const rules = reactive({
  name: [{ required: true, message: '请填写优惠券名称' }],
  money: [{ required: true, message: '请填写优惠券面值' }],
  total: [{ required: true, message: '请填写发放总数' }],
  couponTimeRange: [
    {
      validator: (_: any, value: string[], callback: (error?: Error) => void) => {
        if ((paramsProps.value.row as any)?.couponTimeUnlimited) return callback();
        if (Array.isArray(value) && value.length === 2) return callback();
        return callback(new Error('请选择领取时间范围或勾选无限期'));
      }
    }
  ],
  useTimeRange: [
    {
      validator: (_: any, value: string[], callback: (error?: Error) => void) => {
        if ((paramsProps.value.row as any)?.useTimeUnlimited) return callback();
        if (Array.isArray(value) && value.length === 2) return callback();
        return callback(new Error('请选择可使用时间范围或勾选无限期'));
      }
    }
  ],
  type: [{ required: true, message: '请选择优惠券类型' }],
  cateKey: [{ required: true, message: '请选择所属商品' }],
  minMoney: [{ required: true, message: '请填写最低消费' }]
});

const visible = ref(false);
const paramsProps = ref<View.DefaultParams>({
  title: '',
  row: {},
  api: undefined,
  getTableList: undefined
});

const selectedProductLabel = ref('');
const productPickerVisible = ref(false);

const parseProductIds = (productId?: number | string) => {
  if (productId === '' || productId == null) return [];
  if (typeof productId === 'number') return [productId];
  return productId
    .split(',')
    .map(item => item.trim())
    .filter(Boolean)
    .map(item => Number(item))
    .filter(item => !Number.isNaN(item));
};

const loadSelectedProduct = async (productId?: number | string) => {
  const ids = parseProductIds(productId);
  if (!ids.length) {
    selectedProductLabel.value = '';
    return;
  }
  if (ids.length > 1) {
    const names = String((paramsProps.value.row as any)?.productNames || '')
      .split(',')
      .map(item => item.trim())
      .filter(Boolean);
    selectedProductLabel.value = names.length ? names.join('、') : `已选 ${ids.length} 个商品`;
    return;
  }

  if (ids[0] === 0) {
    selectedProductLabel.value = '全部商品';
    return;
  }

  try {
    const res: any = await getShopProductDetailApi({ productId: ids[0] });
    const detail = res?.data ?? res?.data?.data ?? res;
    selectedProductLabel.value = detail?.productName ? detail.productName : `ID:${ids[0]}`;
  } catch (error) {
    selectedProductLabel.value = `ID:${ids[0]}`;
  }
};

const syncCouponRange = () => {
  const row = paramsProps.value.row as any;
  if (row?.couponStartTime || row?.couponEndTime) {
    row.couponTimeRange = [row?.couponStartTime, row?.couponEndTime];
  } else if (!row?.couponTimeRange) {
    row.couponTimeRange = [];
  }
  if (row?.couponTimeUnlimited == null) {
    row.couponTimeUnlimited = !(row?.couponStartTime || row?.couponEndTime);
  }
};

const syncUseRange = () => {
  const row = paramsProps.value.row as any;
  if (row?.useStartTime || row?.useEndTime) {
    row.useTimeRange = [row?.useStartTime, row?.useEndTime];
  } else if (!row?.useTimeRange) {
    row.useTimeRange = [];
  }
  if (row?.useTimeUnlimited == null) {
    row.useTimeUnlimited = !(row?.useStartTime || row?.useEndTime);
  }
};

// 接收父组件传过来的参数
const acceptParams = async (params: View.DefaultParams) => {
  paramsProps.value = params;
  syncCouponRange();
  syncUseRange();
  selectedProductLabel.value = (paramsProps.value.row as any)?.productName || '';
  if (!selectedProductLabel.value && (paramsProps.value.row as any)?.cateKey) {
    await loadSelectedProduct((paramsProps.value.row as any)?.cateKey);
  }
  visible.value = true;
};

// 提交数据（新增、编辑）
const ruleFormRef = ref<InstanceType<typeof ElForm>>();
const handleSubmit = () => {
  ruleFormRef.value!.validate(async valid => {
    if (!valid) return;
    try {
      const payload = { ...(paramsProps.value.row as any) };
      if (!payload.couponTimeUnlimited) {
        if (Array.isArray(payload.couponTimeRange) && payload.couponTimeRange.length === 2) {
          payload.couponStartTime = payload.couponTimeRange[0];
          payload.couponEndTime = payload.couponTimeRange[1];
        }
      } else {
        payload.couponStartTime = undefined;
        payload.couponEndTime = undefined;
      }
      if (!payload.useTimeUnlimited) {
        if (Array.isArray(payload.useTimeRange) && payload.useTimeRange.length === 2) {
          payload.useStartTime = payload.useTimeRange[0];
          payload.useEndTime = payload.useTimeRange[1];
        }
      } else {
        payload.useStartTime = undefined;
        payload.useEndTime = undefined;
      }
      delete payload.couponTimeRange;
      delete payload.useTimeRange;
      delete payload.productName;
      delete payload.couponTimeUnlimited;
      delete payload.useTimeUnlimited;

      await paramsProps.value.api!(payload);
      ElMessage.success({ message: `${paramsProps.value.title}成功` });
      paramsProps.value.getTableList!();
      visible.value = false;
    } catch (error) {
      console.log(error);
    }
  });
};

const openProductPicker = () => {
  productPickerVisible.value = true;
};

const handleProductPick = (row: any) => {
  paramsProps.value.row = {
    ...(paramsProps.value.row as any),
    cateKey: row?.id,
    productName: row?.productName
  };
  if (Number(row?.id) === 0) {
    selectedProductLabel.value = '全部商品';
    return;
  }
  selectedProductLabel.value = row?.productName || (row?.id ? `ID:${row.id}` : '');
};

watch(
  () => (paramsProps.value.row as any)?.cateKey,
  value => {
    if (value && !selectedProductLabel.value) {
      loadSelectedProduct(value);
    }
  }
);

watch(
  () => (paramsProps.value.row as any)?.couponTimeUnlimited,
  value => {
    if (value) {
      (paramsProps.value.row as any).couponTimeRange = [];
    }
  }
);

watch(
  () => (paramsProps.value.row as any)?.useTimeUnlimited,
  value => {
    if (value) {
      (paramsProps.value.row as any).useTimeRange = [];
    }
  }
);

defineExpose({
  acceptParams
});
</script>

<style scoped lang="scss"></style>
