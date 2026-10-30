package com.platform.api.system.dict;

import com.platform.api.common.ResultInfo;
import com.platform.api.common.OptionItem;
import org.springframework.data.domain.Page;
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
@RequestMapping("/api/dicts")
public class DictController {

    private final DictService dictService;

    public DictController(DictService dictService) {
        this.dictService = dictService;
    }

    @GetMapping
    @PreAuthorize("@perm.has('sys:dict:list')")
    public ResultInfo<Page<DictDtos.DictTypeItem>> page(DictDtos.DictTypeQuery query) {
        return ResultInfo.ok(dictService.typePage(query));
    }

    @GetMapping("/options")
    public ResultInfo<List<OptionItem>> options() {
        return ResultInfo.ok(dictService.typeOptions());
    }

    @GetMapping("/{id}/form")
    @PreAuthorize("@perm.has('sys:dict:list')")
    public ResultInfo<DictDtos.DictTypeForm> form(@PathVariable Long id) {
        return ResultInfo.ok(dictService.typeForm(id));
    }

    @PostMapping
    @PreAuthorize("@perm.has('sys:dict:create')")
    public ResultInfo<Void> create(@RequestBody DictDtos.DictTypeForm form) {
        dictService.createType(form);
        return ResultInfo.ok();
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('sys:dict:update')")
    public ResultInfo<Void> update(@PathVariable Long id, @RequestBody DictDtos.DictTypeForm form) {
        dictService.updateType(id, form);
        return ResultInfo.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@perm.has('sys:dict:delete')")
    public ResultInfo<Void> delete(@PathVariable String ids) {
        dictService.deleteTypes(ids);
        return ResultInfo.ok();
    }

    @GetMapping("/{dictCode}/items")
    @PreAuthorize("@perm.has('sys:dict-item:list')")
    public ResultInfo<Page<DictDtos.DictItem>> itemPage(@PathVariable String dictCode,
                                                             DictDtos.DictItemQuery query) {
        return ResultInfo.ok(dictService.itemPage(dictCode, query));
    }

    @GetMapping("/{dictCode}/items/options")
    public ResultInfo<List<DictDtos.DictItemOption>> itemOptions(@PathVariable String dictCode) {
        return ResultInfo.ok(dictService.itemOptions(dictCode));
    }

    @GetMapping("/{dictCode}/items/{id}/form")
    @PreAuthorize("@perm.has('sys:dict-item:list')")
    public ResultInfo<DictDtos.DictItemForm> itemForm(@PathVariable String dictCode, @PathVariable Long id) {
        return ResultInfo.ok(dictService.itemForm(dictCode, id));
    }

    @PostMapping("/{dictCode}/items")
    @PreAuthorize("@perm.has('sys:dict-item:create')")
    public ResultInfo<Void> createItem(@PathVariable String dictCode,
                                      @RequestBody DictDtos.DictItemForm form) {
        dictService.createItem(dictCode, form);
        return ResultInfo.ok();
    }

    @PutMapping("/{dictCode}/items/{id}")
    @PreAuthorize("@perm.has('sys:dict-item:update')")
    public ResultInfo<Void> updateItem(@PathVariable String dictCode, @PathVariable Long id,
                                      @RequestBody DictDtos.DictItemForm form) {
        dictService.updateItem(dictCode, id, form);
        return ResultInfo.ok();
    }

    @DeleteMapping("/{dictCode}/items/{ids}")
    @PreAuthorize("@perm.has('sys:dict-item:delete')")
    public ResultInfo<Void> deleteItems(@PathVariable String dictCode, @PathVariable String ids) {
        dictService.deleteItems(dictCode, ids);
        return ResultInfo.ok();
    }
}
