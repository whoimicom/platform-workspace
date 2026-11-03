/// <reference types="vite/client" />

/**
 * Vite 环境变量类型定义
 */
interface ImportMetaEnv {
  readonly VITE_API_BASE: string;
  readonly VITE_APP_TITLE?: string;
  readonly VITE_MOCK_ENABLED?: string;
  /** 统一认证中心地址（OIDC Provider：platform-rs-auth） */
  readonly VITE_AUTH_CENTER_URL: string;
  /** 在认证中心注册的客户端 ID */
  readonly VITE_OIDC_CLIENT_ID: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}

declare const __APP_INFO__: {
  pkg: {
    name: string;
    version: string;
  };
  buildTimestamp: number;
};
