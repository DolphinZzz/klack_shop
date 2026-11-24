<template>
  <div class="sku-wrap">
    <!--    &lt;!&ndash; 顶部说明 &ndash;&gt;-->
    <!--    <el-alert-->
    <!--      title="说明：规格属性（attrList）+ 规格库存（attrValueList）会按后端 JSON 结构提交；attrValueList 内字段为 attr_arr（下划线）。"-->
    <!--      type="info"-->
    <!--      show-icon-->
    <!--      :closable="false"-->
    <!--      class="mb12"-->
    <!--    />-->

    <!-- 规格属性区 -->
    <el-card shadow="never" class="mb12">
      <div class="row head">
        <div class="left">规格属性</div>
        <div class="right">
          <el-button type="primary" @click="addAttr">添加规格</el-button>
          <el-button @click="rebuildSku" :disabled="!canBuildSku">生成/刷新SKU列表</el-button>
        </div>
      </div>

      <div v-if="!attrList.length" class="empty-tip">
        <el-empty description="请先添加规格，如：装帧、纸张，然后录入规格值" />
      </div>

      <div v-for="(attr, aIdx) in attrList" :key="aIdx" class="attr-item">
        <!-- 规格头：规格名 + 删除 -->
        <div class="attr-head">
          <span class="label">规格名：</span>
          <el-input
            v-model="attr.attrName"
            placeholder="如：装帧 / 纸张 / 颜色"
            style="width: 260px"
            @change="syncAttrToParent()"
          />
          <el-button link type="danger" @click="removeAttr(aIdx)">删除规格</el-button>
        </div>

        <!-- 规格值：InputTag -->
        <div class="attr-values">
          <span class="label">规格值：</span>
          <el-input-tag
            v-model="valueTags[aIdx]"
            placeholder="输入后回车添加（同规格内不允许重复）"
            clearable
            :max="30"
            @change="onValuesChange(aIdx)"
            style="width: 520px"
          />
          <div class="tip mt8">例如：平装 / 精装；轻型纸 / 铜版纸</div>
        </div>
      </div>
    </el-card>

    <!-- SKU 表格 -->
    <el-card shadow="never">
      <div class="row head mb12">
        <div class="left">规格库存列表</div>
        <div class="right">
          <el-checkbox v-model="isVipProduct" class="mr12">会员商品</el-checkbox>
          <el-button @click="batchFillDialog = true" :disabled="!attrValueList.length">批量修改</el-button>
          <el-button @click="clearSkuFields" :disabled="!attrValueList.length">清空</el-button>
        </div>
      </div>

      <el-table v-if="attrValueList.length" :data="attrValueList" border stripe size="small">
        <!-- 动态规格列：装帧 / 纸张 ... -->
        <el-table-column
          v-for="(attr, idx) in attrList"
          :key="(attr.attrName || 'attr') + idx"
          :label="attr.attrName || `属性${idx + 1}`"
          min-width="140"
        >
          <template #default="{ row }">
            {{ row.attr_arr?.[idx] }}
          </template>
        </el-table-column>

        <!-- 图片：用你们框架 UploadImg（小尺寸） -->
        <el-table-column label="图片" min-width="120">
          <template #default="{ row }">
            <UploadImg v-model:imageUrl="row.image" :width="'54px'" :height="'54px'" dir="product" />
          </template>
        </el-table-column>

        <el-table-column label="售价" min-width="140">
          <template #default="{ row }">
            <el-input-number v-model="row.price" :min="0" :precision="2" controls-position="right" />
          </template>
        </el-table-column>

        <el-table-column label="成本价" min-width="140">
          <template #default="{ row }">
            <el-input-number v-model="row.cost" :min="0" :precision="2" controls-position="right" />
          </template>
        </el-table-column>

        <el-table-column v-if="isVipProduct" label="会员价" min-width="140">
          <template #default="{ row }">
            <el-input-number v-model="row.vip_price" :min="0" :precision="2" controls-position="right" />
          </template>
        </el-table-column>

        <el-table-column label="划线价" min-width="140">
          <template #default="{ row }">
            <el-input-number v-model="row.otPrice" :min="0" :precision="2" controls-position="right" />
          </template>
        </el-table-column>

        <el-table-column label="库存" min-width="120">
          <template #default="{ row }">
            <el-input-number v-model="row.stock" :min="0" :precision="0" controls-position="right" />
          </template>
        </el-table-column>

        <el-table-column label="商品编码" min-width="160">
          <template #default="{ row }">
            <el-input v-model="row.itemNumber" placeholder="货号/编码" />
          </template>
        </el-table-column>

        <el-table-column label="商品条码" min-width="160">
          <template #default="{ row }">
            <el-input v-model="row.barCode" placeholder="条码" />
          </template>
        </el-table-column>

        <el-table-column label="重量(KG)" min-width="140">
          <template #default="{ row }">
            <el-input-number v-model="row.weight" :min="0" :precision="3" controls-position="right" />
          </template>
        </el-table-column>

        <el-table-column label="体积(m³)" min-width="140">
          <template #default="{ row }">
            <el-input-number v-model="row.volume" :min="0" :precision="3" controls-position="right" />
          </template>
        </el-table-column>

        <el-table-column label="默认选中规格" min-width="140">
          <template #default="{ $index }">
            <el-radio v-model="defaultIndex" :label="$index" @change="onChangeDefault($index)">默认</el-radio>
          </template>
        </el-table-column>

        <el-table-column label="显示" min-width="100">
          <template #default="{ row }">
            <el-switch v-model="row.isShow" />
          </template>
        </el-table-column>

        <el-table-column label="操作" min-width="90" fixed="right">
          <template #default="{ $index }">
            <el-button link type="danger" @click="removeSkuRow($index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-else description="请先添加规格名与规格值，然后点击【生成/刷新SKU列表】" />
    </el-card>

    <!-- 批量填充 -->
    <el-dialog v-model="batchFillDialog" title="批量修改" width="520px">
      <el-form label-width="90px">
        <el-form-item label="售价">
          <el-input-number v-model="batch.price" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="成本价">
          <el-input-number v-model="batch.cost" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item v-if="isVipProduct" label="会员价">
          <el-input-number v-model="batch.vip_price" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="划线价">
          <el-input-number v-model="batch.otPrice" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="库存">
          <el-input-number v-model="batch.stock" :min="0" :precision="0" />
        </el-form-item>
        <el-form-item label="重量">
          <el-input-number v-model="batch.weight" :min="0" :precision="3" />
        </el-form-item>
        <el-form-item label="体积">
          <el-input-number v-model="batch.volume" :min="0" :precision="3" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="batchFillDialog = false">取消</el-button>
        <el-button type="primary" @click="applyBatch">应用到全部SKU</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import UploadImg from '@/components/Upload/Img.vue';

