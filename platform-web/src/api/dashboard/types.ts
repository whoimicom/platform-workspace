/**
 * 首页统计卡片数据
 */
export interface DashboardStats {
  /** 系统用户总数 */
  userCount: number;
  /** 今日新增用户数 */
  todayNewUsers: number;
  /** 新增用户较昨日增长率（昨日为 0 时为 null） */
  userGrowthRate: number | null;
  /** 我的未读通知数 */
  unreadNoticeCount: number;
  /** 已发布通知总数 */
  publishedNoticeCount: number;
  /** 角色总数 */
  roleCount: number;
  /** 架构总数 */
  orgCount: number;
}

/**
 * 用户增长趋势
 */
export interface UserTrend {
  /** 日期列表（MM-dd，升序） */
  dates: string[];
  /** 每日新增用户数 */
  counts: number[];
}
