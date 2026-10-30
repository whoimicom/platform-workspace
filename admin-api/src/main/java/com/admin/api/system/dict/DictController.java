package com.admin.api.system.dict;

import com.admin.api.common.ApiResult;
import com.admin.api.common.OptionItem;
import com.admin.api.common.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dicts")
public class DictController {

    private final DictService dictService;

    public DictController(DictService dictService) {
        this.dictService = dictService;
    }

    @GetMapping
    @PreAuthorize("@perm.has('sys:dict:list')")
    public ApiResult<PageResult<DictDtos.DictTypeItem>> page(DictDtos.DictTypeQuery query) {
        return ApiResult.ok(dictService.typePage(query));
    }

    @GetMapping("/options")
    public ApiResult<List<OptionItem>> options() {
        return ApiResult.ok(dictService.typeOptions());
    }

    @GetMapping("/{id}/form")
    @PreAuthorize("@perm.has('sys:dict:list')")
    public ApiResult<DictDtos.DictTypeForm> form(@PathVariable Long id) {
        return ApiResult.ok(dictService.typeForm(id));
    }

    @PostMapping
    @PreAuthorize("@perm.has('sys:dict:create')")
    public ApiResult<Void> create(@RequestBody DictDtos.DictTypeForm form) {
        dictService.createType(form);
        return ApiResult.ok();
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('sys:dict:update')")
    public ApiResult<Void> update(@PathVariable Long id, @RequestBody DictDtos.DictTypeForm form) {
        dictService.updateType(id, form);
        return ApiResult.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@perm.has('sys:dict:delete')")
    public ApiResult<Void> delete(@PathVariable String ids) {
        dictService.deleteTypes(ids);
        return ApiResult.ok();
    }

    @GetMapping("/{dictCode}/items")
    @PreAuthorize("@perm.has('sys:dict-item:list')")
    public ApiResult<PageResult<DictDtos.DictItem>> itemPage(@PathVariable String dictCode,
                                                             DictDtos.DictItemQuery query) {
        return ApiResult.ok(dictService.itemPage(dictCode, query));
    }

    @GetMapping("/{dictCode}/items/options")
    public ApiResult<List<DictDtos.DictItemOption>> itemOptions(@PathVariable String dictCode) {
        return ApiResult.ok(dictService.itemOptions(dictCode));
    }

    @GetMapping("/{dictCode}/items/{id}/form")
    @PreAuthorize("@perm.has('sys:dict-item:list')")
    public ApiResult<DictDtos.DictItemForm> itemForm(@PathVariable String dictCode, @PathVariable Long id) {
        return ApiResult.ok(dictService.itemForm(dictCode, id));
    }

    @PostMapping("/{dictCode}/items")
    @PreAuthorize("@perm.has('sys:dict-item:create')")
    public ApiResult<Void> createItem(@PathVariable String dictCode,
                                      @RequestBody DictDtos.DictItemForm form) {
        dictService.createItem(dictCode, form);
        return ApiResult.ok();
    }

    @PutMapping("/{dictCode}/items/{id}")
    @PreAuthorize("@perm.has('sys:dict-item:update')")
    public ApiResult<Void> updateItem(@PathVariable String dictCode, @PathVariable Long id,
                                      @RequestBody DictDtos.DictItemForm form) {
        dictService.updateItem(dictCode, id, form);
        return ApiResult.ok();
    }

    @DeleteMapping("/{dictCode}/items/{ids}")
    @PreAuthorize("@perm.has('sys:dict-item:delete')")
    public ApiResult<Void> deleteItems(@PathVariable String dictCode, @PathVariable String ids) {
        dictService.deleteItems(dictCode, ids);
        return ApiResult.ok();
    }
}
