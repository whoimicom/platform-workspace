<template>
  <div class="login-page">
    <div class="login-toolbar">
      <ThemeSwitcher />
    </div>

    <div class="login-layout">
      <div class="login-brand">
        <div class="login-brand__header">
          <el-image :src="logo" class="login-brand__logo" />
          <div class="login-brand__identity">
            <span class="login-brand__name">{{ appConfig.title }}</span>
            <span class="login-brand__version">v{{ appConfig.version }}</span>
          </div>
        </div>

        <div class="login-brand__hero">
          <h1 class="login-brand__title">{{ appConfig.title }}</h1>
          <p class="login-brand__desc">基于 Vue 3 / Spring Boot 的前后端分离管理平台</p>
          <div class="login-brand__features">
            <span class="login-brand__feature">
              <span class="login-brand__feature-text">Vue 3</span>
            </span>
            <span class="login-brand__feature">
              <span class="login-brand__feature-text">Spring Boot 4</span>
            </span>
            <span class="login-brand__feature">
              <span class="login-brand__feature-text">Element Plus</span>
            </span>
            <span class="login-brand__feature">
              <span class="login-brand__feature-text">MySQL</span>
            </span>
          </div>
        </div>
      </div>

      <div class="login-card">
        <div class="login-card__inner">
          <transition name="fade-slide" mode="out-in">
            <div v-if="component === 'login'" key="login" class="login-card__form">
              <!-- 账号密码登录 -->
              <template v-if="loginMode === 'password'">
              <h2 class="login-card__title">欢迎回来</h2>
              <p class="login-card__desc">请完成身份验证后进入系统</p>

              <el-form
                ref="loginFormRef"
                :model="loginFormData"
                :rules="loginRules"
                size="large"
                :validate-on-rule-change="false"
              >
                <el-form-item prop="username">
                  <el-input
                    v-model.trim="loginFormData.username"
                    placeholder="用户名"
                    :prefix-icon="UserIcon"
                  />
                </el-form-item>

                <el-tooltip :visible="isCapsLock" content="大写锁定已开启" placement="right">
                  <el-form-item prop="password">
                    <el-input
                      v-model.trim="loginFormData.password"
                      placeholder="密码"
                      type="password"
                      show-password
                      :prefix-icon="LockIcon"
                      @keyup="checkCapsLock"
                      @keyup.enter="handleLoginSubmit"
                    />
                  </el-form-item>
                </el-tooltip>

                <el-form-item prop="captchaCode">
                  <div class="flex w-full gap-12px">
                    <el-input
                      v-model.trim="loginFormData.captchaCode"
                      placeholder="验证码"
                      class="min-w-0 flex-1"
                      @keyup.enter="handleLoginSubmit"
                    >
                      <template #prefix>
                        <span class="i-svg:security login-card__prefix-icon" />
                      </template>
                    </el-input>
                    <div class="login-card__captcha" @click="getCaptcha">
                      <el-icon v-if="codeLoading" class="is-loading" :size="16">
                        <Loading />
                      </el-icon>
                      <img v-else-if="captchaBase64" :src="captchaBase64" alt="验证码" />
                      <el-icon v-else :size="16"><Refresh /></el-icon>
                    </div>
                  </div>
                </el-form-item>

                <div class="login-card__options">
                  <el-checkbox v-model="loginFormData.rememberMe">记住我</el-checkbox>
                  <a @click="showForm('resetPwd')">忘记密码？</a>
                </div>

                <el-button
                  :loading="loading"
                  type="primary"
                  size="large"
                  class="login-card__submit"
                  @click="handleLoginSubmit"
                >
                  登录
                </el-button>
              </el-form>

              <div v-if="demoAccountsEnabled" class="login-card__demo">
                <div class="login-card__demo-title">演示账号 · 密码统一 123456，点击直接填入</div>
                <div class="flex flex-wrap gap-8px">
                  <button
                    v-for="account in demoAccounts"
                    :key="account.username"
                    type="button"
                    class="login-card__demo-chip"
                    @click="fillDemoAccount(account)"
                  >
                    {{ account.label }}
                  </button>
                </div>
              </div>

              <div class="login-card__alt">
                <div class="login-card__alt-divider">其他登录方式</div>
                <div class="flex gap-12px">
                  <button class="login-card__alt-btn" type="button" @click="handleOauthLogin('WECHAT')">
                    <span class="i-svg:user login-card__alt-icon" />
                    微信
                  </button>
                  <button class="login-card__alt-btn" type="button" @click="handleOauthLogin('WECOM')">
                    <span class="i-svg:user login-card__alt-icon" />
                    企业微信
                  </button>
                  <button class="login-card__alt-btn" type="button" @click="switchToSmsLogin">
                    <span class="i-svg:user login-card__alt-icon" />
                    验证码
                  </button>
                </div>
              </div>
              </template>

              <!-- 手机号验证码登录 -->
              <template v-else-if="loginMode === 'sms'">
                <h2 class="login-card__title">验证码登录</h2>
                <p class="login-card__desc">请输入已注册的手机号，获取验证码后登录</p>

                <el-form ref="smsFormRef" :model="smsLoginDialog" :rules="smsLoginRules" size="large">
                  <el-form-item prop="mobile">
                    <el-input
                      v-model.trim="smsLoginDialog.mobile"
                      placeholder="手机号"
                      maxlength="11"
                      :prefix-icon="PhoneIcon"
                    />
                  </el-form-item>
                  <!-- 短信验证码（与图形验证互斥显示） -->
                  <el-form-item v-if="!smsCaptcha.visible" prop="smsCode">
                    <div class="flex w-full gap-12px">
                      <el-input
                        v-model.trim="smsLoginDialog.smsCode"
                        placeholder="验证码"
                        class="min-w-0 flex-1"
                        @keyup.enter="handleSmsLoginSubmit"
                      >
                        <template #prefix>
                          <span class="i-svg:security login-card__prefix-icon" />
                        </template>
                      </el-input>
                      <el-button
                        :loading="smsLoginDialog.sending"
                        :disabled="smsLoginDialog.cooldown > 0"
                        @click="sendSmsCodeForLogin"
                      >
                        {{ smsLoginDialog.cooldown > 0 ? `${smsLoginDialog.cooldown}s` : "获取验证码" }}
                      </el-button>
                    </div>
                  </el-form-item>

                  <!-- 图形验证：通过后才发送短信，同一时间只显示一种验证码 -->
                  <div v-else class="login-card__sms-captcha">
                    <el-input
                      v-model.trim="smsCaptcha.code"
                      placeholder="图形验证码"
                      maxlength="6"
                      class="min-w-0 flex-1"
                      @keyup.enter="sendSmsCodeForLogin"
                    >
                      <template #prefix>
                        <span class="i-svg:security login-card__prefix-icon" />
                      </template>
                    </el-input>
                    <div class="login-card__captcha" @click="refreshSmsCaptcha">
                      <el-icon v-if="smsCaptcha.loading" class="is-loading" :size="16">
                        <Loading />
                      </el-icon>
                      <img v-else-if="smsCaptcha.base64" :src="smsCaptcha.base64" alt="图形验证码" />
                      <el-icon v-else :size="16"><Refresh /></el-icon>
                    </div>
                    <el-button :loading="smsLoginDialog.sending" @click="sendSmsCodeForLogin">
                      发送
                    </el-button>
                  </div>
                </el-form>

                <p v-if="smsCaptcha.visible" class="login-card__captcha-tip">
                  请先完成图形验证，通过后将发送短信验证码
                </p>

                <el-button
                  :loading="smsLoginDialog.loading"
                  type="primary"
                  size="large"
                  class="login-card__submit"
                  @click="handleSmsLoginSubmit"
                >
                  登录
                </el-button>

                <div class="login-card__back">
                  <a @click="backToPassword">返回账号密码登录</a>
                </div>
              </template>

              <!-- 三方账号绑定 -->
              <template v-else-if="loginMode === 'bind'">
                <h2 class="login-card__title">{{ oauthBindDialog.providerLabel }}账号绑定</h2>
                <p class="login-card__desc">
                  检测到该{{ oauthBindDialog.providerLabel }}账号未关联系统用户，请输入已注册的手机号完成绑定
                </p>

                <el-form ref="oauthBindFormRef" :model="oauthBindDialog" :rules="smsLoginRules" size="large">
                  <el-form-item prop="mobile">
                    <el-input
                      v-model.trim="oauthBindDialog.mobile"
                      placeholder="手机号"
                      maxlength="11"
                      :prefix-icon="PhoneIcon"
                    />
                  </el-form-item>
                  <!-- 短信验证码（与图形验证互斥显示） -->
                  <el-form-item v-if="!smsCaptcha.visible" prop="smsCode">
                    <div class="flex w-full gap-12px">
                      <el-input
                        v-model.trim="oauthBindDialog.smsCode"
                        placeholder="验证码"
                        class="min-w-0 flex-1"
                        @keyup.enter="handleOauthBindSubmit"
                      >
                        <template #prefix>
                          <span class="i-svg:security login-card__prefix-icon" />
                        </template>
                      </el-input>
                      <el-button
                        :loading="oauthBindDialog.sending"
                        :disabled="oauthBindDialog.cooldown > 0"
                        @click="sendSmsCodeForBind"
                      >
                        {{ oauthBindDialog.cooldown > 0 ? `${oauthBindDialog.cooldown}s` : "获取验证码" }}
                      </el-button>
                    </div>
                  </el-form-item>

                  <!-- 图形验证：通过后才发送短信，同一时间只显示一种验证码 -->
                  <div v-else class="login-card__sms-captcha">
                    <el-input
                      v-model.trim="smsCaptcha.code"
                      placeholder="图形验证码"
                      maxlength="6"
                      class="min-w-0 flex-1"
                      @keyup.enter="sendSmsCodeForBind"
                    >
                      <template #prefix>
                        <span class="i-svg:security login-card__prefix-icon" />
                      </template>
                    </el-input>
                    <div class="login-card__captcha" @click="refreshSmsCaptcha">
                      <el-icon v-if="smsCaptcha.loading" class="is-loading" :size="16">
                        <Loading />
                      </el-icon>
                      <img v-else-if="smsCaptcha.base64" :src="smsCaptcha.base64" alt="图形验证码" />
                      <el-icon v-else :size="16"><Refresh /></el-icon>
                    </div>
                    <el-button :loading="oauthBindDialog.sending" @click="sendSmsCodeForBind">
                      发送
                    </el-button>
                  </div>
                </el-form>

                <p v-if="smsCaptcha.visible" class="login-card__captcha-tip">
                  请先完成图形验证，通过后将发送短信验证码
                </p>

                <el-button
                  :loading="oauthBindDialog.loading"
                  type="primary"
                  size="large"
                  class="login-card__submit"
                  @click="handleOauthBindSubmit"
                >
                  绑定并登录
                </el-button>

                <div class="login-card__back">
                  <a @click="backToPassword">返回账号密码登录</a>
                </div>
              </template>

              <!-- 三方扫码登录（内嵌二维码，不跳转页面） -->
              <template v-else-if="loginMode === 'qrcode'">
                <h2 class="login-card__title login-card__qrcode-title">
                  {{ qrcodeProvider === "WECHAT" ? "微信扫码登录" : "企业微信扫码登录" }}
                </h2>
                <p class="login-card__desc login-card__qrcode-desc">
                  请使用{{ qrcodeProvider === "WECHAT" ? "微信" : "企业微信" }}扫描下方二维码，并在手机上确认登录
                </p>

                <div class="login-card__qrcode">
                  <div :id="QRCODE_CONTAINER_ID" class="login-card__qrcode-inner"></div>
                  <div v-if="qrcodeLoading" class="login-card__qrcode-mask">
                    <el-icon class="is-loading"><Loading /></el-icon>
                  </div>
                </div>

                <p v-if="qrcodeError" class="login-card__qrcode-error">{{ qrcodeError }}</p>

                <div class="login-card__back">
                  <a @click="backToPassword">返回账号密码登录</a>
                </div>
              </template>
            </div>

            <ResetPwd
              v-else
              key="resetPwd"
              class="login-card__form"
              @update:model-value="component = $event"
            />
          </transition>
        </div>

        <div class="login-footer">Copyright © 2026 whoimi.com</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: "LoginPage", inheritAttrs: false });

