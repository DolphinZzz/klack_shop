<template>
  <el-dialog v-model="visible" :title="`${paramsProps.title}`" :destroy-on-close="true" width="980px" draggable>
    <el-form
      ref="ruleFormRef"
      label-width="140px"
      label-suffix=" :"
      :rules="rules"
      :model="paramsProps.row"
      @submit.enter.prevent="handleSubmit"
    >
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="用户账号" prop="account">
            <el-input v-model="paramsProps.row.account" placeholder="请填写用户账号" clearable></el-input>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="用户密码" prop="password">
            <el-input v-model="paramsProps.row.password" placeholder="请填写用户密码" clearable></el-input>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="真实姓名" prop="realName">
            <el-input v-model="paramsProps.row.realName" placeholder="请填写真实姓名" clearable></el-input>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="性别" prop="sex">
            <el-select v-model="paramsProps.row.sex" clearable placeholder="请选择性别">
              <el-option
                v-for="item in optionsStore.getDictOptions('user_sex')"
                :key="item.alias"
                :label="item.codeName"
                :value="Number(item.alias)"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="身份证" prop="cardId">
            <el-input v-model="paramsProps.row.cardId" placeholder="请填写身份证" clearable></el-input>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="生日" prop="birthday">
            <el-date-picker
              clearable
              v-model="paramsProps.row.birthday"
              type="datetime"
              value-format="YYYY-MM-DD HH:mm:ss"
              placeholder="请选择生日"
            >
            </el-date-picker>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="用户昵称" prop="nickname">
            <el-input v-model="paramsProps.row.nickname" placeholder="请填写用户昵称" clearable></el-input>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="手机号" prop="phone">
            <el-input v-model.number="paramsProps.row.phone"></el-input>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="1">
        <el-col :span="8">
          <el-form-item label="用户余额" prop="nowMoney">
            <el-input v-model.number="paramsProps.row.nowMoney"></el-input>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="用户积分" prop="integral">
            <el-input v-model.number="paramsProps.row.integral" :precision="0" :min="0" :max="999999" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="用户经验" prop="experience">
            <el-input v-model.number="paramsProps.row.experience" :precision="0" :min="0" :max="999999" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="10">
        <el-col :span="12">
          <el-form-item label="签到天数" prop="signNum">
            <el-input-number v-model="paramsProps.row.signNum" :precision="0" :min="0" :max="999999" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="等级" prop="level">
            <el-input-number v-model="paramsProps.row.level" :precision="0" :min="0" :max="999999" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="用户类型" prop="userType">
            <el-select v-model="paramsProps.row.userType" clearable placeholder="请选择用户类型">
              <el-option
                v-for="item in optionsStore.getDictOptions('user_type')"
                :key="item.alias"
                :label="item.codeName"
                :value="item.alias"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
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
        </el-col>
      </el-row>
      <el-form-item label="用户头像" prop="avatar">
        <UploadImg
          v-model:image-url="paramsProps.row.avatar"
          @change="fileChange"
          dir="user"
          width="135px"
          height="135px"
          border-radius="50%"
        >
          <template #empty>
            <el-icon>
              <Avatar />
            </el-icon>
            <span>请上传头像</span>
          </template>
          <template #tip> 头像大小不能超过 3M</template>
        </UploadImg>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false"> 取消</el-button>
      <el-button type="primary" @click="handleSubmit"> 确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { type ElForm, ElMessage } from 'element-plus';
import { useOptionsStore } from '@/stores/modules/options';
import { getShopCategoryAllListApi } from '@/api/modules/shop_system/shopCategory';
import type { IUploadResult } from '@/api/types/system/upload';
import type { ShopCategoryRow } from '@/api/types/shop_system/shopCategory';
import UploadImg from '@/components/Upload/Img.vue';

defineOptions({
  name: 'ShopUserForm'
});

const optionsStore = useOptionsStore();
const rules = reactive({
  account: [{ required: true, message: '请填写用户账号' }],
  password: [{ required: true, message: '请填写用户密码' }],
  sex: [{ required: true, message: '请填写性别' }],
  birthday: [{ required: true, message: '请填写生日' }],
  cardId: [{ required: true, message: '请填写身份证' }],
  nickname: [{ required: true, message: '请填写用户昵称' }],
  phone: [{ required: true, message: '请填写手机号' }],
  userType: [{ required: true, message: '请填写用户类型' }],
  status: [{ required: true, message: '请填写状态' }]
});

const visible = ref(false);
const paramsProps = ref<View.DefaultParams>({
  title: '',
  row: {},
  api: undefined,
  getTableList: undefined
});

const avatarUploadResult = ref<IUploadResult[] | string[]>([]);

// 接收父组件传过来的参数
const acceptParams = (params: View.DefaultParams) => {
  paramsProps.value = params;
  avatarUploadResult.value = params.row.avatar || [];
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

const fileChange = (data: IUploadResult) => {
  console.log(data);
};
defineExpose({
  acceptParams
});
</script>

<style scoped lang="scss"></style>
