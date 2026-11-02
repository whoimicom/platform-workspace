package com.platform.api.system.menu;

import com.platform.api.common.BusinessException;
import com.platform.api.common.IdUtils;
import com.platform.api.common.JdbcUtil;
import com.platform.api.common.OptionItem;
import com.platform.api.security.LoginUser;
import com.platform.api.system.org.OrgScope;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class MenuService {

    private static final String LAYOUT_COMPONENT = "Layout";

    private final NamedParameterJdbcTemplate jdbc;
    private final OrgScope orgScope;

    public MenuService(NamedParameterJdbcTemplate jdbc, OrgScope orgScope) {
        this.jdbc = jdbc;
        this.orgScope = orgScope;
    }

    public List<MenuDtos.MenuItem> tree(String keywords, LoginUser loginUser) {
        List<MenuRow> rows = filterScope(loadRows(), loginUser);
        java.util.function.Predicate<MenuRow> predicate = row -> keywords == null || keywords.isBlank()
                || (row.name() != null && row.name().contains(keywords.trim()));
        return build(rows, 0L, predicate);
    }

    public List<OptionItem> options(List<String> types, LoginUser loginUser) {
        Set<String> allowed = new LinkedHashSet<>();
        if (types != null) {
            for (String type : types) {
                if (type == null) {
                    continue;
                }
                for (String part : type.split(",")) {
                    if (!part.isBlank()) {
                        allowed.add(part.trim());
                    }
                }
            }
        }
        List<MenuRow> rows = filterScope(loadRows(), loginUser);
        if (!allowed.isEmpty()) {
            rows = rows.stream().filter(row -> allowed.contains(row.type())).toList();
        }
        return buildOptions(rows, 0L);
    }

    public MenuDtos.MenuForm form(Long id, LoginUser loginUser) {
        MenuRow row = loadRows().stream().filter(item -> item.id() == id).findFirst()
                .orElseThrow(() -> BusinessException.notFound("菜单不存在"));
        checkScope(loginUser, row.orgId());
        List<MenuDtos.ParamItem> params = parseParams(row.params());
        return new MenuDtos.MenuForm(String.valueOf(row.id()), String.valueOf(row.parentId()),
                row.name(), row.type(), row.path(), row.path(), row.routeName(), row.redirect(),
                row.component(), row.externalUrl(), row.icon(), row.sort(), row.visible(), row.scope(),
                row.keepAlive(), row.perm(), params, null, null, String.valueOf(row.orgId()));
    }

    @Transactional
    public void create(MenuDtos.MenuForm form, LoginUser loginUser) {
        validate(form);
        long parentId = resolveParentId(form.parentId());
        ensureParentExists(parentId);
        long orgId = resolveOrgId(form, loginUser);

        String type = form.type().trim().toUpperCase();
        String path = resolvePath(form);
        long menuId = insertMenu(parentId, form.name().trim(), type, path, form.routeName(),
                form.redirect(), form.component(), form.externalUrl(), form.icon(),
                form.sort() == null ? 1 : form.sort(), form.visible() == null ? 1 : form.visible(),
                form.scope() == null ? 1 : form.scope(), form.keepAlive(), form.perm(),
                serializeParams(form.params()), orgId);

        if (Boolean.TRUE.equals(form.generateCrudButtons()) && "M".equals(type)
                && form.buttonPermPrefix() != null && !form.buttonPermPrefix().isBlank()) {
            String prefix = form.buttonPermPrefix().trim().replaceAll(":+$", "");
            String[][] buttons = {
                    {"查询", "list"}, {"新增", "create"}, {"修改", "update"}, {"删除", "delete"}};
            for (int index = 0; index < buttons.length; index++) {
                insertMenu(menuId, form.name().trim() + buttons[index][0], "B", null, null, null,
                        null, null, null, index + 1, 1, form.scope() == null ? 1 : form.scope(), null,
                        prefix + ":" + buttons[index][1], null, orgId);
            }
        }
    }

    @Transactional
    public void update(Long id, MenuDtos.MenuForm form, LoginUser loginUser) {
        List<MenuRow> rows = loadRows();
        MenuRow current = rows.stream().filter(item -> item.id() == id).findFirst()
                .orElseThrow(() -> BusinessException.notFound("菜单不存在"));
        checkScope(loginUser, current.orgId());
        validate(form);

        long parentId = resolveParentId(form.parentId());
        ensureParentExists(parentId);
        if (parentId != 0L && (parentId == id || isDescendant(rows, id, parentId))) {
            throw BusinessException.badRequest("上级菜单不能是自身或子级");
        }
        long orgId = resolveOrgId(form, loginUser);

        String type = form.type().trim().toUpperCase();
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("parentId", parentId);
        params.addValue("name", form.name().trim());
        params.addValue("type", type);
        params.addValue("path", resolvePath(form));
        params.addValue("routeName", form.routeName());
        params.addValue("redirect", form.redirect());
        params.addValue("component", form.component());
        params.addValue("externalUrl", form.externalUrl());
        params.addValue("icon", form.icon());
        params.addValue("sort", form.sort() == null ? current.sort() : form.sort());
        params.addValue("visible", form.visible() == null ? current.visible() : form.visible());
        params.addValue("scope", form.scope() == null ? current.scope() : form.scope());
        params.addValue("keepAlive", form.keepAlive());
        params.addValue("perm", form.perm());
        params.addValue("params", serializeParams(form.params()));
        params.addValue("orgId", orgId);
        params.addValue("updateTime", LocalDateTime.now());
        params.addValue("id", id);

        jdbc.update("UPDATE sys_menu SET parent_id = :parentId, name = :name, type = :type, path = :path, "
                + "route_name = :routeName, redirect = :redirect, component = :component, "
                + "external_url = :externalUrl, icon = :icon, sort = :sort, visible = :visible, "
                + "scope = :scope, keep_alive = :keepAlive, perm = :perm, params = :params, "
                + "org_id = :orgId, update_time = :updateTime WHERE id = :id", params);
    }

    @Transactional
    public void delete(Long id, LoginUser loginUser) {
        List<MenuRow> rows = loadRows();
        MenuRow target = rows.stream().filter(item -> item.id() == id).findFirst()
                .orElseThrow(() -> BusinessException.notFound("菜单不存在"));
        checkScope(loginUser, target.orgId());
        Set<Long> ids = new LinkedHashSet<>();
        collectSubtree(rows, id, ids);
        jdbc.update("DELETE FROM sys_role_menu WHERE menu_id IN (:ids)", Map.of("ids", ids));
        jdbc.update("DELETE FROM sys_menu WHERE id IN (:ids)", Map.of("ids", ids));
    }

    public List<MenuDtos.RouteItem> routes(LoginUser loginUser) {
        List<MenuRow> rows = loadRows();
        if (!loginUser.isRoot()) {
            Set<Long> allowed = new LinkedHashSet<>(jdbc.queryForList(
                    "SELECT DISTINCT rm.menu_id FROM sys_role_menu rm "
                            + "JOIN sys_role r ON r.id = rm.role_id "
                            + "JOIN sys_user_role ur ON ur.role_id = r.id "
                            + "WHERE ur.user_id = :userId AND r.status = 1",
                    Map.of("userId", loginUser.userId()), Long.class));
            rows = rows.stream().filter(row -> allowed.contains(row.id())).toList();
        }
        List<MenuRow> routeRows = rows.stream()
                .filter(row -> !"B".equals(row.type()))
                .toList();
        return buildRoutes(routeRows, 0L);
    }

    /**
     * 非 ROOT 仅能管理可见架构子树内的菜单；org_id=0 的全局菜单仅 ROOT 可见可管。
     */
    private List<MenuRow> filterScope(List<MenuRow> rows, LoginUser loginUser) {
        Set<Long> ids = orgScope.subtreeIds(orgScope.scopeOf(loginUser));
        if (ids == null) {
            return rows;
        }
        return rows.stream().filter(row -> ids.contains(row.orgId())).toList();
    }

    private void checkScope(LoginUser loginUser, long orgId) {
        OrgScope.Scope scope = orgScope.scopeOf(loginUser);
        if (scope == null) {
            return;
        }
        if (!orgScope.subtreeIds(scope).contains(orgId)) {
            throw BusinessException.forbidden("无权操作该菜单");
        }
    }

    /**
     * 解析菜单归属架构：非 ROOT 必须选择可见范围内的架构，不允许全局（0）。
     */
    private long resolveOrgId(MenuDtos.MenuForm form, LoginUser loginUser) {
        Long parsed = IdUtils.parseLong(form.orgId());
        long orgId = parsed == null ? 0L : parsed;
        if (loginUser == null || loginUser.isRoot()) {
            return orgId;
        }
        if (orgId == 0L) {
            throw BusinessException.badRequest("请选择归属架构");
        }
        orgScope.check(loginUser, orgId);
        return orgId;
    }

    private long insertMenu(long parentId, String name, String type, String path, String routeName,
                            String redirect, String component, String externalUrl, String icon,
                            Integer sort, Integer visible, Integer scope, Integer keepAlive,
                            String perm, String params, long orgId) {
        MapSqlParameterSource source = new MapSqlParameterSource();
        source.addValue("parentId", parentId);
        source.addValue("name", name);
        source.addValue("type", type);
        source.addValue("path", path);
        source.addValue("routeName", routeName);
        source.addValue("redirect", redirect);
        source.addValue("component", component);
        source.addValue("externalUrl", externalUrl);
        source.addValue("icon", icon);
        source.addValue("sort", sort);
        source.addValue("visible", visible);
        source.addValue("scope", scope);
        source.addValue("keepAlive", keepAlive);
        source.addValue("perm", perm);
        source.addValue("params", params);
        source.addValue("orgId", orgId);
        source.addValue("createTime", LocalDateTime.now());
        source.addValue("updateTime", LocalDateTime.now());

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update("INSERT INTO sys_menu (parent_id, name, type, path, route_name, redirect, component, "
                        + "external_url, icon, sort, visible, scope, keep_alive, perm, params, org_id, create_time, update_time) "
                        + "VALUES (:parentId, :name, :type, :path, :routeName, :redirect, :component, "
                        + ":externalUrl, :icon, :sort, :visible, :scope, :keepAlive, :perm, :params, "
                        + ":orgId, :createTime, :updateTime)", source, keyHolder, new String[]{"id"});
        Number key = keyHolder.getKey();
        return key == null ? 0L : key.longValue();
    }

    private String resolvePath(MenuDtos.MenuForm form) {
        if (form.routePath() != null && !form.routePath().isBlank()) {
            return form.routePath().trim();
        }
        if (form.path() != null && !form.path().isBlank()) {
            return form.path().trim();
        }
        return null;
    }

    private long resolveParentId(String parentId) {
        Long parsed = IdUtils.parseLong(parentId);
        return parsed == null ? 0L : parsed;
    }

    private void ensureParentExists(long parentId) {
        if (parentId == 0L) {
            return;
        }
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM sys_menu WHERE id = :id",
                Map.of("id", parentId), Long.class);
        if (count == null || count == 0) {
            throw BusinessException.badRequest("上级菜单不存在");
        }
    }

    private boolean isDescendant(List<MenuRow> rows, long ancestorId, long candidateId) {
        Set<Long> ids = new LinkedHashSet<>();
        collectSubtree(rows, ancestorId, ids);
        return ids.contains(candidateId);
    }

    private void collectSubtree(List<MenuRow> rows, long rootId, Set<Long> target) {
        if (!target.add(rootId)) {
            return;
        }
        rows.stream().filter(row -> row.parentId() == rootId)
                .forEach(row -> collectSubtree(rows, row.id(), target));
    }

    private List<MenuDtos.MenuItem> build(List<MenuRow> rows, long parentId,
                                          java.util.function.Predicate<MenuRow> predicate) {
        Map<Long, List<MenuRow>> grouped = new HashMap<>();
        rows.forEach(row -> grouped.computeIfAbsent(row.parentId(), key -> new ArrayList<>()).add(row));

        List<MenuDtos.MenuItem> result = new ArrayList<>();
        for (MenuRow row : grouped.getOrDefault(parentId, List.of())) {
            List<MenuDtos.MenuItem> children = build(rows, row.id(), predicate);
            if (predicate.test(row) || !children.isEmpty()) {
                result.add(toItem(row, children));
            }
        }
        result.sort(Comparator.comparing(item -> item.sort() == null ? 0 : item.sort()));
        return result;
    }

    private MenuDtos.MenuItem toItem(MenuRow row, List<MenuDtos.MenuItem> children) {
        return new MenuDtos.MenuItem(String.valueOf(row.id()), String.valueOf(row.parentId()), row.name(),
                row.type(), row.path(), row.path(), row.routeName(), row.redirect(), row.component(),
                row.externalUrl(), row.icon(), row.sort(), row.visible(), row.scope(), row.keepAlive(),
                row.perm(), parseParams(row.params()), String.valueOf(row.orgId()), children);
    }

    private List<OptionItem> buildOptions(List<MenuRow> rows, long parentId) {
        List<OptionItem> result = new ArrayList<>();
        for (MenuRow row : rows) {
            if (row.parentId() != parentId) {
                continue;
            }
            result.add(new OptionItem(String.valueOf(row.id()), row.name(), buildOptions(rows, row.id())));
        }
        return result;
    }

    private List<MenuDtos.RouteItem> buildRoutes(List<MenuRow> rows, long parentId) {
        Map<Long, List<MenuRow>> grouped = new HashMap<>();
        rows.forEach(row -> grouped.computeIfAbsent(row.parentId(), key -> new ArrayList<>()).add(row));

        List<MenuDtos.RouteItem> result = new ArrayList<>();
        for (MenuRow row : grouped.getOrDefault(parentId, List.of())) {
            result.add(toRoute(row, buildRoutes(rows, row.id())));
        }
        return result;
    }

    private MenuDtos.RouteItem toRoute(MenuRow row, List<MenuDtos.RouteItem> children) {
        boolean keepAlive = row.keepAlive() != null && row.keepAlive() == 1;
        boolean hidden = row.visible() != null && row.visible() == 0;
        MenuDtos.RouteMeta meta = new MenuDtos.RouteMeta(row.name(), row.icon(), hidden ? Boolean.TRUE : null,
                keepAlive ? Boolean.TRUE : null, null, null);

        if ("E".equals(row.type())) {
            String externalUrl = row.externalUrl() == null ? "" : row.externalUrl();
            MenuDtos.RouteMeta externalMeta = new MenuDtos.RouteMeta(row.name(), row.icon(),
                    hidden ? Boolean.TRUE : null, keepAlive ? Boolean.TRUE : null, externalUrl, null);
            return new MenuDtos.RouteItem(externalUrl, null, "", null, externalMeta, children);
        }

        if (row.component() == null || row.component().isBlank() || LAYOUT_COMPONENT.equals(row.component())) {
            return new MenuDtos.RouteItem(row.path(), row.path(), LAYOUT_COMPONENT, row.redirect(),
                    meta, children);
        }

        return new MenuDtos.RouteItem(row.path(), row.routeName(), row.component(), null, meta, children);
    }

    /**
     * 路由参数以 key=value&key=value（URL 编码）形式存储，避免额外引入 JSON 序列化依赖。
     */
    private List<MenuDtos.ParamItem> parseParams(String stored) {
        List<MenuDtos.ParamItem> result = new ArrayList<>();
        if (stored == null || stored.isBlank()) {
            return result;
        }
        for (String pair : stored.split("&")) {
            int index = pair.indexOf('=');
            if (index <= 0) {
                continue;
            }
            String key = URLDecoder.decode(pair.substring(0, index), StandardCharsets.UTF_8);
            String value = URLDecoder.decode(pair.substring(index + 1), StandardCharsets.UTF_8);
            result.add(new MenuDtos.ParamItem(key, value));
        }
        return result;
    }

    private String serializeParams(List<MenuDtos.ParamItem> params) {
        if (params == null || params.isEmpty()) {
            return null;
        }
        List<String> pairs = new ArrayList<>();
        for (MenuDtos.ParamItem param : params) {
            if (param == null || param.key() == null || param.key().isBlank()) {
                continue;
            }
            String value = param.value() == null ? "" : param.value();
            pairs.add(URLEncoder.encode(param.key(), StandardCharsets.UTF_8) + "="
                    + URLEncoder.encode(value, StandardCharsets.UTF_8));
        }
        return pairs.isEmpty() ? null : String.join("&", pairs);
    }

    private void validate(MenuDtos.MenuForm form) {
        if (form.name() == null || form.name().isBlank()) {
            throw BusinessException.badRequest("菜单名称不能为空");
        }
        if (form.type() == null || form.type().isBlank()) {
            throw BusinessException.badRequest("菜单类型不能为空");
        }
    }

    private List<MenuRow> loadRows() {
        return jdbc.query("SELECT id, parent_id, name, type, path, route_name, redirect, component, "
                        + "external_url, icon, sort, visible, scope, keep_alive, params, perm, org_id FROM sys_menu "
                        + "ORDER BY sort ASC, id ASC",
                new HashMap<>(), (rs, rowNum) -> new MenuRow(
                        rs.getLong("id"),
                        rs.getLong("parent_id"),
                        rs.getString("name"),
                        rs.getString("type"),
                        rs.getString("path"),
                        rs.getString("route_name"),
                        rs.getString("redirect"),
                        rs.getString("component"),
                        rs.getString("external_url"),
                        rs.getString("icon"),
                        JdbcUtil.getInt(rs, "sort"),
                        JdbcUtil.getInt(rs, "visible"),
                        JdbcUtil.getInt(rs, "scope"),
                        JdbcUtil.getInt(rs, "keep_alive"),
                        rs.getString("params"),
                        rs.getString("perm"),
                        rs.getLong("org_id")));
    }

    private record MenuRow(long id, long parentId, String name, String type, String path,
                           String routeName, String redirect, String component, String externalUrl,
                           String icon, Integer sort, Integer visible, Integer scope, Integer keepAlive,
                           String params, String perm, long orgId) {
    }
}
