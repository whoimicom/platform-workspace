package com.platform.api.rental.supervise;

import com.platform.api.common.query.PageQuery;

import java.time.LocalDateTime;

/**
 * 设备锁查询参数：只负责接收前端条件，条件到 {@code Criteria} 的转换在 Service 中完成。
 */
public class SuperviseLockQuery extends PageQuery {

    /** 订单号 / 序列号 / 串码 / 型号 关键字 */
    private String keywords;
    private String orderId;
    private Long superviseId;
    private String serialNumber;
    private String serialCode;
    /** 是否上锁 0-正常 1-已锁 */
    private String status;
    private String isInterface;
    private String tenant;
    private String brandId;
    private String sysType;
    /** 创建时间区间 [start, end] */
    private LocalDateTime[] createTime;

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public Long getSuperviseId() {
        return superviseId;
    }

    public void setSuperviseId(Long superviseId) {
        this.superviseId = superviseId;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getSerialCode() {
        return serialCode;
    }

    public void setSerialCode(String serialCode) {
        this.serialCode = serialCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getIsInterface() {
        return isInterface;
    }

    public void setIsInterface(String isInterface) {
        this.isInterface = isInterface;
    }

    public String getTenant() {
        return tenant;
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

    public String getBrandId() {
        return brandId;
    }

    public void setBrandId(String brandId) {
        this.brandId = brandId;
    }

    public String getSysType() {
        return sysType;
    }

    public void setSysType(String sysType) {
        this.sysType = sysType;
    }

    public LocalDateTime[] getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime[] createTime) {
        this.createTime = createTime;
    }
}
