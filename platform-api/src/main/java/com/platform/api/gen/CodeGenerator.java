package com.platform.api.gen;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 代码生成器：根据 gen_table / gen_table_column 配置输出后端、前端与菜单 SQL。
 *
 * <p>两种后端风格：
 * <ul>
 *     <li>{@code dynamic-sql}：查询对象 + @QueryField 注解 + DynamicQueryExecutor + RecordRowMapper</li>
 *     <li>{@code data-jdbc}：Entity + Query + Criteria + JdbcAggregateOperations</li>
 * </ul>
 * 生成的代码仅覆盖列表查询（单表），写操作留待后续优化。
 */
@Component
public class CodeGenerator {

    private static final Set<String> IMPORTS = Set.of("BigDecimal", "LocalDate", "LocalDateTime");

    private static final Set<String> FILTER_TYPES = Set.of(
            "EQ", "NE", "GT", "GE", "LT", "LE",
            "LIKE", "LIKE_LEFT", "LIKE_RIGHT",
            "IN", "NOT_IN", "BETWEEN");

    public List<GenDtos.GenFileItem> generate(GenDtos.GenConfigDetail detail) {
        Ctx ctx = new Ctx(detail);
        List<GenDtos.GenFileItem> files = new ArrayList<>();

        if (ctx.jdbc) {
            files.add(file(ctx, ctx.className + "Entity.java", entity(ctx)));
            files.add(file(ctx, ctx.className + "Query.java", jdbcQuery(ctx)));
            files.add(file(ctx, ctx.className + "Service.java", jdbcService(ctx)));
        } else {
            files.add(file(ctx, ctx.className + "Dtos.java", dynamicDtos(ctx)));
            files.add(file(ctx, ctx.className + "Service.java", dynamicService(ctx)));
        }
        files.add(file(ctx, ctx.className + "Controller.java", controller(ctx)));

        files.add(new GenDtos.GenFileItem(
                "platform-web/src/api/" + ctx.frontendPath + "/types.ts", frontendTypes(ctx)));
        files.add(new GenDtos.GenFileItem(
                "platform-web/src/api/" + ctx.frontendPath + "/index.ts", frontendApi(ctx)));
        files.add(new GenDtos.GenFileItem(
                "platform-web/src/views/" + ctx.frontendPath + "/index.vue", frontendView(ctx)));
        files.add(new GenDtos.GenFileItem(
                "platform-api/sql/gen_" + ctx.tableName + ".sql", menuSql(ctx)));
        return files;
    }

    // ------------------------------------------------------------------ 动态 SQL 风格

    private String dynamicDtos(Ctx ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(ctx.pkg).append(";\n\n")
                .append("import com.platform.api.common.query.Op;\n")
                .append("import com.platform.api.common.query.PageQuery;\n")
                .append("import com.platform.api.common.query.QueryField;\n")
                .append("import com.platform.api.common.query.QuerySort;\n")
                .append("import com.platform.api.common.query.QuerySortMapping;\n");
        appendTypeImports(sb, ctx.columns);
        sb.append("\n/**\n * ").append(ctx.commentSafe(ctx.functionName))
                .append("：查询模型（动态 SQL 风格）。\n */\n")
                .append("public class ").append(ctx.className).append("Dtos {\n\n");

        sb.append("    /** 列表行：字段与 ").append(ctx.tableName).append(" 一一对应 */\n")
                .append("    public record Item(").append(itemComponents(ctx)).append(") {\n    }\n\n");

        sb.append("    /** 分页查询参数 */\n")
                .append("    @QuerySort(value = {\n");
        for (GenDtos.GenColumnItem column : ctx.columns) {
            sb.append("        @QuerySortMapping(property = \"")
                    .append(column.propertyName()).append("\", column = \"")
                    .append(column.columnName()).append("\"),\n");
        }
        sb.append("    }, defaultSort = \"").append(ctx.pk.columnName()).append(" DESC\")\n")
                .append("    public static class Query extends PageQuery {\n");
        appendDynamicQueryFields(sb, ctx);
        sb.append("    }\n}\n");
        return sb.toString();
    }

    private void appendDynamicQueryFields(StringBuilder sb, Ctx ctx) {
        List<Field> fields = queryFields(ctx);
        for (Field field : fields) {
            sb.append("\n        /** ").append(field.comment()).append(" */\n")
                    .append("        @QueryField(value = \"").append(field.column())
                    .append("\", op = Op.").append(field.op()).append(")\n")
                    .append("        private String ").append(field.prop()).append(";\n");
        }
        for (Field field : fields) {
            String accessor = GenService.pascal(field.prop());
            sb.append("\n        public String get").append(accessor).append("() {\n")
                    .append("            return ").append(field.prop()).append(";\n        }\n\n")
                    .append("        public void set").append(accessor).append("(String ").append(field.prop())
                    .append(") {\n            this.").append(field.prop()).append(" = ").append(field.prop())
                    .append(";\n        }\n");
        }
    }

