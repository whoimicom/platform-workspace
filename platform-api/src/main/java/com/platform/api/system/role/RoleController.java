package com.platform.api.system.role;

import com.platform.api.common.ResultInfo;
import com.platform.api.common.OptionItem;
import org.springframework.data.domain.Page;
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
@RequestMapping("/api/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    @PreAuthorize("@perm.has('sys:role:list')")
    public ResultInfo<Page<RoleDtos.RoleItem>> page(RoleDtos.RoleQuery query) {
        return ResultInfo.ok(roleService.page(query));
    }

    @GetMapping("/options")
    public ResultInfo<List<OptionItem>> options() {
        return ResultInfo.ok(roleService.options());
    }

    @GetMapping("/code-options")
    public ResultInfo<List<OptionItem>> codeOptions() {
        return ResultInfo.ok(roleService.codeOptions());
    }

    @GetMapping("/{roleId}/menu-ids")
    @PreAuthorize("@perm.has('sys:role:list')")
    public ResultInfo<List<String>> menuIds(@PathVariable Long roleId) {
        return ResultInfo.ok(roleService.menuIds(roleId));
    }

    @PutMapping("/{roleId}/menus")
    @PreAuthorize("@perm.has('sys:role:assign')")
    public ResultInfo<Void> updateMenus(@PathVariable Long roleId, @RequestBody List<Long> menuIds) {
        roleService.updateMenus(roleId, menuIds);
        return ResultInfo.ok();
    }

    @GetMapping("/{roleId}/org-ids")
    @PreAuthorize("@perm.has('sys:role:list')")
    public ResultInfo<List<String>> orgIds(@PathVariable Long roleId) {
        return ResultInfo.ok(roleService.orgIds(roleId));
    }

    @GetMapping("/{id}/form")
    @PreAuthorize("@perm.has('sys:role:list')")
    public ResultInfo<RoleDtos.RoleForm> form(@PathVariable Long id) {
        return ResultInfo.ok(roleService.form(id));
    }

    @PostMapping
    @PreAuthorize("@perm.has('sys:role:create')")
    public ResultInfo<Void> create(@RequestBody RoleDtos.RoleForm form) {
        roleService.create(form);
        return ResultInfo.ok();
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('sys:role:update')")
    public ResultInfo<Void> update(@PathVariable Long id, @RequestBody RoleDtos.RoleForm form) {
        roleService.update(id, form);
        return ResultInfo.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@perm.has('sys:role:delete')")
    public ResultInfo<Void> delete(@PathVariable String ids) {
        roleService.delete(ids);
        return ResultInfo.ok();
    }
}
