import request from "@/utils/request";
import type { Pagination } from "@/api/common";
import type {
  DbColumnItem,
  DbTableItem,
  GenConfigDetail,
  GenConfigForm,
  GenConfigItem,
  GenFileItem,
  GenImportForm,
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
   * 指定表的字段列表（选择关联列）
   */
  getDbColumns(tableName: string) {
    return request<unknown, DbColumnItem[]>({
      url: `${GEN_BASE_URL}/db-tables/${tableName}/columns`,
      method: "get",
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
   * 导入多张表（重复导入返回已有配置）
   */
  createConfig(data: GenImportForm) {
    return request<unknown, number>({
      url: `${GEN_BASE_URL}/configs`,
      method: "post",
      data,
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

  /**
   * 生成代码并直接写入项目目录（覆盖同路径文件），返回写入的文件路径
   */
  generate(id: number) {
    return request<unknown, string[]>({
      url: `${GEN_BASE_URL}/configs/${id}/generate`,
      method: "post",
    });
  },
};

export default GenAPI;

export * from "./types";
