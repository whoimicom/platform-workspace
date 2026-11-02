package com.platform.api.system.dashboard;

import java.util.List;

/**
 * 首页统计数据结构。
 */
public final class DashboardDtos {

    private DashboardDtos() {
    }

    /**
     * 首页统计卡片数据。
     */
    public record Stats(
            /** 系统用户总数 */
            long userCount,
            /** 今日新增用户数 */
            long todayNewUsers,
            /** 新增用户较昨日增长率（昨日为 0 时为 null，前端显示 --） */
            Double userGrowthRate,
            /** 我的未读通知数 */
            long unreadNoticeCount,
            /** 已发布通知总数 */
            long publishedNoticeCount,
            /** 角色总数 */
            long roleCount,
            /** 架构总数 */
            long orgCount) {
    }

    /**
     * 用户增长趋势（近 N 天，含今天，日期升序）。
     */
    public record UserTrend(List<String> dates, List<Long> counts) {
    }
}
