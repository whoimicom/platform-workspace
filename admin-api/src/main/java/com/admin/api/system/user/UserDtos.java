package com.admin.api.system.user;

import java.util.ArrayList;
import java.util.List;

/**
 * 与前端 src/api/system/user/types.ts 对应的数据结构。
 */
public final class UserDtos {

    private UserDtos() {
    }

    public record UserItem(String id, String avatar, String createTime, String deptName,
                           String email, Integer gender, String mobile, String nickname,
                           String roleNames, Integer status, String username) {
    }

    public record UserInfo(String userId, String username, String nickname, String avatar,
                           Integer gender, String deptName, List<String> roles, List<String> roleNames,
                           List<String> perms, Boolean canSwitchTenant) {
    }

    public record UserForm(String id, String avatar, String deptId, String email, Integer gender,
                           String mobile, String nickname, List<String> roleIds, Integer status,
                           String username) {
    }

    public record PasswordResetRequest(String password) {
    }

    /**
     * 用户分页查询参数（查询串绑定使用）。
     */
    public static class UserQuery {

        private Integer pageNum = 1;
        private Integer pageSize = 10;
        private String keywords;
        private Integer status;
        private String deptId;
        private List<String> roleIds = new ArrayList<>();
        private List<String> createTime = new ArrayList<>();
        private List<String> sort = new ArrayList<>();

        public Integer getPageNum() {
            return pageNum;
        }

        public void setPageNum(Integer pageNum) {
            this.pageNum = pageNum;
        }

        public Integer getPageSize() {
            return pageSize;
        }

        public void setPageSize(Integer pageSize) {
            this.pageSize = pageSize;
        }

        public String getKeywords() {
            return keywords;
        }

        public void setKeywords(String keywords) {
            this.keywords = keywords;
        }

        public Integer getStatus() {
            return status;
        }

        public void setStatus(Integer status) {
            this.status = status;
        }

        public String getDeptId() {
            return deptId;
        }

        public void setDeptId(String deptId) {
            this.deptId = deptId;
        }

        public List<String> getRoleIds() {
            return roleIds;
        }

        public void setRoleIds(List<String> roleIds) {
            this.roleIds = roleIds;
        }

        public List<String> getCreateTime() {
            return createTime;
        }

        public void setCreateTime(List<String> createTime) {
            this.createTime = createTime;
        }

        public List<String> getSort() {
            return sort;
        }

        public void setSort(List<String> sort) {
            this.sort = sort;
        }
    }
}