    private String dynamicService(Ctx ctx) {
        String selectColumns = selectColumns(ctx);
        return new StringBuilder()
                .append("package ").append(ctx.pkg).append(";\n\n")
                .append("import com.platform.api.common.query.DynamicQueryExecutor;\n")
                .append("import com.platform.api.common.query.RecordRowMapper;\n")
                .append("import org.springframework.data.domain.Page;\n")
                .append("import org.springframework.stereotype.Service;\n\n")
                .append("/**\n * ").append(ctx.commentSafe(ctx.functionName))
                .append("：查询服务（动态 SQL 风格）。\n */\n")
                .append("@Service\n")
                .append("public class ").append(ctx.className).append("Service {\n\n")
                .append("    private static final String SELECT_SQL = \"")
                .append("SELECT ").append(selectColumns)
                .append(" FROM ").append(ctx.tableName).append(" t\";\n\n")
                .append("    private static final String COUNT_SQL = \"SELECT COUNT(*) FROM ")
                .append(ctx.tableName).append("\";\n\n")
                .append("    private final DynamicQueryExecutor executor;\n\n")
                .append("    public ").append(ctx.className)
                .append("Service(DynamicQueryExecutor executor) {\n")
                .append("        this.executor = executor;\n    }\n\n")
                .append("    public Page<").append(ctx.className).append("Dtos.Item> page(")
                .append(ctx.className).append("Dtos.Query query) {\n")
                .append("        return executor.page(SELECT_SQL, COUNT_SQL, query,\n")
                .append("                RecordRowMapper.of(").append(ctx.className).append("Dtos.Item.class));\n")
                .append("    }\n}\n")
                .toString();
    }

    // ------------------------------------------------------------------ Data JDBC 风格

    private String entity(Ctx ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(ctx.pkg).append(";\n\n")
                .append("import org.springframework.data.annotation.Id;\n")
                .append("import org.springframework.data.relational.core.mapping.Table;\n");
        appendTypeImports(sb, ctx.columns);
        sb.append("\n/**\n * ").append(ctx.commentSafe(ctx.functionName)).append("实体（").append(ctx.tableName)
                .append("）。\n */\n")
                .append("@Table(\"").append(ctx.tableName).append("\")\n")
                .append("public class ").append(ctx.className).append("Entity {\n");
        for (GenDtos.GenColumnItem column : ctx.columns) {
            sb.append("\n    /** ").append(comment(column)).append(" */\n");
            if (Boolean.TRUE.equals(column.isPk())) {
                sb.append("    @Id\n");
            }
            sb.append("    private ").append(javaType(column)).append(" ")
                    .append(column.propertyName()).append(";\n");
        }
        for (GenDtos.GenColumnItem column : ctx.columns) {
            String prop = column.propertyName();
            String accessor = GenService.pascal(prop);
            String type = javaType(column);
            sb.append("\n    public ").append(type).append(" get").append(accessor).append("() {\n")
                    .append("        return ").append(prop).append(";\n    }\n\n")
                    .append("    public void set").append(accessor).append("(").append(type).append(" ")
                    .append(prop).append(") {\n        this.").append(prop).append(" = ").append(prop)
                    .append(";\n    }\n");
        }
        sb.append("}\n");
        return sb.toString();
    }

    private String jdbcQuery(Ctx ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(ctx.pkg).append(";\n\n")
                .append("import com.platform.api.common.query.PageQuery;\n\n")
                .append("/**\n * ").append(ctx.commentSafe(ctx.functionName))
                .append("：分页查询参数（Data JDBC 风格）。\n */\n")
                .append("public class ").append(ctx.className).append("Query extends PageQuery {\n");
        for (Field field : queryFields(ctx)) {
            sb.append("\n    /** ").append(field.comment()).append(" */\n")
                    .append("    private String ").append(field.prop()).append(";\n");
        }
        for (Field field : queryFields(ctx)) {
            String accessor = GenService.pascal(field.prop());
            sb.append("\n    public String get").append(accessor).append("() {\n")
                    .append("        return ").append(field.prop()).append(";\n    }\n\n")
                    .append("    public void set").append(accessor).append("(String ").append(field.prop())
                    .append(") {\n        this.").append(field.prop()).append(" = ").append(field.prop())
                    .append(";\n    }\n");
        }
        sb.append("}\n");
        return sb.toString();
    }