type AttrOption = { image?: string; optionName: string; sort?: number };
type AttrItem = { attrName: string; isShowImage?: boolean; id?: number; sort?: number; optionList: AttrOption[] };

type AttrValueRow = {
  attr_arr: string[];
  attrValueShow?: Record<string, string>;
  image?: string;
  price?: number;
  cost?: number;
  otPrice?: number;
  barCode?: string;
  itemNumber?: string;
  stock?: number;
  weight?: number;
  volume?: number;
  isShow?: boolean;
  sku?: string;
  isDefault?: boolean;
  attrValue?: string;
  index?: number;
  vip_price?: number;
};

const props = defineProps<{
  attrList: AttrItem[];
  attrValueList: AttrValueRow[];
  isVipProduct?: boolean;
}>();

const emit = defineEmits<{
  (e: 'update:attrList', v: AttrItem[]): void;
  (e: 'update:attrValueList', v: AttrValueRow[]): void;
  (e: 'update:isVipProduct', v: boolean): void;
}>();

const isVipProduct = computed<boolean>({
  get: () => props.isVipProduct,
  set: v => emit('update:isVipProduct', v)
});

const attrList = computed({
  get: () => props.attrList,
  set: v => emit('update:attrList', v)
});

const attrValueList = computed({
  get: () => props.attrValueList,
  set: v => emit('update:attrValueList', v)
});

