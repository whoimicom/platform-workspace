package com.platform.api.gen;

import com.platform.api.common.ResultInfo;
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

/**
 * 代码生成：库内表列表、生成配置维护、代码预览、类型映射维护。
 */
@RestController
@RequestMapping("/api/gen")
public class GenController {

    private final GenService genService;

    public GenController(GenService genService) {
        this.genService = genService;
    }

    /** 当前库内可导入的表 */
    @GetMapping("/db-tables")
    @PreAuthorize("@perm.has('gen:config:list')")
    public ResultInfo<Page<GenDtos.DbTableItem>> dbTables(GenDtos.GenQuery query) {
        return ResultInfo.ok(genService.dbTables(query));
    }

    /** 已保存的生成配置 */
    @GetMapping("/configs")
    @PreAuthorize("@perm.has('gen:config:list')")
    public ResultInfo<Page<GenDtos.GenConfigItem>> configs(GenDtos.GenQuery query) {
        return ResultInfo.ok(genService.configs(query));
    }

    /** 导入表：首次导入生成默认配置，重复导入返回已有配置 */
    @PostMapping("/configs/import/{tableName}")
    @PreAuthorize("@perm.has('gen:config:import')")
    public ResultInfo<Long> importTable(@PathVariable String tableName) {
        return ResultInfo.ok(genService.importTable(tableName));
    }

    /** 配置详情（含字段规则） */
    @GetMapping("/configs/{id}")
    @PreAuthorize("@perm.has('gen:config:list')")
    public ResultInfo<GenDtos.GenConfigDetail> detail(@PathVariable long id) {
        return ResultInfo.ok(genService.detail(id));
    }

    /** 保存配置 */
    @PutMapping("/configs/{id}")
    @PreAuthorize("@perm.has('gen:config:edit')")
    public ResultInfo<Void> update(@PathVariable long id, @RequestBody GenDtos.GenConfigForm form) {
        genService.update(id, form);
        return ResultInfo.ok();
    }

    /** 删除配置 */
    @DeleteMapping("/configs/{ids}")
    @PreAuthorize("@perm.has('gen:config:delete')")
    public ResultInfo<Void> delete(@PathVariable List<Long> ids) {
        genService.delete(ids);
        return ResultInfo.ok();
    }

    /** 预览生成的代码 */
    @GetMapping("/configs/{id}/preview")
    @PreAuthorize("@perm.has('gen:config:preview')")
    public ResultInfo<List<GenDtos.GenFileItem>> preview(@PathVariable long id) {
        return ResultInfo.ok(genService.preview(id));
    }
}
