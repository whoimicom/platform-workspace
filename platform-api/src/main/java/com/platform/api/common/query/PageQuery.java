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

    /** 每页最大条数：防止前端注入超大 pageSize 一次拉取全量数据 */
    public static final int MAX_PAGE_SIZE = 100;

    /** 默认每页条数 */
    public static final int DEFAULT_PAGE_SIZE = 10;

    private Integer pageNum = 1;
    private Integer pageSize = DEFAULT_PAGE_SIZE;
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

    /**
     * 参数绑定阶段即完成收敛，后续无论读取 getPageSize 还是 safePageSize 都不会超过上限。
     */
    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize == null ? DEFAULT_PAGE_SIZE
                : Math.min(MAX_PAGE_SIZE, Math.max(1, pageSize));
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
        return pageSize == null ? DEFAULT_PAGE_SIZE
                : Math.min(MAX_PAGE_SIZE, Math.max(1, pageSize));
    }
}
