package com.platform.api.system.org;

import java.util.List;

/**
 * 与前端 src/api/system/org/types.ts 对应的数据结构。
 */
public final class OrgDtos {

    private OrgDtos() {
    }

    public record OrgItem(String id, String parentId, String name, Integer sort, Integer status,
                          String treePath, String createTime, String updateTime,
                          List<OrgItem> children) {
    }

    public record OrgForm(String id, String name, String code, String parentId, Integer sort,
                          Integer status) {
    }
}
