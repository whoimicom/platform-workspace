-- 代码生成模块（多表配置）：已有数据库执行一次；全新初始化由 platform_api.sql 覆盖
USE platform;

-- ------------------------------------------------------------
-- 旧版本表清理（按多表配置重构，不保留历史配置）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS sys_gen_config_column;
DROP TABLE IF EXISTS sys_gen_config_table;
DROP TABLE IF EXISTS sys_gen_config;
DROP TABLE IF EXISTS sys_gen_table_column;
DROP TABLE IF EXISTS sys_gen_table;
DROP TABLE IF EXISTS gen_table_column;
DROP TABLE IF EXISTS gen_table;
DROP TABLE IF EXISTS gen_type_mapping;

-- ------------------------------------------------------------
-- 代码生成配置表（系统级，统一 sys_ 前缀）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_gen_config (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    config_name VARCHAR(100) NOT NULL COMMENT '配置名称',
    main_table VARCHAR(100) NOT NULL COMMENT '主表名',
    function_name VARCHAR(100) NOT NULL COMMENT '功能名（页面标题）',
    module_name VARCHAR(64) NOT NULL DEFAULT 'rental' COMMENT '模块名（后端包名/前端目录一级）',
    business_name VARCHAR(64) NOT NULL COMMENT '业务名（camelCase）',
    class_name VARCHAR(100) NOT NULL COMMENT 'Java 类名',
    frontend_path VARCHAR(128) NOT NULL COMMENT '前端页面路径',
    perm_prefix VARCHAR(100) NOT NULL COMMENT '权限前缀',
    gen_style VARCHAR(32) NOT NULL DEFAULT 'dynamic-sql' COMMENT '生成风格 data-jdbc / dynamic-sql',
    package_name VARCHAR(128) NOT NULL DEFAULT 'com.platform.api' COMMENT '后端根包名',
    author VARCHAR(64) DEFAULT NULL COMMENT '作者',
    create_time DATETIME DEFAULT NULL,
    update_time DATETIME DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_main_table (main_table)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '代码生成配置表';

CREATE TABLE IF NOT EXISTS sys_gen_config_table (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    config_id BIGINT NOT NULL COMMENT 'sys_gen_config.id',
    table_name VARCHAR(100) NOT NULL COMMENT '表名',
    table_comment VARCHAR(255) DEFAULT NULL COMMENT '表注释',
    table_alias VARCHAR(64) NOT NULL COMMENT 'SQL 别名（t/t1...）',
    relation_type VARCHAR(16) NOT NULL COMMENT '关联方式 MAIN/ONE_TO_ONE/ONE_TO_MANY/MANY_TO_ONE',
    main_column VARCHAR(100) DEFAULT NULL COMMENT '主表参与关联的列',
    join_column VARCHAR(100) DEFAULT NULL COMMENT '关联表参与关联的列',
    sort INT NOT NULL DEFAULT 0 COMMENT '排序（决定 JOIN 顺序）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_config_table (config_id, table_name),
    KEY idx_config (config_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '代码生成关联表配置';

CREATE TABLE IF NOT EXISTS sys_gen_config_column (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    config_id BIGINT NOT NULL COMMENT 'sys_gen_config.id',
    table_name VARCHAR(100) NOT NULL COMMENT '所属表名',
    column_name VARCHAR(100) NOT NULL COMMENT '列名',
    column_type VARCHAR(64) NOT NULL COMMENT '列类型',
    column_comment VARCHAR(255) DEFAULT NULL COMMENT '列注释',
    display_name VARCHAR(64) DEFAULT NULL COMMENT '显示列名（为空时使用列注释）',
    property_name VARCHAR(100) NOT NULL COMMENT '实体属性名（camelCase）',
    java_type VARCHAR(64) NOT NULL DEFAULT 'String' COMMENT 'Java 类型',
    is_pk TINYINT NOT NULL DEFAULT 0 COMMENT '是否主键',
    is_required TINYINT NOT NULL DEFAULT 0 COMMENT '是否必填',
    is_list TINYINT NOT NULL DEFAULT 1 COMMENT '是否列表展示',
    is_filter TINYINT NOT NULL DEFAULT 0 COMMENT '是否查询条件',
    filter_type VARCHAR(16) NOT NULL DEFAULT 'EQ' COMMENT '条件类型 EQ/NE/GT/GE/LT/LE/LIKE/LIKE_LEFT/LIKE_RIGHT/IN/NOT_IN/BETWEEN',
    is_form TINYINT NOT NULL DEFAULT 1 COMMENT '是否表单字段',
    form_component VARCHAR(16) DEFAULT NULL COMMENT '表单元素 INPUT/TEXTAREA/NUMBER/DATE/DATETIME/SWITCH/SELECT',
    sort INT NOT NULL DEFAULT 0 COMMENT '排序',
    PRIMARY KEY (id),
    KEY idx_config (config_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '代码生成字段配置表';

-- ------------------------------------------------------------
-- 菜单（已存在则跳过）
-- ------------------------------------------------------------
INSERT INTO sys_menu (id, parent_id, name, type, path, route_name, redirect, component, external_url, icon, sort, visible, scope, keep_alive, perm, create_time, update_time)
SELECT 3, 0, '代码生成', 'C', '/gen', NULL, '/gen/config', 'Layout', NULL, 'code', 3, 1, 1, NULL, NULL, NOW(), NOW()
 WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 3);

INSERT INTO sys_menu (id, parent_id, name, type, path, route_name, redirect, component, external_url, icon, sort, visible, scope, keep_alive, perm, create_time, update_time)
SELECT 301, 3, '生成配置', 'M', 'config', 'GenConfig', NULL, 'gen/config/index', NULL, 'code', 1, 1, 1, 1, NULL, NOW(), NOW()
 WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 301);

INSERT INTO sys_menu (id, parent_id, name, type, path, route_name, redirect, component, external_url, icon, sort, visible, scope, keep_alive, perm, create_time, update_time)
SELECT 30101, 301, '配置查询', 'B', NULL, NULL, NULL, NULL, NULL, NULL, 1, 1, 1, NULL, 'gen:config:list', NOW(), NOW()
 WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 30101);

INSERT INTO sys_menu (id, parent_id, name, type, path, route_name, redirect, component, external_url, icon, sort, visible, scope, keep_alive, perm, create_time, update_time)
SELECT 30102, 301, '导入表', 'B', NULL, NULL, NULL, NULL, NULL, NULL, 2, 1, 1, NULL, 'gen:config:import', NOW(), NOW()
 WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 30102);

INSERT INTO sys_menu (id, parent_id, name, type, path, route_name, redirect, component, external_url, icon, sort, visible, scope, keep_alive, perm, create_time, update_time)
SELECT 30103, 301, '配置编辑', 'B', NULL, NULL, NULL, NULL, NULL, NULL, 3, 1, 1, NULL, 'gen:config:edit', NOW(), NOW()
 WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 30103);

