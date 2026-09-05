-- 清除历史 PowerShell/latin1 导入留下的不可恢复开发分类乱码。
-- 唯一关联的探针商品先迁移到正确的 UTF-8 开发分类。
UPDATE pm_spu
SET category_id = 900001
WHERE spu_code = 'PROBE0001' AND category_id = 1;

DELETE FROM pm_category
WHERE id BETWEEN 1 AND 85
  AND category_name REGEXP '^[?]+-[0-9]{2}$';
