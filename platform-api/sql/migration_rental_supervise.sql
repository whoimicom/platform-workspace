-- 租赁管理-设备锁管理升级脚本（已有数据库执行一次；全新初始化直接使用 platform_api.sql）
-- 用法：SOURCE /path/to/migration_rental_supervise.sql;

USE platform;

-- ------------------------------------------------------------
-- 菜单与按钮权限
-- ------------------------------------------------------------
INSERT IGNORE INTO sys_menu (id, parent_id, name, type, path, route_name, redirect, component, external_url, icon, sort, visible, scope, keep_alive, perm, create_time, update_time) VALUES
    (202,   2,   '设备锁管理', 'M', 'supervise', 'RentalSuperviseLock', NULL, 'rental/supervise/index', NULL, 'security', 2, 1, 1, 1, NULL, NOW(), NOW()),
    (20201, 202, '设备锁查询', 'B', NULL, NULL, NULL, NULL, NULL, NULL, 1, 1, 1, NULL, 'rental:supervise:list', NOW(), NOW());

-- 超级管理员 / 系统管理员：可进入并查询
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r CROSS JOIN sys_menu m
WHERE r.id IN (1, 2) AND m.id IN (202, 20201);

-- 其余演示角色：可进入设备锁页面并查询
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r CROSS JOIN sys_menu m
WHERE r.id BETWEEN 3 AND 10 AND m.id IN (202, 20201);
