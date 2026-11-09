-- ============================================================
-- platform-api 初始化脚本（MySQL 8）
-- 使用方式：
--   1. 创建一个空数据库（脚本内已包含 CREATE DATABASE IF NOT EXISTS admin）
--   2. 登录 MySQL 后执行：SOURCE /path/to/platform_api.sql;
--   3. 初始账号密码统一由后端首次启动时初始化（见 sys_user.password = NEED_INIT），
--      默认密码来自配置项 app.default-password（默认 123456）
-- ============================================================

CREATE DATABASE IF NOT EXISTS platform DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE platform;

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
    UNIQUE KEY uk_sys_user_mobile (mobile),
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

-- ------------------------------------------------------------
-- 通知公告 / 阅读记录
-- ------------------------------------------------------------
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

-- ------------------------------------------------------------
-- 租赁订单
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `zl_order` (
  `order_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '订单号',
  `tenant` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '租户主体：HHHZ: 好货惠租；HHX: 换换星',
  `channel_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '渠道类型',
  `cre_sn` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '身份证号',
  `status` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '订单状态',
  `product_id` bigint DEFAULT NULL COMMENT '产品Id',
  `address_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '收获方式：0-自提；1-邮寄',
  `address_json` json DEFAULT NULL COMMENT '地址',
  `sku_json` json DEFAULT NULL COMMENT '产品sku',
  `product_contract_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '1: 到期买断 / 归还2. 租满即送',
  `borrow_days` int DEFAULT NULL COMMENT '借款天数',
  `tenor` int DEFAULT NULL COMMENT '期数',
  `number` int DEFAULT NULL COMMENT '租用数量',
  `pay_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '支付方式：1-指定还款;2-预收多期;4:次月为首期',
  `amount` decimal(10,2) DEFAULT NULL COMMENT '订单金额',
  `month_amount` decimal(10,2) DEFAULT NULL COMMENT '月租金',
  `deposit` decimal(10,2) DEFAULT NULL COMMENT '押金',
  `buy_out_price` decimal(10,2) DEFAULT NULL COMMENT '买断价',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `create_user` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `settle_type` varchar(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '商户结算方式(1:一次性结算 2:分次结算)',
  `merchant_id` bigint DEFAULT NULL COMMENT '商户ID',
  `merchant_amt` decimal(10,2) DEFAULT NULL COMMENT '商户结算总金额',
  `merchant_after_amt` decimal(10,2) DEFAULT NULL COMMENT '商户延期结算金额',
  `sales_id` bigint DEFAULT NULL COMMENT '业代ID',
  `pay_tenor` int DEFAULT NULL COMMENT '支付期数',
  `pay_first_amount` decimal(10,2) DEFAULT NULL COMMENT '首付金额',
  `buy_out_flag` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '买断标识(是否需要支付)',
  `broken_fee` decimal(10,2) DEFAULT NULL COMMENT '碎屏险',
  `modify_fee` decimal(10,2) DEFAULT NULL COMMENT '维修费',
  `lock_fee` decimal(10,2) DEFAULT NULL COMMENT '锁机费',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `take_date` date DEFAULT NULL COMMENT '提货日期',
  `address_deliver` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '交付地址',
  `color` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '颜色',
  `refuse_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '拒绝码',
  `account_date` date DEFAULT NULL COMMENT '非首期账单日',
  `approve_user` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '审核人',
  `approve_time` datetime DEFAULT NULL COMMENT '审核时间',
  `finish_time` datetime DEFAULT NULL COMMENT '订单完成时间',
  `is_notarial` varchar(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '是否公证(0:否 1:是)',
  `notarial_result` varchar(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '公证结果(-1:审核拒绝 0:未公证 2:公证办理中 3:已发证 4:撤销公证)',
  `pass_code` varchar(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '审核通过代码',
  `version` int DEFAULT '0' COMMENT '数据锁',
  `back_date` datetime DEFAULT NULL COMMENT '退货时间',
  `back_user` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '退货人',
  `official_price` decimal(10,2) DEFAULT NULL COMMENT '官网价格',
  `keep_entry` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '提前归还保持入账标识(0:否 1:是)',
  `common_id` bigint DEFAULT NULL COMMENT '公共账户ID',
  `member_fee` decimal(10,2) DEFAULT NULL COMMENT '会员费',
  `claim_user` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `claim_time` datetime DEFAULT NULL,
  `channel` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '扣款渠道(SNXY:苏宁 ZJXY:中金)',
  `merchant_no` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '扣款商户',
  `job_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '职业类型',
  `job_detail` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '职业',
  `more_first_amount` decimal(10,2) DEFAULT NULL COMMENT '首付增加金额',
  `return_status` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '产品状态',
  `use_phone` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '当前使用手机',
  `guarantee` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '是否担保',
  `pass_point` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '通过支撑点',
  `risk_point` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '风险点',
  `report_msg` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '保单信息',
  `subsidy_amount` decimal(10,2) DEFAULT NULL COMMENT '补贴金额',
  `first_pay_type` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '首付支付方式',
  `sensitive_msg` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '审核敏感词',
  `first_pay_source` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '首付来源',
  `follow_user` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '跟进人',
  `follow_time` datetime DEFAULT NULL COMMENT '跟进时间',
  PRIMARY KEY (`order_id`) USING BTREE,
  UNIQUE KEY `orderId_index` (`order_id`) USING BTREE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '订单表';

-- ============================================================
-- 代码生成模块
-- ============================================================
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

-- ============================================================
-- 初始化数据
-- ============================================================

-- 部门
INSERT INTO sys_dept (id, parent_id, name, code, sort, status, tree_path, create_time, update_time) VALUES
    (1, 0, 'whoimi', 'whoimi', 1, 1, '1', '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
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
    (1, 'whoimi', 'NEED_INIT', 'whoimi', 'https://foruda.gitee.com/images/1723603502796844527/03cdca2a_716974.gif', 0, NULL, NULL, 1, NULL, '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (2, 'admin', 'NEED_INIT', '系统管理员', 'https://foruda.gitee.com/images/1723603502796844527/03cdca2a_716974.gif', 1, '18888888888', 'whoimi@163.com', 1, 1, '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (3, 'test', 'NEED_INIT', '测试小用户', 'https://foruda.gitee.com/images/1723603502796844527/03cdca2a_716974.gif', 1, '18812345679', 'whoimi@163.com', 1, 3, '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (4, 'dept_manager', 'NEED_INIT', '部门主管', 'https://foruda.gitee.com/images/1723603502796844527/03cdca2a_716974.gif', 1, '18812345680', 'manager@whoimi.com', 1, 1, '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (5, 'dept_member', 'NEED_INIT', '部门成员', 'https://foruda.gitee.com/images/1723603502796844527/03cdca2a_716974.gif', 1, '18812345681', 'member@whoimi.com', 1, 1, '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (6, 'employee', 'NEED_INIT', '普通员工', 'https://foruda.gitee.com/images/1723603502796844527/03cdca2a_716974.gif', 1, '18812345682', 'employee@whoimi.com', 1, 2, '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (7, 'custom_user', 'NEED_INIT', '自定义权限用户', 'https://foruda.gitee.com/images/1723603502796844527/03cdca2a_716974.gif', 1, '18812345683', 'custom@whoimi.com', 1, 3, '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (8, 'manager', 'NEED_INIT', '总经理', 'https://foruda.gitee.com/images/1723603502796844527/03cdca2a_716974.gif', 1, '18812345684', 'manager@whoimi.com', 1, 1, '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (9, 'finance', 'NEED_INIT', '财务专员', 'https://foruda.gitee.com/images/1723603502796844527/03cdca2a_716974.gif', 2, '18812345685', 'finance@whoimi.com', 1, 1, '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (10, 'clerk', 'NEED_INIT', '行政专员', 'https://foruda.gitee.com/images/1723603502796844527/03cdca2a_716974.gif', 2, '18812345686', 'clerk@whoimi.com', 1, 1, '2026-09-26 08:10:02', '2026-09-26 08:10:02');

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
    (10508, 105, '字典项删除', 'B', NULL, NULL, NULL, NULL, NULL, NULL, 8, 1, 1, NULL, 'sys:dict-item:delete', '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (106,   1,   '通知公告', 'M', 'notice', 'Notice', NULL, 'system/notice/index', NULL, 'document', 6, 1, 1, 1, NULL, '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (10601, 106, '通知查询', 'B', NULL, NULL, NULL, NULL, NULL, NULL, 1, 1, 1, NULL, 'sys:notice:list', '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (10602, 106, '通知新增', 'B', NULL, NULL, NULL, NULL, NULL, NULL, 2, 1, 1, NULL, 'sys:notice:create', '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (10603, 106, '通知编辑', 'B', NULL, NULL, NULL, NULL, NULL, NULL, 3, 1, 1, NULL, 'sys:notice:update', '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (10604, 106, '通知删除', 'B', NULL, NULL, NULL, NULL, NULL, NULL, 4, 1, 1, NULL, 'sys:notice:delete', '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (10605, 106, '通知发布', 'B', NULL, NULL, NULL, NULL, NULL, NULL, 5, 1, 1, NULL, 'sys:notice:publish', '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (10606, 106, '通知撤回', 'B', NULL, NULL, NULL, NULL, NULL, NULL, 6, 1, 1, NULL, 'sys:notice:revoke', '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (2,     0,   '租赁管理', 'C', '/rental', NULL, '/rental/order', 'Layout', NULL, 'table', 2, 1, 1, NULL, NULL, '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (201,   2,   '订单管理', 'M', 'order', 'RentalOrder', NULL, 'rental/order/index', NULL, 'file', 1, 1, 1, 1, NULL, '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (20101, 201, '订单查询', 'B', NULL, NULL, NULL, NULL, NULL, NULL, 1, 1, 1, NULL, 'rental:order:list', '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (20102, 201, '订单新增', 'B', NULL, NULL, NULL, NULL, NULL, NULL, 2, 1, 1, NULL, 'rental:order:create', '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (20103, 201, '订单编辑', 'B', NULL, NULL, NULL, NULL, NULL, NULL, 3, 1, 1, NULL, 'rental:order:update', '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (20104, 201, '订单删除', 'B', NULL, NULL, NULL, NULL, NULL, NULL, 4, 1, 1, NULL, 'rental:order:delete', '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (202,   2,   '设备锁管理', 'M', 'supervise', 'RentalSuperviseLock', NULL, 'rental/supervise/index', NULL, 'security', 2, 1, 1, 1, NULL, '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (20201, 202, '设备锁查询', 'B', NULL, NULL, NULL, NULL, NULL, NULL, 1, 1, 1, NULL, 'rental:supervise:list', '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (3,     0,   '代码生成', 'C', '/gen', NULL, '/gen/config', 'Layout', NULL, 'code', 3, 1, 1, NULL, NULL, '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (301,   3,   '生成配置', 'M', 'config', 'GenConfig', NULL, 'gen/config/index', NULL, 'code', 1, 1, 1, 1, NULL, '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (30101, 301, '配置查询', 'B', NULL, NULL, NULL, NULL, NULL, NULL, 1, 1, 1, NULL, 'gen:config:list', '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (30102, 301, '导入表', 'B', NULL, NULL, NULL, NULL, NULL, NULL, 2, 1, 1, NULL, 'gen:config:import', '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (30103, 301, '配置编辑', 'B', NULL, NULL, NULL, NULL, NULL, NULL, 3, 1, 1, NULL, 'gen:config:edit', '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (30104, 301, '配置删除', 'B', NULL, NULL, NULL, NULL, NULL, NULL, 4, 1, 1, NULL, 'gen:config:delete', '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (30105, 301, '代码预览', 'B', NULL, NULL, NULL, NULL, NULL, NULL, 5, 1, 1, NULL, 'gen:config:preview', '2026-09-26 08:10:02', '2026-09-26 08:10:02');

-- 超级管理员 / 系统管理员拥有全部菜单权限
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r CROSS JOIN sys_menu m WHERE r.id IN (1, 2);

-- 其余演示角色：可进入各菜单页面，仅有查询权限
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r
JOIN sys_menu m ON m.id IN (1, 101, 102, 103, 104, 105, 106, 2, 201, 202, 3, 301, 10101, 10201, 10301, 10401, 10501, 10601, 20101, 20201, 30101)
WHERE r.id BETWEEN 3 AND 10;

-- 自定义权限用户角色的自定义部门数据范围
INSERT INTO sys_role_dept (role_id, dept_id) VALUES
    (7, 1),
    (7, 2);

-- 字典类型
INSERT INTO sys_dict (id, dict_code, name, status, remark, create_time, update_time) VALUES
    (1, 'gender', '性别', 1, '用户性别', '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (2, 'common_status', '通用状态', 1, '启用/禁用状态', '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (3, 'notice_type', '通知类型', 1, '通知公告类型', '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (4, 'notice_level', '通知等级', 1, '通知公告等级', '2026-09-26 08:10:02', '2026-09-26 08:10:02');

-- 字典项
INSERT INTO sys_dict_item (id, dict_code, label, value, sort, status, tag_type, create_time, update_time) VALUES
    (1, 'gender', '未知', '0', 1, 1, '', '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (2, 'gender', '男', '1', 2, 1, 'primary', '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (3, 'gender', '女', '2', 3, 1, 'danger', '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (4, 'common_status', '正常', '1', 1, 1, 'primary', '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (5, 'common_status', '禁用', '0', 2, 1, 'danger', '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (6, 'notice_type', '通知', '1', 1, 1, 'primary', '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (7, 'notice_type', '公告', '2', 2, 1, 'success', '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (8, 'notice_level', '低', 'L', 1, 1, 'info', '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (9, 'notice_level', '中', 'M', 2, 1, 'warning', '2026-09-26 08:10:02', '2026-09-26 08:10:02'),
    (10, 'notice_level', '高', 'H', 3, 1, 'danger', '2026-09-26 08:10:02', '2026-09-26 08:10:02');
