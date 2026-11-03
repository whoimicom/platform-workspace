mod config;
mod db;
mod handlers;
mod keys;
mod store;
mod tokens;

use anyhow::Context;
use sqlx::mysql::MySqlPoolOptions;
use std::sync::Arc;
use tower_http::cors::{AllowHeaders, AllowMethods, AllowOrigin, CorsLayer};

/// 全局共享状态
struct AppStateInner {
    pool: sqlx::MySqlPool,
    store: store::Store,
    tokens: tokens::TokenFactory,
    config: config::Config,
}

pub type AppState = Arc<AppStateInner>;

#[tokio::main]
async fn main() -> anyhow::Result<()> {
    tracing_subscriber::fmt()
        .with_env_filter(
            tracing_subscriber::EnvFilter::try_from_default_env()
                .unwrap_or_else(|_| "info".into()),
        )
        .init();

    let config = config::Config::load()?;
    let keys = keys::load_or_create(std::path::Path::new("keys"))?;
    tracing::info!("签名密钥就绪 (kid={})", keys.kid);
    tracing::info!("配置已加载: issuer={} port={} db={}", config.issuer, config.port, config.db_url);

    let pool = {
        let connect_future = MySqlPoolOptions::new()
            .max_connections(5)
            .min_connections(1)
            // 运行时请求等待空闲连接的超时：默认 30s 会让浏览器看起来"没反应"，改为 5s
            .acquire_timeout(std::time::Duration::from_secs(5))
            .connect(&config.db_url);
        tokio::time::timeout(
            std::time::Duration::from_secs(5),
            connect_future,
        )
        .await
        .context("连接 MySQL 超时（请先启动 MySQL，或检查 AUTH_DB_URL / config.toml 中的 db_url）")?
        .context("连接 MySQL 失败")?
    };
    sqlx::query("SELECT 1").execute(&pool).await.context("数据库连通性检查失败")?;
    tracing::info!("MySQL 用户主数据已连接");
    tracing::info!("提示: 所有用户登录/改密/注销操作都依赖此 MySQL 中的 sys_user 表");

    let state: AppState = Arc::new(AppStateInner {
        pool,
        store: store::Store::default(),
        tokens: tokens::TokenFactory::new(&keys, config.issuer.clone()),
        config: config.clone(),
    });

    let cors = CorsLayer::new()
        .allow_origin(
            AllowOrigin::list(
                config
                    .cors_origins
                    .iter()
                    .filter_map(|o| o.parse().ok()),
            ),
        )
        .allow_methods(AllowMethods::list([
            axum::http::Method::GET,
            axum::http::Method::POST,
        ]))
        .allow_headers(AllowHeaders::list([
            axum::http::header::CONTENT_TYPE,
            axum::http::header::AUTHORIZATION,
        ]));

    let app = axum::Router::new()
        .route("/.well-known/openid-configuration", axum::routing::get(handlers::discovery))
        .route("/jwks.json", axum::routing::get(handlers::jwks))
        .route("/authorize", axum::routing::get(handlers::authorize))
        .route("/authorize/login", axum::routing::post(handlers::authorize_login))
        .route("/token", axum::routing::post(handlers::token))
        .route("/userinfo", axum::routing::get(handlers::userinfo))
        .route("/logout", axum::routing::get(handlers::logout))
        .route("/api/manage/password", axum::routing::post(handlers::change_password))
        .route("/api/manage/users/status", axum::routing::post(handlers::set_user_status))
        .layer(cors)
        .with_state(state);

    let addr = format!("0.0.0.0:{}", config.port);
    let listener = tokio::net::TcpListener::bind(&addr).await?;
    tracing::info!("OIDC 认证中心已启动: {} (issuer={})", addr, config.issuer);
    axum::serve(listener, app).await?;
    Ok(())
}
