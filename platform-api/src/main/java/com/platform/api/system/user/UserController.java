package com.platform.api.system.user;

import com.platform.api.common.ApiResult;
import com.platform.api.common.OptionItem;
import com.platform.api.common.PageResult;
import com.platform.api.security.LoginUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ApiResult<UserDtos.UserInfo> me(@AuthenticationPrincipal LoginUser loginUser) {
        return ApiResult.ok(userService.me(loginUser));
    }

    @GetMapping
    @PreAuthorize("@perm.has('sys:user:list')")
    public ApiResult<PageResult<UserDtos.UserItem>> page(UserDtos.UserQuery query) {
        return ApiResult.ok(userService.page(query));
    }

    @GetMapping("/options")
    public ApiResult<List<OptionItem>> options() {
        return ApiResult.ok(userService.options());
    }

    @GetMapping("/{id}/form")
    @PreAuthorize("@perm.has('sys:user:list')")
    public ApiResult<UserDtos.UserForm> form(@PathVariable Long id) {
        return ApiResult.ok(userService.form(id));
    }

    @PostMapping
    @PreAuthorize("@perm.has('sys:user:create')")
    public ApiResult<Void> create(@RequestBody UserDtos.UserForm form) {
        userService.create(form);
        return ApiResult.ok();
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('sys:user:update')")
    public ApiResult<Void> update(@PathVariable Long id, @RequestBody UserDtos.UserForm form) {
        userService.update(id, form);
        return ApiResult.ok();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("@perm.has('sys:user:update')")
    public ApiResult<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        userService.updateStatus(id, status);
        return ApiResult.ok();
    }

    @PutMapping("/{id}/password/reset")
    @PreAuthorize("@perm.has('sys:user:reset-password')")
    public ApiResult<Void> resetPassword(@PathVariable Long id,
                                         @RequestBody UserDtos.PasswordResetRequest request) {
        userService.resetPassword(id, request.password());
        return ApiResult.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@perm.has('sys:user:delete')")
    public ApiResult<Void> delete(@PathVariable String ids) {
        userService.delete(ids);
        return ApiResult.ok();
    }
}
