package com.platform.api.system.notice;

import com.platform.api.common.BusinessException;
import com.platform.api.common.IdUtils;
import com.platform.api.common.JdbcUtil;
import com.platform.api.common.PageResult;
import com.platform.api.security.LoginUser;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Service
public class NoticeService {

    private static final int STATUS_DRAFT = 0;
    private static final int STATUS_PUBLISHED = 1;
    private static final int STATUS_REVOKED = -1;
    private static final int TARGET_ALL = 1;
    private static final int TARGET_SPECIFIED = 2;

    private static final String DETAIL_SELECT = """
            SELECT n.id AS id,
                   n.title AS title,
                   n.content AS content,
                   n.type AS type,
                   n.level AS level,
                   n.publish_status AS publish_status,
                   n.target_type AS target_type,
                   n.target_user_ids AS target_user_ids,
                   DATE_FORMAT(n.publish_time, '%Y-%m-%d %H:%i:%s') AS publish_time,
                   DATE_FORMAT(n.revoke_time, '%Y-%m-%d %H:%i:%s') AS revoke_time,
                   DATE_FORMAT(n.create_time, '%Y-%m-%d %H:%i:%s') AS create_time,
                   COALESCE(u.nickname, u.username) AS publisher_name,
                   CASE WHEN EXISTS (SELECT 1 FROM sys_notice_read nr
                                      WHERE nr.notice_id = n.id AND nr.user_id = :userId)
                        THEN 1 ELSE 0 END AS is_read
              FROM sys_notice n
              LEFT JOIN sys_user u ON u.id = n.publisher_id
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public NoticeService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * 管理端分页。
     */
    public PageResult<NoticeDtos.NoticeItem> page(NoticeDtos.NoticeQuery query, LoginUser loginUser) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        Map<String, Object> params = new HashMap<>();
        params.put("userId", currentUserId(loginUser));
        if (query.getTitle() != null && !query.getTitle().isBlank()) {
            where.append(" AND n.title LIKE :title");
            params.put("title", "%" + query.getTitle().trim() + "%");
        }
        if (query.getPublishStatus() != null) {
            where.append(" AND n.publish_status = :publishStatus");
            params.put("publishStatus", query.getPublishStatus());
        }

        long total = jdbc.queryForObject("SELECT COUNT(*) FROM sys_notice n" + where, params, Long.class);
        applyPage(params, query.getPageNum(), query.getPageSize());

        List<NoticeDtos.NoticeItem> list = jdbc.query(
                DETAIL_SELECT + where + " ORDER BY n.create_time DESC, n.id DESC LIMIT :limit OFFSET :offset",
                params, this::mapItem);
        return new PageResult<>(list, total);
    }

    /**
     * 我的通知分页：仅已发布且对当前用户可见的通知。
     */
    public PageResult<NoticeDtos.NoticeItem> myPage(NoticeDtos.NoticeQuery query, LoginUser loginUser) {
        StringBuilder where = new StringBuilder(visibleWhere());
        Map<String, Object> params = new HashMap<>();
        params.put("userId", currentUserId(loginUser));

        if (query.getTitle() != null && !query.getTitle().isBlank()) {
            where.append(" AND n.title LIKE :title");
            params.put("title", "%" + query.getTitle().trim() + "%");
        }
        if (query.getIsRead() != null && query.getIsRead() == 0) {
            where.append(" AND NOT EXISTS (SELECT 1 FROM sys_notice_read nr "
                    + "WHERE nr.notice_id = n.id AND nr.user_id = :userId)");
        } else if (query.getIsRead() != null && query.getIsRead() == 1) {
            where.append(" AND EXISTS (SELECT 1 FROM sys_notice_read nr "
                    + "WHERE nr.notice_id = n.id AND nr.user_id = :userId)");
        }

        long total = jdbc.queryForObject("SELECT COUNT(*) FROM sys_notice n" + where, params, Long.class);
        applyPage(params, query.getPageNum(), query.getPageSize());

        List<NoticeDtos.NoticeItem> list = jdbc.query(
                DETAIL_SELECT + where + " ORDER BY n.publish_time DESC, n.id DESC LIMIT :limit OFFSET :offset",
                params, this::mapItem);
        return new PageResult<>(list, total);
    }

    public NoticeDtos.NoticeForm form(Long id) {
        Map<String, Object> row = loadRow(id);
        List<Long> targetUsers = parseTargetUsers((String) row.get("target_user_ids"));
        return new NoticeDtos.NoticeForm(String.valueOf(id), (String) row.get("title"),
                (String) row.get("content"), toInteger(row.get("type"), 1),
                (String) row.get("level"), toInteger(row.get("publish_status"), STATUS_DRAFT),
                toInteger(row.get("target_type"), TARGET_ALL), targetUsers, targetUsers);
    }

    /**
     * 管理端预览，不写入阅读记录。
     */
    public NoticeDtos.NoticeDetail preview(Long id) {
        return detail(id, null, false);
    }

    /**
     * 详情；阅读端调用时写入当前用户的已读记录。
     */
    @Transactional
    public NoticeDtos.NoticeDetail detail(Long id, LoginUser loginUser, boolean markRead) {
        Map<String, Object> row = loadRow(id);
        int publishStatus = toInteger(row.get("publish_status"), STATUS_DRAFT);

        boolean readable = loginUser != null
                && (canManage(loginUser) || (publishStatus == STATUS_PUBLISHED && isVisible(row, loginUser)));
        if (loginUser != null && !readable) {
            throw BusinessException.notFound("通知不存在");
        }

        long userId = loginUser == null ? 0L : currentUserId(loginUser);
        boolean read = loginUser != null && isRead(id, userId);
        if (markRead && readable && publishStatus == STATUS_PUBLISHED && !read) {
            jdbc.update("INSERT IGNORE INTO sys_notice_read (notice_id, user_id, read_time) "
                    + "VALUES (:noticeId, :userId, :readTime)",
                    Map.of("noticeId", id, "userId", userId, "readTime", LocalDateTime.now()));
            read = true;
        }

        return new NoticeDtos.NoticeDetail(String.valueOf(id), (String) row.get("title"),
                (String) row.get("content"), toInteger(row.get("type"), 1), (String) row.get("level"),
                publishStatus, toInteger(row.get("target_type"), TARGET_ALL),
                (String) row.get("target_user_ids"), (String) row.get("publisher_name"),
                (String) row.get("publish_time"), (String) row.get("revoke_time"),
                (String) row.get("create_time"), read ? 1 : 0);
    }

    @Transactional
    public void create(NoticeDtos.NoticeForm form, LoginUser loginUser) {
        validate(form);
        List<Long> targets = resolveTargets(form);
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("title", form.title().trim());
        params.addValue("content", form.content());
        params.addValue("type", form.type());
        params.addValue("level", form.level() == null || form.level().isBlank() ? "L" : form.level());
        params.addValue("publishStatus", STATUS_DRAFT);
        params.addValue("targetType", form.targetType() == null ? TARGET_ALL : form.targetType());
        params.addValue("targetUserIds", toCsv(targets));
        params.addValue("publisherId", currentUserId(loginUser));
        params.addValue("createTime", LocalDateTime.now());
        params.addValue("updateTime", LocalDateTime.now());

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update("INSERT INTO sys_notice (title, content, type, level, publish_status, target_type, "
                        + "target_user_ids, publisher_id, create_time, update_time) "
                        + "VALUES (:title, :content, :type, :level, :publishStatus, :targetType, "
                        + ":targetUserIds, :publisherId, :createTime, :updateTime)",
                params, keyHolder, new String[]{"id"});
    }

    @Transactional
    public void update(Long id, NoticeDtos.NoticeForm form) {
        Map<String, Object> row = loadRow(id);
        if (toInteger(row.get("publish_status"), STATUS_DRAFT) == STATUS_PUBLISHED) {
            throw BusinessException.badRequest("已发布的通知不可编辑，请先撤回");
        }
        validate(form);
        List<Long> targets = resolveTargets(form);

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("id", id);
        params.addValue("title", form.title().trim());
        params.addValue("content", form.content());
        params.addValue("type", form.type());
        params.addValue("level", form.level() == null || form.level().isBlank() ? "L" : form.level());
        params.addValue("targetType", form.targetType() == null ? TARGET_ALL : form.targetType());
        params.addValue("targetUserIds", toCsv(targets));
        params.addValue("updateTime", LocalDateTime.now());
        jdbc.update("UPDATE sys_notice SET title = :title, content = :content, type = :type, level = :level, "
                + "target_type = :targetType, target_user_ids = :targetUserIds, update_time = :updateTime "
                + "WHERE id = :id", params);
    }

    @Transactional
    public void delete(String ids) {
        List<Long> noticeIds = parseIds(ids);
        if (noticeIds.isEmpty()) {
            throw BusinessException.badRequest("请选择要删除的通知");
        }
        Long published = jdbc.queryForObject("SELECT COUNT(*) FROM sys_notice "
                        + "WHERE id IN (:ids) AND publish_status = :publishStatus",
                Map.of("ids", noticeIds, "publishStatus", STATUS_PUBLISHED), Long.class);
        if (published != null && published > 0) {
            throw BusinessException.badRequest("已发布的通知不可删除，请先撤回");
        }
        jdbc.update("DELETE FROM sys_notice_read WHERE notice_id IN (:ids)", Map.of("ids", noticeIds));
        jdbc.update("DELETE FROM sys_notice WHERE id IN (:ids)", Map.of("ids", noticeIds));
    }

    @Transactional
    public void publish(Long id, LoginUser loginUser) {
        Map<String, Object> row = loadRow(id);
        if (toInteger(row.get("publish_status"), STATUS_DRAFT) == STATUS_PUBLISHED) {
            throw BusinessException.badRequest("通知已发布");
        }
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("id", id);
        params.addValue("publisherId", currentUserId(loginUser));
        params.addValue("publishTime", LocalDateTime.now());
        params.addValue("updateTime", LocalDateTime.now());
        jdbc.update("UPDATE sys_notice SET publish_status = :publishStatus, publisher_id = :publisherId, "
                + "publish_time = :publishTime, revoke_time = NULL, update_time = :updateTime "
                + "WHERE id = :id",
                params.addValue("publishStatus", STATUS_PUBLISHED));
    }

    @Transactional
    public void revoke(Long id) {
        Map<String, Object> row = loadRow(id);
        if (toInteger(row.get("publish_status"), STATUS_DRAFT) != STATUS_PUBLISHED) {
            throw BusinessException.badRequest("仅已发布的通知可撤回");
        }
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("id", id);
        params.addValue("revokeTime", LocalDateTime.now());
        params.addValue("updateTime", LocalDateTime.now());
        jdbc.update("UPDATE sys_notice SET publish_status = :publishStatus, revoke_time = :revokeTime, "
                + "update_time = :updateTime WHERE id = :id",
                params.addValue("publishStatus", STATUS_REVOKED));
    }

    /**
     * 将当前用户可见的已发布通知全部标记为已读。
     */
    @Transactional
    public int readAll(LoginUser loginUser) {
        long userId = currentUserId(loginUser);
        Map<String, Object> params = Map.of("userId", userId, "readTime", LocalDateTime.now());
        return jdbc.update("INSERT IGNORE INTO sys_notice_read (notice_id, user_id, read_time) "
                + "SELECT n.id, :userId, :readTime FROM sys_notice n" + visibleWhere()
                + " AND NOT EXISTS (SELECT 1 FROM sys_notice_read nr "
                + "WHERE nr.notice_id = n.id AND nr.user_id = :userId)", params);
    }

    private String visibleWhere() {
        return " WHERE n.publish_status = 1 AND (n.target_type = 1 "
                + "OR FIND_IN_SET(CAST(:userId AS CHAR), n.target_user_ids))";
    }

    private boolean canManage(LoginUser loginUser) {
        return loginUser.isRoot() || loginUser.perms().contains("sys:notice:list");
    }

    private boolean isVisible(Map<String, Object> row, LoginUser loginUser) {
        int targetType = toInteger(row.get("target_type"), TARGET_ALL);
        if (targetType == TARGET_ALL) {
            return true;
        }
        List<Long> targets = parseTargetUsers((String) row.get("target_user_ids"));
        return targets.contains(loginUser.userId());
    }

    private boolean isRead(Long noticeId, long userId) {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM sys_notice_read "
                + "WHERE notice_id = :noticeId AND user_id = :userId",
                Map.of("noticeId", noticeId, "userId", userId), Long.class);
        return count != null && count > 0;
    }

    private Map<String, Object> loadRow(Long id) {
        List<Map<String, Object>> rows = jdbc.queryForList(DETAIL_SELECT + " WHERE n.id = :id",
                Map.of("id", id, "userId", 0L));
        if (rows.isEmpty()) {
            throw BusinessException.notFound("通知不存在");
        }
        return rows.get(0);
    }

    private NoticeDtos.NoticeItem mapItem(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new NoticeDtos.NoticeItem(String.valueOf(rs.getLong("id")), rs.getString("title"),
                rs.getString("content"), JdbcUtil.getInt(rs, "type"), rs.getString("level"),
                JdbcUtil.getInt(rs, "publish_status"), JdbcUtil.getInt(rs, "is_read"),
                JdbcUtil.getInt(rs, "target_type"), rs.getString("publisher_name"),
                rs.getString("publish_time"), rs.getString("revoke_time"), rs.getString("create_time"));
    }

    private void applyPage(Map<String, Object> params, Integer pageNum, Integer pageSize) {
        int num = pageNum == null ? 1 : Math.max(1, pageNum);
        int size = pageSize == null ? 10 : Math.min(100, Math.max(1, pageSize));
        params.put("limit", size);
        params.put("offset", (long) (num - 1) * size);
    }

    private void validate(NoticeDtos.NoticeForm form) {
        if (form.title() == null || form.title().isBlank()) {
            throw BusinessException.badRequest("通知标题不能为空");
        }
        if (form.content() == null || form.content().replaceAll("<[^>]+>", "").isBlank()) {
            throw BusinessException.badRequest("通知内容不能为空");
        }
        if (form.type() == null) {
            throw BusinessException.badRequest("请选择通知类型");
        }
        if (form.targetType() != null && form.targetType() == TARGET_SPECIFIED
                && resolveTargets(form).isEmpty()) {
            throw BusinessException.badRequest("请选择接收通知的用户");
        }
    }

    private List<Long> resolveTargets(NoticeDtos.NoticeForm form) {
        if (form.targetType() != null && form.targetType() != TARGET_SPECIFIED) {
            return List.of();
        }
        List<Long> source = form.targetUserIds() != null && !form.targetUserIds().isEmpty()
                ? form.targetUserIds() : form.targetUsers();
        if (source == null) {
            return List.of();
        }
        return source.stream().filter(java.util.Objects::nonNull).distinct().toList();
    }

    private List<Long> parseTargetUsers(String csv) {
        List<Long> result = new ArrayList<>();
        if (csv == null || csv.isBlank()) {
            return result;
        }
        for (String part : csv.split(",")) {
            Long value = IdUtils.parseLong(part.trim());
            if (value != null) {
                result.add(value);
            }
        }
        return result;
    }

    private String toCsv(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        return String.join(",", ids.stream().map(String::valueOf).toList());
    }

    private List<Long> parseIds(String ids) {
        LinkedHashSet<Long> result = new LinkedHashSet<>();
        if (ids == null) {
            return new ArrayList<>();
        }
        for (String part : ids.split(",")) {
            Long value = IdUtils.parseLong(part.trim());
            if (value != null) {
                result.add(value);
            }
        }
        return new ArrayList<>(result);
    }

    private long currentUserId(LoginUser loginUser) {
        if (loginUser == null || loginUser.userId() == null) {
            throw BusinessException.unauthorized("A0230", "访问令牌无效或已过期");
        }
        return loginUser.userId();
    }

    private static Integer toInteger(Object value, Integer fallback) {
        return value instanceof Number number ? number.intValue() : fallback;
    }
}
