<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import {
  createOrderApi,
  createUserAddressApi,
  getCityListApi,
  getPreOrderDetailApi,
  getUserAddressListApi,
  recalculatePreOrderApi,
  type CityItem,
  type PreOrderDetail,
  type UserAddress
} from '@/api';
import SiteHeader from '@/components/SiteHeader.vue';
import SiteFooter from '@/components/SiteFooter.vue';
import { useAuthStore } from '@/stores/modules/auth';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

const loading = ref(false);
const recalculating = ref(false);
const submitting = ref(false);
const couponDialogVisible = ref(false);
const showAllAddress = ref(false);
const addressDialogVisible = ref(false);
const addressSubmitting = ref(false);
const regionLoading = ref(false);
const streetLoading = ref(false);

const orderNo = ref('');
const preOrder = ref<PreOrderDetail | null>(null);
const addressList = ref<UserAddress[]>([]);
const selectedAddressId = ref<number | null>(null);
const selectedCouponId = ref<number | null>(null);
const couponDraftId = ref<number | null>(null);
const useIntegral = ref(false);
const integralCapacity = ref(0);
const remainingSeconds = ref(0);

let countdownTimer: number | null = null;
let flashExpiredNotified = false;

const addressFormRef = ref<FormInstance>();
const addressForm = reactive({
  realName: '',
  phone: '',
  provinceCityId: null as number | null,
  cityCityId: null as number | null,
  districtCityId: null as number | null,
  streetCityId: null as number | null,
  streetText: '',
  detail: '',
  isDefault: false
});

const provinceOptions = ref<CityItem[]>([]);
const cityOptions = ref<CityItem[]>([]);
const districtOptions = ref<CityItem[]>([]);
const streetOptions = ref<CityItem[]>([]);

const addressRules: FormRules = {
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  provinceCityId: [{ required: true, message: '请选择省份', trigger: 'change' }],
  cityCityId: [{ required: true, message: '请选择城市', trigger: 'change' }],
  districtCityId: [{ required: true, message: '请选择区县', trigger: 'change' }],
  detail: [{ required: true, message: '请输入详细地址', trigger: 'blur' }]
};

const orderItems = computed(() => preOrder.value?.orderInfoList || []);

const availableCoupons = computed(() => {
  const list = preOrder.value?.platCouponUserList || [];
  return list.filter(item => Number(item.status ?? 0) === 0);
});

const selectedCoupon = computed(() => {
  return availableCoupons.value.find(item => item.id === selectedCouponId.value) || null;
});

const defaultAddress = computed(() => {
  return addressList.value.find(item => Number(item.isDefault ?? 0) === 1) || addressList.value[0] || null;
});

const selectedAddress = computed(() => {
  return addressList.value.find(item => item.id === selectedAddressId.value) || null;
});

const mainAddress = computed(() => selectedAddress.value || defaultAddress.value);

const otherAddresses = computed(() => {
  const activeId = mainAddress.value?.id;
  return addressList.value.filter(item => item.id !== activeId);
});

const goodsTotal = computed(() => {
  return orderItems.value.reduce((sum, item) => {
    return sum + parseMoney(item.price) * Number(item.cartNum || 0);
  }, 0);
});

const freightFee = computed(() => parseMoney(preOrder.value?.freightTotalFee));
const finalTotal = computed(() => parseMoney(preOrder.value?.finalTotalFee));
const isFlashOrder = computed(() => Number(preOrder.value?.orderType || 0) === 1);
const orderTypeLabel = computed(() => (isFlashOrder.value ? '闪购订单' : '常规订单'));
const canSubmitOrder = computed(
  () => selectedAddressId.value !== null && !recalculating.value && (!isFlashOrder.value || remainingSeconds.value > 0)
);
const couponDiscount = computed(() => parseMoney(preOrder.value?.couponPrice));
const integralDiscount = computed(() => parseMoney(preOrder.value?.deductionPrice));
const canUseIntegral = computed(() => integralCapacity.value > 0);

const integralLabel = computed(() => {
  const points = useIntegral.value ? Number(preOrder.value?.userIntegral || 0) : integralCapacity.value;
  return `${points} 积分（10000 积分 = ¥1）`;
});

