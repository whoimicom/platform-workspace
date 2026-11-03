use crate::config;
use crate::db;
use crate::store::{AuthCodeRecord, RefreshRecord, CODE_TTL, REFRESH_TTL};
use crate::tokens::s256_base64url;

use axum::extract::{Form, Query, State};
use axum::http::header::{AUTHORIZATION, SET_COOKIE};
use axum::http::{HeaderMap, HeaderValue, StatusCode};
use axum::response::{Html, IntoResponse, Redirect, Response};
use axum::Json;
use serde::Deserialize;
use serde_json::{json, Value};
use std::time::Instant;

use crate::AppState;

pub const SESSION_COOKIE: &str = "auth_session";

type ApiResult<T> = Result<T, ApiError>;

pub struct ApiError(StatusCode, Value);

impl IntoResponse for ApiError {
    fn into_response(self) -> Response {
        (self.0, Json(self.1)).into_response()
    }
}

fn bad_request(desc: &str) -> ApiError {
    ApiError(
        StatusCode::BAD_REQUEST,
        json!({ "error": "invalid_request", "error_description": desc }),
    )
}

fn invalid_grant(desc: &str) -> ApiError {
    ApiError(
        StatusCode::BAD_REQUEST,
        json!({ "error": "invalid_grant", "error_description": desc }),
    )
}

// ---------------------------------------------------------------------------
// 发现与密钥
// ---------------------------------------------------------------------------

pub async fn discovery(State(state): State<AppState>) -> Json<Value> {
    let issuer = &state.config.issuer;
    Json(json!({
        "issuer": issuer,
        "authorization_endpoint": format!("{issuer}/authorize"),
        "token_endpoint": format!("{issuer}/token"),
        "userinfo_endpoint": format!("{issuer}/userinfo"),
        "jwks_uri": format!("{issuer}/jwks.json"),
        "end_session_endpoint": format!("{issuer}/logout"),
        "response_types_supported": ["code"],
        "grant_types_supported": ["authorization_code", "refresh_token"],
        "subject_types_supported": ["public"],
        "id_token_signing_alg_values_supported": ["RS256"],
        "scopes_supported": ["openid", "profile"],
        "token_endpoint_auth_methods_supported": ["none", "client_secret_post"],
        "claims_parameter_supported": false,
        "code_challenge_methods_supported": ["S256"],
        "claims_supported": ["sub", "uid", "preferred_username", "nickname", "iss", "aud", "exp", "iat"],
    }))
}

pub async fn jwks(State(state): State<AppState>) -> Json<Value> {
    Json(state.tokens.jwks())
}

// ---------------------------------------------------------------------------
// 授权端点
// ---------------------------------------------------------------------------

#[derive(Debug, Clone, Deserialize)]
pub struct AuthorizeParams {
    pub response_type: Option<String>,
    pub client_id: Option<String>,
    pub redirect_uri: Option<String>,
    pub scope: Option<String>,
    pub state: Option<String>,
    pub nonce: Option<String>,
    pub code_challenge: Option<String>,
    pub code_challenge_method: Option<String>,
    pub username: Option<String>,
    pub password: Option<String>,
    pub error: Option<String>,
}

impl AuthorizeParams {
    /// 校验客户端与 PKCE 参数，通过返回 (client, redirect_uri)
    fn validate(&self) -> Result<(&'static config::ClientApp, String), String> {
        let client_id = self.client_id.as_deref().unwrap_or_default();
        let Some(client) = config::client(client_id) else {
            return Err("未知的 client_id".into());
        };
        let redirect_uri = self.redirect_uri.clone().unwrap_or_default();
        if !client.redirect_uris.contains(&redirect_uri.as_str()) {
            return Err("redirect_uri 未注册".into());
        }
        if self.response_type.as_deref() != Some("code") {
            return Err("仅支持 response_type=code".into());
        }
        if self.code_challenge.as_deref().unwrap_or_default().is_empty() {
            return Err("公开客户端必须提供 code_challenge（PKCE）".into());
        }
        if self.code_challenge_method.as_deref() != Some("S256") {
            return Err("仅支持 code_challenge_method=S256".into());
        }
        Ok((client, redirect_uri))
    }
}

