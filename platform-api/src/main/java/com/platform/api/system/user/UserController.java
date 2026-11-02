package com.platform.api.system.user;

import com.platform.api.common.ResultInfo;
import com.platform.api.common.OptionItem;
import org.springframework.data.domain.Page;
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
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResultInfo<UserDtos.UserInfo> me(@AuthenticationPrincipal LoginUser loginUser) {
        return ResultInfo.ok(userService.me(loginUser));
    }

    @GetMapping
    @PreAuthorize("@perm.has('sys:user:list')")
    public ResultInfo<Page<UserDtos.UserItem>> page(UserDtos.UserQuery query,
                                                    @AuthenticationPrincipal LoginUser loginUser) {
        return ResultInfo.ok(userService.page(query, loginUser));
    }

    @GetMapping("/options")
    public ResultInfo<List<OptionItem>> options() {
        return ResultInfo.ok(userService.options());
    }

    @GetMapping("/{id}/form")
    @PreAuthorize("@perm.has('sys:user:list')")
    public ResultInfo<UserDtos.UserForm> form(@PathVariable Long id,
                                              @AuthenticationPrincipal LoginUser loginUser) {
        return ResultInfo.ok(userService.form(id, loginUser));
    }

    @PostMapping
    @PreAuthorize("@perm.has('sys:user:create')")
    public ResultInfo<Void> create(@RequestBody UserDtos.UserForm form,
                                   @AuthenticationPrincipal LoginUser loginUser) {
        userService.create(form, loginUser);
        return ResultInfo.ok();
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('sys:user:update')")
    public ResultInfo<Void> update(@PathVariable Long id, @RequestBody UserDtos.UserForm form,
                                   @AuthenticationPrincipal LoginUser loginUser) {
        userService.update(id, form, loginUser);
        return ResultInfo.ok();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("@perm.has('sys:user:update')")
    public ResultInfo<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status,
                                         @AuthenticationPrincipal LoginUser loginUser) {
        userService.updateStatus(id, status, loginUser);
        return ResultInfo.ok();
    }

    @PutMapping("/{id}/password/reset")
    @PreAuthorize("@perm.has('sys:user:reset-password')")
    public ResultInfo<Void> resetPassword(@PathVariable Long id,
                                         @RequestBody UserDtos.PasswordResetRequest request,
                                         @AuthenticationPrincipal LoginUser loginUser) {
        userService.resetPassword(id, request.password(), loginUser);
        return ResultInfo.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@perm.has('sys:user:delete')")
    public ResultInfo<Void> delete(@PathVariable String ids,
                                   @AuthenticationPrincipal LoginUser loginUser) {
        userService.delete(ids, loginUser);
        return ResultInfo.ok();
    }
}
