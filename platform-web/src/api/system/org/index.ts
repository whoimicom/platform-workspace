import request from "@/utils/request";
import type { OrgQuery, OrgItem, OrgForm } from "./types";
import type { OptionItem } from "@/api/common";

const ORG_BASE_URL = "/api/orgs";

const OrgAPI = {
  /**
   * 获取架构树形列表
   */
  getList(queryParams?: OrgQuery) {
    return request<unknown, OrgItem[]>({
      url: ORG_BASE_URL,
      method: "get",
      params: queryParams,
    });
  },
  /**
   * 获取架构下拉数据源
   */
  getOptions() {
    return request<unknown, OptionItem[]>({ url: `${ORG_BASE_URL}/options`, method: "get" });
  },
  /**
   * 获取架构表单数据
   */
  getFormData(id: string) {
    return request<unknown, OrgForm>({ url: `${ORG_BASE_URL}/${id}/form`, method: "get" });
  },
  /**
   * 新增架构
   */
  create(data: OrgForm) {
    return request({ url: ORG_BASE_URL, method: "post", data });
  },
  /**
   * 修改架构
   */
  update(id: string, data: OrgForm) {
    return request({ url: `${ORG_BASE_URL}/${id}`, method: "put", data });
  },
  /**
   * 批量删除架构，多个以英文逗号(,)分割
   */
  deleteByIds(ids: string) {
    return request({ url: `${ORG_BASE_URL}/${ids}`, method: "delete" });
  },
};

export default OrgAPI;

export * from "./types";
