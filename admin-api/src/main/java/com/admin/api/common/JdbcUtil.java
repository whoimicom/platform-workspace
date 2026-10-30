package com.admin.api.common;

import java.sql.ResultSet;
import java.sql.SQLException;

public final class JdbcUtil {

    private JdbcUtil() {
    }

    public static Integer getInt(ResultSet rs, String column) throws SQLException {
        Object value = rs.getObject(column);
        if (value == null) {
            return null;
        }
        return ((Number) value).intValue();
    }

    public static Long getLong(ResultSet rs, String column) throws SQLException {
        Object value = rs.getObject(column);
        if (value == null) {
            return null;
        }
        return ((Number) value).longValue();
    }
}
