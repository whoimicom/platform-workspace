-- 通知中心升级脚本（已有数据库执行一次；全新初始化直接使用 platform_api.sql）
-- 用法：SOURCE /path/to/migration_notice.sql;

CREATE TABLE IF NOT EXISTS sys_notice (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '通知ID',
    title           VARCHAR(200) NOT NULL COMMENT '通知标题',
    content         TEXT         NOT NULL COMMENT '通知内容(富文本)',
    type            INT          NOT NULL DEFAULT 1 COMMENT '通知类型',
    level           VARCHAR(16)  NOT NULL DEFAULT 'L' COMMENT '通知等级',
    publish_status  TINYINT      NOT NULL DEFAULT 0 COMMENT '发布状态(0:草稿;1:已发布;-1:已撤回)',
    target_type     TINYINT      NOT NULL DEFAULT 1 COMMENT '目标类型(1:全体;2:指定用户)',
    target_user_ids VARCHAR(1024) DEFAULT NULL COMMENT '指定用户ID，英文逗号分隔',
    publisher_id    BIGINT       DEFAULT NULL COMMENT '发布人ID',
    publish_time    DATETIME     DEFAULT NULL COMMENT '发布时间',
    revoke_time     DATETIME     DEFAULT NULL COMMENT '撤回时间',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_sys_notice_status (publish_status),
    KEY idx_sys_notice_publish_time (publish_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '通知公告表';

CREATE TABLE IF NOT EXISTS sys_notice_read (
    notice_id BIGINT   NOT NULL COMMENT '通知ID',
    user_id   BIGINT   NOT NULL COMMENT '用户ID',
    read_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '阅读时间',
    PRIMARY KEY (notice_id, user_id),
    KEY idx_sys_notice_read_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '通知阅读记录表';

-- 菜单与按钮权限
INSERT IGNORE INTO sys_menu (id, parent_id, name, type, path, route_name, redirect, component, external_url, icon, sort, visible, scope, keep_alive, perm, create_time, update_time) VALUES
    (106,   1,   '通知公告', 'M', 'notice', 'Notice', NULL, 'system/notice/index', NULL, 'document', 6, 1, 1, 1, NULL, NOW(), NOW()),
    (10601, 106, '通知查询', 'B', NULL, NULL, NULL, NULL, NULL, NULL, 1, 1, 1, NULL, 'sys:notice:list', NOW(), NOW()),
    (10602, 106, '通知新增', 'B', NULL, NULL, NULL, NULL, NULL, NULL, 2, 1, 1, NULL, 'sys:notice:create', NOW(), NOW()),
    (10603, 106, '通知编辑', 'B', NULL, NULL, NULL, NULL, NULL, NULL, 3, 1, 1, NULL, 'sys:notice:update', NOW(), NOW()),
    (10604, 106, '通知删除', 'B', NULL, NULL, NULL, NULL, NULL, NULL, 4, 1, 1, NULL, 'sys:notice:delete', NOW(), NOW()),
    (10605, 106, '通知发布', 'B', NULL, NULL, NULL, NULL, NULL, NULL, 5, 1, 1, NULL, 'sys:notice:publish', NOW(), NOW()),
    (10606, 106, '通知撤回', 'B', NULL, NULL, NULL, NULL, NULL, NULL, 6, 1, 1, NULL, 'sys:notice:revoke', NOW(), NOW());

-- 超级管理员 / 系统管理员：全部通知权限
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r CROSS JOIN sys_menu m
WHERE r.id IN (1, 2) AND m.id IN (106, 10601, 10602, 10603, 10604, 10605, 10606);

-- 其余演示角色：可进入通知页面并查询
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r CROSS JOIN sys_menu m
WHERE r.id BETWEEN 3 AND 10 AND m.id IN (106, 10601);

-- 字典：通知类型 / 通知等级
INSERT IGNORE INTO sys_dict (dict_code, name, status, remark, create_time, update_time) VALUES
    ('notice_type', '通知类型', 1, '通知公告类型', NOW(), NOW()),
    ('notice_level', '通知等级', 1, '通知公告等级', NOW(), NOW());

INSERT IGNORE INTO sys_dict_item (dict_code, label, value, sort, status, tag_type, create_time, update_time) VALUES
    ('notice_type', '通知', '1', 1, 1, 'primary', NOW(), NOW()),
    ('notice_type', '公告', '2', 2, 1, 'success', NOW(), NOW()),
    ('notice_level', '低', 'L', 1, 1, 'info', NOW(), NOW()),
    ('notice_level', '中', 'M', 2, 1, 'warning', NOW(), NOW()),
    ('notice_level', '高', 'H', 3, 1, 'danger', NOW(), NOW());
