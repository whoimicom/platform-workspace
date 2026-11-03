use anyhow::Context;
use base64::Engine;
use rsa::pkcs8::{DecodePrivateKey, EncodePrivateKey, EncodePublicKey, LineEnding};
use rsa::traits::PublicKeyParts;
use sha2::{Digest, Sha256};

/// RS256 签名密钥与会话签名密钥，首次启动生成后持久化到 keys/ 目录
pub struct SigningKeys {
    pub kid: String,
    pub encoding_key: jsonwebtoken::EncodingKey,
    pub decoding_key: jsonwebtoken::DecodingKey,
    /// JWKS 所需的 RSA 模数与指数（base64url）
    pub n_b64: String,
    pub e_b64: String,
    /// 会话 ID 等内部用途的服务端密钥（十六进制）
    #[allow(dead_code)]
    pub server_secret: String,
}

pub fn load_or_create(dir: &std::path::Path) -> anyhow::Result<SigningKeys> {
    std::fs::create_dir_all(dir)?;
    let private_path = dir.join("signing-key.pem");
    let secret_path = dir.join("server.secret");

    let private_pem = if private_path.exists() {
        std::fs::read_to_string(&private_path).context("读取 signing-key.pem 失败")?
    } else {
        tracing::info!("未找到签名密钥，正在生成 RSA-2048 密钥对 ...");
        let mut rng = rand::rngs::OsRng;
        let private = rsa::RsaPrivateKey::new(&mut rng, 2048)?;
        let pem = private
            .to_pkcs8_pem(LineEnding::LF)
            .context("导出 PKCS#8 私钥失败")?
            .to_string();
        std::fs::write(&private_path, &pem)?;
        pem
    };

    let private =
        rsa::RsaPrivateKey::from_pkcs8_pem(&private_pem).context("解析 PKCS#8 私钥失败")?;
    let public = private.to_public_key();
    let public_pem = public
        .to_public_key_pem(LineEnding::LF)
        .context("导出公钥失败")?;

    // kid 取公钥指纹前 16 位，换钥后 JWKS 自动更新
    let digest = Sha256::digest(public_pem.as_bytes());
    let kid: String = digest.iter().map(|b| format!("{b:02x}")).take(16).collect();

    let n_b64 = base64::engine::general_purpose::URL_SAFE_NO_PAD
        .encode(public.n().to_bytes_be());
    let e_b64 = base64::engine::general_purpose::URL_SAFE_NO_PAD
        .encode(public.e().to_bytes_be());

    let server_secret = if secret_path.exists() {
        std::fs::read_to_string(&secret_path).context("读取 server.secret 失败")?
    } else {
        let mut bytes = [0u8; 32];
        use rand::RngCore;
        rand::rngs::OsRng.fill_bytes(&mut bytes);
        let hex: String = bytes.iter().map(|b| format!("{b:02x}")).collect();
        std::fs::write(&secret_path, &hex)?;
        hex
    };

    Ok(SigningKeys {
        kid,
        encoding_key: jsonwebtoken::EncodingKey::from_rsa_pem(private_pem.as_bytes())?,
        decoding_key: jsonwebtoken::DecodingKey::from_rsa_pem(public_pem.as_bytes())?,
        n_b64,
        e_b64,
        server_secret,
    })
}