pub async fn authorize(
    State(state): State<AppState>,
    Query(params): Query<AuthorizeParams>,
    headers: HeaderMap,
) -> Response {
    tracing::info!(
        "OIDC authorize GET: client_id={} redirect_uri={} response_type={} has_pkce={}",
        params.client_id.as_deref().unwrap_or(""),
        params.redirect_uri.as_deref().unwrap_or(""),
        params.response_type.as_deref().unwrap_or(""),
        params.code_challenge.as_deref().map(|c| !c.is_empty()).unwrap_or(false),
    );
    match do_authorize(&state, &params, &headers).await {
        Ok(resp) => resp,
        Err(msg) => {
            tracing::warn!("OIDC authorize 失败: {msg}");
            Html(login_page(&params, Some(&msg))).into_response()
        }
    }
}

async fn do_authorize(
    state: &AppState,
    params: &AuthorizeParams,
    headers: &HeaderMap,
) -> Result<Response, String> {
    let (_, redirect_uri) = params.validate()?;

    // 已有 SSO 会话：直接发授权码
    if let Some(sid) = get_cookie(headers, SESSION_COOKIE) {
        if let Some(session) = state.store.get_session(&sid) {
            return issue_code_redirect(state, params, &redirect_uri, &session).await;
        }
    }
    // 未登录：渲染统一登录页
    Ok(Html(login_page(params, None)).into_response())
}

#[derive(Debug, Clone, Deserialize)]
pub struct LoginParams {
    pub username: Option<String>,
    pub password: Option<String>,
    pub client_id: Option<String>,
    pub redirect_uri: Option<String>,
    pub scope: Option<String>,
    pub state: Option<String>,
    pub nonce: Option<String>,
    pub code_challenge: Option<String>,
    pub code_challenge_method: Option<String>,
}

impl From<LoginParams> for AuthorizeParams {
    fn from(p: LoginParams) -> Self {
        AuthorizeParams {
            response_type: Some("code".into()),
            client_id: p.client_id,
            redirect_uri: p.redirect_uri,
            scope: p.scope,
            state: p.state,
            nonce: p.nonce,
            code_challenge: p.code_challenge,
            code_challenge_method: p.code_challenge_method,
            username: None,
            password: None,
            error: None,
        }
    }
}

pub async fn authorize_login(
    State(state): State<AppState>,
    Form(form): Form<LoginParams>,
) -> Response {
    tracing::info!(
        "OIDC authorize/login POST: username={} client_id={} redirect_uri={}",
        form.username.as_deref().unwrap_or(""),
        form.client_id.as_deref().unwrap_or(""),
        form.redirect_uri.as_deref().unwrap_or(""),
    );
    let params = AuthorizeParams::from(form.clone());
    match do_login(state, &form, &params).await {
        Ok(resp) => {
            tracing::info!("OIDC 登录成功: username={}", form.username.as_deref().unwrap_or(""));
            resp
        }
        Err(msg) => {
            tracing::warn!("OIDC 登录失败: {msg}");
            Html(login_page(&params, Some(&msg))).into_response()
        }
    }
}

async fn do_login(
    state: AppState,
    form: &LoginParams,
    params: &AuthorizeParams,
) -> Result<Response, String> {
    let (_, redirect_uri) = params.validate()?;

    let username = form.username.as_deref().unwrap_or_default().trim().to_string();
    let password = form.password.as_deref().unwrap_or_default();
    if username.is_empty() || password.is_empty() {
        return Err("用户名和密码不能为空".into());
    }

    let user = db::find_user_by_username(&state.pool, &username)
        .await
        .map_err(|e| {
            tracing::error!("查询用户失败: {e}");
            "登录服务暂时不可用，请稍后再试".to_string()
        })?
        .ok_or("用户名或密码错误")?;
    if user.status != 1 {
        return Err("账号已被禁用，请联系管理员".into());
    }
    let ok = bcrypt::verify(password, &user.password).map_err(|e| {
        tracing::error!("bcrypt 校验失败: {e}");
        "登录服务暂时不可用，请稍后再试".to_string()
    })?;
    if !ok {
        return Err("用户名或密码错误".into());
    }

    // 建立 SSO 会话并下发授权码
    let sid = state
        .store
        .create_session(user.username.clone(), user.id, user.nickname.clone());
    let session = state.store.get_session(&sid).expect("会话刚创建");
    let resp = issue_code_redirect(&state, params, &redirect_uri, &session).await?;
    let cookie = format!(
        "{SESSION_COOKIE}={sid}; Path=/; HttpOnly; SameSite=Lax; Max-Age={}",
        crate::store::SESSION_TTL.as_secs()
    );
    Ok(set_cookie(resp, cookie))
}

