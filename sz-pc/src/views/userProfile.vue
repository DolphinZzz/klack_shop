<script setup lang="ts">
import { Delete, Plus, ZoomIn } from '@element-plus/icons-vue';
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, type FormInstance, type FormRules, type UploadProps, type UploadRequestOptions } from 'element-plus';
import { getUserProfileApi, updateUserProfileApi, uploadUserAvatarApi, type UserProfile } from '@/api';
import { useAuthStore } from '@/stores/modules/auth';
import UserCenterScaffold from '@/components/UserCenterScaffold.vue';

const authStore = useAuthStore();
const formRef = ref<FormInstance>();
const loading = ref(false);
const saving = ref(false);
const avatarUploading = ref(false);
const avatarPreviewVisible = ref(false);
const profile = ref<UserProfile | null>(null);

const form = reactive({
  account: '',
  nickname: '',
  realName: '',
  phone: '',
  sex: 0,
  birthday: '',
  cardId: '',
  avatar: ''
});

const rules: FormRules = {
  account: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  cardId: [
    {
      pattern: /^(|\d{15}|\d{17}[\dXx])$/,
      message: '身份证格式不正确',
      trigger: 'blur'
    }
  ]
};

const syncForm = (data: UserProfile) => {
  form.account = data.account || '';
  form.nickname = data.nickname || '';
  form.realName = data.realName || '';
  form.phone = data.phone || '';
  form.sex = Number(data.sex ?? 0);
  form.birthday = data.birthday || '';
  form.cardId = data.cardId || '';
  form.avatar = data.avatar || '';
};

const syncStore = (data: UserProfile) => {
  authStore.updateUserInfo({
    uid: data.uid,
    account: data.account,
    username: data.account,
    nickname: data.nickname,
    realName: data.realName,
    phone: data.phone,
    avatar: data.avatar,
    level: data.level,
    integral: data.integral,
    experience: data.experience,
    nowMoney: data.nowMoney,
    userType: data.userType,
    createTime: data.createTime
  });
};

const fetchProfile = async () => {
  loading.value = true;
  try {
    const res = await getUserProfileApi();
    profile.value = res.data || null;
    if (profile.value) {
      syncForm(profile.value);
      syncStore(profile.value);
    }
  } finally {
    loading.value = false;
  }
};

const beforeAvatarUpload: UploadProps['beforeUpload'] = rawFile => {
  const allowTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'];
  const isImage = allowTypes.includes(rawFile.type);
  const isLt3M = rawFile.size / 1024 / 1024 <= 3;

  if (!isImage) {
    ElMessage.warning('请上传 JPG、PNG、GIF 或 WEBP 格式的图片');
  }
  if (!isLt3M) {
    ElMessage.warning('头像图片大小不能超过 3MB');
  }

  return isImage && isLt3M;
};

const handleAvatarUpload = async (options: UploadRequestOptions) => {
  avatarUploading.value = true;
  try {
    const res = await uploadUserAvatarApi(options.file as File);
    form.avatar = res.data?.url || '';
    ElMessage.success('头像上传成功');
    options.onSuccess(res.data);
  } catch (error) {
    options.onError(error as Error);
  } finally {
    avatarUploading.value = false;
  }
};

const handleAvatarRemove = () => {
  form.avatar = '';
  avatarPreviewVisible.value = false;
};

const saveProfile = async () => {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;

  saving.value = true;
  try {
    await updateUserProfileApi({
      account: form.account.trim(),
      nickname: form.nickname.trim(),
      realName: form.realName.trim() || undefined,
      phone: form.phone.trim(),
      sex: form.sex,
      birthday: form.birthday || undefined,
      cardId: form.cardId.trim() || undefined,
      avatar: form.avatar.trim() || undefined
    });
    ElMessage.success('个人资料已更新');
    await fetchProfile();
  } finally {
    saving.value = false;
  }
};

onMounted(() => {
  void fetchProfile();
});
</script>

