package com.platform.api.auth;

import com.platform.api.common.ResultInfo;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登录/改密/刷新令牌已统一收口到认证中心（platform-rs-auth），
 * 这里仅保留登出接口供前端清理本地会话。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @DeleteMapping("/logout")
    public ResultInfo<Void> logout() {
        return ResultInfo.ok();
    }
}
