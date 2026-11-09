package com.platform.api.rental.order;

import com.platform.api.common.BusinessException;
import com.platform.api.common.query.DynamicQueryExecutor;
import com.platform.api.common.query.RawCondition;
import com.platform.api.common.query.RecordRowMapper;
import com.platform.api.security.LoginUser;
import org.springframework.data.domain.Page;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.sql.Types;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 租赁订单 CRUD。zl_order 以订单号作为主键，update/delete 均按订单号定位。
 */
@Service
public class RentalOrderService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter ORDER_ID_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    /**
     * 全字段查询：日期时间统一格式化为字符串，JSON 列按原文本返回，供列表与表单复用。
     */
    private static final String SELECT_SQL = """
            SELECT z.order_id AS order_id,
                   z.tenant AS tenant,
                   z.channel_type AS channel_type,
                   z.cre_sn AS cre_sn,
                   z.status AS status,
                   z.product_id AS product_id,
                   z.address_type AS address_type,
                   z.address_json AS address_json,
                   z.sku_json AS sku_json,
                   z.product_contract_type AS product_contract_type,
                   z.borrow_days AS borrow_days,
                   z.tenor AS tenor,
                   z.number AS number,
                   z.pay_type AS pay_type,
                   z.amount AS amount,
                   z.month_amount AS month_amount,
                   z.deposit AS deposit,
                   z.buy_out_price AS buy_out_price,
                   DATE_FORMAT(z.update_time, '%Y-%m-%d %H:%i:%s') AS update_time,
                   z.create_user AS create_user,
                   DATE_FORMAT(z.create_time, '%Y-%m-%d %H:%i:%s') AS create_time,
                   z.settle_type AS settle_type,
                   z.merchant_id AS merchant_id,
                   z.merchant_amt AS merchant_amt,
                   z.merchant_after_amt AS merchant_after_amt,
                   z.sales_id AS sales_id,
                   z.pay_tenor AS pay_tenor,
                   z.pay_first_amount AS pay_first_amount,
                   z.buy_out_flag AS buy_out_flag,
                   z.broken_fee AS broken_fee,
                   z.modify_fee AS modify_fee,
                   z.lock_fee AS lock_fee,
                   z.remark AS remark,
                   DATE_FORMAT(z.take_date, '%Y-%m-%d') AS take_date,
                   z.address_deliver AS address_deliver,
                   z.color AS color,
                   z.refuse_code AS refuse_code,
                   DATE_FORMAT(z.account_date, '%Y-%m-%d') AS account_date,
                   z.approve_user AS approve_user,
                   DATE_FORMAT(z.approve_time, '%Y-%m-%d %H:%i:%s') AS approve_time,
                   DATE_FORMAT(z.finish_time, '%Y-%m-%d %H:%i:%s') AS finish_time,
                   z.is_notarial AS is_notarial,
                   z.notarial_result AS notarial_result,
                   z.pass_code AS pass_code,
                   z.version AS version,
                   DATE_FORMAT(z.back_date, '%Y-%m-%d %H:%i:%s') AS back_date,
                   z.back_user AS back_user,
                   z.official_price AS official_price,
                   z.keep_entry AS keep_entry,
                   z.common_id AS common_id,
                   z.member_fee AS member_fee,
                   z.claim_user AS claim_user,
                   DATE_FORMAT(z.claim_time, '%Y-%m-%d %H:%i:%s') AS claim_time,
                   z.channel AS channel,
                   z.merchant_no AS merchant_no,
                   z.job_type AS job_type,
                   z.job_detail AS job_detail,
                   z.more_first_amount AS more_first_amount,
                   z.return_status AS return_status,
                   z.use_phone AS use_phone,
                   z.guarantee AS guarantee,
                   z.pass_point AS pass_point,
                   z.risk_point AS risk_point,
                   z.report_msg AS report_msg,
                   z.subsidy_amount AS subsidy_amount,
                   z.first_pay_type AS first_pay_type,
                   z.sensitive_msg AS sensitive_msg,
                   z.first_pay_source AS first_pay_source,
                   z.follow_user AS follow_user,
                   DATE_FORMAT(z.follow_time, '%Y-%m-%d %H:%i:%s') AS follow_time,
                   m.real_name AS real_name,
                   m.mobile AS mobile,
                   m.apply_status AS apply_status,
                   m.zl_type AS zl_type,
                   m.product_name AS product_name
              FROM zl_order z
              LEFT JOIN zl_member m ON m.order_id = z.order_id
            """;

    /**
     * 总数统计：只统计主表行数，不重复投影 70 列，也不再套派生表；
     * 保留 LEFT JOIN 是因为筛选条件会用到 m.apply_status / m.real_name 等会员字段。
     */
    private static final String COUNT_SQL = """
            SELECT COUNT(*) FROM zl_order z
              LEFT JOIN zl_member m ON m.order_id = z.order_id
            """;

    private static final String INSERT_SQL = """
            INSERT INTO zl_order (order_id, tenant, channel_type, cre_sn, status, product_id,
                address_type, address_json, sku_json, product_contract_type, borrow_days, tenor,
                number, pay_type, amount, month_amount, deposit, buy_out_price, update_time,
                create_user, create_time, settle_type, merchant_id, merchant_amt, merchant_after_amt,
                sales_id, pay_tenor, pay_first_amount, buy_out_flag, broken_fee, modify_fee, lock_fee,
                remark, take_date, address_deliver, color, refuse_code, account_date, approve_user,
                approve_time, finish_time, is_notarial, notarial_result, pass_code, version, back_date,
                back_user, official_price, keep_entry, common_id, member_fee, claim_user, claim_time,
                channel, merchant_no, job_type, job_detail, more_first_amount, return_status, use_phone,
                guarantee, pass_point, risk_point, report_msg, subsidy_amount, first_pay_type,
                sensitive_msg, first_pay_source, follow_user, follow_time)
            VALUES (:orderId, :tenant, :channelType, :creSn, :status, :productId,
                :addressType, :addressJson, :skuJson, :productContractType, :borrowDays, :tenor,
                :number, :payType, :amount, :monthAmount, :deposit, :buyOutPrice, :updateTime,
                :createUser, :createTime, :settleType, :merchantId, :merchantAmt, :merchantAfterAmt,
                :salesId, :payTenor, :payFirstAmount, :buyOutFlag, :brokenFee, :modifyFee, :lockFee,
                :remark, :takeDate, :addressDeliver, :color, :refuseCode, :accountDate, :approveUser,
                :approveTime, :finishTime, :isNotarial, :notarialResult, :passCode, :version, :backDate,
                :backUser, :officialPrice, :keepEntry, :commonId, :memberFee, :claimUser, :claimTime,
                :channel, :merchantNo, :jobType, :jobDetail, :moreFirstAmount, :returnStatus, :usePhone,
                :guarantee, :passPoint, :riskPoint, :reportMsg, :subsidyAmount, :firstPayType,
                :sensitiveMsg, :firstPaySource, :followUser, :followTime)
            """;

    /**
     * 创建人 / 创建时间属于审计字段，更新时不允许覆盖；version 递增用于乐观锁。
     */
    private static final String UPDATE_SQL = """
            UPDATE zl_order
               SET tenant = :tenant, channel_type = :channelType, cre_sn = :creSn, status = :status,
                   product_id = :productId, address_type = :addressType, address_json = :addressJson,
                   sku_json = :skuJson, product_contract_type = :productContractType,
                   borrow_days = :borrowDays, tenor = :tenor, number = :number, pay_type = :payType,
                   amount = :amount, month_amount = :monthAmount, deposit = :deposit,
                   buy_out_price = :buyOutPrice, settle_type = :settleType, merchant_id = :merchantId,
                   merchant_amt = :merchantAmt, merchant_after_amt = :merchantAfterAmt,
                   sales_id = :salesId, pay_tenor = :payTenor, pay_first_amount = :payFirstAmount,
                   buy_out_flag = :buyOutFlag, broken_fee = :brokenFee, modify_fee = :modifyFee,
                   lock_fee = :lockFee, remark = :remark, take_date = :takeDate,
                   address_deliver = :addressDeliver, color = :color, refuse_code = :refuseCode,
                   account_date = :accountDate, approve_user = :approveUser, approve_time = :approveTime,
                   finish_time = :finishTime, is_notarial = :isNotarial, notarial_result = :notarialResult,
                   pass_code = :passCode, back_date = :backDate, back_user = :backUser,
                   official_price = :officialPrice, keep_entry = :keepEntry, common_id = :commonId,
                   member_fee = :memberFee, claim_user = :claimUser, claim_time = :claimTime,
                   channel = :channel, merchant_no = :merchantNo, job_type = :jobType,
                   job_detail = :jobDetail, more_first_amount = :moreFirstAmount,
                   return_status = :returnStatus, use_phone = :usePhone, guarantee = :guarantee,
                   pass_point = :passPoint, risk_point = :riskPoint, report_msg = :reportMsg,
                   subsidy_amount = :subsidyAmount, first_pay_type = :firstPayType,
                   sensitive_msg = :sensitiveMsg, first_pay_source = :firstPaySource,
                   follow_user = :followUser, follow_time = :followTime,
                   update_time = :updateTime,
                   version = version + 1
             WHERE order_id = :orderId AND version = :version
            """;

    private final NamedParameterJdbcTemplate jdbc;
    private final DynamicQueryExecutor executor;
    private final JsonMapper jsonMapper;

    public RentalOrderService(NamedParameterJdbcTemplate jdbc, DynamicQueryExecutor executor,
                              JsonMapper jsonMapper) {
        this.jdbc = jdbc;
        this.executor = executor;
        this.jsonMapper = jsonMapper;
    }

    /**
     * 订单分页：keywords 在订单号 / 身份证号 / 使用手机 / 会员姓名 / 会员手机号之间做 OR 模糊匹配。
     */
    public Page<RentalOrderDtos.RentalOrderItem> page(RentalOrderDtos.RentalOrderQuery query) {
        List<RawCondition> extras = new ArrayList<>();
        if (query.getKeywords() != null && !query.getKeywords().isBlank()) {
            String keyword = "%" + query.getKeywords().trim() + "%";
            extras.add(RawCondition.of("(z.order_id LIKE :keywords OR z.cre_sn LIKE :keywords "
                    + "OR z.use_phone LIKE :keywords OR m.real_name LIKE :keywords "
                    + "OR m.mobile LIKE :keywords)", Map.of("keywords", keyword)));
        }
        return executor.page(SELECT_SQL, COUNT_SQL, query,
                RecordRowMapper.of(RentalOrderDtos.RentalOrderItem.class),
                extras.toArray(RawCondition[]::new));
    }

    public RentalOrderDtos.RentalOrderForm form(String orderId) {
        return loadForm(orderId);
    }

    @Transactional
    public String create(RentalOrderDtos.RentalOrderForm form, LoginUser loginUser) {
        validate(form);
        String orderId = normalizeOrderId(form.orderId());
        if (exists(orderId)) {
            throw BusinessException.badRequest("订单号已存在：" + orderId);
        }

        MapSqlParameterSource params = new MapSqlParameterSource();
        applyParams(params, form);
        params.addValue("orderId", orderId);
        params.addValue("createUser", textOrDefault(form.createUser(),
                loginUser == null ? null : loginUser.username()));
        params.addValue("createTime", textOrDefault(form.createTime(), nowText()));
        params.addValue("updateTime", nowText());
        params.addValue("version", 0);
        jdbc.update(INSERT_SQL, params);
        return orderId;
    }

    @Transactional
    public void update(String orderId, RentalOrderDtos.RentalOrderForm form) {
        validate(form);
        RentalOrderDtos.RentalOrderForm current = loadForm(orderId);
        int version = current.version() == null ? 0 : current.version();
        if (form.version() != null && form.version() != version) {
            throw BusinessException.badRequest("订单已被其他操作修改，请刷新后重试");
        }

        MapSqlParameterSource params = new MapSqlParameterSource();
        applyParams(params, form);
        params.addValue("orderId", orderId);
        params.addValue("updateTime", nowText());
        params.addValue("version", version);
        if (jdbc.update(UPDATE_SQL, params) == 0) {
            throw BusinessException.badRequest("订单已被其他操作修改，请刷新后重试");
        }
    }

    @Transactional
    public void delete(String ids) {
        List<String> orderIds = parseIds(ids);
        if (orderIds.isEmpty()) {
            throw BusinessException.badRequest("请选择要删除的订单");
        }
        if (jdbc.update("DELETE FROM zl_order WHERE order_id IN (:orderIds)",
                Map.of("orderIds", orderIds)) == 0) {
            throw BusinessException.notFound("订单不存在");
        }
    }

    private RentalOrderDtos.RentalOrderForm loadForm(String orderId) {
        List<RentalOrderDtos.RentalOrderForm> rows = jdbc.query(
                SELECT_SQL + " WHERE z.order_id = :orderId", Map.of("orderId", orderId),
                RecordRowMapper.of(RentalOrderDtos.RentalOrderForm.class));
        if (rows.isEmpty()) {
            throw BusinessException.notFound("订单不存在");
        }
        return rows.getFirst();
    }

    private boolean exists(String orderId) {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM zl_order WHERE order_id = :orderId",
                Map.of("orderId", orderId), Long.class);
        return count != null && count > 0;
    }

    private void applyParams(MapSqlParameterSource params, RentalOrderDtos.RentalOrderForm form) {
        params.addValue("tenant", trimToNull(form.tenant()));
        params.addValue("channelType", trimToNull(form.channelType()));
        params.addValue("creSn", trimToNull(form.creSn()));
        params.addValue("status", trimToNull(form.status()));
        params.addValue("productId", form.productId());
        params.addValue("addressType", trimToNull(form.addressType()));
        // JSON 列显式声明 VARCHAR，保证 null 值也能正确写入
        params.addValue("addressJson", trimToNull(form.addressJson()), Types.VARCHAR);
        params.addValue("skuJson", trimToNull(form.skuJson()), Types.VARCHAR);
        params.addValue("productContractType", trimToNull(form.productContractType()));
        params.addValue("borrowDays", form.borrowDays());
        params.addValue("tenor", form.tenor());
        params.addValue("number", form.number());
        params.addValue("payType", trimToNull(form.payType()));
        params.addValue("amount", form.amount());
        params.addValue("monthAmount", form.monthAmount());
        params.addValue("deposit", form.deposit());
        params.addValue("buyOutPrice", form.buyOutPrice());
        params.addValue("updateTime", trimToNull(form.updateTime()));
        params.addValue("createUser", trimToNull(form.createUser()));
        params.addValue("createTime", trimToNull(form.createTime()));
        params.addValue("settleType", trimToNull(form.settleType()));
        params.addValue("merchantId", form.merchantId());
        params.addValue("merchantAmt", form.merchantAmt());
        params.addValue("merchantAfterAmt", form.merchantAfterAmt());
        params.addValue("salesId", form.salesId());
        params.addValue("payTenor", form.payTenor());
        params.addValue("payFirstAmount", form.payFirstAmount());
        params.addValue("buyOutFlag", trimToNull(form.buyOutFlag()));
        params.addValue("brokenFee", form.brokenFee());
        params.addValue("modifyFee", form.modifyFee());
        params.addValue("lockFee", form.lockFee());
        params.addValue("remark", trimToNull(form.remark()));
        params.addValue("takeDate", trimToNull(form.takeDate()));
        params.addValue("addressDeliver", trimToNull(form.addressDeliver()));
        params.addValue("color", trimToNull(form.color()));
        params.addValue("refuseCode", trimToNull(form.refuseCode()));
        params.addValue("accountDate", trimToNull(form.accountDate()));
        params.addValue("approveUser", trimToNull(form.approveUser()));
        params.addValue("approveTime", trimToNull(form.approveTime()));
        params.addValue("finishTime", trimToNull(form.finishTime()));
        params.addValue("isNotarial", trimToNull(form.isNotarial()));
        params.addValue("notarialResult", trimToNull(form.notarialResult()));
        params.addValue("passCode", trimToNull(form.passCode()));
        params.addValue("version", form.version());
        params.addValue("backDate", trimToNull(form.backDate()));
        params.addValue("backUser", trimToNull(form.backUser()));
        params.addValue("officialPrice", form.officialPrice());
        params.addValue("keepEntry", trimToNull(form.keepEntry()));
        params.addValue("commonId", form.commonId());
        params.addValue("memberFee", form.memberFee());
        params.addValue("claimUser", trimToNull(form.claimUser()));
        params.addValue("claimTime", trimToNull(form.claimTime()));
        params.addValue("channel", trimToNull(form.channel()));
        params.addValue("merchantNo", trimToNull(form.merchantNo()));
        params.addValue("jobType", trimToNull(form.jobType()));
        params.addValue("jobDetail", trimToNull(form.jobDetail()));
        params.addValue("moreFirstAmount", form.moreFirstAmount());
        params.addValue("returnStatus", trimToNull(form.returnStatus()));
        params.addValue("usePhone", trimToNull(form.usePhone()));
        params.addValue("guarantee", trimToNull(form.guarantee()));
        params.addValue("passPoint", trimToNull(form.passPoint()));
        params.addValue("riskPoint", trimToNull(form.riskPoint()));
        params.addValue("reportMsg", trimToNull(form.reportMsg()));
        params.addValue("subsidyAmount", form.subsidyAmount());
        params.addValue("firstPayType", trimToNull(form.firstPayType()));
        params.addValue("sensitiveMsg", trimToNull(form.sensitiveMsg()));
        params.addValue("firstPaySource", trimToNull(form.firstPaySource()));
        params.addValue("followUser", trimToNull(form.followUser()));
        params.addValue("followTime", trimToNull(form.followTime()));
    }

    /**
     * 枚举类字段按数据库注释约束取值；JSON 字段保证能写入 json 列。
     */
    private void validate(RentalOrderDtos.RentalOrderForm form) {
        requireAllowed("收货方式", form.addressType(), "0", "1");
        requireAllowed("合同类型", form.productContractType(), "1", "2");
        requireAllowed("支付方式", form.payType(), "1", "2", "4");
        requireAllowed("商户结算方式", form.settleType(), "1", "2");
        requireAllowed("是否公证", form.isNotarial(), "0", "1");
        requireAllowed("公证结果", form.notarialResult(), "-1", "0", "2", "3", "4");
        validateJson("地址信息", form.addressJson());
        validateJson("产品SKU", form.skuJson());
    }

    private void requireAllowed(String label, String value, String... allowed) {
        if (value == null || value.isBlank()) {
            return;
        }
        for (String item : allowed) {
            if (item.equals(value.trim())) {
                return;
            }
        }
        throw BusinessException.badRequest(label + "取值不合法");
    }

    private void validateJson(String label, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        try {
            jsonMapper.readTree(value);
        } catch (Exception ex) {
            throw BusinessException.badRequest(label + "不是合法的 JSON");
        }
    }

    private String normalizeOrderId(String orderId) {
        if (orderId == null || orderId.isBlank()) {
            return "RO" + LocalDateTime.now().format(ORDER_ID_TIME)
                    + ThreadLocalRandom.current().nextInt(1000, 10000);
        }
        String trimmed = orderId.trim();
        if (trimmed.length() > 64) {
            throw BusinessException.badRequest("订单号长度不能超过 64 个字符");
        }
        return trimmed;
    }

    private List<String> parseIds(String ids) {
        List<String> result = new ArrayList<>();
        if (ids == null || ids.isBlank()) {
            return result;
        }
        for (String part : ids.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String textOrDefault(String value, String fallback) {
        String trimmed = trimToNull(value);
        return trimmed == null ? fallback : trimmed;
    }

    private static String nowText() {
        return LocalDateTime.now().format(DATE_TIME);
    }
}
