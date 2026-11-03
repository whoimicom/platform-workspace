use serde::Deserialize;
use std::env;
use std::fs;

#[derive(Clone, Deserialize)]
pub struct Config {
    pub issuer: String,
    pub port: u16,
    pub cors_origins: Vec<String>,
    /// MySQL 连接串（复用 platform 库的 sys_user 作为用户主数据）
    pub db_url: String,
}

impl Config {
    /// 加载配置：先读 config.toml（存在则加载），再用环境变量覆盖。
    ///
    /// 环境变量名遵循 `AUTH_` 前缀 + TOML 全路径全大写：
    ///   AUTH_ISSUER, AUTH_PORT, AUTH_CORS_ORIGINS（逗号分隔）
    ///   AUTH_DB_URL
    ///
    /// 语义对齐 Java 项目的 `${VAR:default}`：环境变量 > 本地配置 > 硬编码默认。
    pub fn load() -> anyhow::Result<Self> {
        let mut toml_config: Option<RawConfig> = None;
        for candidate in ["config.toml", "config.example.toml"] {
            if let Ok(text) = fs::read_to_string(candidate) {
                match toml::from_str::<RawConfig>(&text) {
                    Ok(c) => {
                        tracing::info!("已加载本地配置 {candidate}");
                        toml_config = Some(c);
                        break;
                    }
                    Err(e) => tracing::warn!("解析 {candidate} 失败，忽略：{e}"),
                }
            }
        }

        let base = toml_config.unwrap_or_else(default_raw_config);

        let issuer = env_or("AUTH_ISSUER", Some(&base.issuer), "http://localhost:9000");
        let port = env::var("AUTH_PORT")
            .ok()
            .and_then(|v| v.parse::<u16>().ok())
            .unwrap_or(base.port);
        let cors_origins = match env::var("AUTH_CORS_ORIGINS") {
            Ok(v) if !v.is_empty() => v.split(',').map(|s| s.trim().to_string()).collect(),
            _ => base.cors_origins,
        };
        let db_url = env_or(
            "AUTH_DB_URL",
            Some(&base.db_url),
            "mysql://platform:platform123456@127.0.0.1:3306/platform",
        );

        Ok(Config { issuer, port, cors_origins, db_url })
    }

    /// 兼容旧入口名
    pub fn from_env() -> anyhow::Result<Self> {
        Self::load()
    }
}

/// TOML 解析时用的原始结构（字段可全部由环境变量覆盖）
#[derive(Clone, Deserialize)]
struct RawConfig {
    issuer: String,
    port: u16,
    cors_origins: Vec<String>,
    db_url: String,
}

fn env_or(
    key: &str,
    local: Option<&str>,
    hardcoded: &str,
) -> String {
    env::var(key)
        .ok()
        .filter(|v| !v.is_empty())
        .or_else(|| local.map(|s| s.to_string()))
        .unwrap_or_else(|| hardcoded.to_string())
}

fn default_raw_config() -> RawConfig {
    RawConfig {
        issuer: "http://localhost:9000".into(),
        port: 9000,
        cors_origins: vec![
            "http://localhost:3000".into(),
            "http://127.0.0.1:3000".into(),
        ],
        db_url: "mysql://platform:platform123456@127.0.0.1:3306/platform".into(),
    }
}

/// 已注册的 OIDC 客户端（白名单，不随部署环境变化，代码内维护）
pub struct ClientApp {
    pub id: &'static str,
    /// 机密客户端的 client_secret；None 表示公开客户端（强制 PKCE）
    pub secret: Option<&'static str>,
    pub redirect_uris: &'static [&'static str],
    pub post_logout_uris: &'static [&'static str],
}

pub static CLIENTS: &[ClientApp] = &[
    // 前端 SPA：公开客户端，授权码 + PKCE（hash 路由，回跳应用根路径，由前端守卫处理）
    ClientApp {
        id: "platform-web",
        secret: None,
        redirect_uris: &[
            "http://localhost:3000/platform-web/",
            "http://127.0.0.1:3000/platform-web/",
            "http://localhost:4173/platform-web/",
        ],
        post_logout_uris: &[
            "http://localhost:3000/platform-web/",
            "http://127.0.0.1:3000/platform-web/",
            "http://localhost:4173/platform-web/",
        ],
    },
    // 后端 API：机密客户端（资源服务器/未来服务间调用）
    ClientApp {
        id: "platform-api",
        secret: Some("platform-api-secret-2026"),
        redirect_uris: &[],
        post_logout_uris: &[],
    },
];

pub fn client(client_id: &str) -> Option<&'static ClientApp> {
    CLIENTS.iter().find(|c| c.id == client_id)
}
