package com.platform.api.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 将 SQL 脚本中的 NEED_INIT 占位密码替换为 BCrypt 加密的默认密码。
 * 这样仓库中不需要出现任何明文口令的哈希值，重复启动也不会重复处理。
 */
@Component
public class PasswordInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PasswordInitializer.class);
    private static final String PLACEHOLDER = "NEED_INIT";

    private final NamedParameterJdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties properties;

    public PasswordInitializer(NamedParameterJdbcTemplate jdbc,
                               PasswordEncoder passwordEncoder,
                               AppProperties properties) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        JdbcTemplate template = jdbc.getJdbcTemplate();
        List<Long> userIds = template.queryForList(
                "SELECT id FROM sys_user WHERE password = ?", Long.class, PLACEHOLDER);
        if (userIds.isEmpty()) {
            return;
        }
        String encoded = passwordEncoder.encode(properties.getDefaultPassword());
        for (Long userId : userIds) {
            jdbc.update("UPDATE sys_user SET password = :password WHERE id = :id",
                    Map.of("password", encoded, "id", userId));
        }
        log.info("已初始化 {} 个账号的默认密码", userIds.size());
    }
}
