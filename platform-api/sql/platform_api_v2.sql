-- ============================================================
-- 2026-11-03 新增：三方身份绑定 / 短信验证码 表
-- 执行方式：
--   mysql -uroot -p platform < platform_api_v2.sql
-- 或在数据库中 SOURCE 本文件
-- ============================================================
USE platform;

-- ------------------------------------------------------------
-- 三方身份绑定（不写入 sys_user）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_user_identity (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id         BIGINT       NOT NULL COMMENT '绑定的系统用户ID',
    provider        VARCHAR(32)  NOT NULL COMMENT '提供者: WECHAT / QQ',
    app_id          VARCHAR(128) DEFAULT NULL COMMENT '应用 appid',
    union_id        VARCHAR(128) DEFAULT NULL COMMENT '同一开发者主体下跨应用唯一标识',
    open_id         VARCHAR(128) NOT NULL COMMENT '应用内唯一标识',
    nickname        VARCHAR(64)  DEFAULT NULL COMMENT '三方昵称(冗余快照)',
    avatar          VARCHAR(512) DEFAULT NULL COMMENT '三方头像(冗余快照)',
    last_login_time DATETIME     DEFAULT NULL COMMENT '最近登录时间',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_provider_openid (provider, open_id),
    KEY idx_user_id (user_id),
    KEY idx_union_id (union_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '三方身份绑定表';
