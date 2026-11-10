-- 订单表 菜单与权限（主表 zl_order）
-- 执行后刷新前端或重新登录，菜单才会出现
USE platform;

INSERT INTO sys_menu (parent_id, name, type, path, route_name, redirect, component, icon, sort, visible, scope, keep_alive, create_time, update_time)
SELECT 0, 'rental', 'C', '/rental', NULL, NULL, 'Layout', 'folder', 1, 1, 1, NULL, NOW(), NOW()
  FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE type = 'C' AND path = '/rental');

SET @parent_id := (SELECT id FROM sys_menu WHERE type = 'C' AND path = '/rental' LIMIT 1);

INSERT INTO sys_menu (parent_id, name, type, path, route_name, redirect, component, icon, sort, visible, scope, keep_alive, create_time, update_time)
VALUES (@parent_id, '订单表', 'M', 'order', 'Order', NULL, 'rental/order/index', 'document', 1, 1, 1, 1, NOW(), NOW());
SET @menu_id := LAST_INSERT_ID();

INSERT INTO sys_menu (parent_id, name, type, path, route_name, redirect, component, icon, sort, visible, scope, keep_alive, perm, create_time, update_time)
VALUES (@menu_id, '查询', 'B', NULL, NULL, NULL, NULL, NULL, 1, 1, 1, NULL, 'rental:order:list', NOW(), NOW()),
       (@menu_id, '新增', 'B', NULL, NULL, NULL, NULL, NULL, 2, 1, 1, NULL, 'rental:order:add', NOW(), NOW()),
       (@menu_id, '修改', 'B', NULL, NULL, NULL, NULL, NULL, 3, 1, 1, NULL, 'rental:order:edit', NOW(), NOW()),
       (@menu_id, '删除', 'B', NULL, NULL, NULL, NULL, NULL, 4, 1, 1, NULL, 'rental:order:remove', NOW(), NOW());

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, @menu_id FROM sys_role r WHERE r.id IN (1, 2);
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r JOIN sys_menu m ON m.parent_id = @menu_id AND m.type = 'B'
WHERE r.id IN (1, 2);