const showStreetSelect = computed(() => {
  return Boolean(addressForm.districtCityId) && (streetLoading.value || streetOptions.value.length > 0);
});

const showStreetEmptyTip = computed(() => {
  return Boolean(addressForm.districtCityId) && !streetLoading.value && streetOptions.value.length === 0;
});

const normalizePreOrder = (data?: Partial<PreOrderDetail> | null): PreOrderDetail => {
  return {
    orderNo: String(data?.orderNo || ''),
    addressId: data?.addressId ?? null,
    freightTotalFee: String(data?.freightTotalFee ?? '0'),
    payFee: String(data?.payFee ?? '0'),
    isUseIntegral: Boolean(data?.isUseIntegral),
    userIntegral: Number(data?.userIntegral ?? 0),
    isUseCoupon: Boolean(data?.isUseCoupon),
    couponPrice: String(data?.couponPrice ?? '0'),
    deductionPrice: String(data?.deductionPrice ?? '0'),
    finalTotalFee: String(data?.finalTotalFee ?? '0'),
    totalNum: Number(data?.totalNum ?? 0),
    orderType: Number(data?.orderType ?? 0),
    remainingSeconds: Number(data?.remainingSeconds ?? 0),
    orderInfoList: Array.isArray(data?.orderInfoList) ? data.orderInfoList : [],
    platCouponUserList: Array.isArray(data?.platCouponUserList) ? data.platCouponUserList : []
  };
};

const parseOrderNo = () => {
  const raw = route.query.orderNo;
  if (Array.isArray(raw)) return raw[0] || '';
  return typeof raw === 'string' ? raw : '';
};

const parseMoney = (value: unknown) => {
  const num = Number(value ?? 0);
  return Number.isFinite(num) ? num : 0;
};

const toNum = (value: unknown) => {
  const num = Number(value ?? 0);
  return Number.isFinite(num) ? num : 0;
};

const money = (value: unknown) => `¥${parseMoney(value).toFixed(2)}`;

const resolveAddressId = (defaultId: number | null | undefined, list: UserAddress[]) => {
  if (defaultId && list.some(item => item.id === defaultId)) return defaultId;
  const defaultItem = list.find(item => Number(item.isDefault ?? 0) === 1);
  if (defaultItem) return defaultItem.id;
  return list[0]?.id ?? null;
};

const formatAddress = (item: UserAddress) => {
  return [item.province, item.city, item.district, item.detail].filter(Boolean).join('') || '-';
};

const formatTime = (value?: string) => {
  if (!value) return '-';
  return String(value).replace('T', ' ');
};

const formatCountdown = (seconds: number) => {
  const safe = Math.max(0, Number(seconds || 0));
  const minutes = String(Math.floor(safe / 60)).padStart(2, '0');
  const secs = String(safe % 60).padStart(2, '0');
  return `${minutes}:${secs}`;
};

const stopCountdown = () => {
  if (countdownTimer !== null) {
    window.clearInterval(countdownTimer);
    countdownTimer = null;
  }
};

const startCountdown = () => {
  stopCountdown();
  if (!isFlashOrder.value || remainingSeconds.value <= 0) return;
  countdownTimer = window.setInterval(() => {
    if (remainingSeconds.value <= 1) {
      remainingSeconds.value = 0;
      stopCountdown();
      if (!flashExpiredNotified) {
        flashExpiredNotified = true;
        ElMessage.warning('闪购下单时间已到，请重新抢购');
      }
      return;
    }
    remainingSeconds.value -= 1;
  }, 1000);
};

const syncCountdown = (seconds: number) => {
  remainingSeconds.value = Math.max(0, Number(seconds || 0));
  flashExpiredNotified = false;
  startCountdown();
};

const toCart = () => router.push('/cart');

const fetchAddressList = async () => {
  const res = await getUserAddressListApi({ page: 1, limit: 200 });
  addressList.value = res.data?.rows || [];
};

const recalculateOrder = async () => {
  if (!orderNo.value || selectedAddressId.value === null) return;
  recalculating.value = true;
  try {
    const res = await recalculatePreOrderApi({
      preOrderNo: orderNo.value,
      addressId: selectedAddressId.value,
      isUseIntegral: useIntegral.value,
      platUserCouponId: selectedCouponId.value || undefined
    });
    preOrder.value = normalizePreOrder(res.data);
    syncCountdown(preOrder.value.remainingSeconds || 0);
  } finally {
    recalculating.value = false;
  }
};