/// 生成一次性授权码并 302 回客户端
async fn issue_code_redirect(
    state: &AppState,
    params: &AuthorizeParams,
    redirect_uri: &str,
    session: &crate::store::SessionRecord,
) -> Result<Response, String> {
    let code = uuid::Uuid::new_v4().to_string();
    state.store.put_code(
        code.clone(),
        AuthCodeRecord {
            client_id: params.client_id.clone().unwrap_or_default(),
            redirect_uri: redirect_uri.to_string(),
            username: session.username.clone(),
            uid: session.uid,
            nickname: session.nickname.clone(),
            nonce: params.nonce.clone(),
            code_challenge: params.code_challenge.clone().unwrap_or_default(),
            expires_at: Instant::now() + CODE_TTL,
        },
    );
    let target = format!(
        "{}?code={}&state={}",
        redirect_uri,
        urlencoding::encode(&code),
        urlencoding::encode(params.state.as_deref().unwrap_or_default())
    );
    Ok(Redirect::to(&target).into_response())
}

// ---------------------------------------------------------------------------
// 令牌端点
// ---------------------------------------------------------------------------

#[derive(Debug, Deserialize)]
pub struct TokenParams {
    pub grant_type: Option<String>,
    pub code: Option<String>,
    pub redirect_uri: Option<String>,
    pub client_id: Option<String>,
    pub client_secret: Option<String>,
    pub code_verifier: Option<String>,
    pub refresh_token: Option<String>,
}

pub async fn token(
    State(state): State<AppState>,
    Form(params): Form<TokenParams>,
) -> ApiResult<Json<Value>> {
    tracing::info!(
        "OIDC /token: grant_type={} client_id={} has_code={} has_refresh={}",
        params.grant_type.as_deref().unwrap_or(""),
        params.client_id.as_deref().unwrap_or(""),
        params.code.as_deref().map(|c| !c.is_empty()).unwrap_or(false),
        params.refresh_token.as_deref().map(|t| !t.is_empty()).unwrap_or(false),
    );
    let result = match params.grant_type.as_deref() {
        Some("authorization_code") => token_by_code(axum::extract::State(state), params).await,
        Some("refresh_token") => token_by_refresh(axum::extract::State(state), params).await,
        _ => Err(bad_request("不支持的 grant_type")),
    };
    match &result {
        Ok(_) => tracing::info!("OIDC /token 成功"),
        Err(e) => tracing::warn!("OIDC /token 失败: {} {}", e.0.as_u16(), e.1),
    }
    result
}

async fn token_by_code(
    state: State<AppState>,
    params: TokenParams,
) -> ApiResult<Json<Value>> {
    let client_id = params.client_id.clone().unwrap_or_default();
    let Some(client) = config::client(&client_id) else {
        return Err(bad_request("未知的 client_id"));
    };
    check_client_secret(client, params.client_secret.as_deref())?;

    let code = params.code.clone().unwrap_or_default();
    let rec = state
        .store
        .take_code(&code)
        .ok_or_else(|| invalid_grant("授权码无效或已过期"))?;
    if rec.client_id != client_id {
        return Err(invalid_grant("授权码与客户端不匹配"));
    }
    if rec.redirect_uri != params.redirect_uri.clone().unwrap_or_default() {
        return Err(invalid_grant("redirect_uri 与授权请求不一致"));
    }
    let verifier = params.code_verifier.clone().unwrap_or_default();
    if s256_base64url(&verifier) != rec.code_challenge {
        return Err(invalid_grant("PKCE 校验失败"));
    }

    // 发放令牌前再次校验用户状态，禁用用户立即失效
    let user = ensure_enabled_user(&state, rec.uid).await?;

    let (access_token, expires_in) = state
        .tokens
        .issue_access(&user.username, user.id, user.nickname.as_deref(), &client_id)
        .map_err(|e| internal(e))?;
    let id_token = state
        .tokens
        .issue_id(
            &user.username,
            user.nickname.as_deref(),
            &client_id,
            rec.nonce.as_deref(),
        )
        .map_err(internal)?;
    let refresh_token = issue_refresh(&state, &user.username, user.id, &client_id);

    Ok(Json(json!({
        "access_token": access_token,
        "token_type": "Bearer",
        "expires_in": expires_in,
        "refresh_token": refresh_token,
        "id_token": id_token,
        "scope": "openid",
    })))
}

