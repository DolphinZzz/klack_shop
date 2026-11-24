<template>
  <el-dialog v-model="visible" title="新增商品" width="1100px" top="5vh" destroy-on-close>
    <el-tabs v-model="activeTab">
      <!-- 商品信息 -->
      <el-tab-pane label="商品信息" name="base">
        <el-form ref="baseFormRef" :model="form" :rules="baseRules" label-width="120px" class="form">
          <el-form-item label="商品名称" prop="productName">
            <el-input v-model="form.productName" maxlength="50" show-word-limit />
          </el-form-item>

          <el-form-item label="商品分类" prop="cateId">
            <el-tree-select
              v-model="form.cateId"
              :data="categoryTree"
              :props="treeProps"
              node-key="id"
              check-strictly
              clearable
              placeholder="请选择商品分类"
              style="width: 100%"
            />
          </el-form-item>

          <el-form-item label="关键字" prop="keyword">
            <el-input-tag v-model="keywordTags" placeholder="输入后回车添加标签" clearable :max="10" />
            <div class="tip mt8">最多 10 个标签”</div>
          </el-form-item>

          <el-form-item label="商品简介" prop="productInfo">
            <el-input v-model="form.productInfo" type="textarea" :rows="3" maxlength="100" show-word-limit />
          </el-form-item>

          <el-form-item label="单位" prop="unitName">
            <el-input v-model="form.unitName" />
          </el-form-item>

          <el-form-item label="快递模板">
            <el-select
              v-model="form.expressId"
              class="w100"
              filterable
              clearable
              placeholder="不选择则默认包邮"
              @change="handleExpressTemplateChange"
            >
              <el-option v-for="it in expressTemplateOptions" :key="it.id" :label="it.label" :value="it.id" />
            </el-select>
            <div class="tip mt8">不选择快递模板时，系统将按包邮处理。</div>
          </el-form-item>

          <el-form-item label="邮费" prop="postage">
            <el-input-number v-model="form.postage" :min="0" :precision="2" :disabled="true" />
          </el-form-item>

          <el-form-item label="获得积分" prop="giveIntegral">
            <el-input-number v-model="form.giveIntegral" :min="0" :precision="0" :max="999999" />
          </el-form-item>

          <el-form-item label="是否上架" prop="isShow">
            <el-switch v-model="form.isShow" :active-value="1" :inactive-value="0" />
          </el-form-item>

          <el-form-item label="封面图" prop="image">
            <UploadImg v-model:imageUrl="form.image" :width="'120px'" :height="'120px'" dir="product">
              <template #tip>
                <div class="tip">建议尺寸：800×800，最大 5M</div>
              </template>
            </UploadImg>
          </el-form-item>

          <el-form-item label="轮播图" prop="sliderImages">
            <UploadImgs v-model:fileList="sliderFileList" :limit="9" :width="'120px'" :height="'120px'" dir="product">
              <template #tip>
                <div class="tip">最多上传9张</div>
              </template>
            </UploadImgs>
          </el-form-item>
        </el-form>
      </el-tab-pane>

      <!-- 规格库存 -->
      <el-tab-pane label="规格库存" name="sku">
        <SpecInventory
          v-model:attrList="form.attrList"
          v-model:attrValueList="form.attrValueList"
          v-model:isVipProduct="form.isVipProduct"
        />
      </el-tab-pane>

      <!-- 商品详情 -->
      <el-tab-pane label="商品详情" name="detail">
        <div class="editor-wrap">
          <Toolbar class="editor-toolbar" :editor="editorRef" :defaultConfig="toolbarConfig" mode="default" />
          <Editor
            class="editor-content"
            v-model="form.desc"
            :defaultConfig="editorConfig"
            mode="default"
            @onCreated="handleCreated"
          />
        </div>
      </el-tab-pane>
    </el-tabs>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="onSubmit">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import '@wangeditor/editor/dist/css/style.css';
