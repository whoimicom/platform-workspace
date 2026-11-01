package com.platform.api.common.query;

import java.util.Map;

/**
 * 无法用 {@link QueryField} 表达的补充条件，如 OR 组合、EXISTS 子查询。
 * SQL 片段中的命名参数来自 params，最终与其它条件以 AND 拼接；
 * params 为空 Map 时片段仍参与拼接（纯 SQL 条件）。
 * 参数与 contextParams 同名且值相等时直接复用；值不同则抛出异常。
 *
 * <pre>{@code
 * RawCondition.of("(u.username LIKE :kw OR u.nickname LIKE :kw)", Map.of("kw", "%张%"))
 * RawCondition.of("EXISTS (SELECT 1 FROM sys_user_role ur WHERE ur.user_id = u.id AND ur.role_id IN (:roleIds))",
 *                 Map.of("roleIds", roleIds))
 * }</pre>
 */
public record RawCondition(String sql, Map<String, Object> params) {

    public static RawCondition of(String sql, Map<String, Object> params) {
        return new RawCondition(sql, params == null ? Map.of() : params);
    }

    public static RawCondition of(String sql) {
        return new RawCondition(sql, Map.of());
    }
}
