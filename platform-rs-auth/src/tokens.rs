use crate::keys::SigningKeys;
use base64::Engine;
use jsonwebtoken::{Algorithm, EncodingKey, Header, Validation};
use serde::{Deserialize, Serialize};
use sha2::{Digest, Sha256};
use std::time::{SystemTime, UNIX_EPOCH};

/// 访问令牌有效期 30 分钟（与原 platform-api 约定一致）
pub const ACCESS_TTL: u64 = 1800;
/// ID 令牌有效期 30 分钟
pub const ID_TTL: u64 = 1800;

fn now_secs() -> u64 {
    SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .expect("系统时间早于 1970")
        .as_secs()
}

#[allow(dead_code)] // 令牌声明按 OIDC 规范完整签发，部分字段仅用于对外输出
#[derive(Serialize)]
struct AccessClaims {
    iss: String,
    sub: String,
    aud: String,
    /// 用户 ID（字符串），资源服务器据此加载权限
    uid: String,
    nickname: Option<String>,
    typ: &'static str,
    iat: u64,
    exp: u64,
    jti: String,
}

#[allow(dead_code)]
#[derive(Serialize)]
struct IdClaims {
    iss: String,
    sub: String,
    aud: String,
    nonce: Option<String>,
    preferred_username: String,
    nickname: Option<String>,
    iat: u64,
    exp: u64,
}

#[allow(dead_code)] // sub 保留给调用方按需使用
#[derive(Deserialize)]
pub struct AccessClaimsOut {
    pub sub: String,
    pub uid: String,
}

pub struct TokenFactory {
    kid: String,
    encoding: EncodingKey,
    decoding: jsonwebtoken::DecodingKey,
    issuer: String,
    n_b64: String,
    e_b64: String,
}

impl TokenFactory {
    pub fn new(keys: &SigningKeys, issuer: String) -> Self {
        Self {
            kid: keys.kid.clone(),
            encoding: keys.encoding_key.clone(),
            decoding: keys.decoding_key.clone(),
            issuer,
            n_b64: keys.n_b64.clone(),
            e_b64: keys.e_b64.clone(),
        }
    }

    fn sign<T: Serialize>(&self, claims: &T) -> anyhow::Result<String> {
        let mut header = Header::new(Algorithm::RS256);
        header.kid = Some(self.kid.clone());
        Ok(jsonwebtoken::encode(&header, claims, &self.encoding)?)
    }

    /// 签发访问令牌，返回 (token, expires_in)
    pub fn issue_access(
        &self,
        username: &str,
        uid: i64,
        nickname: Option<&str>,
        client_id: &str,
    ) -> anyhow::Result<(String, u64)> {
        let now = now_secs();
        let claims = AccessClaims {
            iss: self.issuer.clone(),
            sub: username.to_string(),
            aud: client_id.to_string(),
            uid: uid.to_string(),
            nickname: nickname.map(|s| s.to_string()),
            typ: "access",
            iat: now,
            exp: now + ACCESS_TTL,
            jti: uuid::Uuid::new_v4().to_string(),
        };
        Ok((self.sign(&claims)?, ACCESS_TTL))
    }

    pub fn issue_id(
        &self,
        username: &str,
        nickname: Option<&str>,
        client_id: &str,
        nonce: Option<&str>,
    ) -> anyhow::Result<String> {
        let now = now_secs();
        let claims = IdClaims {
            iss: self.issuer.clone(),
            sub: username.to_string(),
            aud: client_id.to_string(),
            nonce: nonce.map(|s| s.to_string()),
            preferred_username: username.to_string(),
            nickname: nickname.map(|s| s.to_string()),
            iat: now,
            exp: now + ID_TTL,
        };
        self.sign(&claims)
    }

    /// 校验并解析访问令牌（本服务自签发，仅校验签名/过期/签发者）
    pub fn decode_access(&self, token: &str) -> anyhow::Result<AccessClaimsOut> {
        let mut validation = Validation::new(Algorithm::RS256);
        validation.validate_aud = false;
        validation.set_issuer(&[&self.issuer]);
        let data =
            jsonwebtoken::decode::<AccessClaimsOut>(token, &self.decoding, &validation)?;
        Ok(data.claims)
    }

    pub fn jwks(&self) -> serde_json::Value {
        serde_json::json!({
            "keys": [{
                "kty": "RSA",
                "use": "sig",
                "alg": "RS256",
                "kid": self.kid,
                "n": self.n_b64,
                "e": self.e_b64,
            }]
        })
    }
}

/// PKCE S256：BASE64URL(SHA256(verifier))
pub fn s256_base64url(verifier: &str) -> String {
    let digest = Sha256::digest(verifier.as_bytes());
    base64::engine::general_purpose::URL_SAFE_NO_PAD.encode(digest)
}