// 每个规格对应的 tags（el-input-tag 用）
const valueTags = reactive<Record<number, string[]>>({});

// 初始化 tags：把 optionList -> string[]
watch(
  () => attrList.value,
  list => {
    list.forEach((a, idx) => {
      const arr = (a.optionList || []).map(o => o.optionName).filter(Boolean);
      valueTags[idx] = Array.from(new Set(arr));
    });
  },
  { deep: true, immediate: true }
);

const canBuildSku = computed(() => {
  return attrList.value.length > 0;
});

function uniqTrim(arr: string[]) {
  return Array.from(new Set((arr || []).map(s => (s ?? '').trim()).filter(Boolean)));
}

// tags 变化时：同步回 optionList（并保证不重复）
function onValuesChange(attrIdx: number) {
  const tags = uniqTrim(valueTags[attrIdx] || []);
  valueTags[attrIdx] = tags;

  const next = [...attrList.value];
  const old = next[attrIdx]?.optionList || [];
  const oldMap = new Map(old.map(o => [o.optionName, o]));

  next[attrIdx].optionList = tags.map((name, i) => ({
    ...(oldMap.get(name) || { image: '' }),
    optionName: name,
    sort: i + 1
  }));

  emit('update:attrList', next);
}

function syncAttrToParent() {
  // attrName 的改变也会响应式影响 props，但这里显式触发一下更稳
  emit('update:attrList', [...attrList.value]);
}

function addAttr() {
  const next = [...attrList.value];
  next.push({
    attrName: '',
    isShowImage: false,
    id: 0,
    sort: next.length + 1,
    optionList: []
  });
  emit('update:attrList', next);
  valueTags[next.length - 1] = [];
}

function removeAttr(idx: number) {
  const next = [...attrList.value];
  next.splice(idx, 1);
  emit('update:attrList', next);

  // 删除后重建 valueTags 的索引映射
  const newTags: Record<number, string[]> = {};
  next.forEach((a, i) => {
    newTags[i] = uniqTrim((a.optionList || []).map(o => o.optionName));
  });
  Object.keys(valueTags).forEach(k => delete valueTags[Number(k)]);
  Object.assign(valueTags, newTags);
}

// -------- SKU 生成逻辑 --------
function cartesian<T>(arr: T[][]): T[][] {
  if (!arr.length) return [];
  return arr.reduce<T[][]>(
    (acc, cur) => {
      const res: T[][] = [];
      acc.forEach(a => cur.forEach(b => res.push([...(a as any), b] as any)));
      return res;
    },
    [[] as any]
  );
}

function buildAttrValueShow(attrNames: string[], attrArr: string[]) {
  const obj: Record<string, string> = {};
  attrNames.forEach((n, i) => {
    obj[n || `属性${i + 1}`] = attrArr[i];
  });
  return obj;
}

function rebuildSku() {
  if (!attrList.value.length) {
    ElMessage.warning('请先添加规格');
    return;
  }

  for (const a of attrList.value) {
    if (!a.attrName?.trim()) {
      ElMessage.warning('请填写规格名');
      return;
    }
    if (!a.optionList?.length) {
      ElMessage.warning(`规格【${a.attrName}】至少添加一个规格值`);
      return;
    }
  }

  const attrNames = attrList.value.map(x => x.attrName);
  const optionMatrix = attrList.value.map(a => (a.optionList || []).map(o => o.optionName));
  const combos = cartesian(optionMatrix);
  const old = attrValueList.value || [];

  const next: AttrValueRow[] = combos.map((arr, idx) => {
    const key = arr.join('||');
    const existed = old.find(x => (x.attr_arr || []).join('||') === key);

    const attrValueShow = buildAttrValueShow(attrNames, arr);
    const row: AttrValueRow = existed
      ? { ...existed }
      : {
          attr_arr: arr,
          image: '',
          price: 0,
          cost: 0,
          otPrice: 0,
          barCode: '',
          itemNumber: '',
          stock: 0,
          weight: 0,
          volume: 0,
          isShow: true,
          isDefault: false,
          sku: '',
          vip_price: 0,

          index: idx + 1
        };

    row.attr_arr = arr;
    row.attrValueShow = attrValueShow;
    row.attrValue = JSON.stringify(attrValueShow);
    row.sku = arr.join(',');

    return row;
  });

  // 默认规则：没有默认则第一条默认；多个默认保留第一个
  ensureSingleDefault(next);

  emit('update:attrValueList', next);
  ElMessage.success(`已生成 ${next.length} 条SKU`);
}

