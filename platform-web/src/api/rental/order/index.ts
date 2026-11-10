import request from "@/utils/request";
import type { Pagination } from "@/api/common";
import type {
  OrderDetail,
  OrderItem,
  OrderQuery,
  OrderSaveForm,
} from "./types";

const BASE_URL = "/api/rental/order";

const OrderAPI = {
  /**
     * 获取订单表分页数据
   */
  getPage(queryParams?: OrderQuery) {
    return request<unknown, Pagination<OrderItem>>({
      url: BASE_URL,
      method: "get",
      params: queryParams,
    });
  },

  /**
     * 主表详情
   */
  getDetail(id: string | number) {
    return request<unknown, OrderDetail>({
      url: `${BASE_URL}/${id}`,
      method: "get",
    });
  },

  /**
     * 新增（仅主表）
   */
  create(data: OrderSaveForm) {
    return request<unknown, string | number>({
      url: BASE_URL,
      method: "post",
      data,
    });
  },

  /**
     * 修改（仅主表）
   */
  update(id: string | number, data: OrderSaveForm) {
    return request({
      url: `${BASE_URL}/${id}`,
      method: "put",
      data,
    });
  },

  /**
     * 删除（仅主表）
   */
  remove(id: string | number) {
    return request({
      url: `${BASE_URL}/${id}`,
      method: "delete",
    });
  },
};

export default OrderAPI;

export * from "./types";
