package com.platform.api.rental.supervise;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 设备锁实体：Spring Data JDBC 按命名策略自动把 camelCase 属性映射到 snake_case 列。
 */
@Table("zl_supervise_lock")
public class SuperviseLockEntity {

    @Id
    private Long id;
    private String orderId;
    private Long superviseId;
    private String superviseName;
    private String serialNumber;
    /** 是否上锁 0-正常 1-已锁 */
    private String status;
    /** 是否支持接口 0-否 1-是 */
    private String isInterface;
    private Integer normalWallpaper;
    private String activationLockStatus;
    /** 是否处于监管 */
    private Integer supervise;
    private Integer onlineStatus;
    private String abmStatus;
    private String remark;
    private String updateUser;
    private String createUser;
    private LocalDateTime updateTime;
    private LocalDateTime createTime;
    private String sysType;
    private String brandId;
    private String serialCode;
    private String model;
    private String superviseCode;
    private String serialCode2;
    private String tenant;
    /** APP 限制 0-否 1-是 */
    private Integer restriction;
    private LocalDateTime lastActiveTime;
    private LocalDate dealDate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getSuperviseName() {
        return superviseName;
    }

    public void setSuperviseName(String superviseName) {
        this.superviseName = superviseName;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
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

    public Integer getNormalWallpaper() {
        return normalWallpaper;
    }

    public void setNormalWallpaper(Integer normalWallpaper) {
        this.normalWallpaper = normalWallpaper;
    }

    public String getActivationLockStatus() {
        return activationLockStatus;
    }

    public void setActivationLockStatus(String activationLockStatus) {
        this.activationLockStatus = activationLockStatus;
    }

    public Integer getSupervise() {
        return supervise;
    }

    public void setSupervise(Integer supervise) {
        this.supervise = supervise;
    }

    public Integer getOnlineStatus() {
        return onlineStatus;
    }

    public void setOnlineStatus(Integer onlineStatus) {
        this.onlineStatus = onlineStatus;
    }

    public String getAbmStatus() {
        return abmStatus;
    }

    public void setAbmStatus(String abmStatus) {
        this.abmStatus = abmStatus;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getUpdateUser() {
        return updateUser;
    }

    public void setUpdateUser(String updateUser) {
        this.updateUser = updateUser;
    }

    public String getCreateUser() {
        return createUser;
    }

    public void setCreateUser(String createUser) {
        this.createUser = createUser;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public String getSysType() {
        return sysType;
    }

    public void setSysType(String sysType) {
        this.sysType = sysType;
    }

    public String getBrandId() {
        return brandId;
    }

    public void setBrandId(String brandId) {
        this.brandId = brandId;
    }

    public String getSerialCode() {
        return serialCode;
    }

    public void setSerialCode(String serialCode) {
        this.serialCode = serialCode;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getSuperviseCode() {
        return superviseCode;
    }

    public void setSuperviseCode(String superviseCode) {
        this.superviseCode = superviseCode;
    }

    public String getSerialCode2() {
        return serialCode2;
    }

    public void setSerialCode2(String serialCode2) {
        this.serialCode2 = serialCode2;
    }

    public String getTenant() {
        return tenant;
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

    public Integer getRestriction() {
        return restriction;
    }

    public void setRestriction(Integer restriction) {
        this.restriction = restriction;
    }

    public LocalDateTime getLastActiveTime() {
        return lastActiveTime;
    }

    public void setLastActiveTime(LocalDateTime lastActiveTime) {
        this.lastActiveTime = lastActiveTime;
    }

    public LocalDate getDealDate() {
        return dealDate;
    }

    public void setDealDate(LocalDate dealDate) {
        this.dealDate = dealDate;
    }
}
