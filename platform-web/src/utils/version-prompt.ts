import { ElMessageBox } from "element-plus";
import "element-plus/es/components/message-box/style/index";

/** 当前构建版本标识 */
const CURRENT_VERSION = String(__APP_INFO__.buildTimestamp);

/** 本次会话已提示过的版本，避免同一版本反复弹窗 */
const PROMPTED_KEY = "app:version-prompted";

/**
 * 提醒用户刷新页面加载新版本
 *
 * 同一构建版本在本次会话中只提示一次；刷新不会丢失登录态（凭证存于
 * localStorage/sessionStorage，均可在整页刷新后读取）。
 *
 * @param force 页面已不可用时为 true，只保留"立即刷新"，不允许稍后
 */
export async function promptAppUpdate(force = false): Promise<void> {
  try {
    if (sessionStorage.getItem(PROMPTED_KEY) === CURRENT_VERSION) return;
    sessionStorage.setItem(PROMPTED_KEY, CURRENT_VERSION);
  } catch {
    // 隐私模式下 sessionStorage 不可用，仅影响去重
  }

  try {
    await ElMessageBox.confirm(
      "系统已发布新版本，请刷新页面以使用最新版本。刷新不会退出登录。",
      "发现新版本",
      {
        confirmButtonText: "立即刷新",
        cancelButtonText: "稍后提醒",
        showCancelButton: !force,
        showClose: !force,
        closeOnClickModal: false,
        closeOnPressEscape: false,
        draggable: false,
        type: "warning",
      }
    );
    window.location.reload();
  } catch {
    // 用户选择稍后，本次会话内不再提示该版本
  }
}
