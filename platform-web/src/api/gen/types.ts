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
  /** 是否已作为主表保存过生成配置 */
  configured?: boolean;
}

/**
 * 数据表字段（用于选择关联列）
 */
export interface DbColumnItem {
  columnName: string;
  columnType: string;
  columnComment?: string;
  propertyName: string;
  javaType: string;
  isPk?: boolean;
  nullable?: boolean;
}

/**
 * 导入表：一次选择多张表，指定其中一张为主表
 */
export interface GenImportForm {
  tableNames: string[];
  mainTable: string;
}

/**
 * 生成配置列表项
 */
export interface GenConfigItem {
  id: number;
  configName: string;
  /** 主表 */
  mainTable: string;
  /** 参与关联的表，逗号分隔 */
  tableList?: string;
  functionName: string;
  moduleName: string;
  businessName: string;
  className: string;
  frontendPath: string;
  permPrefix: string;
  /** dynamic-sql / data-jdbc */
  genStyle: string;
  packageName: string;
  author?: string;
  createTime?: string;
  updateTime?: string;
}

/**
 * 关联表配置（主表 relationType = MAIN，主表列 mainColumn 关联从表列 joinColumn）
 */
export interface GenTableItem {
  id?: number;
  tableName: string;
  tableComment?: string;
  tableAlias?: string;
  /** MAIN / ONE_TO_ONE / ONE_TO_MANY / MANY_TO_ONE */
  relationType: string;
  mainColumn?: string;
  joinColumn?: string;
  sort?: number;
}

/**
 * 字段生成规则
 */
export interface GenColumnItem {
  id?: number;
  tableName: string;
  columnName: string;
  columnType: string;
  columnComment?: string;
  /** 显示列名（为空时使用 SQL 列注释） */
  displayName?: string;
  propertyName: string;
  javaType: string;
  isPk?: boolean;
  isRequired?: boolean;
  isList?: boolean;
  isFilter?: boolean;
  /** EQ / NE / GT / GE / LT / LE / LIKE / LIKE_LEFT / LIKE_RIGHT / IN / NOT_IN / BETWEEN */
  filterType?: string;
  /** 是否表单字段（仅主表非主键字段有效） */
  isForm?: boolean;
  /** 表单元素 INPUT / TEXTAREA / NUMBER / DATE / DATETIME / SWITCH / SELECT */
  formComponent?: string;
  sort?: number;
}

/**
 * 配置详情
 */
export interface GenConfigDetail {
  config: GenConfigItem;
  tables: GenTableItem[];
  columns: GenColumnItem[];
}

/**
 * 保存配置的请求体
 */
export interface GenConfigForm {
  configName: string;
  functionName: string;
  moduleName: string;
  businessName: string;
  className: string;
  frontendPath: string;
  permPrefix: string;
  genStyle: string;
  packageName: string;
  author?: string;
  tables: GenTableItem[];
  columns: GenColumnItem[];
}

/**
 * 生成的文件
 */
export interface GenFileItem {
  path: string;
  content: string;
}
