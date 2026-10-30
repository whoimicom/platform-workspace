-- ============================================================
-- platform-api 初始化脚本（MySQL 8）
-- 使用方式：
--   1. 创建一个空数据库（脚本内已包含 CREATE DATABASE IF NOT EXISTS admin）
--   2. 登录 MySQL 后执行：SOURCE /path/to/platform_api.sql;
--   3. 初始账号密码统一由后端首次启动时初始化（见 sys_user.password = NEED_INIT），
--      默认密码来自配置项 app.default-password（默认 123456）
-- ============================================================

CREATE DATABASE IF NOT EXISTS admin DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE admin;

-- ------------------------------------------------------------
-- 部门
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_dept (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '部门ID',
    parent_id   BIGINT       NOT NULL DEFAULT 0 COMMENT '父部门ID(0=顶级)',
    name        VARCHAR(64)  NOT NULL COMMENT '部门名称',
    code        VARCHAR(64)  DEFAULT NULL COMMENT '部门编码',
    sort        INT          NOT NULL DEFAULT 1 COMMENT '排序',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态(1:启用;0:禁用)',
    tree_path   VARCHAR(255) DEFAULT NULL COMMENT '父节点路径',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_sys_dept_parent (parent_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '部门表';

-- ------------------------------------------------------------
-- 用户
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    username    VARCHAR(64)  NOT NULL COMMENT '用户名',
    password    VARCHAR(255) NOT NULL COMMENT '密码(BCrypt)',
    nickname    VARCHAR(64)  DEFAULT NULL COMMENT '用户昵称',
    avatar      VARCHAR(512) DEFAULT NULL COMMENT '头像地址',
    gender      TINYINT      DEFAULT 0 COMMENT '性别(0:未知;1:男;2:女)',
    mobile      VARCHAR(32)  DEFAULT NULL COMMENT '手机号',
    email       VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态(1:启用;0:禁用)',
    dept_id     BIGINT       DEFAULT NULL COMMENT '部门ID',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_user_username (username),
    KEY idx_sys_user_dept (dept_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户表';

-- ------------------------------------------------------------
-- 角色
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_role (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '角色ID',
    code        VARCHAR(64)  NOT NULL COMMENT '角色编码',
    name        VARCHAR(64)  NOT NULL COMMENT '角色名称',
    sort        INT          NOT NULL DEFAULT 1 COMMENT '排序',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态(1:启用;0:禁用)',
    data_scope  TINYINT      NOT NULL DEFAULT 1 COMMENT '数据权限(1:所有;2:部门及子部门;3:本部门;4:本人;5:自定义部门)',
    remark      VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_role_code (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '角色表';

-- ------------------------------------------------------------
-- 菜单 / 权限
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_menu (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
    parent_id    BIGINT       NOT NULL DEFAULT 0 COMMENT '父菜单ID(0=顶级)',
    name         VARCHAR(64)  NOT NULL COMMENT '菜单名称',
    type         CHAR(1)      NOT NULL COMMENT '类型(C:目录;M:菜单;E:外链;B:按钮)',
    path         VARCHAR(255) DEFAULT NULL COMMENT '路由路径',
    route_name   VARCHAR(128) DEFAULT NULL COMMENT '路由名称',
    redirect     VARCHAR(255) DEFAULT NULL COMMENT '跳转路径',
    component    VARCHAR(255) DEFAULT NULL COMMENT '组件路径',
    external_url VARCHAR(512) DEFAULT NULL COMMENT '外链地址',
    icon         VARCHAR(64)  DEFAULT NULL COMMENT '图标',
    sort         INT          NOT NULL DEFAULT 1 COMMENT '排序',
    visible      TINYINT      NOT NULL DEFAULT 1 COMMENT '是否可见(1:显示;0:隐藏)',
    scope        TINYINT      NOT NULL DEFAULT 1 COMMENT '菜单范围(1:平台;2:业务)',
    keep_alive   TINYINT      DEFAULT NULL COMMENT '是否缓存(1:是;0:否)',
    params       VARCHAR(512) DEFAULT NULL COMMENT '路由参数(key=value 以 & 连接)',
    perm         VARCHAR(128) DEFAULT NULL COMMENT '按钮权限标识',
    create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_sys_menu_parent (parent_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '菜单权限表';

-- ------------------------------------------------------------
-- 关联表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_user_role (
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    PRIMARY KEY (user_id, role_id),
    KEY idx_sys_user_role_role (role_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户角色关联表';

CREATE TABLE IF NOT EXISTS sys_role_menu (
    role_id BIGINT NOT NULL COMMENT '角色ID',
    menu_id BIGINT NOT NULL COMMENT '菜单ID',
    PRIMARY KEY (role_id, menu_id),
    KEY idx_sys_role_menu_menu (menu_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '角色菜单关联表';

CREATE TABLE IF NOT EXISTS sys_role_dept (
    role_id BIGINT NOT NULL COMMENT '角色ID',
    dept_id BIGINT NOT NULL COMMENT '部门ID',
    PRIMARY KEY (role_id, dept_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '角色部门关联表(自定义数据权限)';

-- ------------------------------------------------------------
-- 字典类型 / 字典项
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_dict (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '字典ID',
    dict_code   VARCHAR(64)  NOT NULL COMMENT '字典编码',
    name        VARCHAR(64)  NOT NULL COMMENT '字典名称',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态(1:启用;0:禁用)',
    remark      VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_dict_code (dict_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '字典类型表';

CREATE TABLE IF NOT EXISTS sys_dict_item (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '字典项ID',
    dict_code   VARCHAR(64)  NOT NULL COMMENT '字典编码',
    label       VARCHAR(64)  NOT NULL COMMENT '字典标签',
    value       VARCHAR(64)  NOT NULL COMMENT '字典值',
    sort        INT          NOT NULL DEFAULT 1 COMMENT '排序',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态(1:启用;0:禁用)',
    tag_type    VARCHAR(32)  DEFAULT NULL COMMENT '标签类型(primary/success/warning/danger/info)',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_dict_item (dict_code, value)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '字典项表';

-- ============================================================
-- 初始化数据
-- ============================================================

-- 部门
INSERT INTO sys_dept (id, parent_id, name, code, sort, status, tree_path, create_time, update_time) VALUES
    (1, 0, '有来技术', 'YOULAI', 1, 1, '1', '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (2, 1, '研发部门', 'RD001', 1, 1, '1,2', '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (3, 1, '测试部门', 'QA001', 1, 1, '1,3', '2026-09-26 08:10:02', '2026-09-26 08:10:02');

-- 角色
INSERT INTO sys_role (id, code, name, sort, status, data_scope, remark, create_time, update_time) VALUES
    (1, 'ROOT', '超级管理员', 1, 1, 1, '超级管理员', '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (2, 'ADMIN', '系统管理员', 2, 1, 1, '系统管理员', '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (3, 'GUEST', '访问游客', 3, 1, 3, '访问游客', '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (4, 'DEPT_MANAGER', '部门主管', 4, 1, 2, '部门主管', '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (5, 'DEPT_MEMBER', '部门成员', 5, 1, 3, '部门成员', '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (6, 'EMPLOYEE', '普通员工', 6, 1, 4, '普通员工', '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (7, 'CUSTOM_USER', '自定义权限用户', 7, 1, 5, '自定义权限用户', '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (8, 'GENERAL_MANAGER', '总经理', 8, 1, 4, '总经理', '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (9, 'FINANCE', '财务', 9, 1, 4, '财务', '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (10, 'ADMINISTRATION', '行政', 10, 1, 4, '行政', '2026-09-26 08:10:02', '2026-09-26 08:10:02');

-- 用户（密码 NEED_INIT 会在后端首次启动时替换为 BCrypt 加密的默认密码）
INSERT INTO sys_user (id, username, password, nickname, avatar, gender, mobile, email, status, dept_id, create_time, update_time) VALUES
    (1, 'youlai', 'NEED_INIT', '有来技术', 'https://foruda.gitee.com/images/1723603502796844527/03cdca2a_716974.gif', 0, NULL, NULL, 1, NULL, '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (2, 'admin', 'NEED_INIT', '系统管理员', 'https://foruda.gitee.com/images/1723603502796844527/03cdca2a_716974.gif', 1, '18888888888', 'youlaitech@163.com', 1, 1, '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (3, 'test', 'NEED_INIT', '测试小用户', 'https://foruda.gitee.com/images/1723603502796844527/03cdca2a_716974.gif', 1, '18812345679', 'youlaitech@163.com', 1, 3, '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (4, 'dept_manager', 'NEED_INIT', '部门主管', 'https://foruda.gitee.com/images/1723603502796844527/03cdca2a_716974.gif', 1, '18812345680', 'manager@youlaitech.com', 1, 1, '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (5, 'dept_member', 'NEED_INIT', '部门成员', 'https://foruda.gitee.com/images/1723603502796844527/03cdca2a_716974.gif', 1, '18812345681', 'member@youlaitech.com', 1, 1, '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (6, 'employee', 'NEED_INIT', '普通员工', 'https://foruda.gitee.com/images/1723603502796844527/03cdca2a_716974.gif', 1, '18812345682', 'employee@youlaitech.com', 1, 2, '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (7, 'custom_user', 'NEED_INIT', '自定义权限用户', 'https://foruda.gitee.com/images/1723603502796844527/03cdca2a_716974.gif', 1, '18812345683', 'custom@youlaitech.com', 1, 3, '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (8, 'manager', 'NEED_INIT', '总经理', 'https://foruda.gitee.com/images/1723603502796844527/03cdca2a_716974.gif', 1, '18812345684', 'manager@youlaitech.com', 1, 1, '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (9, 'finance', 'NEED_INIT', '财务专员', 'https://foruda.gitee.com/images/1723603502796844527/03cdca2a_716974.gif', 2, '18812345685', 'finance@youlaitech.com', 1, 1, '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (10, 'clerk', 'NEED_INIT', '行政专员', 'https://foruda.gitee.com/images/1723603502796844527/03cdca2a_716974.gif', 2, '18812345686', 'clerk@youlaitech.com', 1, 1, '2026-09-26 08:10:02', '2026-09-26 08:10:02');

-- 用户角色关联
INSERT INTO sys_user_role (user_id, role_id) VALUES
    (1, 1),
    (2, 2),
    (3, 3),
    (4, 4),
    (5, 5),
    (6, 6),
    (7, 7),
    (8, 8),
    (9, 9),
    (10, 10);

-- 菜单与权限（按钮权限标识与前端 v-hasPerm 保持一致）
INSERT INTO sys_menu (id, parent_id, name, type, path, route_name, redirect, component, external_url, icon, sort, visible, scope, keep_alive, perm, create_time, update_time) VALUES
    (1,     0,   '系统管理', 'C', '/system', NULL, '/system/user', 'Layout', NULL, 'system', 1, 1, 1, NULL, NULL, '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (101,   1,   '用户管理', 'M', 'user', 'User', NULL, 'system/user/index', NULL, 'el-icon-User', 1, 1, 1, 1, NULL, '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (10101, 101, '用户查询', 'B', NULL, NULL, NULL, NULL, NULL, NULL, 1, 1, 1, NULL, 'sys:user:list', '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (10102, 101, '用户新增', 'B', NULL, NULL, NULL, NULL, NULL, NULL, 2, 1, 1, NULL, 'sys:user:create', '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (10103, 101, '用户编辑', 'B', NULL, NULL, NULL, NULL, NULL, NULL, 3, 1, 1, NULL, 'sys:user:update', '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (10104, 101, '用户删除', 'B', NULL, NULL, NULL, NULL, NULL, NULL, 4, 1, 1, NULL, 'sys:user:delete', '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (10105, 101, '重置密码', 'B', NULL, NULL, NULL, NULL, NULL, NULL, 5, 1, 1, NULL, 'sys:user:reset-password', '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (102,   1,   '角色管理', 'M', 'role', 'Role', NULL, 'system/role/index', NULL, 'role', 2, 1, 1, 1, NULL, '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (10201, 102, '角色查询', 'B', NULL, NULL, NULL, NULL, NULL, NULL, 1, 1, 1, NULL, 'sys:role:list', '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (10202, 102, '角色新增', 'B', NULL, NULL, NULL, NULL, NULL, NULL, 2, 1, 1, NULL, 'sys:role:create', '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (10203, 102, '角色编辑', 'B', NULL, NULL, NULL, NULL, NULL, NULL, 3, 1, 1, NULL, 'sys:role:update', '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (10204, 102, '角色删除', 'B', NULL, NULL, NULL, NULL, NULL, NULL, 4, 1, 1, NULL, 'sys:role:delete', '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (10205, 102, '角色分配权限', 'B', NULL, NULL, NULL, NULL, NULL, NULL, 5, 1, 1, NULL, 'sys:role:assign', '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (103,   1,   '菜单管理', 'M', 'menu', 'SysMenu', NULL, 'system/menu/index', NULL, 'menu', 3, 1, 1, 1, NULL, '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (10301, 103, '菜单查询', 'B', NULL, NULL, NULL, NULL, NULL, NULL, 1, 1, 1, NULL, 'sys:menu:list', '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (10302, 103, '菜单新增', 'B', NULL, NULL, NULL, NULL, NULL, NULL, 2, 1, 1, NULL, 'sys:menu:create', '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (10303, 103, '菜单编辑', 'B', NULL, NULL, NULL, NULL, NULL, NULL, 3, 1, 1, NULL, 'sys:menu:update', '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (10304, 103, '菜单删除', 'B', NULL, NULL, NULL, NULL, NULL, NULL, 4, 1, 1, NULL, 'sys:menu:delete', '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (104,   1,   '部门管理', 'M', 'dept', 'Dept', NULL, 'system/dept/index', NULL, 'tree', 4, 1, 1, 1, NULL, '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (10401, 104, '部门查询', 'B', NULL, NULL, NULL, NULL, NULL, NULL, 1, 1, 1, NULL, 'sys:dept:list', '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (10402, 104, '部门新增', 'B', NULL, NULL, NULL, NULL, NULL, NULL, 2, 1, 1, NULL, 'sys:dept:create', '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (10403, 104, '部门编辑', 'B', NULL, NULL, NULL, NULL, NULL, NULL, 3, 1, 1, NULL, 'sys:dept:update', '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (10404, 104, '部门删除', 'B', NULL, NULL, NULL, NULL, NULL, NULL, 4, 1, 1, NULL, 'sys:dept:delete', '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (105,   1,   '字典管理', 'M', 'dict', 'Dict', NULL, 'system/dict/index', NULL, 'dict', 5, 1, 1, 1, NULL, '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (10501, 105, '字典查询', 'B', NULL, NULL, NULL, NULL, NULL, NULL, 1, 1, 1, NULL, 'sys:dict:list', '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (10502, 105, '字典新增', 'B', NULL, NULL, NULL, NULL, NULL, NULL, 2, 1, 1, NULL, 'sys:dict:create', '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (10503, 105, '字典编辑', 'B', NULL, NULL, NULL, NULL, NULL, NULL, 3, 1, 1, NULL, 'sys:dict:update', '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (10504, 105, '字典删除', 'B', NULL, NULL, NULL, NULL, NULL, NULL, 4, 1, 1, NULL, 'sys:dict:delete', '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (10505, 105, '字典项查询', 'B', NULL, NULL, NULL, NULL, NULL, NULL, 5, 1, 1, NULL, 'sys:dict-item:list', '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (10506, 105, '字典项新增', 'B', NULL, NULL, NULL, NULL, NULL, NULL, 6, 1, 1, NULL, 'sys:dict-item:create', '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (10507, 105, '字典项编辑', 'B', NULL, NULL, NULL, NULL, NULL, NULL, 7, 1, 1, NULL, 'sys:dict-item:update', '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (10508, 105, '字典项删除', 'B', NULL, NULL, NULL, NULL, NULL, NULL, 8, 1, 1, NULL, 'sys:dict-item:delete', '2026-09-26 08:10:02', '2026-09-26 08:10:02');

-- 超级管理员 / 系统管理员拥有全部菜单权限
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r CROSS JOIN sys_menu m WHERE r.id IN (1, 2);

-- 其余演示角色：可进入各菜单页面，仅有查询权限
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r
JOIN sys_menu m ON m.id IN (1, 101, 102, 103, 104, 105, 10101, 10201, 10301, 10401, 10501)
WHERE r.id BETWEEN 3 AND 10;

-- 自定义权限用户角色的自定义部门数据范围
INSERT INTO sys_role_dept (role_id, dept_id) VALUES
    (7, 1),
    (7, 2);

-- 字典类型
INSERT INTO sys_dict (id, dict_code, name, status, remark, create_time, update_time) VALUES
    (1, 'gender', '性别', 1, '用户性别', '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (2, 'common_status', '通用状态', 1, '启用/禁用状态', '2026-09-26 08:10:02', '2026-09-26 08:10:02');

-- 字典项
INSERT INTO sys_dict_item (id, dict_code, label, value, sort, status, tag_type, create_time, update_time) VALUES
    (1, 'gender', '未知', '0', 1, 1, '', '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (2, 'gender', '男', '1', 2, 1, 'primary', '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (3, 'gender', '女', '2', 3, 1, 'danger', '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (4, 'common_status', '正常', '1', 1, 1, 'primary', '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (5, 'common_status', '禁用', '0', 2, 1, 'danger', '2026-09-26 08:10:02', '2026-09-26 08:10:02');
