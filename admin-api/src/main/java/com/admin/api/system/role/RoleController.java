package com.admin.api.system.role;

import com.admin.api.common.ApiResult;
import com.admin.api.common.OptionItem;
import com.admin.api.common.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    @PreAuthorize("@perm.has('sys:role:list')")
    public ApiResult<PageResult<RoleDtos.RoleItem>> page(RoleDtos.RoleQuery query) {
        return ApiResult.ok(roleService.page(query));
    }

    @GetMapping("/options")
    public ApiResult<List<OptionItem>> options() {
        return ApiResult.ok(roleService.options());
    }

    @GetMapping("/code-options")
    public ApiResult<List<OptionItem>> codeOptions() {
        return ApiResult.ok(roleService.codeOptions());
    }

    @GetMapping("/{roleId}/menu-ids")
    @PreAuthorize("@perm.has('sys:role:list')")
    public ApiResult<List<String>> menuIds(@PathVariable Long roleId) {
        return ApiResult.ok(roleService.menuIds(roleId));
    }

    @PutMapping("/{roleId}/menus")
    @PreAuthorize("@perm.has('sys:role:assign')")
    public ApiResult<Void> updateMenus(@PathVariable Long roleId, @RequestBody List<Long> menuIds) {
        roleService.updateMenus(roleId, menuIds);
        return ApiResult.ok();
    }

    @GetMapping("/{roleId}/dept-ids")
    @PreAuthorize("@perm.has('sys:role:list')")
    public ApiResult<List<String>> deptIds(@PathVariable Long roleId) {
        return ApiResult.ok(roleService.deptIds(roleId));
    }

    @GetMapping("/{id}/form")
    @PreAuthorize("@perm.has('sys:role:list')")
    public ApiResult<RoleDtos.RoleForm> form(@PathVariable Long id) {
        return ApiResult.ok(roleService.form(id));
    }

    @PostMapping
    @PreAuthorize("@perm.has('sys:role:create')")
    public ApiResult<Void> create(@RequestBody RoleDtos.RoleForm form) {
        roleService.create(form);
        return ApiResult.ok();
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('sys:role:update')")
    public ApiResult<Void> update(@PathVariable Long id, @RequestBody RoleDtos.RoleForm form) {
        roleService.update(id, form);
        return ApiResult.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@perm.has('sys:role:delete')")
    public ApiResult<Void> delete(@PathVariable String ids) {
        roleService.delete(ids);
        return ApiResult.ok();
    }
}
