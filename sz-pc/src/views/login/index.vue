<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import { lbUserLoginApi, lbUserRegisterApi, type LbUserLoginInfo } from '@/api';
import { useAuthStore } from '@/stores/modules/auth';
import { useCartStore } from '@/stores/modules/cart';

type AuthTab = 'login' | 'register';

type RegisterForm = {
  account: string;
  password: string;
  confirmPassword: string;
  phone: string;
  realName: string;
  nickname: string;
};

type KeycapDef = {
  id: string;
  label: string;
  sub?: string;
  accent?: boolean;
  wide?: boolean;
  travel: string;
  force: string;
};

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();
const cartStore = useCartStore();

const loginFormRef = ref<FormInstance>();
const registerFormRef = ref<FormInstance>();
const loginLoading = ref(false);
const registerLoading = ref(false);
const activeTab = ref<AuthTab>(resolveTab(route.query.mode));

const loginForm = reactive({
  accountOrPhone: '',
  password: ''
});

const registerForm = reactive<RegisterForm>({
  account: '',
  password: '',
  confirmPassword: '',
  phone: '',
  realName: '',
  nickname: ''
});

/* Interactive Mechanical Stage State */
const stageRef = ref<HTMLElement | null>(null);
const pointerPos = reactive({ x: 52, y: 42, rawX: 420, rawY: 280 });
const soundEnabled = ref(false);
const pressedKeys = ref<Record<string, boolean>>({});
const rippleKey = ref('');
const actuationCount = ref(0);
const lastKeyMeta = reactive({
  label: 'K',
  travel: '2.0mm',
  force: '45gf',
  switchType: 'LINEAR // POM'
});

const keyRows: KeycapDef[][] = [
  [
    { id: 'ESC', label: 'ESC', sub: 'RST', accent: true, travel: '1.8mm', force: '50gf' },
    { id: 'K', label: 'K', sub: '01', travel: '2.0mm', force: '45gf' },
    { id: 'L', label: 'L', sub: '02', travel: '2.0mm', force: '45gf' },
    { id: 'A', label: 'A', sub: '03', travel: '2.0mm', force: '45gf' },
    { id: 'C', label: 'C', sub: '04', travel: '2.0mm', force: '45gf' },
    { id: 'K2', label: 'K', sub: '05', travel: '2.0mm', force: '45gf' }
  ],
  [
    { id: 'C2', label: 'C', sub: 'CNC', travel: '1.9mm', force: '42gf' },
    { id: 'R', label: 'R', sub: 'RGB', travel: '1.9mm', force: '42gf' },
    { id: 'A2', label: 'A', sub: 'ALU', travel: '1.9mm', force: '42gf' },
    { id: 'F', label: 'F', sub: 'FR4', travel: '1.9mm', force: '42gf' },
    { id: 'T', label: 'T', sub: 'PBT', travel: '1.9mm', force: '42gf' },
    { id: 'ENT', label: '↵', sub: 'GO', wide: true, travel: '2.0mm', force: '55gf' }
  ]
];

const allKeys = keyRows.flat();
let idleTimer: number | null = null;
let audioCtx: AudioContext | null = null;
const keyTimeouts = new Map<string, number>();

const panelTitle = computed(() => (activeTab.value === 'login' ? '欢迎回到工坊' : '注册极客账号'));
const panelSubtitle = computed(() =>
  activeTab.value === 'login'
    ? '输入你的账号凭证，同步定制方案、购物清单与订单状态。'
    : '创建你的 KlackCraft 专属身份，开启客制化外设与桌搭体验。'
);
const heroTitle = computed(() => (activeTab.value === 'login' ? '每次触发，皆为造物' : '组装你的理想桌搭'));
const heroDesc = computed(() =>
  activeTab.value === 'login'
    ? '机械键盘、轻量化鼠标与桌面声学系统——以极简工业精度，回应指尖的每一次敲击。'
    : '加入咔嗒造物商城，收藏心仪套件、追踪限量闪购并管理你的专属客制化外设订单。'
);
const redirectPath = computed(() => (typeof route.query.redirect === 'string' ? route.query.redirect : '/home'));

