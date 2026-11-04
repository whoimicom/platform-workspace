package com.platform.api.auth;

import com.platform.api.common.BusinessException;
import com.platform.api.config.OAuthProperties;
import com.platform.api.security.JwtService;
import com.platform.api.system.user.SysUser;
import com.platform.api.system.user.SysUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

/**
 * 三方登录（微信开放平台 / QQ 互联）流程。
 * <p>
 * 绑定规则：
 * <ol>
 *   <li>前端扫码/授权 → 三方回调带 code → 调 /oauth/login(provider, code)</li>
 *   <li>code 换 openId → 查 sys_user_identity(provider, openId)</li>
 *   <li>已绑定 → 直接签发 token；未绑定 → 返回 needBind + bindToken</li>
 *   <li>前端切换到绑定模式要手机号+短信验证码 → /oauth/bind → 写 identity → 签发 token</li>
 *   <li>绑定时手机号不存在 → 提示"用户不存在，请联系管理员"，不自动创建</li>
 *   <li>绑定时手机号已被其他 openId 绑定 → 替换为当前 openId</li>
 * </ol>
 * <p>
 * 真实调用三方 openId 接口：
 * <ul>
 *   <li>微信开放平台：GET https://api.weixin.qq.com/sns/oauth2/access_token?appid=xxx&secret=xxx&code=xxx&grant_type=authorization_code → openid / unionid</li>
 *   <li>QQ 互联：GET https://graph.qq.com/oauth2.0/token → access_token；
 *       GET https://graph.qq.com/oauth2.0/me → openid；
 *       GET https://graph.qq.com/user/get_user_info → 昵称 / 头像</li>
 * </ul>
 * 未配置 appId/appSecret 时 exchangeOpenId 自动退化为 mock（以 code 作为 openId）。
 */
@Service
public class OAuthService {

    private static final Logger log = LoggerFactory.getLogger(OAuthService.class);

    private static final String BIND_SECRET = "oauth-bind-token-secret-must-be-at-least-32-bytes!!";
    private static final long BIND_TTL_SECONDS = 600L; // 10 分钟有效

    private final SysUserIdentityRepository identityRepository;
    private final SysUserRepository userRepository;
    private final JwtService jwtService;
    private final SmsCodeService smsCodeService;
    private final OAuthProperties oauthProperties;
    private final RestClient restClient;
    private final JsonMapper jsonMapper;

    public OAuthService(SysUserIdentityRepository identityRepository,
                        SysUserRepository userRepository,
                        JwtService jwtService,
                        SmsCodeService smsCodeService,
                        OAuthProperties oauthProperties,
                        JsonMapper jsonMapper) {
        this.identityRepository = identityRepository;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.smsCodeService = smsCodeService;
        this.oauthProperties = oauthProperties;
        this.jsonMapper = jsonMapper;
        this.restClient = RestClient.builder().build();
    }

    // ---- 三方登录入口 ----

    /**
     * 三方 code 换登录态。
     *
     * @param provider WECHAT / QQ
     * @param appId    应用 appId（可空，留空则从配置文件读取）
     * @param code     前端拿到的授权 code
     */
    public AuthDtos.OauthLoginResult oauthLogin(String provider, String appId, String code) {
        if (code == null || code.isBlank()) {
            throw BusinessException.badRequest("授权 code 缺失");
        }
        if (!isSupportedProvider(provider)) {
            throw BusinessException.badRequest("暂不支持的登录方式：" + provider);
        }

        // 1) code → openId / unionId
        AuthDtos.OauthIdentity identity = exchangeOpenId(provider, appId, code);

        // 2) 查绑定：openId 唯一键命中即直接登录
        Optional<SysUserIdentity> bound = identityRepository
                .findByProviderAndOpenId(provider, identity.openId());

        if (bound.isPresent()) {
            SysUser sysUser = userRepository.findById(bound.get().getUserId())
                    .orElseThrow(() -> BusinessException.badRequest("绑定的用户不存在，请联系管理员"));

            bound.get().setLastLoginTime(LocalDateTime.now());
            identityRepository.save(bound.get());

            return AuthDtos.OauthLoginResult.loggedIn(buildLoginResult(sysUser));
        }

        // 3) 未绑定 → 返回绑定流程 token
        String bindToken = buildBindToken(provider, identity);
        return AuthDtos.OauthLoginResult.needBind(bindToken);
    }

