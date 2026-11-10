package com.platform.api.gen;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 代码生成器：按「主表 + 关联表」配置输出后端、前端与菜单 SQL。
 *
 * <p>两种风格：
 * <ul>
 *     <li>{@code dynamic-sql}：展平的查询 DTO（record）+ 动态 SQL 执行器，不生成实体</li>
 *     <li>{@code data-jdbc}：额外生成每张表的领域实体，主表实体按关联方式声明聚合关系</li>
 * </ul>
 * 列表与查询条件始终基于主表 LEFT JOIN 关联表，列可以来自任意参与关联的表。
 */
@Component
public class CodeGenerator {

    private static final String MAIN = GenService.MAIN;

    private static final String ONE_TO_MANY = "ONE_TO_MANY";

    private static final String ONE_TO_ONE = "ONE_TO_ONE";

    public List<GenDtos.GenFileItem> generate(GenDtos.GenConfigDetail detail) {
        Ctx ctx = new Ctx(detail);
        List<GenDtos.GenFileItem> files = new ArrayList<>();
        files.add(javaFile(ctx, ctx.className + "Dtos.java", dtos(ctx)));
        files.add(javaFile(ctx, ctx.className + "Service.java", service(ctx)));
        files.add(javaFile(ctx, ctx.className + "Controller.java", controller(ctx)));
        if (ctx.jdbc) {
            for (TableModel table : ctx.tables) {
                files.add(javaFile(ctx, table.entityName() + ".java", entity(ctx, table)));
            }
            files.add(javaFile(ctx, ctx.className + "Repository.java", repository(ctx)));
        }
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

    // ------------------------------------------------------------------ 查询 DTO

    private String dtos(Ctx ctx) {
        StringBuilder sb = new StringBuilder("package ").append(ctx.pkg).append(";\n\n")
                .append("import com.platform.api.common.query.Op;\n")
                .append("import com.platform.api.common.query.PageQuery;\n")
                .append("import com.platform.api.common.query.QueryField;\n")
                .append("import com.platform.api.common.query.QuerySort;\n")
                .append("import com.platform.api.common.query.QuerySortMapping;\n");
        appendTypeImports(sb, ctx.allColumns.stream().map(Col::javaType).toList());
        sb.append("\n/**\n * ").append(ctx.title()).append("：多表查询模型。\n")
                .append(" *\n * <p>主表 ").append(ctx.tableName).append(ctx.joinTitle()).append("\n */\n")
                .append("public class ").append(ctx.className).append("Dtos {\n\n")
                .append("    /** 列表行：字段按「表前缀 + 列名」展平，避免同名列冲突；固定包含主表主键 */\n")
                .append("    public record Item(").append(itemComponents(ctx)).append(") {\n    }\n\n")
                .append("    /** 详情：主表全部字段 */\n")
                .append("    public record Detail(").append(components(ctx.mainColumns)).append(") {\n    }\n\n")
                .append("    /** 新增/编辑表单：主表可编辑字段")
                .append(ctx.autoIncrementPk ? "（数值主键按自增处理）" : "（含字符串主键，新增时需传入）")
                .append(" */\n")
                .append("    public record SaveForm(").append(components(ctx.saveFormColumns())).append(") {\n    }\n\n")
                .append("    /** 分页查询参数 */\n");

        if (!ctx.sortColumns.isEmpty()) {
            sb.append("    @QuerySort(value = {\n");
            for (Col column : ctx.sortColumns) {
                sb.append("        @QuerySortMapping(property = \"").append(column.apiProp())
                        .append("\", column = \"").append(ctx.qualified(column)).append("\"),\n");
            }
            sb.append("    }, defaultSort = \"").append(ctx.defaultSort()).append("\")\n");
        }

        sb.append("    public static class Query extends PageQuery {\n");
        if (ctx.filterColumns.isEmpty()) {
            sb.append("        // 未配置查询条件\n");
        }
        for (Col column : ctx.filterColumns) {
            sb.append("\n        /** ").append(column.comment()).append(" */\n")
                    .append("        @QueryField(value = \"").append(ctx.qualified(column))
                    .append("\", op = Op.").append(column.filterType()).append(")\n")
                    .append("        private ").append(queryFieldType(column)).append(" ")
                    .append(column.apiProp()).append(";\n");
        }
        for (Col column : ctx.filterColumns) {
            String type = queryFieldType(column);
            String accessor = GenService.capitalize(column.apiProp());
            sb.append("\n        public ").append(type).append(" get").append(accessor).append("() {\n")
                    .append("            return ").append(column.apiProp()).append(";\n        }\n\n")
                    .append("        public void set").append(accessor).append("(").append(type)
                    .append(" ").append(column.apiProp()).append(") {\n            this.")
                    .append(column.apiProp()).append(" = ").append(column.apiProp())
                    .append(";\n        }\n");
        }
        sb.append("    }\n}\n");
        return sb.toString();
    }

    private String itemComponents(Ctx ctx) {
        List<String> parts = new ArrayList<>();
        for (Col column : ctx.itemColumns) {
            parts.add(column.javaType() + " " + column.apiProp());
        }
        return String.join(", ", parts);
    }

    private String components(List<Col> columns) {
        List<String> parts = new ArrayList<>();
        for (Col column : columns) {
            parts.add(column.javaType() + " " + column.propertyName());
        }
        return String.join(", ", parts);
    }

    // ------------------------------------------------------------------ 后端服务

    private String service(Ctx ctx) {
        StringBuilder sb = new StringBuilder()
                .append("package ").append(ctx.pkg).append(";\n\n")
                .append("import com.platform.api.common.BusinessException;\n")
                .append("import com.platform.api.common.query.DynamicQueryExecutor;\n")
                .append("import com.platform.api.common.query.RecordRowMapper;\n")
                .append("import org.springframework.data.domain.Page;\n")
                .append("import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;\n")
                .append("import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;\n");
        if (ctx.autoIncrementPk) {
            sb.append("import org.springframework.jdbc.support.GeneratedKeyHolder;\n")
                    .append("import org.springframework.jdbc.support.KeyHolder;\n");
        }
        sb.append("import org.springframework.stereotype.Service;\n\n")
                .append("import java.util.List;\n")
                .append("import java.util.Map;\n\n")
                .append("/**\n * ").append(ctx.title()).append("：多表分页查询 + 主表增删改查。\n")
                .append(" *\n * <p>列表与查询条件基于主表 LEFT JOIN 关联表；增删改只操作主表 ")
                .append(ctx.tableName).append("。\n */\n")
                .append("@Service\n")
                .append("public class ").append(ctx.className).append("Service {\n\n")
                .append("    private static final String FROM_SQL = \"").append(escape(ctx.fromSql))
                .append("\";\n\n")
                .append("    private static final String SELECT_SQL = \"SELECT ")
                .append(escape(ctx.selectColumns)).append(" \" + FROM_SQL;\n\n")
                .append("    private static final String COUNT_SQL = \"SELECT COUNT(*) \" + FROM_SQL;\n\n")
                .append("    private static final String DETAIL_SQL = \"").append(escape(detailSql(ctx)))
                .append("\";\n\n")
                .append("    private static final String INSERT_SQL = \"").append(escape(insertSql(ctx)))
                .append("\";\n\n")
                .append("    private static final String UPDATE_SQL = \"").append(escape(updateSql(ctx)))
                .append("\";\n\n")
                .append("    private static final String DELETE_SQL = \"").append(escape(deleteSql(ctx)))
                .append("\";\n\n")
                .append("    private final DynamicQueryExecutor executor;\n\n")
                .append("    private final NamedParameterJdbcTemplate jdbc;\n\n")
                .append("    public ").append(ctx.className)
                .append("Service(DynamicQueryExecutor executor, NamedParameterJdbcTemplate jdbc) {\n")
                .append("        this.executor = executor;\n")
                .append("        this.jdbc = jdbc;\n    }\n\n")
                .append("    public Page<").append(ctx.className).append("Dtos.Item> page(")
                .append(ctx.className).append("Dtos.Query query) {\n")
                .append("        return executor.page(SELECT_SQL, COUNT_SQL, query,\n")
                .append("                RecordRowMapper.of(").append(ctx.className).append("Dtos.Item.class));\n")
                .append("    }\n\n")
                .append("    /** 主表详情 */\n")
                .append("    public ").append(ctx.className).append("Dtos.Detail getById(")
                .append(ctx.mainPk.javaType()).append(" id) {\n")
                .append("        List<").append(ctx.className).append("Dtos.Detail> rows = jdbc.query(DETAIL_SQL,\n")
                .append("                Map.of(\"pk\", id), RecordRowMapper.of(").append(ctx.className)
                .append("Dtos.Detail.class));\n")
                .append("        if (rows.isEmpty()) {\n")
                .append("            throw BusinessException.notFound(\"").append(ctx.title())
                .append("不存在: \" + id);\n        }\n")
                .append("        return rows.getFirst();\n    }\n\n")
                .append("    /** 新增（仅主表） */\n")
                .append("    public ").append(ctx.mainPk.javaType()).append(" create(")
                .append(ctx.className).append("Dtos.SaveForm form) {\n");
        if (ctx.autoIncrementPk) {
            sb.append("        KeyHolder keyHolder = new GeneratedKeyHolder();\n")
                    .append("        jdbc.update(INSERT_SQL, saveParams(form), keyHolder, new String[]{\"")
                    .append(ctx.mainPk.columnName()).append("\"});\n")
                    .append("        Number key = keyHolder.getKey();\n")
                    .append("        if (key == null) {\n")
                    .append("            throw new IllegalStateException(\"主键生成失败\");\n        }\n")
                    .append("        return key.").append(ctx.mainPk.javaType().equals("Long") ? "longValue()"
                    : ctx.mainPk.javaType().equals("Integer") ? "intValue()" : "shortValue()").append(";\n");
        } else {
            sb.append("        jdbc.update(INSERT_SQL, saveParams(form));\n")
                    .append("        return form.").append(ctx.mainPk.propertyName()).append("();\n");
        }
        sb.append("    }\n\n")
                .append("    /** 修改（仅主表，按主键） */\n")
                .append("    public void update(").append(ctx.mainPk.javaType()).append(" id, ")
                .append(ctx.className).append("Dtos.SaveForm form) {\n")
                .append("        int rows = jdbc.update(UPDATE_SQL, saveParams(form).addValue(\"pk\", id));\n")
                .append("        if (rows == 0) {\n")
                .append("            throw BusinessException.notFound(\"").append(ctx.title())
                .append("不存在: \" + id);\n        }\n")
                .append("    }\n\n")
                .append("    /** 删除（仅主表，按主键） */\n")
                .append("    public void remove(").append(ctx.mainPk.javaType()).append(" id) {\n")
                .append("        int rows = jdbc.update(DELETE_SQL, Map.of(\"pk\", id));\n")
                .append("        if (rows == 0) {\n")
                .append("            throw BusinessException.notFound(\"").append(ctx.title())
                .append("不存在: \" + id);\n        }\n")
                .append("    }\n\n")
                .append("    private MapSqlParameterSource saveParams(").append(ctx.className)
                .append("Dtos.SaveForm form) {\n")
                .append("        return new MapSqlParameterSource()\n");
        List<Col> saveColumns = ctx.saveFormColumns();
        for (int i = 0; i < saveColumns.size(); i++) {
            Col column = saveColumns.get(i);
            sb.append("                .addValue(\"").append(column.propertyName()).append("\", form.")
                    .append(column.propertyName()).append("())")
                    .append(i == saveColumns.size() - 1 ? ";\n" : "\n");
        }
        sb.append("    }\n}\n");
        return sb.toString();
    }

    /** 详情 SQL：主表全部字段，按主键查询 */
    private String detailSql(Ctx ctx) {
        List<String> columns = new ArrayList<>();
        for (Col column : ctx.mainColumns) {
            columns.add(column.columnName());
        }
        return "SELECT " + String.join(", ", columns) + " FROM " + ctx.tableName
                + " WHERE " + ctx.mainPk.columnName() + " = :pk";
    }

    /** 新增 SQL：可编辑列 +（字符串主键） */
    private String insertSql(Ctx ctx) {
        List<String> columns = new ArrayList<>();
        List<String> values = new ArrayList<>();
        for (Col column : ctx.saveFormColumns()) {
            columns.add(column.columnName());
            values.add(":" + column.propertyName());
        }
        return "INSERT INTO " + ctx.tableName + " (" + String.join(", ", columns)
                + ") VALUES (" + String.join(", ", values) + ")";
    }

    /** 修改 SQL：仅可编辑列 */
    private String updateSql(Ctx ctx) {
        List<String> sets = new ArrayList<>();
        for (Col column : ctx.editableColumns) {
            sets.add(column.columnName() + " = :" + column.propertyName());
        }
        return "UPDATE " + ctx.tableName + " SET " + String.join(", ", sets)
                + " WHERE " + ctx.mainPk.columnName() + " = :pk";
    }

    private String deleteSql(Ctx ctx) {
        return "DELETE FROM " + ctx.tableName + " WHERE " + ctx.mainPk.columnName() + " = :pk";
    }

    private String controller(Ctx ctx) {
        String serviceField = decapitalize(ctx.className) + "Service";
        String pkType = ctx.mainPk.javaType();
        return new StringBuilder()
                .append("package ").append(ctx.pkg).append(";\n\n")
                .append("import com.platform.api.common.ResultInfo;\n")
                .append("import org.springframework.data.domain.Page;\n")
                .append("import org.springframework.security.access.prepost.PreAuthorize;\n")
                .append("import org.springframework.web.bind.annotation.DeleteMapping;\n")
                .append("import org.springframework.web.bind.annotation.GetMapping;\n")
                .append("import org.springframework.web.bind.annotation.PathVariable;\n")
                .append("import org.springframework.web.bind.annotation.PostMapping;\n")
                .append("import org.springframework.web.bind.annotation.PutMapping;\n")
                .append("import org.springframework.web.bind.annotation.RequestBody;\n")
                .append("import org.springframework.web.bind.annotation.RequestMapping;\n")
                .append("import org.springframework.web.bind.annotation.RestController;\n\n")
                .append("/**\n * ").append(ctx.title()).append("：多表分页查询 + 主表增删改查。\n */\n")
                .append("@RestController\n")
                .append("@RequestMapping(\"").append(ctx.urlPath).append("\")\n")
                .append("public class ").append(ctx.className).append("Controller {\n\n")
                .append("    private final ").append(ctx.className).append("Service ").append(serviceField)
                .append(";\n\n")
                .append("    public ").append(ctx.className).append("Controller(")
                .append(ctx.className).append("Service ").append(serviceField).append(") {\n")
                .append("        this.").append(serviceField).append(" = ").append(serviceField)
                .append(";\n    }\n\n")
                .append("    @GetMapping\n")
                .append("    @PreAuthorize(\"@perm.has('").append(ctx.perm).append("')\")\n")
                .append("    public ResultInfo<Page<").append(ctx.className).append("Dtos.Item>> page(")
                .append(ctx.className).append("Dtos.Query query) {\n")
                .append("        return ResultInfo.ok(").append(serviceField).append(".page(query));\n")
                .append("    }\n\n")
                .append("    @GetMapping(\"/{id}\")\n")
                .append("    @PreAuthorize(\"@perm.has('").append(ctx.perm).append("')\")\n")
                .append("    public ResultInfo<").append(ctx.className).append("Dtos.Detail> detail(")
                .append("@PathVariable ").append(pkType).append(" id) {\n")
                .append("        return ResultInfo.ok(").append(serviceField).append(".getById(id));\n")
                .append("    }\n\n")
                .append("    @PostMapping\n")
                .append("    @PreAuthorize(\"@perm.has('").append(ctx.permBase()).append(":add')\")\n")
                .append("    public ResultInfo<").append(pkType).append("> create(@RequestBody ")
                .append(ctx.className).append("Dtos.SaveForm form) {\n")
                .append("        return ResultInfo.ok(").append(serviceField).append(".create(form));\n")
                .append("    }\n\n")
                .append("    @PutMapping(\"/{id}\")\n")
                .append("    @PreAuthorize(\"@perm.has('").append(ctx.permBase()).append(":edit')\")\n")
                .append("    public ResultInfo<Void> update(@PathVariable ").append(pkType)
                .append(" id, @RequestBody ").append(ctx.className).append("Dtos.SaveForm form) {\n")
                .append("        ").append(serviceField).append(".update(id, form);\n")
                .append("        return ResultInfo.ok();\n")
                .append("    }\n\n")
                .append("    @DeleteMapping(\"/{id}\")\n")
                .append("    @PreAuthorize(\"@perm.has('").append(ctx.permBase()).append(":remove')\")\n")
                .append("    public ResultInfo<Void> delete(@PathVariable ").append(pkType).append(" id) {\n")
                .append("        ").append(serviceField).append(".remove(id);\n")
                .append("        return ResultInfo.ok();\n")
                .append("    }\n}\n")
                .toString();
    }

    // ------------------------------------------------------------------ Data JDBC 实体

    private String entity(Ctx ctx, TableModel table) {
        boolean mainTable = table.tableName().equals(ctx.tableName);
        List<RelationModel> relations = mainTable ? ctx.relations : List.of();
        boolean hasCollection = relations.stream().anyMatch(r -> ONE_TO_MANY.equals(r.relationType()));
        boolean hasReference = relations.stream().anyMatch(r -> !ONE_TO_MANY.equals(r.relationType()));
        boolean needsColumn = table.columns().stream()
                .anyMatch(column -> !snake(column.propertyName()).equalsIgnoreCase(column.columnName()));

        StringBuilder sb = new StringBuilder("package ").append(ctx.pkg).append(";\n\n")
                .append("import org.springframework.data.annotation.Id;\n")
                .append("import org.springframework.data.relational.core.mapping.Table;\n");
        if (needsColumn) {
            sb.append("import org.springframework.data.relational.core.mapping.Column;\n");
        }
        if (hasReference) {
            sb.append("import org.springframework.data.annotation.Transient;\n");
        }
        if (hasCollection) {
            sb.append("import org.springframework.data.relational.core.mapping.MappedCollection;\n")
                    .append("import java.util.LinkedHashSet;\n")
                    .append("import java.util.Set;\n");
        }
        appendTypeImports(sb, table.columns().stream().map(Col::javaType).toList());

        sb.append("\n/**\n * ").append(table.tableComment()).append("实体（").append(table.tableName())
                .append("）。\n */\n")
                .append("@Table(\"").append(table.tableName()).append("\")\n")
                .append("public class ").append(table.entityName()).append(" {\n");

        for (Col column : table.columns()) {
            sb.append("\n    /** ").append(column.comment()).append(" */\n");
            if (column.pk()) {
                sb.append("    @Id\n");
            }
            if (!snake(column.propertyName()).equalsIgnoreCase(column.columnName())) {
                sb.append("    @Column(\"").append(column.columnName()).append("\")\n");
            }
            sb.append("    private ").append(column.javaType()).append(" ").append(column.propertyName())
                    .append(";\n");
        }

        for (RelationModel relation : relations) {
            sb.append("\n    /** ").append(relationTitle(relation.relationType())).append("：")
                    .append(relation.title()).append(" */\n");
            if (ONE_TO_MANY.equals(relation.relationType())) {
                sb.append("    @MappedCollection(idColumn = \"").append(relation.joinColumn()).append("\")\n")
                        .append("    private Set<").append(relation.entityName()).append("> ")
                        .append(relation.property()).append(" = new LinkedHashSet<>();\n");
            } else {
                sb.append("    @Transient\n")
                        .append("    private ").append(relation.entityName()).append(" ")
                        .append(relation.property()).append(";\n");
            }
        }

        for (Col column : table.columns()) {
            String accessor = GenService.capitalize(column.propertyName());
            sb.append("\n    public ").append(column.javaType()).append(" get").append(accessor).append("() {\n")
                    .append("        return ").append(column.propertyName()).append(";\n    }\n\n")
                    .append("    public void set").append(accessor).append("(").append(column.javaType())
                    .append(" ").append(column.propertyName()).append(") {\n        this.")
                    .append(column.propertyName()).append(" = ").append(column.propertyName())
                    .append(";\n    }\n");
        }
        for (RelationModel relation : relations) {
            String accessor = GenService.capitalize(relation.property());
            String type = ONE_TO_MANY.equals(relation.relationType())
                    ? "Set<" + relation.entityName() + ">" : relation.entityName();
            sb.append("\n    public ").append(type).append(" get").append(accessor).append("() {\n")
                    .append("        return ").append(relation.property()).append(";\n    }\n\n")
                    .append("    public void set").append(accessor).append("(").append(type).append(" ")
                    .append(relation.property()).append(") {\n        this.").append(relation.property())
                    .append(" = ").append(relation.property()).append(";\n    }\n");
        }
        sb.append("}\n");
        return sb.toString();
    }

    private String repository(Ctx ctx) {
        return new StringBuilder()
                .append("package ").append(ctx.pkg).append(";\n\n")
                .append("import org.springframework.data.repository.CrudRepository;\n\n")
                .append("/**\n * ").append(ctx.title()).append("：主表聚合根仓库。\n */\n")
                .append("public interface ").append(ctx.className).append("Repository extends CrudRepository<")
                .append(ctx.mainEntityName()).append(", ").append(ctx.mainPk.javaType()).append("> {\n}\n")
                .toString();
    }

    // ------------------------------------------------------------------ 前端

    private String frontendTypes(Ctx ctx) {
        StringBuilder sb = new StringBuilder("import type { PageQuery } from \"@/api/common\";\n\n")
                .append("/**\n * ").append(ctx.title())
                .append("查询参数（字段来自 ").append(ctx.tableName).append(" 及关联表）\n */\n")
                .append("export interface ").append(ctx.className).append("Query extends PageQuery {\n");
        if (ctx.filterColumns.isEmpty()) {
            sb.append("  // 未配置查询条件\n");
        }
        for (Col column : ctx.filterColumns) {
            sb.append("  /** ").append(column.comment()).append(" */\n");
            if ("BETWEEN".equals(column.filterType())) {
                if (column.dateType()) {
                    sb.append("  ").append(column.apiProp()).append("?: [string, string];\n");
                } else {
                    sb.append("  ").append(column.apiProp()).append("Start?: string;\n")
                            .append("  ").append(column.apiProp()).append("End?: string;\n");
                }
            } else {
                sb.append("  ").append(column.apiProp()).append("?: string;\n");
            }
        }
        sb.append("}\n\n")
                .append("/**\n * ").append(ctx.title()).append("列表行\n */\n")
                .append("export interface ").append(ctx.className).append("Item {\n");
        for (Col column : ctx.itemColumns) {
            sb.append("  /** ").append(column.comment()).append("（").append(column.tableName()).append("） */\n")
                    .append("  ").append(column.apiProp()).append("?: ").append(tsType(column.javaType()))
                    .append(";\n");
        }
        sb.append("}\n\n")
                .append("/**\n * ").append(ctx.title()).append("详情（主表全部字段）\n */\n")
                .append("export interface ").append(ctx.className).append("Detail {\n");
        for (Col column : ctx.mainColumns) {
            sb.append("  /** ").append(column.comment()).append(" */\n")
                    .append("  ").append(column.propertyName()).append("?: ").append(tsType(column.javaType()))
                    .append(";\n");
        }
        sb.append("}\n\n")
                .append("/**\n * ").append(ctx.title()).append("新增/编辑表单（仅主表可编辑字段）\n */\n")
                .append("export interface ").append(ctx.className).append("SaveForm {\n");
        for (Col column : ctx.saveFormColumns()) {
            sb.append("  /** ").append(column.comment()).append(" */\n")
                    .append("  ").append(column.propertyName()).append("?: ").append(tsType(column.javaType()))
                    .append(";\n");
        }
        sb.append("}\n");
        return sb.toString();
    }

    private String frontendApi(Ctx ctx) {
        return new StringBuilder()
                .append("import request from \"@/utils/request\";\n")
                .append("import type { Pagination } from \"@/api/common\";\n")
                .append("import type {\n  ").append(ctx.className).append("Detail,\n  ")
                .append(ctx.className).append("Item,\n  ").append(ctx.className).append("Query,\n  ")
                .append(ctx.className).append("SaveForm,\n} from \"./types\";\n\n")
                .append("const BASE_URL = \"").append(ctx.urlPath).append("\";\n\n")
                .append("const ").append(ctx.className).append("API = {\n")
                .append("  /**\n     * 获取").append(ctx.title()).append("分页数据\n   */\n")
                .append("  getPage(queryParams?: ").append(ctx.className).append("Query) {\n")
                .append("    return request<unknown, Pagination<").append(ctx.className).append("Item>>({\n")
                .append("      url: BASE_URL,\n")
                .append("      method: \"get\",\n")
                .append("      params: queryParams,\n")
                .append("    });\n  },\n\n")
                .append("  /**\n     * 主表详情\n   */\n")
                .append("  getDetail(id: string | number) {\n")
                .append("    return request<unknown, ").append(ctx.className).append("Detail>({\n")
                .append("      url: `${BASE_URL}/${id}`,\n")
                .append("      method: \"get\",\n")
                .append("    });\n  },\n\n")
                .append("  /**\n     * 新增（仅主表）\n   */\n")
                .append("  create(data: ").append(ctx.className).append("SaveForm) {\n")
                .append("    return request<unknown, string | number>({\n")
                .append("      url: BASE_URL,\n")
                .append("      method: \"post\",\n")
                .append("      data,\n")
                .append("    });\n  },\n\n")
                .append("  /**\n     * 修改（仅主表）\n   */\n")
                .append("  update(id: string | number, data: ").append(ctx.className).append("SaveForm) {\n")
                .append("    return request({\n")
                .append("      url: `${BASE_URL}/${id}`,\n")
                .append("      method: \"put\",\n")
                .append("      data,\n")
                .append("    });\n  },\n\n")
                .append("  /**\n     * 删除（仅主表）\n   */\n")
                .append("  remove(id: string | number) {\n")
                .append("    return request({\n")
                .append("      url: `${BASE_URL}/${id}`,\n")
                .append("      method: \"delete\",\n")
                .append("    });\n  },\n};\n\n")
                .append("export default ").append(ctx.className).append("API;\n\n")
                .append("export * from \"./types\";\n")
                .toString();
    }

    private String frontendView(Ctx ctx) {
        String apiName = ctx.className + "API";
        List<Col> rangeColumns = ctx.filterColumns.stream()
                .filter(column -> "BETWEEN".equals(column.filterType()) && !column.dateType()).toList();
        boolean hasDate = ctx.itemColumns.stream().anyMatch(Col::dateType);
        String pkProp = ctx.mainPk.propertyName();
        String pkTsType = tsType(ctx.mainPk.javaType());

        StringBuilder sb = new StringBuilder("<template>\n")
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
                .append("        <div class=\"page-toolbar__left\">\n")
                .append("          <el-button v-hasPerm=\"'").append(ctx.permBase())
                .append(":add'\" type=\"primary\" @click=\"handleAdd\">新增</el-button>\n")
                .append("        </div>\n")
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
                .append(tableColumns(ctx))
                .append("          <el-table-column label=\"操作\" width=\"140\" fixed=\"right\">\n")
                .append("            <template #default=\"scope\">\n")
                .append("              <el-button\n")
                .append("                v-hasPerm=\"'").append(ctx.permBase()).append(":edit'\"\n")
                .append("                link\n")
                .append("                type=\"primary\"\n")
                .append("                @click=\"handleEdit(scope.row)\"\n")
                .append("              >\n")
                .append("                编辑\n")
                .append("              </el-button>\n")
                .append("              <el-button\n")
                .append("                v-hasPerm=\"'").append(ctx.permBase()).append(":remove'\"\n")
                .append("                link\n")
                .append("                type=\"danger\"\n")
                .append("                @click=\"handleDelete(scope.row)\"\n")
                .append("              >\n")
                .append("                删除\n")
                .append("              </el-button>\n")
                .append("            </template>\n")
                .append("          </el-table-column>\n")
                .append("        </el-table>\n")
                .append("      </div>\n\n")
                .append("      <pagination\n")
                .append("        v-if=\"total > 0\"\n")
                .append("        v-model:total=\"total\"\n")
                .append("        v-model:page=\"params.pageNum\"\n")
                .append("        v-model:limit=\"params.pageSize\"\n")
                .append("        @pagination=\"fetchData\"\n")
                .append("      />\n")
                .append("    </el-card>\n\n")
                .append("    <el-dialog\n")
                .append("      v-model=\"dialog.visible\"\n")
                .append("      :title=\"dialog.title\"\n")
                .append("      width=\"640px\"\n")
                .append("      destroy-on-close\n")
                .append("    >\n")
                .append("      <el-form ref=\"formRef\" :model=\"form\" :rules=\"rules\" label-width=\"120px\">\n")
                .append(formItems(ctx))
                .append("      </el-form>\n")
                .append("      <template #footer>\n")
                .append("        <el-button @click=\"dialog.visible = false\">取消</el-button>\n")
                .append("        <el-button type=\"primary\" :loading=\"dialog.saving\" @click=\"handleSubmit\">\n")
                .append("          确定\n")
                .append("        </el-button>\n")
                .append("      </template>\n")
                .append("    </el-dialog>\n")
                .append("  </div>\n")
                .append("</template>\n\n")
                .append("<script setup lang=\"ts\">\n")
                .append("import { Refresh } from \"@element-plus/icons-vue\";\n")
                .append("import { ElMessage, ElMessageBox } from \"element-plus\";\n")
                .append("import type { FormInstance, FormRules } from \"element-plus\";\n\n")
                .append("import ").append(apiName).append(" from \"@/api/").append(ctx.frontendPath)
                .append("\";\n")
                .append("import type {\n")
                .append("  ").append(ctx.className).append("Item,\n")
                .append("  ").append(ctx.className).append("Query,\n")
                .append("  ").append(ctx.className).append("SaveForm,\n")
                .append("} from \"@/api/").append(ctx.frontendPath).append("\";\n")
                .append("import { usePageTable } from \"@/composables\";\n\n")
                .append("defineOptions({\n")
                .append("  name: \"").append(ctx.className).append("\",\n")
                .append("  inheritAttrs: false,\n")
                .append("});\n\n");
        if (hasDate) {
            sb.append("const formatDateTime = (value?: string) => (value ? value.replace(\"T\", \" \") : \"\");\n\n");
        }
        if (!rangeColumns.isEmpty()) {
            sb.append("const buildQuery = (query: ").append(ctx.className).append("Query) => ({\n")
                    .append("  ...query,\n");
            for (Col column : rangeColumns) {
                sb.append("  ").append(column.apiProp()).append(": [query.").append(column.apiProp())
                        .append("Start, query.").append(column.apiProp()).append("End]")
                        .append(".filter((value) => value !== undefined && value !== \"\"),\n");
            }
            sb.append("});\n\n");
        }
        String request = rangeColumns.isEmpty()
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
                .append("const formRef = ref<FormInstance>();\n")
                .append("const dialog = reactive({\n")
                .append("  visible: false,\n")
                .append("  title: \"\",\n")
                .append("  saving: false,\n")
                .append("  id: undefined as ").append(pkTsType).append(" | undefined,\n")
                .append("});\n\n")
                .append("const initialForm = (): ").append(ctx.className).append("SaveForm => ({\n");
        for (Col column : ctx.saveFormColumns()) {
            sb.append("  ").append(column.propertyName()).append(": ")
                    .append("Boolean".equals(column.javaType()) ? "false" : "undefined").append(",\n");
        }
        sb.append("});\n\n")
                .append("const form = reactive<").append(ctx.className).append("SaveForm>(initialForm());\n\n");
        List<Col> requiredColumns = ctx.saveFormColumns().stream().filter(Col::required).toList();
        if (!requiredColumns.isEmpty()) {
            sb.append("const rules: FormRules = {\n");
            for (Col column : requiredColumns) {
                String trigger = switch (column.formComponent()) {
                    case "NUMBER", "SWITCH", "DATE", "DATETIME", "SELECT" -> "change";
                    default -> "blur";
                };
                sb.append("  ").append(column.propertyName())
                        .append(": [{ required: true, message: \"请输入").append(column.label())
                        .append("\", trigger: \"").append(trigger).append("\" }],\n");
            }
            sb.append("});\n\n");
        } else {
            sb.append("const rules: FormRules = {};\n\n");
        }
        sb.append("const resetForm = () => Object.assign(form, initialForm());\n\n")
                .append("const handleAdd = () => {\n")
                .append("  resetForm();\n")
                .append("  dialog.id = undefined;\n")
                .append("  dialog.title = \"新增").append(ctx.title()).append("\";\n")
                .append("  dialog.visible = true;\n")
                .append("};\n\n")
                .append("const handleEdit = async (row: ").append(ctx.className).append("Item) => {\n")
                .append("  const detail = await ").append(apiName).append(".getDetail(row.").append(pkProp)
                .append(" as ").append(pkTsType).append(");\n")
                .append("  resetForm();\n")
                .append("  Object.assign(form, detail);\n")
                .append("  dialog.id = detail.").append(pkProp).append(";\n")
                .append("  dialog.title = \"编辑").append(ctx.title()).append("\";\n")
                .append("  dialog.visible = true;\n")
                .append("};\n\n")
                .append("const handleSubmit = async () => {\n")
                .append("  await formRef.value?.validate();\n")
                .append("  dialog.saving = true;\n")
                .append("  try {\n")
                .append("    if (dialog.id === undefined) {\n")
                .append("      await ").append(apiName).append(".create(form);\n")
                .append("    } else {\n")
                .append("      await ").append(apiName).append(".update(dialog.id, form);\n")
                .append("    }\n")
                .append("    ElMessage.success(\"保存成功\");\n")
                .append("    dialog.visible = false;\n")
                .append("    fetchData();\n")
                .append("  } finally {\n")
                .append("    dialog.saving = false;\n")
                .append("  }\n")
                .append("};\n\n")
                .append("const handleDelete = async (row: ").append(ctx.className).append("Item) => {\n")
                .append("  await ElMessageBox.confirm(\"确认删除该记录吗？\", \"警告\", {\n")
                .append("    confirmButtonText: \"确定\",\n")
                .append("    cancelButtonText: \"取消\",\n")
                .append("    type: \"warning\",\n")
                .append("  });\n")
                .append("  await ").append(apiName).append(".remove(row.").append(pkProp).append(" as ")
                .append(pkTsType).append(");\n")
                .append("  ElMessage.success(\"删除成功\");\n")
                .append("  fetchData();\n")
                .append("};\n\n")
                .append("onMounted(() => {\n")
                .append("  handleQuery();\n")
                .append("});\n")
                .append("</script>\n");
        return sb.toString();
    }

    /** 编辑对话框的表单项（仅主表可编辑字段） */
    private String formItems(Ctx ctx) {
        StringBuilder sb = new StringBuilder();
        for (Col column : ctx.saveFormColumns()) {
            String prop = column.propertyName();
            String label = column.label();
            sb.append("        <el-form-item label=\"").append(label).append("\" prop=\"").append(prop)
                    .append("\">\n");
            switch (column.formComponent()) {
                case "SWITCH" -> sb.append("          <el-switch v-model=\"form.").append(prop)
                        .append("\" />\n");
                case "NUMBER" -> sb.append("          <el-input-number v-model=\"form.").append(prop)
                        .append("\" controls-position=\"right\" style=\"width: 100%\" />\n");
                case "DATE" -> sb.append("          <el-date-picker\n")
                        .append("            v-model=\"form.").append(prop).append("\"\n")
                        .append("            type=\"date\"\n")
                        .append("            value-format=\"YYYY-MM-DD\"\n")
                        .append("            placeholder=\"请选择").append(label).append("\"\n")
                        .append("            style=\"width: 100%\"\n")
                        .append("          />\n");
                case "DATETIME" -> sb.append("          <el-date-picker\n")
                        .append("            v-model=\"form.").append(prop).append("\"\n")
                        .append("            type=\"datetime\"\n")
                        .append("            value-format=\"YYYY-MM-DD HH:mm:ss\"\n")
                        .append("            placeholder=\"请选择").append(label).append("\"\n")
                        .append("            style=\"width: 100%\"\n")
                        .append("          />\n");
                case "TEXTAREA" -> sb.append("          <el-input\n")
                        .append("            v-model=\"form.").append(prop).append("\"\n")
                        .append("            type=\"textarea\"\n")
                        .append("            :rows=\"3\"\n")
                        .append("            placeholder=\"请输入").append(label).append("\"\n")
                        .append("          />\n");
                default -> sb.append("          <el-input\n")
                        .append("            v-model=\"form.").append(prop).append("\"\n")
                        .append("            placeholder=\"请输入").append(label).append("\"\n")
                        .append("            clearable\n")
                        .append(column.pk() ? "            :disabled=\"dialog.id !== undefined\"\n" : "")
                        .append("          />\n");
            }
            sb.append("        </el-form-item>\n");
        }
        return sb.toString();
    }

    private String searchItems(Ctx ctx) {
        StringBuilder sb = new StringBuilder();
        for (Col column : ctx.filterColumns) {
            String prop = column.apiProp();
            String label = column.label();
            if (!column.tableName().equals(ctx.tableName)) {
                label = label + "（" + column.tableName() + "）";
            }
            if ("BETWEEN".equals(column.filterType())) {
                if (column.dateType()) {
                    boolean localDate = "LocalDate".equals(column.javaType());
                    sb.append("        <el-form-item label=\"").append(label).append("\" prop=\"").append(prop)
                            .append("\">\n")
                            .append("          <el-date-picker\n")
                            .append("            v-model=\"params.").append(prop).append("\"\n")
                            .append("            type=\"").append(localDate ? "daterange" : "datetimerange")
                            .append("\"\n")
                            .append("            value-format=\"")
                            .append(localDate ? "YYYY-MM-DD" : "YYYY-MM-DD[T]HH:mm:ss").append("\"\n")
                            .append("            range-separator=\"~\"\n")
                            .append("            start-placeholder=\"开始时间\"\n")
                            .append("            end-placeholder=\"结束时间\"\n")
                            .append("            style=\"width: 260px\"\n")
                            .append("          />\n")
                            .append("        </el-form-item>\n");
                } else {
                    sb.append("        <el-form-item label=\"").append(label).append(" 从\" prop=\"").append(prop)
                            .append("Start\">\n")
                            .append("          <el-input v-model=\"params.").append(prop)
                            .append("Start\" placeholder=\"最小值\" clearable />\n")
                            .append("        </el-form-item>\n")
                            .append("        <el-form-item label=\"至\" prop=\"").append(prop).append("End\">\n")
                            .append("          <el-input v-model=\"params.").append(prop)
                            .append("End\" placeholder=\"最大值\" clearable />\n")
                            .append("        </el-form-item>\n");
                }
                continue;
            }
            String placeholder = "IN".equals(column.filterType()) || "NOT_IN".equals(column.filterType())
                    ? "多个值用英文逗号分隔" : "请输入" + label;
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

    private String tableColumns(Ctx ctx) {
        StringBuilder sb = new StringBuilder();
        for (Col column : ctx.listColumns) {
            String prop = column.apiProp();
            String label = column.label();
            if (column.dateType()) {
                sb.append("          <el-table-column key=\"").append(prop).append("\" label=\"")
                        .append(label).append("\" min-width=\"160\">\n")
                        .append("            <template #default=\"scope\">{{ formatDateTime(scope.row.")
                        .append(prop).append(") }}</template>\n")
                        .append("          </el-table-column>\n");
            } else {
                sb.append("          <el-table-column key=\"").append(prop).append("\" label=\"")
                        .append(label).append("\" prop=\"").append(prop).append("\" min-width=\"120\" />\n");
            }
        }
        return sb.toString();
    }

    private String initialParams(Ctx ctx) {
        StringBuilder sb = new StringBuilder();
        for (Col column : ctx.filterColumns) {
            if ("BETWEEN".equals(column.filterType())) {
                // 日期区间由单个 range 选择器绑定，初始为空即可
                if (!column.dateType()) {
                    sb.append("    ").append(column.apiProp()).append("Start: \"\",\n")
                            .append("    ").append(column.apiProp()).append("End: \"\",\n");
                }
            } else {
                sb.append("    ").append(column.apiProp()).append(": \"\",\n");
            }
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ 菜单 SQL

    private String menuSql(Ctx ctx) {
        String menuPath = kebab(ctx.config.businessName());
        String routeName = GenService.capitalize(ctx.config.businessName());
        String modulePath = "/" + ctx.config.moduleName();
        return new StringBuilder()
                .append("-- ").append(ctx.title()).append(" 菜单与权限（主表 ").append(ctx.tableName).append("）\n")
                .append("-- 执行后刷新前端或重新登录，菜单才会出现\n")
                .append("USE platform;\n\n")
                .append("INSERT INTO sys_menu (parent_id, name, type, path, route_name, redirect, component, icon, sort, visible, scope, keep_alive, create_time, update_time)\n")
                .append("SELECT 0, '").append(ctx.config.moduleName()).append("', 'C', '")
                .append(modulePath).append("', NULL, NULL, 'Layout', 'folder', 1, 1, 1, NULL, NOW(), NOW()\n")
                .append("  FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE type = 'C' AND path = '")
                .append(modulePath).append("');\n\n")
                .append("SET @parent_id := (SELECT id FROM sys_menu WHERE type = 'C' AND path = '")
                .append(modulePath).append("' LIMIT 1);\n\n")
                .append("INSERT INTO sys_menu (parent_id, name, type, path, route_name, redirect, component, icon, sort, visible, scope, keep_alive, create_time, update_time)\n")
                .append("VALUES (@parent_id, '").append(ctx.title())
                .append("', 'M', '").append(menuPath).append("', '").append(routeName)
                .append("', NULL, '").append(ctx.frontendPath).append("/index', 'document', 1, 1, 1, 1, NOW(), NOW());\n")
                .append("SET @menu_id := LAST_INSERT_ID();\n\n")
                .append("INSERT INTO sys_menu (parent_id, name, type, path, route_name, redirect, component, icon, sort, visible, scope, keep_alive, perm, create_time, update_time)\n")
                .append("VALUES (@menu_id, '查询', 'B', NULL, NULL, NULL, NULL, NULL, 1, 1, 1, NULL, '")
                .append(ctx.perm).append("', NOW(), NOW()),\n")
                .append("       (@menu_id, '新增', 'B', NULL, NULL, NULL, NULL, NULL, 2, 1, 1, NULL, '")
                .append(ctx.permBase()).append(":add', NOW(), NOW()),\n")
                .append("       (@menu_id, '修改', 'B', NULL, NULL, NULL, NULL, NULL, 3, 1, 1, NULL, '")
                .append(ctx.permBase()).append(":edit', NOW(), NOW()),\n")
                .append("       (@menu_id, '删除', 'B', NULL, NULL, NULL, NULL, NULL, 4, 1, 1, NULL, '")
                .append(ctx.permBase()).append(":remove', NOW(), NOW());\n\n")
                .append("INSERT IGNORE INTO sys_role_menu (role_id, menu_id)\n")
                .append("SELECT r.id, @menu_id FROM sys_role r WHERE r.id IN (1, 2);\n")
                .append("INSERT IGNORE INTO sys_role_menu (role_id, menu_id)\n")
                .append("SELECT r.id, m.id FROM sys_role r JOIN sys_menu m ON m.parent_id = @menu_id AND m.type = 'B'\n")
                .append("WHERE r.id IN (1, 2);\n")
                .toString();
    }

    // ------------------------------------------------------------------ 工具

    private GenDtos.GenFileItem javaFile(Ctx ctx, String name, String content) {
        return new GenDtos.GenFileItem(
                "platform-api/src/main/java/" + ctx.pkg.replace('.', '/') + "/" + name, content);
    }

    private static void appendTypeImports(StringBuilder sb, List<String> javaTypes) {
        Set<String> types = new LinkedHashSet<>(javaTypes);
        if (types.contains("BigDecimal")) {
            sb.append("import java.math.BigDecimal;\n");
        }
        if (types.contains("LocalDate")) {
            sb.append("import java.time.LocalDate;\n");
        }
        if (types.contains("LocalDateTime")) {
            sb.append("import java.time.LocalDateTime;\n");
        }
    }

    /** 区间、集合、模糊匹配在查询对象里统一用字符串接收；区间按字段类型生成日期/数值数组 */
    private static String queryFieldType(Col column) {
        return switch (column.filterType()) {
            case "LIKE", "LIKE_LEFT", "LIKE_RIGHT", "IN", "NOT_IN" -> "String";
            case "BETWEEN" -> column.javaType() + "[]";
            default -> column.javaType();
        };
    }

    private static String tsType(String javaType) {
        return switch (javaType) {
            case "Integer", "Long", "Double", "Float", "BigDecimal" -> "number";
            case "Boolean", "boolean" -> "boolean";
            default -> "string";
        };
    }

    private static String decapitalize(String name) {
        return name == null || name.isEmpty()
                ? name : Character.toLowerCase(name.charAt(0)) + name.substring(1);
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

    private static String snake(String camel) {
        StringBuilder sb = new StringBuilder();
        for (char ch : camel.toCharArray()) {
            if (Character.isUpperCase(ch)) {
                sb.append('_').append(Character.toLowerCase(ch));
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    private static String escape(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String text(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private static String relationTitle(String relationType) {
        return switch (relationType) {
            case ONE_TO_MANY -> "一对多";
            case ONE_TO_ONE -> "一对一";
            default -> "多对一";
        };
    }

    private static String normalizeRelation(String relationType) {
        String value = StringUtils.hasText(relationType) ? relationType.trim().toUpperCase(Locale.ROOT) : "";
        return switch (value) {
            case ONE_TO_MANY, ONE_TO_ONE, "MANY_TO_ONE" -> value;
            default -> ONE_TO_ONE;
        };
    }

    private static String normalizeFilter(String filterType) {
        return StringUtils.hasText(filterType) ? filterType.trim().toUpperCase(Locale.ROOT) : "EQ";
    }

    /** 表单元素：优先取配置，缺省时按 Java 类型推导 */
    private static String normalizeComponent(String formComponent, String javaType) {
        if (StringUtils.hasText(formComponent)) {
            return formComponent.trim().toUpperCase(Locale.ROOT);
        }
        return switch (javaType) {
            case "Integer", "Long", "Double", "Float", "BigDecimal", "Short" -> "NUMBER";
            case "LocalDate" -> "DATE";
            case "LocalDateTime" -> "DATETIME";
            case "Boolean" -> "SWITCH";
            default -> "INPUT";
        };
    }

    /** 生成过程使用的列视图 */
    private record Col(String tableName, String columnName, String propertyName, String javaType,
                       String comment, String label, boolean pk, boolean required, boolean list,
                       boolean filter, String filterType, boolean form, String formComponent,
                       String apiProp, boolean dateType) {
    }

    /** 参与生成的表视图 */
    private record TableModel(String tableName, String tableComment, String relationType,
                              String mainColumn, String joinColumn, String alias, String entityName,
                              List<Col> columns) {
    }

    /** 主表实体上的关联属性 */
    private record RelationModel(String tableName, String relationType, String mainColumn,
                                 String joinColumn, String property, String entityName, String title) {
    }

    /** 生成上下文 */
    private static final class Ctx {

        final GenDtos.GenConfigItem config;
        final String tableName;
        final String className;
        final String pkg;
        final String frontendPath;
        final String perm;
        final String urlPath;
        final boolean jdbc;
        final List<TableModel> tables = new ArrayList<>();
        final List<Col> allColumns;
        final List<Col> listColumns;
        final List<Col> filterColumns;
        /** 列表行固定包含主表主键，供编辑/删除定位记录 */
        final List<Col> itemColumns;
        /** 允许排序的字段默认只取主表，避免关联表同名列带来歧义 */
        final List<Col> sortColumns;
        /** 主表全部列 */
        final List<Col> mainColumns;
        /** 主表可编辑列（非主键） */
        final List<Col> editableColumns;
        /** 数值主键按数据库自增处理，不出现在保存表单 */
        final boolean autoIncrementPk;
        final List<RelationModel> relations;
        final Col mainPk;
        final String fromSql;
        final String selectColumns;
        private final Map<String, String> aliasByTable = new LinkedHashMap<>();

        Ctx(GenDtos.GenConfigDetail detail) {
            if (detail == null || detail.tables() == null || detail.tables().isEmpty()) {
                throw new IllegalStateException("生成配置缺少关联表");
            }
            this.config = detail.config();
            this.tableName = config.mainTable();
            this.className = text(config.className(), "Generated");
            this.frontendPath = text(config.frontendPath(), config.moduleName() + "/" + config.businessName());
            this.perm = config.permPrefix();
            this.pkg = config.packageName() + "." + config.moduleName() + "." + config.businessName();
            this.urlPath = "/api/" + config.moduleName() + "/" + kebab(config.businessName());
            this.jdbc = "data-jdbc".equalsIgnoreCase(config.genStyle());

            List<GenDtos.GenTableItem> tableConfigs = detail.tables();
            Set<String> tableNames = new LinkedHashSet<>();
            for (GenDtos.GenTableItem table : tableConfigs) {
                tableNames.add(table.tableName());
            }
            if (!tableNames.contains(tableName)) {
                throw new IllegalStateException("主表未包含在关联表内: " + tableName);
            }

            List<GenDtos.GenColumnItem> source = detail.columns() == null ? List.of() : detail.columns();
            List<Col> columns = new ArrayList<>();
            for (GenDtos.GenColumnItem column : source) {
                if (!tableNames.contains(column.tableName())) {
                    continue;
                }
                String javaType = text(column.javaType(), "String");
                String comment = text(column.columnComment(), column.columnName());
                // 显示列名优先取配置，未配置时回退到 SQL 列注释
                String label = text(column.displayName(), comment);
                columns.add(new Col(column.tableName(), column.columnName(), column.propertyName(), javaType,
                        comment, label, column.isPk(), column.isRequired(),
                        column.isList(), column.isFilter(), normalizeFilter(column.filterType()),
                        column.isForm(), normalizeComponent(column.formComponent(), javaType),
                        GenService.apiProperty(column.tableName(), column.propertyName(), tableName),
                        javaType.equals("LocalDate") || javaType.equals("LocalDateTime")));
            }
            this.allColumns = List.copyOf(columns);
            this.listColumns = allColumns.stream().filter(Col::list).toList();
            this.filterColumns = allColumns.stream().filter(Col::filter).toList();
            List<Col> mainListColumns = listColumns.stream()
                    .filter(column -> tableName.equals(column.tableName())).toList();
            this.sortColumns = mainListColumns.isEmpty() ? listColumns : mainListColumns;
            this.mainPk = allColumns.stream()
                    .filter(column -> column.pk() && tableName.equals(column.tableName()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("主表 " + tableName + " 缺少主键字段"));
            this.autoIncrementPk = switch (mainPk.javaType()) {
                case "Long", "Integer", "Short" -> true;
                default -> false;
            };
            List<Col> items = new ArrayList<>(listColumns);
            if (items.stream().noneMatch(Col::pk)) {
                items.addFirst(mainPk);
            }
            this.itemColumns = List.copyOf(items);
            this.mainColumns = allColumns.stream()
                    .filter(column -> tableName.equals(column.tableName())).toList();
            this.editableColumns = mainColumns.stream().filter(column -> !column.pk()).toList();
            if (editableColumns.isEmpty()) {
                throw new IllegalStateException("主表 " + tableName + " 除主键外没有可编辑字段");
            }

            int aliasIndex = 1;
            for (GenDtos.GenTableItem table : tableConfigs) {
                aliasByTable.put(table.tableName(), tableName.equals(table.tableName()) ? "t" : "t" + aliasIndex++);
            }

            Set<String> usedEntityNames = new LinkedHashSet<>();
            Map<String, String> entityNameByTable = new LinkedHashMap<>();
            for (GenDtos.GenTableItem table : tableConfigs) {
                String candidate = GenService.pascal(GenService.stripPrefix(table.tableName())) + "Entity";
                if (!usedEntityNames.add(candidate)) {
                    candidate = GenService.pascal(table.tableName()) + "Entity";
                    if (!usedEntityNames.add(candidate)) {
                        throw new IllegalStateException("实体类名冲突: " + table.tableName());
                    }
                }
                entityNameByTable.put(table.tableName(), candidate);
            }

            for (GenDtos.GenTableItem table : tableConfigs) {
                String relationType = tableName.equals(table.tableName())
                        ? MAIN : normalizeRelation(table.relationType());
                if (!MAIN.equals(relationType) && (!StringUtils.hasText(table.mainColumn())
                        || !StringUtils.hasText(table.joinColumn()))) {
                    throw new IllegalStateException("关联表 " + table.tableName() + " 未选择关联字段");
                }
                tables.add(new TableModel(table.tableName(), text(table.tableComment(), table.tableName()),
                        relationType, table.mainColumn(), table.joinColumn(), aliasByTable.get(table.tableName()),
                        entityNameByTable.get(table.tableName()),
                        allColumns.stream().filter(column -> column.tableName().equals(table.tableName())).toList()));
            }

            List<RelationModel> relationModels = new ArrayList<>();
            Set<String> usedProps = new LinkedHashSet<>();
            for (Col column : tables.stream().filter(table -> table.tableName().equals(tableName))
                    .findFirst().orElseThrow().columns()) {
                usedProps.add(column.propertyName());
            }
            for (TableModel table : tables) {
                if (table.tableName().equals(tableName)) {
                    continue;
                }
                String shortName = GenService.camel(GenService.stripPrefix(table.tableName()));
                String property = switch (table.relationType()) {
                    case ONE_TO_MANY -> shortName + "Set";
                    case ONE_TO_ONE -> shortName;
                    default -> shortName + "Ref";
                };
                property = unique(property, usedProps);
                relationModels.add(new RelationModel(table.tableName(), table.relationType(),
                        table.mainColumn(), table.joinColumn(), property, table.entityName(),
                        relationTitle(table.relationType())));
            }
            this.relations = List.copyOf(relationModels);

            StringBuilder from = new StringBuilder("FROM ").append(tableName).append(" t");
            for (TableModel table : tables) {
                if (table.tableName().equals(tableName)) {
                    continue;
                }
                from.append(" LEFT JOIN ").append(table.tableName()).append(" ").append(table.alias())
                        .append(" ON ").append(table.alias()).append(".").append(table.joinColumn())
                        .append(" = t.").append(table.mainColumn());
            }
            this.fromSql = from.toString();

            List<String> selectParts = new ArrayList<>();
            for (Col column : itemColumns) {
                selectParts.add(qualified(column) + " AS " + snake(column.apiProp()));
            }
            this.selectColumns = String.join(", ", selectParts);
        }

        String qualified(Col column) {
            return aliasByTable.get(column.tableName()) + "." + column.columnName();
        }

        /** 按钮权限前缀：去掉 permPrefix 末尾的 :list */
        String permBase() {
            return perm.endsWith(":list") ? perm.substring(0, perm.length() - ":list".length()) : perm;
        }

        /** 保存表单字段：勾选表单的主表可编辑列 + 字符串主键（新增时需传入） */
        List<Col> saveFormColumns() {
            List<Col> formColumns = editableColumns.stream().filter(Col::form).toList();
            if (autoIncrementPk) {
                return formColumns;
            }
            List<Col> columns = new ArrayList<>();
            columns.add(mainPk);
            columns.addAll(formColumns);
            return columns;
        }

        String defaultSort() {
            return qualified(mainPk) + " DESC";
        }

        String mainEntityName() {
            return tables.stream().filter(table -> table.tableName().equals(tableName))
                    .findFirst().orElseThrow().entityName();
        }

        String title() {
            return text(config.functionName(), tableName);
        }

        String joinTitle() {
            List<String> joins = new ArrayList<>();
            for (TableModel table : tables) {
                if (!table.tableName().equals(tableName)) {
                    joins.add(table.tableName() + " " + relationTitle(table.relationType()));
                }
            }
            return joins.isEmpty() ? "（单表）" : "，关联表：" + String.join("、", joins);
        }

        private static String unique(String property, Set<String> used) {
            String candidate = property;
            int index = 1;
            while (!used.add(candidate)) {
                candidate = property + index++;
            }
            return candidate;
        }
    }
}
