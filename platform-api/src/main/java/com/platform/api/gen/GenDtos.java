package com.platform.api.gen;

import com.platform.api.common.query.PageQuery;

import java.util.List;

/**
 * 代码生成模块的数据结构。
 */
public final class GenDtos {

    private GenDtos() {
    }

    /** 数据库表列表行 */
    public record DbTableItem(String tableName, Long tableRows, String tableComment, Boolean configured) {
    }

    /** 已保存的生成配置（不含字段明细） */
    public record GenConfigItem(Long id, String tableName, String tableComment, String moduleName,
                                String businessName, String functionName, String className,
                                String frontendPath, String permPrefix, String genStyle,
                                String packageName, String author, String createTime, String updateTime) {
    }

    /** 字段配置 */
    public record GenColumnItem(Long id, String columnName, String columnType, String columnComment,
                                String propertyName, String javaType, Boolean isPk, Boolean isRequired,
                                Boolean isList, Boolean isFilter, String filterType) {
    }

    /** 配置详情：表配置 + 字段配置 */
    public record GenConfigDetail(GenConfigItem table, List<GenColumnItem> columns) {
    }

    /** 保存配置的请求体（表名不可改） */
    public record GenConfigForm(String functionName, String moduleName, String businessName,
                                String className, String frontendPath, String permPrefix,
                                String genStyle, String packageName, String author,
                                List<GenColumnItem> columns) {
    }

    /** 生成的文件 */
    public record GenFileItem(String path, String content) {
    }

    /** 列表查询参数 */
    public static class GenQuery extends PageQuery {
        private String keywords;

        public String getKeywords() {
            return keywords;
        }

        public void setKeywords(String keywords) {
            this.keywords = keywords;
        }
    }
}