import { ElMessage, type FormInstance } from "element-plus";
import { Lock, Loading, Phone, Refresh, User } from "@element-plus/icons-vue";
import AuthAPI from "@/api/auth";
import type { LoginRequest, OauthProvider } from "@/api/auth";
import router from "@/router";
import { useUserStore } from "@/stores";
import { AuthStorage } from "@/utils/auth";
import { appConfig } from "@/settings";
import ResetPwd from "./components/ResetPwd.vue";
import logo from "@/assets/images/logo.svg";

const userStore = useUserStore();
const route = useRoute();
const component = ref<"login" | "resetPwd">("login");

const loginFormRef = ref<FormInstance>();
const loading = ref(false);
const isCapsLock = ref(false);
const captchaBase64 = ref<string>();
const codeLoading = ref(false);

const UserIcon = markRaw(User);
const LockIcon = markRaw(Lock);
const PhoneIcon = markRaw(Phone);

/** 登录卡片内的模式：账号密码 / 手机验证码 / 三方绑定 / 三方扫码 */
const loginMode = ref<"password" | "sms" | "bind" | "qrcode">("password");

// ---- 账号密码登录表单 ----

const loginFormData = ref<LoginRequest>({
  username: "whoimi",
  password: "123456",
  captchaToken: "",
  captchaCode: "",
  rememberMe: AuthStorage.getRememberMe(),
});

