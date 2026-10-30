import axios, { type AxiosResponse } from "axios";
import type { AxiosRequestConfig } from "axios";
import qs from "qs";

import { usePermissionStoreHook } from "@/stores/permission";
import type { ResultInfo } from "@/api/common";
import { installTokenRefresh } from "@/utils/token-refresh";
import { readBinaryApiResult } from "@/utils/http-response";

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE,
  timeout: 50000,
  headers: { "Content-Type": "application/json;charset=utf-8" },
  // 数组参数序列化为 ids=1&ids=2，而非 ids[]=1&ids=2
  paramsSerializer: (params) => qs.stringify(params, { arrayFormat: "repeat" }),
});

// 先注册续期拦截器，使令牌失效在业务错误提示之前处理。
installTokenRefresh(http);

// 拦截器保留完整响应，解包仅在下方 request 入口执行一次。
http.interceptors.response.use(
  async (response: AxiosResponse<ResultInfo>): Promise<AxiosResponse<ResultInfo>> => {
    const { responseType } = response.config;

    // 文件接口的业务失败仍按 JSON 处理，成功时保留完整文件响应。
    if (responseType === "blob" || responseType === "arraybuffer") {
      try {
        const result = await readBinaryApiResult(response);
        if (!result || result.success) return response;
        throw new Error(result.resultMsg || "文件请求失败");
      } catch (error) {
        ElMessage.error(error instanceof Error ? error.message : "文件请求失败");
        throw error;
      }
    }

    if (response.data.success) {
      return response;
    }

    ElMessage.error(response.data.resultMsg || "系统出错");
    throw new Error(response.data.resultMsg || "系统出错");
  },

  async (error) => {
    if (axios.isCancel(error)) return Promise.reject(error);
    // 令牌续期拦截器已终结的错误不再提示
    if (!axios.isAxiosError(error)) {
      return Promise.reject(error);
    }

    const { response } = error;

    if (response && error.config?.silentErrorStatuses?.includes(response.status)) {
      return Promise.reject(error);
    }

    if (!response) {
      ElMessage.error("网络连接失败");
      return Promise.reject(error);
    }

    // 401 已交由续期拦截器处理，这里不重复提示。
    if (response.status === 401) {
      return Promise.reject(error);
    }

    const { resultMsg } = response.data as ResultInfo;

    // 权限不足：刷新权限后再提示
    if (response.status === 403) {
      const permissionStore = usePermissionStoreHook();
      try {
        await permissionStore.refreshPermissions();
      } catch {
        // 权限刷新失败不能覆盖原请求的 403。
        return Promise.reject(error);
      }
      ElMessage.error(resultMsg || "权限不足");
      return Promise.reject(error);
    }

    ElMessage.error(resultMsg || "请求失败");
    return Promise.reject(error);
  }
);

/**
 * 二进制保留响应头，JSON 返回业务 resultBody；Axios 泛型适配集中在此处。
 */
function request<T = Blob>(
  config: AxiosRequestConfig & { responseType: "blob" }
): Promise<AxiosResponse<T>>;
function request<T = ArrayBuffer>(
  config: AxiosRequestConfig & { responseType: "arraybuffer" }
): Promise<AxiosResponse<T>>;
function request<T = unknown, R = T>(config: AxiosRequestConfig): Promise<R>;
async function request(config: AxiosRequestConfig): Promise<unknown> {
  const response = await http.request<ResultInfo>(config);
  return config.responseType === "blob" || config.responseType === "arraybuffer"
    ? response
    : response.data.resultBody;
}

export default request;
