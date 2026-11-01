package com.platform.api.common.query;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * {@link QuerySort#value()} 的排序映射项：属性名到列名。
 * 独立顶层注解（而非 QuerySort 的嵌套类型），引用更直观。
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface QuerySortMapping {

    /**
     * 对外暴露的属性名（前端 sort 参数使用）。
     */
    String property();

    /**
     * 数据库列名，可带表别名。
     */
    String column();
}
