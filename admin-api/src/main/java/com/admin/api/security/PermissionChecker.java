package com.admin.api.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 供 @PreAuthorize("@perm.has('sys:user:list')") 使用的权限判断器。
 */
@Component("perm")
public class PermissionChecker {

    public boolean has(String permission) {
        if (permission == null || permission.isBlank()) {
            return false;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String value = authority.getAuthority();
            if ("*:*:*".equals(value) || permission.equals(value)) {
                return true;
            }
        }
        return false;
    }
}
