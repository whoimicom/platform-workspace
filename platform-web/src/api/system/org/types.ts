/**
 * 架构查询参数
 */
export interface OrgQuery {
  /** 搜索关键字 */
  keywords?: string;
  /** 状态 */
  status?: number;
}

/**
 * 架构视图对象
 */
export interface OrgItem {
  /** 子架构 */
  children?: OrgItem[];
  /** 创建时间 */
  createTime?: Date;
  /** 架构 ID */
  id?: string;
  /** 架构名称 */
  name?: string;
  /** 父架构 ID */
  parentId?: string;
  /** 排序 */
  sort?: number;
  /** 状态(1:启用；0:禁用) */
  status?: number;
  /** 父节点 ID 路径 */
  treePath?: string;
  /** 修改时间 */
  updateTime?: Date;
}

/**
 * 架构表单对象
 */
export interface OrgForm {
  /** 架构 ID */
  id?: string;
  /** 架构名称 */
  name?: string;
  /** 架构编号 */
  code?: string;
  /** 父架构 ID */
  parentId?: string;
  /** 排序 */
  sort?: number;
  /** 状态(1:启用；0:禁用) */
  status?: number;
}
