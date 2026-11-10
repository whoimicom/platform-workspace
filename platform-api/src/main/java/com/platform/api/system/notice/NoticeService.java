package com.platform.api.system.notice;

import com.platform.api.common.BusinessException;
import com.platform.api.common.IdUtils;
import com.platform.api.common.JdbcUtil;
import org.springframework.data.domain.Page;
import com.platform.api.common.query.DynamicQueryExecutor;
import com.platform.api.common.query.RawCondition;
import com.platform.api.security.LoginUser;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private final DynamicQueryExecutor executor;
    private final SysNoticeRepository noticeRepository;

    public NoticeService(NamedParameterJdbcTemplate jdbc,
                         DynamicQueryExecutor executor,
                         SysNoticeRepository noticeRepository) {
        this.jdbc = jdbc;
        this.executor = executor;
        this.noticeRepository = noticeRepository;
    }

    /**
     * 管理端分页。
     */
    public Page<NoticeDtos.NoticeItem> page(NoticeDtos.NoticeQuery query, LoginUser loginUser) {
        return executor.page(DETAIL_SELECT, query,
                Map.of("userId", currentUserId(loginUser)), this::mapItem);
    }

    /**
     * 我的通知分页：仅已发布且对当前用户可见的通知。
     */
    public Page<NoticeDtos.NoticeItem> myPage(NoticeDtos.NoticeQuery query, LoginUser loginUser) {
        long userId = currentUserId(loginUser);
        List<RawCondition> extras = new ArrayList<>();
        extras.add(RawCondition.of("n.publish_status = 1 AND (n.target_type = 1 "
                + "OR FIND_IN_SET(CAST(:userId AS CHAR), n.target_user_ids))", Map.of("userId", userId)));
        if (query.getIsRead() != null && query.getIsRead() == 0) {
            extras.add(RawCondition.of("NOT EXISTS (SELECT 1 FROM sys_notice_read nr "
                    + "WHERE nr.notice_id = n.id AND nr.user_id = :userId)", Map.of("userId", userId)));
        } else if (query.getIsRead() != null && query.getIsRead() == 1) {
            extras.add(RawCondition.of("EXISTS (SELECT 1 FROM sys_notice_read nr "
                    + "WHERE nr.notice_id = n.id AND nr.user_id = :userId)", Map.of("userId", userId)));
        }
        return executor.page(DETAIL_SELECT, query, Map.of("userId", userId),
                "n.publish_time DESC, n.id DESC", this::mapItem, extras.toArray(RawCondition[]::new));
    }

    public NoticeDtos.NoticeForm form(Long id) {
        SysNotice notice = noticeRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("通知不存在"));
        List<Long> targetUsers = parseTargetUsers(notice.getTargetUserIds());
        return new NoticeDtos.NoticeForm(String.valueOf(id), notice.getTitle(), notice.getContent(),
                notice.getType() == null ? 1 : notice.getType(), notice.getLevel(),
                notice.getPublishStatus() == null ? STATUS_DRAFT : notice.getPublishStatus(),
                notice.getTargetType() == null ? TARGET_ALL : notice.getTargetType(),
                targetUsers, targetUsers);
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
        SysNotice notice = new SysNotice();
        notice.setTitle(form.title().trim());
        notice.setContent(form.content());
        notice.setType(form.type());
        notice.setLevel(form.level() == null || form.level().isBlank() ? "L" : form.level());
        notice.setPublishStatus(STATUS_DRAFT);
        notice.setTargetType(form.targetType() == null ? TARGET_ALL : form.targetType());
        notice.setTargetUserIds(toCsv(targets));
        notice.setPublisherId(currentUserId(loginUser));
        LocalDateTime now = LocalDateTime.now();
        notice.setCreateTime(now);
        notice.setUpdateTime(now);
        noticeRepository.save(notice);
    }

    @Transactional
    public void update(Long id, NoticeDtos.NoticeForm form) {
        SysNotice notice = noticeRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("通知不存在"));
        if (notice.getPublishStatus() != null && notice.getPublishStatus() == STATUS_PUBLISHED) {
            throw BusinessException.badRequest("已发布的通知不可编辑，请先撤回");
        }
        validate(form);
        List<Long> targets = resolveTargets(form);
        notice.setTitle(form.title().trim());
        notice.setContent(form.content());
        notice.setType(form.type());
        notice.setLevel(form.level() == null || form.level().isBlank() ? "L" : form.level());
        notice.setTargetType(form.targetType() == null ? TARGET_ALL : form.targetType());
        notice.setTargetUserIds(toCsv(targets));
        notice.setUpdateTime(LocalDateTime.now());
        noticeRepository.save(notice);
    }

    @Transactional
    public void delete(String ids) {
        List<Long> noticeIds = parseIds(ids);
        if (noticeIds.isEmpty()) {
            throw BusinessException.badRequest("请选择要删除的通知");
        }
        if (noticeRepository.countByIdInAndPublishStatus(noticeIds, STATUS_PUBLISHED) > 0) {
            throw BusinessException.badRequest("已发布的通知不可删除，请先撤回");
        }
        // sys_notice_read 为复合主键关联表，保留 SQL 删除
        jdbc.update("DELETE FROM sys_notice_read WHERE notice_id IN (:ids)", Map.of("ids", noticeIds));
        noticeRepository.deleteAllById(noticeIds);
    }

    @Transactional
    public void publish(Long id, LoginUser loginUser) {
        SysNotice notice = noticeRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("通知不存在"));
        if (notice.getPublishStatus() != null && notice.getPublishStatus() == STATUS_PUBLISHED) {
            throw BusinessException.badRequest("通知已发布");
        }
        notice.setPublishStatus(STATUS_PUBLISHED);
        notice.setPublisherId(currentUserId(loginUser));
        notice.setPublishTime(LocalDateTime.now());
        notice.setRevokeTime(null);
        notice.setUpdateTime(LocalDateTime.now());
        noticeRepository.save(notice);
    }

    @Transactional
    public void revoke(Long id) {
        SysNotice notice = noticeRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("通知不存在"));
        if (notice.getPublishStatus() == null || notice.getPublishStatus() != STATUS_PUBLISHED) {
            throw BusinessException.badRequest("仅已发布的通知可撤回");
        }
        notice.setPublishStatus(STATUS_REVOKED);
        notice.setRevokeTime(LocalDateTime.now());
        notice.setUpdateTime(LocalDateTime.now());
        noticeRepository.save(notice);
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
            throw BusinessException.unauthorized("访问令牌无效或已过期");
        }
        return loginUser.userId();
    }

    private static Integer toInteger(Object value, Integer fallback) {
        return value instanceof Number number ? number.intValue() : fallback;
    }
}
