/**
 * API 统一响应体，对齐后端 ResultInfo。
 */
export interface ResultInfo<T = unknown> {
  /** 是否成功，业务成功失败的唯一标准 */
  success: boolean;
  /** 业务状态码，成功为 200，失败为对应 HTTP 状态码 */
  resultCode: number;
  /** 失败描述 / 提示信息 */
  resultMsg: string;
  /** 业务数据 */
  resultBody: T;
  /** 链路 ID */
  traceId?: string;
  /** 服务端响应时间（毫秒） */
  timestamp?: number;
}

/**
 * 分页与排序条件。
 */
export interface PageQuery {
  /** 页码 */
  pageNum: number;
  /** 每页记录数 */
  pageSize: number;

  /** 排序规则；接口需声明支持的公开属性 */
  sort?: SortExpression[];
}

/**
 * 接口排序方向。
 */
export type SortDirection = "asc" | "desc";

/**
 * 公开排序属性与方向，数组顺序即排序优先级。
 */
export type SortExpression<TField extends string = string> = `${TField},${SortDirection}`;

/**
 * 分页接口结果，对齐 Spring Data Page 的 JSON 结构。
 */
export interface Page<T> {
  /** 当前页数据 */
  content: T[];
  /** 总记录数 */
  totalElements: number;
  /** 总页数 */
  totalPages: number;
  /** 当前页码（从 0 开始） */
  number: number;
  /** 每页记录数 */
  size: number;
  /** 当前页记录数 */
  numberOfElements: number;
  /** 是否首页 */
  first: boolean;
  /** 是否末页 */
  last: boolean;
  /** 是否空页 */
  empty: boolean;
}

/**
 * 下拉或树形选项。
 */
export interface OptionItem {
  /** 选项值 */
  value: string | number;
  /** 选项标签 */
  label: string;
  /** 子选项 */
  children?: OptionItem[];
}

/**
 * Excel 导入结果
 */
export interface ExcelResult {
  /** 业务状态码 */
  code: string;
  /** 无效数据数量 */
  invalidCount: number;
  /** 有效数据数量 */
  validCount: number;
  /** 错误信息列表 */
  messageList: string[];
}
