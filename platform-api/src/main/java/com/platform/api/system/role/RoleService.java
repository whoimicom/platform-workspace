package com.platform.api.system.role;

import com.platform.api.common.BusinessException;
import com.platform.api.common.IdUtils;
import com.platform.api.common.JdbcUtil;
import com.platform.api.common.OptionItem;
import com.platform.api.common.PageResult;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Service
public class RoleService {

    private static final Map<Integer, String> DATA_SCOPE_LABELS = Map.of(
            1, "全部数据",
            2, "部门及子部门数据",
            3, "本部门数据",
            4, "本人数据",
            5, "自定义部门数据");

    private final NamedParameterJdbcTemplate jdbc;

    public RoleService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public PageResult<RoleDtos.RoleItem> page(RoleDtos.RoleQuery query) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        Map<String, Object> params = new HashMap<>();
        if (query.getKeywords() != null && !query.getKeywords().isBlank()) {
            where.append(" AND (r.name LIKE :keywords OR r.code LIKE :keywords)");
            params.put("keywords", "%" + query.getKeywords().trim() + "%");
        }

        long total = jdbc.queryForObject("SELECT COUNT(*) FROM sys_role r" + where, params, Long.class);

        int pageNum = query.getPageNum() == null ? 1 : Math.max(1, query.getPageNum());
        int pageSize = query.getPageSize() == null ? 10 : Math.min(100, Math.max(1, query.getPageSize()));
        params.put("limit", pageSize);
        params.put("offset", (long) (pageNum - 1) * pageSize);

        List<RoleDtos.RoleItem> list = jdbc.query(
                "SELECT r.id, r.code, r.name, r.sort, r.status, r.data_scope, "
                        + "DATE_FORMAT(r.update_time, '%Y-%m-%d %H:%i:%s') AS update_time "
                        + "FROM sys_role r" + where
                        + " ORDER BY r.sort ASC, r.id ASC LIMIT :limit OFFSET :offset",
                params, (rs, rowNum) -> {
                    Integer dataScope = JdbcUtil.getInt(rs, "data_scope");
                    return new RoleDtos.RoleItem(
                            String.valueOf(rs.getLong("id")),
                            rs.getString("code"),
                            rs.getString("name"),
                            JdbcUtil.getInt(rs, "sort"),
                            JdbcUtil.getInt(rs, "status"),
                            dataScope,
                            DATA_SCOPE_LABELS.getOrDefault(dataScope == null ? 1 : dataScope, ""),
                            rs.getString("update_time"));
                });