async fn token_by_refresh(
    state: State<AppState>,
    params: TokenParams,
) -> ApiResult<Json<Value>> {
    let client_id = params.client_id.clone().unwrap_or_default();
    if config::client(&client_id).is_none() {
        return Err(bad_request("未知的 client_id"));
    }
    let refresh_token = params.refresh_token.clone().unwrap_or_default();
    let rec = state
        .store
        .take_refresh(&refresh_token)
        .ok_or_else(|| invalid_grant("刷新令牌无效或已过期"))?;
    if rec.client_id != client_id {
        return Err(invalid_grant("刷新令牌与客户端不匹配"));
    }

    let user = ensure_enabled_user(&state, rec.uid).await?;

    let (access_token, expires_in) = state
        .tokens
        .issue_access(&user.username, user.id, user.nickname.as_deref(), &client_id)
        .map_err(internal)?;
    let id_token = state
        .tokens
        .issue_id(&user.username, user.nickname.as_deref(), &client_id, None)
        .map_err(internal)?;
    // 轮换：旧令牌已删除，换发新令牌
    let new_refresh = issue_refresh(&state, &user.username, user.id, &client_id);

    Ok(Json(json!({
        "access_token": access_token,
        "token_type": "Bearer",
        "expires_in": expires_in,
        "refresh_token": new_refresh,
        "id_token": id_token,
        "scope": "openid",
    })))
}

fn check_client_secret(
    client: &'static config::ClientApp,
    provided: Option<&str>,
) -> Result<(), ApiError> {
    match client.secret {
        Some(secret) if provided == Some(secret) => Ok(()),
        Some(_) => Err(ApiError(
            StatusCode::UNAUTHORIZED,
            json!({ "error": "invalid_client", "error_description": "客户端认证失败" }),
        )),
        None => Ok(()),
    }
}

fn issue_refresh(state: &AppState, username: &str, uid: i64, client_id: &str) -> String {
    let token = uuid::Uuid::new_v4().to_string() + uuid::Uuid::new_v4().to_string().as_str();
    state.store.put_refresh(
        token.clone(),
        RefreshRecord {
            username: username.to_string(),
            uid,
            client_id: client_id.to_string(),
            expires_at: Instant::now() + REFRESH_TTL,
        },
    );
    token
}

async fn ensure_enabled_user(state: &AppState, uid: i64) -> Result<db::UserRow, ApiError> {
    let user = db::find_user_by_id(&state.pool, uid)
        .await
        .map_err(internal)?
        .ok_or_else(|| invalid_grant("用户不存在"))?;
    if user.status != 1 {
        return Err(invalid_grant("账号已被禁用"));
    }
    Ok(user)
}

fn internal(e: anyhow::Error) -> ApiError {
    tracing::error!("签发令牌失败: {e:#}");
    ApiError(
        StatusCode::INTERNAL_SERVER_ERROR,
        json!({ "error": "server_error", "error_description": "签发令牌失败" }),
    )
}

// ---------------------------------------------------------------------------
// 用户信息 / 登出 / 统一管理
// ---------------------------------------------------------------------------

pub async fn userinfo(
    State(state): State<AppState>,
    headers: HeaderMap,
) -> ApiResult<Json<Value>> {
    let token = bearer_token(&headers).ok_or_else(|| {
        ApiError(
            StatusCode::UNAUTHORIZED,
            json!({ "error": "invalid_token", "error_description": "缺少访问令牌" }),
        )
    })?;
    let claims = state.tokens.decode_access(token).map_err(|_| {
        ApiError(
            StatusCode::UNAUTHORIZED,
            json!({ "error": "invalid_token", "error_description": "访问令牌无效或已过期" }),
        )
    })?;
    let uid: i64 = claims.uid.parse().map_err(|_| bad_request("令牌声明异常"))?;
    let user = ensure_enabled_user(&state, uid).await?;

    Ok(Json(json!({
        "sub": user.username,
        "uid": user.id.to_string(),
        "preferred_username": user.username,
        "nickname": user.nickname,
    })))
}

#[derive(Deserialize)]
pub struct LogoutParams {
    pub client_id: Option<String>,
    pub post_logout_redirect_uri: Option<String>,
}

