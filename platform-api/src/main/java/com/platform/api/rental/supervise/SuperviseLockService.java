package com.platform.api.rental.supervise;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jdbc.core.JdbcAggregateOperations;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 设备锁查询：Spring Data JDBC 原生动态查询 + 分页示例。
 *
 * <p>Spring Data JDBC 不支持 {@code @Query} 方法返回 {@code Page}（字符串查询无法推导 count），
 * 因此原生的动态分页写法是：
 * {@code Criteria} 组装条件 → {@code Query.with(Pageable)} 应用 LIMIT/OFFSET/Sort →
 * {@code JdbcAggregateOperations.findAll(Query, Class)} 取当前页 →
 * {@code JdbcAggregateOperations.count(Query, Class)} 取总数 → 组装 {@code PageImpl}。
 */
@Service
public class SuperviseLockService {

    /** 允许前端排序的属性白名单，由 Spring Data JDBC 映射成对应列 */
    private static final Set<String> SORTABLE = Set.of(
            "id", "orderId", "serialNumber", "createTime", "updateTime", "lastActiveTime");

    private final JdbcAggregateOperations operations;

    public SuperviseLockService(JdbcAggregateOperations operations) {
        this.operations = operations;
    }

    public Page<SuperviseLockEntity> page(SuperviseLockQuery query) {
        Criteria criteria = Criteria.empty();
        if (StringUtils.hasText(query.getOrderId())) {
            criteria = criteria.and("order_id").is(query.getOrderId().trim());
        }
        if (query.getSuperviseId() != null) {
            criteria = criteria.and("supervise_id").is(query.getSuperviseId());
        }
        if (StringUtils.hasText(query.getSerialNumber())) {
            criteria = criteria.and("serial_number").like("%" + query.getSerialNumber().trim() + "%");
        }
        if (StringUtils.hasText(query.getSerialCode())) {
            criteria = criteria.and("serial_code").like("%" + query.getSerialCode().trim() + "%");
        }
        if (StringUtils.hasText(query.getStatus())) {
            criteria = criteria.and("status").is(query.getStatus().trim());
        }
        if (StringUtils.hasText(query.getIsInterface())) {
            criteria = criteria.and("is_interface").is(query.getIsInterface().trim());
        }
        if (StringUtils.hasText(query.getTenant())) {
            criteria = criteria.and("tenant").is(query.getTenant().trim());
        }
        if (StringUtils.hasText(query.getBrandId())) {
            criteria = criteria.and("brand_id").is(query.getBrandId().trim());
        }
        if (StringUtils.hasText(query.getSysType())) {
            criteria = criteria.and("sys_type").is(query.getSysType().trim());
        }
        if (StringUtils.hasText(query.getKeywords())) {
            String keyword = "%" + query.getKeywords().trim() + "%";
            // 多个 LIKE 之间是 OR，作为一组 AND 到主条件上
            Criteria keywordCriteria = Criteria.where("order_id").like(keyword)
                    .or("serial_number").like(keyword)
                    .or("serial_code").like(keyword)
                    .or("serial_code2").like(keyword)
                    .or("model").like(keyword);
            criteria = criteria.and(keywordCriteria);
        }
        LocalDateTime[] createTime = query.getCreateTime();
        if (createTime != null && createTime.length == 2 && createTime[0] != null && createTime[1] != null) {
            criteria = criteria.and("create_time").between(createTime[0], createTime[1]);
        }

        Sort sort = resolveSort(query.getSort());
        Pageable pageable = PageRequest.of(query.safePageNum() - 1, query.safePageSize(), sort);
        Query q = Query.query(criteria).with(pageable);

        List<SuperviseLockEntity> content = operations.findAll(q, SuperviseLockEntity.class);
        long total = operations.count(Query.query(criteria), SuperviseLockEntity.class);
        return new PageImpl<>(content, pageable, total);
    }

    /**
     * 解析 sort 参数：元素格式 "property" 或 "property,asc|desc"，属性名必须命中白名单。
     */
    private Sort resolveSort(List<String> sortParams) {
        List<Sort.Order> orders = new ArrayList<>();
        if (sortParams != null) {
            for (String item : sortParams) {
                if (!StringUtils.hasText(item)) {
                    continue;
                }
                String[] parts = item.split(",");
                String property = parts[0].trim();
                if (!SORTABLE.contains(property)) {
                    continue;
                }
                boolean asc = parts.length < 2 || "asc".equalsIgnoreCase(parts[1].trim());
                orders.add(asc ? Sort.Order.asc(property) : Sort.Order.desc(property));
            }
        }
        return orders.isEmpty() ? Sort.by(Sort.Order.desc("id")) : Sort.by(orders);
    }
}
