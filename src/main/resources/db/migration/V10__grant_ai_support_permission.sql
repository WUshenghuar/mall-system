INSERT IGNORE INTO sys_menu (id, menu_name, parent_id, order_num, path, perms, menu_type, status, create_time, update_time) VALUES
(36, '平台客服工单', 30, 6, 'support', 'order:support:list', 'C', 1, NOW(), NOW()),
(37, '处理平台客服工单', 36, 1, NULL, 'order:support:handle', 'F', 1, NOW(), NOW());

INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES
(1, 36), (1, 37), (3, 36), (3, 37);
