package com.platform.api.system.notice;

import com.platform.api.common.ApiResult;
import com.platform.api.common.PageResult;
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
@RequestMapping("/api/v1/notices")
public class NoticeController {

    private final NoticeService noticeService;

    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @GetMapping
    @PreAuthorize("@perm.has('sys:notice:list')")
    public ApiResult<PageResult<NoticeDtos.NoticeItem>> page(NoticeDtos.NoticeQuery query,
                                                             @AuthenticationPrincipal LoginUser loginUser) {
        return ApiResult.ok(noticeService.page(query, loginUser));
    }

    @GetMapping("/my")
    public ApiResult<PageResult<NoticeDtos.NoticeItem>> myPage(NoticeDtos.NoticeQuery query,
                                                               @AuthenticationPrincipal LoginUser loginUser) {
        return ApiResult.ok(noticeService.myPage(query, loginUser));
    }

    @PutMapping("/read-all")
    public ApiResult<Void> readAll(@AuthenticationPrincipal LoginUser loginUser) {
        noticeService.readAll(loginUser);
        return ApiResult.ok();
    }

    @GetMapping("/{id}/preview")
    @PreAuthorize("@perm.has('sys:notice:list')")
    public ApiResult<NoticeDtos.NoticeDetail> preview(@PathVariable Long id) {
        return ApiResult.ok(noticeService.preview(id));
    }

    @GetMapping("/{id}/form")
    @PreAuthorize("@perm.has('sys:notice:list')")
    public ApiResult<NoticeDtos.NoticeForm> form(@PathVariable Long id) {
        return ApiResult.ok(noticeService.form(id));
    }

    @GetMapping("/{id}/detail")
    public ApiResult<NoticeDtos.NoticeDetail> detail(@PathVariable Long id,
                                                     @AuthenticationPrincipal LoginUser loginUser) {
        return ApiResult.ok(noticeService.detail(id, loginUser, true));
    }

    @PostMapping
    @PreAuthorize("@perm.has('sys:notice:create')")
    public ApiResult<Void> create(@RequestBody NoticeDtos.NoticeForm form,
                                  @AuthenticationPrincipal LoginUser loginUser) {
        noticeService.create(form, loginUser);
        return ApiResult.ok();
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('sys:notice:update')")
    public ApiResult<Void> update(@PathVariable Long id, @RequestBody NoticeDtos.NoticeForm form) {
        noticeService.update(id, form);
        return ApiResult.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@perm.has('sys:notice:delete')")
    public ApiResult<Void> delete(@PathVariable String ids) {
        noticeService.delete(ids);
        return ApiResult.ok();
    }

    @PutMapping("/{id}/publish")
    @PreAuthorize("@perm.has('sys:notice:publish')")
    public ApiResult<Void> publish(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        noticeService.publish(id, loginUser);
        return ApiResult.ok();
    }

    @PutMapping("/{id}/revoke")
    @PreAuthorize("@perm.has('sys:notice:revoke')")
    public ApiResult<Void> revoke(@PathVariable Long id) {
        noticeService.revoke(id);
        return ApiResult.ok();
    }
}
