-- 为 pmhub_wf_approval_set 表添加 approval_info 字段
-- 如果字段已存在，此脚本会报错，可以忽略

-- 检查字段是否存在，如果不存在则添加
SET @dbname = DATABASE();
SET @tablename = 'pmhub_wf_approval_set';
SET @columnname = 'approval_info';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (TABLE_SCHEMA = @dbname)
      AND (TABLE_NAME = @tablename)
      AND (COLUMN_NAME = @columnname)
  ) > 0,
  'SELECT 1', -- 字段已存在，不执行任何操作
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @columnname, ' text DEFAULT NULL COMMENT ''审批人信息（JSON字符串）''')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;


