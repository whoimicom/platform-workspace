use sqlx::MySqlPool;

/// sys_user 行（认证所需字段）
#[derive(Debug, sqlx::FromRow)]
pub struct UserRow {
    pub id: i64,
    pub username: String,
    pub password: String,
    pub nickname: Option<String>,
    pub status: i8,
}

pub async fn find_user_by_username(pool: &MySqlPool, username: &str) -> anyhow::Result<Option<UserRow>> {
    let user = sqlx::query_as::<_, UserRow>(
        "SELECT id, username, password, nickname, status FROM sys_user WHERE username = ?",
    )
    .bind(username)
    .fetch_optional(pool)
    .await?;
    Ok(user)
}

pub async fn find_user_by_id(pool: &MySqlPool, uid: i64) -> anyhow::Result<Option<UserRow>> {
    let user = sqlx::query_as::<_, UserRow>(
        "SELECT id, username, password, nickname, status FROM sys_user WHERE id = ?",
    )
    .bind(uid)
    .fetch_optional(pool)
    .await?;
    Ok(user)
}

/// 判断用户是否持有启用的 ROOT 角色（统一管理操作鉴权用）
pub async fn is_root_user(pool: &MySqlPool, uid: i64) -> anyhow::Result<bool> {
    let (count,): (i64,) = sqlx::query_as(concat!(
        "SELECT COUNT(*) FROM sys_user_role ur ",
        "JOIN sys_role r ON r.id = ur.role_id ",
        "WHERE ur.user_id = ? AND r.code = 'ROOT' AND r.status = 1"
    ))
    .bind(uid)
    .fetch_one(pool)
    .await?;
    Ok(count > 0)
}

pub async fn update_password(pool: &MySqlPool, uid: i64, password_hash: &str) -> anyhow::Result<()> {
    sqlx::query("UPDATE sys_user SET password = ?, update_time = NOW() WHERE id = ?")
        .bind(password_hash)
        .bind(uid)
        .execute(pool)
        .await?;
    Ok(())
}

/// 离职注销：禁用/启用用户，返回是否命中
pub async fn update_status(pool: &MySqlPool, username: &str, status: i8) -> anyhow::Result<bool> {
    let result = sqlx::query("UPDATE sys_user SET status = ?, update_time = NOW() WHERE username = ?")
        .bind(status)
        .bind(username)
        .execute(pool)
        .await?;
    Ok(result.rows_affected() > 0)
}