    private String jdbcService(Ctx ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(ctx.pkg).append(";\n\n")
                .append("import org.springframework.data.domain.Page;\n")
                .append("import org.springframework.data.domain.PageImpl;\n")
                .append("import org.springframework.data.domain.PageRequest;\n")
                .append("import org.springframework.data.domain.Pageable;\n")
                .append("import org.springframework.data.domain.Sort;\n")
                .append("import org.springframework.data.jdbc.core.JdbcAggregateOperations;\n")
                .append("import org.springframework.data.relational.core.query.Criteria;\n")
                .append("import org.springframework.data.relational.core.query.Query;\n")
                .append("import org.springframework.stereotype.Service;\n")
                .append("import org.springframework.util.StringUtils;\n\n")
                .append("import java.util.ArrayList;\n")
                .append("import java.util.List;\n")
                .append("import java.util.Set;\n\n")
                .append("/**\n * ").append(ctx.commentSafe(ctx.functionName))
                .append("：分页查询服务（Data JDBC 原生动态查询）。\n */\n")
                .append("@Service\n")
                .append("public class ").append(ctx.className).append("Service {\n\n");

        sb.append("    /** 允许排序的属性白名单 */\n")
                .append("    private static final Set<String> SORTABLE = Set.of(");
        Set<String> sortable = new LinkedHashSet<>();
        for (GenDtos.GenColumnItem column : ctx.columns) {
            sortable.add(column.propertyName());
        }
        sb.append(String.join(", ", sortable.stream().map(p -> "\"" + p + "\"").toList()))
                .append(");\n\n")
                .append("    private final JdbcAggregateOperations operations;\n\n")
                .append("    public ").append(ctx.className).append("Service(JdbcAggregateOperations operations) {\n")
                .append("        this.operations = operations;\n    }\n\n");

        sb.append("    public Page<").append(ctx.className).append("Entity> page(")
                .append(ctx.className).append("Query query) {\n")
                .append("        Criteria criteria = Criteria.empty();\n");
        appendJdbcConditions(sb, ctx);
        sb.append("\n        Sort sort = resolveSort(query.getSort());\n")
                .append("        Pageable pageable = PageRequest.of(query.safePageNum() - 1, query.safePageSize(), sort);\n")
                .append("        Query jdbcQuery = Query.query(criteria).with(pageable);\n")
                .append("        List<").append(ctx.className).append("Entity> content = operations.findAll(jdbcQuery,")
                .append(" ").append(ctx.className).append("Entity.class);\n")
                .append("        long total = operations.count(Query.query(criteria), ")
                .append(ctx.className).append("Entity.class);\n")
                .append("        return new PageImpl<>(content, pageable, total);\n    }\n\n");

        sb.append("    /** 逗号分隔的查询值（用于 IN / NOT_IN / BETWEEN） */\n")
                .append("    private static List<String> splitValues(String text) {\n")
                .append("        List<String> values = new ArrayList<>();\n")
                .append("        for (String item : text.split(\",\")) {\n")
                .append("            if (StringUtils.hasText(item)) {\n")
                .append("                values.add(item.trim());\n")
                .append("            }\n")
                .append("        }\n")
                .append("        return values;\n")
                .append("    }\n\n");

        sb.append("    /**\n")
                .append("     * 解析 sort 参数：元素格式 \"property\" 或 \"property,asc|desc\"，属性名必须命中白名单。\n")
                .append("     */\n")
                .append("    private static Sort resolveSort(List<String> sortParams) {\n")
                .append("        List<Sort.Order> orders = new ArrayList<>();\n")
                .append("        if (sortParams != null) {\n")
                .append("            for (String item : sortParams) {\n")
                .append("                if (!StringUtils.hasText(item)) {\n")
                .append("                    continue;\n")
                .append("                }\n")
                .append("                String[] parts = item.split(\",\");\n")
                .append("                String property = parts[0].trim();\n")
                .append("                if (!SORTABLE.contains(property)) {\n")
                .append("                    continue;\n")
                .append("                }\n")
                .append("                boolean asc = parts.length < 2 || \"asc\".equalsIgnoreCase(parts[1].trim());\n")
                .append("                orders.add(asc ? Sort.Order.asc(property) : Sort.Order.desc(property));\n")
                .append("            }\n")
                .append("        }\n")
                .append("        return orders.isEmpty() ? Sort.by(Sort.Order.desc(\"").append(ctx.pk.propertyName())
                .append("\")) : Sort.by(orders);\n")
                .append("    }\n}\n");
        return sb.toString();
    }