const loginRules = computed(() => ({
  username: [{ required: true, trigger: "blur", message: "请输入用户名" }],
  password: [
    { required: true, trigger: "blur", message: "请输入密码" },
    { min: 6, message: "密码不能少于6位", trigger: "blur" },
  ],
  captchaCode: [{ required: true, trigger: "blur", message: "请输入验证码" }],
}));

// ---- 短信登录（卡片内模式）----

const smsFormRef = ref<FormInstance>();
const smsLoginDialog = reactive({
  mobile: "",
  smsCode: "",
  sending: false,
  loading: false,
  cooldown: 0,
  cooldownTimer: null as number | null,
});

// ---- 三方绑定（卡片内模式）----

const oauthBindFormRef = ref<FormInstance>();
const oauthBindDialog = reactive({
  provider: "" as OauthProvider | "",
  providerLabel: "",
  bindToken: "",
  mobile: "",
  smsCode: "",
  sending: false,
  loading: false,
  cooldown: 0,
  cooldownTimer: null as number | null,
});

const smsLoginRules = {
  mobile: [
    { required: true, message: "请输入手机号", trigger: "blur" },
    { pattern: /^1[3-9]\d{9}$/, message: "手机号格式不正确", trigger: "blur" },
  ],
  smsCode: [{ required: true, message: "请输入验证码", trigger: "blur" }],
};

const providerLabelMap: Record<OauthProvider, string> = {
  WECHAT: "微信",
  WECOM: "企业微信",
};

/**
 * 启动冷却计时器
 */
function startCooldown<T extends { cooldown: number; cooldownTimer: number | null }>(
  state: T,
  seconds = 60
) {
  state.cooldown = seconds;
  state.cooldownTimer = window.setInterval(() => {
    state.cooldown--;
    if (state.cooldown <= 0 && state.cooldownTimer !== null) {
      window.clearInterval(state.cooldownTimer);
      state.cooldownTimer = null;
    }
  }, 1000);
}

// ---- 演示账号 ----

const demoAccountsEnabled = false;
const demoAccounts = [
  { username: "employee", label: "普通员工" },
  { username: "org_manager", label: "架构主管" },
  { username: "manager", label: "总经理" },
  { username: "finance", label: "财务" },
  { username: "clerk", label: "行政" },
];

function fillDemoAccount(account: { username: string }): void {
  loginFormData.value.username = account.username;
  loginFormData.value.password = "123456";
}

function getCaptcha() {
  codeLoading.value = true;
  AuthAPI.getCaptcha()
    .then((d) => {
      loginFormData.value.captchaToken = d.captchaToken;
      captchaBase64.value = d.captchaBase64;
    })
    .finally(() => (codeLoading.value = false));
}

async function handleLoginSubmit() {
  const valid = await loginFormRef.value?.validate().then(
    () => true,
    () => false
  );
  if (!valid) return;

  loading.value = true;
  try {
    await userStore.login(loginFormData.value).then(
      async () => {
        const redirectPath = (route.query.redirect as string) || "/";
        await router.push(decodeURIComponent(redirectPath));
      },
      () => getCaptcha()
    );
  } finally {
    loading.value = false;
  }
}

function checkCapsLock(event: KeyboardEvent) {
  if (event instanceof KeyboardEvent) {
    isCapsLock.value = event.getModifierState("CapsLock");
  }
}

function showForm(type: "resetPwd") {
  component.value = type;
}

// ---- 三方登录（微信 / 企业微信，登录卡片内嵌二维码）----

const OAUTH_PROVIDER_KEY = "oauth_provider";
const OAUTH_STATE_KEY = "oauth_state";
const QRCODE_CONTAINER_ID = "oauth-qrcode-container";

