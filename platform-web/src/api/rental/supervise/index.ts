import request from "@/utils/request";
import type { Pagination } from "@/api/common";
import type { SuperviseLockItem, SuperviseLockQuery } from "./types";

const SUPERVISE_LOCK_BASE_URL = "/api/rental/supervise-locks";

const SuperviseLockAPI = {
  /**
   * 获取设备锁分页数据
   */
  getPage(queryParams?: SuperviseLockQuery) {
    return request<unknown, Pagination<SuperviseLockItem>>({
      url: SUPERVISE_LOCK_BASE_URL,
      method: "get",
      params: queryParams,
    });
  },
};

export default SuperviseLockAPI;

export * from "./types";
