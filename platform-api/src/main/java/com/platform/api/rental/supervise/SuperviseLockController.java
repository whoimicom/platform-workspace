package com.platform.api.rental.supervise;

import com.platform.api.common.ResultInfo;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 设备锁查询，权限 rental:supervise:list（菜单 202 / 按钮 20201）。
 */
@RestController
@RequestMapping("/api/rental/supervise-locks")
public class SuperviseLockController {

    private final SuperviseLockService superviseLockService;

    public SuperviseLockController(SuperviseLockService superviseLockService) {
        this.superviseLockService = superviseLockService;
    }

    @GetMapping
    @PreAuthorize("@perm.has('rental:supervise:list')")
    public ResultInfo<Page<SuperviseLockEntity>> page(SuperviseLockQuery query) {
        return ResultInfo.ok(superviseLockService.page(query));
    }
}
