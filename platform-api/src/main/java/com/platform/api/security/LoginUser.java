package com.platform.api.security;

import java.util.Set;

/**
 * 放入 SecurityContext 的登录主体。
 */
public record LoginUser(Long userId, String username, Set<String> roles, Set<String> perms) {

    public boolean isRoot() {
        return roles.contains("ROOT");
    }
}
