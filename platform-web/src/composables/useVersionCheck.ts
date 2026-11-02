import { onMounted, onUnmounted, ref } from "vue";
import { promptAppUpdate } from "@/utils/version-prompt";

/** 当前构建版本标识 */
const CURRENT_VERSION = String(__APP_INFO__.buildTimestamp);

/**
 * 低频检测部署版本，发现新版本后弹窗提醒用户刷新。
 *
 * 只在页面加载、重新回到前台和固定间隔时各检查一次，不做静默自动刷新，
 * 由用户决定何时刷新，体验更可控。
 *
 * @param interval 轮询间隔，默认 10 分钟
 */
export function useVersionCheck(interval = 10 * 60_000) {
  const hasNewVersion = ref(false);
  let timer: ReturnType<typeof setInterval> | undefined;

  /**
   * 查询部署版本，版本不一致时提示刷新
   */
  const checkVersion = async () => {
    try {
      const base = import.meta.env.BASE_URL;
      const baseUrl = base.endsWith("/") ? base : `${base}/`;
      // 时间戳防 CDN/代理缓存，no-cache 防浏览器磁盘缓存
      const res = await fetch(`${baseUrl}version.json?t=${Date.now()}`, {
        cache: "no-cache",
      });
      if (!res.ok) return; // 非核心功能，网络异常不影响正常使用

      const data = (await res.json()) as { version?: number | string };
      if (data.version && String(data.version) !== CURRENT_VERSION) {
        hasNewVersion.value = true;
        promptAppUpdate();
      }
    } catch (error) {
      console.warn("版本检测失败", error);
    }
  };

  /**
   * 页面重新可见时立即检查
   */
  const handleVisibilityChange = () => {
    if (!document.hidden) checkVersion();
  };

  onMounted(() => {
    checkVersion();
    timer = setInterval(checkVersion, Math.max(60_000, interval));
    document.addEventListener("visibilitychange", handleVisibilityChange);
  });

  onUnmounted(() => {
    clearInterval(timer);
    document.removeEventListener("visibilitychange", handleVisibilityChange);
  });

  return { hasNewVersion };
}
