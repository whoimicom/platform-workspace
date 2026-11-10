package com.platform.api.gen;

import com.platform.api.common.BusinessException;
import com.platform.api.common.query.RecordRowMapper;
import org.springframework.beans.factory.annotation.Value;
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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 代码生成配置：库内表元数据、多表配置 CRUD、代码预览。
 *
 * <p>字段类型映射来自 {@link DbTypeMappings}（按当前数据源读取 YAML 配置），不落库。
 */
@Service
public class GenService {

    /** 主表 */
    public static final String MAIN = "MAIN";

    /** 其它表可选的关联方式 */
    private static final Set<String> RELATION_TYPES = Set.of("ONE_TO_ONE", "ONE_TO_MANY", "MANY_TO_ONE");

    /** 支持的表单元素 */
    private static final Set<String> FORM_COMPONENTS = Set.of(
            "INPUT", "TEXTAREA", "NUMBER", "DATE", "DATETIME", "SWITCH", "SELECT");

    /** 支持的筛选方式，与 {@code Op} 保持一致 */
    private static final Set<String> FILTER_TYPES = Set.of(
            "EQ", "NE", "GT", "GE", "LT", "LE",
            "LIKE", "LIKE_LEFT", "LIKE_RIGHT",
            "IN", "NOT_IN", "BETWEEN");

    /** 默认最多带出的查询条件数量 */
    private static final int DEFAULT_FILTER_LIMIT = 5;

    /** 默认开启列表展示的字段数量（主表前 N 个字段） */
    private static final int DEFAULT_LIST_LIMIT = 5;

    private static final String CONFIG_SELECT = """
            SELECT id AS id, config_name AS config_name, main_table AS main_table,
                   function_name AS function_name, module_name AS module_name,
                   business_name AS business_name, class_name AS class_name,
                   frontend_path AS frontend_path, perm_prefix AS perm_prefix,
                   gen_style AS gen_style, package_name AS package_name, author AS author,
                   DATE_FORMAT(create_time, '%Y-%m-%d %H:%i:%s') AS create_time,
                   DATE_FORMAT(update_time, '%Y-%m-%d %H:%i:%s') AS update_time
              FROM sys_gen_config
            """;

    private static final String TABLE_SELECT = """
            SELECT id AS id, table_name AS table_name, table_comment AS table_comment,
                   table_alias AS table_alias, relation_type AS relation_type,
                   main_column AS main_column, join_column AS join_column, sort AS sort
              FROM sys_gen_config_table
             WHERE config_id = :configId
             ORDER BY sort, id
            """;

    private static final String COLUMN_SELECT = """
            SELECT id AS id, table_name AS table_name, column_name AS column_name,
                   column_type AS column_type, column_comment AS column_comment,
                   display_name AS display_name, property_name AS property_name, java_type AS java_type,
                   is_pk AS is_pk, is_required AS is_required, is_list AS is_list,
                   is_filter AS is_filter, filter_type AS filter_type,
                   is_form AS is_form, form_component AS form_component, sort AS sort
              FROM sys_gen_config_column
             WHERE config_id = :configId
             ORDER BY sort, id
            """;

    private static final DateTimeFormatter NOW = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final NamedParameterJdbcTemplate jdbc;
    private final DataSource dataSource;
    private final DbTypeMappings dbTypeMappings;
    private final CodeGenerator codeGenerator;

    /** 代码输出根目录（包含 platform-api / platform-web 的工作区根），默认按启动目录推导 */
    @Value("${app.gen.output-root:}")
    private String outputRoot;

    public GenService(NamedParameterJdbcTemplate jdbc, DataSource dataSource,
                      DbTypeMappings dbTypeMappings, CodeGenerator codeGenerator) {
        this.jdbc = jdbc;
        this.dataSource = dataSource;
        this.dbTypeMappings = dbTypeMappings;
        this.codeGenerator = codeGenerator;
    }

    public void setOutputRoot(String outputRoot) {
        this.outputRoot = outputRoot;
    }

    // ------------------------------------------------------------------ 库内表

    public Page<GenDtos.DbTableItem> dbTables(GenDtos.GenQuery query) {
        StringBuilder where = new StringBuilder("""
                 WHERE table_schema = DATABASE() AND table_type = 'BASE TABLE'
                   AND table_name NOT REGEXP '^(sys|gen|qrtz)_'
                """);
        Map<String, Object> params = new HashMap<>();
        if (StringUtils.hasText(query.getKeywords())) {
            where.append(" AND (table_name LIKE :keywords OR table_comment LIKE :keywords)");
            params.put("keywords", "%" + query.getKeywords().trim() + "%");
        }

        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables" + where,
                params, Long.class);
        long totalElements = total == null ? 0L : total;

        int pageNum = query.safePageNum();
        int pageSize = query.safePageSize();
        params.put("limit", pageSize);
        params.put("offset", (long) (pageNum - 1) * pageSize);
        List<DbTableRow> rows = jdbc.query("""
                SELECT table_name AS table_name, table_rows AS table_rows, table_comment AS table_comment
                  FROM information_schema.tables""" + where + " ORDER BY table_name LIMIT :limit OFFSET :offset",
                params, RecordRowMapper.of(DbTableRow.class));

