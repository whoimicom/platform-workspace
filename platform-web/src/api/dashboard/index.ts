import request from "@/utils/request";
import type { DashboardStats, UserTrend } from "./types";

const DASHBOARD_BASE_URL = "/api/dashboard";

const DashboardAPI = {
  /**
   * 首页统计卡片数据
   */
  getStats() {
    return request<unknown, DashboardStats>({
      url: `${DASHBOARD_BASE_URL}/stats`,
      method: "get",
    });
  },

  /**
   * 近 N 天用户增长趋势
   */
  getUserTrend(days: number) {
    return request<unknown, UserTrend>({
      url: `${DASHBOARD_BASE_URL}/user-trend`,
      method: "get",
      params: { days },
    });
  },
};

export default DashboardAPI;

export * from "./types";
