package com.platform.api.common.query;

import org.springframework.jdbc.core.RowMapper;

import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * record 行映射器：按列名自动匹配 record 组件，免去逐列手写取值。
 *
 * <p>匹配规则：列名与组件名忽略大小写和下划线差异（{@code order_id} ↔ {@code orderId}），
 * 因此 SELECT 里保留 {@code z.xxx AS xxx} 的 snake_case 别名即可。
 * 结果集缺少 record 组件对应的列时直接报错，避免字段漂移后静默返回 null。
 *
 * <p>每次查询通过 {@link #of(Class)} 新建实例，不要跨查询复用（内部缓存列解析结果）。
 */
public final class RecordRowMapper<T> implements RowMapper<T> {

    private static final Map<Class<?>, Meta> META_CACHE = new ConcurrentHashMap<>();

    private final Meta meta;
    private String[] labels;

    private RecordRowMapper(Meta meta) {
        this.meta = meta;
    }

    public static <T> RowMapper<T> of(Class<T> type) {
        return new RecordRowMapper<>(metaOf(type));
    }

    @Override
    @SuppressWarnings("unchecked")
    public T mapRow(ResultSet rs, int rowNum) throws SQLException {
        if (labels == null) {
            labels = resolveLabels(rs.getMetaData(), meta.names());
        }
        Object[] args = new Object[labels.length];
        for (int i = 0; i < labels.length; i++) {
            args[i] = read(rs, labels[i], meta.types()[i]);
        }
        try {
            return (T) meta.constructor().newInstance(args);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("record 构造函数调用失败: " + meta.type().getName(), e);
        }
    }

    /**
     * 列名与 record 组件名做规范化匹配，返回每个组件对应的真实列名。
     */
    private static String[] resolveLabels(ResultSetMetaData metaData, String[] names) throws SQLException {
        Map<String, String> columns = new HashMap<>();
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            String label = metaData.getColumnLabel(i);
            columns.putIfAbsent(normalize(label), label);
        }

        String[] labels = new String[names.length];
        List<String> missing = new ArrayList<>();
        for (int i = 0; i < names.length; i++) {
            labels[i] = columns.get(normalize(names[i]));
            if (labels[i] == null) {
                missing.add(names[i]);
            }
        }
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("SQL 结果缺少 record 字段对应的列: " + String.join(", ", missing));
        }
        return labels;
    }

    private static Object read(ResultSet rs, String label, Class<?> type) throws SQLException {
        if (type == String.class || type == CharSequence.class) {
            return rs.getString(label);
        }
        if (type == Integer.class || type == int.class) {
            Number value = (Number) rs.getObject(label);
            return value == null ? null : value.intValue();
        }
        if (type == Long.class || type == long.class) {
            Number value = (Number) rs.getObject(label);
            return value == null ? null : value.longValue();
        }
        if (type == Short.class || type == short.class) {
            Number value = (Number) rs.getObject(label);
            return value == null ? null : value.shortValue();
        }
        if (type == Double.class || type == double.class) {
            Number value = (Number) rs.getObject(label);
            return value == null ? null : value.doubleValue();
        }
        if (type == Float.class || type == float.class) {
            Number value = (Number) rs.getObject(label);
            return value == null ? null : value.floatValue();
        }
        if (type == BigDecimal.class) {
            return rs.getBigDecimal(label);
        }
        if (type == Boolean.class || type == boolean.class) {
            Object value = rs.getObject(label);
            if (value == null) {
                return null;
            }
            if (value instanceof Boolean bool) {
                return bool;
            }
            if (value instanceof Number number) {
                return number.intValue() != 0;
            }
            return Boolean.valueOf(value.toString());
        }
        if (type == LocalDate.class) {
            return rs.getObject(label, LocalDate.class);
        }
        if (type == LocalDateTime.class) {
            return rs.getObject(label, LocalDateTime.class);
        }
        return rs.getObject(label, type);
    }

    private static String normalize(String name) {
        return name.replace("_", "").toLowerCase(Locale.ROOT);
    }

    @SuppressWarnings("unchecked")
    private static Meta metaOf(Class<?> type) {
        return META_CACHE.computeIfAbsent(type, key -> {
            if (!key.isRecord()) {
                throw new IllegalArgumentException("RecordRowMapper 仅支持 record 类型: " + key.getName());
            }
            RecordComponent[] components = key.getRecordComponents();
            String[] names = new String[components.length];
            Class<?>[] types = new Class<?>[components.length];
            for (int i = 0; i < components.length; i++) {
                names[i] = components[i].getName();
                types[i] = components[i].getType();
            }
            try {
                Constructor<?> constructor = key.getDeclaredConstructor(types);
                // 私有嵌套 record 的规范构造函数是 private，需要开放访问
                constructor.setAccessible(true);
                return new Meta(key, constructor, names, types);
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException("record 规范构造函数不存在: " + key.getName(), e);
            }
        });
    }

    private record Meta(Class<?> type, Constructor<?> constructor, String[] names, Class<?>[] types) {
    }
}