<template>
  <UserCenterScaffold
    active-menu="profile"
    crumb-label="个人资料"
    title="个人资料"
    description="你可以在这里维护账号、昵称、头像、实名和联系方式。"
  >
    <div class="profile-page" v-loading="loading">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="profile-form">
        <div class="form-grid">
          <el-form-item label="账号" prop="account">
            <el-input v-model="form.account" placeholder="请输入账号" maxlength="30" />
          </el-form-item>
          <el-form-item label="昵称" prop="nickname">
            <el-input v-model="form.nickname" placeholder="请输入昵称" maxlength="30" />
          </el-form-item>
          <el-form-item label="真实姓名">
            <el-input v-model="form.realName" placeholder="请输入真实姓名" maxlength="20" />
          </el-form-item>
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="form.phone" placeholder="请输入手机号" maxlength="11" />
          </el-form-item>
          <el-form-item label="性别">
            <el-radio-group v-model="form.sex">
              <el-radio :label="0">未知</el-radio>
              <el-radio :label="1">男</el-radio>
              <el-radio :label="2">女</el-radio>
              <el-radio :label="3">保密</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="生日">
            <el-date-picker
              v-model="form.birthday"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="请选择生日"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="身份证" prop="cardId">
            <el-input v-model="form.cardId" placeholder="请输入身份证号" maxlength="18" />
          </el-form-item>
          <el-form-item label="头像" class="avatar-form-item">
            <div class="avatar-upload-field">
              <el-upload
                class="avatar-uploader"
                action="#"
                :show-file-list="false"
                :disabled="avatarUploading"
                accept="image/jpeg,image/png,image/gif,image/webp"
                :http-request="handleAvatarUpload"
                :before-upload="beforeAvatarUpload"
              >
                <img v-if="form.avatar" :src="form.avatar" class="avatar-upload-image" alt="头像预览" />
                <div v-else class="avatar-upload-empty">
                  <el-icon class="avatar-upload-icon"><Plus /></el-icon>
                  <span>点击上传头像</span>
                </div>
                <div v-if="avatarUploading" class="avatar-upload-mask">上传中...</div>
              </el-upload>

              <div class="avatar-upload-actions">
                <el-button text type="primary" :icon="ZoomIn" :disabled="!form.avatar" @click="avatarPreviewVisible = true">
                  预览
                </el-button>
                <el-button
                  text
                  type="danger"
                  :icon="Delete"
                  :disabled="!form.avatar || avatarUploading"
                  @click="handleAvatarRemove"
                >
                  删除
                </el-button>
              </div>

              <p class="avatar-upload-tip">支持 JPG、PNG、GIF、WEBP，图片大小不超过 3MB。</p>
            </div>
          </el-form-item>
        </div>

        <div class="form-actions">
          <el-button type="danger" :loading="saving" :disabled="avatarUploading" @click="saveProfile">保存资料</el-button>
        </div>
      </el-form>

      <aside class="profile-side panel-lite">
        <div class="avatar-card">
          <el-avatar :size="88" :src="form.avatar || authStore.userInfo.avatar">
            {{ (form.nickname || form.account || 'U').slice(0, 1) }}
          </el-avatar>
          <h3>{{ form.nickname || form.account || '未命名用户' }}</h3>
          <p>{{ form.realName || '未设置真实姓名' }}</p>
        </div>

        <div class="summary-list">
          <div class="summary-item">
            <span>账户余额</span>
            <strong>￥{{ Number(profile?.nowMoney || 0).toFixed(2) }}</strong>
          </div>
          <div class="summary-item">
            <span>积分</span>
            <strong>{{ profile?.integral || 0 }}</strong>
          </div>
          <div class="summary-item">
            <span>经验值</span>
            <strong>{{ profile?.experience || 0 }}</strong>
          </div>
          <div class="summary-item">
            <span>等级</span>
            <strong>Lv.{{ profile?.level || 1 }}</strong>
          </div>
        </div>
      </aside>
    </div>

    <el-image-viewer v-if="avatarPreviewVisible && form.avatar" :url-list="[form.avatar]" @close="avatarPreviewVisible = false" />
  </UserCenterScaffold>
</template>

<style scoped lang="scss">
.profile-page {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 280px;
  gap: 18px;
}

.profile-form {
  min-width: 0;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.avatar-form-item {
  grid-column: 1 / -1;
}

.form-actions {
  margin-top: 6px;
}

.panel-lite {
  border: 1px solid transparent;
  box-shadow: var(--kc-shadow-sm);
  border-radius: 18px;
  background: var(--kc-surface-muted);
  padding: 18px;
}

.avatar-upload-field {
  display: grid;
  gap: 10px;
}

.avatar-uploader :deep(.el-upload) {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 168px;
  height: 168px;
  overflow: hidden;
  border: 1px dashed rgba(21, 22, 26, 0.16);
  border-radius: 20px;
  background: var(--kc-surface-muted);
  cursor: pointer;
  transition: border-color 0.2s ease, transform 0.2s ease;
}

.avatar-uploader :deep(.el-upload:hover) {
  border-color: var(--kc-accent);
  transform: translateY(-1px);
}

.avatar-upload-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-upload-empty {
  display: grid;
  justify-items: center;
  gap: 8px;
  color: #64748b;
  font-size: 13px;
}

.avatar-upload-icon {
  font-size: 30px;
  color: var(--kc-accent);
}

.avatar-upload-mask {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgb(15 23 42 / 58%);
  color: #fff;
  font-size: 14px;
}

.avatar-upload-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.avatar-upload-tip {
  margin: 0;
  color: #64748b;
  font-size: 12px;
}

.avatar-card {
  display: grid;
  justify-items: center;
  text-align: center;
}

.avatar-card h3 {
  margin: 14px 0 6px;
  color: #0f172a;
  font-size: 20px;
}

.avatar-card p {
  margin: 0;
  color: #64748b;
  font-size: 13px;
}

.summary-list {
  margin-top: 18px;
  display: grid;
  gap: 10px;
}

.summary-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  border-radius: 18px;
  background: rgb(255 255 255 / 86%);
  color: #475569;
}

.summary-item strong {
  color: var(--kc-accent);
}

@media (max-width: 1024px) {
  .profile-page,
  .form-grid {
    grid-template-columns: 1fr;
  }

  .avatar-form-item {
    grid-column: auto;
  }
}
</style>
