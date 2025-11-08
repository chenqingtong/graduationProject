-- 为 pmhub_wf_approval_set 表添加 approval_info 字段
-- 如果字段已存在，执行此脚本会报错，可以忽略

ALTER TABLE `pmhub_wf_approval_set` 
ADD COLUMN `approval_info` text DEFAULT NULL COMMENT '审批人信息（JSON字符串）';


