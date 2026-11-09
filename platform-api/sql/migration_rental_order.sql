-- 租赁管理-订单管理升级脚本（已有数据库执行一次；全新初始化直接使用 platform_api.sql）
-- 用法：SOURCE /path/to/migration_rental_order.sql;

USE platform;

-- ------------------------------------------------------------
-- 租赁订单表
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

-- ------------------------------------------------------------
-- 菜单与按钮权限
-- ------------------------------------------------------------
INSERT IGNORE INTO sys_menu (id, parent_id, name, type, path, route_name, redirect, component, external_url, icon, sort, visible, scope, keep_alive, perm, create_time, update_time) VALUES
    (2,     0,   '租赁管理', 'C', '/rental', NULL, '/rental/order', 'Layout', NULL, 'table', 2, 1, 1, NULL, NULL, NOW(), NOW()),
    (201,   2,   '订单管理', 'M', 'order', 'RentalOrder', NULL, 'rental/order/index', NULL, 'file', 1, 1, 1, 1, NULL, NOW(), NOW()),
    (20101, 201, '订单查询', 'B', NULL, NULL, NULL, NULL, NULL, NULL, 1, 1, 1, NULL, 'rental:order:list', NOW(), NOW()),
    (20102, 201, '订单新增', 'B', NULL, NULL, NULL, NULL, NULL, NULL, 2, 1, 1, NULL, 'rental:order:create', NOW(), NOW()),
    (20103, 201, '订单编辑', 'B', NULL, NULL, NULL, NULL, NULL, NULL, 3, 1, 1, NULL, 'rental:order:update', NOW(), NOW()),
    (20104, 201, '订单删除', 'B', NULL, NULL, NULL, NULL, NULL, NULL, 4, 1, 1, NULL, 'rental:order:delete', NOW(), NOW());

-- 超级管理员 / 系统管理员：全部租赁订单权限
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r CROSS JOIN sys_menu m
WHERE r.id IN (1, 2) AND m.id IN (2, 201, 20101, 20102, 20103, 20104);

-- 其余演示角色：可进入订单页面并查询
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r CROSS JOIN sys_menu m
WHERE r.id BETWEEN 3 AND 10 AND m.id IN (2, 201, 20101);
