package com.platform.api.system.dept;

import com.platform.api.common.ApiResult;
import com.platform.api.common.OptionItem;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/depts")
public class DeptController {

    private final DeptService deptService;

    public DeptController(DeptService deptService) {
        this.deptService = deptService;
    }

    @GetMapping
    @PreAuthorize("@perm.has('sys:dept:list')")
    public ApiResult<List<DeptDtos.DeptItem>> tree(@RequestParam(required = false) String keywords,
                                                   @RequestParam(required = false) Integer status) {
        return ApiResult.ok(deptService.tree(keywords, status));
    }

    @GetMapping("/options")
    public ApiResult<List<OptionItem>> options() {
        return ApiResult.ok(deptService.options());
    }

    @GetMapping("/{id}/form")
    @PreAuthorize("@perm.has('sys:dept:list')")
    public ApiResult<DeptDtos.DeptForm> form(@PathVariable Long id) {
        return ApiResult.ok(deptService.form(id));
    }

    @PostMapping
    @PreAuthorize("@perm.has('sys:dept:create')")
    public ApiResult<Void> create(@RequestBody DeptDtos.DeptForm form) {
        deptService.create(form);
        return ApiResult.ok();
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('sys:dept:update')")
    public ApiResult<Void> update(@PathVariable Long id, @RequestBody DeptDtos.DeptForm form) {
        deptService.update(id, form);
        return ApiResult.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@perm.has('sys:dept:delete')")
    public ApiResult<Void> delete(@PathVariable String ids) {
        deptService.delete(ids);
        return ApiResult.ok();
    }
}
