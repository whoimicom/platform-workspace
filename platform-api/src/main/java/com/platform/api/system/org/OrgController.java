package com.platform.api.system.org;

import com.platform.api.common.ResultInfo;
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
@RequestMapping("/api/orgs")
public class OrgController {

    private final OrgService orgService;

    public OrgController(OrgService orgService) {
        this.orgService = orgService;
    }

    @GetMapping
    @PreAuthorize("@perm.has('sys:org:list')")
    public ResultInfo<List<OrgDtos.OrgItem>> tree(@RequestParam(required = false) String keywords,
                                                  @RequestParam(required = false) Integer status,
                                                  @AuthenticationPrincipal LoginUser loginUser) {
        return ResultInfo.ok(orgService.tree(keywords, status, loginUser));
    }

    @GetMapping("/options")
    public ResultInfo<List<OptionItem>> options(@AuthenticationPrincipal LoginUser loginUser) {
        return ResultInfo.ok(orgService.options(loginUser));
    }

    @GetMapping("/{id}/form")
    @PreAuthorize("@perm.has('sys:org:list')")
    public ResultInfo<OrgDtos.OrgForm> form(@PathVariable Long id,
                                            @AuthenticationPrincipal LoginUser loginUser) {
        return ResultInfo.ok(orgService.form(id, loginUser));
    }

    @PostMapping
    @PreAuthorize("@perm.has('sys:org:create')")
    public ResultInfo<Void> create(@RequestBody OrgDtos.OrgForm form,
                                   @AuthenticationPrincipal LoginUser loginUser) {
        orgService.create(form, loginUser);
        return ResultInfo.ok();
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('sys:org:update')")
    public ResultInfo<Void> update(@PathVariable Long id, @RequestBody OrgDtos.OrgForm form,
                                   @AuthenticationPrincipal LoginUser loginUser) {
        orgService.update(id, form, loginUser);
        return ResultInfo.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@perm.has('sys:org:delete')")
    public ResultInfo<Void> delete(@PathVariable String ids,
                                   @AuthenticationPrincipal LoginUser loginUser) {
        orgService.delete(ids, loginUser);
        return ResultInfo.ok();
    }
}