const stageStyle = computed(() => ({
  '--mx': `${pointerPos.x}%`,
  '--my': `${pointerPos.y}%`
}));

watch(
  () => route.query.mode,
  mode => {
    activeTab.value = resolveTab(mode);
  }
);

function playMechanicalClick(accent = false) {
  if (!soundEnabled.value) return;
  try {
    const AudioContextCtor = window.AudioContext || (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext;
    if (!AudioContextCtor) return;
    if (!audioCtx) {
      audioCtx = new AudioContextCtor();
    }
    if (audioCtx.state === 'suspended') {
      void audioCtx.resume();
    }
    const now = audioCtx.currentTime;
    const osc = audioCtx.createOscillator();
    const gain = audioCtx.createGain();
    osc.type = 'triangle';
    osc.frequency.setValueAtTime(accent ? 520 : 390, now);
    osc.frequency.exponentialRampToValueAtTime(95, now + 0.036);
    gain.gain.setValueAtTime(0.11, now);
    gain.gain.exponentialRampToValueAtTime(0.001, now + 0.04);
    osc.connect(gain);
    gain.connect(audioCtx.destination);
    osc.start(now);
    osc.stop(now + 0.042);
  } catch {
    // Ignore audio context restriction
  }
}

function triggerKey(keyDef: KeycapDef, withSound = true) {
  pressedKeys.value[keyDef.id] = true;
  rippleKey.value = keyDef.id;
  actuationCount.value += 1;
  lastKeyMeta.label = keyDef.label;
  lastKeyMeta.travel = keyDef.travel;
  lastKeyMeta.force = keyDef.force;
  lastKeyMeta.switchType = keyDef.accent ? 'TACTILE // ESC' : 'LINEAR // POM';

  if (withSound) {
    playMechanicalClick(Boolean(keyDef.accent));
  }

  const prevTimer = keyTimeouts.get(keyDef.id);
  if (prevTimer) window.clearTimeout(prevTimer);
  const timer = window.setTimeout(() => {
    pressedKeys.value[keyDef.id] = false;
    if (rippleKey.value === keyDef.id) {
      rippleKey.value = '';
    }
  }, 190);
  keyTimeouts.set(keyDef.id, timer);
}

function pulseKeyFromTyping(event?: KeyboardEvent) {
  const char = event?.key?.toUpperCase() || '';
  if (char === 'ENTER') {
    const enterKey = allKeys.find(item => item.id === 'ENT');
    if (enterKey) triggerKey(enterKey, true);
    return;
  }
  if (char === 'ESCAPE') {
    const escKey = allKeys.find(item => item.id === 'ESC');
    if (escKey) triggerKey(escKey, true);
    return;
  }
  const matched = allKeys.find(item => item.label === char);
  if (matched) {
    triggerKey(matched, true);
    return;
  }
  const index = (actuationCount.value + char.length) % (allKeys.length - 2) + 1;
  triggerKey(allKeys[index], true);
}

function handleStageMove(event: MouseEvent) {
  if (!stageRef.value) return;
  const rect = stageRef.value.getBoundingClientRect();
  if (!rect.width || !rect.height) return;
  const relX = Math.max(0, Math.min(1, (event.clientX - rect.left) / rect.width));
  const relY = Math.max(0, Math.min(1, (event.clientY - rect.top) / rect.height));
  pointerPos.x = Math.round(relX * 100);
  pointerPos.y = Math.round(relY * 100);
  pointerPos.rawX = Math.round(event.clientX - rect.left);
  pointerPos.rawY = Math.round(event.clientY - rect.top);
}

function toggleSound() {
  soundEnabled.value = !soundEnabled.value;
  if (soundEnabled.value) {
    playMechanicalClick(true);
  }
}

const startIdlePulse = () => {
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return;
  let seq = 1;
  idleTimer = window.setInterval(() => {
    if (document.hidden) return;
    const key = allKeys[seq % allKeys.length];
    triggerKey(key, false);
    seq += 1;
  }, 2400);
};

onMounted(() => {
  startIdlePulse();
});

onBeforeUnmount(() => {
  if (idleTimer !== null) {
    window.clearInterval(idleTimer);
    idleTimer = null;
  }
  keyTimeouts.forEach(timer => window.clearTimeout(timer));
  keyTimeouts.clear();
  if (audioCtx) {
    void audioCtx.close();
    audioCtx = null;
  }
});

/* Validation & Auth Logic (100% Preserved) */
const phoneValidator = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (!value) {
    callback(new Error('请输入手机号'));
    return;
  }
  if (!/^1[3-9]\d{9}$/.test(value)) {
    callback(new Error('请输入正确的手机号'));
    return;
  }
  callback();
};

