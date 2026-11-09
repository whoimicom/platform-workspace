-- 代码生成模块（已有数据库执行一次；全新初始化由 platform_api.sql 覆盖）
USE platform;

-- ------------------------------------------------------------
-- 旧版本迁移：gen_* → sys_gen_*，并移除数据库类型映射表（改为 Java 配置）
-- ------------------------------------------------------------
SET @has_old_gen := (SELECT COUNT(*) FROM information_schema.tables
                      WHERE table_schema = DATABASE() AND table_name = 'gen_table');
SET @rename_sql := IF(@has_old_gen = 1,
    'RENAME TABLE gen_table TO sys_gen_table, gen_table_column TO sys_gen_table_column',
    'SELECT 1');
PREPARE rename_stmt FROM @rename_sql;
EXECUTE rename_stmt;
DEALLOCATE PREPARE rename_stmt;
DROP TABLE IF EXISTS gen_type_mapping;

-- ------------------------------------------------------------
-- 代码生成配置表（系统级，统一 sys_ 前缀）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_gen_table (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    table_name VARCHAR(100) NOT NULL COMMENT '表名',
    table_comment VARCHAR(255) DEFAULT NULL COMMENT '表注释',
    module_name VARCHAR(64) NOT NULL DEFAULT 'rental' COMMENT '模块名（后端包名/前端目录一级）',
    business_name VARCHAR(64) NOT NULL COMMENT '业务名（camelCase）',
    function_name VARCHAR(100) NOT NULL COMMENT '功能名（页面标题）',
    class_name VARCHAR(100) NOT NULL COMMENT 'Java 类名',
    frontend_path VARCHAR(128) NOT NULL COMMENT '前端页面路径',
    perm_prefix VARCHAR(100) NOT NULL COMMENT '权限前缀',
    gen_style VARCHAR(32) NOT NULL DEFAULT 'dynamic-sql' COMMENT '生成风格 data-jdbc / dynamic-sql',
    package_name VARCHAR(128) NOT NULL DEFAULT 'com.platform.api' COMMENT '后端根包名',
    author VARCHAR(64) DEFAULT NULL COMMENT '作者',
    create_time DATETIME DEFAULT NULL,
    update_time DATETIME DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_table_name (table_name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '代码生成配置表';

CREATE TABLE IF NOT EXISTS sys_gen_table_column (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    table_id BIGINT NOT NULL COMMENT 'sys_gen_table.id',
    sort INT NOT NULL DEFAULT 0 COMMENT '排序',
    column_name VARCHAR(100) NOT NULL COMMENT '列名',
    column_type VARCHAR(64) NOT NULL COMMENT '列类型',
    column_comment VARCHAR(255) DEFAULT NULL COMMENT '列注释',
    property_name VARCHAR(100) NOT NULL COMMENT 'Java 属性名',
    java_type VARCHAR(64) NOT NULL DEFAULT 'String' COMMENT 'Java 类型',
    is_pk TINYINT NOT NULL DEFAULT 0 COMMENT '是否主键',
    is_required TINYINT NOT NULL DEFAULT 0 COMMENT '是否必填',
    is_list TINYINT NOT NULL DEFAULT 1 COMMENT '是否列表展示',
    is_filter TINYINT NOT NULL DEFAULT 0 COMMENT '是否查询条件',
    filter_type VARCHAR(16) NOT NULL DEFAULT 'EQ' COMMENT '条件类型 EQ/NE/GT/GE/LT/LE/LIKE/LIKE_LEFT/LIKE_RIGHT/IN/NOT_IN/BETWEEN',
    PRIMARY KEY (id),
    KEY idx_table_id (table_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '代码生成字段配置表';

-- ------------------------------------------------------------
-- 菜单与按钮权限
-- ------------------------------------------------------------
INSERT IGNORE INTO sys_menu (id, parent_id, name, type, path, route_name, redirect, component, external_url, icon, sort, visible, scope, keep_alive, perm, create_time, update_time) VALUES
    (3,     0,   '代码生成', 'C', '/gen', NULL, '/gen/config', 'Layout', NULL, 'code', 3, 1, 1, NULL, NULL, NOW(), NOW()),
    (301,   3,   '生成配置', 'M', 'config', 'GenConfig', NULL, 'gen/config/index', NULL, 'code', 1, 1, 1, 1, NULL, NOW(), NOW()),
    (30101, 301, '配置查询', 'B', NULL, NULL, NULL, NULL, NULL, NULL, 1, 1, 1, NULL, 'gen:config:list', NOW(), NOW()),
    (30102, 301, '导入表', 'B', NULL, NULL, NULL, NULL, NULL, NULL, 2, 1, 1, NULL, 'gen:config:import', NOW(), NOW()),
    (30103, 301, '配置编辑', 'B', NULL, NULL, NULL, NULL, NULL, NULL, 3, 1, 1, NULL, 'gen:config:edit', NOW(), NOW()),
    (30104, 301, '配置删除', 'B', NULL, NULL, NULL, NULL, NULL, NULL, 4, 1, 1, NULL, 'gen:config:delete', NOW(), NOW()),
    (30105, 301, '代码预览', 'B', NULL, NULL, NULL, NULL, NULL, NULL, 5, 1, 1, NULL, 'gen:config:preview', NOW(), NOW());

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r CROSS JOIN sys_menu m
WHERE r.id IN (1, 2) AND m.id IN (3, 301, 30101, 30102, 30103, 30104, 30105);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r CROSS JOIN sys_menu m
WHERE r.id BETWEEN 3 AND 10 AND m.id IN (3, 301, 30101);
