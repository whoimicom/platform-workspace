package com.admin.api.system.role;

import java.util.ArrayList;
import java.util.List;

/**
 * 与前端 src/api/system/role/types.ts 对应的数据结构。
 */
public final class RoleDtos {

    private RoleDtos() {
    }

    public record RoleItem(String id, String code, String name, Integer sort, Integer status,
                           Integer dataScope, String dataScopeLabel, String updateTime) {
    }

    public record RoleForm(String id, String code, String name, Integer sort, Integer status,
                           Integer dataScope, List<String> deptIds, String remark) {
    }

    public static class RoleQuery {

        private Integer pageNum = 1;
        private Integer pageSize = 10;
        private String keywords;

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
    }

    static List<String> emptyIfNull(List<String> values) {
        return values == null ? new ArrayList<>() : values;
    }
}