    private void appendJdbcConditions(StringBuilder sb, Ctx ctx) {
        for (Field field : queryFields(ctx)) {
            String getter = getter(field.prop());
            String value = "query." + getter + "().trim()";
            sb.append("\n        if (StringUtils.hasText(query.").append(getter).append("())) {\n");
            if ("IN".equals(field.op()) || "NOT_IN".equals(field.op())) {
                sb.append("            List<String> values = splitValues(query.").append(getter).append("());\n")
                        .append("            if (!values.isEmpty()) {\n")
                        .append("                criteria = criteria.and(\"").append(field.column()).append("\").")
                        .append(field.op().equals("IN") ? "in" : "notIn").append("(values);\n")
                        .append("            }\n");
            } else if ("BETWEEN".equals(field.op())) {
                sb.append("            List<String> values = splitValues(query.").append(getter).append("());\n")
                        .append("            if (values.size() >= 2) {\n")
                        .append("                criteria = criteria.and(\"").append(field.column())
                        .append("\").between(values.get(0), values.get(1));\n")
                        .append("            }\n");
            } else {
                sb.append("            criteria = criteria.and(\"").append(field.column()).append("\").")
                        .append(jdbcOperator(field.op())).append("(")
                        .append(jdbcValue(field.op(), value)).append(");\n");
            }
            sb.append("        }\n");
        }
    }

    private static String jdbcOperator(String op) {
        return switch (op) {
            case "NE" -> "not";
            case "GT" -> "greaterThan";
            case "GE" -> "greaterThanOrEquals";
            case "LT" -> "lessThan";
            case "LE" -> "lessThanOrEquals";
            case "LIKE", "LIKE_LEFT", "LIKE_RIGHT" -> "like";
            default -> "is";
        };
    }

    private static String jdbcValue(String op, String value) {
        return switch (op) {
            case "LIKE" -> "\"%\" + " + value + " + \"%\"";
            case "LIKE_LEFT" -> "\"%\" + " + value;
            case "LIKE_RIGHT" -> value + " + \"%\"";
            default -> value;
        };
    }

    // ------------------------------------------------------------------ Controller

    private String controller(Ctx ctx) {
        String pageType = ctx.jdbc
                ? "Page<" + ctx.className + "Entity>"
                : "Page<" + ctx.className + "Dtos.Item>";
        String queryType = ctx.jdbc
                ? ctx.className + "Query"
                : ctx.className + "Dtos.Query";
        String field = GenService.camel(ctx.className) + "Service";
        return new StringBuilder()
                .append("package ").append(ctx.pkg).append(";\n\n")
                .append("import com.platform.api.common.ResultInfo;\n")
                .append("import org.springframework.data.domain.Page;\n")
                .append("import org.springframework.security.access.prepost.PreAuthorize;\n")
                .append("import org.springframework.web.bind.annotation.GetMapping;\n")
                .append("import org.springframework.web.bind.annotation.RequestMapping;\n")
                .append("import org.springframework.web.bind.annotation.RestController;\n\n")
                .append("/**\n * ").append(ctx.commentSafe(ctx.functionName)).append("查询。\n */\n")
                .append("@RestController\n")
                .append("@RequestMapping(\"").append(ctx.urlPath).append("\")\n")
                .append("public class ").append(ctx.className).append("Controller {\n\n")
                .append("    private final ").append(ctx.className).append("Service ").append(field).append(";\n\n")
                .append("    public ").append(ctx.className).append("Controller(")
                .append(ctx.className).append("Service ").append(field).append(") {\n")
                .append("        this.").append(field).append(" = ").append(field).append(";\n    }\n\n")
                .append("    @GetMapping\n")
                .append("    @PreAuthorize(\"@perm.has('").append(ctx.perm).append("')\")\n")
                .append("    public ResultInfo<").append(pageType).append("> page(")
                .append(queryType).append(" query) {\n")
                .append("        return ResultInfo.ok(").append(field).append(".page(query));\n")
                .append("    }\n}\n")
                .toString();
    }

    // ------------------------------------------------------------------ 前端

    private String frontendTypes(Ctx ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("import type { PageQuery } from \"@/api/common\";\n\n")
                .append("/**\n * ").append(ctx.commentSafe(ctx.functionName)).append("分页查询参数\n */\n")
                .append("export interface ").append(ctx.className).append("Query extends PageQuery {\n");
        for (Field field : queryFields(ctx)) {
            sb.append("  /** ").append(field.comment()).append(" */\n")
                    .append("  ").append(field.prop()).append("?: string;\n");
            if ("BETWEEN".equals(field.op())) {
                sb.append("  ").append(field.prop()).append("Start?: string;\n")
                        .append("  ").append(field.prop()).append("End?: string;\n");
            }
        }
        sb.append("}\n\n")
                .append("/**\n * ").append(ctx.commentSafe(ctx.functionName)).append("列表行（字段与 ")
                .append(ctx.tableName).append(" 对应）\n */\n")
                .append("export interface ").append(ctx.className).append("Item {\n");
        for (GenDtos.GenColumnItem column : ctx.columns) {
            sb.append("  /** ").append(comment(column)).append(" */\n")
                    .append("  ").append(column.propertyName()).append("?: ")
                    .append(tsType(column.javaType())).append(";\n");
        }
        sb.append("}\n");
        return sb.toString();
    }

