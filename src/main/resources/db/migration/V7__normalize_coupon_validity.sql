UPDATE mk_coupon
SET coupon_type = CASE coupon_type
        WHEN '0' THEN 'FULL_REDUCTION'
        WHEN '1' THEN 'DISCOUNT'
        WHEN '2' THEN 'SHIPPING'
        ELSE coupon_type
    END,
    scope = CASE scope
        WHEN '0' THEN 'ALL'
        WHEN '1' THEN 'CATEGORY'
        WHEN '2' THEN 'SKU'
        ELSE scope
    END,
    valid_start = COALESCE(valid_start, create_time, NOW()),
    valid_end = CASE
        WHEN valid_end IS NULL OR valid_end <= COALESCE(valid_start, create_time, NOW())
            THEN DATE_ADD(COALESCE(valid_start, create_time, NOW()), INTERVAL 30 DAY)
        ELSE valid_end
    END,
    currency = COALESCE(currency, 'CNY'),
    per_limit = CASE WHEN per_limit IS NULL OR per_limit < 1 THEN 1 ELSE per_limit END
WHERE deleted = 0;
