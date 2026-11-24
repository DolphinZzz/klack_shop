<template>
  <el-dialog
    v-model="visible"
    :title="`${paramsProps.title}`"
    :destroy-on-close="true"
    width="580px"
    draggable
  >
    <el-form
      ref="ruleFormRef"
      label-width="140px"
      label-suffix=" :"
      :rules="rules"
      :model="paramsProps.row"
      @submit.enter.prevent="handleSubmit"
    >
      <el-form-item label="订单号" prop="orderId">
        <el-input v-model="paramsProps.row.orderId" placeholder="请填写订单号" clearable></el-input>
      </el-form-item>
      <el-form-item label="用户id" prop="uid">
        <el-input-number v-model="paramsProps.row.uid" :precision="0" :min="1" :max="999999" />
      </el-form-item>
      <el-form-item label="用户姓名" prop="realName">
        <el-input v-model="paramsProps.row.realName" placeholder="请填写用户姓名" clearable></el-input>
      </el-form-item>
      <el-form-item label="用户电话" prop="userPhone">
        <el-input v-model="paramsProps.row.userPhone" placeholder="请填写用户电话" clearable></el-input>
      </el-form-item>
      <el-form-item label="用户地址" prop="userAddress">
        <el-input v-model="paramsProps.row.userAddress" placeholder="请填写用户地址" clearable></el-input>
      </el-form-item>
      <el-form-item label="购物车id" prop="cartIds">
        <el-input v-model="paramsProps.row.cartIds" placeholder="请填写购物车id" clearable></el-input>
      </el-form-item>
      <el-form-item label="运费金额" prop="freightPrice">
        <el-input-number v-model="paramsProps.row.freightPrice" :precision="0" :min="1" :max="999999" />
      </el-form-item>
      <el-form-item label="订单商品总数" prop="totalNum">
        <el-input-number v-model="paramsProps.row.totalNum" :precision="0" :min="1" :max="999999" />
      </el-form-item>
      <el-form-item label="订单总价" prop="totalPirce">
        <el-input-number v-model="paramsProps.row.totalPirce" :precision="0" :min="1" :max="999999" />
      </el-form-item>
      <el-form-item label="抵扣金额" prop="deductionPrice">
        <el-input-number v-model="paramsProps.row.deductionPrice" :precision="0" :min="1" :max="999999" />
      </el-form-item>
      <el-form-item label="消费券id" prop="couponId">
        <el-input-number v-model="paramsProps.row.couponId" :precision="0" :min="1" :max="999999" />
      </el-form-item>
      <el-form-item label="消费券金额" prop="couponPrice">
        <el-input-number v-model="paramsProps.row.couponPrice" :precision="0" :min="1" :max="999999" />
      </el-form-item>
      <el-form-item label="支付状态" prop="paid">
        <el-input-number v-model="paramsProps.row.paid" :precision="0" :min="1" :max="999999" />
      </el-form-item>
      <el-form-item label="支付时间" prop="payTime">
        <el-date-picker clearable
          v-model="paramsProps.row.payTime"
          type="datetime"
          value-format="YYYY-MM-DD HH:mm:ss"
          placeholder="请选择支付时间">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="支付方式" prop="payType">
        <el-select v-model="paramsProps.row.payType" clearable placeholder="请选择支付方式">
          <el-option
            v-for="item in optionsStore.getDictOptions('')"
            :key="item.id"
            :label="item.codeName"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="订单状态" prop="status">
        <el-select v-model="paramsProps.row.status" clearable placeholder="请选择订单状态">
          <el-option
            v-for="item in optionsStore.getDictOptions('order_status')"
            :key="item.alias"
            :label="item.codeName"
            :value="Number(item.alias)"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="退款状态" prop="refundStatus">
        <el-select v-model="paramsProps.row.refundStatus" clearable placeholder="请选择退款状态">
          <el-option
            v-for="item in optionsStore.getDictOptions('refund_status')"
            :key="item.alias"
            :label="item.codeName"
            :value="Number(item.alias)"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="用户退款说明" prop="refundReasonExplain">
        <el-input v-model="paramsProps.row.refundReasonExplain" placeholder="请填写用户退款说明" :rows="2" type="textarea" clearable></el-input>
      </el-form-item>
      <el-form-item label="不退款理由" prop="refundReason">
        <el-input v-model="paramsProps.row.refundReason" placeholder="请填写不退款理由" clearable></el-input>
      </el-form-item>
      <el-form-item label="退款金额" prop="refundPrice">
        <el-input-number v-model="paramsProps.row.refundPrice" :precision="0" :min="1" :max="999999" />
      </el-form-item>
      <el-form-item label="快递单号" prop="expressNo">
        <el-input v-model="paramsProps.row.expressNo" placeholder="请填写快递单号" clearable></el-input>
      </el-form-item>
      <el-form-item label="发货id" prop="deliveryId">
        <el-input-number v-model="paramsProps.row.deliveryId" :precision="0" :min="1" :max="999999" />
      </el-form-item>
      <el-form-item label="消费赚取积分" prop="gainIntegral">
        <el-input-number v-model="paramsProps.row.gainIntegral" :precision="0" :min="1" :max="999999" />
      </el-form-item>
      <el-form-item label="消费使用积分" prop="useIntegral">
        <el-input-number v-model="paramsProps.row.useIntegral" :precision="0" :min="1" :max="999999" />
      </el-form-item>
      <el-form-item label="是否删除" prop="isDel">
        <el-input-number v-model="paramsProps.row.isDel" :precision="0" :min="1" :max="999999" />
      </el-form-item>
      <el-form-item label="拼团id" prop="pinkId">
        <el-input-number v-model="paramsProps.row.pinkId" :precision="0" :min="1" :max="999999" />
      </el-form-item>
      <el-form-item label="秒杀id" prop="seckillId">
        <el-input-number v-model="paramsProps.row.seckillId" :precision="0" :min="1" :max="999999" />
      </el-form-item>
      <el-form-item label="砍价id" prop="bargainId">
        <el-input-number v-model="paramsProps.row.bargainId" :precision="0" :min="1" :max="999999" />
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
defineOptions({
  name: 'ShopOrderForm'
});

const optionsStore = useOptionsStore();
const rules = reactive({
  cartIds: [{ required: true, message: '请填写购物车id' }],
  createTime: [{ required: true, message: '请填写创建时间' }],
  updateTime: [{ required: true, message: '请填写更新时间' }],
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
  paramsProps.value = params
  visible.value = true
};

// 提交数据（新增/编辑）
const ruleFormRef = ref<InstanceType<typeof ElForm>>();
const handleSubmit = () => {
  ruleFormRef.value!.validate(async (valid) => {
    if (!valid) return
    try {
      await paramsProps.value.api!(paramsProps.value.row);
      ElMessage.success({ message: `${paramsProps.value.title}成功！` });
      paramsProps.value.getTableList!();
      visible.value = false;
    } catch (error) {
      console.log(error);
    }
  });
}

defineExpose({
  acceptParams
});
</script>

<style scoped lang="scss"></style>