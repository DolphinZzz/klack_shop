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
      <el-form-item label="快递名称" prop="name">
        <el-input v-model="paramsProps.row.name" placeholder="请填写快递名称" clearable></el-input>
      </el-form-item>
      <el-form-item label="快递类型" prop="expressId">
        <el-input v-model="paramsProps.row.expressName" placeholder="请选择快递类型" readonly clearable>
          <template #append>
            <el-button @click="openExpressPicker">选择</el-button>
          </template>
        </el-input>
      </el-form-item>

      <el-form-item label="快递编号" v-if="paramsProps.row.code">
        <el-input v-model="paramsProps.row.code" readonly />
      </el-form-item>

      <el-form-item label="价格" prop="expressPrice">
        <el-input-number v-model="paramsProps.row.expressPrice" :precision="0" :min="1" :max="999999" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false"> 取消</el-button>
      <el-button type="primary" @click="handleSubmit"> 确定</el-button>
    </template>
  </el-dialog>
  <ExpressPickerDialog v-model="expressPickerVisible" @pick="handlePickExpress" />
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue';
import { type ElForm, ElMessage } from 'element-plus';
import ExpressPickerDialog, { type ShopExpressRow } from './ExpressPickerDialog.vue';

defineOptions({
  name: 'ShopExpressTemplateForm'
});

const expressPickerVisible = ref(false);

const openExpressPicker = () => {
  expressPickerVisible.value = true;
};

const handlePickExpress = (row: ShopExpressRow) => {
  // 回填到表单
  paramsProps.value.row.expressId = row.id;
  paramsProps.value.row.expressName = row.expressName;
  paramsProps.value.row.code = row.code;
};
const rules = reactive({});

const visible = ref(false);
const paramsProps = ref<View.DefaultParams>({
  title: '',
  row: {},
  api: undefined,
  getTableList: undefined
});

// 接收父组件传过来的参数
const acceptParams = (params: View.DefaultParams) => {
  paramsProps.value = params;
  visible.value = true;
};

// 提交数据（新增/编辑）
const ruleFormRef = ref<InstanceType<typeof ElForm>>();
const handleSubmit = () => {
  ruleFormRef.value!.validate(async valid => {
    if (!valid) return;
    try {
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
