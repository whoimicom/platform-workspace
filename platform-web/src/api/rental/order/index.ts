import request from "@/utils/request";
import type { RentalOrderQuery, RentalOrderForm, RentalOrderItem } from "./types";
import type { Pagination } from "@/api/common";

const RENTAL_ORDER_BASE_URL = "/api/rental/orders";

const RentalOrderAPI = {
  /**
   * 获取租赁订单分页数据
   */
  getPage(queryParams?: RentalOrderQuery) {
    return request<unknown, Pagination<RentalOrderItem>>({
      url: RENTAL_ORDER_BASE_URL,
      method: "get",
      params: queryParams,
    });
  },
  /**
   * 获取租赁订单表单数据
   */
  getFormData(orderId: string) {
    return request<unknown, RentalOrderForm>({
      url: `${RENTAL_ORDER_BASE_URL}/${orderId}/form`,
      method: "get",
    });
  },
  /**
   * 新增租赁订单
   */
  create(data: RentalOrderForm) {
    return request<unknown, string>({ url: RENTAL_ORDER_BASE_URL, method: "post", data });
  },
  /**
   * 修改租赁订单
   */
  update(orderId: string, data: RentalOrderForm) {
    return request({ url: `${RENTAL_ORDER_BASE_URL}/${orderId}`, method: "put", data });
  },
  /**
   * 删除租赁订单
   */
  deleteById(orderId: string) {
    return request({ url: `${RENTAL_ORDER_BASE_URL}/${orderId}`, method: "delete" });
  },
};

export default RentalOrderAPI;

export * from "./types";