const qrcodeProvider = ref<OauthProvider | "">("");
const qrcodeLoading = ref(false);
const qrcodeError = ref("");

/** 清空二维码容器，避免切换 provider 时残留上一个 iframe */
function clearQrcode() {
  const container = document.getElementById(QRCODE_CONTAINER_ID);
  if (container) container.innerHTML = "";
}

function resetQrcode() {
  clearQrcode();
  qrcodeProvider.value = "";
  qrcodeLoading.value = false;
  qrcodeError.value = "";
}

function generateState() {
  return Math.random().toString(36).slice(2) + Date.now().toString(36);
}

/** 官方扫码 SDK 的构造函数签名 */
type QrSdkCtor = new (options: Record<string, unknown>) => unknown;

/** 动态加载官方 SDK（幂等） */
function loadSdk(src: string, sdkKey: string): Promise<void> {
  const flagKey = `__${sdkKey}_loaded__`;
  const flags = window as unknown as Record<string, boolean>;
  if (flags[flagKey]) return Promise.resolve();
  return new Promise((resolve, reject) => {
    const script = document.createElement("script");
    script.src = src;
    script.async = true;
    script.onload = () => {
      flags[flagKey] = true;
      resolve();
    };
    script.onerror = () => reject(new Error("二维码组件加载失败，请检查网络或域名配置"));
    document.head.appendChild(script);
  });
}

/**
 * 点击微信 / 企业微信：在登录卡片内渲染官方二维码，不跳转页面。
 * 扫码确认后，官方 iframe 会跳到中转页，中转页通过 postMessage 把 code 传回本页。
 */
async function handleOauthLogin(provider: OauthProvider) {
  qrcodeError.value = "";
  qrcodeLoading.value = true;
  qrcodeProvider.value = provider;
  loginMode.value = "qrcode";
  clearQrcode();

  const state = generateState();
  sessionStorage.setItem(OAUTH_PROVIDER_KEY, provider);
  sessionStorage.setItem(OAUTH_STATE_KEY, state);

  try {
    const config = await AuthAPI.getOauthClientConfig();
    const providerConfig = provider === "WECHAT" ? config.wechat : config.wecom;
    if (!providerConfig.enabled || !providerConfig.redirectUri) {
      throw new Error(
        provider === "WECHAT"
          ? "微信登录未配置：请设置 WECHAT_OPEN_APPID / WECHAT_OPEN_APP_SECRET / WECHAT_OPEN_REDIRECT_URI"
          : "企业微信登录未配置：请设置 WECOM_CORP_ID / WECOM_APP_SECRET / WECOM_REDIRECT_URI"
      );
    }

    // 等待 qrcode 模式的容器渲染完成
    await nextTick();

    if (provider === "WECHAT") {
      await loadSdk("https://res.wx.qq.com/connect/zh_CN/htmledition/js/wxLogin.js", "wxLogin");
      const WxLoginCtor = (window as unknown as Record<string, QrSdkCtor | undefined>).WxLogin;
      if (!WxLoginCtor) throw new Error("微信扫码组件加载失败");
      new WxLoginCtor({
        self_redirect: true,
        id: QRCODE_CONTAINER_ID,
        appid: providerConfig.appId,
        scope: "snsapi_login",
        redirect_uri: encodeURIComponent(providerConfig.redirectUri),
        state,
        // 新版登录页开关：带底部“快捷登录”入口，面板尺寸 300×450
        login_type: "jssdk",
      });
    } else {
      await loadSdk(
        "https://wwcdn.weixin.qq.com/node/wwl/20240119170700/wwLogin-1.3.0.js",
        "wwLogin"
      );
      const WwLoginCtor = (window as unknown as Record<string, QrSdkCtor | undefined>).WwLogin;
      if (!WwLoginCtor) throw new Error("企业微信扫码组件加载失败");
      new WwLoginCtor({
        id: QRCODE_CONTAINER_ID,
        appid: providerConfig.appId,
        agentid: providerConfig.agentId,
        redirect_uri: encodeURIComponent(providerConfig.redirectUri),
        state,
        lang: "zh",
        href: "",
      });
    }
  } catch (error) {
    qrcodeError.value = error instanceof Error ? error.message : "二维码加载失败";
  } finally {
    qrcodeLoading.value = false;
  }
}

/**
 * 统一处理授权 code：已绑定直接登录，未绑定切到绑定模式。
 */
async function handleOauthCode(provider: OauthProvider, code: string) {
  try {
    const result = await AuthAPI.oauthLogin({ provider, code });
    if (result.loggedIn && result.accessToken) {
      await finalizeLogin({
        accessToken: result.accessToken,
        refreshToken: result.refreshToken ?? "",
        tokenType: "Bearer",
        expiresIn: 7200,
      });
      return;
    }
    if (result.needBind && result.bindToken) {
      // 在登录卡片内切换到绑定模式
      oauthBindDialog.provider = provider;
      oauthBindDialog.providerLabel = providerLabelMap[provider];
      oauthBindDialog.bindToken = result.bindToken;
      loginMode.value = "bind";
      return;
    }
    ElMessage.error("登录失败：未识别的返回结果");
  } catch {
    // 后端已通过 businessException 返回错误消息
  }
}

/**
 * 内嵌二维码回调：中转页在 iframe 内通过 postMessage 把 code 传回本页。
 */
function handleOauthMessage(event: MessageEvent) {
  // 只接受本域中转页发来的消息
  if (event.origin !== window.location.origin) return;
  const data = event.data as { type?: string; code?: string; state?: string } | null;
  if (!data || data.type !== "oauth-callback" || !data.code) return;

  const provider = sessionStorage.getItem(OAUTH_PROVIDER_KEY) as OauthProvider | null;
  const expectedState = sessionStorage.getItem(OAUTH_STATE_KEY);
  sessionStorage.removeItem(OAUTH_PROVIDER_KEY);
  sessionStorage.removeItem(OAUTH_STATE_KEY);

  if (!provider || !expectedState || data.state !== expectedState) {
    qrcodeError.value = "登录校验失败，请刷新二维码后重试";
    return;
  }
  void handleOauthCode(provider, data.code);
}

