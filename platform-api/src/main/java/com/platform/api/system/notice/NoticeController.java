package com.platform.api.system.notice;

import com.platform.api.common.ResultInfo;
import org.springframework.data.domain.Page;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notices")
public class NoticeController {

    private final NoticeService noticeService;

    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @GetMapping
    @PreAuthorize("@perm.has('sys:notice:list')")
    public ResultInfo<Page<NoticeDtos.NoticeItem>> page(NoticeDtos.NoticeQuery query,
                                                             @AuthenticationPrincipal LoginUser loginUser) {
        return ResultInfo.ok(noticeService.page(query, loginUser));
    }

    @GetMapping("/my")
    public ResultInfo<Page<NoticeDtos.NoticeItem>> myPage(NoticeDtos.NoticeQuery query,
                                                               @AuthenticationPrincipal LoginUser loginUser) {
        return ResultInfo.ok(noticeService.myPage(query, loginUser));
    }

    @PutMapping("/read-all")
    public ResultInfo<Void> readAll(@AuthenticationPrincipal LoginUser loginUser) {
        noticeService.readAll(loginUser);
        return ResultInfo.ok();
    }

    @GetMapping("/{id}/preview")
    @PreAuthorize("@perm.has('sys:notice:list')")
    public ResultInfo<NoticeDtos.NoticeDetail> preview(@PathVariable Long id) {
        return ResultInfo.ok(noticeService.preview(id));
    }

    @GetMapping("/{id}/form")
    @PreAuthorize("@perm.has('sys:notice:list')")
    public ResultInfo<NoticeDtos.NoticeForm> form(@PathVariable Long id) {
        return ResultInfo.ok(noticeService.form(id));
    }

    @GetMapping("/{id}/detail")
    public ResultInfo<NoticeDtos.NoticeDetail> detail(@PathVariable Long id,
                                                     @AuthenticationPrincipal LoginUser loginUser) {
        return ResultInfo.ok(noticeService.detail(id, loginUser, true));
    }

    @PostMapping
    @PreAuthorize("@perm.has('sys:notice:create')")
    public ResultInfo<Void> create(@RequestBody NoticeDtos.NoticeForm form,
                                  @AuthenticationPrincipal LoginUser loginUser) {
        noticeService.create(form, loginUser);
        return ResultInfo.ok();
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('sys:notice:update')")
    public ResultInfo<Void> update(@PathVariable Long id, @RequestBody NoticeDtos.NoticeForm form) {
        noticeService.update(id, form);
        return ResultInfo.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@perm.has('sys:notice:delete')")
    public ResultInfo<Void> delete(@PathVariable String ids) {
        noticeService.delete(ids);
        return ResultInfo.ok();
    }

    @PutMapping("/{id}/publish")
    @PreAuthorize("@perm.has('sys:notice:publish')")
    public ResultInfo<Void> publish(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        noticeService.publish(id, loginUser);
        return ResultInfo.ok();
    }

    @PutMapping("/{id}/revoke")
    @PreAuthorize("@perm.has('sys:notice:revoke')")
    public ResultInfo<Void> revoke(@PathVariable Long id) {
        noticeService.revoke(id);
        return ResultInfo.ok();
    }
}
