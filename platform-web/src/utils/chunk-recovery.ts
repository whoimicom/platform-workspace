import { promptAppUpdate } from "@/utils/version-prompt";

/**
 * 版本更新后旧 index.html 引用的 chunk 已被删除，动态 import 会 404。
 * 这里捕获这类错误并提示用户刷新，不静默自动刷新，避免打断当前操作。
 */

/** 各浏览器动态 import 失败的报错特征 */
const CHUNK_ERROR_PATTERNS = [
  /Failed to fetch dynamically imported module/i, // Chrome / Edge
  /Importing a module script failed/i, // Firefox
  /error loading dynamically imported module/i, // Safari
  /Unable to preload CSS/i, // Vite 样式预加载失败
];

/**
 * 判断错误信息是否属于 chunk 加载失败
 *
 * @param message 错误信息
 */
export function isChunkLoadError(message: string): boolean {
  return CHUNK_ERROR_PATTERNS.some((pattern) => pattern.test(message));
}

/**
 * 统一处理 chunk 加载失败：页面已不可用，强制提示刷新
 *
 * @param message 错误信息
 */
export function handleChunkLoadError(message: string): void {
  if (!isChunkLoadError(message)) return;
  promptAppUpdate(true);
}

/**
 * 安装全局异常监听，捕获未被代码捕获的 chunk 加载失败
 */
export function installChunkLoadRecovery(): void {
  window.addEventListener("unhandledrejection", (event) => {
    const reason: unknown = event.reason;
    const message =
      typeof reason === "string" ? reason : reason instanceof Error ? reason.message : "";
    handleChunkLoadError(message);
  });

  // 兼容 <link rel="modulepreload"> 等资源加载失败
  window.addEventListener("error", (event) => {
    handleChunkLoadError(event.message ?? "");
  });
}
