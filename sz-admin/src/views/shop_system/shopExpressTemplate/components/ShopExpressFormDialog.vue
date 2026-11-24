<template>
  <el-dialog v-model="visible" :title="title" width="520px" draggable :destroy-on-close="true">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" label-suffix=" :">
      <el-form-item label="快递名" prop="expressName">
        <el-input v-model="form.expressName" placeholder="请输入快递名" clearable />
      </el-form-item>

      <el-form-item label="编号" prop="code">
        <el-input v-model="form.code" placeholder="例如 SF / YTO" clearable />
      </el-form-item>

      <el-form-item label="排序" prop="sort">
        <el-input-number v-model="form.sort" :min="0" :max="999999" :precision="0" />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import { ElForm, ElMessage } from 'element-plus';
import { createShopExpressApi } from '@/api/modules/shop_system/shopExpress'; // 你需要在 api 里提供这个方法

type FormModel = {
  expressName: string;
  code: string;
  sort: number;
};

const visible = ref(false);
const title = ref('新增快递公司');
const submitting = ref(false);

const form = reactive<FormModel>({
  expressName: '',
  code: '',
  sort: 0
});

const rules = reactive({
  expressName: [{ required: true, message: '请输入快递名', trigger: 'blur' }],
  code: [{ required: true, message: '请输入编号', trigger: 'blur' }]
});

const formRef = ref<InstanceType<typeof ElForm>>();

const open = () => {
  title.value = '新增快递公司';
  form.expressName = '';
  form.code = '';
  form.sort = 0;
  visible.value = true;
};

const handleSubmit = async () => {
  await formRef.value?.validate(async valid => {
    if (!valid) return;
    try {
      submitting.value = true;
      await createShopExpressApi({ ...form });
      ElMessage.success('新增成功');
      visible.value = false;
      emit('success');
    } finally {
      submitting.value = false;
    }
  });
};

const emit = defineEmits<{
  (e: 'success'): void;
}>();

defineExpose({ open });
</script>
