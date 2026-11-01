package com.platform.api.common.query;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * 动态条件分页/列表查询执行器。
 *
 * <p>条件由查询对象上的 {@link QueryField} 注解声明（列名写死在后端，前端只提交值），
 * 复杂条件（OR 组合、EXISTS 子查询等）通过 {@link RawCondition} 补充；
 * 排序属性必须命中查询类上 {@link QuerySort} 的白名单；分页自动加 LIMIT/OFFSET。
 *
 * <p>baseSql 为不含 WHERE/ORDER BY/LIMIT 的 SELECT 语句，可携带自身的命名参数
 * （由 contextParams 提供，如详情列里的 :userId）。
 */
@Component
public class DynamicQueryExecutor {

    private static final String LIMIT_PARAM = "__limit";
    private static final String OFFSET_PARAM = "__offset";

    private final NamedParameterJdbcTemplate jdbc;

    public DynamicQueryExecutor(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * 分页查询。
     */
    public <T> Page<T> page(String baseSql, PageQuery query, RowMapper<T> rowMapper, RawCondition... extras) {
        return page(baseSql, query, Map.of(), rowMapper, extras);
    }

    /**
     * 分页查询，baseSql 需要额外命名参数时使用 contextParams 提供。
     */
    public <T> Page<T> page(String baseSql, PageQuery query, Map<String, Object> contextParams,
                            RowMapper<T> rowMapper, RawCondition... extras) {
        return page(baseSql, query, contextParams, null, rowMapper, extras);
    }

    /**
     * 分页查询，可覆盖查询类注解声明的默认排序。
     */
    public <T> Page<T> page(String baseSql, PageQuery query, Map<String, Object> contextParams,
                            String defaultOrder, RowMapper<T> rowMapper, RawCondition... extras) {
        int pageNum = query.safePageNum();
        int pageSize = query.safePageSize();
        Parsed parsed = parse(baseSql, query, contextParams, defaultOrder, extras);

        StringBuilder pagedSql = new StringBuilder(parsed.sql());
        pagedSql.append(" LIMIT :").append(LIMIT_PARAM).append(" OFFSET :").append(OFFSET_PARAM);
        Map<String, Object> pagedParams = new HashMap<>(parsed.params());
        pagedParams.put(LIMIT_PARAM, pageSize);
        pagedParams.put(OFFSET_PARAM, (long) (pageNum - 1) * pageSize);

        List<T> content = jdbc.query(pagedSql.toString(), pagedParams, rowMapper);
        return new PageImpl<>(content, PageRequest.of(pageNum - 1, pageSize), parsed.total());
    }

    /**
     * 列表查询（不分页），条件与排序规则同分页查询。
     */
    public <T> List<T> list(String baseSql, Object query, RowMapper<T> rowMapper, RawCondition... extras) {
        return list(baseSql, query, Map.of(), rowMapper, extras);
    }

    /**
     * 列表查询（不分页），baseSql 需要额外命名参数时使用 contextParams 提供。
     */
    public <T> List<T> list(String baseSql, Object query, Map<String, Object> contextParams,
                            RowMapper<T> rowMapper, RawCondition... extras) {
        Parsed parsed = parse(baseSql, query, contextParams, null, extras);
        return jdbc.query(parsed.sql(), parsed.params(), rowMapper);
    }

    /**
     * 仅统计满足条件的总数。
     */
    public long count(String baseSql, Object query, Map<String, Object> contextParams, RawCondition... extras) {
        return parse(baseSql, query, contextParams, null, extras).total();
    }

    /**
     * 条件、排序组装结果：完整 SQL + 合并后的参数 + 总数。
     */
    private Parsed parse(String baseSql, Object query, Map<String, Object> contextParams,
                         String overrideDefaultOrder, RawCondition... extras) {
        Class<?> queryClass = query.getClass();
        Map<String, Object> params = new HashMap<>(contextParams);

        List<String> conditions = new ArrayList<>();
        appendAnnotatedConditions(query, params, conditions);
        appendRawConditions(extras, params, conditions);

        String sql = conditions.isEmpty() ? baseSql : baseSql + " WHERE " + String.join(" AND ", conditions);
        long total = requireCount(jdbc.queryForObject(
                "SELECT COUNT(*) FROM (" + sql + ") count_wrapper", params, Long.class));

        String orderBy = resolveOrderBy(queryClass, query instanceof PageQuery pageQuery ? pageQuery.getSort() : null,
                overrideDefaultOrder);
        if (!orderBy.isEmpty()) {
            sql = sql + " ORDER BY " + orderBy;
        }
        return new Parsed(sql, params, total);
    }

    /**
     * 拼接 {@link QueryField} 声明的条件，参数名自动生成避免冲突。
     */
    private void appendAnnotatedConditions(Object query, Map<String, Object> params, List<String> conditions) {
        int index = 0;
        for (Field field : getAllFields(query.getClass())) {
            QueryField queryField = field.getAnnotation(QueryField.class);
            if (queryField == null) {
                continue;
            }
            Object value = fieldValue(query, field);
            if (!hasValue(value)) {
                continue;
            }
            String paramName = "q" + index++;
            String fragment = buildFragment(queryField.value(), queryField.op(), value, paramName, params);
            if (fragment != null) {
                conditions.add(fragment);
            }
        }
    }

    /**
     * 按操作符生成条件片段。
     *
     * @return 片段；值无法构成有效条件（如 BETWEEN 不足两端）时返回 null
     */
    private String buildFragment(String column, Op op, Object value, String paramName, Map<String, Object> params) {
        switch (op) {
            case EQ, NE, GT, GE, LT, LE -> {
                params.put(paramName, value);
                return column + " " + op.sql() + " :" + paramName;
            }
            case LIKE, LIKE_LEFT, LIKE_RIGHT -> {
                String text = value.toString();
                String wrapped = switch (op) {
                    case LIKE -> "%" + text + "%";
                    case LIKE_LEFT -> "%" + text;
                    default -> text + "%";
                };
                params.put(paramName, wrapped);
                return column + " LIKE :" + paramName;
            }
            case IN, NOT_IN -> {
                List<Object> values = toList(value);
                if (values.isEmpty()) {
                    return null;
                }
                if (values.size() == 1) {
                    params.put(paramName, values.getFirst());
                    return column + " " + (op == Op.IN ? "=" : "!=") + " :" + paramName;
                }
                StringJoiner placeholders = new StringJoiner(", ", "(", ")");
                for (int i = 0; i < values.size(); i++) {
                    String itemParam = paramName + "_" + i;
                    params.put(itemParam, values.get(i));
                    placeholders.add(":" + itemParam);
                }
                return column + " " + op.sql() + " " + placeholders;
            }
            case BETWEEN -> {
                List<Object> values = toList(value);
                if (values.size() < 2) {
                    return null;
                }
                params.put(paramName + "_start", values.get(0));
                params.put(paramName + "_end", values.get(1));
                return column + " BETWEEN :" + paramName + "_start AND :" + paramName + "_end";
            }
            case IS_NULL -> {
                return column + " IS NULL";
            }
            case NOT_NULL -> {
                return column + " IS NOT NULL";
            }
        }
        return null;
    }

    /**
     * 拼接调用方补充的 RawCondition。
     */
    private void appendRawConditions(RawCondition[] extras, Map<String, Object> params, List<String> conditions) {
        if (extras == null) {
            return;
        }
        for (RawCondition extra : extras) {
            if (extra == null || !StringUtils.hasText(extra.sql())) {
                continue;
            }
            conditions.add(extra.sql().trim());
            extra.params().forEach((name, value) -> {
                Object existing = params.get(name);
                if (existing != null && !existing.equals(value)) {
                    throw new IllegalArgumentException("查询参数名冲突且值不同: " + name);
                }
                params.putIfAbsent(name, value);
            });
        }
    }

    /**
     * 排序优先级：显式指定 > 前端 sort（白名单映射）> 注解 defaultSort。
     */
    private String resolveOrderBy(Class<?> queryClass, List<String> sort, String overrideDefaultOrder) {
        QuerySort querySort = queryClass.getAnnotation(QuerySort.class);
        if (sort != null && !sort.isEmpty() && querySort != null) {
            Map<String, String> mappings = new LinkedHashMap<>();
            for (QuerySortMapping mapping : querySort.value()) {
                mappings.put(mapping.property(), mapping.column());
            }
            List<String> parts = new ArrayList<>();
            for (String expression : sort) {
                if (!StringUtils.hasText(expression)) {
                    continue;
                }
                String[] pieces = expression.split(",");
                String column = mappings.get(pieces[0].trim());
                if (column == null) {
                    continue;
                }
                String direction = pieces.length > 1 && "desc".equalsIgnoreCase(pieces[1].trim()) ? "DESC" : "ASC";
                parts.add(column + " " + direction);
            }
            if (!parts.isEmpty()) {
                return String.join(", ", parts);
            }
        }
        if (StringUtils.hasText(overrideDefaultOrder)) {
            return overrideDefaultOrder;
        }
        return querySort == null ? "" : querySort.defaultSort();
    }

    /**
     * 判断字段值是否参与查询：null / 空白串 / 空集合跳过。
     */
    private boolean hasValue(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof String text) {
            return StringUtils.hasText(text);
        }
        if (value instanceof Collection<?> collection) {
            return !collection.isEmpty();
        }
        return true;
    }

    /**
     * IN / BETWEEN 的值统一转为列表；逗号分隔字符串自动拆分。
     */
    private List<Object> toList(Object value) {
        if (value instanceof Collection<?> collection) {
            return new ArrayList<>(collection);
        }
        List<Object> values = new ArrayList<>();
        for (String part : value.toString().split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                values.add(trimmed);
            }
        }
        return values;
    }

    private Object fieldValue(Object query, Field field) {
        try {
            field.setAccessible(true);
            return field.get(query);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("读取查询字段失败: " + field.getName(), e);
        }
    }

    private List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        for (Class<?> current = clazz; current != null && current != Object.class; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (field.getAnnotation(QueryField.class) != null) {
                    fields.add(field);
                }
            }
        }
        return fields;
    }

    private long requireCount(Long count) {
        return count == null ? 0L : count;
    }

    private record Parsed(String sql, Map<String, Object> params, long total) {
    }
}