    /**
     * 手机号验证码绑定 + 登录。
     */
    @Transactional
    public AuthDtos.LoginResult bindAndLogin(String bindToken, String mobile, String smsCode) {
        AuthDtos.OauthIdentity identity = parseBindToken(bindToken);

        // 短信验证码校验（一次性消费）
        smsCodeService.verify(mobile, smsCode);

        // 手机号必须已存在于 sys_user
        SysUser sysUser = userRepository.findByMobile(mobile)
                .orElseThrow(() -> BusinessException.badRequest("该手机号未关联系统用户，请联系管理员"));

        // 处理 identity 绑定：同一 provider + openId 应该唯一，若存在且 userId 不同则替换
        identityRepository.findByProviderAndOpenId(identity.provider(), identity.openId())
                .ifPresent(existing -> {
                    if (!existing.getUserId().equals(sysUser.getId())) {
                        log.info("openId {} 原绑定用户 {} 被替换为 {}",
                                identity.openId(), existing.getUserId(), sysUser.getId());
                        identityRepository.delete(existing);
                    }
                });

        SysUserIdentity entity = new SysUserIdentity();
        entity.setUserId(sysUser.getId());
        entity.setProvider(identity.provider());
        entity.setAppId(identity.appId());
        entity.setUnionId(identity.unionId());
        entity.setOpenId(identity.openId());
        entity.setNickname(identity.nickname());
        entity.setAvatar(identity.avatar());
        entity.setLastLoginTime(LocalDateTime.now());
        identityRepository.save(entity);

        return buildLoginResult(sysUser);
    }

    /**
     * 手机号验证码直接登录（sys_user.mobile 存在即可）。
     */
    public AuthDtos.LoginResult smsDirectLogin(String mobile, String smsCode) {
        smsCodeService.verify(mobile, smsCode);
        SysUser sysUser = userRepository.findByMobile(mobile)
                .orElseThrow(() -> BusinessException.badRequest("该手机号未关联系统用户，请联系管理员"));
        return buildLoginResult(sysUser);
    }

    private AuthDtos.LoginResult buildLoginResult(SysUser user) {
        String accessToken = jwtService.createAccessToken(user.getId(), user.getUsername());
        String refreshToken = jwtService.createRefreshToken(user.getId(), user.getUsername());
        return new AuthDtos.LoginResult(accessToken, refreshToken, "Bearer", jwtService.getAccessExpireSeconds());
    }

    private static boolean isSupportedProvider(String provider) {
        return "WECHAT".equalsIgnoreCase(provider) || "QQ".equalsIgnoreCase(provider);
    }

    // ---- 授权页地址（前端跳转扫码 / 授权）----

    /**
     * 前端内嵌二维码所需的公开配置（不含任何 secret）。
     */
    public AuthDtos.OauthClientConfig clientConfig() {
        OAuthProperties.WeChat wechat = oauthProperties.getWechat();
        OAuthProperties.Qq qq = oauthProperties.getQq();
        return new AuthDtos.OauthClientConfig(
                new AuthDtos.OauthClientConfig.ProviderConfig(
                        wechat.getAppId(), wechat.getRedirectUri(),
                        wechat.isEnabled() && !isBlank(wechat.getRedirectUri())),
                new AuthDtos.OauthClientConfig.ProviderConfig(
                        qq.getAppId(), qq.getRedirectUri(),
                        qq.isEnabled() && !isBlank(qq.getRedirectUri()))
        );
    }

