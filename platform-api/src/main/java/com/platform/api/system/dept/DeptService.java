package com.platform.api.system.dept;

import com.platform.api.common.BusinessException;
import com.platform.api.common.IdUtils;
import com.platform.api.common.JdbcUtil;
import com.platform.api.common.OptionItem;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DeptService {

    private final NamedParameterJdbcTemplate jdbc;

    public DeptService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<DeptDtos.DeptItem> tree(String keywords, Integer status) {
        List<DeptRow> rows = loadRows();
        java.util.function.Predicate<DeptRow> predicate = row -> {
            boolean keywordMatched = keywords == null || keywords.isBlank()
                    || (row.name() != null && row.name().contains(keywords.trim()));
            boolean statusMatched = status == null || (row.status() != null && row.status().equals(status));
            return keywordMatched && statusMatched;
        };
        return build(rows, 0L, predicate);
    }

    public List<OptionItem> options() {
        List<DeptRow> rows = loadRows();
        return buildOptions(rows, 0L);
    }

    public DeptDtos.DeptForm form(Long id) {
        List<DeptDtos.DeptForm> rows = jdbc.query(
                "SELECT id, name, code, parent_id, sort, status FROM sys_dept WHERE id = :id",
                Map.of("id", id),
                (rs, rowNum) -> new DeptDtos.DeptForm(
                        String.valueOf(rs.getLong("id")),
                        rs.getString("name"),
                        rs.getString("code"),
                        String.valueOf(rs.getLong("parent_id")),
                        JdbcUtil.getInt(rs, "sort"),
                        JdbcUtil.getInt(rs, "status")));
        if (rows.isEmpty()) {
            throw BusinessException.notFound("部门不存在");
        }
        return rows.get(0);
    }

    @Transactional
    public void create(DeptDtos.DeptForm form) {
        validate(form);
        Long parentId = resolveParentId(form.parentId());
        ensureParentExists(parentId);

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("parentId", parentId);
        params.addValue("name", form.name().trim());
        params.addValue("code", form.code());
        params.addValue("sort", form.sort() == null ? 1 : form.sort());
        params.addValue("status", form.status() == null ? 1 : form.status());
        params.addValue("createTime", LocalDateTime.now());
        params.addValue("updateTime", LocalDateTime.now());

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update("INSERT INTO sys_dept (parent_id, name, code, sort, status, create_time, update_time) "
                        + "VALUES (:parentId, :name, :code, :sort, :status, :createTime, :updateTime)",
                params, keyHolder, new String[]{"id"});

        Number key = keyHolder.getKey();
        if (key != null) {
            updateTreePath(key.longValue());
        }
    }

    @Transactional
    public void update(Long id, DeptDtos.DeptForm form) {
        if (!exists(id)) {
            throw BusinessException.notFound("部门不存在");
        }
        validate(form);
        Long parentId = resolveParentId(form.parentId());
        if (parentId == id) {
            throw BusinessException.badRequest("上级部门不能是自身");
        }
        ensureParentExists(parentId);

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("parentId", parentId);
        params.addValue("name", form.name().trim());
        params.addValue("code", form.code());
        params.addValue("sort", form.sort() == null ? 1 : form.sort());
        params.addValue("status", form.status() == null ? 1 : form.status());
        params.addValue("updateTime", LocalDateTime.now());
        params.addValue("id", id);
        jdbc.update("UPDATE sys_dept SET parent_id = :parentId, name = :name, code = :code, "
                + "sort = :sort, status = :status, update_time = :updateTime WHERE id = :id", params);
        updateTreePath(id);
        updateChildTreePaths(id);
    }

    @Transactional
    public void delete(String ids) {
        List<Long> idList = IdUtils.parse(ids);
        if (idList.isEmpty()) {
            throw BusinessException.badRequest("请选择要删除的部门");
        }
        Map<String, Object> params = Map.of("ids", idList);
        Long children = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sys_dept WHERE parent_id IN (:ids)", params, Long.class);
        if (children != null && children > 0) {
            throw BusinessException.badRequest("请先删除子部门");
        }
        Long users = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sys_user WHERE dept_id IN (:ids)", params, Long.class);
        if (users != null && users > 0) {
            throw BusinessException.badRequest("部门下存在用户，无法删除");
        }
        jdbc.update("DELETE FROM sys_role_dept WHERE dept_id IN (:ids)", params);
        jdbc.update("DELETE FROM sys_dept WHERE id IN (:ids)", params);
    }

    private List<DeptRow> loadRows() {
        return jdbc.query(
                "SELECT id, parent_id, name, code, sort, status, tree_path, "
                        + "DATE_FORMAT(create_time, '%Y-%m-%d %H:%i:%s') AS create_time, "
                        + "DATE_FORMAT(update_time, '%Y-%m-%d %H:%i:%s') AS update_time "
                        + "FROM sys_dept ORDER BY sort ASC, id ASC",
                new HashMap<>(), (rs, rowNum) -> new DeptRow(
                rs.getLong("id"),
                rs.getLong("parent_id"),
                rs.getString("name"),
                rs.getString("code"),
                JdbcUtil.getInt(rs, "sort"),
                JdbcUtil.getInt(rs, "status"),
                rs.getString("tree_path"),
                rs.getString("create_time"),
                rs.getString("update_time")));
    }

    /**
     * 命中节点保留祖先链，保证树形结构完整。
     */
    private List<DeptDtos.DeptItem> build(List<DeptRow> rows, long parentId,
                                          java.util.function.Predicate<DeptRow> predicate) {
        Map<Long, List<DeptRow>> grouped = new HashMap<>();
        rows.forEach(row -> grouped.computeIfAbsent(row.parentId(), key -> new ArrayList<>()).add(row));

        List<DeptDtos.DeptItem> result = new ArrayList<>();
        for (DeptRow row : grouped.getOrDefault(parentId, List.of())) {
            List<DeptDtos.DeptItem> children = build(rows, row.id(), predicate);
            if (predicate.test(row) || !children.isEmpty()) {
                result.add(toItem(row, children));
            }
        }
        result.sort(Comparator.comparing(item -> item.sort() == null ? 0 : item.sort()));
        return result;
    }

    private DeptDtos.DeptItem toItem(DeptRow row, List<DeptDtos.DeptItem> children) {
        return new DeptDtos.DeptItem(String.valueOf(row.id()), String.valueOf(row.parentId()),
                row.name(), row.sort(), row.status(), row.treePath(),
                row.createTime(), row.updateTime(), children);
    }

    private List<OptionItem> buildOptions(List<DeptRow> rows, long parentId) {
        List<OptionItem> result = new ArrayList<>();
        for (DeptRow row : rows) {
            if (row.parentId() != parentId) {
                continue;
            }
            result.add(new OptionItem(String.valueOf(row.id()), row.name(), buildOptions(rows, row.id())));
        }
        return result;
    }

    private void updateTreePath(Long id) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT parent_id FROM sys_dept WHERE id = :id", Map.of("id", id));
        if (rows.isEmpty()) {
            return;
        }
        Long parentId = ((Number) rows.get(0).get("parent_id")).longValue();
        String treePath;
        if (parentId == 0L) {
            treePath = String.valueOf(id);
        } else {
            List<Map<String, Object>> parents = jdbc.queryForList(
                    "SELECT tree_path FROM sys_dept WHERE id = :id", Map.of("id", parentId));
            String parentPath = parents.isEmpty() || parents.get(0).get("tree_path") == null
                    ? String.valueOf(parentId) : parents.get(0).get("tree_path").toString();
            treePath = parentPath + "," + id;
        }
        jdbc.update("UPDATE sys_dept SET tree_path = :treePath WHERE id = :id",
                Map.of("treePath", treePath, "id", id));
    }

    private void updateChildTreePaths(Long parentId) {
        List<Long> childIds = jdbc.queryForList(
                "SELECT id FROM sys_dept WHERE parent_id = :parentId", Map.of("parentId", parentId), Long.class);
        for (Long childId : childIds) {
            updateTreePath(childId);
            updateChildTreePaths(childId);
        }
    }

    private Long resolveParentId(String parentId) {
        Long parsed = IdUtils.parseLong(parentId);
        return parsed == null ? 0L : parsed;
    }

    private void ensureParentExists(Long parentId) {
        if (parentId == 0L) {
            return;
        }
        if (!exists(parentId)) {
            throw BusinessException.badRequest("上级部门不存在");
        }
    }

    private boolean exists(Long id) {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM sys_dept WHERE id = :id",
                Map.of("id", id), Long.class);
        return count != null && count > 0;
    }

    private void validate(DeptDtos.DeptForm form) {
        if (form.name() == null || form.name().isBlank()) {
            throw BusinessException.badRequest("部门名称不能为空");
        }
    }

    private record DeptRow(long id, long parentId, String name, String code, Integer sort, Integer status,
                           String treePath, String createTime, String updateTime) {
    }
}