import { Editor, Toolbar } from '@wangeditor/editor-for-vue';
import type { IDomEditor, IToolbarConfig, IEditorConfig } from '@wangeditor/editor';
import { onMounted, reactive, ref, watch, shallowRef, onBeforeUnmount } from 'vue';
import { ElMessage, type UploadUserFile } from 'element-plus';
import SpecInventory from '@/views/shop_system/shopProduct/components/SpecInventory.vue';
import { createShopProductApi, getShopProductDetailApi, updateShopProductApi } from '@/api/modules/shop_system/shopProduct';
import { getShopCategoryAllListApi } from '@/api/modules/shop_system/shopCategory';
import UploadImg from '@/components/Upload/Img.vue';
import UploadImgs from '@/components/Upload/Imgs.vue';
import loading from '@/components/Loading/index.vue';
import { getShopExpressTemplateListApi } from '@/api/modules/shop_system/shopExpressTemplate';

// editor 实例，必须 shallowRef
const editorRef = shallowRef<IDomEditor | null>(null);

const toolbarConfig: Partial<IToolbarConfig> = {
  // 你可以按需配置
};

const editorConfig: Partial<IEditorConfig> = {
  placeholder: '请输入商品详情...'
  // 如果你希望图片也走你们上传接口，可在这里配置 customUpload
  // MENU_CONF: { uploadImage: { customUpload: ... } }
};

const handleCreated = (editor: IDomEditor) => {
  editorRef.value = editor;
};

onBeforeUnmount(() => {
  const editor = editorRef.value;
  if (editor) editor.destroy();
});

type ExpressTemplateItem = {
  id: number;
  name: string; // 模板名（如 顺丰标快）
  expressPrice: number; // 价格
  expressName?: string; // 快递公司名（可选）
  code?: string; // 快递编号（可选）
};

const expressTemplateRaw = ref<ExpressTemplateItem[]>([]);
const expressTemplateOptions = ref<{ id: number; label: string }[]>([]);

const loadExpressTemplates = async () => {
  // 下拉一般不需要分页太复杂，直接取前几百条
  const res = await getShopExpressTemplateListApi({ current: 1, limit: 500 });
  const rows: ExpressTemplateItem[] = res?.data?.rows || [];
  expressTemplateRaw.value = rows;

  // label 可以按你喜欢的格式展示
  expressTemplateOptions.value = rows.map(r => ({
    id: r.id,
    label: `${r.name}（${r.expressName ?? '-'} / ￥${r.expressPrice}）`
  }));
};

const handleExpressTemplateChange = (templateId: number | null) => {
  if (!templateId) {
    form.isPostage = 1;
    form.expressId = null;
    form.expressName = '包邮';
    form.postage = 0;
    return;
  }
  const picked = expressTemplateRaw.value.find(x => x.id === templateId);
  if (!picked) return;

  // ✅ 关键：回填商品表三个字段
  form.isPostage = 0;
  form.expressId = picked.id;
  form.expressName = picked.name; // 你商品表 express_name 存“模板名称”
  form.postage = Number(picked.expressPrice ?? 0);
};

type CategoryNode = {
  id: number;
  pid: number;
  cateName: string;
  children?: CategoryNode[];
};

type AttrOption = { image?: string; optionName: string; sort?: number };
type AttrItem = { attrName: string; isShowImage?: boolean; id?: number; sort?: number; optionList: AttrOption[] };
type AttrValueRow = {
  attr_arr: string[];
  attrValueShow?: Record<string, string>;
  image?: string;
  price?: number;
  vipPrice?: number;
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
};

const mode = ref<'create' | 'edit'>('create');
const editId = ref<number | null>(null);
const visible = ref(false);
const activeTab = ref<'base' | 'sku' | 'detail'>('base');
const baseFormRef = ref();
const sliderInput = ref('');
const keywordTags = ref<string[]>([]);
const sliderFileList = ref<UploadUserFile[]>([]);

