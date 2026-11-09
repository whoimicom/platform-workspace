package com.platform.api.gen;

import com.platform.api.common.BusinessException;
import com.platform.api.common.query.RecordRowMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 代码生成配置：读取数据库表元数据、生成配置 CRUD、代码预览。
 * 字段类型映射由 {@link DbTypeMappings} 按当前数据源提供，不落库。
 */
@Service
public class GenService {

    private static final String TABLE_SELECT = """
            SELECT id AS id, table_name AS table_name, table_comment AS table_comment,
                   module_name AS module_name, business_name AS business_name,
                   function_name AS function_name, class_name AS class_name,
                   frontend_path AS frontend_path, perm_prefix AS perm_prefix,
                   gen_style AS gen_style, package_name AS package_name, author AS author,
                   DATE_FORMAT(create_time, '%Y-%m-%d %H:%i:%s') AS create_time,
                   DATE_FORMAT(update_time, '%Y-%m-%d %H:%i:%s') AS update_time
              FROM sys_gen_table
            """;

    private static final String COLUMN_SELECT = """
            SELECT id AS id, column_name AS column_name, column_type AS column_type,
                   column_comment AS column_comment, property_name AS property_name,
                   java_type AS java_type, is_pk AS is_pk, is_required AS is_required,
                   is_list AS is_list, is_filter AS is_filter, filter_type AS filter_type
              FROM sys_gen_table_column
             WHERE table_id = :tableId
             ORDER BY sort, id
            """;

    private static final DateTimeFormatter NOW = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final NamedParameterJdbcTemplate jdbc;
    private final DataSource dataSource;
    private final DbTypeMappings dbTypeMappings;
    private final CodeGenerator codeGenerator;

    public GenService(NamedParameterJdbcTemplate jdbc, DataSource dataSource,
                      DbTypeMappings dbTypeMappings, CodeGenerator codeGenerator) {
        this.jdbc = jdbc;
        this.dataSource = dataSource;
        this.dbTypeMappings = dbTypeMappings;
        this.codeGenerator = codeGenerator;
    }

    // ------------------------------------------------------------------ 数据库表

    public Page<GenDtos.DbTableItem> dbTables(GenDtos.GenQuery query) {
        StringBuilder where = new StringBuilder(
                " WHERE table_schema = DATABASE() AND table_name NOT REGEXP '^(sys|gen|qrtz)_'");
        Map<String, Object> params = new HashMap<>();
        if (StringUtils.hasText(query.getKeywords())) {
            where.append(" AND (table_name LIKE :keywords OR table_comment LIKE :keywords)");
            params.put("keywords", "%" + query.getKeywords().trim() + "%");
        }

        long total = jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables" + where,
                params, Long.class);
        int pageNum = query.safePageNum();
        int pageSize = query.safePageSize();
        params.put("limit", pageSize);
        params.put("offset", (long) (pageNum - 1) * pageSize);

        List<DbTableRow> rows = jdbc.query("""
                SELECT table_name AS table_name, table_rows AS table_rows, table_comment AS table_comment
                  FROM information_schema.tables""" + where + " ORDER BY table_name LIMIT :limit OFFSET :offset",
                params, RecordRowMapper.of(DbTableRow.class));

