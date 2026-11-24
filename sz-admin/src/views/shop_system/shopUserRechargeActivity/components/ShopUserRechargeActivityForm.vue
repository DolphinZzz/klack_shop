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
      <el-form-item label="活动名称" prop="activityName">
        <el-input v-model="paramsProps.row.activityName" placeholder="请填写活动名称" clearable></el-input>
      </el-form-item>
      <el-form-item label="充值金额" prop="price">
        <el-input v-model="paramsProps.row.price" placeholder="请填写充值金额" clearable></el-input>
      </el-form-item>
      <el-form-item label="赠送金额" prop="givePrice">
        <el-input v-model="paramsProps.row.givePrice" placeholder="请填写赠送金额" clearable></el-input>
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
defineOptions({
  name: 'ShopUserRechargeActivityForm'
});

const rules = reactive({
  activityName: [{ required: true, message: '请填写活动名称' }],
  price: [{ required: true, message: '请填写充值金额' }],
  givePrice: [{ required: true, message: '请填写赠送金额' }]
});

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
