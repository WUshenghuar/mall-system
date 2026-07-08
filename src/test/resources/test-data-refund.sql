-- Test data for RefundServiceImplTest

INSERT INTO om_order_refund (id, order_id, order_no, sku_id, quantity, refund_amount, refund_reason, refund_type, refund_status, applicant_id, create_time, update_time)
VALUES (400, 1, 'ORD-REFUND-001', 100, 1, 29.99, '质量问题', 0, 0, 1, NOW(), NOW());

INSERT INTO om_order_refund (id, order_id, order_no, sku_id, quantity, refund_amount, refund_reason, refund_type, refund_status, applicant_id, approver_id, approve_time, approve_comment, create_time, update_time)
VALUES (401, 2, 'ORD-REFUND-002', 200, 2, 50.00, '尺寸不合适', 1, 1, 2, 99, NOW(), '已退款', NOW(), NOW());