const form = reactive({
  image: '',
  sliderImages: [] as string[],
  keyword: '',
  postage: 0,
  productName: '',
  productInfo: '',
  cateId: null as number | null,
  unitName: '',
  sort: 0,
  giveIntegral: 0,
  isShow: 1,
  isPostage: 1,
  isVipProduct: false,
  desc: '',
  expressId: null as number | null,
  expressName: '包邮',

  attrList: [] as AttrItem[],
  attrValueList: [] as AttrValueRow[]
});

// form.keyword -> keywordTags（用于回显/重置）
watch(
  () => form.keyword,
  v => {
    const arr = (v || '')
      .split(',')
      .map(s => s.trim())
      .filter(Boolean);
    // 去重
    keywordTags.value = Array.from(new Set(arr));
  },
  { immediate: true }
);

// keywordTags -> form.keyword（用于提交）
watch(
  keywordTags,
  arr => {
    const cleaned = (arr || []).map(s => s.trim()).filter(Boolean);
    form.keyword = Array.from(new Set(cleaned)).join(',');
  },
  { deep: true }
);

const categoryTree = ref<CategoryNode[]>([]);

const treeProps = {
  value: 'id', // v-model 绑定的值
  label: 'cateName', // 显示名称
  children: 'children'
};

const loadCategoryTree = async () => {
  const res = await getShopCategoryAllListApi();
  categoryTree.value = res?.data || [];
};

onMounted(() => {
  loadCategoryTree();
  loadExpressTemplates();
});

watch(
  sliderFileList,
  list => {
    const newImages = (list || []).map(x => x.url as string).filter(Boolean);
    // 只有当值真正变化时才更新
    if (JSON.stringify(newImages) !== JSON.stringify(form.sliderImages)) {
      form.sliderImages = newImages;
    }
  },
  { deep: true }
);

watch(
  () => form.sliderImages,
  arr => {
    const newFileList = (arr || []).map((url, idx) => ({ name: `slider_${idx + 1}`, url }));
    // 只有当值真正变化时才更新
    if (JSON.stringify(newFileList) !== JSON.stringify(sliderFileList.value)) {
      sliderFileList.value = newFileList;
    }
  },
  { immediate: true }
);

const baseRules = {
  productName: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  cateId: [{ required: true, message: '请输入分类', trigger: 'change' }],
  unitName: [{ required: true, message: '请输入单位', trigger: 'blur' }],
  image: [{ required: true, message: '请填写/上传封面图', trigger: 'blur' }],
  sliderImages: [
    {
      validator: (_: any, val: string[], cb: any) => {
        if (!val || val.length === 0) cb(new Error('请至少上传一张轮播图'));
        else cb();
      },
      trigger: 'change'
    }
  ],
  keyword: [{ required: true, message: '请输入关键字', trigger: 'change' }]
};

const emit = defineEmits<{ (e: 'success'): void }>();

function resetForm() {
  activeTab.value = 'base';
  sliderInput.value = '';
  sliderFileList.value = [];
  keywordTags.value = [];

  Object.assign(form, {
    image: '',
    sliderImages: [],
    keyword: '',
    postage: 0,
    productName: '',
    productInfo: '',
    cateId: null as number | null,
    unitName: '',
    sort: 0,
    giveIntegral: 0,
    isShow: 1,
    isPostage: 1,
    isVipProduct: false,
    expressId: null,
    expressName: '包邮',
    desc: '',
    attrList: [],
    attrValueList: []
  });
}

function openCreate() {
  mode.value = 'create';
  editId.value = null;
  resetForm();
  visible.value = true;
}

async function openEdit(productId: number) {
  mode.value = 'edit';
  editId.value = productId;
  resetForm();
  visible.value = true;
  await loadExpressTemplates();
  await loadDetail(productId);
}