        Set<String> configured = new HashSet<>(jdbc.queryForList(
                "SELECT main_table FROM sys_gen_config", Map.of(), String.class));
        List<GenDtos.DbTableItem> content = rows.stream()
                .map(row -> new GenDtos.DbTableItem(row.tableName(), row.tableRows(), row.tableComment(),
                        configured.contains(row.tableName())))
                .toList();
        return new PageImpl<>(content, PageRequest.of(pageNum - 1, pageSize), totalElements);
    }

    /** 指定表的字段列表，供前端选择关联列 */
    public List<GenDtos.DbColumnItem> dbColumns(String tableName) {
        DbTypeMappings.Mapping mapping = currentMapping();
        return tableColumns(tableName).stream()
                .map(meta -> new GenDtos.DbColumnItem(meta.name(), meta.type(), meta.comment(),
                        camel(meta.name()), mapping.javaType(meta.type()), meta.pk(), meta.nullable()))
                .toList();
    }

    // ------------------------------------------------------------------ 配置

    public Page<GenDtos.GenConfigItem> configs(GenDtos.GenQuery query) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        Map<String, Object> params = new HashMap<>();
        if (StringUtils.hasText(query.getKeywords())) {
            where.append(" AND (main_table LIKE :keywords OR function_name LIKE :keywords"
                    + " OR config_name LIKE :keywords)");
            params.put("keywords", "%" + query.getKeywords().trim() + "%");
        }
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM sys_gen_config" + where, params, Long.class);
        long totalElements = total == null ? 0L : total;

        int pageNum = query.safePageNum();
        int pageSize = query.safePageSize();
        params.put("limit", pageSize);
        params.put("offset", (long) (pageNum - 1) * pageSize);
        List<ConfigRow> rows = jdbc.query(CONFIG_SELECT + where + " ORDER BY id DESC LIMIT :limit OFFSET :offset",
                params, RecordRowMapper.of(ConfigRow.class));

        List<Long> ids = rows.stream().map(ConfigRow::id).toList();
        Map<Long, String> tableLists = tableLists(ids);
        List<GenDtos.GenConfigItem> content = rows.stream()
                .map(row -> toItem(row, tableLists.getOrDefault(row.id(), row.mainTable())))
                .toList();
        return new PageImpl<>(content, PageRequest.of(pageNum - 1, pageSize), totalElements);
    }

    public GenDtos.GenConfigDetail detail(long id) {
        ConfigRow row = requireConfig(id);
        Map<Long, String> tableLists = tableLists(List.of(id));
        List<GenDtos.GenTableItem> tables = jdbc.query(TABLE_SELECT, Map.of("configId", id),
                RecordRowMapper.of(GenDtos.GenTableItem.class));
        List<GenDtos.GenColumnItem> columns = jdbc.query(COLUMN_SELECT, Map.of("configId", id),
                RecordRowMapper.of(GenDtos.GenColumnItem.class));
        // 读取时按当前库结构过滤，表/字段已删除或重命名的旧配置直接忽略，不影响打开与预览
        // 主表固定排第一，其余按配置顺序
        List<GenDtos.GenTableItem> ordered = tables.stream()
                .sorted(Comparator.comparing((GenDtos.GenTableItem table) -> !table.tableName().equals(row.mainTable()))
                        .thenComparingInt(GenDtos.GenTableItem::sort))
                .toList();
        ValidateResult valid = filterBySchema(row.mainTable(), ordered, columns);
        return new GenDtos.GenConfigDetail(
                toItem(row, tableLists.getOrDefault(id, row.mainTable())), valid.tables(), valid.columns());
    }

    /**
     * 导入多张表：首次导入按表结构生成默认规则，重复导入返回已有配置。
     */
    @Transactional
    public long importConfig(GenDtos.GenImportForm form) {
        List<String> tableNames = distinctTables(form);
        String mainTable = form.mainTable().trim();
        if (!tableNames.contains(mainTable)) {
            throw BusinessException.badRequest("主表必须包含在选择的表内");
        }

        Long exists = jdbc.query("SELECT id FROM sys_gen_config WHERE main_table = :mainTable",
                Map.of("mainTable", mainTable), rs -> rs.next() ? rs.getLong(1) : null);
        if (exists != null) {
            mergeTables(exists, mainTable, tableNames);
            return exists;
        }

        DbTypeMappings.Mapping mapping = currentMapping();
        Map<String, List<ColumnMeta>> columnsByTable = new LinkedHashMap<>();
        Map<String, String> commentByTable = new LinkedHashMap<>();
        for (String tableName : tableNames) {
            columnsByTable.put(tableName, tableColumns(tableName));
            commentByTable.put(tableName, tableComment(tableName));
        }
        List<ColumnMeta> mainColumns = columnsByTable.get(mainTable);
        if (mainColumns.isEmpty()) {
            throw BusinessException.badRequest("表 " + mainTable + " 没有字段");
        }

        String businessName = camel(stripPrefix(mainTable));
        String className = capitalize(businessName);
        String functionName = StringUtils.hasText(commentByTable.get(mainTable))
                ? commentByTable.get(mainTable) : businessName;
        String moduleName = inferModule(mainTable);
        String now = LocalDateTime.now().format(NOW);

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("configName", mainTable)
                .addValue("mainTable", mainTable)
                .addValue("functionName", functionName)
                .addValue("moduleName", moduleName)
                .addValue("businessName", businessName)
                .addValue("className", className)
                .addValue("frontendPath", moduleName + "/" + businessName)
                .addValue("permPrefix", moduleName + ":" + businessName + ":list")
                .addValue("genStyle", "dynamic-sql")
                .addValue("packageName", "com.platform.api")
                .addValue("author", "")
                .addValue("createTime", now)
                .addValue("updateTime", now);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update("""
                INSERT INTO sys_gen_config (config_name, main_table, function_name, module_name, business_name,
                        class_name, frontend_path, perm_prefix, gen_style, package_name, author,
                        create_time, update_time)
                VALUES (:configName, :mainTable, :functionName, :moduleName, :businessName,
                        :className, :frontendPath, :permPrefix, :genStyle, :packageName, :author,
                        :createTime, :updateTime)
                """, params, keyHolder, new String[]{"id"});
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("sys_gen_config 主键生成失败");
        }
        long configId = key.longValue();

        List<GenDtos.GenTableItem> tables = new ArrayList<>();
        for (int i = 0; i < tableNames.size(); i++) {
            String tableName = tableNames.get(i);
            boolean main = tableName.equals(mainTable);
            Relation relation = main ? new Relation(MAIN, null, null)
                    : guessRelation(mainTable, mainColumns, tableName, columnsByTable.get(tableName));
            tables.add(new GenDtos.GenTableItem(0L, tableName, commentByTable.get(tableName),
                    main ? "t" : "t" + (i), relation.type(), relation.mainColumn(), relation.joinColumn(), i));
        }
        insertTables(configId, tables);

        List<GenDtos.GenColumnItem> columns = new ArrayList<>();
        int sort = 0;
        int filterCount = 0;
        for (String tableName : tableNames) {
            boolean main = tableName.equals(mainTable);
            List<ColumnMeta> metas = columnsByTable.get(tableName);
            for (int i = 0; i < metas.size(); i++) {
                ColumnMeta meta = metas.get(i);
                DefaultRule rule = defaultRule(meta, mapping, main && filterCount < DEFAULT_FILTER_LIMIT,
                        main && i < DEFAULT_LIST_LIMIT);
                if (rule.filter()) {
                    filterCount++;
                }
                String javaType = mapping.javaType(meta.type());
                columns.add(new GenDtos.GenColumnItem(0L, tableName, meta.name(), meta.type(), meta.comment(),
                        null, camel(meta.name()), javaType, meta.pk(),
                        !meta.pk() && !meta.nullable(), rule.list(), rule.filter(), rule.filterType(),
                        main && !meta.pk(), defaultFormComponent(javaType, meta.type()), sort++));
            }
        }
        insertColumns(configId, columns);
        return configId;
    }

    /**
     * 重复导入时把新选择的表并入已有配置，已存在的表保持原规则不动。
     */
    private void mergeTables(long configId, String mainTable, List<String> tableNames) {
        List<GenDtos.GenTableItem> existing = jdbc.query(TABLE_SELECT, Map.of("configId", configId),
                RecordRowMapper.of(GenDtos.GenTableItem.class));
        Set<String> existingNames = existing.stream()
                .map(GenDtos.GenTableItem::tableName)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<String> missing = tableNames.stream().filter(name -> !existingNames.contains(name)).toList();
        if (missing.isEmpty()) {
            return;
        }

        DbTypeMappings.Mapping mapping = currentMapping();
        List<ColumnMeta> mainColumns = tableColumns(mainTable);
        int tableSort = existing.stream().mapToInt(GenDtos.GenTableItem::sort).max().orElse(-1) + 1;
        Long maxColumnSort = jdbc.queryForObject(
                "SELECT COALESCE(MAX(sort), -1) FROM sys_gen_config_column WHERE config_id = :configId",
                Map.of("configId", configId), Long.class);
        int columnSort = maxColumnSort == null ? 0 : maxColumnSort.intValue() + 1;

        List<GenDtos.GenTableItem> tables = new ArrayList<>();
        List<GenDtos.GenColumnItem> columns = new ArrayList<>();
        int aliasIndex = existingNames.size();
        for (String tableName : missing) {
            List<ColumnMeta> tableColumns = tableColumns(tableName);
            Relation relation = guessRelation(mainTable, mainColumns, tableName, tableColumns);
            tables.add(new GenDtos.GenTableItem(0L, tableName, tableComment(tableName),
                    "t" + aliasIndex++, relation.type(), relation.mainColumn(), relation.joinColumn(), tableSort++));
            for (ColumnMeta meta : tableColumns) {
                DefaultRule rule = defaultRule(meta, mapping, false, false);
                columns.add(new GenDtos.GenColumnItem(0L, tableName, meta.name(), meta.type(), meta.comment(),
                        null, camel(meta.name()), mapping.javaType(meta.type()), meta.pk(),
                        !meta.pk() && !meta.nullable(), rule.list(), rule.filter(), rule.filterType(),
                        false, defaultFormComponent(mapping.javaType(meta.type()), meta.type()), columnSort++));
            }
        }
        insertTables(configId, tables);
        insertColumns(configId, columns);
    }

    @Transactional
    public void update(long id, GenDtos.GenConfigForm form) {
        ConfigRow row = requireConfig(id);
        ValidateResult valid = validate(row.mainTable(), form);

        String now = LocalDateTime.now().format(NOW);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("configName", text(form.configName(), row.mainTable()))
                .addValue("functionName", form.functionName())
                .addValue("moduleName", form.moduleName())
                .addValue("businessName", form.businessName())
                .addValue("className", form.className())
                .addValue("frontendPath", form.frontendPath())
                .addValue("permPrefix", form.permPrefix())
                .addValue("genStyle", form.genStyle())
                .addValue("packageName", form.packageName())
                .addValue("author", form.author())
                .addValue("updateTime", now);
        jdbc.update("""
                UPDATE sys_gen_config
                   SET config_name = :configName, function_name = :functionName, module_name = :moduleName,
                       business_name = :businessName, class_name = :className, frontend_path = :frontendPath,
                       perm_prefix = :permPrefix, gen_style = :genStyle, package_name = :packageName,
                       author = :author, update_time = :updateTime
                 WHERE id = :id
                """, params);

        jdbc.update("DELETE FROM sys_gen_config_table WHERE config_id = :id", Map.of("id", id));
        jdbc.update("DELETE FROM sys_gen_config_column WHERE config_id = :id", Map.of("id", id));
        insertTables(id, valid.tables());
        insertColumns(id, valid.columns());
    }

    @Transactional
    public void delete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        jdbc.update("DELETE FROM sys_gen_config_table WHERE config_id IN (:ids)", Map.of("ids", ids));
        jdbc.update("DELETE FROM sys_gen_config_column WHERE config_id IN (:ids)", Map.of("ids", ids));
        jdbc.update("DELETE FROM sys_gen_config WHERE id IN (:ids)", Map.of("ids", ids));
    }

    public List<GenDtos.GenFileItem> preview(long id) {
        return codeGenerator.generate(detail(id));
    }

    /**
     * 生成代码并直接写入项目目录（覆盖同路径文件），返回写入的文件相对路径。
     */
    public List<String> generateToProject(long id) {
        List<GenDtos.GenFileItem> files = preview(id);
        Path root = outputRoot();
        List<String> written = new ArrayList<>();
        for (GenDtos.GenFileItem file : files) {
            Path target = root.resolve(file.path()).normalize();
            if (!target.startsWith(root)) {
                throw BusinessException.badRequest("非法的输出路径: " + file.path());
            }
            try {
                Files.createDirectories(target.getParent());
                Files.writeString(target, file.content(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new IllegalStateException("写入文件失败: " + file.path(), e);
            }
            written.add(file.path());
        }
        return written;
    }

    private Path outputRoot() {
        if (StringUtils.hasText(outputRoot)) {
            return Path.of(outputRoot).toAbsolutePath().normalize();
        }
        Path dir = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        // 从模块目录启动时，工作区根是其上级
        if (dir.getFileName() != null && "platform-api".equals(dir.getFileName().toString())) {
            return dir.getParent();
        }
        return dir;
    }

    // ------------------------------------------------------------------ 内部：元数据

    private DbTypeMappings.Mapping currentMapping() {
        try (Connection connection = dataSource.getConnection()) {
            return dbTypeMappings.resolve(connection.getMetaData().getDatabaseProductName());
        } catch (SQLException e) {
            throw new IllegalStateException("读取数据库类型失败", e);
        }
    }

    private List<ColumnMeta> tableColumns(String tableName) {
        List<ColumnMeta> columns = metaColumns(tableName);
        if (columns.isEmpty()) {
            throw BusinessException.notFound("表不存在或没有字段: " + tableName);
        }
        return columns;
    }

    /** 表字段元数据；表不存在时返回空列表（用于旧配置兼容，不抛异常） */
    private List<ColumnMeta> metaColumns(String tableName) {
        return jdbc.query("""
                SELECT column_name AS column_name, column_type AS column_type,
                       column_comment AS column_comment, is_nullable AS is_nullable,
                       column_key AS column_key
                  FROM information_schema.columns
                 WHERE table_schema = DATABASE() AND table_name = :tableName
                 ORDER BY ordinal_position
                """, Map.of("tableName", tableName), (rs, rowNum) -> new ColumnMeta(
                rs.getString("column_name"),
                rs.getString("column_type"),
                rs.getString("column_comment"),
                "YES".equalsIgnoreCase(rs.getString("is_nullable")),
                "PRI".equalsIgnoreCase(rs.getString("column_key"))));
    }

    /** 当前库内存在的表名 */
    private Set<String> existingTables(Set<String> tableNames) {
        if (tableNames.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(jdbc.queryForList("""
                SELECT table_name FROM information_schema.tables
                 WHERE table_schema = DATABASE() AND table_name IN (:names)
                """, Map.of("names", tableNames), String.class));
    }

    /**
     * 按当前库结构过滤配置：已删除/重命名的表与字段直接忽略，同时同步最新的列类型与注释，
     * 让历史配置在表结构变化后仍可正常打开、编辑与生成。
     */
    private ValidateResult filterBySchema(String mainTable, List<GenDtos.GenTableItem> tables,
                                          List<GenDtos.GenColumnItem> columns) {
        Set<String> names = tables.stream()
                .map(GenDtos.GenTableItem::tableName)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> existing = existingTables(names);
        List<GenDtos.GenTableItem> keptTables = tables.stream()
                .filter(table -> table.tableName().equals(mainTable) || existing.contains(table.tableName()))
                .toList();
        Set<String> keptNames = keptTables.stream()
                .map(GenDtos.GenTableItem::tableName)
                .collect(Collectors.toSet());

        Map<String, Map<String, ColumnMeta>> schema = new HashMap<>();
        for (String name : keptNames) {
            Map<String, ColumnMeta> metas = new HashMap<>();
            for (ColumnMeta meta : metaColumns(name)) {
                metas.put(meta.name().toLowerCase(Locale.ROOT), meta);
            }
            schema.put(name, metas);
        }

        List<GenDtos.GenColumnItem> keptColumns = new ArrayList<>();
        for (GenDtos.GenColumnItem column : columns) {
            if (!keptNames.contains(column.tableName())) {
                continue;
            }
            ColumnMeta meta = schema.get(column.tableName())
                    .get(column.columnName().toLowerCase(Locale.ROOT));
            if (meta == null) {
                continue;
            }
            keptColumns.add(new GenDtos.GenColumnItem(column.id(), column.tableName(), column.columnName(),
                    meta.type(), meta.comment(), column.displayName(), column.propertyName(),
                    text(column.javaType(), "String"), column.isPk(), column.isRequired(), column.isList(),
                    column.isFilter(), column.filterType(), column.isForm(), column.formComponent(),
                    column.sort()));
        }
        return new ValidateResult(List.copyOf(keptTables), List.copyOf(keptColumns));
    }

    private String tableComment(String tableName) {
        List<String> comments = jdbc.query("""
                SELECT table_comment FROM information_schema.tables
                 WHERE table_schema = DATABASE() AND table_name = :tableName
                """, Map.of("tableName", tableName), (rs, rowNum) -> rs.getString(1));
        return comments.isEmpty() ? "" : comments.getFirst();
    }

    // ------------------------------------------------------------------ 内部：配置读写

    private ConfigRow requireConfig(long id) {
        List<ConfigRow> rows = jdbc.query(CONFIG_SELECT + " WHERE id = :id", Map.of("id", id),
                RecordRowMapper.of(ConfigRow.class));
        if (rows.isEmpty()) {
            throw BusinessException.notFound("生成配置不存在: " + id);
        }
        return rows.getFirst();
    }

    private Map<Long, String> tableLists(List<Long> configIds) {
        if (configIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> result = new HashMap<>();
        jdbc.query("""
                SELECT config_id, GROUP_CONCAT(table_name ORDER BY sort SEPARATOR ',') AS table_list
                  FROM sys_gen_config_table WHERE config_id IN (:ids) GROUP BY config_id
                """, Map.of("ids", configIds), rs -> {
            result.put(rs.getLong("config_id"), rs.getString("table_list"));
        });
        return result;
    }

    private void insertTables(long configId, List<GenDtos.GenTableItem> tables) {
        List<MapSqlParameterSource> batch = new ArrayList<>();
        for (int i = 0; i < tables.size(); i++) {
            GenDtos.GenTableItem table = tables.get(i);
            batch.add(new MapSqlParameterSource()
                    .addValue("configId", configId)
                    .addValue("tableName", table.tableName())
                    .addValue("tableComment", table.tableComment())
                    .addValue("tableAlias", StringUtils.hasText(table.tableAlias()) ? table.tableAlias() : "t" + i)
                    .addValue("relationType", table.relationType())
                    .addValue("mainColumn", table.mainColumn())
                    .addValue("joinColumn", table.joinColumn())
                    .addValue("sort", table.sort()));
        }
        jdbc.batchUpdate("""
                INSERT INTO sys_gen_config_table (config_id, table_name, table_comment, table_alias,
                        relation_type, main_column, join_column, sort)
                VALUES (:configId, :tableName, :tableComment, :tableAlias,
                        :relationType, :mainColumn, :joinColumn, :sort)
                """, batch.toArray(MapSqlParameterSource[]::new));
    }

    private void insertColumns(long configId, List<GenDtos.GenColumnItem> columns) {
        List<MapSqlParameterSource> batch = new ArrayList<>();
        for (int i = 0; i < columns.size(); i++) {
            GenDtos.GenColumnItem column = columns.get(i);
            batch.add(new MapSqlParameterSource()
                    .addValue("configId", configId)
                    .addValue("tableName", column.tableName())
                    .addValue("columnName", column.columnName())
                    .addValue("columnType", column.columnType())
                    .addValue("columnComment", column.columnComment())
                    .addValue("displayName", StringUtils.hasText(column.displayName())
                            ? column.displayName().trim() : null)
                    .addValue("propertyName", column.propertyName())
                    .addValue("javaType", text(column.javaType(), "String"))
                    .addValue("isPk", column.isPk())
                    .addValue("isRequired", column.isRequired())
                    .addValue("isList", column.isList())
                    .addValue("isFilter", column.isFilter())
                    .addValue("filterType", normalizeFilter(column.filterType()))
                    .addValue("isForm", column.isForm())
                    .addValue("formComponent", normalizeFormComponent(column.formComponent()))
                    .addValue("sort", column.sort()));
        }
        jdbc.batchUpdate("""
                INSERT INTO sys_gen_config_column (config_id, table_name, column_name, column_type,
                        column_comment, display_name, property_name, java_type, is_pk, is_required,
                        is_list, is_filter, filter_type, is_form, form_component, sort)
                VALUES (:configId, :tableName, :columnName, :columnType,
                        :columnComment, :displayName, :propertyName, :javaType, :isPk, :isRequired,
                        :isList, :isFilter, :filterType, :isForm, :formComponent, :sort)
                """, batch.toArray(MapSqlParameterSource[]::new));
    }

    // ------------------------------------------------------------------ 内部：校验与默认值

    private List<String> distinctTables(GenDtos.GenImportForm form) {
        if (form == null || form.tableNames() == null || form.tableNames().isEmpty()) {
            throw BusinessException.badRequest("请至少选择一张表");
        }
        if (!StringUtils.hasText(form.mainTable())) {
            throw BusinessException.badRequest("请选择主表");
        }
        Set<String> names = new LinkedHashSet<>();
        for (String name : form.tableNames()) {
            if (StringUtils.hasText(name)) {
                names.add(name.trim());
            }
        }
        if (names.isEmpty()) {
            throw BusinessException.badRequest("请至少选择一张表");
        }
        return new ArrayList<>(names);
    }

    /**
     * 校验保存表单：主表唯一、关联列完整、至少一个列表字段、属性名不冲突。
     *
     * <p>库内已删除或重命名的表/字段会被忽略，历史配置仍可正常保存。
     */
    private ValidateResult validate(String mainTable, GenDtos.GenConfigForm form) {
        if (form == null || form.tables() == null || form.tables().isEmpty()) {
            throw BusinessException.badRequest("关联表不能为空");
        }
        if (!StringUtils.hasText(form.functionName()) || !StringUtils.hasText(form.moduleName())
                || !StringUtils.hasText(form.businessName()) || !StringUtils.hasText(form.className())
                || !StringUtils.hasText(form.frontendPath()) || !StringUtils.hasText(form.permPrefix())
                || !StringUtils.hasText(form.packageName())) {
            throw BusinessException.badRequest("配置名称、类名、路径等基础信息不能为空");
        }

        List<GenDtos.GenTableItem> tables = new ArrayList<>();
        Set<String> tableNames = new LinkedHashSet<>();
        int mainCount = 0;
        for (int i = 0; i < form.tables().size(); i++) {
            GenDtos.GenTableItem table = form.tables().get(i);
            if (!StringUtils.hasText(table.tableName())) {
                throw BusinessException.badRequest("关联表名不能为空");
            }
            String relation = StringUtils.hasText(table.relationType())
                    ? table.relationType().trim().toUpperCase(Locale.ROOT) : MAIN;
            boolean main = table.tableName().equals(mainTable);
            if (main) {
                relation = MAIN;
                mainCount++;
            } else if (!RELATION_TYPES.contains(relation)) {
                throw BusinessException.badRequest("关联方式不支持: " + table.relationType());
            }
            if (!main && (!StringUtils.hasText(table.mainColumn()) || !StringUtils.hasText(table.joinColumn()))) {
                throw BusinessException.badRequest("关联表 " + table.tableName() + " 需要选择关联字段");
            }
            if (!tableNames.add(table.tableName())) {
                throw BusinessException.badRequest("关联表重复: " + table.tableName());
            }
            tables.add(new GenDtos.GenTableItem(0L, table.tableName(), table.tableComment(),
                    StringUtils.hasText(table.tableAlias()) ? table.tableAlias() : (main ? "t" : "t" + i),
                    relation, main ? null : table.mainColumn(), main ? null : table.joinColumn(), i));
        }
        if (mainCount != 1) {
            throw BusinessException.badRequest("必须且只能指定一张主表");
        }

        if (form.columns() == null || form.columns().isEmpty()) {
            throw BusinessException.badRequest("字段配置不能为空");
        }
        List<GenDtos.GenColumnItem> submitted = new ArrayList<>();
        for (int i = 0; i < form.columns().size(); i++) {
            GenDtos.GenColumnItem column = form.columns().get(i);
            if (!tableNames.contains(column.tableName())) {
                continue;
            }
            submitted.add(new GenDtos.GenColumnItem(column.id(), column.tableName(), column.columnName(),
                    column.columnType(), column.columnComment(), column.displayName(), column.propertyName(),
                    text(column.javaType(), "String"), column.isPk(), column.isRequired(), column.isList(),
                    column.isFilter(), normalizeFilter(column.filterType()),
                    column.isForm() && mainTable.equals(column.tableName()) && !column.isPk(),
                    column.formComponent(), i));
        }

        // 兼容性：过滤掉库内已不存在的表与字段（删除或重命名），旧配置不报错
        ValidateResult filtered = filterBySchema(mainTable, tables, submitted);
        List<GenDtos.GenColumnItem> columns = new ArrayList<>(filtered.columns());
        if (columns.isEmpty()) {
            throw BusinessException.badRequest("字段配置为空或字段均已不存在，请重新导入表结构");
        }

        Map<String, String> apiProps = new HashMap<>();
        boolean hasList = false;
        for (GenDtos.GenColumnItem column : columns) {
            hasList |= column.isList();
            if (column.isList() || column.isFilter()) {
                String apiProp = apiProperty(column.tableName(), column.propertyName(), mainTable);
                String previous = apiProps.putIfAbsent(apiProp, column.tableName() + "." + column.columnName());
                if (previous != null) {
                    throw BusinessException.badRequest("生成的属性名冲突: " + apiProp + "（"
                            + previous + " 与 " + column.tableName() + "." + column.columnName() + "）");
                }
            }
        }
        if (!hasList) {
            columns = defaultListColumns(mainTable, columns);
        }
        return new ValidateResult(filtered.tables(), columns);
    }

    /** 列表字段被删除后回退为主表字段展示，保证旧配置仍可保存与生成 */
    private List<GenDtos.GenColumnItem> defaultListColumns(String mainTable, List<GenDtos.GenColumnItem> columns) {
        List<GenDtos.GenColumnItem> result = new ArrayList<>();
        boolean marked = false;
        for (GenDtos.GenColumnItem column : columns) {
            boolean list = mainTable.equals(column.tableName());
            marked |= list;
            result.add(list
                    ? new GenDtos.GenColumnItem(column.id(), column.tableName(), column.columnName(),
                    column.columnType(), column.columnComment(), column.displayName(), column.propertyName(),
                    column.javaType(), column.isPk(), column.isRequired(), true, column.isFilter(),
                    column.filterType(), column.isForm(), column.formComponent(), column.sort())
                    : column);
        }
        if (!marked) {
            throw BusinessException.badRequest("至少选择一个列表展示字段");
        }
        return result;
    }

    /** 按 Java/DB 类型推导默认表单元素 */
    private static String defaultFormComponent(String javaType, String columnType) {
        String base = columnType == null ? "" : columnType.toLowerCase(Locale.ROOT);
        if (base.endsWith("text") || base.equals("json") || base.endsWith("blob")) {
            return "TEXTAREA";
        }
        return switch (javaType) {
            case "Integer", "Long", "Double", "Float", "BigDecimal", "Short" -> "NUMBER";
            case "LocalDate" -> "DATE";
            case "LocalDateTime" -> "DATETIME";
            case "Boolean" -> "SWITCH";
            default -> "INPUT";
        };
    }

    private static String normalizeFormComponent(String formComponent) {
        if (!StringUtils.hasText(formComponent)) {
            return null;
        }
        String value = formComponent.trim().toUpperCase(Locale.ROOT);
        return FORM_COMPONENTS.contains(value) ? value : null;
    }

    /** 列表/查询字段在生成的 API 中的属性名：非主表加表名前缀，避免同名列冲突 */
    public static String apiProperty(String tableName, String propertyName, String mainTable) {
        if (tableName.equals(mainTable)) {
            return propertyName;
        }
        return camel(stripPrefix(tableName)) + capitalize(propertyName);
    }

    private DefaultRule defaultRule(ColumnMeta meta, DbTypeMappings.Mapping mapping,
                                    boolean allowFilter, boolean allowList) {
        String baseType = mapping.baseType(meta.type());
        String javaType = mapping.javaType(meta.type());
        boolean dateType = javaType.equals("LocalDate") || javaType.equals("LocalDateTime");
        boolean textType = baseType.endsWith("text") || baseType.equals("json")
                || baseType.equals("jsonb") || baseType.equals("bytea")
                || baseType.equals("blob") || baseType.endsWith("blob");
        String lower = meta.name().toLowerCase(Locale.ROOT);

        String filterType = null;
        if (allowFilter && !meta.pk()) {
            if (dateType) {
                filterType = "BETWEEN";
            } else if (lower.endsWith("status") || lower.endsWith("type") || lower.equals("tenant")) {
                filterType = "EQ";
            } else if ((baseType.equals("varchar") || baseType.equals("char")
                    || baseType.equals("character") || baseType.equals("character varying"))
                    && (lower.contains("name") || lower.contains("mobile") || lower.contains("phone")
                    || lower.contains("serial") || lower.endsWith("no") || lower.endsWith("code"))) {
                filterType = "LIKE";
            }
        }
        return new DefaultRule(allowList && !textType, filterType != null, filterType == null ? "EQ" : filterType);
    }

    /**
     * 关联方式猜测：其它表带主表外键 → 一对多；主表带其它表外键 → 多对一；否则一对一（需人工补关联列）。
     */
    private Relation guessRelation(String mainTable, List<ColumnMeta> mainColumns,
                                   String otherTable, List<ColumnMeta> otherColumns) {
        String mainPk = pkOf(mainColumns);
        String otherPk = pkOf(otherColumns);
        String mainShort = stripPrefix(mainTable);
        String otherShort = stripPrefix(otherTable);

        String otherFk = findColumn(otherColumns, mainShort + "_id", mainTable + "_id", mainShort + "Id");
        if (otherFk != null) {
            return new Relation("ONE_TO_MANY", mainPk, otherFk);
        }
        String mainFk = findColumn(mainColumns, otherShort + "_id", otherTable + "_id", otherShort + "Id");
        if (mainFk != null) {
            return new Relation("MANY_TO_ONE", mainFk, otherPk);
        }
        return new Relation("ONE_TO_ONE", mainPk, null);
    }

    private static String pkOf(List<ColumnMeta> columns) {
        return columns.stream().filter(ColumnMeta::pk).map(ColumnMeta::name).findFirst()
                .orElse(columns.getFirst().name());
    }

    private static String findColumn(List<ColumnMeta> columns, String... names) {
        for (String name : names) {
            for (ColumnMeta column : columns) {
                if (column.name().equalsIgnoreCase(name)) {
                    return column.name();
                }
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ 内部：工具

    private GenDtos.GenConfigItem toItem(ConfigRow row, String tableList) {
        return new GenDtos.GenConfigItem(row.id(), row.configName(), row.mainTable(), tableList,
                row.functionName(), row.moduleName(), row.businessName(), row.className(),
                row.frontendPath(), row.permPrefix(), row.genStyle(), row.packageName(), row.author(),
                row.createTime(), row.updateTime());
    }

    private static String normalizeFilter(String filterType) {
        String value = StringUtils.hasText(filterType) ? filterType.trim().toUpperCase(Locale.ROOT) : "EQ";
        return FILTER_TYPES.contains(value) ? value : "EQ";
    }

    private static String text(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private static String inferModule(String tableName) {
        String lower = tableName.toLowerCase(Locale.ROOT);
        if (lower.startsWith("sys_")) {
            return "system";
        }
        if (lower.startsWith("zl_")) {
            return "rental";
        }
        int underscore = lower.indexOf('_');
        return underscore > 0 ? lower.substring(0, underscore) : "business";
    }

    /** zl_order → order；order → order */
    public static String stripPrefix(String tableName) {
        int underscore = tableName.indexOf('_');
        return underscore >= 0 && underscore < tableName.length() - 1
                ? tableName.substring(underscore + 1) : tableName;
    }

    /** order_id → orderId */
    public static String camel(String name) {
        StringBuilder sb = new StringBuilder();
        boolean upper = false;
        for (char ch : name.toCharArray()) {
            if (ch == '_' || ch == '-') {
                upper = true;
                continue;
            }
            sb.append(upper ? Character.toUpperCase(ch) : Character.toLowerCase(ch));
            upper = false;
        }
        return sb.toString();
    }

    /** order_id → OrderId */
    public static String pascal(String name) {
        String value = camel(name);
        return value.isEmpty() ? value : Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    /** 仅首字母大写，保留已有的驼峰（orderId → OrderId） */
    public static String capitalize(String name) {
        return name == null || name.isEmpty()
                ? name : Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    private record ConfigRow(long id, String configName, String mainTable, String functionName,
                             String moduleName, String businessName, String className, String frontendPath,
                             String permPrefix, String genStyle, String packageName, String author,
                             String createTime, String updateTime) {
    }

    private record DbTableRow(String tableName, Long tableRows, String tableComment) {
    }

    private record ColumnMeta(String name, String type, String comment, boolean nullable, boolean pk) {
    }

    private record Relation(String type, String mainColumn, String joinColumn) {
    }

    private record DefaultRule(boolean list, boolean filter, String filterType) {
    }

    private record ValidateResult(List<GenDtos.GenTableItem> tables, List<GenDtos.GenColumnItem> columns) {
    }
}