const confirmPasswordValidator = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (!value) {
    callback(new Error('请再次输入密码'));
    return;
  }
  if (value !== registerForm.password) {
    callback(new Error('两次输入的密码不一致'));
    return;
  }
  callback();
};

const loginRules: FormRules = {
  accountOrPhone: [{ required: true, message: '请输入账号或手机号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
};

const registerRules: FormRules = {
  account: [{ required: true, message: '请输入注册账号', trigger: 'blur' }],
  phone: [{ validator: phoneValidator, trigger: 'blur' }],
  nickname: [{ max: 20, message: '昵称不超过 20 个字符', trigger: 'blur' }],
  realName: [{ max: 20, message: '真实姓名不超过 20 个字符', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少 6 位', trigger: 'blur' }
  ],
  confirmPassword: [{ validator: confirmPasswordValidator, trigger: 'blur' }]
};

function resolveTab(mode: unknown): AuthTab {
  return mode === 'register' ? 'register' : 'login';
}

function sanitizeOptional(value: string) {
  const next = value.trim();
  return next ? next : undefined;
}

function syncAuthState(user: LbUserLoginInfo) {
  authStore.updateToken(user.accessToken);
  authStore.updateUserInfo({
    uid: user.uid,
    account: user.account,
    username: user.account,
    nickname: user.nickname,
    realName: user.realName,
    phone: user.phone,
    avatar: user.avatar,
    level: user.level,
    integral: user.integral,
    experience: user.experience,
    nowMoney: user.nowMoney,
    userType: user.userType,
    createTime: user.createTime
  });
}

async function finishLogin(user: LbUserLoginInfo, message: string) {
  syncAuthState(user);
  await cartStore.fetchCount();
  ElMessage.success(message);
  await router.replace(redirectPath.value);
}

async function handleLoginSubmit() {
  const valid = await loginFormRef.value?.validate().catch(() => false);
  if (!valid) return;

  const enterKey = allKeys.find(item => item.id === 'ENT');
  if (enterKey) triggerKey(enterKey, true);

  loginLoading.value = true;
  try {
    const response = await lbUserLoginApi({
      accountOrPhone: loginForm.accountOrPhone.trim(),
      password: loginForm.password
    });
    await finishLogin(response.data, '登录成功');
  } finally {
    loginLoading.value = false;
  }
}

async function handleRegisterSubmit() {
  const valid = await registerFormRef.value?.validate().catch(() => false);
  if (!valid) return;

  const enterKey = allKeys.find(item => item.id === 'ENT');
  if (enterKey) triggerKey(enterKey, true);

  registerLoading.value = true;
  const payload = {
    account: registerForm.account.trim(),
    password: registerForm.password,
    phone: registerForm.phone.trim(),
    realName: sanitizeOptional(registerForm.realName),
    nickname: sanitizeOptional(registerForm.nickname)
  };

  try {
    await lbUserRegisterApi(payload);
    const loginResponse = await lbUserLoginApi({
      accountOrPhone: payload.account,
      password: payload.password
    });
    await finishLogin(loginResponse.data, '注册成功，已自动登录');
  } catch (error) {
    loginForm.accountOrPhone = payload.account;
    loginForm.password = payload.password;
    await switchTab('login');
    throw error;
  } finally {
    registerLoading.value = false;
  }
}

async function switchTab(tab: AuthTab) {
  activeTab.value = tab;
  loginFormRef.value?.clearValidate();
  registerFormRef.value?.clearValidate();
  triggerKey(tab === 'login' ? allKeys[1] : allKeys[6], true);

  const query = { ...route.query };
  if (tab === 'register') {
    query.mode = 'register';
  } else {
    delete query.mode;
  }

  if (route.query.mode === query.mode) return;
  await router.replace({
    path: '/login',
    query
  });
}

const goHome = () => {
  router.push('/home');
};
</script>

<template>
  <div class="auth-studio">
    <section
      ref="stageRef"
      class="industrial-stage"
      :style="stageStyle"
      @mousemove="handleStageMove"
    >
      <div class="stage-grid" aria-hidden="true" />
      <div class="stage-spotlight" aria-hidden="true" />

      <header class="stage-top">
        <button type="button" class="stage-back" @click="goHome">
          <el-icon><ArrowLeft /></el-icon>
          <span>返回工坊</span>
        </button>

        <div class="stage-tools">
          <button
            type="button"
            class="sound-toggle kc-mono"
            :class="{ active: soundEnabled }"
            @click="toggleSound"
          >
            <span class="led" />
            <span>CLACK AUDIO: {{ soundEnabled ? 'ON' : 'OFF' }}</span>
          </button>
          <span class="coord-chip kc-mono">
            X:{{ String(pointerPos.rawX).padStart(3, '0') }} Y:{{ String(pointerPos.rawY).padStart(3, '0') }}
          </span>
        </div>
      </header>

      <div class="stage-body">
        <div class="stage-brand">
          <span class="studio-tag kc-mono">KLACKCRAFT // CUSTOM PERIPHERALS</span>
          <h1>{{ heroTitle }}</h1>
          <p>{{ heroDesc }}</p>
        </div>

        <div class="keycap-deck" aria-label="交互式机械键帽矩阵">
          <div class="deck-header kc-mono">
            <span>SWITCH MATRIX // 65% LAB DEMO</span>
            <span>ACTUATIONS: {{ String(actuationCount).padStart(4, '0') }}</span>
          </div>

          <div class="deck-plate">
            <div v-for="(row, rowIdx) in keyRows" :key="rowIdx" class="key-row">
              <button
                v-for="item in row"
                :key="item.id"
                type="button"
                class="mech-key"
                :class="{
                  accent: item.accent,
                  wide: item.wide,
                  pressed: Boolean(pressedKeys[item.id]),
                  rippling: rippleKey === item.id
                }"
                @click="triggerKey(item, true)"
              >
                <span class="mech-key-cap">
                  <span class="cap-main kc-mono">{{ item.label }}</span>
                  <span v-if="item.sub" class="cap-sub kc-mono">{{ item.sub }}</span>
                </span>
              </button>
            </div>
          </div>

          <div class="deck-telemetry kc-mono">
            <div class="tele-cell">
              <span>ACTIVE KEY</span>
              <strong>[{{ lastKeyMeta.label }}]</strong>
            </div>
            <div class="tele-cell">
              <span>PRE-TRAVEL</span>
              <strong>{{ lastKeyMeta.travel }}</strong>
            </div>
            <div class="tele-cell">
              <span>ACTUATION</span>
              <strong>{{ lastKeyMeta.force }}</strong>
            </div>
            <div class="tele-cell">
              <span>PROFILE</span>
              <strong>{{ lastKeyMeta.switchType }}</strong>
            </div>
          </div>
        </div>
      </div>

      <footer class="stage-foot kc-mono">
        <span>咔嗒造物商城 · KLACKCRAFT</span>
        <span>CLICK ANY KEYCAP OR TYPE TO ACTUATE</span>
      </footer>
    </section>

    <section class="auth-surface">
      <div class="auth-card">
        <div class="card-brand-mini">
          <div class="mini-emblem kc-mono">
            <div class="mini-emblem-cap">
              K
              <i class="mini-emblem-dot" aria-hidden="true" />
            </div>
          </div>
          <div class="mini-brand-copy">
            <strong>咔嗒造物</strong>
            <span class="mini-sub kc-mono">KlackCraft Studio</span>
          </div>
        </div>

        <div class="auth-heading">
          <span class="kc-kicker">{{ activeTab === 'login' ? 'MEMBER SIGN IN' : 'NEW MEMBER' }}</span>
          <h2>{{ panelTitle }}</h2>
          <p>{{ panelSubtitle }}</p>
        </div>

        <div class="segmented-switch" role="tablist" aria-label="登录注册切换">
          <div class="switch-slider" :class="{ right: activeTab === 'register' }" aria-hidden="true" />
          <button
            type="button"
            role="tab"
            :aria-selected="activeTab === 'login'"
            class="switch-btn"
            :class="{ active: activeTab === 'login' }"
            @click="switchTab('login')"
          >
            账号登录
          </button>
          <button
            type="button"
            role="tab"
            :aria-selected="activeTab === 'register'"
            class="switch-btn"
            :class="{ active: activeTab === 'register' }"
            @click="switchTab('register')"
          >
            极客注册
          </button>
        </div>

        <Transition name="auth-form-shift" mode="out-in">
          <el-form
            v-if="activeTab === 'login'"
            key="login"
            ref="loginFormRef"
            :model="loginForm"
            :rules="loginRules"
            label-position="top"
            size="large"
            class="kc-auth-form"
            @keydown="pulseKeyFromTyping"
            @keyup.enter="handleLoginSubmit"
          >
            <el-form-item label="账号或手机号" prop="accountOrPhone">
              <el-input v-model="loginForm.accountOrPhone" placeholder="请输入账号或手机号" clearable>
                <template #prefix>
                  <el-icon><User /></el-icon>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item label="登录密码" prop="password">
              <el-input v-model="loginForm.password" type="password" show-password placeholder="请输入登录密码">
                <template #prefix>
                  <el-icon><Lock /></el-icon>
                </template>
              </el-input>
            </el-form-item>

            <div class="form-helper-row">
              <span class="helper-hint kc-mono">ENCRYPTED SESSION</span>
              <button type="button" class="switch-link" @click="switchTab('register')">还没有账号？前往注册 →</button>
            </div>

            <el-button
              type="primary"
              class="submit-keycap-btn"
              :loading="loginLoading"
              @click="handleLoginSubmit"
            >
              <span>登录并进入工坊</span>
              <span class="btn-enter-hint kc-mono">↵</span>
            </el-button>
          </el-form>

          <el-form
            v-else
            key="register"
            ref="registerFormRef"
            :model="registerForm"
            :rules="registerRules"
            label-position="top"
            size="large"
            class="kc-auth-form"
            @keydown="pulseKeyFromTyping"
            @keyup.enter="handleRegisterSubmit"
          >
            <div class="form-two-col">
              <el-form-item label="注册账号" prop="account">
                <el-input v-model="registerForm.account" placeholder="字母或数字组合" clearable>
                  <template #prefix>
                    <el-icon><User /></el-icon>
                  </template>
                </el-input>
              </el-form-item>

              <el-form-item label="手机号" prop="phone">
                <el-input v-model="registerForm.phone" maxlength="11" placeholder="11 位手机号" clearable>
                  <template #prefix>
                    <el-icon><Iphone /></el-icon>
                  </template>
                </el-input>
              </el-form-item>
            </div>

            <div class="form-two-col">
              <el-form-item label="昵称（选填）" prop="nickname">
                <el-input v-model="registerForm.nickname" maxlength="20" placeholder="展示名称" clearable>
                  <template #prefix>
                    <el-icon><ChatDotRound /></el-icon>
                  </template>
                </el-input>
              </el-form-item>

              <el-form-item label="真实姓名（选填）" prop="realName">
                <el-input v-model="registerForm.realName" maxlength="20" placeholder="收件联系用" clearable>
                  <template #prefix>
                    <el-icon><EditPen /></el-icon>
                  </template>
                </el-input>
              </el-form-item>
            </div>

            <div class="form-two-col">
              <el-form-item label="设置密码" prop="password">
                <el-input v-model="registerForm.password" type="password" show-password placeholder="至少 6 位密码">
                  <template #prefix>
                    <el-icon><Lock /></el-icon>
                  </template>
                </el-input>
              </el-form-item>

              <el-form-item label="确认密码" prop="confirmPassword">
                <el-input v-model="registerForm.confirmPassword" type="password" show-password placeholder="再次确认密码">
                  <template #prefix>
                    <el-icon><Key /></el-icon>
                  </template>
                </el-input>
              </el-form-item>
            </div>

            <div class="form-helper-row">
              <span class="helper-hint kc-mono">AUTO SIGN-IN READY</span>
              <button type="button" class="switch-link" @click="switchTab('login')">已有账号？直接登录 →</button>
            </div>

            <el-button
              type="primary"
              class="submit-keycap-btn"
              :loading="registerLoading"
              @click="handleRegisterSubmit"
            >
              <span>完成注册并进入工坊</span>
              <span class="btn-enter-hint kc-mono">↵</span>
            </el-button>
          </el-form>
        </Transition>
      </div>
    </section>
  </div>
</template>

<style scoped lang="scss">
.auth-studio {
  min-height: 100dvh;
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(440px, 560px);
  background: var(--kc-bg);
}

/* Left Anodized Carbon Stage */
.industrial-stage {
  --mx: 52%;
  --my: 42%;
  position: relative;
  overflow: hidden;
  background: #101114;
  color: #f4f4f2;
  padding: 32px 44px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  border-right: 1px solid #24262e;
}

.stage-grid {
  position: absolute;
  inset: 0;
  background-image:
    radial-gradient(rgba(255, 255, 255, 0.14) 1px, transparent 1px);
  background-size: 26px 26px;
  opacity: 0.42;
  pointer-events: none;
}

.stage-spotlight {
  position: absolute;
  inset: 0;
  background: radial-gradient(
    520px circle at var(--mx) var(--my),
    rgba(255, 77, 0, 0.16),
    rgba(255, 77, 0, 0.03) 42%,
    transparent 70%
  );
  pointer-events: none;
}

.stage-top,
.stage-body,
.stage-foot {
  position: relative;
  z-index: 2;
}

.stage-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.stage-back {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  height: 36px;
  padding: 0 14px;
  border-radius: var(--kc-radius-sm);
  border: 1px solid #2e313b;
  background: rgba(255, 255, 255, 0.04);
  color: #e4e5ea;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: border-color 0.16s ease, background-color 0.16s ease;
}

.stage-back:hover {
  border-color: var(--kc-accent);
  background: rgba(255, 77, 0, 0.12);
}

.stage-tools {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.sound-toggle {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  height: 30px;
  padding: 0 11px;
  border-radius: var(--kc-radius-xs);
  border: 1px solid #2b2e37;
  background: rgba(255, 255, 255, 0.03);
  color: #9da1af;
  font-size: 11px;
  cursor: pointer;
  transition: border-color 0.15s ease, color 0.15s ease;
}

.sound-toggle .led {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #4b4f5c;
  transition: background-color 0.15s ease;
}

.sound-toggle.active {
  border-color: rgba(255, 77, 0, 0.45);
  color: #ffffff;
}

.sound-toggle.active .led {
  background: var(--kc-accent);
}

.coord-chip {
  font-size: 11px;
  color: #6d717f;
  padding: 6px 10px;
  border-radius: var(--kc-radius-xs);
  border: 1px solid #23262e;
  background: rgba(16, 17, 20, 0.72);
}

.stage-body {
  margin: auto 0;
  padding: 36px 0;
  max-width: 620px;
  animation: stageReveal 0.55s ease-out both;
}

.studio-tag {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  border-radius: var(--kc-radius-xs);
  border: 1px solid rgba(255, 77, 0, 0.32);
  background: rgba(255, 77, 0, 0.1);
  color: #ff8552;
  font-size: 11px;
  letter-spacing: 0.14em;
}

.stage-brand h1 {
  margin: 20px 0 0;
  font-size: clamp(36px, 4.2vw, 54px);
  line-height: 1.08;
  letter-spacing: -0.03em;
  color: #ffffff;
}

.stage-brand p {
  margin: 16px 0 0;
  max-width: 520px;
  font-size: 15px;
  line-height: 1.75;
  color: #9ba0ae;
}

/* Interactive Mechanical Keycap Deck */
.keycap-deck {
  margin-top: 38px;
  border-radius: 14px;
  border: 1px solid #2a2d36;
  background: #17191e;
  padding: 18px;
  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.42);
}

.deck-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 11px;
  color: #7c8190;
  letter-spacing: 0.08em;
  margin-bottom: 14px;
}

.deck-plate {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 14px;
  border-radius: 10px;
  background: #0e0f12;
  border: 1px solid #242730;
}

.key-row {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 10px;
}

.mech-key {
  position: relative;
  height: 66px;
  padding: 0 0 6px;
  border: 0;
  border-radius: 9px;
  background: #181a20;
  cursor: pointer;
  transform: translateY(0);
  transition: transform 0.09s ease-out;
  box-shadow:
    inset 0 -1px 0 rgba(255, 255, 255, 0.05),
    0 4px 0 #090a0c;
}

.mech-key:hover .mech-key-cap {
  border-color: #4b5060;
}

.mech-key.pressed,
.mech-key:active {
  transform: translateY(4px);
  box-shadow: 0 0 0 #090a0c;
}

.mech-key.accent {
  background: #b83700;
  box-shadow: 0 4px 0 #6e2000;
}

.mech-key.accent.pressed,
.mech-key.accent:active {
  box-shadow: 0 0 0 #6e2000;
}

.mech-key-cap {
  width: 100%;
  height: 100%;
  border-radius: 7px;
  background: linear-gradient(180deg, #2b2e37 0%, #22252d 100%);
  border: 1px solid #393d4a;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: space-between;
  padding: 9px 11px;
  color: #f4f4f2;
  transition: border-color 0.14s ease, background-color 0.14s ease;
}

.mech-key.accent .mech-key-cap {
  background: linear-gradient(180deg, #ff5e1a 0%, #ff4d00 100%);
  border-color: #ff824d;
  color: #ffffff;
}

.mech-key.pressed .mech-key-cap {
  border-color: var(--kc-accent);
}

.mech-key.rippling::after {
  content: '';
  position: absolute;
  inset: -3px;
  border-radius: 11px;
  border: 1.5px solid rgba(255, 77, 0, 0.7);
  animation: keyRipple 0.28s ease-out forwards;
  pointer-events: none;
}

.cap-main {
  font-size: 16px;
  font-weight: 700;
  line-height: 1;
}

.cap-sub {
  font-size: 10px;
  color: #848999;
  line-height: 1;
}

.mech-key.accent .cap-sub {
  color: rgba(255, 255, 255, 0.78);
}

.deck-telemetry {
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid #262933;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.tele-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.tele-cell span {
  font-size: 10px;
  color: #6d717f;
  letter-spacing: 0.08em;
}

.tele-cell strong {
  font-size: 12px;
  color: #e6e7ec;
}

.stage-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  font-size: 11px;
  color: #646875;
  letter-spacing: 0.08em;
}

/* Right Precision Form Panel */
.auth-surface {
  background: var(--kc-surface);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 48px;
}

.auth-card {
  width: min(440px, 100%);
  animation: formReveal 0.45s ease-out both;
}

.card-brand-mini {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 28px;
}

.mini-emblem {
  width: 38px;
  height: 38px;
  border-radius: 8px;
  background: var(--kc-carbon);
  padding: 3px 3px 5px;
  display: grid;
  place-items: stretch;
  flex-shrink: 0;
}

.mini-emblem-cap {
  position: relative;
  border-radius: 5px;
  background: #23252c;
  border: 1px solid #363943;
  color: #ffffff;
  font-size: 15px;
  font-weight: 700;
  line-height: 1;
  display: grid;
  place-items: center;
}

.mini-emblem-dot {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 4px;
  height: 4px;
  border-radius: 1px;
  background: var(--kc-accent);
}

.mini-brand-copy strong {
  display: block;
  font-size: 15px;
  line-height: 1.15;
  color: var(--kc-text);
}

.mini-brand-copy .mini-sub {
  display: block;
  margin-top: 2px;
  font-size: 11px;
  color: var(--kc-text-muted);
}

.auth-heading h2 {
  margin: 12px 0 0;
  font-size: 30px;
  line-height: 1.15;
  letter-spacing: -0.02em;
  color: var(--kc-text);
}

.auth-heading p {
  margin: 10px 0 0;
  font-size: 14px;
  line-height: 1.65;
  color: var(--kc-text-secondary);
}

/* Sliding Segmented Switch */
.segmented-switch {
  position: relative;
  margin-top: 28px;
  padding: 4px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  border-radius: var(--kc-radius-md);
  background: var(--kc-bg);
  border: 1px solid var(--kc-border);
}

.switch-slider {
  position: absolute;
  top: 4px;
  left: 4px;
  width: calc(50% - 4px);
  height: calc(100% - 8px);
  border-radius: 7px;
  background: var(--kc-carbon);
  transform: translateX(0);
  transition: transform 0.22s cubic-bezier(0.22, 1, 0.36, 1);
  pointer-events: none;
}

.switch-slider.right {
  transform: translateX(100%);
}

.switch-btn {
  position: relative;
  z-index: 1;
  height: 42px;
  border: 0;
  background: transparent;
  color: var(--kc-text-secondary);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: color 0.18s ease;
}

.switch-btn.active {
  color: #ffffff;
}

.kc-auth-form {
  margin-top: 26px;
}

.form-two-col {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.kc-auth-form :deep(.el-form-item) {
  margin-bottom: 18px;
}

.kc-auth-form :deep(.el-form-item__label) {
  padding-bottom: 6px;
  color: var(--kc-text);
  font-size: 13px;
  font-weight: 600;
}

.kc-auth-form :deep(.el-input__wrapper) {
  min-height: 46px;
  border-radius: var(--kc-radius-sm);
  background: var(--kc-surface-muted);
  box-shadow: 0 0 0 1px var(--kc-border) inset;
  padding: 0 14px;
  transition: box-shadow 0.16s ease, background-color 0.16s ease;
}

.kc-auth-form :deep(.el-input__wrapper.is-focus) {
  background: var(--kc-surface);
  box-shadow: 0 0 0 1.5px var(--kc-carbon) inset, 0 0 0 4px rgba(20, 20, 22, 0.06);
}

.form-helper-row {
  margin: 2px 0 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.helper-hint {
  font-size: 11px;
  color: var(--kc-text-muted);
  letter-spacing: 0.08em;
}

.switch-link {
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--kc-accent);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.switch-link:hover {
  color: var(--kc-accent-hover);
}

.submit-keycap-btn {
  width: 100%;
  height: 50px;
  border: 0;
  border-radius: var(--kc-radius-sm);
  background: var(--kc-carbon);
  color: #ffffff;
  font-size: 15px;
  font-weight: 700;
  box-shadow: 0 4px 0 #000000;
  transition: transform 0.1s ease, background-color 0.16s ease, box-shadow 0.1s ease;
}

.submit-keycap-btn:hover {
  background: var(--kc-accent);
  box-shadow: 0 4px 0 #b83700;
}

.submit-keycap-btn:active {
  transform: translateY(3px);
  box-shadow: 0 1px 0 #b83700;
}

.btn-enter-hint {
  margin-left: 10px;
  padding: 2px 6px;
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.16);
  font-size: 12px;
}

/* Transitions */
.auth-form-shift-enter-active,
.auth-form-shift-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.auth-form-shift-enter-from {
  opacity: 0;
  transform: translateY(8px);
}

.auth-form-shift-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

@keyframes stageReveal {
  from {
    opacity: 0;
    transform: translateY(14px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes formReveal {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes keyRipple {
  from {
    opacity: 0.9;
    transform: scale(0.98);
  }
  to {
    opacity: 0;
    transform: scale(1.06);
  }
}

@media (max-width: 1120px) {
  .auth-studio {
    grid-template-columns: 1fr;
  }

  .industrial-stage {
    padding: 24px 24px 28px;
    border-right: 0;
    border-bottom: 1px solid #24262e;
  }

  .stage-body {
    padding: 28px 0 24px;
  }

  .auth-surface {
    padding: 36px 24px 48px;
  }
}

@media (max-width: 640px) {
  .coord-chip {
    display: none;
  }

  .key-row {
    gap: 6px;
  }

  .mech-key {
    height: 54px;
  }

  .mech-key-cap {
    padding: 7px 8px;
  }

  .cap-main {
    font-size: 14px;
  }

  .deck-telemetry {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .form-two-col {
    grid-template-columns: 1fr;
    gap: 0;
  }

  .stage-foot {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