/**
 * 整页授权回调兜底：地址栏携带 code + state 时，校验后换取登录态。
 */
async function handleOauthCallback() {
  const code = route.query.code as string | undefined;
  const state = route.query.state as string | undefined;
  if (!code) return;

  const provider = sessionStorage.getItem(OAUTH_PROVIDER_KEY) as OauthProvider | null;
  const expectedState = sessionStorage.getItem(OAUTH_STATE_KEY);
  sessionStorage.removeItem(OAUTH_PROVIDER_KEY);
  sessionStorage.removeItem(OAUTH_STATE_KEY);

  // 清理地址栏上的 code/state，避免刷新重复提交
  await router.replace({ path: route.path, query: {} });

  if (!provider || !expectedState || state !== expectedState) {
    ElMessage.error("登录校验失败，请重新登录");
    return;
  }
  await handleOauthCode(provider, code);
}

async function handleOauthBindSubmit() {
  const valid = await oauthBindFormRef.value?.validate().then(
    () => true,
    () => false
  );
  if (!valid) return;

  oauthBindDialog.loading = true;
  try {
    const result = await AuthAPI.oauthBind({
      bindToken: oauthBindDialog.bindToken,
      mobile: oauthBindDialog.mobile,
      smsCode: oauthBindDialog.smsCode,
    });
    await finalizeLogin(result);
  } finally {
    oauthBindDialog.loading = false;
  }
}

function resetOauthBind() {
  oauthBindFormRef.value?.resetFields();
  oauthBindDialog.provider = "";
  oauthBindDialog.providerLabel = "";
  oauthBindDialog.bindToken = "";
  oauthBindDialog.mobile = "";
  oauthBindDialog.smsCode = "";
  if (oauthBindDialog.cooldownTimer !== null) {
    window.clearInterval(oauthBindDialog.cooldownTimer);
    oauthBindDialog.cooldownTimer = null;
  }
  oauthBindDialog.cooldown = 0;
  resetSmsCaptcha();
}

async function sendSmsCodeForBind() {
  if (!oauthBindDialog.mobile.match(/^1[3-9]\d{9}$/)) {
    ElMessage.warning("请先输入正确的手机号");
    return;
  }
  // 第一次点击：先展开图形验证
  if (!smsCaptcha.visible) {
    await refreshSmsCaptcha();
    return;
  }
  if (!smsCaptcha.code.trim()) {
    ElMessage.warning("请先输入图形验证码");
    return;
  }
  oauthBindDialog.sending = true;
  try {
    await AuthAPI.sendSmsCode({
      mobile: oauthBindDialog.mobile,
      captchaToken: smsCaptcha.token,
      captchaCode: smsCaptcha.code.trim(),
    });
    ElMessage.success("验证码已发送");
    resetSmsCaptcha();
    startCooldown(oauthBindDialog, 60);
  } catch {
    // 图形验证码为一次性消费，失败后自动刷新
    await refreshSmsCaptcha();
  } finally {
    oauthBindDialog.sending = false;
  }
}

// ---- 发送短信前的图形验证 ----

const smsCaptcha = reactive({
  visible: false,
  token: "",
  base64: "",
  code: "",
  loading: false,
});

/** 拉取（或刷新）图形验证码，并展开图形验证输入行 */
async function refreshSmsCaptcha() {
  smsCaptcha.loading = true;
  try {
    const info = await AuthAPI.getCaptcha();
    smsCaptcha.token = info.captchaToken;
    smsCaptcha.base64 = info.captchaBase64;
    smsCaptcha.code = "";
    smsCaptcha.visible = true;
  } catch {
    ElMessage.error("图形验证码加载失败，请重试");
  } finally {
    smsCaptcha.loading = false;
  }
}

function resetSmsCaptcha() {
  smsCaptcha.visible = false;
  smsCaptcha.token = "";
  smsCaptcha.base64 = "";
  smsCaptcha.code = "";
}

// ---- 手机验证码直接登录 ----

function switchToSmsLogin() {
  resetSmsLogin();
  loginMode.value = "sms";
}

/** 返回账号密码登录 */
function backToPassword() {
  if (loginMode.value === "sms") resetSmsLogin();
  if (loginMode.value === "bind") resetOauthBind();
  if (loginMode.value === "qrcode") resetQrcode();
  loginMode.value = "password";
}

async function sendSmsCodeForLogin() {
  if (!smsLoginDialog.mobile.match(/^1[3-9]\d{9}$/)) {
    ElMessage.warning("请先输入正确的手机号");
    return;
  }
  // 第一次点击：先展开图形验证
  if (!smsCaptcha.visible) {
    await refreshSmsCaptcha();
    return;
  }
  if (!smsCaptcha.code.trim()) {
    ElMessage.warning("请先输入图形验证码");
    return;
  }
  smsLoginDialog.sending = true;
  try {
    await AuthAPI.sendSmsCode({
      mobile: smsLoginDialog.mobile,
      captchaToken: smsCaptcha.token,
      captchaCode: smsCaptcha.code.trim(),
    });
    ElMessage.success("验证码已发送");
    resetSmsCaptcha();
    startCooldown(smsLoginDialog, 60);
  } catch {
    // 图形验证码为一次性消费，失败后自动刷新
    await refreshSmsCaptcha();
  } finally {
    smsLoginDialog.sending = false;
  }
}

