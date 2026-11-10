package com.platform.api.rental.order;

import com.platform.api.common.query.Op;
import com.platform.api.common.query.PageQuery;
import com.platform.api.common.query.QueryField;
import com.platform.api.common.query.QuerySort;
import com.platform.api.common.query.QuerySortMapping;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 与前端 src/api/rental/order/types.ts 对应的数据结构。
 */
public final class RentalOrderDtos {

    private RentalOrderDtos() {
    }

    /**
     * 列表行：zl_order 关键字段 + zl_member 申请人信息（按 order_id 一对一关联）。
     */
    public record RentalOrderItem(String orderId, String tenant, String channelType, String creSn,
                                  String status, Long productId, String addressType, Integer number,
                                  BigDecimal amount, BigDecimal monthAmount, BigDecimal deposit,
                                  Long merchantId, String usePhone, String createUser,
                                  String createTime, String remark,
                                  String realName, String mobile, String applyStatus, String zlType,
                                  String productName) {
    }

    /**
     * 表单：与 zl_order 全部字段一一对应，用于新增回显与完整编辑。
     */
    public record RentalOrderForm(
            String orderId, String tenant, String channelType, String creSn, String status,
            Long productId, String addressType, String addressJson, String skuJson,
            String productContractType, Integer borrowDays, Integer tenor, Integer number,
            String payType, BigDecimal amount, BigDecimal monthAmount, BigDecimal deposit,
            BigDecimal buyOutPrice, String updateTime, String createUser, String createTime,
            String settleType, Long merchantId, BigDecimal merchantAmt, BigDecimal merchantAfterAmt,
            Long salesId, Integer payTenor, BigDecimal payFirstAmount, String buyOutFlag,
            BigDecimal brokenFee, BigDecimal modifyFee, BigDecimal lockFee, String remark,
            String takeDate, String addressDeliver, String color, String refuseCode,
            String accountDate, String approveUser, String approveTime, String finishTime,
            String isNotarial, String notarialResult, String passCode, Integer version,
            String backDate, String backUser, BigDecimal officialPrice, String keepEntry,
            Long commonId, BigDecimal memberFee, String claimUser, String claimTime,
            String channel, String merchantNo, String jobType, String jobDetail,
            BigDecimal moreFirstAmount, String returnStatus, String usePhone, String guarantee,
            String passPoint, String riskPoint, String reportMsg, BigDecimal subsidyAmount,
            String firstPayType, String sensitiveMsg, String firstPaySource,
            String followUser, String followTime) {
    }

    /**
     * 订单分页查询参数（查询串绑定使用）。
     */
    @QuerySort(value = {
            @QuerySortMapping(property = "orderId", column = "z.order_id"),
            @QuerySortMapping(property = "amount", column = "z.amount"),
            @QuerySortMapping(property = "monthAmount", column = "z.month_amount"),
            @QuerySortMapping(property = "createTime", column = "z.create_time"),
            @QuerySortMapping(property = "updateTime", column = "z.update_time")
    }, defaultSort = "z.create_time DESC, z.order_id DESC")
    public static class RentalOrderQuery extends PageQuery {

        /** 订单号 / 身份证号 / 使用手机 模糊匹配，由 Service 转成 OR 条件 */
        private String keywords;

        @QueryField(value = "z.tenant")
        private String tenant;

        @QueryField(value = "z.status")
        private String status;

        @QueryField(value = "z.channel_type", op = Op.LIKE)
        private String channelType;

        @QueryField(value = "z.cre_sn")
        private String creSn;

        @QueryField(value = "z.product_id")
        private Long productId;

        @QueryField(value = "z.merchant_id")
        private Long merchantId;

        @QueryField(value = "z.use_phone", op = Op.LIKE)
        private String usePhone;

        /** zl_member.apply_status：00-录入 01-提交签约 11-待审核 26-审核ok 44-拒绝 41-退货 */
        @QueryField(value = "m.apply_status")
        private String applyStatus;

        /** zl_member.zl_type：cash-我要现金 zuji-我要租机 */
        @QueryField(value = "m.zl_type")
        private String zlType;

        @QueryField(value = "m.real_name", op = Op.LIKE)
        private String realName;

        @QueryField(value = "m.mobile", op = Op.LIKE)
        private String mobile;

        /** 创建时间区间 [start, end] */
        @QueryField(value = "z.create_time", op = Op.BETWEEN)
        private LocalDateTime[] createTime;

        public String getKeywords() {
            return keywords;
        }

        public void setKeywords(String keywords) {
            this.keywords = keywords;
        }

        public String getTenant() {
            return tenant;
        }

        public void setTenant(String tenant) {
            this.tenant = tenant;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getChannelType() {
            return channelType;
        }

        public void setChannelType(String channelType) {
            this.channelType = channelType;
        }

        public String getCreSn() {
            return creSn;
        }

        public void setCreSn(String creSn) {
            this.creSn = creSn;
        }

        public Long getProductId() {
            return productId;
        }

        public void setProductId(Long productId) {
            this.productId = productId;
        }

        public Long getMerchantId() {
            return merchantId;
        }

        public void setMerchantId(Long merchantId) {
            this.merchantId = merchantId;
        }

        public String getUsePhone() {
            return usePhone;
        }

        public void setUsePhone(String usePhone) {
            this.usePhone = usePhone;
        }

        public String getApplyStatus() {
            return applyStatus;
        }

        public void setApplyStatus(String applyStatus) {
            this.applyStatus = applyStatus;
        }

        public String getZlType() {
            return zlType;
        }

        public void setZlType(String zlType) {
            this.zlType = zlType;
        }

        public String getRealName() {
            return realName;
        }

        public void setRealName(String realName) {
            this.realName = realName;
        }

        public String getMobile() {
            return mobile;
        }

        public void setMobile(String mobile) {
            this.mobile = mobile;
        }

        public LocalDateTime[] getCreateTime() {
            return createTime;
        }

        public void setCreateTime(LocalDateTime[] createTime) {
            this.createTime = createTime;
        }
    }
}
