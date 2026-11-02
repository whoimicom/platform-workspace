import type { DictItemOption } from "@/api/system/dict";

/**
 * 静态字典注册表 —— 前端源码内置、不走后端网络请求。
 *
 * 适用场景：性别、通用状态、是否、通知类型等不会变化的枚举。
 * 新增静态字典：在此处加 dictCode → items 映射，然后去 sys_dict 表
 * 把对应记录 type 改为 STATIC 即可（后端会禁止通过管理平台修改其 items）。
 *
 * value 规则：英文大写简写（如 M / F / U、ENABLED / DISABLED），禁止 1/2 等无意义数字。
 * desc 规则：完整英文描述（如 male / female / unknown），仅管理页展示，业务代码不依赖。
 */
export const STATIC_DICTS: Record<string, DictItemOption[]> = {
  // ========== 现有字典（value 已从英文拼写改为大写简写） ==========

  /** 性别 */
  gender: [
    { value: "U", label: "未知", desc: "unknown" },
    { value: "M", label: "男", desc: "male", tagType: "primary" },
    { value: "F", label: "女", desc: "female", tagType: "danger" },
  ],

  /** 通用启用/禁用状态 */
  common_status: [
    { value: "ENABLED", label: "正常", desc: "enabled", tagType: "primary" },
    { value: "DISABLED", label: "禁用", desc: "disabled", tagType: "danger" },
  ],

  /** 通知类型 */
  notice_type: [
    { value: "NOTIFY", label: "通知", desc: "notify", tagType: "primary" },
    { value: "ANNOUNCE", label: "公告", desc: "announcement", tagType: "success" },
  ],

  /** 通知等级 */
  notice_level: [
    { value: "LOW", label: "低", desc: "low", tagType: "info" },
    { value: "MID", label: "中", desc: "mid", tagType: "warning" },
    { value: "HIGH", label: "高", desc: "high", tagType: "danger" },
  ],

  // ========== 20 个新增常用静态字典 ==========

  /** 是否布尔 */
  yes_no: [
    { value: "Y", label: "是", desc: "yes", tagType: "success" },
    { value: "N", label: "否", desc: "no", tagType: "info" },
  ],

  /** 用户状态 */
  user_status: [
    { value: "ENABLED", label: "正常", desc: "enabled", tagType: "primary" },
    { value: "DISABLED", label: "禁用", desc: "disabled", tagType: "danger" },
    { value: "LOCKED", label: "锁定", desc: "locked", tagType: "warning" },
  ],

  /** 账号状态 */
  account_status: [
    { value: "ENABLED", label: "正常", desc: "enabled", tagType: "primary" },
    { value: "LOCKED", label: "锁定", desc: "locked", tagType: "warning" },
    { value: "EXPIRED", label: "过期", desc: "expired", tagType: "danger" },
    { value: "PWD_EXPIRED", label: "密码过期", desc: "password expired", tagType: "warning" },
  ],

  /** 登录方式 */
  login_type: [
    { value: "PWD", label: "密码", desc: "password" },
    { value: "SMS", label: "短信", desc: "sms code" },
    { value: "OAUTH2", label: "OAuth2", desc: "oauth2" },
    { value: "CAS", label: "CAS", desc: "cas" },
  ],

  /** HTTP 请求方法 */
  http_method: [
    { value: "GET", label: "GET", desc: "get", tagType: "success" },
    { value: "POST", label: "POST", desc: "post", tagType: "primary" },
    { value: "PUT", label: "PUT", desc: "put", tagType: "warning" },
    { value: "DELETE", label: "DELETE", desc: "delete", tagType: "danger" },
    { value: "PATCH", label: "PATCH", desc: "patch", tagType: "info" },
  ],

  /** 任务执行状态（定时任务/异步任务） */
  job_status: [
    { value: "PENDING", label: "等待中", desc: "pending", tagType: "info" },
    { value: "RUNNING", label: "执行中", desc: "running", tagType: "primary" },
    { value: "SUCCESS", label: "成功", desc: "success", tagType: "success" },
    { value: "FAILED", label: "失败", desc: "failed", tagType: "danger" },
    { value: "STOPPED", label: "已停止", desc: "stopped", tagType: "warning" },
  ],

  /** 业务操作类型（审计日志） */
  operate_type: [
    { value: "CREATE", label: "新增", desc: "create", tagType: "success" },
    { value: "UPDATE", label: "修改", desc: "update", tagType: "primary" },
    { value: "DELETE", label: "删除", desc: "delete", tagType: "danger" },
    { value: "EXPORT", label: "导出", desc: "export", tagType: "info" },
    { value: "IMPORT", label: "导入", desc: "import", tagType: "info" },
    { value: "LOGIN", label: "登录", desc: "login", tagType: "success" },
    { value: "LOGOUT", label: "登出", desc: "logout", tagType: "warning" },
  ],

  /** 是否开启 */
  switch: [
    { value: "ON", label: "开启", desc: "on", tagType: "success" },
    { value: "OFF", label: "关闭", desc: "off", tagType: "info" },
  ],

  /** 显示/隐藏 */
  visibility: [
    { value: "SHOWN", label: "显示", desc: "shown", tagType: "primary" },
    { value: "HIDDEN", label: "隐藏", desc: "hidden", tagType: "info" },
  ],

  /** 置顶标记 */
  top_flag: [
    { value: "Y", label: "置顶", desc: "top", tagType: "warning" },
    { value: "N", label: "普通", desc: "normal" },
  ],

  /** 优先级 */
  priority: [
    { value: "LOW", label: "低", desc: "low", tagType: "info" },
    { value: "NORMAL", label: "普通", desc: "normal", tagType: "primary" },
    { value: "HIGH", label: "高", desc: "high", tagType: "warning" },
    { value: "URGENT", label: "紧急", desc: "urgent", tagType: "danger" },
  ],

  /** 审批状态 */
  approval_status: [
    { value: "PENDING", label: "待审批", desc: "pending", tagType: "warning" },
    { value: "APPROVED", label: "已通过", desc: "approved", tagType: "success" },
    { value: "REJECTED", label: "已驳回", desc: "rejected", tagType: "danger" },
    { value: "CANCELLED", label: "已取消", desc: "cancelled", tagType: "info" },
  ],

  /** 工单/单据来源 */
  source_channel: [
    { value: "MANUAL", label: "手动创建", desc: "manual" },
    { value: "IMPORT", label: "批量导入", desc: "import" },
    { value: "API", label: "接口同步", desc: "api" },
    { value: "SYSTEM", label: "系统生成", desc: "system" },
  ],

  /** 国家/地区代码（ISO 3166-1 alpha-2，常用） */
  country_code: [
    { value: "CN", label: "中国", desc: "china" },
    { value: "HK", label: "中国香港", desc: "hong kong" },
    { value: "TW", label: "中国台湾", desc: "taiwan" },
    { value: "US", label: "美国", desc: "united states" },
    { value: "JP", label: "日本", desc: "japan" },
    { value: "KR", label: "韩国", desc: "korea" },
    { value: "SG", label: "新加坡", desc: "singapore" },
    { value: "UK", label: "英国", desc: "united kingdom" },
    { value: "DE", label: "德国", desc: "germany" },
    { value: "FR", label: "法国", desc: "france" },
    { value: "IN", label: "印度", desc: "india" },
    { value: "AU", label: "澳大利亚", desc: "australia" },
  ],

  /** 货币代码（ISO 4217，常用） */
  currency_code: [
    { value: "CNY", label: "人民币 (CNY)", desc: "yuan" },
    { value: "USD", label: "美元 (USD)", desc: "dollar" },
    { value: "EUR", label: "欧元 (EUR)", desc: "euro" },
    { value: "JPY", label: "日元 (JPY)", desc: "yen" },
    { value: "GBP", label: "英镑 (GBP)", desc: "pound" },
    { value: "HKD", label: "港币 (HKD)", desc: "hong kong dollar" },
    { value: "KRW", label: "韩元 (KRW)", desc: "korean won" },
    { value: "SGD", label: "新加坡元 (SGD)", desc: "singapore dollar" },
  ],

  /** 时间格式模板 */
  date_format: [
    { value: "yyyy-MM-dd", label: "2024-01-01", desc: "iso date" },
    { value: "yyyy/MM/dd", label: "2024/01/01", desc: "slash date" },
    { value: "yyyy.MM.dd", label: "2024.01.01", desc: "dot date" },
    { value: "yyyy-MM-dd HH:mm:ss", label: "2024-01-01 12:00:00", desc: "iso datetime" },
    { value: "yyyy年MM月dd日", label: "2024年01月01日", desc: "chinese date" },
  ],

  /** 时区 */
  time_zone: [
    { value: "UTC", label: "UTC (UTC+0)", desc: "coordinated universal time" },
    { value: "ASIA/SHANGHAI", label: "北京时间 (UTC+8)", desc: "beijing time" },
    { value: "ASIA/TOKYO", label: "东京时间 (UTC+9)", desc: "tokyo time" },
    { value: "ASIA/SEOUL", label: "首尔时间 (UTC+9)", desc: "seoul time" },
    { value: "ASIA/SINGAPORE", label: "新加坡时间 (UTC+8)", desc: "singapore time" },
    { value: "EUROPE/LONDON", label: "伦敦时间 (UTC+0)", desc: "london time" },
    { value: "EUROPE/BERLIN", label: "柏林时间 (UTC+1)", desc: "berlin time" },
    { value: "AMERICA/NEW_YORK", label: "纽约时间 (UTC-5)", desc: "new york time" },
    { value: "AMERICA/LOS_ANGELES", label: "洛杉矶时间 (UTC-8)", desc: "los angeles time" },
  ],

  /** 排序方向 */
  sort_order: [
    { value: "ASC", label: "升序", desc: "ascending", tagType: "primary" },
    { value: "DESC", label: "降序", desc: "descending", tagType: "primary" },
  ],

  /** 数据权限范围（角色数据权限） */
  data_scope: [
    { value: "ALL", label: "全部数据", desc: "all data", tagType: "danger" },
    { value: "ORG_CHILD", label: "本架构及子架构", desc: "org and children", tagType: "primary" },
    { value: "ORG_ONLY", label: "仅本架构", desc: "org only", tagType: "success" },
    { value: "SELF_ONLY", label: "仅本人", desc: "self only", tagType: "info" },
    { value: "CUSTOM", label: "自定义架构", desc: "custom", tagType: "warning" },
  ],

  /** 菜单类型 */
  menu_type: [
    { value: "DIR", label: "目录", desc: "directory", tagType: "info" },
    { value: "MENU", label: "菜单", desc: "menu", tagType: "primary" },
    { value: "BTN", label: "按钮", desc: "button", tagType: "success" },
  ],

  /** 前端路由组件模板（代码生成/动态路由） */
  page_component: [
    { value: "SYSTEM/USER/INDEX", label: "用户管理", desc: "user management" },
    { value: "SYSTEM/ROLE/INDEX", label: "角色管理", desc: "role management" },
    { value: "SYSTEM/ORG/INDEX", label: "架构管理", desc: "org management" },
    { value: "SYSTEM/MENU/INDEX", label: "菜单管理", desc: "menu management" },
    { value: "SYSTEM/DICT/INDEX", label: "字典管理", desc: "dict management" },
    { value: "SYSTEM/NOTICE/INDEX", label: "通知公告", desc: "notice management" },
    { value: "DASHBOARD/INDEX", label: "首页", desc: "dashboard" },
  ],
};

/** 判断某个 dictCode 是否为静态字典（仅查表，不触发任何加载）。 */
export function isStaticDict(dictCode: string): boolean {
  return Object.prototype.hasOwnProperty.call(STATIC_DICTS, dictCode);
}
