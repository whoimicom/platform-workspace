package com.platform.api.gen;

import com.platform.api.common.query.PageQuery;

import java.util.List;

/**
 * 代码生成模块的传输对象。
 */
public class GenDtos {

    /** 库内数据表 */
    public record DbTableItem(String tableName, Long tableRows, String tableComment, boolean configured) {
    }

    /** 数据表字段（用于关联列选择） */
    public record DbColumnItem(String columnName, String columnType, String columnComment,
                               String propertyName, String javaType, boolean isPk, boolean nullable) {
    }

    /** 生成的文件 */
    public record GenFileItem(String path, String content) {
    }

    /** 导入表：一次选择多张表，指定其中一张为主表 */
    public record GenImportForm(List<String> tableNames, String mainTable) {
    }

    /** 配置列表项 */
    public record GenConfigItem(long id, String configName, String mainTable, String tableList,
                                String functionName, String moduleName, String businessName, String className,
                                String frontendPath, String permPrefix, String genStyle, String packageName,
                                String author, String createTime, String updateTime) {
    }

    /** 关联表配置 */
    public record GenTableItem(long id, String tableName, String tableComment, String tableAlias,
                               String relationType, String mainColumn, String joinColumn, int sort) {
    }

    /** 字段配置 */
    public record GenColumnItem(long id, String tableName, String columnName, String columnType,
                                String columnComment, String displayName, String propertyName,
                                String javaType, boolean isPk, boolean isRequired, boolean isList,
                                boolean isFilter, String filterType, boolean isForm,
                                String formComponent, int sort) {
    }

    /** 配置详情 */
    public record GenConfigDetail(GenConfigItem config, List<GenTableItem> tables, List<GenColumnItem> columns) {
    }

    /** 保存配置的请求体 */
    public record GenConfigForm(String configName, String functionName, String moduleName, String businessName,
                                String className, String frontendPath, String permPrefix, String genStyle,
                                String packageName, String author,
                                List<GenTableItem> tables, List<GenColumnItem> columns) {
    }

    /** 分页查询参数 */
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
