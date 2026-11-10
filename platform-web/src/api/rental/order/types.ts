import type { PageQuery } from "@/api/common";

/**
 * 订单表查询参数（字段来自 zl_order 及关联表）
 */
export interface OrderQuery extends PageQuery {
  /** 租户主体：HHHZ: 好货惠租；HHX: 换换星 */
  tenant?: string;
  /** 渠道类型 */
  channelType?: string;
  /** 订单状态 */
  status?: string;
  /** 收获方式：0-自提；1-邮寄 */
  addressType?: string;
  /** 1: 到期买断 / 归还2. 租满即送 */
  productContractType?: string;
}

/**
 * 订单表列表行
 */
export interface OrderItem {
  /** 个人申请编号（zl_member） */
  memberOrderId?: string;
  /** 状态：00-录入，01-提交签约,11-待审核，26-审核ok，44-拒绝，41-退货（zl_member） */
  memberApplyStatus?: string;
  /** 渠道类型：zfb,wx（zl_member） */
  memberChannelType?: string;
  /** 类型：cash-我要现金,zuji-我要租机（zl_member） */
  memberZlType?: string;
  /** 证件号码 作为先上传附件时唯一关联用（zl_member） */
  memberCreSn?: string;
  /** 手机号（zl_member） */
  memberMobile?: string;
  /** 类型: 租完即送（zl_member） */
  memberProductContractType?: string;
  /** 选择的商品id（zl_member） */
  memberProductSkuId?: number;
  /** 产品类型：1-新机；2-二手（zl_member） */
  memberProductOldNew?: string;
  /** 真实姓名（zl_member） */
  memberRealName?: string;
  /** 还款帐号（zl_member） */
  memberRepaymentAccount?: string;
  /** 银行名称（zl_member） */
  memberRepaymentBankName?: string;
  /** 还款开户行编号（zl_member） */
  memberRepaymentBank?: string;
  /** 绑定类型：3-三要素；4-四要素（zl_member） */
  memberBankBindType?: string;
  /** 签约方式：face-人脸。smsSign-短信签约（zl_member） */
  memberPdfSignType?: string;
  /** 短信签约手机号（zl_member） */
  memberPdfSmsSignPhone?: string;
  /** 等级（zl_member） */
  memberUserLevel?: string;
  /** 风险等级（zl_member） */
  memberRisk?: string;
  /** 推荐额度（zl_member） */
  memberRecommendQuota?: string;
  /** 准入信息（zl_member） */
  memberAccessMsg?: string;
  /** 产品id（zl_member） */
  memberLoanProduct?: string;
  /** 商品总金额（zl_member） */
  memberGoodsTotal?: string;
  /** 分期期数（zl_member） */
  memberPeriods?: string;
  /** 首次还款额（zl_member） */
  memberDownMoney?: string;
  /** 首次还款日（zl_member） */
  memberDownDate?: string;
  /** 每月还款额（zl_member） */
  memberPerMonthMoney?: string;
  /** 每月还款日（zl_member） */
  memberPerMonthDate?: string;
  /** 内存（zl_member） */
  memberMemory?: string;
  /** 颜色（zl_member） */
  memberColor?: string;
  /** 产品（zl_member） */
  memberProductName?: string;
  /** 品牌（zl_member） */
  memberPwAnswer?: string;
  /** 当为商品时替换为商品类型（zl_member） */
  memberBorrowReason?: string;
  /** 公众号简称（zl_member） */
  memberWechatCode?: string;
  /** 公众号名称（zl_member） */
  memberWechatName?: string;
  /** 公众号appid（zl_member） */
  memberWechatAppid?: string;
  /** 微信（zl_member） */
  memberWeixin?: string;
  /** qq（zl_member） */
  memberQq?: string;
  /** 芝麻分（zl_member） */
  memberZhiMaFen?: string;
  /** 民族（zl_member） */
  memberNationality?: string;
  /** 年龄（zl_member） */
  memberAge?: string;
  /** 性别：0-男，1-女（zl_member） */
  memberGender?: string;
  /** 出生日期（zl_member） */
  memberBirthday?: string;
  /** 身份证有效期(yyyymmdd)（zl_member） */
  memberCreValidStart?: string;
  /** 身份证有效期(yyyymmdd)（zl_member） */
  memberCreValid?: string;
  /** 发证机构（zl_member） */
  memberBodies?: string;
  /** 户籍地址：省/直辖市（zl_member） */
  memberCensusProvince?: string;
  /** 户籍地址：城市（zl_member） */
  memberCensusCity?: string;
  /** 户籍地址：区/县/镇（zl_member） */
  memberCensusDistrict?: string;
  /** 户籍地址邮编（zl_member） */
  memberCunsusZip?: string;
  /** 户籍地址：除去省市区后的（zl_member） */
  memberCensusAddr?: string;
  /** 户籍详细地址：省/直辖市，城市，区/县，镇，街/村，居民区，楼号，层号，房间号（zl_member） */
  memberCensusAddrDetail?: string;
  /** 现居住地址:省/直辖市（zl_member） */
  memberLiveProvince?: string;
  /** 现居住地址:城市（zl_member） */
  memberLiveCity?: string;
  /** 现居住地址:区/县/镇（zl_member） */
  memberLiveDistrict?: string;
  /** 现居住地址:邮编（zl_member） */
  memberLiveZip?: string;
  /** 现居住地址:除去省市区后的（zl_member） */
  memberLiveAddr?: string;
  /** 完整现居住地址（zl_member） */
  memberLiveAddrDetail?: string;
  /** 家庭宽带（zl_member） */
  memberLiveKuandai?: string;
  /** 单位名称（zl_member） */
  memberCompanyName?: string;
  /** 单位地址：省/直辖市（zl_member） */
  memberCompanyProvince?: string;
  /** 单位地址：城市（zl_member） */
  memberCompanyCity?: string;
  /** 单位地址：区/县/镇（zl_member） */
  memberCompanyDistrict?: string;
  /** 单位地址：邮编（zl_member） */
  memberCompanyZip?: string;
  /** 单位地址:除去省市区后的（zl_member） */
  memberCompanyAddr?: string;
  /** 完整单位地址（zl_member） */
  memberCompanyAddrDetail?: string;
  /** 单位电话（zl_member） */
  memberCompanyTel?: string;
  /** 工作年限（zl_member） */
  memberWorkYears?: string;
  /** 工资收入（zl_member） */
  memberWorkMoneys?: string;
  /** 收入方式（zl_member） */
  memberWorkMoneyType?: string;
  /** 婚姻状况:0-未婚、1-已婚有子女、2-已婚无子女、3-离异、4-丧偶（zl_member） */
  memberWedlock?: string;
  /** 配偶姓名（zl_member） */
  memberSpouseName?: string;
  /** 配偶联系电话（zl_member） */
  memberSpouseTel?: string;
  /** 父母1姓名（zl_member） */
  memberParentsName?: string;
  /** 申请人关系（zl_member） */
  memberParentsRel?: string;
  /** 父母1电话（zl_member） */
  memberParentsTel?: string;
  /** 父母2姓名（zl_member） */
  memberParents2Name?: string;
  /** 申请人关系（zl_member） */
  memberParents2Rel?: string;
  /** 父母2电话（zl_member） */
  memberParents2Tel?: string;
  /** 家人姓名（zl_member） */
  memberFamily1Name?: string;
  /** 家人关系（zl_member） */
  memberFamily1Rel?: string;
  /** 家人电话（zl_member） */
  memberFamily1Tel?: string;
  /** 紧急联系人姓名（zl_member） */
  memberContactName?: string;
  /** 关系（zl_member） */
  memberContactRel?: string;
  /** 紧急联系人电话（zl_member） */
  memberContactTel?: string;
  /** gps经度（zl_member） */
  memberGpsLng?: string;
  /** gps纬度（zl_member） */
  memberGpsLat?: string;
  /** 腾讯经度（zl_member） */
  memberTencentLng?: string;
  /** 腾讯纬度（zl_member） */
  memberTencentLat?: string;
  /** longitude（zl_member） */
  memberLongitude?: string;
  /** latitude（zl_member） */
  memberLatitude?: string;
  /** record_address（zl_member） */
  memberRecordAddress?: string;
  /** app类型（zl_member） */
  memberAppType?: string;
  /** 手机号时长（zl_member） */
  memberPhoneTime?: string;
  /** 手机号姓名身份证校验（zl_member） */
  memberValidateData?: string;
  /** 是否携号（zl_member） */
  memberIsXhzw?: string;
  /** 运营商（zl_member） */
  memberOperator?: string;
  /** 原运营商（zl_member） */
  memberOperatorYuan?: string;
  /** 手机号时长id（zl_member） */
  memberPtSmsIspId?: number;
  /** 实名验证id（zl_member） */
  memberVdSmsIspId?: number;
  /** 签章商户（zl_member） */
  memberSignShanghu?: string;
  /** 额外需要的合同类型（zl_member） */
  memberOtherTemplateType?: string;
  /** 创建时间（zl_member） */
  memberCreatTime?: string;
  /** update_time（zl_member） */
  memberUpdateTime?: string;
  /** 人脸认证序列号（zl_member） */
  memberSerialNumber?: string;
  /** 撤销黑名单首付限制(N:取消)（zl_member） */
  memberRevokeFlag?: string;
  /** 撤销人（zl_member） */
  memberRevokeUser?: string;
  /** 物流状态（zl_member） */
  memberDeliveryStatus?: string;
  /** 订单号（zl_order） */
  orderId?: string;
  /** 租户主体：HHHZ: 好货惠租；HHX: 换换星（zl_order） */
  tenant?: string;
  /** 渠道类型（zl_order） */
  channelType?: string;
  /** 身份证号（zl_order） */
  creSn?: string;
  /** 订单状态（zl_order） */
  status?: string;
  /** 产品Id（zl_order） */
  productId?: number;
  /** 收获方式：0-自提；1-邮寄（zl_order） */
  addressType?: string;
  /** 1: 到期买断 / 归还2. 租满即送（zl_order） */
  productContractType?: string;
  /** 借款天数（zl_order） */
  borrowDays?: number;
  /** 期数（zl_order） */
  tenor?: number;
  /** 租用数量（zl_order） */
  number?: number;
  /** 支付方式：1-指定还款;2-预收多期;4:次月为首期（zl_order） */
  payType?: string;
  /** 订单金额（zl_order） */
  amount?: number;
  /** 月租金（zl_order） */
  monthAmount?: number;
  /** 押金（zl_order） */
  deposit?: number;
  /** 买断价（zl_order） */
  buyOutPrice?: number;
  /** 修改时间（zl_order） */
  updateTime?: string;
  /** 创建人（zl_order） */
  createUser?: string;
  /** 创建时间（zl_order） */
  createTime?: string;
  /** 商户结算方式(1:一次性结算 2:分次结算)（zl_order） */
  settleType?: string;
  /** 商户ID（zl_order） */
  merchantId?: number;
  /** 商户结算总金额（zl_order） */
  merchantAmt?: number;
  /** 商户延期结算金额（zl_order） */
  merchantAfterAmt?: number;
  /** 业代ID（zl_order） */
  salesId?: number;
  /** 支付期数（zl_order） */
  payTenor?: number;
  /** 首付金额（zl_order） */
  payFirstAmount?: number;
  /** 买断标识(是否需要支付)（zl_order） */
  buyOutFlag?: string;
  /** 碎屏险（zl_order） */
  brokenFee?: number;
  /** 维修费（zl_order） */
  modifyFee?: number;
  /** 锁机费（zl_order） */
  lockFee?: number;
  /** 备注（zl_order） */
  remark?: string;
  /** 提货日期（zl_order） */
  takeDate?: string;
  /** 交付地址（zl_order） */
  addressDeliver?: string;
  /** 颜色（zl_order） */
  color?: string;
  /** 拒绝码（zl_order） */
  refuseCode?: string;
  /** 非首期账单日（zl_order） */
  accountDate?: string;
  /** 审核人（zl_order） */
  approveUser?: string;
  /** 审核时间（zl_order） */
  approveTime?: string;
  /** 订单完成时间（zl_order） */
  finishTime?: string;
  /** 是否公证(0:否 1:是)（zl_order） */
  isNotarial?: string;
  /** 公证结果(-1:审核拒绝 0:未公证 2:公证办理中 3:已发证 4:撤销公证)（zl_order） */
  notarialResult?: string;
  /** 审核通过代码（zl_order） */
  passCode?: string;
  /** 数据锁（zl_order） */
  version?: number;
  /** 退货时间（zl_order） */
  backDate?: string;
  /** 退货人（zl_order） */
  backUser?: string;
  /** 官网价格（zl_order） */
  officialPrice?: number;
  /** 提前归还保持入账标识(0:否 1:是)（zl_order） */
  keepEntry?: string;
  /** 公共账户ID（zl_order） */
  commonId?: number;
  /** 会员费（zl_order） */
  memberFee?: number;
  /** claim_user（zl_order） */
  claimUser?: string;
  /** claim_time（zl_order） */
  claimTime?: string;
  /** 扣款渠道(SNXY:苏宁 ZJXY:中金)（zl_order） */
  channel?: string;
  /** 扣款商户（zl_order） */
  merchantNo?: string;
  /** 职业类型（zl_order） */
  jobType?: string;
  /** 职业（zl_order） */
  jobDetail?: string;
  /** 首付增加金额（zl_order） */
  moreFirstAmount?: number;
  /** 产品状态（zl_order） */
  returnStatus?: string;
  /** 当前使用手机（zl_order） */
  usePhone?: string;
  /** 是否担保（zl_order） */
  guarantee?: string;
  /** 补贴金额（zl_order） */
  subsidyAmount?: number;
  /** 首付支付方式（zl_order） */
  firstPayType?: string;
  /** 审核敏感词（zl_order） */
  sensitiveMsg?: string;
  /** 首付来源（zl_order） */
  firstPaySource?: string;
  /** 跟进人（zl_order） */
  followUser?: string;
  /** 跟进时间（zl_order） */
  followTime?: string;
  /** id（zl_supervise_lock） */
  superviseLockId?: number;
  /** 订单号（zl_supervise_lock） */
  superviseLockOrderId?: string;
  /** 监管ID（zl_supervise_lock） */
  superviseLockSuperviseId?: number;
  /** 监管名称（zl_supervise_lock） */
  superviseLockSuperviseName?: string;
  /** 序列号（zl_supervise_lock） */
  superviseLockSerialNumber?: string;
  /** 是否上锁(1:已锁 0:正常)（zl_supervise_lock） */
  superviseLockStatus?: string;
  /** 是否支持接口(0:否 1:是)（zl_supervise_lock） */
  superviseLockIsInterface?: string;
  /** 正常壁纸（zl_supervise_lock） */
  superviseLockNormalWallpaper?: number;
  /** activationLockStatus 激活锁状态（zl_supervise_lock） */
  superviseLockActivationLockStatus?: string;
  /** 是否处于监管（zl_supervise_lock） */
  superviseLockSupervise?: number;
  /** 在线状态（zl_supervise_lock） */
  superviseLockOnlineStatus?: number;
  /** ABM在库状态（zl_supervise_lock） */
  superviseLockAbmStatus?: string;
  /** 备注（zl_supervise_lock） */
  superviseLockRemark?: string;
  /** 修改人（zl_supervise_lock） */
  superviseLockUpdateUser?: string;
  /** 创建人（zl_supervise_lock） */
  superviseLockCreateUser?: string;
  /** 修改时间（zl_supervise_lock） */
  superviseLockUpdateTime?: string;
  /** 创建时间（zl_supervise_lock） */
  superviseLockCreateTime?: string;
  /** 系统类型（zl_supervise_lock） */
  superviseLockSysType?: string;
  /** 品牌（zl_supervise_lock） */
  superviseLockBrandId?: string;
  /** 串码（zl_supervise_lock） */
  superviseLockSerialCode?: string;
  /** 手机型号(vivo锁)（zl_supervise_lock） */
  superviseLockModel?: string;
  /** 监管码(vivo锁)（zl_supervise_lock） */
  superviseLockSuperviseCode?: string;
  /** 监管码2(vivo锁)（zl_supervise_lock） */
  superviseLockSerialCode2?: string;
  /** 租户主体（zl_supervise_lock） */
  superviseLockTenant?: string;
  /** APP限制(0:否 1:是)（zl_supervise_lock） */
  superviseLockRestriction?: number;
  /** 最后活动时间（zl_supervise_lock） */
  superviseLockLastActiveTime?: string;
  /** 处理日期（zl_supervise_lock） */
  superviseLockDealDate?: string;
}

