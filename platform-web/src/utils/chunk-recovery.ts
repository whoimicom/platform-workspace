import { ElMessage } from "element-plus";

/**
 * 版本更新后旧 index.html 引用的 chunk 已被删除，动态 import 会 404。
 * 这里捕获这类错误并静默刷新一次，避免用户停留在白屏或异常页面。
 */

/** 各浏览器动态 import 失败的报错特征 */
const CHUNK_ERROR_PATTERNS = [
  /Failed to fetch dynamically imported module/i, // Chrome / Edge
  /Importing a module script failed/i, // Firefox
  /error loading dynamically imported module/i, // Safari
  /Unable to preload CSS/i, // Vite 样式预加载失败
];

/** 记录"该构建版本已重试过刷新"，防止资源确实不可用时无限刷新 */
const RELOAD_FLAG_KEY = "app:chunk-reload-version";

/**
 * 判断错误信息是否属于 chunk 加载失败
 *
 * @param message 错误信息
 */
export function isChunkLoadError(message: string): boolean {
  return CHUNK_ERROR_PATTERNS.some((pattern) => pattern.test(message));
}

/**
 * 执行一次整页刷新，同一构建版本最多刷新一次
 *
 * @returns true 表示已触发刷新，false 表示本版本已重试过
 */
function reloadOnceForNewBuild(): boolean {
  const version = String(__APP_INFO__.buildTimestamp);
  try {
    if (sessionStorage.getItem(RELOAD_FLAG_KEY) === version) return false;
    sessionStorage.setItem(RELOAD_FLAG_KEY, version);
  } catch {
    // sessionStorage 不可用（隐私模式）时直接放行一次刷新
  }
  window.location.reload();
  return true;
}

/**
 * 统一处理 chunk 加载失败：能刷新则静默刷新，已重试过则提示用户手动处理
 *
 * @param message 错误信息
 */
export function handleChunkLoadError(message: string): void {
  if (!isChunkLoadError(message)) return;
  if (reloadOnceForNewBuild()) return;
  ElMessage.error("页面资源加载失败，请手动刷新后重试");
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
