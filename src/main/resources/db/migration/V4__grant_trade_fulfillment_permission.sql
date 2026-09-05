-- C 端交易订单由店长和客服在 B 端完成发货。
INSERT IGNORE INTO sys_menu (id, menu_name, parent_id, order_num, path, perms, menu_type, status, create_time, update_time)
VALUES (35, '交易订单发货', 30, 5, NULL, 'order:edit', 'F', 1, NOW(), NOW());

INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES (1, 35), (3, 35);