const loadPage = async () => {
  const queryOrderNo = parseOrderNo();
  if (!queryOrderNo) {
    ElMessage.error('缺少订单号，请从购物车重新结算');
    router.replace('/cart');
    return;
  }
  orderNo.value = queryOrderNo;
  loading.value = true;
  try {
    const detailRes = await getPreOrderDetailApi(orderNo.value);
    preOrder.value = normalizePreOrder(detailRes.data);
    syncCountdown(preOrder.value.remainingSeconds || 0);
    await fetchAddressList();
    integralCapacity.value = Math.max(0, Number(preOrder.value.userIntegral || 0));
    selectedAddressId.value = resolveAddressId(preOrder.value.addressId, addressList.value);
    useIntegral.value = false;
    selectedCouponId.value = null;
    couponDraftId.value = null;

    if (selectedAddressId.value === null) {
      ElMessage.warning('请先添加收货地址');
      return;
    }

    if (preOrder.value.addressId !== selectedAddressId.value) {
      await recalculateOrder();
    }
  } finally {
    loading.value = false;
  }
};

const selectAddress = async (id: number) => {
  if (id === selectedAddressId.value) return;
  selectedAddressId.value = id;
  showAllAddress.value = false;
  await recalculateOrder();
};

const toggleIntegral = async (value: boolean | string | number) => {
  const next = Boolean(value);
  if (!next && useIntegral.value === false) return;
  if (next && !canUseIntegral.value) return;
  if (next === useIntegral.value) return;
  useIntegral.value = next;
  await recalculateOrder();
};

const openCouponDialog = () => {
  if (selectedAddressId.value === null) {
    ElMessage.warning('请先添加收货地址后再选择优惠券');
    return;
  }
  couponDraftId.value = selectedCouponId.value;
  couponDialogVisible.value = true;
};

const pickCouponDraft = (id: number | null) => {
  couponDraftId.value = id;
};

const confirmCoupon = async () => {
  couponDialogVisible.value = false;
  if (couponDraftId.value === selectedCouponId.value) return;
  selectedCouponId.value = couponDraftId.value;
  await recalculateOrder();
};

const submitOrder = async () => {
  if (!preOrder.value) return;
  if (isFlashOrder.value && remainingSeconds.value <= 0) {
    ElMessage.warning('闪购订单已超时，请重新抢购');
    return;
  }
  if (selectedAddressId.value === null) {
    ElMessage.warning('请先选择收货地址');
    return;
  }
  submitting.value = true;
  try {
    const res = await createOrderApi({
      addressId: String(selectedAddressId.value),
      isUseIntegral: useIntegral.value,
      platUserCouponId: selectedCouponId.value || undefined,
      preOrderNo: orderNo.value
    });
    const nextOrderNo = res.data?.orderNo || orderNo.value;
    ElMessage.success(`下单成功，订单号：${nextOrderNo}`);
    router.replace({ path: '/order_payment', query: { orderNo: nextOrderNo } });
  } finally {
    submitting.value = false;
  }
};

const loadCityOptions = async (regionType: number, parentId?: number) => {
  const res = await getCityListApi({ regionType, parentId });
  return res.data || [];
};

const onAddressDialogOpen = async () => {
  if (!provinceOptions.value.length) {
    regionLoading.value = true;
    try {
      provinceOptions.value = await loadCityOptions(1, 0);
    } finally {
      regionLoading.value = false;
    }
  }
};

const resetAddressForm = () => {
  addressForm.realName = '';
  addressForm.phone = '';
  addressForm.provinceCityId = null;
  addressForm.cityCityId = null;
  addressForm.districtCityId = null;
  addressForm.streetCityId = null;
  addressForm.streetText = '';
  addressForm.detail = '';
  addressForm.isDefault = addressList.value.length === 0;
  cityOptions.value = [];
  districtOptions.value = [];
  streetOptions.value = [];
  streetLoading.value = false;
};

