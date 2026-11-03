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
          <div class="login-card__form">
            <h2 class="login-card__title">欢迎回来</h2>
            <p class="login-card__desc">请使用公司统一账号登录</p>

            <el-button
              type="primary"
              size="large"
              class="login-card__submit"
              :loading="redirecting"
              @click="handleSsoLogin"
            >
              <span class="i-svg:security login-card__sso-icon" />
              使用统一认证登录
            </el-button>

            <p class="login-card__sso-tip">
              登录由统一认证中心（OIDC）处理，一次登录即可访问公司各管理系统
            </p>
          </div>
        </div>

        <div class="login-footer">Copyright © 2026 whoimi.com</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: "LoginPage", inheritAttrs: false });

import { useUserStore } from "@/stores";
import { appConfig } from "@/settings";
import logo from "@/assets/images/logo.svg";

const userStore = useUserStore();
const route = useRoute();
const redirecting = ref(false);

/**
 * 跳转统一认证中心完成登录（认证后再回到本应用）
 */
async function handleSsoLogin() {
  redirecting.value = true;
  try {
    const redirectPath = (route.query.redirect as string) || "/";
    await userStore.login(decodeURIComponent(redirectPath));
  } finally {
    redirecting.value = false;
  }
}
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

  &__sso-icon {
    display: inline-flex;
    width: 16px;
    height: 16px;
    margin-right: 6px;
    color: currentcolor;
  }

  &__sso-tip {
    margin-top: 18px;
    font-size: 12px;
    line-height: 1.6;
    color: $text-muted;
    text-align: center;
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
}

.dark .login-footer {
  color: rgb(255 255 255 / 15%);
}

@keyframes login-pane-in {
  from {
    opacity: 0;
    filter: blur(4px);
  }

  to {
    opacity: 1;
    filter: blur(0);
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
