import type { PageQuery } from "@/api/common";

/**
 * 设备锁（zl_supervise_lock）分页查询参数
 */
export interface SuperviseLockQuery extends PageQuery {
  /** 订单号 / 序列号 / 串码 / 型号 关键字 */
  keywords?: string;
  /** 订单号 */
  orderId?: string;
  /** 监管 ID */
  superviseId?: number;
  /** 是否上锁 0-正常 1-已锁 */
  status?: string;
  /** 租户主体 */
  tenant?: string;
  /** 品牌 */
  brandId?: string;
  /** 系统类型 IOS / ANDROID */
  sysType?: string;
  /** 创建时间起 */
  createTimeStart?: string;
  /** 创建时间止 */
  createTimeEnd?: string;
}

/**
 * 设备锁列表行（字段与 zl_supervise_lock 一一对应）
 */
export interface SuperviseLockItem {
  id: number;
  orderId?: string;
  superviseId?: number;
  superviseName?: string;
  serialNumber?: string;
  /** 是否上锁(1:已锁 0:正常) */
  status?: string;
  /** 是否支持接口(0:否 1:是) */
  isInterface?: string;
  /** 正常壁纸 */
  normalWallpaper?: number;
  /** activationLockStatus 激活锁状态 */
  activationLockStatus?: string;
  /** 是否处于监管 */
  supervise?: number;
  /** 在线状态 */
  onlineStatus?: number;
  /** ABM 在库状态 */
  abmStatus?: string;
  remark?: string;
  updateUser?: string;
  createUser?: string;
  updateTime?: string;
  createTime?: string;
  /** 系统类型 */
  sysType?: string;
  /** 品牌 */
  brandId?: string;
  /** 串码 */
  serialCode?: string;
  /** 手机型号(vivo锁) */
  model?: string;
  /** 监管码(vivo锁) */
  superviseCode?: string;
  /** 监管码2(vivo锁) */
  serialCode2?: string;
  /** 租户主体 */
  tenant?: string;
  /** APP 限制(0:否 1:是) */
  restriction?: number;
  /** 最后活动时间 */
  lastActiveTime?: string;
  /** 处理日期 */
  dealDate?: string;
}
