package com.platform.api.system.org;

import com.platform.api.common.BusinessException;
import com.platform.api.security.LoginUser;
import com.platform.api.system.user.SysUser;
import com.platform.api.system.user.SysUserRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 架构数据权限：非 ROOT 用户对「用户管理 / 菜单管理 / 架构管理」的增删改查
 * 仅能作用于自己所在架构节点及其子树。
 * 子树匹配基于 sys_org.tree_path（逗号分隔的祖先链）前缀，实体加载 + 内存过滤完成。
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

    private final SysOrgRepository orgRepository;
    private final SysUserRepository userRepository;

    public OrgScope(SysOrgRepository orgRepository, SysUserRepository userRepository) {
        this.orgRepository = orgRepository;
        this.userRepository = userRepository;
    }

    /**
     * 计算登录用户的可见架构范围；ROOT 返回 null 表示不限制。
     */
    public Scope scopeOf(LoginUser user) {
        if (user == null || user.isRoot()) {
            return null;
        }
        SysUser owner = userRepository.findById(user.userId()).orElse(null);
        if (owner == null || owner.getOrgId() == null) {
            return EMPTY;
        }
        SysOrg org = orgRepository.findById(owner.getOrgId()).orElse(null);
        if (org == null || org.getTreePath() == null) {
            return EMPTY;
        }
        return new Scope(owner.getOrgId(), org.getTreePath());
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
        SysOrg org = orgRepository.findById(orgId).orElse(null);
        if (org == null) {
            return false;
        }
        return scope.rootId().equals(org.getId())
                || (org.getTreePath() != null && org.getTreePath().startsWith(scope.path() + ","));
    }

    /**
     * 子树 SQL 片段（命中节点自身及全部后代），参数名固定为 scopeRootId / scopePath。
     * 供动态分页查询在 WHERE 子句中做架构过滤使用。
     */
    public String subtreeCondition(String alias) {
        return "(" + alias + ".id = :scopeRootId OR " + alias + ".tree_path LIKE CONCAT(:scopePath, ',%'))";
    }

    /**
     * {@link #subtreeCondition(String)} 对应的查询参数。
     */
    public Map<String, Object> params(Scope scope) {
        return Map.of("scopeRootId", scope.rootId(), "scopePath", scope.path());
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
        List<SysOrg> all = new ArrayList<>();
        orgRepository.findAll().forEach(all::add);
        return all.stream()
                .filter(org -> scope.rootId().equals(org.getId())
                        || (org.getTreePath() != null && org.getTreePath().startsWith(scope.path() + ",")))
                .map(SysOrg::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * 校验目标用户所属架构在可见范围内，否则抛出 403。
     */
    public void checkUser(LoginUser user, Long targetUserId) {
        Scope scope = scopeOf(user);
        if (scope == null) {
            return;
        }
        SysUser target = userRepository.findById(targetUserId).orElse(null);
        if (target == null) {
            throw BusinessException.notFound("用户不存在");
        }
        if (!contains(scope, target.getOrgId())) {
            throw BusinessException.forbidden("无权操作该用户");
        }
    }
}