async function handleSmsLoginSubmit() {
  const valid = await smsFormRef.value?.validate().then(
    () => true,
    () => false
  );
  if (!valid) return;

  smsLoginDialog.loading = true;
  try {
    const result = await AuthAPI.smsLogin({
      mobile: smsLoginDialog.mobile,
      smsCode: smsLoginDialog.smsCode,
    });
    await finalizeLogin(result);
  } finally {
    smsLoginDialog.loading = false;
  }
}

function resetSmsLogin() {
  smsFormRef.value?.resetFields();
  smsLoginDialog.mobile = "";
  smsLoginDialog.smsCode = "";
  if (smsLoginDialog.cooldownTimer !== null) {
    window.clearInterval(smsLoginDialog.cooldownTimer);
    smsLoginDialog.cooldownTimer = null;
  }
  smsLoginDialog.cooldown = 0;
  resetSmsCaptcha();
}

// ---- 通用：拿到后端返回结果后写 token + 拉用户信息 + 跳转 ----

async function finalizeLogin(result: {
  accessToken: string;
  refreshToken?: string;
  tokenType?: string;
  expiresIn?: number;
}) {
  // 写入 token（rememberMe 用默认 false，与账号密码登录保持一致）
  AuthStorage.setTokens(result.accessToken, result.refreshToken ?? "", false);
  // 拉用户信息并缓存
  await userStore.fetchUserInfo();

  const redirectPath = (route.query.redirect as string) || "/";
  await router.push(decodeURIComponent(redirectPath));
}

onMounted(() => {
  getCaptcha();
  // 三方授权回调（URL 携带 code + state，整页模式兜底）
  handleOauthCallback();
  // 内嵌二维码扫码完成后，由中转页 postMessage 通知本页
  window.addEventListener("message", handleOauthMessage);
});

onUnmounted(() => {
  window.removeEventListener("message", handleOauthMessage);
});
</script>

<style lang="scss" scoped>
$text-primary: #273248;
$text-secondary: #667085;
$text-muted: #98a2b3;
$input-h: 44px;

