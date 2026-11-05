<template>
  <div class="layout-logo">
    <!-- transition 包裹 router-link 的 key 会触发 enter 动画，但 transition 没有 appear 属性，
         导致 Layout 首次挂载（addRoute + replace 场景）时动画状态不确定，Logo 卡在 translate 起点位置。
         改为无 transition，直接渲染，避免与 sidebar 初始状态竞争。 -->
    <router-link :key="+collapse" class="wh-full flex-center" to="/">
      <img :src="logo" alt="logo" width="20" height="20" class="layout-logo__img" />
      <span v-if="!collapse" class="layout-logo__title">
        {{ appConfig.title }}
      </span>
    </router-link>
  </div>
</template>

<script lang="ts" setup>
import { appConfig } from "@/settings";
import logo from "@/assets/images/logo.svg";

defineProps({
  collapse: {
    type: Boolean,
    required: true,
  },
});
</script>

<style lang="scss" scoped>
.layout-logo {
  width: 100%;
  height: $navbar-height;
  background-color: $sidebar-logo-background;

  // 尺寸与 HTML width/height 属性保持一致，避免样式未生效时按 SVG 固有尺寸渲染
  &__img {
    flex-shrink: 0;
    width: 20px;
    height: 20px;
  }

  &__title {
    flex-shrink: 0;
    margin-left: 10px;
    font-size: 14px;
    font-weight: bold;
    color: $sidebar-logo-text-color;
  }
}

.logo-fade-enter-active {
  animation: logo-fade-in-left 1s both;
}

@keyframes logo-fade-in-left {
  from {
    opacity: 0;
    transform: translate3d(-100%, 0, 0);
  }

  to {
    opacity: 1;
    transform: translate3d(0, 0, 0);
  }
}
</style>

<style lang="scss">
.layout-root.layout--top,
.layout-root.layout--mix {
  .layout-logo {
    background-color: transparent !important;

    &__title {
      color: var(--menu-text);
    }
  }
}

.layout-root.is-sidebar-open {
  &.layout--top .layout-header__left .layout-logo,
  &.layout--mix .layout-header__logo .layout-logo {
    width: $sidebar-width;
  }
}

.layout-root.is-sidebar-collapsed {
  &.layout--top .layout-header__left .layout-logo,
  &.layout--mix .layout-header__logo .layout-logo {
    width: $sidebar-width-collapsed;
  }

  .layout-logo__title {
    display: none;
  }
}
</style>
