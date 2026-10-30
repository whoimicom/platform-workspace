package com.platform.api.common;

import java.util.List;

/**
 * 分页结果：{ list, total }。
 */
public record PageResult<T>(List<T> list, long total) {
}