    /**
     * 构建三方授权页 URL。redirect_uri 直接取配置值，必须与微信 / QQ 互联后台登记的地址完全一致。
     */
    public AuthDtos.OauthAuthorizeUrl buildAuthorizeUrl(String provider) {
        String state = UUID.randomUUID().toString().replace("-", "");
        if ("WECHAT".equalsIgnoreCase(provider)) {
            OAuthProperties.WeChat cfg = oauthProperties.getWechat();
            if (!cfg.isEnabled() || isBlank(cfg.getRedirectUri())) {
                throw BusinessException.badRequest(
                        "微信登录未配置：请设置环境变量 WECHAT_OPEN_APPID / WECHAT_OPEN_APP_SECRET / WECHAT_OPEN_REDIRECT_URI");
            }
            validateRedirectUri(cfg.getRedirectUri());
            String url = "https://open.weixin.qq.com/connect/qrconnect"
                    + "?appid=" + cfg.getAppId()
                    + "&redirect_uri=" + URLEncoder.encode(cfg.getRedirectUri(), StandardCharsets.UTF_8)
                    + "&response_type=code&scope=snsapi_login"
                    + "&state=" + state
                    + "#wechat_redirect";
            log.info("微信扫码授权 URL: {}", url);
            return new AuthDtos.OauthAuthorizeUrl(url, state);
        }
        if ("QQ".equalsIgnoreCase(provider)) {
            OAuthProperties.Qq cfg = oauthProperties.getQq();
            if (!cfg.isEnabled() || isBlank(cfg.getRedirectUri())) {
                throw BusinessException.badRequest(
                        "QQ 登录未配置：请设置环境变量 QQ_APP_ID / QQ_APP_SECRET / QQ_REDIRECT_URI");
            }
            validateRedirectUri(cfg.getRedirectUri());
            String url = "https://graph.qq.com/oauth2.0/authorize"
                    + "?response_type=code"
                    + "&client_id=" + cfg.getAppId()
                    + "&redirect_uri=" + URLEncoder.encode(cfg.getRedirectUri(), StandardCharsets.UTF_8)
                    + "&state=" + state
                    + "&scope=get_user_info";
            log.info("QQ 授权 URL: {}", url);
            return new AuthDtos.OauthAuthorizeUrl(url, state);
        }
        throw BusinessException.badRequest("暂不支持的登录方式：" + provider);
    }

    /**
     * 校验 redirect_uri 合法性，提前给出可读提示（三方平台只会返回"redirect_uri 参数错误"）。
     */
    private void validateRedirectUri(String uri) {
        if (uri.contains("#")) {
            throw BusinessException.badRequest(
                    "redirect_uri 不能包含 #（前端为 hash 路由，三方平台会报\"redirect_uri 参数错误\"）。"
                            + "请改为静态中转页，例如 https://你的域名/platform-web/oauth-callback.html");
        }
        if (uri.contains("localhost") || uri.contains("127.0.0.1")) {
            throw BusinessException.badRequest(
                    "redirect_uri 不能使用 localhost/127.0.0.1，三方平台要求使用已备案域名（80/443 端口）");
        }
    }

    // ---- code → openId（真实调用微信 / QQ 接口，未配置则退化为 mock）----

    protected AuthDtos.OauthIdentity exchangeOpenId(String provider, String appId, String code) {
        if ("WECHAT".equalsIgnoreCase(provider)) {
            return exchangeWechatOpenId(appId, code);
        }
        if ("QQ".equalsIgnoreCase(provider)) {
            return exchangeQqOpenId(code);
        }
        throw BusinessException.badRequest("暂不支持的登录方式：" + provider);
    }

