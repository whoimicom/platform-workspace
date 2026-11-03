import request from "@/utils/request";

const AUTH_BASE_URL = "/api/auth";

const AuthAPI = {
  /**
   * 退出登录（本地会话清理；SSO 会话登出跳转见 oidc.ts）
   */
  logout() {
    return request({
      url: `${AUTH_BASE_URL}/logout`,
      method: "delete",
    });
  },
};

export default AuthAPI;
