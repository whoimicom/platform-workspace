import { createDefineMock } from "vite-plugin-mock-dev-server";

/**
 * 接口前缀：
 *   完整请求路径 = VITE_API_BASE + /api + 业务路径，如 /dev-api/api/users
 *
 * 说明：插件的 mock url 需为**完整请求路径**（内部用 path-to-regexp 匹配完整 pathname），
 * 因此这里统一拼接前缀，各 mock 文件内只写业务路径（如 users、auth/login）。
 *
 * 各 mock 文件仍按旧结构编写 `{ code, data, msg }` / 分页 `{ list, total }`，
 * 这里统一转换为后端当前的 ResultInfo 与 Spring Data Page 结构。
 */
const API_BASE = `${import.meta.env.VITE_API_BASE ?? "/dev-api"}`.replace(/\/+$/, "");
const API_PREFIX = "/api";

interface LegacyResponse {
  code: string;
  data?: unknown;
  msg?: string;
}

/** 旧版分页结构 { list, total } */
interface LegacyPage {
  list: unknown[];
  total: number;
}

function isLegacyPage(value: unknown): value is LegacyPage {
  return (
    typeof value === "object" &&
    value !== null &&
    Array.isArray((value as LegacyPage).list) &&
    typeof (value as LegacyPage).total === "number"
  );
}

/** 转换为 Spring Data Page 的 JSON 结构，页码/每页条数取自请求参数 */
function toPage(page: LegacyPage, request: any) {
  const query = request?.query ?? {};
  const size = Math.max(1, Number(query.pageSize) || 10);
  const number = Math.max(0, (Number(query.pageNum) || 1) - 1);
  const totalPages = Math.ceil(page.total / size);
  return {
    content: page.list,
    totalElements: page.total,
    totalPages,
    number,
    size,
    numberOfElements: page.list.length,
    first: number === 0,
    last: number >= totalPages - 1,
    empty: page.list.length === 0,
  };
}

/** 转换为 ResultInfo 结构 */
function toResultInfo(body: unknown, request?: any): unknown {
  const legacy = body as LegacyResponse | undefined;
  if (!legacy || typeof legacy !== "object" || legacy.code !== "00000") {
    return body;
  }
  return {
    success: true,
    resultCode: 200,
    resultMsg: legacy.msg ?? "OK",
    resultBody: isLegacyPage(legacy.data) ? toPage(legacy.data, request) : legacy.data,
  };
}

export const defineMock = createDefineMock((mock: any) => {
  // 拼接完整请求路径：/dev-api/api/users
  mock.url = `${API_BASE}${API_PREFIX}/${`${mock.url}`.replace(/^\/+/, "")}`;

  const body = mock.body;
  if (typeof body === "function") {
    mock.body = (request: any) => toResultInfo(body(request), request);
  } else {
    mock.body = toResultInfo(body);
  }
});
