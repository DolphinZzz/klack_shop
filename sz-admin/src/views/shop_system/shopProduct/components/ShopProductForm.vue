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
      <el-form-item label="商品图片" prop="image">
        <upload-files v-model:modelValue="imageUploadResult" :limit="5" :file-size="3" :dir="'tmp'" :accept="''" />
      </el-form-item>
      <el-form-item label="商品名称" prop="productName">
        <el-input v-model="paramsProps.row.productName" placeholder="请填写商品名称" clearable></el-input>
      </el-form-item>
      <el-form-item label="商品介绍" prop="productInfo">
        <el-input v-model="paramsProps.row.productInfo" placeholder="请填写商品介绍" clearable></el-input>
      </el-form-item>
      <el-form-item label="关键词" prop="keyword">
        <el-input v-model="paramsProps.row.keyword" placeholder="请填写关键词" clearable></el-input>
      </el-form-item>
      <el-form-item label="分类id" prop="cateId">
        <el-select v-model="paramsProps.row.cateId" clearable placeholder="请选择分类id">
          <el-option
            v-for="item in optionsStore.getDictOptions('')"
            :key="item.id"
            :label="item.codeName"
            :value="Number(item.id)"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="商品价格" prop="price">
        <el-input-number v-model="paramsProps.row.price" :precision="0" :min="1" :max="999999" />
      </el-form-item>
      <el-form-item label="vip价格" prop="vipPrice">
        <el-input-number v-model="paramsProps.row.vipPrice" :precision="0" :min="1" :max="999999" />
      </el-form-item>
      <el-form-item label="市场价" prop="otPrice">
        <el-input-number v-model="paramsProps.row.otPrice" :precision="0" :min="1" :max="999999" />
      </el-form-item>
      <el-form-item label="是否包邮" prop="isPostage">
        <el-select v-model="paramsProps.row.isPostage" clearable placeholder="请选择是否包邮">
          <el-option
            v-for="item in optionsStore.getDictOptions('is_postage')"
            :key="item.id"
            :label="item.codeName"
            :value="Number(item.id)"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="邮费" prop="postage">
        <el-input-number v-model="paramsProps.row.postage" :precision="0" :min="1" :max="999999" />
      </el-form-item>
      <el-form-item label="单位名" prop="unitName">
        <el-input v-model="paramsProps.row.unitName" placeholder="请填写单位名" clearable></el-input>
      </el-form-item>
      <el-form-item label="销量" prop="sales">
        <el-input-number v-model="paramsProps.row.sales" :precision="0" :min="1" :max="999999" />
      </el-form-item>
      <el-form-item label="库存" prop="stock">
        <el-input-number v-model="paramsProps.row.stock" :precision="0" :min="1" :max="999999" />
      </el-form-item>
      <el-form-item label="状态" prop="isShow">
        <el-select v-model="paramsProps.row.isShow" clearable placeholder="请选择状态">
          <el-option
            v-for="item in optionsStore.getDictOptions('is_show')"
            :key="item.id"
            :label="item.codeName"
            :value="Number(item.id)"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="是否热卖" prop="isHot">
        <el-select v-model="paramsProps.row.isHot" clearable placeholder="请选择是否热卖">
          <el-option
            v-for="item in optionsStore.getDictOptions('is_hot')"
            :key="item.id"
            :label="item.codeName"
            :value="Number(item.id)"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="是否精品" prop="isBest">
        <el-select v-model="paramsProps.row.isBest" clearable placeholder="请选择是否精品">
          <el-option
            v-for="item in optionsStore.getDictOptions('is_best')"
            :key="item.id"
            :label="item.codeName"
            :value="Number(item.id)"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="是否新品" prop="isNew">
        <el-select v-model="paramsProps.row.isNew" clearable placeholder="请选择是否新品">
          <el-option
            v-for="item in optionsStore.getDictOptions('is_new')"
            :key="item.id"
            :label="item.codeName"
            :value="Number(item.id)"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="开启秒杀" prop="isSeckill">
        <el-select v-model="paramsProps.row.isSeckill" clearable placeholder="请选择开启秒杀">
          <el-option
            v-for="item in optionsStore.getDictOptions('is_seckill')"
            :key="item.id"
            :label="item.codeName"
            :value="Number(item.id)"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="开启拼团" prop="isBargain">
        <el-select v-model="paramsProps.row.isBargain" clearable placeholder="请选择开启拼团">
          <el-option
            v-for="item in optionsStore.getDictOptions('is_bargain')"
            :key="item.id"
            :label="item.codeName"
            :value="Number(item.id)"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="获得积分" prop="giveIntegral">
        <el-input-number v-model="paramsProps.row.giveIntegral" :precision="0" :min="1" :max="999999" />
      </el-form-item>
      <el-form-item label="产品描述" prop="desc">
        <el-input v-model="paramsProps.row.desc" placeholder="请填写产品描述" clearable></el-input>
      </el-form-item>
      <el-form-item label="活动显示排序" prop="activity">
        <el-select v-model="paramsProps.row.activity" clearable placeholder="请选择活动显示排序">
          <el-option
            v-for="item in optionsStore.getDictOptions('activity_type')"
            :key="item.id"
            :label="item.codeName"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="删除" prop="isDel">
        <el-select v-model="paramsProps.row.isDel" clearable placeholder="请选择删除">
          <el-option
            v-for="item in optionsStore.getDictOptions('logitech_del')"
            :key="item.id"
            :label="item.codeName"
            :value="Number(item.id)"
          />
        </el-select>
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
import type { IUploadResult } from '@/api/types/system/upload';
import UploadFiles from '@/components/Upload/UploadFiles.vue';
defineOptions({
  name: 'ShopProductForm'
});