.login-page {
  // 品牌色阶：从运行时主题色派生，切换主题色板（ArcoD/AntD/ElementD）时整页跟随
  --brand-strong: color-mix(in srgb, var(--el-color-primary), #000 15%);
  --brand-deep: color-mix(in srgb, var(--el-color-primary), #000 45%);

  position: relative;
  display: flex;
  min-height: 100vh;
  overflow: auto;
  /* 浅色渐变底色；桌面端被品牌区背景与白色卡片完全覆盖，窄屏两区透明后透出 */
  background: linear-gradient(180deg, #ffffff 0%, #dbeafe 100%);
}

.login-toolbar {
  position: fixed;
  top: 28px;
  right: 32px;
  z-index: 10;
  display: flex;
  gap: 12px;
  align-items: center;

  :deep(*) {
    cursor: pointer;
  }
}

.login-layout {
  display: flex;
  flex: 1;
  min-height: 100%;
}

.login-brand {
  position: relative;
  display: flex;
  flex: 0 0 65%;
  flex-direction: column;
  min-height: 100vh;
  padding: 28px 64px 48px;
  overflow: hidden;
  // 白云蓝海渐变（原 bg.svg）：白 → 主题色浅蓝，各 stop 按主题色混合比例派生
  background: linear-gradient(
    180deg,
    #fff 0%,
    color-mix(in srgb, var(--el-color-primary) 3%, #fff) 16%,
    color-mix(in srgb, var(--el-color-primary) 5%, #fff) 32%,
    color-mix(in srgb, var(--el-color-primary) 6%, #fff) 48%,
    color-mix(in srgb, var(--el-color-primary) 8%, #fff) 62%,
    color-mix(in srgb, var(--el-color-primary) 15%, #fff) 76%,
    color-mix(in srgb, var(--el-color-primary) 25%, #fff) 88%,
    color-mix(in srgb, var(--el-color-primary) 42%, #fff) 100%
  );
  animation: login-pane-in 0.36s ease-out both;

  &__header,
  &__hero {
    position: relative;
    z-index: 1;
  }

  &__header {
    display: flex;
    gap: 14px;
    align-items: center;
  }

  &__logo {
    box-sizing: border-box;
    width: 42px;
    height: 42px;
    padding: 5px;
    background: color-mix(in srgb, var(--el-color-primary) 8%, transparent);
    border: 1px solid color-mix(in srgb, var(--el-color-primary) 18%, transparent);
    border-radius: 12px;
  }

  &__identity {
    display: inline-flex;
    gap: 10px;
    align-items: center;
    min-width: 0;
  }

  &__name {
    font-size: 24px;
    font-weight: 600;
    line-height: 1;
    color: var(--brand-deep);
  }

  &__version {
    display: inline-flex;
    align-items: center;
    height: 22px;
    padding: 0 8px;
    font-size: 12px;
    font-weight: 600;
    line-height: 1;
    color: color-mix(in srgb, var(--brand-deep) 80%, transparent);
    background: color-mix(in srgb, var(--el-color-primary) 8%, transparent);
    border: 1px solid color-mix(in srgb, var(--el-color-primary) 16%, transparent);
    border-radius: 999px;
  }

  &__hero {
    display: flex;
    flex: 1;
    flex-direction: column;
    justify-content: center;
    width: min(760px, 100%);
    padding: 40px 0 60px;
  }

  &__title {
    margin: 0;
    font-size: 36px;
    font-weight: 800;
    line-height: 1.15;
    color: var(--brand-deep);
    letter-spacing: 0;
  }

  &__desc {
    max-width: 560px;
    margin: 16px 0 0;
    font-size: 18px;
    line-height: 1.6;
    color: #475569;
  }

  &__features {
    display: inline-flex;
    align-items: center;
    width: fit-content;
    max-width: 100%;
    margin-top: 32px;
  }

  &__feature {
    position: relative;
    display: inline-flex;
    gap: 10px;
    align-items: center;
    height: 32px;
    padding: 0 15px;
    font-size: 14px;
    font-weight: 600;
    color: color-mix(in srgb, var(--brand-deep) 85%, transparent);
    background: transparent;

    &:first-child {
      padding-left: 0;
    }

    &:not(:last-child)::after {
      position: absolute;
      top: 7px;
      right: 0;
      width: 1px;
      height: 14px;
      content: "";
      background: color-mix(in srgb, var(--el-color-primary) 18%, transparent);
    }
  }

  &__feature-text {
    line-height: 1;
    white-space: nowrap;
  }
}

.login-card {
  position: relative;
  z-index: 1;
  display: flex;
  flex: 0 0 35%;
  flex-direction: column;
  align-items: center;
  padding: 0 0 32px;
  background: #fff;
  animation: login-pane-in 0.36s ease-out 0.04s both;

  &__inner {
    box-sizing: border-box;
    display: flex;
    flex: 1;
    flex-direction: column;
    justify-content: center;
    width: 100%;
    max-width: 430px;
    padding: 0 20px;
  }

  &__form {
    width: 100%;
  }

  &__title {
    margin: 0 0 10px;
    font-size: 28px;
    font-weight: 750;
    line-height: 1.1;
    color: $text-primary;
    letter-spacing: 0;
  }

  &__desc {
    margin: 10px 0 28px;
    font-size: 15px;
    color: $text-muted;
  }

  &__prefix-icon {
    display: inline-flex;
    width: 14px;
    height: 14px;
    color: var(--el-text-color-placeholder);
  }

  &__captcha {
    box-sizing: border-box;
    display: flex;
    flex-shrink: 0;
    align-items: center;
    justify-content: center;
    width: 108px;
    height: $input-h;
    overflow: hidden;
    cursor: pointer;
    background: var(--el-fill-color-blank);
    border: 1px solid var(--el-border-color);
    border-radius: var(--el-border-radius-base);
    transition: border-color 0.2s;

    &:hover {
      border-color: var(--el-color-primary);
    }

    img {
      display: block;
      width: 100%;
      height: 100%;
      object-fit: contain;
    }
  }

  &__options {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 22px;
    font-size: 14px;
    color: $text-secondary;

    a {
      font-weight: 500;
      color: var(--el-color-primary);
      cursor: pointer;
      transition: opacity 0.15s;

      &:hover {
        opacity: 0.8;
      }
    }
  }

  &__submit {
    width: 100%;
    height: 44px;
    font-size: 16px;
    font-weight: 600;
    border-radius: 8px;
    box-shadow: 0 12px 24px color-mix(in srgb, var(--el-color-primary) 18%, transparent);

    &:hover {
      box-shadow: 0 14px 28px color-mix(in srgb, var(--el-color-primary) 22%, transparent);
    }

    &:focus,
    &:focus-visible {
      outline: none;
    }
  }

  /* 卡片内切换模式（验证码登录 / 三方绑定）的返回入口 */
  &__back {
    margin-top: 18px;
    font-size: 13px;
    text-align: center;

    a {
      color: var(--el-color-primary);
      cursor: pointer;

      &:hover {
        text-decoration: underline;
      }
    }
  }

  /* 内嵌三方扫码二维码：保持官方 300×400 原始面板，不做额外卡片修饰 */
  &__qrcode-title {
    text-align: center;
  }

  &__qrcode-desc {
    margin-bottom: 20px;
    text-align: center;
  }

  &__qrcode {
    position: relative;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    margin: 4px 0 0;
  }

  &__qrcode-inner {
    position: relative;
    display: flex;
    overflow: hidden;
    background: #fff;

    :deep(iframe) {
      display: block;
      width: 300px;
      height: 450px;
      border: none;
    }
  }

  &__qrcode-mask {
    position: absolute;
    inset: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 22px;
    color: var(--el-color-primary);
    background: color-mix(in srgb, var(--el-bg-color) 88%, transparent);
  }

  &__qrcode-error {
    margin: 8px 0 0;
    font-size: 13px;
    color: var(--el-color-danger);
    text-align: center;
  }

  /* 视口高度不足时整体缩小官方面板，避免整页出现滚动条 */
  @media (max-height: 820px) {
    &__qrcode-inner :deep(iframe) {
      zoom: 0.85;
    }
  }

  @media (max-height: 700px) {
    &__qrcode-inner :deep(iframe) {
      zoom: 0.7;
    }
  }

  /* 短信发送前的图形验证行（与短信验证码输入互斥显示） */
  &__sms-captcha {
    display: flex;
    align-items: center;
    gap: 8px;
    margin: 0 0 18px;

    :deep(.el-button) {
      flex-shrink: 0;
    }
  }

  &__captcha-tip {
    margin: -4px 0 14px;
    font-size: 12px;
    color: var(--el-text-color-secondary);
    text-align: center;
  }

  &__demo {
    margin-top: 20px;

    &-title {
      margin-bottom: 10px;
      font-size: 12px;
      color: $text-muted;
    }

    &-chip {
      height: 28px;
      padding: 0 12px;
      font-size: 12px;
      color: $text-secondary;
      cursor: pointer;
      background: color-mix(in srgb, var(--el-color-primary) 5%, transparent);
      border: 1px solid color-mix(in srgb, var(--el-color-primary) 16%, transparent);
      border-radius: 999px;
      transition:
        color 0.2s,
        background 0.2s,
        border-color 0.2s;

      &:hover {
        color: var(--el-color-primary);
        background: color-mix(in srgb, var(--el-color-primary) 10%, transparent);
        border-color: color-mix(in srgb, var(--el-color-primary) 40%, transparent);
      }
    }
  }

  /* 半透明白底按钮，在渐变背景上保持可见；hover 时提实并加深投影 */
  &__alt {
    margin-top: 28px;

    &-divider {
      display: flex;
      gap: 12px;
      align-items: center;
      margin-bottom: 14px;
      font-size: 13px;
      color: $text-muted;

      &::before,
      &::after {
        flex: 1;
        height: 1px;
        content: "";
        background: color-mix(in srgb, var(--brand-deep) 12%, transparent);
      }
    }

    &-btn {
      display: flex;
      flex: 1;
      gap: 8px;
      align-items: center;
      justify-content: center;
      height: 44px;
      padding: 0;
      font-size: 14px;
      color: $text-secondary;
      cursor: pointer;
      background: rgb(255 255 255 / 80%);
      border: 1px solid color-mix(in srgb, var(--brand-deep) 10%, transparent);
      border-radius: 8px;
      box-shadow: 0 1px 2px color-mix(in srgb, var(--brand-deep) 6%, transparent);
      backdrop-filter: blur(4px);
      transition:
        color 0.2s,
        background 0.2s,
        border-color 0.2s,
        box-shadow 0.2s;

      &:hover {
        color: var(--el-color-primary);
        background: #fff;
        border-color: color-mix(in srgb, var(--el-color-primary) 35%, transparent);
        box-shadow: 0 4px 12px color-mix(in srgb, var(--el-color-primary) 14%, transparent);
      }
    }

    &-icon {
      width: 16px;
      height: 16px;
    }
  }
}

:deep(.el-form-item) {
  margin-bottom: 14px;
}

:deep(.el-input__wrapper) {
  height: $input-h;
}

.login-footer {
  flex-shrink: 0;
  font-size: 12px;
  color: $text-muted;
}

.dark .login-page {
  background: #0b1020;
}

.dark .login-brand {
  // 深蓝渐变底（原 bg-dark.svg），暗色下不随主题色变化
  background: linear-gradient(135deg, #0f1a33 0%, #0c1428 55%, #0a1020 100%);

  // 网格纹理（原 bg-dark.svg pattern）：白色细线 + 上浓下淡遮罩
  &::before {
    position: absolute;
    inset: 0;
    content: "";
    background-image:
      linear-gradient(rgb(255 255 255 / 5%) 1px, transparent 1px),
      linear-gradient(90deg, rgb(255 255 255 / 5%) 1px, transparent 1px);
    background-size: 72px 72px;
    mask-image: linear-gradient(
      to bottom,
      rgb(0 0 0 / 90%) 0%,
      rgb(0 0 0 / 30%) 70%,
      transparent 100%
    );
  }

  &__logo {
    background: rgb(255 255 255 / 10%);
    border-color: rgb(255 255 255 / 20%);
  }

  &__name {
    color: rgb(255 255 255 / 92%);
  }

  &__version {
    color: rgb(255 255 255 / 85%);
    background: rgb(255 255 255 / 10%);
    border-color: rgb(255 255 255 / 16%);
  }

  &__title {
    color: #fff;
  }

  &__desc {
    color: rgb(255 255 255 / 65%);
  }

  &__feature {
    color: rgb(255 255 255 / 85%);

    &:not(:last-child)::after {
      background: rgb(255 255 255 / 18%);
    }
  }
}

.dark .login-card {
  background: #0b1020;

  &__title {
    color: rgb(255 255 255 / 85%);
  }

  &__desc {
    color: rgb(255 255 255 / 30%);
  }

  &__demo-title {
    color: rgb(255 255 255 / 35%);
  }

  &__demo-chip {
    color: rgb(255 255 255 / 70%);
    background: rgb(255 255 255 / 6%);
    border-color: rgb(255 255 255 / 14%);

    &:hover {
      color: #fff;
      background: rgb(255 255 255 / 12%);
      border-color: rgb(255 255 255 / 28%);
    }
  }

  &__alt-divider {
    color: rgb(255 255 255 / 20%);

    &::before,
    &::after {
      background: rgb(255 255 255 / 12%);
    }
  }

  &__alt-btn {
    color: rgb(255 255 255 / 65%);
    background: rgb(255 255 255 / 6%);
    border-color: rgb(255 255 255 / 12%);
    box-shadow: none;

    &:hover {
      color: #fff;
      background: rgb(255 255 255 / 12%);
      border-color: rgb(255 255 255 / 24%);
    }
  }
}

.dark .login-footer {
  color: rgb(255 255 255 / 15%);
}

.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.2s ease;
}

.fade-slide-enter-from,
.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(6px);
}

@keyframes login-pane-in {
  from {
    opacity: 0;
  }

  to {
    opacity: 1;
  }
}

@media (max-width: 1024px) {
  .login-layout {
    flex-direction: column;
  }

  .login-toolbar {
    position: absolute;
    top: 37px;
  }

  .login-brand {
    flex: none;
    height: auto;
    min-height: auto;
    padding: 28px 40px 32px;
    background: transparent;

    &__hero {
      display: none;
    }
  }

  /* 深色品牌区的渐变与网格纹理不适合窄屏，透出页面底色 */
  .dark .login-brand {
    background: none;

    &::before {
      display: none;
    }
  }

  /* 卡片占满剩余高度并透出页面渐变，inner 沿用全局的垂直居中，页脚沉底 */
  .login-card {
    flex: 1;
    padding: 40px 48px 32px;
    background: transparent;
  }

  .dark .login-card {
    background: transparent;
  }
}

@media (max-width: 640px) {
  .login-toolbar {
    top: 33px;
    right: 20px;
  }

  .login-brand {
    padding: 24px 24px 28px;

    /* 版本号移到名称下方，避免与右上角工具栏图标重叠 */
    &__identity {
      flex-direction: column;
      gap: 6px;
      align-items: flex-start;
    }

    &__name {
      font-size: 20px;
      white-space: nowrap;
    }
  }

  .login-card {
    padding: 32px 24px 24px;

    &__inner {
      width: 100%;
      padding: 0;
    }
  }
}
</style>