    private AuthDtos.OauthIdentity exchangeWechatOpenId(String appIdFromReq, String code) {
        OAuthProperties.WeChat cfg = oauthProperties.getWechat();
        if (!cfg.isEnabled()) {
            log.warn("[MOCK] 微信开放平台未配置 WECHAT_OPEN_APPID / WECHAT_OPEN_APP_SECRET，" +
                    "exchangeOpenId 退化为 mock。请在环境变量中设置后重启。");
            return mockIdentity("WECHAT", code);
        }

        // appId 允许请求参数覆盖（优先用请求方传的）
        String appId = (appIdFromReq != null && !appIdFromReq.isBlank()) ? appIdFromReq : cfg.getAppId();
        String url = "https://api.weixin.qq.com/sns/oauth2/access_token"
                + "?appid=" + appId
                + "&secret=" + cfg.getAppSecret()
                + "&code=" + code
                + "&grant_type=authorization_code";

        try {
            String body = restClient.get().uri(url).retrieve().body(String.class);
            JsonNode json = jsonMapper.readTree(body);

            // 微信返回错误码 errcode，例如 40029 code 无效、40125 appsecret 错误
            if (json.has("errcode")) {
                int err = json.path("errcode").asInt();
                String errmsg = json.path("errmsg").asText("");
                log.warn("微信换取 openId 失败: errcode={}, errmsg={}", err, errmsg);
                throw BusinessException.badRequest("授权 code 无效或已过期，请重新扫码");
            }

            String openId = json.path("openid").asText(null);
            String unionId = json.path("unionid").asText(null);
            if (openId == null || openId.isBlank()) {
                log.warn("微信返回缺少 openid 字段: {}", body);
                throw BusinessException.badRequest("微信返回数据异常，请稍后重试");
            }

            log.info("微信 code → openId={}, unionId={}", openId, unionId);
            return new AuthDtos.OauthIdentity("WECHAT", appId, unionId, openId, null, null);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("微信换取 openId 异常", ex);
            throw BusinessException.badRequest("微信授权服务异常，请稍后重试");
        }
    }

    private AuthDtos.OauthIdentity exchangeQqOpenId(String code) {
        OAuthProperties.Qq cfg = oauthProperties.getQq();
        if (!cfg.isEnabled()) {
            log.warn("[MOCK] QQ 互联未配置 QQ_APP_ID / QQ_APP_SECRET，" +
                    "exchangeOpenId 退化为 mock。请在环境变量中设置后重启。");
            return mockIdentity("QQ", code);
        }

        try {
            // 1) code → access_token（fmt=json 让 QQ 返回 JSON 而不是 a=b&c=d 文本）
            String tokenUrl = "https://graph.qq.com/oauth2.0/token"
                    + "?grant_type=authorization_code"
                    + "&client_id=" + cfg.getAppId()
                    + "&client_secret=" + cfg.getAppSecret()
                    + "&code=" + code
                    + "&redirect_uri=" + URLEncoder.encode(cfg.getRedirectUri(), StandardCharsets.UTF_8)
                    + "&fmt=json";
            JsonNode tokenJson = jsonMapper.readTree(
                    restClient.get().uri(tokenUrl).retrieve().body(String.class));
            if (tokenJson.has("error")) {
                log.warn("QQ 换取 access_token 失败: {}", tokenJson);
                throw BusinessException.badRequest("授权 code 无效或已过期，请重新授权");
            }
            String accessToken = tokenJson.path("access_token").asText(null);
            if (isBlank(accessToken)) {
                log.warn("QQ 返回缺少 access_token: {}", tokenJson);
                throw BusinessException.badRequest("QQ 返回数据异常，请稍后重试");
            }

            // 2) access_token → openid
            String meUrl = "https://graph.qq.com/oauth2.0/me"
                    + "?access_token=" + accessToken
                    + "&fmt=json";
            JsonNode meJson = jsonMapper.readTree(
                    restClient.get().uri(meUrl).retrieve().body(String.class));
            if (meJson.has("error")) {
                log.warn("QQ 获取 openid 失败: {}", meJson);
                throw BusinessException.badRequest("授权 code 无效或已过期，请重新授权");
            }
            String openId = meJson.path("openid").asText(null);
            if (isBlank(openId)) {
                log.warn("QQ 返回缺少 openid: {}", meJson);
                throw BusinessException.badRequest("QQ 返回数据异常，请稍后重试");
            }

            // 3) 昵称/头像（可选信息，失败不影响登录）
            String nickname = null;
            String avatar = null;
            try {
                String userUrl = "https://graph.qq.com/user/get_user_info"
                        + "?access_token=" + accessToken
                        + "&oauth_consumer_key=" + cfg.getAppId()
                        + "&openid=" + openId
                        + "&fmt=json";
                JsonNode userJson = jsonMapper.readTree(
                        restClient.get().uri(userUrl).retrieve().body(String.class));
                if (userJson.path("ret").asInt(-1) == 0) {
                    nickname = userJson.path("nickname").asText(null);
                    avatar = userJson.path("figureurl_qq_2").asText(null);
                } else {
                    log.warn("QQ 获取用户信息失败（不影响登录）: {}", userJson);
                }
            } catch (Exception ex) {
                log.warn("QQ 获取用户信息异常（不影响登录）: {}", ex.getMessage());
            }

            log.info("QQ code → openId={}, nickname={}", openId, nickname);
            return new AuthDtos.OauthIdentity("QQ", cfg.getAppId(), null, openId, nickname, avatar);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("QQ 换取 openId 异常", ex);
            throw BusinessException.badRequest("QQ 授权服务异常，请稍后重试");
        }
    }

