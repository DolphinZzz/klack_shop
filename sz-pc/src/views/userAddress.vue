<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import {
  createUserAddressApi,
  deleteUserAddressApi,
  getCityListApi,
  getUserAddressListApi,
  updateUserAddressApi,
  type CityItem,
  type UserAddress
} from '@/api';
import UserCenterScaffold from '@/components/UserCenterScaffold.vue';
import { showConfirmDialog } from '@/utils/confirm';

const loading = ref(false);
const dialogVisible = ref(false);
const saving = ref(false);
const regionLoading = ref(false);
const formRef = ref<FormInstance>();
const addressList = ref<UserAddress[]>([]);
const editingId = ref<number | null>(null);

const form = reactive({
  realName: '',
  phone: '',
  provinceId: null as number | null,
  cityId: null as number | null,
  districtId: null as number | null,
  detail: '',
  postCode: '',
  isDefault: false
});

const provinceOptions = ref<CityItem[]>([]);
const cityOptions = ref<CityItem[]>([]);
const districtOptions = ref<CityItem[]>([]);

const rules: FormRules = {
  realName: [{ required: true, message: '请输入收货人姓名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  provinceId: [{ required: true, message: '请选择省份', trigger: 'change' }],
  cityId: [{ required: true, message: '请选择城市', trigger: 'change' }],
  districtId: [{ required: true, message: '请选择区县', trigger: 'change' }],
  detail: [{ required: true, message: '请输入详细地址', trigger: 'blur' }]
};

const sortedAddressList = computed(() => {
  return [...addressList.value].sort((a, b) => Number(b.isDefault || 0) - Number(a.isDefault || 0));
});

const loadRegionOptions = async (regionType: number, parentId?: number) => {
  const res = await getCityListApi({ regionType, parentId });
  return res.data || [];
};

const fetchAddressList = async () => {
  loading.value = true;
  try {
    const res = await getUserAddressListApi({ page: 1, limit: 200 });
    addressList.value = res.data?.rows || [];
  } finally {
    loading.value = false;
  }
};

const resetForm = () => {
  editingId.value = null;
  form.realName = '';
  form.phone = '';
  form.provinceId = null;
  form.cityId = null;
  form.districtId = null;
  form.detail = '';
  form.postCode = '';
  form.isDefault = addressList.value.length === 0;
  cityOptions.value = [];
  districtOptions.value = [];
};

const ensureProvinceOptions = async () => {
  if (provinceOptions.value.length) return;
  regionLoading.value = true;
  try {
    provinceOptions.value = await loadRegionOptions(1, 0);
  } finally {
    regionLoading.value = false;
  }
};

const onProvinceChange = async (value: string | number) => {
  const nextId = Number(value || 0);
  form.cityId = null;
  form.districtId = null;
  districtOptions.value = [];
  if (!nextId) {
    cityOptions.value = [];
    return;
  }
  regionLoading.value = true;
  try {
    cityOptions.value = await loadRegionOptions(2, nextId);
  } finally {
    regionLoading.value = false;
  }
};

const onCityChange = async (value: string | number) => {
  const nextId = Number(value || 0);
  form.districtId = null;
  if (!nextId) {
    districtOptions.value = [];
    return;
  }
  regionLoading.value = true;
  try {
    districtOptions.value = await loadRegionOptions(3, nextId);
  } finally {
    regionLoading.value = false;
  }
};

const openCreateDialog = async () => {
  resetForm();
  dialogVisible.value = true;
  await ensureProvinceOptions();
};

const openEditDialog = async (item: UserAddress) => {
  resetForm();
  dialogVisible.value = true;
  await ensureProvinceOptions();

  editingId.value = item.id;
  form.realName = item.realName || '';
  form.phone = item.phone || '';
  form.provinceId = Number(item.provinceId || 0) || null;
  form.cityId = Number(item.cityId || 0) || null;
  form.districtId = Number(item.districtId || 0) || null;
  form.detail = item.detail || '';
  form.postCode = item.postCode || '';
  form.isDefault = Number(item.isDefault || 0) === 1;

  if (form.provinceId) {
    cityOptions.value = await loadRegionOptions(2, form.provinceId);
  }
  if (form.cityId) {
    districtOptions.value = await loadRegionOptions(3, form.cityId);
  }
};

const saveAddress = async () => {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;

  const province = provinceOptions.value.find(item => item.cityId === form.provinceId);
  const city = cityOptions.value.find(item => item.cityId === form.cityId);
  const district = districtOptions.value.find(item => item.cityId === form.districtId);

  if (!province || !city || !district) {
    ElMessage.warning('请选择完整的省市区');
    return;
  }

  const payload = {
    realName: form.realName.trim(),
    phone: form.phone.trim(),
    provinceId: province.cityId,
    province: province.name,
    cityId: String(city.cityId),
    city: city.name,
    districtId: String(district.cityId),
    district: district.name,
    detail: form.detail.trim(),
    postCode: form.postCode.trim() || undefined,
    isDefault: form.isDefault ? 1 : 0
  };

  saving.value = true;
  try {
    if (editingId.value) {
      await updateUserAddressApi({ id: editingId.value, ...payload });
      ElMessage.success('地址已更新');
    } else {
      await createUserAddressApi(payload);
      ElMessage.success('地址已新增');
    }
    dialogVisible.value = false;
    await fetchAddressList();
  } finally {
    saving.value = false;
  }
};

const removeAddress = async (item: UserAddress) => {
  try {
    await showConfirmDialog(`确认删除收货地址“${item.realName}”吗？`, '提示', { type: 'warning' });
  } catch {
    return;
  }

  await deleteUserAddressApi([item.id]);
  ElMessage.success('地址已删除');
  await fetchAddressList();
};

onMounted(() => {
  void fetchAddressList();
});
</script>

<template>
  <UserCenterScaffold
    active-menu="address"
    crumb-label="收货地址"
    title="收货地址"
  >
    <template #toolbar-extra>
      <el-button type="danger" @click="openCreateDialog">新增地址</el-button>
    </template>

    <div class="address-page" v-loading="loading">
      <div v-if="sortedAddressList.length" class="address-list">
        <article v-for="item in sortedAddressList" :key="item.id" class="address-card">
          <div class="address-head">
            <div class="address-title">
              <strong>{{ item.realName }}</strong>
              <span>{{ item.phone }}</span>
              <el-tag v-if="Number(item.isDefault || 0) === 1" size="small" type="danger" effect="plain">默认地址</el-tag>
            </div>
            <div class="address-actions">
              <el-button link type="danger" @click="openEditDialog(item)">编辑</el-button>
              <el-button link @click="removeAddress(item)">删除</el-button>
            </div>
          </div>
          <p class="address-text">{{ [item.province, item.city, item.district, item.detail].filter(Boolean).join(' ') }}</p>
          <p class="address-meta">邮编：{{ item.postCode || '-' }}</p>
        </article>
      </div>
      <el-empty v-else description="暂无收货地址，点击右上角新增地址即可开始使用。" />
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑收货地址' : '新增收货地址'"
      width="720px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="address-form">
        <div class="form-grid">
          <el-form-item label="收货人" prop="realName">
            <el-input v-model="form.realName" placeholder="请输入收货人姓名" />
          </el-form-item>
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="form.phone" placeholder="请输入手机号" maxlength="11" />
          </el-form-item>
        </div>

        <div class="form-grid region-grid" v-loading="regionLoading">
          <el-form-item label="省份" prop="provinceId">
            <el-select v-model="form.provinceId" placeholder="请选择省份" filterable clearable @change="onProvinceChange">
              <el-option v-for="item in provinceOptions" :key="item.cityId" :label="item.name" :value="item.cityId" />
            </el-select>
          </el-form-item>
          <el-form-item label="城市" prop="cityId">
            <el-select v-model="form.cityId" placeholder="请选择城市" filterable clearable :disabled="!form.provinceId" @change="onCityChange">
              <el-option v-for="item in cityOptions" :key="item.cityId" :label="item.name" :value="item.cityId" />
            </el-select>
          </el-form-item>
          <el-form-item label="区县" prop="districtId">
            <el-select v-model="form.districtId" placeholder="请选择区县" filterable clearable :disabled="!form.cityId">
              <el-option v-for="item in districtOptions" :key="item.cityId" :label="item.name" :value="item.cityId" />
            </el-select>
          </el-form-item>
          <el-form-item label="邮编">
            <el-input v-model="form.postCode" placeholder="请输入邮编" maxlength="6" />
          </el-form-item>
        </div>

        <el-form-item label="详细地址" prop="detail">
          <el-input v-model="form.detail" type="textarea" :rows="4" maxlength="120" show-word-limit placeholder="请输入门牌号、楼栋、单元等详细地址" />
        </el-form-item>

        <el-form-item>
          <el-checkbox v-model="form.isDefault">设为默认地址</el-checkbox>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="saving" @click="saveAddress">保存</el-button>
      </template>
    </el-dialog>
  </UserCenterScaffold>
</template>

<style scoped lang="scss">
.address-list {
  display: grid;
  gap: 14px;
}

.address-card {
  border: 1px solid transparent;
  box-shadow: var(--kc-shadow-sm);
  border-radius: 18px;
  padding: 16px 18px;
  background: var(--kc-surface-muted);
}

.address-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.address-title {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  color: #111827;
}

.address-actions {
  flex-shrink: 0;
}

.address-text {
  margin: 12px 0 0;
  color: #334155;
  line-height: 1.8;
}

.address-meta {
  margin: 8px 0 0;
  color: #94a3b8;
  font-size: 13px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.region-grid {
  margin-top: 2px;
}

@media (max-width: 900px) {
  .address-head {
    flex-direction: column;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>