const optionsStore = useOptionsStore();
const rules = reactive({
  productName: [{ required: true, message: '请填写商品名称' }],
  productInfo: [{ required: true, message: '请填写商品介绍' }],
  keyword: [{ required: true, message: '请填写关键词' }],
  cateId: [{ required: true, message: '请填写分类id' }],
  price: [{ required: true, message: '请填写商品价格' }],
  vipPrice: [{ required: true, message: '请填写vip价格' }],
  otPrice: [{ required: true, message: '请填写市场价' }],
  isPostage: [{ required: true, message: '请填写是否包邮' }],
  stock: [{ required: true, message: '请填写库存' }],
  isShow: [{ required: true, message: '请填写状态' }],
  isHot: [{ required: true, message: '请填写是否热卖' }],
  isBest: [{ required: true, message: '请填写是否精品' }],
  isNew: [{ required: true, message: '请填写是否新品' }],
  isSeckill: [{ required: true, message: '请填写开启秒杀' }],
  isBargain: [{ required: true, message: '请填写开启拼团' }],
  giveIntegral: [{ required: true, message: '请填写获得积分' }],
  desc: [{ required: true, message: '请填写产品描述' }],
  createTime: [{ required: true, message: '请填写创建时间' }],
  updateTime: [{ required: true, message: '请填写更新时间' }],
  isDel: [{ required: true, message: '请填写删除' }]
});

const visible = ref(false);
const paramsProps = ref<View.DefaultParams>({
  title: '',
  row: {},
  api: undefined,
  getTableList: undefined
});

const imageUploadResult = ref<IUploadResult[] | string[]>([]);
const sliderImageUploadResult = ref<IUploadResult[] | string[]>([]);

// 接收父组件传过来的参数
const acceptParams = (params: View.DefaultParams) => {
  paramsProps.value = params;
  imageUploadResult.value = params.row.image || [];
  sliderImageUploadResult.value = params.row.sliderImage || [];
  visible.value = true;
};

// 提交数据（新增/编辑）
const ruleFormRef = ref<InstanceType<typeof ElForm>>();
const handleSubmit = () => {
  ruleFormRef.value!.validate(async valid => {
    if (!valid) return;
    try {
      paramsProps.value.row.image = imageUploadResult.value; // 附件数据添加--从上传组件获取
      paramsProps.value.row.sliderImage = sliderImageUploadResult.value; // 附件数据添加--从上传组件获取
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
