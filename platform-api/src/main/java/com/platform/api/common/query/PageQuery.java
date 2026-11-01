package com.platform.api.common.query;

import java.util.ArrayList;
import java.util.List;

/**
 * 分页查询参数基类：pageNum / pageSize / sort。
 * 业务查询对象继承后只需声明自己的筛选字段。
 *
 * <p>sort 元素格式为 "property" 或 "property,asc|desc"，
 * 属性名必须命中查询对象类上 {@link QuerySort} 的白名单。
 */
public class PageQuery {

    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private List<String> sort = new ArrayList<>();

    public Integer getPageNum() {
        return pageNum;
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public List<String> getSort() {
        return sort;
    }

    public void setSort(List<String> sort) {
        this.sort = sort;
    }

    /**
     * 修正后的页码，最小为 1。
     */
    public int safePageNum() {
        return pageNum == null ? 1 : Math.max(1, pageNum);
    }

    /**
     * 修正后的每页条数，范围 [1, 100]。
     */
    public int safePageSize() {
        return pageSize == null ? 10 : Math.min(100, Math.max(1, pageSize));
    }
}
