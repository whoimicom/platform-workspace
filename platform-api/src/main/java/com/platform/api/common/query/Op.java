package com.platform.api.common.query;

/**
 * 动态查询支持的操作符。
 */
public enum Op {

    /** 等于 */
    EQ,
    /** 不等于 */
    NE,
    /** 大于 */
    GT,
    /** 大于等于 */
    GE,
    /** 小于 */
    LT,
    /** 小于等于 */
    LE,
    /** 包含 %value% */
    LIKE,
    /** 左模糊 %value */
    LIKE_LEFT,
    /** 右模糊 value% */
    LIKE_RIGHT,
    /** 集合包含（单值自动降级为 =） */
    IN,
    /** 集合排除（单值自动降级为 !=） */
    NOT_IN,
    /** 区间，值为 2 个元素的集合或逗号分隔字符串 */
    BETWEEN,
    /** 为空 */
    IS_NULL,
    /** 不为空 */
    NOT_NULL;

    /**
     * 对应的 SQL 片段符号。
     */
    public String sql() {
        return switch (this) {
            case EQ -> "=";
            case NE -> "!=";
            case GT -> ">";
            case GE -> ">=";
            case LT -> "<";
            case LE -> "<=";
            case IN -> "IN";
            case NOT_IN -> "NOT IN";
            default -> name();
        };
    }
}