/**
 * 订单表详情（主表全部字段）
 */
export interface OrderDetail {
  /** 订单号 */
  orderId?: string;
  /** 租户主体：HHHZ: 好货惠租；HHX: 换换星 */
  tenant?: string;
  /** 渠道类型 */
  channelType?: string;
  /** 身份证号 */
  creSn?: string;
  /** 订单状态 */
  status?: string;
  /** 产品Id */
  productId?: number;
  /** 收获方式：0-自提；1-邮寄 */
  addressType?: string;
  /** 地址 */
  addressJson?: string;
  /** 产品sku */
  skuJson?: string;
  /** 1: 到期买断 / 归还2. 租满即送 */
  productContractType?: string;
  /** 借款天数 */
  borrowDays?: number;
  /** 期数 */
  tenor?: number;
  /** 租用数量 */
  number?: number;
  /** 支付方式：1-指定还款;2-预收多期;4:次月为首期 */
  payType?: string;
  /** 订单金额 */
  amount?: number;
  /** 月租金 */
  monthAmount?: number;
  /** 押金 */
  deposit?: number;
  /** 买断价 */
  buyOutPrice?: number;
  /** 修改时间 */
  updateTime?: string;
  /** 创建人 */
  createUser?: string;
  /** 创建时间 */
  createTime?: string;
  /** 商户结算方式(1:一次性结算 2:分次结算) */
  settleType?: string;
  /** 商户ID */
  merchantId?: number;
  /** 商户结算总金额 */
  merchantAmt?: number;
  /** 商户延期结算金额 */
  merchantAfterAmt?: number;
  /** 业代ID */
  salesId?: number;
  /** 支付期数 */
  payTenor?: number;
  /** 首付金额 */
  payFirstAmount?: number;
  /** 买断标识(是否需要支付) */
  buyOutFlag?: string;
  /** 碎屏险 */
  brokenFee?: number;
  /** 维修费 */
  modifyFee?: number;
  /** 锁机费 */
  lockFee?: number;
  /** 备注 */
  remark?: string;
  /** 提货日期 */
  takeDate?: string;
  /** 交付地址 */
  addressDeliver?: string;
  /** 颜色 */
  color?: string;
  /** 拒绝码 */
  refuseCode?: string;
  /** 非首期账单日 */
  accountDate?: string;
  /** 审核人 */
  approveUser?: string;
  /** 审核时间 */
  approveTime?: string;
  /** 订单完成时间 */
  finishTime?: string;
  /** 是否公证(0:否 1:是) */
  isNotarial?: string;
  /** 公证结果(-1:审核拒绝 0:未公证 2:公证办理中 3:已发证 4:撤销公证) */
  notarialResult?: string;
  /** 审核通过代码 */
  passCode?: string;
  /** 数据锁 */
  version?: number;
  /** 退货时间 */
  backDate?: string;
  /** 退货人 */
  backUser?: string;
  /** 官网价格 */
  officialPrice?: number;
  /** 提前归还保持入账标识(0:否 1:是) */
  keepEntry?: string;
  /** 公共账户ID */
  commonId?: number;
  /** 会员费 */
  memberFee?: number;
  /** claim_user */
  claimUser?: string;
  /** claim_time */
  claimTime?: string;
  /** 扣款渠道(SNXY:苏宁 ZJXY:中金) */
  channel?: string;
  /** 扣款商户 */
  merchantNo?: string;
  /** 职业类型 */
  jobType?: string;
  /** 职业 */
  jobDetail?: string;
  /** 首付增加金额 */
  moreFirstAmount?: number;
  /** 产品状态 */
  returnStatus?: string;
  /** 当前使用手机 */
  usePhone?: string;
  /** 是否担保 */
  guarantee?: string;
  /** 通过支撑点 */
  passPoint?: string;
  /** 风险点 */
  riskPoint?: string;
  /** 保单信息 */
  reportMsg?: string;
  /** 补贴金额 */
  subsidyAmount?: number;
  /** 首付支付方式 */
  firstPayType?: string;
  /** 审核敏感词 */
  sensitiveMsg?: string;
  /** 首付来源 */
  firstPaySource?: string;
  /** 跟进人 */
  followUser?: string;
  /** 跟进时间 */
  followTime?: string;
}

