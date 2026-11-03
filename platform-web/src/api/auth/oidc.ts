/**
 * OIDC 授权码 + PKCE 客户端（对接统一认证中心 platform-rs-auth）
 *
 * 路由为 hash 模式，认证中心回跳应用根路径，code/state 出现在
 * window.location.search 上，由路由守卫统一读取并换取令牌。
 */

const AUTH_CENTER_URL = import.meta.env.VITE_AUTH_CENTER_URL || "http://localhost:9000";
const CLIENT_ID = import.meta.env.VITE_OIDC_CLIENT_ID || "platform-web";

const PENDING_KEY = "oidc-pending-auth";

/**
 * 发起登录时暂存的上下文
 */
interface PendingAuth {
  verifier: string;
  state: string;
  redirect: string;
}

/**
 * 令牌结果
 */
export interface OidcTokens {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
}

/**
 * 应用根路径（同时作为 redirect_uri 与登出回落地址注册在认证中心）
 */
function appBaseUrl(): string {
  return `${window.location.origin}${import.meta.env.BASE_URL}`;
}

function readPending(): PendingAuth | null {
  try {
    const raw = sessionStorage.getItem(PENDING_KEY);
    return raw ? (JSON.parse(raw) as PendingAuth) : null;
  } catch {
    return null;
  }
}

function base64UrlFromBuffer(buffer: ArrayBuffer): string {
  const bytes = new Uint8Array(buffer);
  let binary = "";
  bytes.forEach((byte) => {
    binary += String.fromCharCode(byte);
  });
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

function randomToken(): string {
  const bytes = new Uint8Array(32);
  crypto.getRandomValues(bytes);
  return base64UrlFromBuffer(bytes.buffer);
}

/**
 * 发起统一认证登录：生成 PKCE 并整页跳转认证中心
 */
export async function startOidcLogin(redirectPath = "/"): Promise<void> {
  const verifier = randomToken();
  const state = randomToken();
  const digest = await crypto.subtle.digest(
    "SHA-256",
    new TextEncoder().encode(verifier)
  );
  const challenge = base64UrlFromBuffer(digest);

  sessionStorage.setItem(
    PENDING_KEY,
    JSON.stringify({ verifier, state, redirect: redirectPath } satisfies PendingAuth)
  );

  const params = new URLSearchParams({
    response_type: "code",
    client_id: CLIENT_ID,
    redirect_uri: appBaseUrl(),
    scope: "openid",
    state,
    code_challenge: challenge,
    code_challenge_method: "S256",
  });
  window.location.href = `${AUTH_CENTER_URL}/authorize?${params.toString()}`;
}

/**
 * 授权码换取令牌：校验 state 防 CSRF，返回令牌与登录前目标路径
 */
export async function exchangeOidcCode(
  code: string,
  state: string
): Promise<{ tokens: OidcTokens; redirect: string }> {
  const pending = readPending();
  if (!pending || pending.state !== state) {
    throw new Error("登录状态已失效，请重新发起登录");
  }
  const tokens = await tokenRequest({
    grant_type: "authorization_code",
    code,
    redirect_uri: appBaseUrl(),
    client_id: CLIENT_ID,
    code_verifier: pending.verifier,
  });
  sessionStorage.removeItem(PENDING_KEY);
  return { tokens, redirect: pending.redirect };
}

/**
 * 刷新令牌（认证中心侧轮换）
 */
export function refreshOidcTokens(refreshToken: string): Promise<OidcTokens> {
  return tokenRequest({
    grant_type: "refresh_token",
    refresh_token: refreshToken,
    client_id: CLIENT_ID,
  });
}

/**
 * 认证中心登出地址：终止 SSO 会话后回到应用根路径
 */
export function buildOidcLogoutUrl(): string {
  const params = new URLSearchParams({
    client_id: CLIENT_ID,
    post_logout_redirect_uri: appBaseUrl(),
  });
  return `${AUTH_CENTER_URL}/logout?${params.toString()}`;
}

async function tokenRequest(body: Record<string, string>): Promise<OidcTokens> {
  const response = await fetch(`${AUTH_CENTER_URL}/token`, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams(body),
  });
  const data = await response.json().catch(() => ({}));
  if (!response.ok || !data.access_token) {
    throw new Error(data.error_description || data.message || "获取令牌失败");
  }
  return {
    accessToken: data.access_token,
    refreshToken: data.refresh_token ?? "",
    expiresIn: Number(data.expires_in) || 0,
  };
}
