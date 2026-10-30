package com.admin.api.system.dict;

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

    public static class DictTypeQuery {

        private Integer pageNum = 1;
        private Integer pageSize = 10;
        private String keywords;
        private Integer status;

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
    }

    public static class DictItemQuery {

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
}