// -------- 默认规格唯一 --------
const defaultIndex = ref<number>(-1);

watch(
  () => attrValueList.value,
  list => {
    const idx = list.findIndex(x => x.isDefault === true);
    defaultIndex.value = idx >= 0 ? idx : list.length ? 0 : -1;
  },
  { deep: true, immediate: true }
);

function ensureSingleDefault(list: AttrValueRow[]) {
  if (!list.length) {
    defaultIndex.value = -1;
    return;
  }
  let idx = list.findIndex(x => x.isDefault === true);
  if (idx === -1) idx = 0;
  list.forEach((x, i) => (x.isDefault = i === idx));
  defaultIndex.value = idx;
}

function onChangeDefault(idx: number) {
  const next = [...attrValueList.value];
  next.forEach((x, i) => (x.isDefault = i === idx));
  defaultIndex.value = idx;
  emit('update:attrValueList', next);
}

function removeSkuRow(idx: number) {
  const next = [...attrValueList.value];
  next.splice(idx, 1);
  ensureSingleDefault(next);
  emit('update:attrValueList', next);
}

// -------- 批量修改 / 清空 --------
const batchFillDialog = ref(false);
const batch = reactive({
  price: undefined as number | undefined,
  vip_price: undefined as number | undefined,
  cost: undefined as number | undefined,
  otPrice: undefined as number | undefined,
  stock: undefined as number | undefined,
  weight: undefined as number | undefined,
  volume: undefined as number | undefined
});

function applyBatch() {
  const next = [...attrValueList.value].map(x => ({
    ...x,
    price: batch.price ?? x.price,
    vip_price: isVipProduct.value ? (batch.vip_price ?? x.vip_price) : x.vip_price,
    cost: batch.cost ?? x.cost,
    otPrice: batch.otPrice ?? x.otPrice,
    stock: batch.stock ?? x.stock,
    weight: batch.weight ?? x.weight,
    volume: batch.volume ?? x.volume
  }));

  emit('update:attrValueList', next);
  batchFillDialog.value = false;
  ElMessage.success('批量修改完成');
}

function clearSkuFields() {
  const next = [...attrValueList.value].map(x => ({
    ...x,
    image: '',
    price: 0,
    vip_price: isVipProduct.value ? (batch.vip_price ?? x.vip_price) : x.vip_price,
    cost: 0,
    otPrice: 0,
    stock: 0,
    barCode: '',
    itemNumber: '',
    weight: 0,
    volume: 0
  }));
  emit('update:attrValueList', next);
  ElMessage.success('已清空 SKU 字段');
}
</script>

<style scoped>
.sku-wrap {
  max-width: 1100px;
}
.row.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.left {
  font-weight: 600;
}
.attr-item {
  padding: 12px 0;
  border-top: 1px solid var(--el-border-color-lighter);
}
.attr-head,
.attr-values {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}
.label {
  width: 68px;
  color: var(--el-text-color-regular);
}
.mb12 {
  margin-bottom: 12px;
}
.mt8 {
  margin-top: 8px;
}
.tip {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
.empty-tip {
  padding: 10px 0;
}

.mr12 {
  margin-right: 12px;
}
</style>