    private String frontendApi(Ctx ctx) {
        return new StringBuilder()
                .append("import request from \"@/utils/request\";\n")
                .append("import type { Pagination } from \"@/api/common\";\n")
                .append("import type { ").append(ctx.className).append("Item, ")
                .append(ctx.className).append("Query } from \"./types\";\n\n")
                .append("const ").append(ctx.className).append("API = {\n")
                .append("  /**\n     * 获取").append(ctx.commentSafe(ctx.functionName)).append("分页数据\n   */\n")
                .append("  getPage(queryParams?: ").append(ctx.className).append("Query) {\n")
                .append("    return request<unknown, Pagination<").append(ctx.className).append("Item>>({\n")
                .append("      url: \"").append(ctx.urlPath).append("\",\n")
                .append("      method: \"get\",\n")
                .append("      params: queryParams,\n")
                .append("    });\n  },\n};\n\n")
                .append("export default ").append(ctx.className).append("API;\n\n")
                .append("export * from \"./types\";\n")
                .toString();
    }

    private String frontendView(Ctx ctx) {
        List<GenDtos.GenColumnItem> listColumns = ctx.columns.stream()
                .filter(column -> Boolean.TRUE.equals(column.isList()))
                .toList();
        if (listColumns.isEmpty()) {
            listColumns = ctx.columns;
        }
        boolean hasDateColumn = listColumns.stream().anyMatch(CodeGenerator::isDateType);
        String apiName = ctx.className + "API";

        StringBuilder sb = new StringBuilder();
        sb.append("<template>\n")
                .append("  <div class=\"page-container\">\n")
                .append("    <el-card class=\"page-search\" shadow=\"never\">\n")
                .append("      <el-form ref=\"queryFormRef\" :model=\"params\" :inline=\"true\">\n")
                .append(searchItems(ctx))
                .append("        <el-form-item>\n")
                .append("          <el-button type=\"primary\" @click=\"handleQuery\">搜索</el-button>\n")
                .append("          <el-button @click=\"handleResetQuery\">重置</el-button>\n")
                .append("        </el-form-item>\n")
                .append("      </el-form>\n")
                .append("    </el-card>\n\n")
                .append("    <el-card class=\"page-content\" shadow=\"never\">\n")
                .append("      <div class=\"page-toolbar\">\n")
                .append("        <div class=\"page-toolbar__left\" />\n")
                .append("        <div class=\"page-toolbar__right\">\n")
                .append("          <el-tooltip content=\"刷新\" placement=\"top\">\n")
                .append("            <el-button class=\"page-icon-btn\" @click=\"handleQuery\">\n")
                .append("              <el-icon><Refresh /></el-icon>\n")
                .append("            </el-button>\n")
                .append("          </el-tooltip>\n")
                .append("        </div>\n")
                .append("      </div>\n\n")
                .append("      <div class=\"page-table-wrapper\">\n")
                .append("        <el-table\n")
                .append("          v-loading=\"loading\"\n")
                .append("          :data=\"list\"\n")
                .append("          class=\"page-table\"\n")
                .append("          height=\"100%\"\n")
                .append("          highlight-current-row\n")
                .append("          border\n")
                .append("        >\n")
                .append("          <el-table-column type=\"index\" label=\"序号\" width=\"60\" />\n")
                .append(tableColumns(listColumns))
                .append("        </el-table>\n")
                .append("      </div>\n\n")
                .append("      <pagination\n")
                .append("        v-if=\"total > 0\"\n")
                .append("        v-model:total=\"total\"\n")
                .append("        v-model:page=\"params.pageNum\"\n")
                .append("        v-model:limit=\"params.pageSize\"\n")
                .append("        @pagination=\"fetchData\"\n")
                .append("      />\n")
                .append("    </el-card>\n")
                .append("  </div>\n")
                .append("</template>\n\n")
                .append("<script setup lang=\"ts\">\n")
                .append("import { Refresh } from \"@element-plus/icons-vue\";\n")
                .append("import type { FormInstance } from \"element-plus\";\n\n")
                .append("import ").append(apiName).append(" from \"@/api/").append(ctx.frontendPath).append("\";\n")
                .append("import type { ").append(ctx.className).append("Item, ").append(ctx.className)
                .append("Query } from \"@/api/").append(ctx.frontendPath).append("\";\n")
                .append("import { usePageTable } from \"@/composables\";\n\n")
                .append("defineOptions({\n")
                .append("  name: \"").append(ctx.className).append("\",\n")
                .append("  inheritAttrs: false,\n")
                .append("});\n\n");
        if (hasDateColumn) {
            sb.append("const formatDateTime = (value?: string) => (value ? value.replace(\"T\", \" \") : \"\");\n\n");
        }
        List<Field> betweenFields = queryFields(ctx).stream()
                .filter(field -> "BETWEEN".equals(field.op())).toList();
        if (!betweenFields.isEmpty()) {
            sb.append("const buildQuery = (query: ").append(ctx.className).append("Query) => ({\n")
                    .append("  ...query,\n");
            for (Field field : betweenFields) {
                sb.append("  ").append(field.prop()).append(": [query.").append(field.prop()).append("Start, query.")
                        .append(field.prop()).append("End].filter(Boolean).join(\",\"),\n");
            }
            sb.append("});\n\n");
        }
        String request = betweenFields.isEmpty()
                ? apiName + ".getPage"
                : "(query: " + ctx.className + "Query) => " + apiName + ".getPage(buildQuery(query))";
        sb.append("const queryFormRef = ref<FormInstance>();\n\n")
                .append("const { loading, list, total, params, fetchData, handleQuery, handleResetQuery } = usePageTable<\n")
                .append("  ").append(ctx.className).append("Item,\n")
                .append("  ").append(ctx.className).append("Query\n")
                .append(">({\n")
                .append("  initialParams: {\n")
                .append("    pageNum: 1,\n")
                .append("    pageSize: 10,\n")
                .append(initialParams(ctx))
                .append("  },\n")
                .append("  request: ").append(request).append(",\n")
                .append("  onBeforeReset: () => queryFormRef.value?.resetFields(),\n")
                .append("});\n\n")
                .append("onMounted(() => {\n")
                .append("  handleQuery();\n")
                .append("});\n")
                .append("</script>\n");
        return sb.toString();
    }