INSERT INTO sys_menu (id, parent_id, name, type, path, route_name, redirect, component, external_url, icon, sort, visible, scope, keep_alive, perm, create_time, update_time)
SELECT 30104, 301, '配置删除', 'B', NULL, NULL, NULL, NULL, NULL, NULL, 4, 1, 1, NULL, 'gen:config:delete', NOW(), NOW()
 WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 30104);

INSERT INTO sys_menu (id, parent_id, name, type, path, route_name, redirect, component, external_url, icon, sort, visible, scope, keep_alive, perm, create_time, update_time)
SELECT 30105, 301, '代码预览', 'B', NULL, NULL, NULL, NULL, NULL, NULL, 5, 1, 1, NULL, 'gen:config:preview', NOW(), NOW()
 WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 30105);

INSERT INTO sys_menu (id, parent_id, name, type, path, route_name, redirect, component, external_url, icon, sort, visible, scope, keep_alive, perm, create_time, update_time)
SELECT 30106, 301, '生成代码', 'B', NULL, NULL, NULL, NULL, NULL, NULL, 6, 1, 1, NULL, 'gen:config:generate', NOW(), NOW()
 WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 30106);

-- 角色授权（已存在则跳过）
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r
  JOIN sys_menu m ON m.id IN (3, 301, 30101, 30102, 30103, 30104, 30105, 30106)
 WHERE r.id IN (1, 2);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r
  JOIN sys_menu m ON m.id IN (3, 301, 30101)
 WHERE r.id BETWEEN 3 AND 10;