        return new PageResult<>(list, total);
    }

    public List<OptionItem> options() {
        return jdbc.query("SELECT id, name FROM sys_role WHERE status = 1 ORDER BY sort ASC, id ASC",
                new HashMap<>(), (rs, rowNum) -> new OptionItem(
                        String.valueOf(rs.getLong("id")), rs.getString("name")));
    }

    public List<OptionItem> codeOptions() {
        return jdbc.query("SELECT code, name FROM sys_role WHERE status = 1 ORDER BY sort ASC, id ASC",
                new HashMap<>(), (rs, rowNum) -> new OptionItem(
                        rs.getString("code"), rs.getString("name")));
    }

    public RoleDtos.RoleForm form(Long id) {
        List<RoleDtos.RoleForm> rows = jdbc.query(
                "SELECT id, code, name, sort, status, data_scope, remark FROM sys_role WHERE id = :id",
                Map.of("id", id),
                (rs, rowNum) -> new RoleDtos.RoleForm(
                        String.valueOf(rs.getLong("id")),
                        rs.getString("code"),
                        rs.getString("name"),
                        JdbcUtil.getInt(rs, "sort"),
                        JdbcUtil.getInt(rs, "status"),
                        JdbcUtil.getInt(rs, "data_scope"),
                        new ArrayList<>(),
                        rs.getString("remark")));
        if (rows.isEmpty()) {
            throw BusinessException.notFound("角色不存在");
        }
        RoleDtos.RoleForm role = rows.get(0);
        return new RoleDtos.RoleForm(role.id(), role.code(), role.name(), role.sort(), role.status(),
                role.dataScope(), deptIds(id), role.remark());
    }

    public List<String> menuIds(Long roleId) {
        return jdbc.queryForList("SELECT menu_id FROM sys_role_menu WHERE role_id = :roleId",
                        Map.of("roleId", roleId), Long.class)
                .stream().map(String::valueOf).toList();
    }

    public List<String> deptIds(Long roleId) {
        return jdbc.queryForList("SELECT dept_id FROM sys_role_dept WHERE role_id = :roleId",
                        Map.of("roleId", roleId), Long.class)
                .stream().map(String::valueOf).toList();
    }

    @Transactional
    public void create(RoleDtos.RoleForm form) {
        validate(form);
        ensureCodeUnique(form.code(), null);

        MapSqlParameterSource params = roleParams(form);
        params.addValue("createTime", LocalDateTime.now());
        params.addValue("updateTime", LocalDateTime.now());

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update("INSERT INTO sys_role (code, name, sort, status, data_scope, remark, create_time, update_time) "
                        + "VALUES (:code, :name, :sort, :status, :dataScope, :remark, :createTime, :updateTime)",
                params, keyHolder, new String[]{"id"});

        Number key = keyHolder.getKey();
        if (key != null) {
            replaceDepts(key.longValue(), form);
        }
    }

    @Transactional
    public void update(Long id, RoleDtos.RoleForm form) {
        if (!exists(id)) {
            throw BusinessException.notFound("角色不存在");
        }
        validate(form);
        ensureCodeUnique(form.code(), id);

        MapSqlParameterSource params = roleParams(form);
        params.addValue("updateTime", LocalDateTime.now());
        params.addValue("id", id);
        jdbc.update("UPDATE sys_role SET code = :code, name = :name, sort = :sort, status = :status, "
                + "data_scope = :dataScope, remark = :remark, update_time = :updateTime WHERE id = :id", params);
        replaceDepts(id, form);
    }

    @Transactional
    public void delete(String ids) {
        List<Long> idList = IdUtils.parse(ids);
        if (idList.isEmpty()) {
            throw BusinessException.badRequest("请选择要删除的角色");
        }
        Map<String, Object> params = Map.of("ids", idList);
        jdbc.update("DELETE FROM sys_user_role WHERE role_id IN (:ids)", params);
        jdbc.update("DELETE FROM sys_role_menu WHERE role_id IN (:ids)", params);
        jdbc.update("DELETE FROM sys_role_dept WHERE role_id IN (:ids)", params);
        jdbc.update("DELETE FROM sys_role WHERE id IN (:ids)", params);
    }

    @Transactional
    public void updateMenus(Long roleId, List<Long> menuIds) {
        if (!exists(roleId)) {
            throw BusinessException.notFound("角色不存在");
        }
        jdbc.update("DELETE FROM sys_role_menu WHERE role_id = :roleId", Map.of("roleId", roleId));
        for (Long menuId : new LinkedHashSet<>(menuIds == null ? List.<Long>of() : menuIds)) {
            if (menuId != null) {
                jdbc.update("INSERT INTO sys_role_menu (role_id, menu_id) VALUES (:roleId, :menuId)",
                        Map.of("roleId", roleId, "menuId", menuId));
            }
        }
    }

    private void replaceDepts(Long roleId, RoleDtos.RoleForm form) {
        jdbc.update("DELETE FROM sys_role_dept WHERE role_id = :roleId", Map.of("roleId", roleId));
        if (form.dataScope() == null || form.dataScope() != 5 || form.deptIds() == null) {
            return;
        }
        for (String deptId : form.deptIds()) {
            Long parsed = IdUtils.parseLong(deptId);
            if (parsed != null) {
                jdbc.update("INSERT INTO sys_role_dept (role_id, dept_id) VALUES (:roleId, :deptId)",
                        Map.of("roleId", roleId, "deptId", parsed));
            }
        }
    }

    private MapSqlParameterSource roleParams(RoleDtos.RoleForm form) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("code", form.code() == null ? null : form.code().trim());
        params.addValue("name", form.name() == null ? null : form.name().trim());
        params.addValue("sort", form.sort() == null ? 1 : form.sort());
        params.addValue("status", form.status() == null ? 1 : form.status());
        params.addValue("dataScope", form.dataScope() == null ? 1 : form.dataScope());
        params.addValue("remark", form.remark());
        return params;
    }

    private void validate(RoleDtos.RoleForm form) {
        if (form.code() == null || form.code().isBlank()) {
            throw BusinessException.badRequest("角色编码不能为空");
        }
        if (form.name() == null || form.name().isBlank()) {
            throw BusinessException.badRequest("角色名称不能为空");
        }
    }

    private void ensureCodeUnique(String code, Long excludeId) {
        Map<String, Object> params = new HashMap<>();
        params.put("code", code.trim());
        String sql = "SELECT COUNT(*) FROM sys_role WHERE code = :code";
        if (excludeId != null) {
            sql += " AND id <> :excludeId";
            params.put("excludeId", excludeId);
        }
        Long count = jdbc.queryForObject(sql, params, Long.class);
        if (count != null && count > 0) {
            throw BusinessException.badRequest("角色编码已存在");
        }
    }

    private boolean exists(Long id) {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM sys_role WHERE id = :id",
                Map.of("id", id), Long.class);
        return count != null && count > 0;
    }
}
