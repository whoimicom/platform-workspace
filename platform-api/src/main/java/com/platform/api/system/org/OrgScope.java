package com.platform.api.system.org;

import com.platform.api.common.BusinessException;
import com.platform.api.security.LoginUser;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 架构数据权限：非 ROOT 用户对「用户管理 / 菜单管理 / 架构管理」的增删改查
 * 仅能作用于自己所在架构节点及其子树。
 * 子树匹配基于 sys_org.tree_path（逗号分隔的祖先链）前缀，单条 SQL 即可完成过滤，
 * 避免递归查询。
 */
@Component
public class OrgScope {

    /**
     * 可见范围；rootId + path 定位子树。scope 为 null 表示不限制（ROOT）。
     */
    public record Scope(Long rootId, String path) {
    }

    /** 用户未挂架构时返回一个匹配不到任何节点的范围。 */
    private static final Scope EMPTY = new Scope(-1L, "-1");

    private final NamedParameterJdbcTemplate jdbc;

    public OrgScope(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * 计算登录用户的可见架构范围；ROOT 返回 null 表示不限制。
     */
    public Scope scopeOf(LoginUser user) {
        if (user == null || user.isRoot()) {
            return null;
        }
        List<Long> orgIds = jdbc.queryForList(
                "SELECT org_id FROM sys_user WHERE id = :userId",
                Map.of("userId", user.userId()), Long.class);
        if (orgIds.isEmpty() || orgIds.get(0) == null) {
            return EMPTY;
        }
        Long orgId = orgIds.get(0);
        List<String> paths = jdbc.queryForList(
                "SELECT tree_path FROM sys_org WHERE id = :orgId",
                Map.of("orgId", orgId), String.class);
        if (paths.isEmpty() || paths.get(0) == null) {
            return EMPTY;
        }
        return new Scope(orgId, paths.get(0));
    }

    /**
     * 子树 SQL 片段（命中节点自身及全部后代），参数名固定为 scopeRootId / scopePath。
     */
    public String subtreeCondition(String alias) {
        return "(" + alias + ".id = :scopeRootId OR " + alias + ".tree_path LIKE CONCAT(:scopePath, ',%'))";
    }

    public Map<String, Object> params(Scope scope) {
        return Map.of("scopeRootId", scope.rootId(), "scopePath", scope.path());
    }

    /**
     * 目标架构是否在范围内。
     */
    public boolean contains(Scope scope, Long orgId) {
        if (scope == null) {
            return true;
        }
        if (orgId == null) {
            return false;
        }
        Map<String, Object> params = new java.util.HashMap<>(params(scope));
        params.put("orgId", orgId);
        Long count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sys_org o WHERE o.id = :orgId AND " + subtreeCondition("o"),
                params, Long.class);
        return count != null && count > 0;
    }

    /**
     * 校验目标架构在可见范围内，否则抛出 403。
     */
    public void check(LoginUser user, Long orgId) {
        Scope scope = scopeOf(user);
        if (scope == null) {
            return;
        }
        if (!contains(scope, orgId)) {
            throw BusinessException.forbidden("无权操作该架构");
        }
    }

    /**
     * 范围内全部架构 id（含根），用于内存过滤；scope 为 null 时返回 null 表示不限制。
     */
    public Set<Long> subtreeIds(Scope scope) {
        if (scope == null) {
            return null;
        }
        return new LinkedHashSet<>(jdbc.queryForList(
                "SELECT o.id FROM sys_org o WHERE " + subtreeCondition("o"),
                params(scope), Long.class));
    }

    /**
     * 校验目标用户所属架构在可见范围内，否则抛出 403。
     */
    public void checkUser(LoginUser user, Long targetUserId) {
        Scope scope = scopeOf(user);
        if (scope == null) {
            return;
        }
        List<Long> orgIds = jdbc.queryForList(
                "SELECT org_id FROM sys_user WHERE id = :id", Map.of("id", targetUserId), Long.class);
        if (orgIds.isEmpty()) {
            throw BusinessException.notFound("用户不存在");
        }
        if (!contains(scope, orgIds.get(0))) {
            throw BusinessException.forbidden("无权操作该用户");
        }
    }
}
