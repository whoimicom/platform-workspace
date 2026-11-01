package com.platform.api.common.query;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明查询对象字段如何生成 WHERE 条件。
 *
 * <p>列名由开发者在注解中写死（支持多表别名字符串常量），前端只能提交值，
 * 因此天然免疫 SQL 注入。值为 null / 空串 / 空集合时条件自动跳过。
 *
 * <pre>{@code
 * @QueryField(value = "u.status", op = Op.EQ)
 * private Integer status;
 * }</pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface QueryField {

    /**
     * 数据库列名，可带表别名，如 u.username。
     */
    String value();

    /**
     * 操作符，默认等于。
     */
    Op op() default Op.EQ;
}