const openAddressDialog = async () => {
  resetAddressForm();
  addressDialogVisible.value = true;
  await onAddressDialogOpen();
};

const onProvinceChange = async (value: string | number) => {
  const cityId = toNum(value);
  addressForm.cityCityId = null;
  addressForm.districtCityId = null;
  addressForm.streetCityId = null;
  addressForm.streetText = '';
  districtOptions.value = [];
  streetOptions.value = [];
  streetLoading.value = false;
  if (!cityId) {
    cityOptions.value = [];
    return;
  }
  regionLoading.value = true;
  try {
    cityOptions.value = await loadCityOptions(2, cityId);
  } finally {
    regionLoading.value = false;
  }
};

const onCityChange = async (value: string | number) => {
  const cityId = toNum(value);
  addressForm.districtCityId = null;
  addressForm.streetCityId = null;
  addressForm.streetText = '';
  streetOptions.value = [];
  streetLoading.value = false;
  if (!cityId) {
    districtOptions.value = [];
    return;
  }
  regionLoading.value = true;
  try {
    districtOptions.value = await loadCityOptions(3, cityId);
  } finally {
    regionLoading.value = false;
  }
};

const onDistrictChange = async (value: string | number) => {
  const cityId = toNum(value);
  addressForm.streetCityId = null;
  addressForm.streetText = '';
  if (!cityId) {
    streetOptions.value = [];
    streetLoading.value = false;
    return;
  }
  streetLoading.value = true;
  try {
    streetOptions.value = await loadCityOptions(4, cityId);
  } finally {
    streetLoading.value = false;
  }
};

const saveAddress = async () => {
  const valid = await addressFormRef.value?.validate().catch(() => false);
  if (!valid) return;

  const uid = Number(authStore.userInfo?.uid || 0);
  if (!uid) {
    ElMessage.error('用户信息失效，请重新登录');
    return;
  }

  const province = provinceOptions.value.find(item => item.cityId === addressForm.provinceCityId) || null;
  const city = cityOptions.value.find(item => item.cityId === addressForm.cityCityId) || null;
  const district = districtOptions.value.find(item => item.cityId === addressForm.districtCityId) || null;
  const street = streetOptions.value.find(item => item.cityId === addressForm.streetCityId) || null;

  if (!province || !city || !district) {
    ElMessage.warning('请选择完整的省市区');
    return;
  }

  const manualStreet = addressForm.streetText.trim();
  const streetPrefix = (street?.name || manualStreet).trim();
  const fullDetail = `${streetPrefix}${addressForm.detail}`.trim();

  addressSubmitting.value = true;
  try {
    await createUserAddressApi({
      uid,
      realName: addressForm.realName.trim(),
      phone: addressForm.phone.trim(),
      provinceId: province.cityId,
      province: province.name,
      cityId: String(city.cityId),
      city: city.name,
      districtId: String(district.cityId),
      district: district.name,
      detail: fullDetail,
      postCode: '',
      isDefault: addressForm.isDefault ? 1 : 0
    });

    const previousSelected = selectedAddressId.value;
    await fetchAddressList();
    if (addressForm.isDefault) {
      selectedAddressId.value = resolveAddressId(null, addressList.value);
    } else if (previousSelected && addressList.value.some(item => item.id === previousSelected)) {
      selectedAddressId.value = previousSelected;
    } else {
      selectedAddressId.value = resolveAddressId(null, addressList.value);
    }
    addressDialogVisible.value = false;
    ElMessage.success('地址已添加');
    await recalculateOrder();
  } finally {
    addressSubmitting.value = false;
  }
};

onMounted(() => {
  loadPage();
});

onBeforeUnmount(() => {
  stopCountdown();
});
</script>

