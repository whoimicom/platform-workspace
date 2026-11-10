package com.platform.api.system.user;

import com.platform.api.common.query.Op;
import com.platform.api.common.query.PageQuery;
import com.platform.api.common.query.QueryField;
import com.platform.api.common.query.QuerySort;
import com.platform.api.common.query.QuerySortMapping;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 与前端 src/api/system/user/types.ts 对应的数据结构。
 */
public final class UserDtos {

    private UserDtos() {
    }

    public record UserItem(String id, String avatar, String createDate, String createTime,
                           String orgName, String email, Integer gender, String mobile,
                           String nickname, String roleNames, Integer status, String username) {
    }

    public record UserInfo(String userId, String username, String nickname, String avatar,
                           Integer gender, String orgName, List<String> roles, List<String> roleNames,
                           List<String> perms, Boolean canSwitchTenant) {
    }

    public record UserForm(String id, String avatar, String orgId, String email, Integer gender,
                           String mobile, String nickname, List<String> roleIds, Integer status,
                           String username) {
    }

    public record PasswordResetRequest(String password) {
    }

    /** 用户权限分配：角色定义的菜单集 + 用户追加的菜单集。 */
    public record UserPerms(List<String> roleMenuIds, List<String> extraMenuIds) {
    }

    /** 用户追加权限保存请求：menuIds 为追加部分（后端会再次剔除角色已含项）。 */
    public record UserPermsForm(List<String> menuIds) {
    }

    /**
     * 用户分页查询参数（查询串绑定使用）。
     */
    @QuerySort(value = {
            @QuerySortMapping(property = "id", column = "u.id"),
            @QuerySortMapping(property = "username", column = "u.username"),
            @QuerySortMapping(property = "createDate", column = "u.create_date"),
            @QuerySortMapping(property = "createTime", column = "u.create_time"),
            @QuerySortMapping(property = "updateDatetime", column = "u.update_datetime")
    }, defaultSort = "u.create_date DESC, u.create_time DESC, u.id DESC")
    public static class UserQuery extends PageQuery {

        @QueryField(value = "u.status")
        private Integer status;

        @QueryField(value = "u.org_id")
        private String orgId;

        private String keywords;
        private List<String> roleIds = new ArrayList<>();

        /** 创建日期区间 [start, end]；查询串重复参数直接绑定为日期数组，由框架生成 BETWEEN 条件 */
        @QueryField(value = "u.create_date", op = Op.BETWEEN)
        private LocalDate[] createDate = new LocalDate[0];

        public Integer getStatus() {
            return status;
        }

        public void setStatus(Integer status) {
            this.status = status;
        }

        public String getOrgId() {
            return orgId;
        }

        public void setOrgId(String orgId) {
            this.orgId = orgId;
        }

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

        public LocalDate[] getCreateDate() {
            return createDate;
        }

        public void setCreateDate(LocalDate[] createDate) {
            this.createDate = createDate;
        }
    }
}
