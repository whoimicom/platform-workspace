package com.admin.api.common;

import java.util.ArrayList;
import java.util.List;

public final class IdUtils {

    private IdUtils() {
    }

    /**
     * 将以英文逗号分隔的 ID 字符串解析为 Long 列表，非法片段会被忽略。
     */
    public static List<Long> parse(String ids) {
        List<Long> result = new ArrayList<>();
        if (ids == null || ids.isBlank()) {
            return result;
        }
        for (String part : ids.split(",")) {
            Long id = parseLong(part.trim());
            if (id != null) {
                result.add(id);
            }
        }
        return result;
    }

    public static Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
