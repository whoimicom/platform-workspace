use std::collections::HashMap;
use std::sync::Mutex;
use std::time::{Duration, Instant};

/// 授权码有效期 10 分钟
pub const CODE_TTL: Duration = Duration::from_secs(600);
/// 认证中心 SSO 会话有效期 7 天
pub const SESSION_TTL: Duration = Duration::from_secs(7 * 24 * 3600);
/// 刷新令牌有效期 7 天
pub const REFRESH_TTL: Duration = Duration::from_secs(7 * 24 * 3600);

#[derive(Clone)]
pub struct AuthCodeRecord {
    pub client_id: String,
    pub redirect_uri: String,
    pub username: String,
    pub uid: i64,
    pub nickname: Option<String>,
    pub nonce: Option<String>,
    pub code_challenge: String,
    pub expires_at: Instant,
}

#[derive(Clone)]
pub struct SessionRecord {
    pub username: String,
    pub uid: i64,
    pub nickname: Option<String>,
    pub expires_at: Instant,
}

#[derive(Clone)]
pub struct RefreshRecord {
    pub username: String,
    pub uid: i64,
    pub client_id: String,
    pub expires_at: Instant,
}

/// 进程内存储：授权码 / SSO 会话 / 刷新令牌（重启即失效，开发环境够用）
pub struct Store {
    codes: Mutex<HashMap<String, AuthCodeRecord>>,
    sessions: Mutex<HashMap<String, SessionRecord>>,
    refresh_tokens: Mutex<HashMap<String, RefreshRecord>>,
}

impl Default for Store {
    fn default() -> Self {
        Self {
            codes: Mutex::new(HashMap::new()),
            sessions: Mutex::new(HashMap::new()),
            refresh_tokens: Mutex::new(HashMap::new()),
        }
    }
}

impl Store {
    fn purge<T>(map: &mut HashMap<String, T>, keep: impl Fn(&T) -> bool) {
        map.retain(|_, v| keep(v));
    }

    pub fn put_code(&self, code: String, rec: AuthCodeRecord) {
        let mut codes = self.codes.lock().unwrap();
        Self::purge(&mut codes, |c| c.expires_at > Instant::now());
        codes.insert(code, rec);
    }

    /// 授权码一次性使用：取出即删除
    pub fn take_code(&self, code: &str) -> Option<AuthCodeRecord> {
        let mut codes = self.codes.lock().unwrap();
        Self::purge(&mut codes, |c| c.expires_at > Instant::now());
        codes.remove(code)
    }

    pub fn create_session(
        &self,
        username: String,
        uid: i64,
        nickname: Option<String>,
    ) -> String {
        let sid = uuid::Uuid::new_v4().to_string();
        let rec = SessionRecord {
            username,
            uid,
            nickname,
            expires_at: Instant::now() + SESSION_TTL,
        };
        let mut sessions = self.sessions.lock().unwrap();
        Self::purge(&mut sessions, |s| s.expires_at > Instant::now());
        sessions.insert(sid.clone(), rec);
        sid
    }

    pub fn get_session(&self, sid: &str) -> Option<SessionRecord> {
        let mut sessions = self.sessions.lock().unwrap();
        Self::purge(&mut sessions, |s| s.expires_at > Instant::now());
        sessions.get(sid).cloned()
    }

    pub fn remove_session(&self, sid: &str) -> bool {
        self.sessions.lock().unwrap().remove(sid).is_some()
    }

    pub fn put_refresh(&self, token: String, rec: RefreshRecord) {
        let mut refresh_tokens = self.refresh_tokens.lock().unwrap();
        Self::purge(&mut refresh_tokens, |r| r.expires_at > Instant::now());
        refresh_tokens.insert(token, rec);
    }

    /// 刷新令牌轮换：取出即删除，换发时写入新令牌
    pub fn take_refresh(&self, token: &str) -> Option<RefreshRecord> {
        let mut refresh_tokens = self.refresh_tokens.lock().unwrap();
        Self::purge(&mut refresh_tokens, |r| r.expires_at > Instant::now());
        refresh_tokens.remove(token)
    }
}