    private String searchItems(Ctx ctx) {
        StringBuilder sb = new StringBuilder();
        for (GenDtos.GenColumnItem column : ctx.filters) {
            String prop = column.propertyName();
            String label = comment(column);
            String filterType = normalizeFilter(column.filterType());
            if ("BETWEEN".equals(filterType) && isDateType(column)) {
                String type = javaType(column).equals("LocalDate") ? "date" : "datetime";
                String format = javaType(column).equals("LocalDate") ? "YYYY-MM-DD" : "YYYY-MM-DD HH:mm:ss";
                sb.append("        <el-form-item label=\"").append(label).append("\" prop=\"").append(prop)
                        .append("Start\">\n")
                        .append("          <el-date-picker\n")
                        .append("            v-model=\"params.").append(prop).append("Start\"\n")
                        .append("            type=\"").append(type).append("\"\n")
                        .append("            value-format=\"").append(format).append("\"\n")
                        .append("            placeholder=\"开始时间\"\n")
                        .append("            style=\"width: 180px\"\n")
                        .append("          />\n")
                        .append("        </el-form-item>\n")
                        .append("        <el-form-item label=\"至\" prop=\"").append(prop).append("End\">\n")
                        .append("          <el-date-picker\n")
                        .append("            v-model=\"params.").append(prop).append("End\"\n")
                        .append("            type=\"").append(type).append("\"\n")
                        .append("            value-format=\"").append(format).append("\"\n")
                        .append("            placeholder=\"结束时间\"\n")
                        .append("            style=\"width: 180px\"\n")
                        .append("          />\n")
                        .append("        </el-form-item>\n");
                continue;
            }
            if ("BETWEEN".equals(filterType)) {
                sb.append("        <el-form-item label=\"").append(label).append("\" prop=\"").append(prop)
                        .append("Start\">\n")
                        .append("          <el-input v-model=\"params.").append(prop)
                        .append("Start\" placeholder=\"最小值\" clearable />\n")
                        .append("        </el-form-item>\n")
                        .append("        <el-form-item label=\"至\" prop=\"").append(prop).append("End\">\n")
                        .append("          <el-input v-model=\"params.").append(prop)
                        .append("End\" placeholder=\"最大值\" clearable />\n")
                        .append("        </el-form-item>\n");
                continue;
            }
            String placeholder = "请输入" + label;
            if ("IN".equals(filterType) || "NOT_IN".equals(filterType)) {
                placeholder = "多个值用英文逗号分隔";
            }
            sb.append("        <el-form-item label=\"").append(label).append("\" prop=\"").append(prop).append("\">\n")
                    .append("          <el-input\n")
                    .append("            v-model=\"params.").append(prop).append("\"\n")
                    .append("            placeholder=\"").append(placeholder).append("\"\n")
                    .append("            clearable\n")
                    .append("            @keyup.enter=\"handleQuery\"\n")
                    .append("          />\n")
                    .append("        </el-form-item>\n");
        }
        return sb.toString();
    }

