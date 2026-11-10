package com.platform.api.system.role;

import com.platform.api.common.BusinessException;
import com.platform.api.common.IdUtils;
import com.platform.api.common.JdbcUtil;
import com.platform.api.common.OptionItem;
import org.springframework.data.domain.Page;
import com.platform.api.common.query.DynamicQueryExecutor;
import com.platform.api.common.query.RawCondition;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
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
            2, "架构及子架构数据",
            3, "本架构数据",
            4, "本人数据",
            5, "自定义架构数据");

    private static final RowMapper<RoleDtos.RoleItem> ROLE_ITEM_MAPPER = (rs, rowNum) -> {
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
    };

    private static final String ROLE_BASE_SELECT = """
            SELECT r.id, r.code, r.name, r.sort, r.status, r.data_scope,
                   DATE_FORMAT(r.update_time, '%Y-%m-%d %H:%i:%s') AS update_time
              FROM sys_role r
            """;

    private final NamedParameterJdbcTemplate jdbc;
    private final DynamicQueryExecutor executor;
    private final SysRoleRepository roleRepository;

    public RoleService(NamedParameterJdbcTemplate jdbc,
                       DynamicQueryExecutor executor,
                       SysRoleRepository roleRepository) {
        this.jdbc = jdbc;
        this.executor = executor;
        this.roleRepository = roleRepository;
    }

    public Page<RoleDtos.RoleItem> page(RoleDtos.RoleQuery query) {
        List<RawCondition> extras = new ArrayList<>();
        if (query.getKeywords() != null && !query.getKeywords().isBlank()) {
            extras.add(RawCondition.of("(r.name LIKE :keywords OR r.code LIKE :keywords)",
                    Map.of("keywords", "%" + query.getKeywords().trim() + "%")));
        }
        return executor.page(ROLE_BASE_SELECT, query, ROLE_ITEM_MAPPER, extras.toArray(RawCondition[]::new));
    }

    public List<OptionItem> options() {
        List<SysRole> roles = loadActiveRoles();
        return roles.stream().map(role -> new OptionItem(
                String.valueOf(role.getId()), role.getName())).toList();
    }

    public List<OptionItem> codeOptions() {
        List<SysRole> roles = loadActiveRoles();
        return roles.stream().map(role -> new OptionItem(role.getCode(), role.getName())).toList();
    }

    /** 启用中的角色，按 sort、id 排序 */
    private List<SysRole> loadActiveRoles() {
        List<SysRole> roles = new ArrayList<>();
        roleRepository.findAll().forEach(roles::add);
        roles.sort(java.util.Comparator
                .comparing((SysRole role) -> role.getSort() == null ? 0 : role.getSort())
                .thenComparing(SysRole::getId));
        return roles.stream()
                .filter(role -> role.getStatus() != null && role.getStatus() == 1)
                .toList();
    }

    public RoleDtos.RoleForm form(Long id) {
        SysRole role = roleRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("角色不存在"));
        return new RoleDtos.RoleForm(String.valueOf(role.getId()), role.getCode(), role.getName(),
                role.getSort(), role.getStatus(), role.getDataScope(), orgIds(id), role.getRemark());
    }

    public List<String> menuIds(Long roleId) {
        return jdbc.queryForList("SELECT menu_id FROM sys_role_menu WHERE role_id = :roleId",
                        Map.of("roleId", roleId), Long.class)
                .stream().map(String::valueOf).toList();
    }

    public List<String> orgIds(Long roleId) {
        return jdbc.queryForList("SELECT org_id FROM sys_role_org WHERE role_id = :roleId",
                        Map.of("roleId", roleId), Long.class)
                .stream().map(String::valueOf).toList();
    }

    @Transactional
    public void create(RoleDtos.RoleForm form) {
        validate(form);
        ensureCodeUnique(form.code(), null);

        SysRole role = new SysRole();
        role.setCode(form.code() == null ? null : form.code().trim());
        role.setName(form.name() == null ? null : form.name().trim());
        role.setSort(form.sort() == null ? 1 : form.sort());
        role.setStatus(form.status() == null ? 1 : form.status());
        role.setDataScope(form.dataScope() == null ? 1 : form.dataScope());
        role.setRemark(form.remark());
        LocalDateTime now = LocalDateTime.now();
        role.setCreateTime(now);
        role.setUpdateTime(now);
        SysRole saved = roleRepository.save(role);
        replaceOrgs(saved.getId(), form);
    }

    @Transactional
    public void update(Long id, RoleDtos.RoleForm form) {
        SysRole role = roleRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("角色不存在"));
        validate(form);
        ensureCodeUnique(form.code(), id);

        role.setCode(form.code() == null ? null : form.code().trim());
        role.setName(form.name() == null ? null : form.name().trim());
        role.setSort(form.sort() == null ? 1 : form.sort());
        role.setStatus(form.status() == null ? 1 : form.status());
        role.setDataScope(form.dataScope() == null ? 1 : form.dataScope());
        role.setRemark(form.remark());
        role.setUpdateTime(LocalDateTime.now());
        roleRepository.save(role);
        replaceOrgs(id, form);
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
        jdbc.update("DELETE FROM sys_role_org WHERE role_id IN (:ids)", params);
        roleRepository.deleteAllById(idList);
    }

    @Transactional
    public void updateMenus(Long roleId, List<Long> menuIds) {
        if (!roleRepository.existsById(roleId)) {
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

    private void replaceOrgs(Long roleId, RoleDtos.RoleForm form) {
        jdbc.update("DELETE FROM sys_role_org WHERE role_id = :roleId", Map.of("roleId", roleId));
        if (form.dataScope() == null || form.dataScope() != 5 || form.orgIds() == null) {
            return;
        }
        for (String orgId : form.orgIds()) {
            Long parsed = IdUtils.parseLong(orgId);
            if (parsed != null) {
                jdbc.update("INSERT INTO sys_role_org (role_id, org_id) VALUES (:roleId, :orgId)",
                        Map.of("roleId", roleId, "orgId", parsed));
            }
        }
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
        var existing = roleRepository.findByCode(code.trim());
        if (existing.isPresent()
                && (excludeId == null || !existing.get().getId().equals(excludeId))) {
            throw BusinessException.badRequest("角色编码已存在");
        }
    }
}
