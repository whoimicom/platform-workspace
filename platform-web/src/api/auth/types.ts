/**
 * 登录请求参数
 */
export interface LoginRequest {
  /** 用户名 */
  username: string;
  /** 密码 */
  password: string;
  /** 验证码签名 token（无状态，携带指纹绑定） */
  captchaToken?: string;
  /** 验证码 */
  captchaCode?: string;
  /** 记住我 */
  rememberMe?: boolean;
}

/**
 * 登录结果
 */
export interface LoginResult {
  /** 访问令牌 */
  accessToken: string;
  /** 刷新令牌 */
  refreshToken: string;
  /** 令牌类型 */
  tokenType: string;
  /** 过期时间(单位:秒) */
  expiresIn: number;
}

/**
 * 验证码信息
 */
export interface CaptchaInfo {
  /** 验证码签名 token */
  captchaToken: string;
  /** 验证码图片 Base64 */
  captchaBase64: string;
}

// ---- 短信验证码 ----

export interface SmsSendRequest {
  mobile: string;
}

export interface SmsLoginRequest {
  mobile: string;
  smsCode: string;
}

// ---- 三方登录 ----

export type OauthProvider = "WECHAT" | "WECOM";

export interface OauthLoginQuery {
  provider: OauthProvider;
  /** 应用/企业 appId，可空 */
  appId?: string;
  /** 前端拿到的授权 code */
  code: string;
}

/** 三方授权页地址 */
export interface OauthAuthorizeUrl {
  /** 需打开的授权页 URL（微信扫码 / 企业微信授权） */
  url: string;
  /** 防 CSRF 的 state，回调时需原样带回并校验 */
  state: string;
}

/**
 * 三方登录结果（已绑定 or 需要绑定）
 */
export interface OauthLoginResult {
  /** 已绑定直接登录 */
  loggedIn: boolean;
  /** 需要手机号+验证码绑定 */
  needBind: boolean;
  /** 已绑定时返回 */
  accessToken?: string;
  refreshToken?: string;
  /** needBind=true 时返回，传给 /oauth/bind */
  bindToken?: string;
}

export interface OauthBindRequest {
  bindToken: string;
  mobile: string;
  smsCode: string;
}
