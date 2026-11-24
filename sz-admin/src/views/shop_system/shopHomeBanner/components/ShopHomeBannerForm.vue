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
      <el-form-item label="标题" prop="title">
        <el-input v-model="paramsProps.row.title" placeholder="请填写标题" clearable></el-input>
      </el-form-item>
      <el-form-item label="图片" prop="image">
        <upload-files v-model:modelValue="imageUploadResult" :limit="1" :file-size="3" :dir="'tmp'" :accept="''" />
      </el-form-item>
      <el-form-item label="跳转链接" prop="link">
        <el-input v-model="paramsProps.row.link" placeholder="请填写跳转链接" :rows="2" type="textarea" clearable></el-input>
      </el-form-item>
      <el-form-item label="排序" prop="sort">
        <el-input-number v-model="paramsProps.row.sort" :precision="0" :min="1" :max="999999" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="paramsProps.row.status" clearable placeholder="请选择状态">
          <el-option
            v-for="item in optionsStore.getDictOptions('biz_status')"
            :key="item.alias"
            :label="item.codeName"
            :value="Number(item.alias)"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="开始时间" prop="startTime">
        <el-date-picker
          clearable
          v-model="paramsProps.row.startTime"
          type="datetime"
          value-format="YYYY-MM-DD HH:mm:ss"
          placeholder="请选择开始时间"
        >
        </el-date-picker>
      </el-form-item>
      <el-form-item label="结束时间" prop="endTime">
        <el-date-picker
          clearable
          v-model="paramsProps.row.endTime"
          type="datetime"
          value-format="YYYY-MM-DD HH:mm:ss"
          placeholder="请选择结束时间"
        >
        </el-date-picker>
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="paramsProps.row.remark" placeholder="请填写备注" :rows="2" type="textarea" clearable></el-input>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false"> 取消</el-button>
      <el-button type="primary" @click="handleSubmit"> 确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue';
import { type ElForm, ElMessage } from 'element-plus';
import { useOptionsStore } from '@/stores/modules/options';
import { useDict } from '@/hooks/useDict';
import type { IUploadResult } from '@/api/types/system/upload';
import UploadFiles from '@/components/Upload/UploadFiles.vue';
defineOptions({
  name: 'ShopHomeBannerForm'
});

const optionsStore = useOptionsStore();
useDict(['biz_status']);
const rules = reactive({
  title: [{ required: true, message: '请填写标题' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
});

const visible = ref(false);
const paramsProps = ref<View.DefaultParams>({
  title: '',
  row: {},
  api: undefined,
  getTableList: undefined
});

const imageUploadResult = ref<IUploadResult[] | string[]>([]);

const getImageUrl = (input: unknown): string => {
  if (!input) return '';
  if (typeof input === 'string') return input;
  if (Array.isArray(input)) {
    const first = input[0] as any;
    if (!first) return '';
    if (typeof first === 'string') return first;
    return first?.url || '';
  }
  if (typeof input === 'object') {
    return (input as any)?.url || '';
  }
  return '';
};

// 接收父组件传过来的参数
const acceptParams = (params: View.DefaultParams) => {
  paramsProps.value = params;
  const imageUrl = getImageUrl(params.row.image);
  imageUploadResult.value = imageUrl ? [imageUrl] : [];
  visible.value = true;
};

// 提交数据（新增/编辑）
const ruleFormRef = ref<InstanceType<typeof ElForm>>();
const handleSubmit = () => {
  ruleFormRef.value!.validate(async valid => {
    if (!valid) return;
    try {
      paramsProps.value.row.image = getImageUrl(imageUploadResult.value);
      await paramsProps.value.api!(paramsProps.value.row);
      ElMessage.success({ message: `${paramsProps.value.title}成功！` });
      paramsProps.value.getTableList!();
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

<style scoped lang="scss"></style>
