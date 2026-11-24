<template>
  <el-dialog v-model="visible" :title="paramsProps.title" width="920px" top="5vh" destroy-on-close>
    <el-form
      ref="ruleFormRef"
      label-width="120px"
      label-suffix=" :"
      :rules="rules"
      :model="paramsProps.row"
      @submit.enter.prevent="handleSubmit"
    >
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="标题" prop="title">
            <el-input v-model="paramsProps.row.title" maxlength="160" show-word-limit placeholder="请填写资讯标题" clearable />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="资讯分类" prop="categoryId">
            <el-select v-model="paramsProps.row.categoryId" class="w100" filterable clearable placeholder="请选择资讯分类">
              <el-option
                v-for="item in categoryOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="资讯标签" prop="tagName">
            <el-input v-model="paramsProps.row.tagName" maxlength="32" placeholder="如：资讯 / 活动" clearable />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="封面图" prop="coverImage">
            <UploadImg v-model:imageUrl="paramsProps.row.coverImage" width="180px" height="120px" dir="news">
              <template #tip>
                <div class="tip">建议尺寸：720×480，最大 5M</div>
              </template>
            </UploadImg>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="跳转链接" prop="link">
            <el-input v-model="paramsProps.row.link" type="textarea" :rows="4" placeholder="可填写外链或站内链接" clearable />
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="摘要" prop="summary">
        <el-input
          v-model="paramsProps.row.summary"
          type="textarea"
          :rows="3"
          maxlength="500"
          show-word-limit
          placeholder="请填写摘要"
          clearable
        />
      </el-form-item>

      <el-form-item label="正文内容" prop="content">
        <div class="editor-wrap">
          <Toolbar class="editor-toolbar" :editor="editorRef" :defaultConfig="toolbarConfig" mode="default" />
          <Editor
            class="editor-content"
            v-model="paramsProps.row.content"
            :defaultConfig="editorConfig"
            mode="default"
            @onCreated="handleCreated"
          />
        </div>
      </el-form-item>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="来源" prop="source">
            <el-input v-model="paramsProps.row.source" maxlength="120" placeholder="请填写来源" clearable />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="作者" prop="author">
            <el-input v-model="paramsProps.row.author" maxlength="64" placeholder="请填写作者" clearable />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item label="浏览量" prop="browseCount">
            <el-input-number v-model="paramsProps.row.browseCount" :precision="0" :min="0" :max="99999999" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="排序" prop="sort">
            <el-input-number v-model="paramsProps.row.sort" :precision="0" :min="0" :max="999999" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="置顶" prop="isTop">
            <el-switch v-model="paramsProps.row.isTop" :active-value="1" :inactive-value="0" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="状态" prop="status">
            <el-select v-model="paramsProps.row.status" placeholder="请选择状态">
              <el-option
                v-for="item in optionsStore.getDictOptions('biz_status')"
                :key="item.alias"
                :label="item.codeName"
                :value="Number(item.alias)"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item label="发布时间" prop="publishTime">
            <el-date-picker
              v-model="paramsProps.row.publishTime"
              type="datetime"
              value-format="YYYY-MM-DD HH:mm:ss"
              placeholder="请选择发布时间"
              clearable
            />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="开始时间" prop="startTime">
            <el-date-picker
              v-model="paramsProps.row.startTime"
              type="datetime"
              value-format="YYYY-MM-DD HH:mm:ss"
              placeholder="请选择开始时间"
              clearable
            />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="结束时间" prop="endTime">
            <el-date-picker
              v-model="paramsProps.row.endTime"
              type="datetime"
              value-format="YYYY-MM-DD HH:mm:ss"
              placeholder="请选择结束时间"
              clearable
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="备注" prop="remark">
        <el-input v-model="paramsProps.row.remark" type="textarea" :rows="2" maxlength="255" show-word-limit placeholder="请填写备注" clearable />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="handleSubmit">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import '@wangeditor/editor/dist/css/style.css';
import { Editor, Toolbar } from '@wangeditor/editor-for-vue';
import type { IDomEditor, IEditorConfig, IToolbarConfig } from '@wangeditor/editor';
import { onBeforeUnmount, reactive, ref, shallowRef } from 'vue';
import { type ElForm, ElMessage } from 'element-plus';
import { useOptionsStore } from '@/stores/modules/options';
import { useDict } from '@/hooks/useDict';
import UploadImg from '@/components/Upload/Img.vue';
import { getShopHomeNewsCategoryAllApi } from '@/api/modules/shop_system/shopHomeNewsCategory';

defineOptions({
  name: 'ShopHomeNewsForm'
});

const optionsStore = useOptionsStore();
useDict(['biz_status']);
const categoryOptions = ref<Array<{ label: string; value: number }>>([]);

const toolbarConfig: Partial<IToolbarConfig> = {};
const editorConfig: Partial<IEditorConfig> = {
  placeholder: '请输入资讯正文...'
};
const editorRef = shallowRef<IDomEditor | null>(null);

const handleCreated = (editor: IDomEditor) => {
  editorRef.value = editor;
};

onBeforeUnmount(() => {
  const editor = editorRef.value;
  if (editor) editor.destroy();
});

const rules = reactive({
  title: [{ required: true, message: '请填写标题', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择资讯分类', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
});

const createDefaultRow = () => ({
  id: undefined,
  categoryId: undefined,
  title: '',
  tagName: '资讯',
  coverImage: '',
  summary: '',
  content: '',
  link: '',
  source: '',
  author: '',
  browseCount: 0,
  sort: 0,
  isTop: 0,
  status: 0,
  publishTime: '',
  startTime: '',
  endTime: '',
  remark: ''
});

const visible = ref(false);
const paramsProps = ref<View.DefaultParams>({
  title: '',
  row: createDefaultRow(),
  api: undefined,
  getTableList: undefined
});

const acceptParams = (params: View.DefaultParams) => {
  paramsProps.value = {
    ...params,
    row: {
      ...createDefaultRow(),
      ...params.row,
      content: params?.row?.content || ''
    }
  };
  visible.value = true;
};

const loadCategoryOptions = async () => {
  const res = await getShopHomeNewsCategoryAllApi();
  categoryOptions.value = (res.data || []).map(item => ({
    label: item.categoryName || '',
    value: Number(item.id)
  }));
};
void loadCategoryOptions();

const ruleFormRef = ref<InstanceType<typeof ElForm>>();
const handleSubmit = () => {
  ruleFormRef.value?.validate(async valid => {
    if (!valid) return;
    try {
      await paramsProps.value.api!(paramsProps.value.row);
      ElMessage.success({ message: `${paramsProps.value.title}成功！` });
      paramsProps.value.getTableList?.();
      visible.value = false;
    } catch (error) {
      console.log(error);
    }
  });
};

defineExpose({
  acceptParams
});
</script>

<style scoped lang="scss">
.editor-wrap {
  width: 100%;
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  overflow: hidden;
}

.editor-toolbar {
  border-bottom: 1px solid var(--el-border-color);
}

.editor-content {
  min-height: 280px;
}

.tip {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
</style>
