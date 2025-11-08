-- ============================================
-- 检查并修复重复的 pmhub_project_task_process 表
-- ============================================
-- 
-- 问题：pmhub_project_task_process 表在 pmhub-project 和 pmhub-workflow 两个数据库中都有
-- 结论：此表应该只在 pmhub-workflow 数据库中存在
-- 
-- 执行步骤：
-- 1. 先检查两个数据库中的数据情况
-- 2. 如果有数据，需要决定是否迁移
-- 3. 删除 pmhub-project 数据库中多余的表
-- ============================================

-- ============================================
-- 步骤1: 检查 pmhub-project 数据库中的数据
-- ============================================
USE `pmhub-project`;

SELECT 
    'pmhub-project数据库' as database_name,
    COUNT(*) as record_count,
    MIN(created_time) as earliest_record,
    MAX(created_time) as latest_record
FROM `pmhub_project_task_process`;

-- 查看具体数据（可选，用于确认是否需要迁移）
-- SELECT * FROM `pmhub_project_task_process` LIMIT 10;

-- ============================================
-- 步骤2: 检查 pmhub-workflow 数据库中的数据
-- ============================================
USE `pmhub-workflow`;

SELECT 
    'pmhub-workflow数据库' as database_name,
    COUNT(*) as record_count,
    MIN(created_time) as earliest_record,
    MAX(created_time) as latest_record
FROM `pmhub_project_task_process`;

-- 查看具体数据（可选）
-- SELECT * FROM `pmhub_project_task_process` LIMIT 10;

-- ============================================
-- 步骤3: 如果需要迁移数据（仅在必要时执行）
-- ============================================
-- 注意：执行前请先备份两个数据库！
-- 
-- 如果 pmhub-project 数据库中有数据，而 pmhub-workflow 中没有，
-- 可以使用以下语句迁移（需要根据实际情况修改）：
--
-- INSERT INTO `pmhub-workflow`.`pmhub_project_task_process`
-- SELECT * FROM `pmhub-project`.`pmhub_project_task_process`
-- WHERE id NOT IN (SELECT id FROM `pmhub-workflow`.`pmhub_project_task_process`);
-- ============================================

-- ============================================
-- 步骤4: 删除 pmhub-project 数据库中的多余表
-- ============================================
-- ⚠️ 警告：执行此操作前，请确保：
-- 1. 已经备份了 pmhub-project 数据库
-- 2. 已经确认 pmhub-workflow 数据库中有正确的数据
-- 3. 已经确认不再需要 pmhub-project 数据库中的表
--
-- 执行以下语句删除 pmhub-project 数据库中的表：
-- USE `pmhub-project`;
-- DROP TABLE IF EXISTS `pmhub_project_task_process`;
-- ============================================


