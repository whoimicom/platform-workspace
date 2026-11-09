import type { PageQuery } from "@/api/common";

/**
 * 租赁订单分页查询参数
 */
export interface RentalOrderQuery extends PageQuery {
  /** 订单号 / 身份证号 / 使用手机 关键字 */
  keywords?: string;
  /** 租户主体 HHHZ / HHX */
  tenant?: string;
  /** 订单状态 */
  status?: string;
  /** 渠道类型 */
  channelType?: string;
  /** 身份证号 */
  creSn?: string;
  /** 产品 ID */
  productId?: number;
  /** 商户 ID */
  merchantId?: number;
  /** 当前使用手机 */
  usePhone?: string;
  /** 会员申请状态 00/01/11/26/44/41 */
  applyStatus?: string;
  /** 会员租赁类型 cash/zuji */
  zlType?: string;
  /** 会员真实姓名 */
  realName?: string;
  /** 会员手机号 */
  mobile?: string;
  /** 创建时间起 */
  createTimeStart?: string;
  /** 创建时间止 */
  createTimeEnd?: string;
}

/**
 * 租赁订单列表行
 */
export interface RentalOrderItem {
  orderId: string;
  tenant?: string;
  channelType?: string;
  creSn?: string;
  status?: string;
  productId?: number;
  /** 收货方式 0-自提 1-邮寄 */
  addressType?: string;
  /** 租用数量 */
  number?: number;
  /** 订单金额 */
  amount?: number;
  /** 月租金 */
  monthAmount?: number;
  /** 押金 */
  deposit?: number;
  merchantId?: number;
  usePhone?: string;
  createUser?: string;
  createTime?: string;
  remark?: string;
  /** 会员真实姓名 */
  realName?: string;
  /** 会员手机号 */
  mobile?: string;
  /** 会员申请状态 */
  applyStatus?: string;
  /** 会员租赁类型 */
  zlType?: string;
  /** 会员产品名称 */
  productName?: string;
}

/**
 * 租赁订单表单对象（与 zl_order 字段一一对应）
 */
export interface RentalOrderForm {
  orderId?: string;
  /** 租户主体 HHHZ: 好货惠租；HHX: 换换星 */
  tenant?: string;
  channelType?: string;
  /** 身份证号 */
  creSn?: string;
  status?: string;
  productId?: number;
  /** 收货方式 0-自提 1-邮寄 */
  addressType?: string;
  /** 地址 JSON */
  addressJson?: string;
  /** 产品 SKU JSON */
  skuJson?: string;
  /** 1-到期买断/归还 2-租满即送 */
  productContractType?: string;
  borrowDays?: number;
  tenor?: number;
  number?: number;
  /** 1-指定还款 2-预收多期 4-次月为首期 */
  payType?: string;
  amount?: number;
  monthAmount?: number;
  deposit?: number;
  buyOutPrice?: number;
  updateTime?: string;
  createUser?: string;
  createTime?: string;
  /** 1-一次性结算 2-分次结算 */
  settleType?: string;
  merchantId?: number;
  merchantAmt?: number;
  merchantAfterAmt?: number;
  salesId?: number;
  payTenor?: number;
  payFirstAmount?: number;
  buyOutFlag?: string;
  brokenFee?: number;
  modifyFee?: number;
  lockFee?: number;
  remark?: string;
  /** 提货日期 yyyy-MM-dd */
  takeDate?: string;
  addressDeliver?: string;
  color?: string;
  refuseCode?: string;
  /** 非首期账单日 yyyy-MM-dd */
  accountDate?: string;
  approveUser?: string;
  approveTime?: string;
  finishTime?: string;
  /** 是否公证 0-否 1-是 */
  isNotarial?: string;
  /** 公证结果 -1/0/2/3/4 */
  notarialResult?: string;
  passCode?: string;
  /** 数据锁版本号 */
  version?: number;
  backDate?: string;
  backUser?: string;
  officialPrice?: number;
  keepEntry?: string;
  commonId?: number;
  memberFee?: number;
  claimUser?: string;
  claimTime?: string;
  channel?: string;
  merchantNo?: string;
  jobType?: string;
  jobDetail?: string;
  moreFirstAmount?: number;
  returnStatus?: string;
  usePhone?: string;
  guarantee?: string;
  passPoint?: string;
  riskPoint?: string;
  reportMsg?: string;
  subsidyAmount?: number;
  firstPayType?: string;
  sensitiveMsg?: string;
  firstPaySource?: string;
  followUser?: string;
  followTime?: string;
}
