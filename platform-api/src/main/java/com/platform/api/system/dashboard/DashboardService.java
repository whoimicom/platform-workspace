package com.platform.api.system.dashboard;

import com.platform.api.security.LoginUser;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 首页统计：只基于现有业务表的轻量 COUNT / 聚合查询，不做访问日志统计。
 */
@Service
public class DashboardService {

    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("MM-dd");

    private final NamedParameterJdbcTemplate jdbc;

    public DashboardService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public DashboardDtos.Stats stats(LoginUser loginUser) {
        long userCount = count("SELECT COUNT(*) FROM sys_user", Map.of());
        long todayNewUsers = count("SELECT COUNT(*) FROM sys_user WHERE DATE(create_time) = CURDATE()", Map.of());
        long yesterdayNewUsers = count(
                "SELECT COUNT(*) FROM sys_user WHERE DATE(create_time) = CURDATE() - INTERVAL 1 DAY", Map.of());
        Double growthRate = yesterdayGrowth(todayNewUsers, yesterdayNewUsers);
        long publishedNotices = count("SELECT COUNT(*) FROM sys_notice WHERE publish_status = 1", Map.of());
        long unread = count("SELECT COUNT(*) FROM sys_notice n "
                        + "WHERE n.publish_status = 1 AND (n.target_type = 1 "
                        + "OR FIND_IN_SET(CAST(:userId AS CHAR), n.target_user_ids)) "
                        + "AND NOT EXISTS (SELECT 1 FROM sys_notice_read nr "
                        + "WHERE nr.notice_id = n.id AND nr.user_id = :userId)",
                Map.of("userId", loginUser.userId()));
        long roleCount = count("SELECT COUNT(*) FROM sys_role", Map.of());
        long orgCount = count("SELECT COUNT(*) FROM sys_org", Map.of());
        return new DashboardDtos.Stats(userCount, todayNewUsers, growthRate, unread,
                publishedNotices, roleCount, orgCount);
    }

    /**
     * 近 N 天（含今天）每日新增用户数，无数据的日期补 0。
     */
    public DashboardDtos.UserTrend userTrend(int days) {
        int effectiveDays = Math.min(Math.max(days, 1), 90);
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(effectiveDays - 1L);

        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT DATE(create_time) AS day, COUNT(*) AS cnt FROM sys_user "
                        + "WHERE create_time >= :start GROUP BY DATE(create_time)",
                Map.of("start", start.atStartOfDay()));
        Map<String, Long> byDay = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object day = row.get("day");
            Object cnt = row.get("cnt");
            if (day != null && cnt != null) {
                byDay.put(day.toString(), ((Number) cnt).longValue());
            }
        }

        List<String> dates = new ArrayList<>(effectiveDays);
        List<Long> counts = new ArrayList<>(effectiveDays);
        for (int i = 0; i < effectiveDays; i++) {
            LocalDate day = start.plusDays(i);
            dates.add(day.format(DAY_FORMAT));
            counts.add(byDay.getOrDefault(day.toString(), 0L));
        }
        return new DashboardDtos.UserTrend(dates, counts);
    }

    private Double yesterdayGrowth(long today, long yesterday) {
        if (yesterday == 0) {
            return today > 0 ? 1.0 : null;
        }
        return (double) (today - yesterday) / yesterday;
    }

    private long count(String sql, Map<String, Object> params) {
        Long value = jdbc.queryForObject(sql, params, Long.class);
        return value == null ? 0L : value;
    }
}
