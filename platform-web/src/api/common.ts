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
 * 分页接口结果，对齐 Spring Data Web {@code VIA_DTO} 序列化模式。
 *
 * <p>后端通过 {@code @EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)} 输出
 * 稳定 DTO 结构：content 是当前页数据，page 是分页元模型。
 */
export interface Pagination<T> {
  /** 当前页数据 */
  content: T[];
  /** 分页元信息 */
  page: PageMeta;
}

/**
 * 分页元信息，与后端 VIA_DTO 输出完全对齐。
 */
export interface PageMeta {
  /** 当前页码（从 0 开始） */
  number: number;
  /** 每页记录数 */
  size: number;
  /** 总记录数 */
  totalElements: number;
  /** 总页数 */
  totalPages: number;
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
