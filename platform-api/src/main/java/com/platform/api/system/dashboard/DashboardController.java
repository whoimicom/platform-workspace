package com.platform.api.system.dashboard;

import com.platform.api.common.ResultInfo;
import com.platform.api.security.LoginUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 首页统计接口（登录即可访问）。
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/stats")
    public ResultInfo<DashboardDtos.Stats> stats(@AuthenticationPrincipal LoginUser loginUser) {
        return ResultInfo.ok(dashboardService.stats(loginUser));
    }

    @GetMapping("/user-trend")
    public ResultInfo<DashboardDtos.UserTrend> userTrend(@RequestParam(defaultValue = "7") int days) {
        return ResultInfo.ok(dashboardService.userTrend(days));
    }
}