        Set<String> configured = new HashSet<>(jdbc.queryForList(
                "SELECT table_name FROM sys_gen_table", Map.of(), String.class));
        List<GenDtos.DbTableItem> content = rows.stream()
                .map(row -> new GenDtos.DbTableItem(row.tableName(), row.tableRows(), row.tableComment(),
                        configured.contains(row.tableName())))
                .toList();
        return new PageImpl<>(content, PageRequest.of(pageNum - 1, pageSize), total);
    }

    // ------------------------------------------------------------------ 配置 CRUD

    public Page<GenDtos.GenConfigItem> configs(GenDtos.GenQuery query) {
        StringBuilder sql = new StringBuilder(TABLE_SELECT);
        Map<String, Object> params = new HashMap<>();
        if (StringUtils.hasText(query.getKeywords())) {
            sql.append(" WHERE table_name LIKE :keywords OR function_name LIKE :keywords");
            params.put("keywords", "%" + query.getKeywords().trim() + "%");
        }
        long total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM (" + sql + ") count_wrapper", params, Long.class);
        int pageNum = query.safePageNum();
        int pageSize = query.safePageSize();
        params.put("limit", pageSize);
        params.put("offset", (long) (pageNum - 1) * pageSize);
        List<GenDtos.GenConfigItem> content = jdbc.query(
                sql + " ORDER BY id DESC LIMIT :limit OFFSET :offset",
                params, RecordRowMapper.of(GenDtos.GenConfigItem.class));
        return new PageImpl<>(content, PageRequest.of(pageNum - 1, pageSize), total);
    }

    /**
     * 导入数据库表：首次导入写入默认配置与字段，已存在时直接返回现有配置 id。
     */
    @Transactional
    public long importTable(String tableName) {
        if (!StringUtils.hasText(tableName)) {
            throw BusinessException.badRequest("表名不能为空");
        }
        Long exists = jdbc.query("SELECT id FROM sys_gen_table WHERE table_name = :tableName",
                Map.of("tableName", tableName), (rs, rowNum) -> rs.getLong("id")).stream()
                .findFirst().orElse(null);
        if (exists != null) {
            return exists;
        }

        String tableComment = jdbc.query("""
                SELECT table_comment FROM information_schema.tables
                 WHERE table_schema = DATABASE() AND table_name = :tableName
                """, Map.of("tableName", tableName),
                (rs, rowNum) -> rs.getString("table_comment")).stream().findFirst().orElse(null);

        List<ColumnMeta> columns = jdbc.query("""
                SELECT column_name, column_type, column_comment, is_nullable, column_key
                  FROM information_schema.columns
                 WHERE table_schema = DATABASE() AND table_name = :tableName
                 ORDER BY ordinal_position
                """, Map.of("tableName", tableName),
                (rs, rowNum) -> new ColumnMeta(rs.getString("column_name"), rs.getString("column_type"),
                        rs.getString("column_comment"), "YES".equals(rs.getString("is_nullable")),
                        "PRI".equals(rs.getString("column_key"))));
        if (columns.isEmpty()) {
            throw BusinessException.notFound("表不存在: " + tableName);
        }

        DbTypeMappings.Mapping mapping = currentMapping();

        String stripped = stripPrefix(tableName);
        String className = pascal(stripped);
        String businessName = camel(stripped);
        String moduleName = inferModule(tableName);
        String functionName = StringUtils.hasText(tableComment) ? tableComment : tableName;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("tableName", tableName)
                .addValue("tableComment", tableComment)
                .addValue("moduleName", moduleName)
                .addValue("businessName", businessName)
                .addValue("functionName", functionName)
                .addValue("className", className)
                .addValue("frontendPath", moduleName + "/" + businessName)
                .addValue("permPrefix", moduleName + ":" + businessName + ":list")
                .addValue("genStyle", "dynamic-sql")
                .addValue("packageName", "com.platform.api")
                .addValue("author", "codegen")
                .addValue("createTime", nowText())
                .addValue("updateTime", nowText());

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update("""
                INSERT INTO sys_gen_table (table_name, table_comment, module_name, business_name,
                        function_name, class_name, frontend_path, perm_prefix, gen_style,
                        package_name, author, create_time, update_time)
                VALUES (:tableName, :tableComment, :moduleName, :businessName,
                        :functionName, :className, :frontendPath, :permPrefix, :genStyle,
                        :packageName, :author, :createTime, :updateTime)
                """, params, keyHolder, new String[]{"id"});
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("sys_gen_table 主键生成失败");
        }
        long tableId = key.longValue();

        insertColumns(tableId, buildDefaultColumns(columns, mapping));
        return tableId;
    }

    public GenDtos.GenConfigDetail detail(long id) {
        GenDtos.GenConfigItem table = jdbc.query(TABLE_SELECT + " WHERE id = :id",
                Map.of("id", id), RecordRowMapper.of(GenDtos.GenConfigItem.class)).stream()
                .findFirst().orElseThrow(() -> BusinessException.notFound("生成配置不存在"));
        List<GenDtos.GenColumnItem> columns = jdbc.query(COLUMN_SELECT, Map.of("tableId", id),
                RecordRowMapper.of(GenDtos.GenColumnItem.class));
        return new GenDtos.GenConfigDetail(table, columns);
    }

    @Transactional
    public void update(long id, GenDtos.GenConfigForm form) {
        detail(id);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("functionName", form.functionName())
                .addValue("moduleName", form.moduleName())
                .addValue("businessName", form.businessName())
                .addValue("className", form.className())
                .addValue("frontendPath", form.frontendPath())
                .addValue("permPrefix", form.permPrefix())
                .addValue("genStyle", form.genStyle())
                .addValue("packageName", form.packageName())
                .addValue("author", form.author())
                .addValue("updateTime", nowText());
        jdbc.update("""
                UPDATE sys_gen_table
                   SET function_name = :functionName, module_name = :moduleName,
                       business_name = :businessName, class_name = :className,
                       frontend_path = :frontendPath, perm_prefix = :permPrefix,
                       gen_style = :genStyle, package_name = :packageName,
                       author = :author, update_time = :updateTime
                 WHERE id = :id
                """, params);

        jdbc.update("DELETE FROM sys_gen_table_column WHERE table_id = :id", Map.of("id", id));
        List<GenDtos.GenColumnItem> columns = form.columns() == null ? List.of() : form.columns();
        insertColumns(id, columns);
    }

    @Transactional
    public void delete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        jdbc.update("DELETE FROM sys_gen_table_column WHERE table_id IN (:ids)", Map.of("ids", ids));
        jdbc.update("DELETE FROM sys_gen_table WHERE id IN (:ids)", Map.of("ids", ids));
    }

    public List<GenDtos.GenFileItem> preview(long id) {
        return codeGenerator.generate(detail(id));
    }

    // ------------------------------------------------------------------ 内部方法

    /** 按当前数据源的产品名选择类型映射（MySQL / PostgreSQL） */
    private DbTypeMappings.Mapping currentMapping() {
        try (Connection connection = dataSource.getConnection()) {
            return dbTypeMappings.resolve(connection.getMetaData().getDatabaseProductName());
        } catch (SQLException e) {
            throw new IllegalStateException("读取数据库类型失败", e);
        }
    }

    private void insertColumns(long tableId, List<GenDtos.GenColumnItem> columns) {
        if (columns.isEmpty()) {
            return;
        }
        List<MapSqlParameterSource> batch = new ArrayList<>();
        for (int i = 0; i < columns.size(); i++) {
            GenDtos.GenColumnItem column = columns.get(i);
            batch.add(new MapSqlParameterSource()
                    .addValue("tableId", tableId)
                    .addValue("sort", i + 1)
                    .addValue("columnName", column.columnName())
                    .addValue("columnType", column.columnType())
                    .addValue("columnComment", column.columnComment())
                    .addValue("propertyName", column.propertyName())
                    .addValue("javaType", column.javaType() == null ? "String" : column.javaType())
                    .addValue("isPk", flag(column.isPk()))
                    .addValue("isRequired", flag(column.isRequired()))
                    .addValue("isList", flag(column.isList()))
                    .addValue("isFilter", flag(column.isFilter()))
                    .addValue("filterType", column.filterType() == null ? "EQ" : column.filterType()));
        }
        jdbc.batchUpdate("""
                INSERT INTO sys_gen_table_column (table_id, sort, column_name, column_type, column_comment,
                        property_name, java_type, is_pk, is_required, is_list, is_filter, filter_type)
                VALUES (:tableId, :sort, :columnName, :columnType, :columnComment,
                        :propertyName, :javaType, :isPk, :isRequired, :isList, :isFilter, :filterType)
                """, batch.toArray(MapSqlParameterSource[]::new));
    }

    /**
     * 导入时的默认规则：日期列做区间筛选，状态/类型列做等值筛选，名称/手机/编号等做模糊筛选，最多 6 个条件。
     */
    private List<GenDtos.GenColumnItem> buildDefaultColumns(List<ColumnMeta> columns,
                                                            DbTypeMappings.Mapping mapping) {
        List<GenDtos.GenColumnItem> result = new ArrayList<>();
        int filterCount = 0;
        for (ColumnMeta meta : columns) {
            String baseType = mapping.baseType(meta.type());
            String javaType = mapping.javaType(meta.type());
            String property = camel(meta.name());
            boolean dateType = javaType.equals("LocalDate") || javaType.equals("LocalDateTime");
            boolean textType = baseType.equals("text") || baseType.equals("mediumtext")
                    || baseType.equals("longtext") || baseType.equals("json")
                    || baseType.equals("jsonb") || baseType.equals("bytea");

            boolean isFilter = false;
            String filterType = "EQ";
            String lower = property.toLowerCase(Locale.ROOT);
            if (!meta.pk() && filterCount < 6) {
                if (dateType) {
                    isFilter = true;
                    filterType = "BETWEEN";
                } else if (lower.endsWith("status") || lower.endsWith("type") || lower.equals("tenant")) {
                    isFilter = true;
                    filterType = "EQ";
                } else if ((baseType.equals("varchar") || baseType.equals("char")
                        || baseType.equals("character") || baseType.equals("character varying"))
                        && (lower.contains("name") || lower.contains("mobile") || lower.contains("phone")
                        || lower.contains("serial") || lower.endsWith("no") || lower.endsWith("code")) ) {
                    isFilter = true;
                    filterType = "LIKE";
                }
            }
            if (isFilter) {
                filterCount++;
            }

            result.add(new GenDtos.GenColumnItem(0L, meta.name(), meta.type(), meta.comment(), property,
                    javaType, meta.pk(), !meta.pk() && !meta.nullable(), !textType, isFilter, filterType));
        }
        return result;
    }

    private static int flag(Boolean value) {
        return Boolean.TRUE.equals(value) ? 1 : 0;
    }

    private static String nowText() {
        return LocalDateTime.now().format(NOW);
    }

    /** zl_order → order */
    private static String stripPrefix(String tableName) {
        int index = tableName.indexOf('_');
        return index >= 0 && index < tableName.length() - 1 ? tableName.substring(index + 1) : tableName;
    }

    private static String inferModule(String tableName) {
        if (tableName.startsWith("zl_")) {
            return "rental";
        }
        if (tableName.startsWith("sys_")) {
            return "system";
        }
        return "business";
    }

    public static String camel(String name) {
        String pascalName = pascal(name);
        return pascalName.isEmpty() ? pascalName
                : Character.toLowerCase(pascalName.charAt(0)) + pascalName.substring(1);
    }

    public static String pascal(String name) {
        StringBuilder result = new StringBuilder();
        for (String part : name.split("[-_]")) {
            if (part.isEmpty()) {
                continue;
            }
            result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return result.toString();
    }

    private record ColumnMeta(String name, String type, String comment, boolean nullable, boolean pk) {
    }

    /** information_schema.tables 行（不含 configured，需与集合比对） */
    private record DbTableRow(String tableName, Long tableRows, String tableComment) {
    }
}
