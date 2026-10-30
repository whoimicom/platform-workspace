package com.platform.api.system.dept;

import java.util.List;

/**
 * 与前端 src/api/system/dept/types.ts 对应的数据结构。
 */
public final class DeptDtos {

    private DeptDtos() {
    }

    public record DeptItem(String id, String parentId, String name, Integer sort, Integer status,
                           String treePath, String createTime, String updateTime,
                           List<DeptItem> children) {
    }

    public record DeptForm(String id, String name, String code, String parentId, Integer sort,
                           Integer status) {
    }
}
