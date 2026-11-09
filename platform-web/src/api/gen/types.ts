import type { PageQuery } from "@/api/common";

/**
 * 代码生成分页查询参数
 */
export interface GenQuery extends PageQuery {
  /** 表名 / 功能名关键字 */
  keywords?: string;
}

/**
 * 当前库内可导入的数据表
 */
export interface DbTableItem {
  tableName: string;
  tableRows?: number;
  tableComment?: string;
  /** 是否已保存生成配置 */
  configured?: boolean;
}

/**
 * 生成配置（不含字段明细）
 */
export interface GenConfigItem {
  id: number;
  tableName: string;
  tableComment?: string;
  moduleName: string;
  businessName: string;
  functionName: string;
  className: string;
  frontendPath: string;
  permPrefix: string;
  /** data-jdbc / dynamic-sql */
  genStyle: string;
  packageName: string;
  author?: string;
  createTime?: string;
  updateTime?: string;
}

/**
 * 字段生成规则
 */
export interface GenColumnItem {
  id?: number;
  columnName: string;
  columnType: string;
  columnComment?: string;
  propertyName: string;
  javaType: string;
  isPk?: boolean;
  isRequired?: boolean;
  isList?: boolean;
  isFilter?: boolean;
  /** EQ / LIKE / GE / LE / RANGE */
  filterType?: string;
}

/**
 * 配置详情
 */
export interface GenConfigDetail {
  table: GenConfigItem;
  columns: GenColumnItem[];
}

/**
 * 保存配置的请求体
 */
export interface GenConfigForm {
  functionName: string;
  moduleName: string;
  businessName: string;
  className: string;
  frontendPath: string;
  permPrefix: string;
  genStyle: string;
  packageName: string;
  author?: string;
  columns: GenColumnItem[];
}

/**
 * 生成的文件
 */
export interface GenFileItem {
  path: string;
  content: string;
}