/**
 * 订单表新增/编辑表单（仅主表可编辑字段）
 */
export interface OrderSaveForm {
  /** 订单号 */
  orderId?: string;
  /** 租户主体：HHHZ: 好货惠租；HHX: 换换星 */
  tenant?: string;
  /** 渠道类型 */
  channelType?: string;
  /** 身份证号 */
  creSn?: string;
  /** 订单状态 */
  status?: string;
  /** 产品Id */
  productId?: number;
  /** 收获方式：0-自提；1-邮寄 */
  addressType?: string;
  /** 地址 */
  addressJson?: string;
  /** 产品sku */
  skuJson?: string;
  /** 1: 到期买断 / 归还2. 租满即送 */
  productContractType?: string;
  /** 借款天数 */
  borrowDays?: number;
  /** 期数 */
  tenor?: number;
  /** 租用数量 */
  number?: number;
  /** 支付方式：1-指定还款;2-预收多期;4:次月为首期 */
  payType?: string;
  /** 订单金额 */
  amount?: number;
  /** 月租金 */
  monthAmount?: number;
  /** 押金 */
  deposit?: number;
  /** 买断价 */
  buyOutPrice?: number;
  /** 修改时间 */
  updateTime?: string;
  /** 创建人 */
  createUser?: string;
  /** 创建时间 */
  createTime?: string;
  /** 商户结算方式(1:一次性结算 2:分次结算) */
  settleType?: string;
  /** 商户ID */
  merchantId?: number;
  /** 商户结算总金额 */
  merchantAmt?: number;
  /** 商户延期结算金额 */
  merchantAfterAmt?: number;
  /** 业代ID */
  salesId?: number;
  /** 支付期数 */
  payTenor?: number;
  /** 首付金额 */
  payFirstAmount?: number;
  /** 买断标识(是否需要支付) */
  buyOutFlag?: string;
  /** 碎屏险 */
  brokenFee?: number;
  /** 维修费 */
  modifyFee?: number;
  /** 锁机费 */
  lockFee?: number;
  /** 备注 */
  remark?: string;
  /** 提货日期 */
  takeDate?: string;
  /** 交付地址 */
  addressDeliver?: string;
  /** 颜色 */
  color?: string;
  /** 拒绝码 */
  refuseCode?: string;
  /** 非首期账单日 */
  accountDate?: string;
  /** 审核人 */
  approveUser?: string;
  /** 审核时间 */
  approveTime?: string;
  /** 订单完成时间 */
  finishTime?: string;
  /** 是否公证(0:否 1:是) */
  isNotarial?: string;
  /** 公证结果(-1:审核拒绝 0:未公证 2:公证办理中 3:已发证 4:撤销公证) */
  notarialResult?: string;
  /** 审核通过代码 */
  passCode?: string;
  /** 数据锁 */
  version?: number;
  /** 退货时间 */
  backDate?: string;
  /** 退货人 */
  backUser?: string;
  /** 官网价格 */
  officialPrice?: number;
  /** 提前归还保持入账标识(0:否 1:是) */
  keepEntry?: string;
  /** 公共账户ID */
  commonId?: number;
  /** 会员费 */
  memberFee?: number;
  /** claim_user */
  claimUser?: string;
  /** claim_time */
  claimTime?: string;
  /** 扣款渠道(SNXY:苏宁 ZJXY:中金) */
  channel?: string;
  /** 扣款商户 */
  merchantNo?: string;
  /** 职业类型 */
  jobType?: string;
  /** 职业 */
  jobDetail?: string;
  /** 首付增加金额 */
  moreFirstAmount?: number;
  /** 产品状态 */
  returnStatus?: string;
  /** 当前使用手机 */
  usePhone?: string;
  /** 是否担保 */
  guarantee?: string;
  /** 通过支撑点 */
  passPoint?: string;
  /** 风险点 */
  riskPoint?: string;
  /** 保单信息 */
  reportMsg?: string;
  /** 补贴金额 */
  subsidyAmount?: number;
  /** 首付支付方式 */
  firstPayType?: string;
  /** 审核敏感词 */
  sensitiveMsg?: string;
  /** 首付来源 */
  firstPaySource?: string;
  /** 跟进人 */
  followUser?: string;
  /** 跟进时间 */
  followTime?: string;
}
