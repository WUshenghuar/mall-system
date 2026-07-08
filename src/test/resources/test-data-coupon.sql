-- Test data for CouponServiceImplTest

INSERT INTO mk_coupon (id, coupon_name, coupon_type, discount, threshold, max_issue, issued_count, status, create_time, update_time)
VALUES (200, '草稿券', 'FULL_REDUCTION', 10.00, 100.00, 100, 0, 0, NOW(), NOW());

INSERT INTO mk_coupon (id, coupon_name, coupon_type, discount, threshold, max_issue, issued_count, status, create_time, update_time)
VALUES (201, '待审核券', 'FULL_REDUCTION', 20.00, 200.00, 50, 0, 1, NOW(), NOW());

INSERT INTO mk_coupon (id, coupon_name, coupon_type, discount, threshold, max_issue, issued_count, status, create_time, update_time)
VALUES (202, '已通过券', 'FULL_REDUCTION', 15.00, 150.00, 200, 10, 2, NOW(), NOW());
