package com.platform.api.system.menu;

import java.util.List;

/**
 * 与前端 src/api/system/menu/types.ts 对应的数据结构。
 */
public final class MenuDtos {

    private MenuDtos() {
    }

    public record ParamItem(String key, String value) {
    }

    public record MenuItem(String id, String parentId, String name, String type, String path,
                           String routePath, String routeName, String redirect, String component,
                           String externalUrl, String icon, Integer sort, Integer visible, Integer scope,
                           Integer keepAlive, String perm, List<ParamItem> params,
                           String orgId, List<MenuItem> children) {
    }

    public record MenuForm(String id, String parentId, String name, String type, String path,
                           String routePath, String routeName, String redirect, String component,
                           String externalUrl, String icon, Integer sort, Integer visible, Integer scope,
                           Integer keepAlive, String perm, List<ParamItem> params,
                           Boolean generateCrudButtons, String buttonPermPrefix, String orgId) {
    }

    public record RouteMeta(String title, String icon, Boolean hidden, Boolean keepAlive,
                            String externalUrl, List<String> roles) {
    }

    public record RouteItem(String path, String name, String component, String redirect,
                            RouteMeta meta, List<RouteItem> children) {
    }
}