<template>
  <div class="kc-page" v-loading="loading">
    <SiteHeader mode="checkout" :checkout-step="2" back-label="返回购物车" @back="toCart" />

    <main class="kc-main confirm-main">
      <template v-if="preOrder">
        <section class="status-banner" :class="{ expired: isFlashOrder && remainingSeconds <= 0 }">
          <div class="banner-left">
            <span class="type-pill kc-mono">{{ orderTypeLabel }}</span>
            <div>
              <h1>{{ isFlashOrder ? '请在倒计时结束前完成闪购下单' : '外设清单已锁定，请核对配置与收件信息' }}</h1>
              <p>{{ isFlashOrder ? '超时后系统将自动释放闪购库存，需重新进入商品页抢购。' : '确认收货地址及优惠抵扣后即可生成订单并前往支付。' }}</p>
            </div>
          </div>
          <div v-if="isFlashOrder" class="banner-timer kc-mono">
            <span>REMAINING</span>
            <strong>{{ formatCountdown(remainingSeconds) }}</strong>
          </div>
        </section>

        <div class="confirm-grid">
          <div class="confirm-left">
            <section class="block kc-surface">
              <div class="block-head">
                <h2>收货地址</h2>
                <button type="button" class="text-btn" @click="openAddressDialog">+ 新增地址</button>
              </div>

              <div class="address-main-grid">
                <button
                  v-if="mainAddress"
                  type="button"
                  class="address-card active"
                  @click="mainAddress.id && selectAddress(mainAddress.id)"
                >
                  <div class="addr-top">
                    <strong>{{ mainAddress.realName }}</strong>
                    <span class="kc-mono">{{ mainAddress.phone }}</span>
                    <span v-if="Number(mainAddress.isDefault ?? 0) === 1" class="default-tag">默认</span>
                  </div>
                  <div class="addr-detail">{{ formatAddress(mainAddress) }}</div>
                </button>
                <div v-else class="empty-line">暂无收货地址，请点击右上角新增地址</div>
              </div>

              <div v-if="otherAddresses.length > 0" class="more-wrap">
                <button type="button" class="more-toggle" @click="showAllAddress = !showAllAddress">
                  <span>{{ showAllAddress ? '收起其他地址' : `展开其他 ${otherAddresses.length} 个地址` }}</span>
                  <el-icon v-if="showAllAddress"><ArrowUp /></el-icon>
                  <el-icon v-else><ArrowDown /></el-icon>
                </button>
              </div>

              <div v-if="showAllAddress && otherAddresses.length > 0" class="other-address-grid">
                <button
                  v-for="item in otherAddresses"
                  :key="item.id"
                  type="button"
                  class="address-card"
                  @click="selectAddress(item.id)"
                >
                  <div class="addr-top">
                    <strong>{{ item.realName }}</strong>
                    <span class="kc-mono">{{ item.phone }}</span>
                    <span v-if="Number(item.isDefault ?? 0) === 1" class="default-tag">默认</span>
                  </div>
                  <div class="addr-detail">{{ formatAddress(item) }}</div>
                </button>
              </div>
            </section>

            <section class="block kc-surface">
              <div class="block-head">
                <h2>外设清单</h2>
                <span class="kc-mono item-count">{{ orderItems.length }} SKUS</span>
              </div>

              <div class="goods-list">
                <div v-for="item in orderItems" :key="item.id" class="goods-row">
                  <img :src="item.image" :alt="item.productName || '商品图片'" />
                  <div class="goods-copy">
                    <div class="goods-name">{{ item.productName || '-' }}</div>
                    <span class="goods-sku kc-mono">{{ item.sku || '标准配置' }}</span>
                  </div>
                  <div class="goods-price kc-mono">{{ money(item.price) }}</div>
                  <div class="goods-qty kc-mono">×{{ item.cartNum }}</div>
                  <div class="goods-sub kc-mono">{{ money(parseMoney(item.price) * Number(item.cartNum || 0)) }}</div>
                </div>
              </div>
            </section>

            <section class="block kc-surface">
              <div class="block-head">
                <h2>优惠与积分抵扣</h2>
              </div>

              <div class="benefit-row">
                <span>工坊优惠券</span>
                <button type="button" class="coupon-trigger" @click="openCouponDialog">
                  <span class="kc-mono">{{ selectedCoupon ? `-${money(couponDiscount)}` : '选择可用优惠券' }}</span>
                  <el-icon><ArrowRight /></el-icon>
                </button>
              </div>

              <div class="benefit-row">
                <span>造物积分抵扣</span>
                <el-radio-group :model-value="useIntegral" @change="toggleIntegral">
                  <el-radio :label="false">不使用积分</el-radio>
                  <el-radio :label="true" :disabled="!canUseIntegral">{{ integralLabel }}</el-radio>
                </el-radio-group>
              </div>
            </section>
          </div>

          <aside class="summary-dock kc-surface">
            <span class="kc-kicker">ORDER SUMMARY</span>
            <h3>费用明细</h3>

            <div class="summary-lines">
              <div class="summary-line">
                <span>商品总额</span>
                <span class="kc-mono">{{ money(goodsTotal) }}</span>
              </div>
              <div class="summary-line">
                <span>物流运费</span>
                <span class="kc-mono">{{ money(freightFee) }}</span>
              </div>
              <div class="summary-line">
                <span>优惠券抵扣</span>
                <span class="kc-mono discount">-{{ money(couponDiscount) }}</span>
              </div>
              <div class="summary-line">
                <span>积分抵扣</span>
                <span class="kc-mono discount">-{{ money(integralDiscount) }}</span>
              </div>
              <div class="summary-line">
                <span>订单类型</span>
                <span>{{ orderTypeLabel }}</span>
              </div>
            </div>

            <div class="summary-total">
              <span>应付总额</span>
              <strong class="kc-mono">{{ money(finalTotal) }}</strong>
            </div>

            <p v-if="recalculating" class="recalc-tip kc-mono">RECALCULATING...</p>

            <el-button
              type="danger"
              class="submit-btn"
              :loading="submitting"
              :disabled="!canSubmitOrder"
              @click="submitOrder"
            >
              {{ isFlashOrder ? '提交闪购订单' : '确认并提交订单' }}
            </el-button>
          </aside>
        </div>
      </template>
    </main>

    <SiteFooter />

    <el-dialog v-model="addressDialogVisible" title="新增收货地址" width="720px" @open="onAddressDialogOpen">
      <el-form ref="addressFormRef" :model="addressForm" :rules="addressRules" label-position="top">
        <div class="address-form-grid">
          <el-form-item label="收货人姓名" prop="realName">
            <el-input v-model="addressForm.realName" placeholder="请输入收货人姓名" />
          </el-form-item>
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="addressForm.phone" placeholder="请输入 11 位手机号" maxlength="11" />
          </el-form-item>
        </div>

        <div class="region-selects" v-loading="regionLoading">
          <el-form-item label="省份" prop="provinceCityId">
            <el-select v-model="addressForm.provinceCityId" placeholder="选择省份" filterable clearable @change="onProvinceChange">
              <el-option v-for="item in provinceOptions" :key="item.cityId" :label="item.name" :value="item.cityId" />
            </el-select>
          </el-form-item>

          <el-form-item label="城市" prop="cityCityId">
            <el-select
              v-model="addressForm.cityCityId"
              placeholder="选择城市"
              filterable
              clearable
              :disabled="!addressForm.provinceCityId"
              @change="onCityChange"
            >
              <el-option v-for="item in cityOptions" :key="item.cityId" :label="item.name" :value="item.cityId" />
            </el-select>
          </el-form-item>

          <el-form-item label="区县" prop="districtCityId">
            <el-select
              v-model="addressForm.districtCityId"
              placeholder="选择区县"
              filterable
              clearable
              :disabled="!addressForm.cityCityId"
              @change="onDistrictChange"
            >
              <el-option v-for="item in districtOptions" :key="item.cityId" :label="item.name" :value="item.cityId" />
            </el-select>
          </el-form-item>

          <el-form-item v-if="showStreetSelect" label="街道 (可选)">
            <el-select
              v-model="addressForm.streetCityId"
              placeholder="选择街道"
              filterable
              clearable
              :disabled="!addressForm.districtCityId"
            >
              <el-option v-for="item in streetOptions" :key="item.cityId" :label="item.name" :value="item.cityId" />
            </el-select>
          </el-form-item>

          <el-form-item v-else-if="showStreetEmptyTip" label="街道 (手动输入)">
            <el-input v-model="addressForm.streetText" placeholder="请输入街道 / 乡镇名称（可选）" clearable />
          </el-form-item>
        </div>

        <el-form-item label="详细地址" prop="detail">
          <el-input v-model="addressForm.detail" placeholder="请输入详细街道门牌号" />
        </el-form-item>

        <el-form-item>
          <el-checkbox v-model="addressForm.isDefault">设为默认收货地址</el-checkbox>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="addressDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="addressSubmitting" @click="saveAddress">保存地址</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="couponDialogVisible" title="选择工坊优惠券" width="680px">
      <div class="coupon-dialog-grid">
        <button type="button" class="dialog-coupon" :class="{ active: couponDraftId === null }" @click="pickCouponDraft(null)">
          <div class="dc-amount none">无</div>
          <div class="dc-info">
            <strong>不使用优惠券</strong>
            <span>保持当前结算金额</span>
          </div>
        </button>

        <button
          v-for="item in availableCoupons"
          :key="item.id"
          type="button"
          class="dialog-coupon"
          :class="{ active: couponDraftId === item.id }"
          @click="pickCouponDraft(item.id)"
        >
          <div class="dc-amount kc-mono">{{ money(item.money) }}</div>
          <div class="dc-info">
            <strong>{{ item.name }}</strong>
            <span class="kc-mono">有效期至 {{ formatTime(item.couponEndTime) }}</span>
          </div>
        </button>
      </div>
      <template #footer>
        <el-button @click="couponDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmCoupon">确认使用</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.confirm-main {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.status-banner {
  padding: 24px 28px;
  border-radius: 24px;
  background: var(--kc-carbon);
  color: #ffffff;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.08),
    var(--kc-shadow-lg);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}

.status-banner.expired {
  opacity: 0.72;
}

.banner-left {
  display: flex;
  align-items: flex-start;
  gap: 14px;
}

.type-pill {
  padding: 5px 11px;
  border-radius: 999px;
  background: var(--kc-accent);
  color: #ffffff;
  font-size: 11px;
  font-weight: 700;
  white-space: nowrap;
}

.banner-left h1 {
  margin: 0;
  font-size: 19px;
  line-height: 1.3;
}

.banner-left p {
  margin: 6px 0 0;
  font-size: 13px;
  color: #9ba0ae;
}

.banner-timer {
  text-align: right;
  flex-shrink: 0;
}

.banner-timer span {
  display: block;
  font-size: 10px;
  letter-spacing: 0.12em;
  color: #8c919f;
}

.banner-timer strong {
  font-size: 28px;
  color: var(--kc-accent);
}

.confirm-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 348px;
  gap: 22px;
  align-items: start;
}