pub async fn logout(
    State(state): State<AppState>,
    Query(params): Query<LogoutParams>,
    headers: HeaderMap,
) -> Response {
    tracing::info!(
        "OIDC /logout: client_id={} post_redirect={} has_session={}",
        params.client_id.as_deref().unwrap_or(""),
        params.post_logout_redirect_uri.as_deref().unwrap_or(""),
        get_cookie(&headers, SESSION_COOKIE).is_some(),
    );
    if let Some(sid) = get_cookie(&headers, SESSION_COOKIE) {
        state.store.remove_session(&sid);
        tracing::info!("OIDC SSO 会话已销毁: sid={}", &sid[..8.min(sid.len())]);
    }
    let cookie = format!("{SESSION_COOKIE}=; Path=/; HttpOnly; SameSite=Lax; Max-Age=0");
    let allow_redirect = match (&params.client_id, &params.post_logout_redirect_uri) {
        (Some(id), Some(uri)) => config::client(id)
            .map(|c| c.post_logout_uris.contains(&uri.as_str()))
            .unwrap_or(false),
        _ => false,
    };
    if allow_redirect {
        let uri = params.post_logout_redirect_uri.unwrap();
        set_cookie(Redirect::to(&uri).into_response(), cookie)
    } else {
        set_cookie(
            Html("<html><body><p>已退出统一认证</p></body></html>").into_response(),
            cookie,
        )
    }
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ChangePasswordBody {
    pub old_password: String,
    pub new_password: String,
}

/// 改密码（统一认证中心是全公司唯一密码入口）
pub async fn change_password(
    State(state): State<AppState>,
    headers: HeaderMap,
    Json(body): Json<ChangePasswordBody>,
) -> ApiResult<Json<Value>> {
    let session = require_session(&state, &headers).await?;
    let user = db::find_user_by_id(&state.pool, session.uid)
        .await
        .map_err(internal)?
        .ok_or_else(|| ApiError(StatusCode::UNAUTHORIZED, json!({ "message": "用户不存在" })))?;

    let old_ok = bcrypt::verify(&body.old_password, &user.password).map_err(|e| internal(e.into()))?;
    if !old_ok {
        return Err(ApiError(StatusCode::BAD_REQUEST, json!({ "message": "原密码不正确" })));
    }
    if body.new_password.len() < 6 {
        return Err(ApiError(StatusCode::BAD_REQUEST, json!({ "message": "新密码长度不能少于 6 位" })));
    }
    let hash = bcrypt::hash(&body.new_password, 10).map_err(|e| internal(e.into()))?;
    db::update_password(&state.pool, session.uid, &hash).await.map_err(internal)?;
    Ok(Json(json!({ "message": "密码修改成功，请使用新密码重新登录" })))
}

#[derive(Deserialize)]
pub struct UserStatusBody {
    pub username: String,
    /// 1 启用，0 禁用（离职注销）
    pub status: i8,
}

/// 禁用/启用用户（ROOT 专用；禁用后该用户所有系统的令牌都无法继续换发）
pub async fn set_user_status(
    State(state): State<AppState>,
    headers: HeaderMap,
    Json(body): Json<UserStatusBody>,
) -> ApiResult<Json<Value>> {
    let session = require_session(&state, &headers).await?;
    let is_root = db::is_root_user(&state.pool, session.uid).await.map_err(internal)?;
    if !is_root {
        return Err(ApiError(StatusCode::FORBIDDEN, json!({ "message": "需要 ROOT 权限" })));
    }
    if body.status != 0 && body.status != 1 {
        return Err(bad_request("status 仅支持 0 或 1"));
    }
    let updated = db::update_status(&state.pool, &body.username, body.status)
        .await
        .map_err(internal)?;
    if !updated {
        return Err(ApiError(StatusCode::NOT_FOUND, json!({ "message": "用户不存在" })));
    }
    Ok(Json(json!({ "message": "操作成功" })))
}

// ---------------------------------------------------------------------------
// 辅助
// ---------------------------------------------------------------------------

async fn require_session(
    state: &AppState,
    headers: &HeaderMap,
) -> Result<crate::store::SessionRecord, ApiError> {
    let sid = get_cookie(headers, SESSION_COOKIE).ok_or_else(|| {
        ApiError(StatusCode::UNAUTHORIZED, json!({ "message": "未登录" }))
    })?;
    state.store.get_session(&sid).ok_or_else(|| {
        ApiError(StatusCode::UNAUTHORIZED, json!({ "message": "会话已过期，请重新登录" }))
    })
}

fn bearer_token<'a>(headers: &'a HeaderMap) -> Option<&'a str> {
    let value = headers.get(AUTHORIZATION)?.to_str().ok()?;
    value.strip_prefix("Bearer ").map(|t| t.trim())
}