    private AuthDtos.OauthIdentity mockIdentity(String provider, String code) {
        String hash = Integer.toHexString(code.hashCode());
        return new AuthDtos.OauthIdentity(
                provider, null,
                "union_" + hash,
                "open_" + provider.toLowerCase() + "_" + code,
                null, null);
    }

    // ---- bindToken 签名（URL-safe Base64(payload|hmac)，payload 以 | 分隔 7 段）----

    private String buildBindToken(String provider, AuthDtos.OauthIdentity identity) {
        long ts = System.currentTimeMillis() / 1000L;
        String payload = String.join("|",
                String.valueOf(ts),
                provider,
                nonBlank(identity.openId()),
                nonBlank(identity.appId()),
                nonBlank(identity.unionId()),
                nonBlank(identity.nickname()),
                nonBlank(identity.avatar()));
        String sig = hmacSha256Hex(payload, BIND_SECRET);
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString((payload + "." + sig).getBytes(StandardCharsets.UTF_8));
    }

    private AuthDtos.OauthIdentity parseBindToken(String token) {
        if (token == null) {
            throw BusinessException.badRequest("绑定凭证缺失");
        }
        String signed;
        try {
            signed = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            throw BusinessException.badRequest("绑定凭证格式非法");
        }
        int dot = signed.lastIndexOf('.');
        if (dot < 0) {
            throw BusinessException.badRequest("绑定凭证格式非法");
        }
        String payload = signed.substring(0, dot);
        String sig = signed.substring(dot + 1);

        String expectedSig = hmacSha256Hex(payload, BIND_SECRET);
        if (!expectedSig.equals(sig)) {
            throw BusinessException.badRequest("绑定凭证已失效");
        }

        String[] parts = payload.split("\\|", 7);
        if (parts.length != 7) {
            throw BusinessException.badRequest("绑定凭证格式非法");
        }
        long ts;
        try {
            ts = Long.parseLong(parts[0]);
        } catch (NumberFormatException ex) {
            throw BusinessException.badRequest("绑定凭证格式非法");
        }
        long now = System.currentTimeMillis() / 1000L;
        if (now - ts > BIND_TTL_SECONDS) {
            throw BusinessException.badRequest("绑定凭证已过期，请重新扫码/点击登录");
        }

        return new AuthDtos.OauthIdentity(
                parts[1],
                blankToNull(parts[3]),
                blankToNull(parts[4]),
                parts[2],
                blankToNull(parts[5]),
                blankToNull(parts[6]));
    }

    // ---- HMAC-SHA256 ----

    private static String hmacSha256Hex(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new IllegalStateException("HMAC-SHA256 不可用", ex);
        }
    }

    private static String nonBlank(String s) {
        return s == null ? "" : s;
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