.confirm-left {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.block {
  padding: 24px 26px;
  border-radius: 22px;
  border: 1px solid transparent;
  background: var(--kc-surface);
  box-shadow: var(--kc-shadow-sm);
}

.block-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.block-head h2 {
  margin: 0;
  font-size: 17px;
  color: var(--kc-text);
}

.item-count {
  padding: 4px 10px;
  border-radius: 999px;
  background: var(--kc-surface-muted);
  font-size: 11px;
  color: var(--kc-text-muted);
}

.text-btn {
  padding: 6px 14px;
  border: 0;
  border-radius: 999px;
  background: var(--kc-accent-soft);
  color: var(--kc-accent);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.address-card {
  width: 100%;
  padding: 18px 20px;
  border-radius: 16px;
  border: 1px solid transparent;
  background: var(--kc-surface-muted);
  text-align: left;
  cursor: pointer;
  transition: box-shadow 150ms ease, background-color 150ms ease;
}

.address-card.active {
  background: var(--kc-surface);
  box-shadow: 0 0 0 2px var(--kc-carbon), var(--kc-shadow-sm);
}

.addr-top {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  color: var(--kc-text);
}

.default-tag {
  padding: 2px 9px;
  border-radius: 999px;
  background: var(--kc-accent-soft);
  color: var(--kc-accent);
  font-size: 11px;
  font-weight: 700;
}

.addr-detail {
  margin-top: 8px;
  font-size: 13px;
  color: var(--kc-text-secondary);
  line-height: 1.55;
}

.empty-line {
  padding: 18px;
  border-radius: 14px;
  background: var(--kc-surface-muted);
  color: var(--kc-text-muted);
  font-size: 13px;
}

.more-wrap {
  margin-top: 12px;
}

.more-toggle {
  border: 0;
  padding: 6px 12px;
  border-radius: 999px;
  background: var(--kc-surface-muted);
  color: var(--kc-text-secondary);
  font-size: 12px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  cursor: pointer;
}

.other-address-grid {
  margin-top: 12px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.goods-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.goods-row {
  display: grid;
  grid-template-columns: 68px minmax(0, 1fr) 92px 54px 104px;
  align-items: center;
  gap: 14px;
  padding: 12px 14px;
  border-radius: 16px;
  background: var(--kc-surface-muted);
}

.goods-row img {
  width: 68px;
  height: 68px;
  object-fit: cover;
  border-radius: 10px;
  outline: 1px solid oklch(0 0 0 / 0.08);
  outline-offset: -1px;
}

.goods-copy {
  min-width: 0;
}

.goods-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--kc-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.goods-sku {
  display: inline-block;
  margin-top: 6px;
  font-size: 12px;
  color: var(--kc-text-muted);
}

.goods-price,
.goods-qty {
  font-size: 13px;
  color: var(--kc-text-secondary);
  text-align: right;
}

.goods-sub {
  font-size: 14px;
  font-weight: 700;
  color: var(--kc-text);
  text-align: right;
}

.benefit-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 16px;
  border-radius: 14px;
  background: var(--kc-surface-muted);
  font-size: 14px;
  color: var(--kc-text);
}

.benefit-row + .benefit-row {
  margin-top: 10px;
}

.coupon-trigger {
  border: 0;
  border-radius: 999px;
  background: var(--kc-surface);
  box-shadow: var(--kc-shadow-sm);
  padding: 7px 14px;
  color: var(--kc-text);
  font-size: 13px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
}

/* Sticky Summary Dock */
.summary-dock {
  position: sticky;
  top: 94px;
  padding: 26px;
  border-radius: 24px;
  border: 1px solid transparent;
  background: var(--kc-surface);
  box-shadow: var(--kc-shadow-md);
}

.summary-dock h3 {
  margin: 8px 0 0;
  font-size: 20px;
  color: var(--kc-text);
}

.summary-lines {
  margin-top: 18px;
  padding: 16px;
  border-radius: 16px;
  background: var(--kc-surface-muted);
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.summary-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
  color: var(--kc-text-secondary);
}

.summary-line .discount {
  color: var(--kc-accent);
}

.summary-total {
  margin-top: 18px;
  display: flex;
  align-items: baseline;
  justify-content: space-between;
}

.summary-total span {
  font-size: 14px;
  font-weight: 600;
  color: var(--kc-text);
}

.summary-total strong {
  font-size: 30px;
  color: var(--kc-accent);
  line-height: 1;
}

.recalc-tip {
  margin: 10px 0 0;
  font-size: 11px;
  color: var(--kc-text-muted);
}

.submit-btn {
  margin-top: 18px;
  width: 100%;
  height: 48px;
  border-radius: 999px;
  font-size: 15px;
  font-weight: 700;
}

/* Dialogs */
.address-form-grid,
.region-selects {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.coupon-dialog-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.dialog-coupon {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px;
  border-radius: 16px;
  border: 1px solid transparent;
  background: var(--kc-surface-muted);
  text-align: left;
  cursor: pointer;
}

.dialog-coupon.active {
  background: var(--kc-accent-soft);
  box-shadow: 0 0 0 2px var(--kc-accent);
}

.dc-amount {
  min-width: 74px;
  font-size: 20px;
  font-weight: 700;
  color: var(--kc-accent);
  text-align: center;
}

.dc-amount.none {
  color: var(--kc-text-muted);
}

.dc-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.dc-info strong {
  font-size: 14px;
  color: var(--kc-text);
}

.dc-info span {
  font-size: 12px;
  color: var(--kc-text-muted);
}

@media (max-width: 1024px) {
  .confirm-grid {
    grid-template-columns: 1fr;
  }

  .summary-dock {
    position: static;
  }
}

@media (max-width: 680px) {
  .other-address-grid,
  .address-form-grid,
  .region-selects,
  .coupon-dialog-grid {
    grid-template-columns: 1fr;
  }

  .goods-row {
    grid-template-columns: 56px minmax(0, 1fr);
  }

  .goods-price,
  .goods-qty,
  .goods-sub {
    text-align: left;
  }
}
</style>
