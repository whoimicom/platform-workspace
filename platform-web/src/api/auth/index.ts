import request from "@/utils/request";
import type {
  CaptchaInfo,
  LoginRequest,
  LoginResult,
  OauthAuthorizeUrl,
  OauthBindRequest,
  OauthClientConfig,
  OauthLoginQuery,
  OauthLoginResult,
  OauthProvider,
  SmsLoginRequest,
  SmsSendRequest,
} from "./types";

const AUTH_BASE_URL = "/api/auth";

const AuthAPI = {
  /**
   * 登录
   */
  login(data: LoginRequest) {
    const payload: Pick<LoginRequest, "username" | "password" | "captchaToken" | "captchaCode"> = {
      username: data.username,
      password: data.password,
      captchaToken: data.captchaToken,
      captchaCode: data.captchaCode,
    };

    return request<unknown, LoginResult>({
      url: `${AUTH_BASE_URL}/login`,
      method: "post",
      data: payload,
      // 白名单接口携带过期令牌仍会被 Spring Security 判 401
      anonymous: true,
    });
  },

  /**
   * 获取验证码图片
   */
  getCaptcha() {
    return request<unknown, CaptchaInfo>({
      url: `${AUTH_BASE_URL}/captcha`,
      method: "get",
      // 残留过期令牌会让白名单接口 401，登录页取不到验证码
      anonymous: true,
    });
  },

  /**
   * 退出登录
   */
  logout() {
    return request({
      url: `${AUTH_BASE_URL}/logout`,
      method: "delete",
    });
  },

  /**
   * 用刷新令牌换取新的访问令牌
   */
  refreshToken(refreshToken: string) {
    return request<unknown, LoginResult>({
      url: `${AUTH_BASE_URL}/refresh-token`,
      method: "post",
      params: { refreshToken },
      anonymous: true,
    });
  },

  // ---- 短信验证码 ----

  /**
   * 发送短信验证码
   */
  sendSmsCode(data: SmsSendRequest) {
    return request<unknown, void>({
      url: `${AUTH_BASE_URL}/sms/send`,
      method: "post",
      params: data,
      anonymous: true,
    });
  },

  /**
   * 手机号验证码直接登录
   */
  smsLogin(data: SmsLoginRequest) {
    return request<unknown, LoginResult>({
      url: `${AUTH_BASE_URL}/sms/login`,
      method: "post",
      data,
      anonymous: true,
    });
  },

  // ---- 三方登录 ----

  /**
   * 获取前端内嵌二维码所需的公开配置（appId / corpId / agentId）
   */
  getOauthClientConfig() {
    return request<unknown, OauthClientConfig>({
      url: `${AUTH_BASE_URL}/oauth/client-config`,
      method: "get",
      anonymous: true,
    });
  },

  /**
   * 获取三方授权页地址（前端跳转扫码/授权，回调携带 code + state）
   */
  getOauthAuthorizeUrl(provider: OauthProvider) {
    return request<unknown, OauthAuthorizeUrl>({
      url: `${AUTH_BASE_URL}/oauth/authorize-url`,
      method: "get",
      params: { provider },
      anonymous: true,
    });
  },

  /**
   * 三方 code 换登录态（已绑定直接登录，未绑定返回 bindToken）
   */
  oauthLogin(data: OauthLoginQuery) {
    return request<unknown, OauthLoginResult>({
      url: `${AUTH_BASE_URL}/oauth/login`,
      method: "post",
      data,
      anonymous: true,
    });
  },

  /**
   * 三方身份 + 手机号验证码绑定后登录
   */
  oauthBind(data: OauthBindRequest) {
    return request<unknown, LoginResult>({
      url: `${AUTH_BASE_URL}/oauth/bind`,
      method: "post",
      data,
      anonymous: true,
    });
  },
};

export default AuthAPI;

export * from "./types";
