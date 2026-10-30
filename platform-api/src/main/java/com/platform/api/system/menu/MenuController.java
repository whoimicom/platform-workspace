package com.platform.api.system.menu;

import com.platform.api.common.ApiResult;
import com.platform.api.common.OptionItem;
import com.platform.api.security.LoginUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/menus")
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping("/routes")
    public ApiResult<List<MenuDtos.RouteItem>> routes(@AuthenticationPrincipal LoginUser loginUser) {
        return ApiResult.ok(menuService.routes(loginUser));
    }

    @GetMapping
    @PreAuthorize("@perm.has('sys:menu:list')")
    public ApiResult<List<MenuDtos.MenuItem>> tree(@RequestParam(required = false) String keywords) {
        return ApiResult.ok(menuService.tree(keywords));
    }

    @GetMapping("/options")
    public ApiResult<List<OptionItem>> options(@RequestParam(required = false) List<String> types) {
        return ApiResult.ok(menuService.options(types));
    }

    @GetMapping("/{id}/form")
    @PreAuthorize("@perm.has('sys:menu:list')")
    public ApiResult<MenuDtos.MenuForm> form(@PathVariable Long id) {
        return ApiResult.ok(menuService.form(id));
    }

    @PostMapping
    @PreAuthorize("@perm.has('sys:menu:create')")
    public ApiResult<Void> create(@RequestBody MenuDtos.MenuForm form) {
        menuService.create(form);
        return ApiResult.ok();
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('sys:menu:update')")
    public ApiResult<Void> update(@PathVariable Long id, @RequestBody MenuDtos.MenuForm form) {
        menuService.update(id, form);
        return ApiResult.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('sys:menu:delete')")
    public ApiResult<Void> delete(@PathVariable Long id) {
        menuService.delete(id);
        return ApiResult.ok();
    }
}
