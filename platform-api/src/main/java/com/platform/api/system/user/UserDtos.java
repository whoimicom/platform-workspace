package com.platform.api.system.user;

import com.platform.api.common.query.Op;
import com.platform.api.common.query.PageQuery;
import com.platform.api.common.query.QueryField;
import com.platform.api.common.query.QuerySort;
import com.platform.api.common.query.QuerySortMapping;

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
    @QuerySort(value = {
            @QuerySortMapping(property = "id", column = "u.id"),
            @QuerySortMapping(property = "username", column = "u.username"),
            @QuerySortMapping(property = "createTime", column = "u.create_time"),
            @QuerySortMapping(property = "updateTime", column = "u.update_time")
    }, defaultSort = "u.create_time DESC, u.id DESC")
    public static class UserQuery extends PageQuery {

        @QueryField(value = "u.status")
        private Integer status;

        @QueryField(value = "u.dept_id")
        private String deptId;

        /** 由 createTime 区间拆分，GE u.create_time */
        @QueryField(value = "u.create_time", op = Op.GE)
        private String createTimeStart;

        /** 由 createTime 区间拆分，LE u.create_time */
        @QueryField(value = "u.create_time", op = Op.LE)
        private String createTimeEnd;

        private String keywords;
        private List<String> roleIds = new ArrayList<>();
        private List<String> createTime = new ArrayList<>();

        public String getKeywords() {
            return keywords;
        }

        public void setKeywords(String keywords) {
            this.keywords = keywords;
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

        /**
         * 前端提交日期区间 [start, end]，拆分为含时分秒的 BETWEEN 边界。
         */
        public void setCreateTime(List<String> createTime) {
            this.createTime = createTime;
            if (createTime != null && !createTime.isEmpty()) {
                String start = normalizeDate(createTime.get(0));
                if (start != null) {
                    this.createTimeStart = start + " 00:00:00";
                }
                if (createTime.size() >= 2) {
                    String end = normalizeDate(createTime.get(1));
                    if (end != null) {
                        this.createTimeEnd = end + " 23:59:59";
                    }
                }
            }
        }

        private String normalizeDate(String value) {
            if (value == null || value.isBlank()) {
                return null;
            }
            String trimmed = value.trim();
            return trimmed.length() >= 10 ? trimmed.substring(0, 10) : null;
        }
    }
}
