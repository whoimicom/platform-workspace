package com.platform.api.system.dict;

import com.platform.api.common.query.Op;
import com.platform.api.common.query.PageQuery;
import com.platform.api.common.query.QueryField;
import com.platform.api.common.query.QuerySort;
import com.platform.api.common.query.QuerySortMapping;

/**
 * 与前端 src/api/system/dict/types.ts 对应的数据结构。
 */
public final class DictDtos {

    private DictDtos() {
    }

    public record DictTypeItem(String id, String name, String dictCode, Integer status) {
    }

    public record DictTypeForm(String id, String name, String dictCode, Integer status, String remark) {
    }

    public record DictItem(String id, String dictCode, String label, String value, Integer status,
                           Integer sort, String tagType) {
    }

    public record DictItemForm(String id, String dictCode, String label, String value, Integer status,
                               Integer sort, String tagType) {
    }

    public record DictItemOption(String value, String label, String tagType) {
    }

    @QuerySort(value = {
            @QuerySortMapping(property = "name", column = "d.name"),
            @QuerySortMapping(property = "dictCode", column = "d.dict_code")
    }, defaultSort = "d.id ASC")
    public static class DictTypeQuery extends PageQuery {

        private String keywords;

        @QueryField(value = "d.status")
        private Integer status;

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
    }

    @QuerySort(value = {
            @QuerySortMapping(property = "label", column = "label"),
            @QuerySortMapping(property = "sort", column = "sort")
    }, defaultSort = "sort ASC, id ASC")
    public static class DictItemQuery extends PageQuery {

        private String keywords;

        public String getKeywords() {
            return keywords;
        }

        public void setKeywords(String keywords) {
            this.keywords = keywords;
        }
    }
}
