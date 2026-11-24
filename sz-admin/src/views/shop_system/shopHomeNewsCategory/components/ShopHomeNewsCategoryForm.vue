<template>
  <el-dialog v-model="visible" :title="paramsProps.title" width="560px" destroy-on-close>
    <el-form
      ref="ruleFormRef"
      label-width="120px"
      label-suffix=" :"
      :rules="rules"
      :model="paramsProps.row"
      @submit.enter.prevent="handleSubmit"
    >
      <el-form-item label="分类名称" prop="categoryName">
        <el-input v-model="paramsProps.row.categoryName" maxlength="64" placeholder="请填写分类名称" clearable />
      </el-form-item>
      <el-form-item label="排序" prop="sort">
        <el-input-number v-model="paramsProps.row.sort" :precision="0" :min="0" :max="999999" />
      </el-form-item>
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
import { reactive, ref } from 'vue';
import { type ElForm, ElMessage } from 'element-plus';
import { useOptionsStore } from '@/stores/modules/options';
import { useDict } from '@/hooks/useDict';

defineOptions({
  name: 'ShopHomeNewsCategoryForm'
});

const optionsStore = useOptionsStore();
useDict(['biz_status']);

const rules = reactive({
  categoryName: [{ required: true, message: '请填写分类名称', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
});

const createDefaultRow = () => ({
  id: undefined,
  categoryName: '',
  sort: 0,
  status: 0,
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
      ...params.row
    }
  };
  visible.value = true;
};

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
