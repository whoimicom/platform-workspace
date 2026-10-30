package com.platform.api.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.helpers.MessageFormatter;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * 统一返回体。所有经 gateway 接入的端（web 管理端 / 公众号 / 支付宝小程序）共用此结构。
 *
 * <p>原始字段（保持不变）：
 * <ul>
 *     <li>success：是否成功（true/false），交易成功失败的唯一标准</li>
 *     <li>resultCode：用于区分失败类型的业务状态码，0 表示成功，非 0 表示各类错误</li>
 *     <li>resultMsg：失败描述 / 提示信息</li>
 *     <li>resultBody：业务内容返回体</li>
 * </ul>
 *
 * <p>扩展字段（新增，由 {@code ResultAdvice} 自动注入，无需手动设置）：
 * <ul>
 *     <li>traceId：链路 ID，由 RequestContext 注入，便于跨服务串联日志，前端可原样回传用于排障</li>
 *     <li>timestamp：服务端响应时间（毫秒）</li>
 * </ul>
 */
public class ResultInfo<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = -8949153510458566367L;

    /**
     * 作为交易成功失败的唯一标准
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean success;

    /**
     * 用于区分失败类型
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer resultCode;

    /**
     * 失败描述
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String resultMsg;

    /**
     * 业务内容返回体
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T resultBody;

    /**
     * 链路 ID（扩展字段，自动注入）
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String traceId;

    /**
     * 服务端响应时间，毫秒（扩展字段，自动注入）
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long timestamp;

    private void initSuccess() {
        this.success = true;
        this.resultCode = Constant.CODE_OK;
        this.resultMsg = Constant.MSG_OK;
    }

    private void initFail() {
        this.success = false;
        this.resultCode = Constant.CODE_FAIL;
        this.resultMsg = Constant.MSG_FAIL;
    }

    public ResultInfo() {
    }

    public ResultInfo(Boolean success) {
        if (Boolean.TRUE.equals(success)) {
            this.initSuccess();
        } else {
            this.initFail();
        }
    }

    public ResultInfo(ResultEnum resultEnum) {
        this.resultCode = resultEnum.getResultCode();
        this.resultMsg = resultEnum.getResultMsg();
        this.success = Objects.equals(this.resultCode, Constant.CODE_OK);
    }

    public ResultInfo(Integer resultCode, String resultMsg) {
        this.resultCode = resultCode;
        this.resultMsg = resultMsg;
        this.success = Objects.equals(resultCode, Constant.CODE_OK);
    }

    public ResultInfo(Integer resultCode, String resultMsg, T resultBody) {
        this.resultCode = resultCode;
        this.resultMsg = resultMsg;
        this.resultBody = resultBody;
        this.success = Objects.equals(resultCode, Constant.CODE_OK);
    }

    // ---------- 静态工厂 ----------

    public static <T> ResultInfo<T> ok() {
        ResultInfo<T> r = new ResultInfo<>();
        r.initSuccess();
        return r;
    }

    public static <T> ResultInfo<T> ok(T resultBody) {
        ResultInfo<T> r = new ResultInfo<>();
        r.initSuccess();
        r.resultBody = resultBody;
        return r;
    }

    public static <T> ResultInfo<T> ok(String resultMsg, T resultBody) {
        ResultInfo<T> r = ok(resultBody);
        r.resultMsg = resultMsg;
        return r;
    }

    public static <T> ResultInfo<T> fail() {
        ResultInfo<T> r = new ResultInfo<>();
        r.initFail();
        return r;
    }

    public static <T> ResultInfo<T> fail(String resultMsg) {
        ResultInfo<T> r = fail();
        r.resultMsg = resultMsg;
        return r;
    }

    public static <T> ResultInfo<T> fail(Integer resultCode, String resultMsg) {
        return new ResultInfo<>(resultCode, resultMsg);
    }

    public static <T> ResultInfo<T> fail(ResultEnum resultEnum) {
        return new ResultInfo<>(resultEnum);
    }

    public static <T> ResultInfo<T> fail(ResultEnum resultEnum, Object... args) {
        ResultInfo<T> r = new ResultInfo<>();
        r.resultCode = resultEnum.getResultCode();
        r.resultMsg = MessageFormatter.arrayFormat(resultEnum.getResultMsg(), args).getMessage();
        r.success = Objects.equals(r.resultCode, Constant.CODE_OK);
        return r;
    }

    // ---------- 链式赋值（便于手动构造） ----------

    public ResultInfo<T> success(Boolean success) {
        this.success = success;
        return this;
    }

    public ResultInfo<T> code(Integer resultCode) {
        this.resultCode = resultCode;
        this.success = Objects.equals(resultCode, Constant.CODE_OK);
        return this;
    }

    public ResultInfo<T> msg(String resultMsg) {
        this.resultMsg = resultMsg;
        return this;
    }

    public ResultInfo<T> data(T resultBody) {
        this.resultBody = resultBody;
        return this;
    }

    public ResultInfo<T> traceId(String traceId) {
        this.traceId = traceId;
        return this;
    }

    // ---------- getter / setter ----------

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public Integer getResultCode() {
        return resultCode;
    }

    public void setResultCode(Integer resultCode) {
        this.resultCode = resultCode;
    }

    public String getResultMsg() {
        return resultMsg;
    }

    public void setResultMsg(String resultMsg) {
        this.resultMsg = resultMsg;
    }

    public T getResultBody() {
        return resultBody;
    }

    public void setResultBody(T resultBody) {
        this.resultBody = resultBody;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public static class Constant {
        public final static Integer CODE_OK = 200;
        public final static Integer CODE_FAIL = 500;
        public final static String MSG_OK = "OK";
        public final static String MSG_FAIL = "FAIL";

    }

    public enum ResultEnum {

        RESULT_SUCCESS(Constant.CODE_OK, Constant.MSG_OK),
        RESULT_FAIL(Constant.CODE_FAIL, Constant.MSG_FAIL),
        PARAM_ERROR(400, "参数错误"),
        UNAUTHORIZED(401, "未登录或登录已过期"),
        FORBIDDEN(403, "无访问权限"),
        NOT_FOUND(404, "资源不存在"),
        SMS_TIMEOUT(452, "短信验证码超时,请重新提交");

        private final Integer resultCode;
        private final String resultMsg;
        private static final ResultEnum[] VALUES = values();

        ResultEnum(Integer resultCode, String resultMsg) {
            this.resultCode = resultCode;
            this.resultMsg = resultMsg;
        }

        public Integer getResultCode() {
            return resultCode;
        }

        public String getResultMsg() {
            return resultMsg;
        }

        public static ResultEnum valueOf(Integer resultCode) {
            ResultEnum resolve = resolve(resultCode);
            if (resolve == null) {
                throw new IllegalArgumentException("No matching constant for [" + resultCode + "]");
            }
            return resolve;
        }

        public static ResultEnum resolve(Integer resultCode) {
            for (ResultEnum status : VALUES) {
                if (Objects.equals(status.resultCode, resultCode)) {
                    return status;
                }
            }
            return null;
        }
    }
}
