package com.admin.api.security;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 每次请求按用户实时加载角色与权限，避免权限变更后仍需等待令牌过期。
 */
@Service
public class UserAuthorityService {

    private final NamedParameterJdbcTemplate jdbc;

    public UserAuthorityService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public LoginUser load(Long userId) {
        if (userId == null) {
            return null;
        }
        Map<String, Object> params = Map.of("userId", userId);

        List<String> usernames = jdbc.query(
                "SELECT username FROM sys_user WHERE id = :userId AND status = 1",
                params,
                (rs, rowNum) -> rs.getString("username"));
        if (usernames.isEmpty()) {
            return null;
        }

        List<String> roleCodes = jdbc.query(
                "SELECT r.code FROM sys_role r JOIN sys_user_role ur ON ur.role_id = r.id "
                        + "WHERE ur.user_id = :userId AND r.status = 1 ORDER BY r.sort ASC",
                params,
                (rs, rowNum) -> rs.getString("code"));

        List<String> permissions = jdbc.query(
                "SELECT DISTINCT m.perm FROM sys_menu m "
                        + "JOIN sys_role_menu rm ON rm.menu_id = m.id "
                        + "JOIN sys_role r ON r.id = rm.role_id "
                        + "JOIN sys_user_role ur ON ur.role_id = r.id "
                        + "WHERE ur.user_id = :userId AND r.status = 1 "
                        + "AND m.perm IS NOT NULL AND m.perm <> ''",
                params,
                (rs, rowNum) -> rs.getString("perm"));

        Set<String> roles = new LinkedHashSet<>(roleCodes);
        Set<String> perms = new LinkedHashSet<>(permissions);
        if (roles.contains("ROOT")) {
            perms.add("*:*:*");
        }
        return new LoginUser(userId, usernames.get(0), roles, perms);
    }
}