fn get_cookie(headers: &HeaderMap, name: &str) -> Option<String> {
    let raw = headers.get(axum::http::header::COOKIE)?.to_str().ok()?;
    raw.split("; ").find_map(|kv| {
        let (k, v) = kv.split_once('=')?;
        (k.trim() == name).then(|| v.trim().to_string())
    })
}

fn set_cookie(mut resp: Response, cookie: String) -> Response {
    if let Ok(value) = HeaderValue::from_str(&cookie) {
        resp.headers_mut().append(SET_COOKIE, value);
    }
    resp
}

/// 统一登录页（自包含 HTML，登录成功后以授权码回跳客户端）
fn login_page(params: &AuthorizeParams, error: Option<&str>) -> String {
    let esc = |s: &str| urlencoding::encode(s).to_string();
    let hidden = |name: &str, value: &str| {
        if value.is_empty() {
            String::new()
        } else {
            format!(r#"<input type="hidden" name="{name}" value="{}" />"#, esc(value))
        }
    };
    let error_html = error
        .map(|msg| format!(r#"<div class="error">{}</div>"#, esc(msg)))
        .unwrap_or_default();
    let auth_error_html = params
        .error
        .as_deref()
        .map(|e| format!(r#"<div class="error">客户端传入错误：{}</div>"#, esc(e)))
        .unwrap_or_default();

    format!(
        r#"<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="utf-8" />
<meta name="viewport" content="width=device-width, initial-scale=1" />
<title>统一认证登录</title>
<style>
  * {{ box-sizing: border-box; margin: 0; padding: 0; }}
  body {{ font-family: "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif;
    min-height: 100vh; display: flex; align-items: center; justify-content: center;
    background: linear-gradient(160deg, #eef2ff 0%, #dbeafe 100%); }}
  .card {{ width: 380px; padding: 40px 36px; background: #fff; border-radius: 16px;
    box-shadow: 0 20px 50px rgb(30 64 175 / 12%); }}
  h1 {{ font-size: 22px; color: #273248; margin-bottom: 6px; }}
  .desc {{ font-size: 13px; color: #98a2b3; margin-bottom: 24px; }}
  .error {{ background: #fef2f2; color: #dc2626; border: 1px solid #fecaca;
    border-radius: 8px; padding: 10px 12px; font-size: 13px; margin-bottom: 16px; }}
  input[type=text], input[type=password] {{ width: 100%; height: 44px; padding: 0 14px;
    margin-bottom: 14px; border: 1px solid #d0d5dd; border-radius: 8px; font-size: 14px;
    outline: none; transition: border-color .15s; }}
  input:focus {{ border-color: #4f7cff; }}
  button {{ width: 100%; height: 44px; border: 0; border-radius: 8px; font-size: 15px;
    font-weight: 600; color: #fff; background: #4f7cff; cursor: pointer; margin-top: 4px; }}
  button:hover {{ background: #3f6cf0; }}
  .tip {{ margin-top: 18px; font-size: 12px; color: #98a2b3; text-align: center; }}
</style>
</head>
<body>
<form class="card" method="post" action="/authorize/login">
  <h1>统一认证中心</h1>
  <p class="desc">登录后将返回你正在访问的系统</p>
  {auth_error_html}
  {error_html}
  <input type="text" name="username" placeholder="用户名" autocomplete="username" autofocus />
  <input type="password" name="password" placeholder="密码" autocomplete="current-password" />
  {hidden_client_id}{hidden_redirect_uri}{hidden_scope}{hidden_state}{hidden_nonce}{hidden_challenge}{hidden_method}
  <button type="submit">登 录</button>
  <p class="tip">platform-rs-auth · OIDC Provider</p>
</form>
</body>
</html>"#,
        auth_error_html = auth_error_html,
        error_html = error_html,
        hidden_client_id = hidden("client_id", params.client_id.as_deref().unwrap_or_default()),
        hidden_redirect_uri = hidden("redirect_uri", params.redirect_uri.as_deref().unwrap_or_default()),
        hidden_scope = hidden("scope", params.scope.as_deref().unwrap_or_default()),
        hidden_state = hidden("state", params.state.as_deref().unwrap_or_default()),
        hidden_nonce = hidden("nonce", params.nonce.as_deref().unwrap_or_default()),
        hidden_challenge = hidden("code_challenge", params.code_challenge.as_deref().unwrap_or_default()),
        hidden_method = hidden("code_challenge_method", params.code_challenge_method.as_deref().unwrap_or_default()),
    )
}
