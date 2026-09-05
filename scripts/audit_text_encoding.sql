-- 列出所有包含问号乱码的文本列；仅审计，不修改数据。
SET SESSION group_concat_max_len = 100000;

SELECT GROUP_CONCAT(
    CONCAT(
        'SELECT ''', table_name, '.', column_name,
        ''' AS field_name, COUNT(*) AS affected_rows FROM `', table_name,
        '` WHERE `', column_name,
        '` LIKE ''%?%'' HAVING affected_rows > 0'
    ) SEPARATOR ' UNION ALL '
) INTO @scan_sql
FROM information_schema.columns
WHERE table_schema = 'mall_system'
  AND data_type IN ('char', 'varchar', 'text', 'mediumtext', 'longtext', 'json');

PREPARE scan_stmt FROM @scan_sql;
EXECUTE scan_stmt;
DEALLOCATE PREPARE scan_stmt;
