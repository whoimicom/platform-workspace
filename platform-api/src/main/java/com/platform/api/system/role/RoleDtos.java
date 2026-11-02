package com.platform.api.system.role;

import com.platform.api.common.query.PageQuery;
import com.platform.api.common.query.QuerySort;
import com.platform.api.common.query.QuerySortMapping;

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
                           Integer dataScope, List<String> orgIds, String remark) {
    }

    @QuerySort(value = {
            @QuerySortMapping(property = "code", column = "r.code"),
            @QuerySortMapping(property = "name", column = "r.name"),
            @QuerySortMapping(property = "sort", column = "r.sort"),
            @QuerySortMapping(property = "updateTime", column = "r.update_time")
    }, defaultSort = "r.sort ASC, r.id ASC")
    public static class RoleQuery extends PageQuery {

        private String keywords;

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