async function loadDetail(productId: number) {
  loading.value = true;
  try {
    const res = await getShopProductDetailApi({ productId });
    const d = res?.data;
    if (!d) return;

    /* ===============================
       一、回显【商品基础信息】
    =============================== */
    Object.assign(form, {
      productName: d.productName ?? '',
      cateId: d.cateId ?? null,
      keyword: d.keyword ?? '',
      productInfo: d.productInfo ?? '',
      unitName: d.unitName ?? '',
      postage: d.postage ?? 0,
      sort: d.sort ?? 0,
      giveIntegral: d.giveIntegral ?? d.give_integral ?? 0,
      isShow: d.isShow ?? 1,
      isPostage: d.isPostage ?? d.is_postage ?? ((d.expressId ?? d.express_id) ? 0 : 1),
      desc: d.desc ?? '',
      expressId: d.expressId ?? d.express_id ?? null,
      expressName: d.expressName ?? d.express_name ?? ((d.expressId ?? d.express_id) ? '' : '包邮'),
      // 封面图（字符串）
      image: d.image ?? ''
    });

    /* ===============================
       二、回显【轮播图】
       后端是 "a,b,c" → 前端要 string[]
    =============================== */
    form.sliderImages = d.sliderImage ? d.sliderImage.split(',').filter(Boolean) : [];

    sliderFileList.value = form.sliderImages.map((url, idx) => ({
      name: `slider_${idx + 1}`,
      url
    }));

    /* ===============================
       三、回显【规格属性 attrList】
       后端 attrValues: "平装,精装"
    =============================== */
    form.attrList = (d.attrList || []).map((a: any, idx: number) => {
      // 优先使用 optionList（最安全）
      if (Array.isArray(a.optionList) && a.optionList.length) {
        return {
          attrName: a.attrName,
          optionList: a.optionList.map((o: any, i: number) => ({
            optionName: o.optionName,
            image: o.image ?? '',
            sort: o.sort ?? i + 1
          }))
        };
      }

      // 兼容 attrValues: "平装,精装"
      const values = (a.attrValues || '')
        .split(',')
        .map((x: string) => x.trim())
        .filter(Boolean);

      return {
        attrName: a.attrName,
        optionList: values.map((v: string, i: number) => ({
          optionName: v,
          image: '',
          sort: i + 1
        }))
      };
    });

    /* ===============================
       四、回显【规格库存 attrValueList】
       ⚠️ SpecInventory 依赖 attr_arr
    =============================== */
    const attrNames = form.attrList.map(a => a.attrName);

    form.attrValueList = (d.attrValueList || []).map((v: any, idx: number) => {
      const attrArr = Array.isArray(v.attr_arr)
        ? v.attr_arr
        : Array.isArray(v.attrArr)
          ? v.attrArr
          : typeof v.sku === 'string'
            ? v.sku
                .split(',')
                .map((x: string) => x.trim())
                .filter(Boolean)
            : [];

      const attrValueShow: Record<string, string> = {};
      attrNames.forEach((n, i) => {
        attrValueShow[n] = attrArr[i] ?? '';
      });

      return {
        ...v,
        attr_arr: attrArr, // ⭐ 关键
        attrValueShow,
        attrValue: JSON.stringify(attrValueShow),
        isDefault: v.isDefault === 1,
        index: v.index ?? idx + 1
      };
    });
  } finally {
    loading.value = false;
  }
}

defineExpose({ openCreate, openEdit });

async function onSubmit() {
  await baseFormRef.value?.validate();

  if (!form.attrList.length || !form.attrValueList.length) {
    activeTab.value = 'sku';
    ElMessage.warning('请先配置规格库存');
    return;
  }

  const payload = {
    ...form,
    id: editId.value!,
    isPostage: form.expressId ? 0 : 1,
    expressId: form.expressId,
    expressName: form.expressId ? form.expressName : '包邮',
    postage: form.expressId ? Number(form.postage ?? 0) : 0
  };

  if (mode.value === 'create') {
    await createShopProductApi(payload);
  } else {
    await updateShopProductApi({
      ...payload
    });
  }

  ElMessage.success(mode.value === 'create' ? '创建成功' : '保存成功');
  visible.value = false;
  emit('success');
}
</script>

<style scoped>
.form {
  max-width: 900px;
}

.mr8 {
  margin-right: 8px;
}

.mt8 {
  margin-top: 8px;
}

.w100 {
  width: 100%;
}

.tip {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
</style>
