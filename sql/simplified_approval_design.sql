-- ============================================
-- 简化审批流程数据库设计
-- 目的：不依赖 Flowable 流程定义（definition_id）的简化审批流程
-- 数据库：pmhub-workflow（工作流数据库）
-- ============================================
-- 注意：此脚本需要在 pmhub-workflow 数据库中执行
-- 所有以 pmhub_wf_ 开头的表都应该在 pmhub-workflow 数据库中

-- ----------------------------
-- Table structure for pmhub_wf_approval_task
-- 简化审批任务表（不依赖 Flowable 流程定义）
-- 数据库：pmhub-workflow
-- ----------------------------
DROP TABLE IF EXISTS `pmhub_wf_approval_task`;
CREATE TABLE `pmhub_wf_approval_task` (
  `id` varchar(64) NOT NULL COMMENT '审批任务ID',
  `extra_id` varchar(64) NOT NULL COMMENT '关联的业务ID（如任务ID、项目ID等）',
  `type` varchar(32) NOT NULL COMMENT '审批类型（task/project等）',
  `title` varchar(255) DEFAULT NULL COMMENT '审批任务标题',
  `url` varchar(1000) DEFAULT NULL COMMENT '详情页URL',
  `status` varchar(20) NOT NULL DEFAULT 'pending' COMMENT '审批状态：pending-待审批, approved-已通过, rejected-已拒绝, cancelled-已取消',
  `approver_id` varchar(64) NOT NULL COMMENT '审批人用户ID',
  `approver_name` varchar(100) DEFAULT NULL COMMENT '审批人姓名',
  `approver_type` varchar(20) NOT NULL COMMENT '审批人类型：user-指定用户, role-角色, dept-部门',
  `approver_value` varchar(255) DEFAULT NULL COMMENT '审批人值（用户ID/角色ID/部门ID，多个用逗号分隔）',
  `initiator_id` varchar(64) NOT NULL COMMENT '发起人用户ID',
  `initiator_name` varchar(100) DEFAULT NULL COMMENT '发起人姓名',
  `approval_comment` text DEFAULT NULL COMMENT '审批意见',
  `approval_time` datetime DEFAULT NULL COMMENT '审批时间',
  `created_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `updated_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_extra_id_type` (`extra_id`, `type`) USING BTREE,
  KEY `idx_approver_id` (`approver_id`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE,
  KEY `idx_created_time` (`created_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='简化审批任务表（不依赖Flowable流程定义）';

-- ----------------------------
-- 修改 pmhub_wf_approval_set 表
-- 移除对 definition_id 和 deployment_id 的依赖
-- ----------------------------
-- 注意：definition_id 和 deployment_id 字段保留，用于兼容有流程定义的审批
-- 当 definition_id 为空时，使用简化审批流程

-- ----------------------------
-- 修改 pmhub_project_task_process 表
-- 添加简化审批任务关联字段
-- ----------------------------
ALTER TABLE `pmhub_project_task_process` 
ADD COLUMN `approval_task_id` varchar(64) DEFAULT NULL COMMENT '简化审批任务ID（当instance_id为空时使用）' AFTER `task_id`;

-- 添加索引
ALTER TABLE `pmhub_project_task_process` 
ADD INDEX `idx_approval_task_id` (`approval_task_id`) USING BTREE;