    private String tableColumns(List<GenDtos.GenColumnItem> columns) {
        StringBuilder sb = new StringBuilder();
        for (GenDtos.GenColumnItem column : columns) {
            String prop = column.propertyName();
            String label = comment(column);
            if (isDateType(column)) {
                sb.append("          <el-table-column key=\"").append(prop).append("\" label=\"").append(label)
                        .append("\" min-width=\"160\">\n")
                        .append("            <template #default=\"scope\">{{ formatDateTime(scope.row.")
                        .append(prop).append(") }}</template>\n")
                        .append("          </el-table-column>\n");
            } else {
                sb.append("          <el-table-column key=\"").append(prop).append("\" label=\"").append(label)
                        .append("\" prop=\"").append(prop).append("\" min-width=\"120\" />\n");
            }
        }
        return sb.toString();
    }

    private String initialParams(Ctx ctx) {
        StringBuilder sb = new StringBuilder();
        for (Field field : queryFields(ctx)) {
            if ("BETWEEN".equals(field.op())) {
                sb.append("    ").append(field.prop()).append("Start: \"\",\n")
                        .append("    ").append(field.prop()).append("End: \"\",\n");
            } else {
                sb.append("    ").append(field.prop()).append(": \"\",\n");
            }
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ 菜单 SQL

    private String menuSql(Ctx ctx) {
        String menuPath = kebab(ctx.businessName);
        String routeName = GenService.pascal(ctx.businessName);
        String modulePath = "/" + ctx.moduleName;
        return new StringBuilder()
                .append("-- ").append(ctx.commentSafe(ctx.functionName))
                .append(" 菜单与权限（" ).append(ctx.tableName).append("）\n")
                .append("-- 执行后需刷新前端或重新登录，菜单才会出现\n")
                .append("USE platform;\n\n")
                .append("-- 一级目录：不存在时创建\n")
                .append("INSERT INTO sys_menu (parent_id, name, type, path, route_name, redirect, component, icon, sort, visible, scope, keep_alive, create_time, update_time)\n")
                .append("SELECT 0, '").append(ctx.moduleName).append("', 'C', '").append(modulePath)
                .append("', NULL, NULL, 'Layout', 'folder', 1, 1, 1, NULL, NOW(), NOW()\n")
                .append("  FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE type = 'C' AND path = '")
                .append(modulePath).append("');\n\n")
                .append("SET @parent_id := (SELECT id FROM sys_menu WHERE type = 'C' AND path = '")
                .append(modulePath).append("' LIMIT 1);\n\n")
                .append("-- 页面菜单\n")
                .append("INSERT INTO sys_menu (parent_id, name, type, path, route_name, redirect, component, icon, sort, visible, scope, keep_alive, create_time, update_time)\n")
                .append("VALUES (@parent_id, '").append(ctx.commentSafe(ctx.functionName)).append("', 'M', '")
                .append(menuPath).append("', '").append(routeName).append("', NULL, '")
                .append(ctx.frontendPath).append("/index', 'document', 1, 1, 1, 1, NOW(), NOW());\n")
                .append("SET @menu_id := LAST_INSERT_ID();\n\n")
                .append("-- 查询按钮权限\n")
                .append("INSERT INTO sys_menu (parent_id, name, type, path, route_name, redirect, component, icon, sort, visible, scope, keep_alive, perm, create_time, update_time)\n")
                .append("VALUES (@menu_id, '查询', 'B', NULL, NULL, NULL, NULL, NULL, 1, 1, 1, NULL, '")
                .append(ctx.perm).append("', NOW(), NOW());\n")
                .append("SET @perm_id := LAST_INSERT_ID();\n\n")
                .append("-- 默认授权给角色 1（超管）与 2\n")
                .append("INSERT IGNORE INTO sys_role_menu (role_id, menu_id)\n")
                .append("SELECT r.id, @menu_id FROM sys_role r WHERE r.id IN (1, 2);\n")
                .append("INSERT IGNORE INTO sys_role_menu (role_id, menu_id)\n")
                .append("SELECT r.id, @perm_id FROM sys_role r WHERE r.id IN (1, 2);\n")
                .toString();
    }

    // ------------------------------------------------------------------ 工具

    private GenDtos.GenFileItem file(Ctx ctx, String name, String content) {
        return new GenDtos.GenFileItem(
                "platform-api/src/main/java/" + ctx.pkg.replace('.', '/') + "/" + name, content);
    }

    private String itemComponents(Ctx ctx) {
        List<String> parts = new ArrayList<>();
        for (GenDtos.GenColumnItem column : ctx.columns) {
            parts.add(javaType(column) + " " + column.propertyName());
        }
        return String.join(", ", parts);
    }

    private String selectColumns(Ctx ctx) {
        List<String> parts = new ArrayList<>();
        for (GenDtos.GenColumnItem column : ctx.columns) {
            parts.add("t.`" + column.columnName() + "` AS `" + column.columnName() + "`");
        }
        return String.join(", ", parts);
    }

    private List<Field> queryFields(Ctx ctx) {
        List<Field> fields = new ArrayList<>();
        for (GenDtos.GenColumnItem column : ctx.filters) {
            String filterType = normalizeFilter(column.filterType());
            fields.add(new Field(column.propertyName(), column.columnName(), filterType, comment(column)));
        }
        return fields;
    }

    private static void appendTypeImports(StringBuilder sb, List<GenDtos.GenColumnItem> columns) {
        Set<String> types = new LinkedHashSet<>();
        for (GenDtos.GenColumnItem column : columns) {
            String type = javaType(column);
            if (IMPORTS.contains(type)) {
                types.add(type);
            }
        }
        if (types.contains("BigDecimal")) {
            sb.append("import java.math.BigDecimal;\n");
        }
        if (types.contains("LocalDate") || types.contains("LocalDateTime")) {
            if (types.contains("LocalDate")) {
                sb.append("import java.time.LocalDate;\n");
            }
            if (types.contains("LocalDateTime")) {
                sb.append("import java.time.LocalDateTime;\n");
            }
        }
    }

    private static String javaType(GenDtos.GenColumnItem column) {
        return StringUtils.hasText(column.javaType()) ? column.javaType().trim() : "String";
    }

    private static String tsType(String javaType) {
        return switch (javaType) {
            case "Integer", "Long", "Double", "Float", "BigDecimal" -> "number";
            case "Boolean", "boolean" -> "boolean";
            default -> "string";
        };
    }

    private static boolean isDateType(GenDtos.GenColumnItem column) {
        return javaType(column).equals("LocalDate") || javaType(column).equals("LocalDateTime");
    }

    private static String normalizeFilter(String filterType) {
        if (!StringUtils.hasText(filterType)) {
            return "EQ";
        }
        String value = filterType.trim().toUpperCase(Locale.ROOT);
        return FILTER_TYPES.contains(value) ? value : "EQ";
    }

    private static String comment(GenDtos.GenColumnItem column) {
        return StringUtils.hasText(column.columnComment()) ? column.columnComment() : column.columnName();
    }

    private static String getter(String prop) {
        return "get" + GenService.pascal(prop);
    }

    private static String kebab(String name) {
        StringBuilder sb = new StringBuilder();
        for (char ch : name.toCharArray()) {
            if (Character.isUpperCase(ch)) {
                if (!sb.isEmpty()) {
                    sb.append('-');
                }
                sb.append(Character.toLowerCase(ch));
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    /** 生成上下文：统一计算包名、路径、主键与筛选项。 */
    private static final class Ctx {

        final GenDtos.GenConfigItem table;
        final List<GenDtos.GenColumnItem> columns;
        final List<GenDtos.GenColumnItem> filters;
        final GenDtos.GenColumnItem pk;
        final boolean jdbc;
        final String pkg;
        final String urlPath;
        final String moduleName;
        final String businessName;
        final String className;
        final String frontendPath;
        final String perm;
        final String functionName;
        final String tableName;

        Ctx(GenDtos.GenConfigDetail detail) {
            this.table = detail.table();
            this.columns = detail.columns() == null ? List.of() : detail.columns();
            this.tableName = table.tableName();
            this.moduleName = table.moduleName();
            this.businessName = table.businessName();
            this.className = table.className();
            this.frontendPath = table.frontendPath();
            this.perm = table.permPrefix();
            this.functionName = table.functionName();
            this.pkg = table.packageName() + "." + moduleName + "." + businessName;
            this.urlPath = "/api/" + moduleName + "/" + kebab(businessName);
            this.jdbc = "data-jdbc".equalsIgnoreCase(table.genStyle());
            this.filters = columns.stream()
                    .filter(column -> Boolean.TRUE.equals(column.isFilter()))
                    .toList();
            this.pk = columns.stream().filter(column -> Boolean.TRUE.equals(column.isPk()))
                    .findFirst().orElse(columns.isEmpty() ? null : columns.getFirst());
            if (pk == null) {
                throw new IllegalStateException("表 " + tableName + " 没有可用字段");
            }
        }

        String commentSafe(String text) {
            return StringUtils.hasText(text) ? text.replace("\"", "'") : tableName;
        }
    }

    /** 生成的查询参数（RANGE 会拆成两个字段）。 */
    private record Field(String prop, String column, String op, String comment) {
    }
}
