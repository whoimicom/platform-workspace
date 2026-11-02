<template>
  <el-config-provider :locale="locale" :size="size">
    <el-watermark
      :font="{ color: fontColor }"
      :content="showWatermark ? watermarkContent : ''"
      :z-index="9999"
      class="wh-full"
    >
      <router-view />
    </el-watermark>
  </el-config-provider>
</template>

<script setup lang="ts">
import { useAppStore, useSettingsStore } from "@/stores";
import { appConfig } from "@/settings";
import { ThemeMode, ComponentSize } from "@/enums";
import { useVersionCheck } from "@/composables/useVersionCheck";
import { installChunkLoadRecovery } from "@/utils/chunk-recovery";

const appStore = useAppStore();
const settingsStore = useSettingsStore();

const locale = computed(() => appStore.locale);
const size = computed(() => appStore.size as ComponentSize);
const showWatermark = computed(() => settingsStore.showWatermark);
const watermarkContent = appConfig.name;

// 明亮/暗黑主题水印字体颜色适配
const fontColor = computed(() => {
  return settingsStore.resolvedTheme === ThemeMode.DARK
    ? "rgba(255, 255, 255, .15)"
    : "rgba(0, 0, 0, .15)";
});
// 初始化版本检查与 chunk 失效提示（开发环境由 HMR 处理，不启用）
if (!import.meta.env.DEV) {
  installChunkLoadRecovery();
  useVersionCheck();
}
</script>
