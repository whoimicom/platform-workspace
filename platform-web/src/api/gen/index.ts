import request from "@/utils/request";
import type { Pagination } from "@/api/common";
import type {
  DbTableItem,
  GenConfigDetail,
  GenConfigForm,
  GenConfigItem,
  GenFileItem,
  GenQuery,
} from "./types";

const GEN_BASE_URL = "/api/gen";

const GenAPI = {
  /**
   * 当前库内可导入的数据表
   */
  getDbTables(queryParams?: GenQuery) {
    return request<unknown, Pagination<DbTableItem>>({
      url: `${GEN_BASE_URL}/db-tables`,
      method: "get",
      params: queryParams,
    });
  },

  /**
   * 已保存的生成配置
   */
  getConfigs(queryParams?: GenQuery) {
    return request<unknown, Pagination<GenConfigItem>>({
      url: `${GEN_BASE_URL}/configs`,
      method: "get",
      params: queryParams,
    });
  },

  /**
   * 导入表（重复导入返回已有配置）
   */
  importTable(tableName: string) {
    return request<unknown, number>({
      url: `${GEN_BASE_URL}/configs/import/${tableName}`,
      method: "post",
    });
  },

  /**
   * 配置详情
   */
  getDetail(id: number) {
    return request<unknown, GenConfigDetail>({
      url: `${GEN_BASE_URL}/configs/${id}`,
      method: "get",
    });
  },

  /**
   * 保存配置
   */
  update(id: number, data: GenConfigForm) {
    return request({ url: `${GEN_BASE_URL}/configs/${id}`, method: "put", data });
  },

  /**
   * 删除配置
   */
  deleteByIds(ids: string) {
    return request({ url: `${GEN_BASE_URL}/configs/${ids}`, method: "delete" });
  },

  /**
   * 预览生成的代码
   */
  preview(id: number) {
    return request<unknown, GenFileItem[]>({
      url: `${GEN_BASE_URL}/configs/${id}/preview`,
      method: "get",
    });
  },
};

export default GenAPI;

export * from "./types";
