import type { AxiosResponse } from "axios";
import type { ResultInfo } from "@/api/common";

/**
 * 读取文件接口返回的 JSON 业务结果，保留原始文件和响应头。
 */
export async function readBinaryApiResult(
  response: AxiosResponse<unknown>
): Promise<ResultInfo | undefined> {
  if (response.config.responseType !== "blob" && response.config.responseType !== "arraybuffer") {
    return undefined;
  }

  const contentType = String(response.headers["content-type"] ?? "")
    .split(";")[0]
    .trim()
    .toLowerCase();
  if (
    contentType !== "application/json" &&
    !(contentType.startsWith("application/") && contentType.endsWith("+json"))
  ) {
    return undefined;
  }

  let text: string;
  if (response.data instanceof Blob) text = await response.data.text();
  else if (response.data instanceof ArrayBuffer) text = new TextDecoder().decode(response.data);
  else return undefined;

  let value: unknown;
  try {
    value = JSON.parse(text);
  } catch (cause) {
    throw new Error("文件接口返回了无效的 JSON", { cause });
  }
  if (
    typeof value !== "object" ||
    value === null ||
    !("success" in value) ||
    typeof value.success !== "boolean" ||
    !("resultCode" in value) ||
    typeof value.resultCode !== "number" ||
    !("resultMsg" in value) ||
    typeof value.resultMsg !== "string"
  ) {
    return undefined;
  }
  return {
    ...value,
    success: value.success,
    resultCode: value.resultCode,
    resultMsg: value.resultMsg,
    resultBody: "resultBody" in value ? (value.resultBody as unknown) : undefined,
  } as ResultInfo;
}
