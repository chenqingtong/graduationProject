-- 修复任务审批记录中 approved 字段的问题
-- 将任务类型（type='task'）的审批记录的 approved 字段从 '1' 更新为 '0'
-- 因为发起审批时，approved 应该为 '0'（已开启审批），而不是 '1'（未开启）

-- 更新所有任务类型的审批记录，将 approved 从 '1' 改为 '0'
UPDATE `pmhub_project_task_process` 
SET `approved` = '0',
    `updated_time` = NOW()
WHERE `type` = 'task' 
  AND `approved` = '1';

-- 查看更新结果（可选，用于验证）
-- SELECT id, extra_id, type, approved, instance_id, created_time, updated_time 
-- FROM `pmhub_project_task_process` 
-- WHERE `type` = 'task';


