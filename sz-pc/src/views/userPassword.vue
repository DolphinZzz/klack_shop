<script setup lang="ts">
import { reactive, ref } from 'vue';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import { updateUserPasswordApi } from '@/api';
import UserCenterScaffold from '@/components/UserCenterScaffold.vue';

const formRef = ref<FormInstance>();
const saving = ref(false);

const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
});

const rules: FormRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '新密码至少 6 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_, value, callback) => {
        if (value !== form.newPassword) {
          callback(new Error('两次输入的新密码不一致'));
          return;
        }
        callback();
      },
      trigger: 'blur'
    }
  ]
};

const savePassword = async () => {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;

  saving.value = true;
  try {
    await updateUserPasswordApi({
      oldPassword: form.oldPassword,
      newPassword: form.newPassword
    });
    ElMessage.success('密码修改成功，请牢记新密码');
    form.oldPassword = '';
    form.newPassword = '';
    form.confirmPassword = '';
    formRef.value?.clearValidate();
  } finally {
    saving.value = false;
  }
};
</script>

<template>
  <UserCenterScaffold
    active-menu="password"
    crumb-label="账号密码"
    title="账号密码"
    description="为了账户安全，修改密码时需要先校验原密码。"
  >
    <div class="password-page">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="password-form">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="form.oldPassword" type="password" show-password placeholder="请输入原密码" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="form.newPassword" type="password" show-password placeholder="请输入新密码" />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
        </el-form-item>
        <div class="password-actions">
          <el-button type="danger" :loading="saving" @click="savePassword">修改密码</el-button>
        </div>
      </el-form>

      <div class="password-tips panel-lite">
        <h3>安全建议</h3>
        <p>建议新密码长度不少于 6 位，并组合使用字母、数字或符号。</p>
        <p>如果你在公共设备登录过，修改密码后建议重新登录，确保账号状态安全。</p>
      </div>
    </div>
  </UserCenterScaffold>
</template>

<style scoped lang="scss">
.password-page {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 18px;
}

.password-form {
  border: 1px solid transparent;
  box-shadow: var(--kc-shadow-sm);
  border-radius: 18px;
  padding: 18px;
}

.password-actions {
  margin-top: 6px;
}

.panel-lite {
  border: 1px solid transparent;
  box-shadow: var(--kc-shadow-sm);
  border-radius: 18px;
  background: var(--kc-surface-muted);
  padding: 18px;
}

.panel-lite h3 {
  margin: 0 0 12px;
  color: #0f172a;
  font-size: 18px;
}

.panel-lite p {
  margin: 0;
  color: #64748b;
  line-height: 1.8;
}

.panel-lite p + p {
  margin-top: 10px;
}

@media (max-width: 1024px) {
  .password-page {
    grid-template-columns: 1fr;
  }
}
</style>
