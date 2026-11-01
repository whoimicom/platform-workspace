package com.platform.api.common.query;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 类级排序白名单：前端 sort 参数中的属性名必须在此映射到列名才会生效，
 * 未映射的属性被忽略，避免注入与暴露物理列名。
 *
 * <pre>{@code
 * @QuerySort(value = {
 *     @QuerySortMapping(property = "createTime", column = "u.create_time"),
 *     @QuerySortMapping(property = "username", column = "u.username")
 * }, defaultSort = "u.create_time DESC, u.id DESC")
 * }</pre>
 *
 * 注意：数组必须写成 {@code value = {...}} 具名形式——注解的无名数组简写
 * {@code @QuerySort({...}, defaultSort = "...")} 与具名元素混用是非法语法。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface QuerySort {

    /**
     * 允许排序的属性与列映射。
     */
    QuerySortMapping[] value();

    /**
     * 前端未传有效排序时使用的默认排序，如 "u.create_time DESC, u.id DESC"。
     * 空串表示不加 ORDER BY。
     */
    String defaultSort() default "";
}
