/*
 Navicat Premium Data Transfer

 Source Server         : canghe-local
 Source Server Type    : MySQL
 Source Server Version : 50742 (5.7.42)
 Source Host           : localhost:3306
 Source Schema         : pmhub-project

 Target Server Type    : MySQL
 Target Server Version : 50742 (5.7.42)
 File Encoding         : 65001

 Date: 21/06/2024 16:18:44
*/
CREATE DATABASE  `pmhub-project` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;


SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

USE `pmhub-project`;



-- ----------------------------
-- Table structure for pmhub_project
-- ----------------------------
DROP TABLE IF EXISTS `pmhub_project`;
CREATE TABLE `pmhub_project` (
  `id` varchar(64) NOT NULL COMMENT '主键id',
  `project_code` varchar(32) NOT NULL COMMENT '项目编码',
  `project_name` varchar(200) NOT NULL COMMENT '项目名称',
  `description` varchar(500) DEFAULT NULL COMMENT '描述',
  `close_begin_time` datetime DEFAULT NULL COMMENT '项目开始时间',
  `close_end_time` datetime DEFAULT NULL COMMENT '项目结束时间',
  `cover` varchar(255) DEFAULT NULL COMMENT '封面',
  `stage_code` int(11) NOT NULL DEFAULT '0' COMMENT '项目阶段 默认是0',
  `type` tinyint(1) NOT NULL DEFAULT '0' COMMENT '项目类型 是否私有 0-公开 1-私有',
  `prefix` varchar(20) DEFAULT NULL COMMENT '项目编号前缀',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除 0-否 1-删除',
  `deleted_time` datetime DEFAULT NULL COMMENT '删除时间',
  `archived` tinyint(1) DEFAULT NULL COMMENT '是否归档 0-否 1-归档',
  `archived_time` datetime DEFAULT NULL COMMENT '归档时间',
  `published` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否发布 0-否 1-发布',
  `project_process` decimal(5,2) NOT NULL DEFAULT '0.00' COMMENT '项目进度',
  `created_by` varchar(100) DEFAULT NULL COMMENT '创建人',
  `created_time` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_by` varchar(100) DEFAULT NULL COMMENT '更新人',
  `updated_time` datetime DEFAULT NULL COMMENT '更新时间',
  `user_id` bigint(20) DEFAULT NULL COMMENT '项目负责人',
  `status` tinyint(1) NOT NULL DEFAULT '0' COMMENT '项目状态 默认0-未开始',
  `auto_update_process` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否自动更新进度 0-否 1-是',
  `open_begin_time` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否开启任务开始时间',
  `open_task_private` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否开启新任务默认开启隐私模式',
  `msg_notify` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否开启消息提醒',
  `notify_day` int(11) NOT NULL DEFAULT '2' COMMENT '提醒的天数',
  `open_prefix` tinyint(1) DEFAULT '0' COMMENT '是否开启项目前缀',
  `project_stage_id` varchar(64) DEFAULT NULL COMMENT '阶段id',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目表';

-- ----------------------------
-- Records of pmhub_project
-- ----------------------------
BEGIN;
INSERT INTO `pmhub_project` (`id`, `project_code`, `project_name`, `description`, `close_begin_time`, `close_end_time`, `cover`, `stage_code`, `type`, `prefix`, `deleted`, `deleted_time`, `archived`, `archived_time`, `published`, `project_process`, `created_by`, `created_time`, `updated_by`, `updated_time`, `user_id`, `status`, `auto_update_process`, `open_begin_time`, `open_task_private`, `msg_notify`, `notify_day`, `open_prefix`, `project_stage_id`) VALUES ('aa3a0a9c72c6322d893768c3b05615fa', 'P20240401145842A001', '来个offer', '🔥🔥 来个offer-开源实战项目,助力学生党和工作党拿个更好的offer💪🏻', '2024-04-01 00:00:00', '2024-05-08 00:00:00', '/profile/cover/admin/20240602/pmhub-login-小图.jpg', 0, 0, NULL, 0, NULL, NULL, NULL, 1, 2.00, 'admin', '2024-04-01 14:58:43', 'admin', '2024-06-02 13:59:10', 1, 1, 0, 0, 0, 0, 2, 0, '936cf8a130ce76e39b73e6e0ebe36800');
INSERT INTO `pmhub_project` (`id`, `project_code`, `project_name`, `description`, `close_begin_time`, `close_end_time`, `cover`, `stage_code`, `type`, `prefix`, `deleted`, `deleted_time`, `archived`, `archived_time`, `published`, `project_process`, `created_by`, `created_time`, `updated_by`, `updated_time`, `user_id`, `status`, `auto_update_process`, `open_begin_time`, `open_task_private`, `msg_notify`, `notify_day`, `open_prefix`, `project_stage_id`) VALUES ('c3e462c400f66fe0b68d9d2c1bef9ffc', 'P20240401160110A002', '222', '', NULL, NULL, NULL, 0, 0, NULL, 1, '2024-04-02 14:52:01', NULL, NULL, 0, 0.00, 'admin', '2024-04-01 16:01:10', 'admin', '2024-04-02 14:52:01', 1, 0, 0, 0, 0, 0, 2, 0, '5a3eda6d32b85df82964021a7a042159');
COMMIT;

-- ----------------------------
-- Table structure for pmhub_project_collection
-- ----------------------------
DROP TABLE IF EXISTS `pmhub_project_collection`;
CREATE TABLE `pmhub_project_collection` (
  `id` varchar(64) NOT NULL COMMENT '主键id',
  `created_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `created_time` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `updated_time` datetime DEFAULT NULL COMMENT '更新时间',
  `project_id` varchar(64) DEFAULT NULL COMMENT '项目id',
  `user_id` bigint(20) DEFAULT NULL COMMENT '用户id',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目-收藏表';

-- ----------------------------
-- Records of pmhub_project_collection
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for pmhub_project_file
-- ----------------------------
DROP TABLE IF EXISTS `pmhub_project_file`;
CREATE TABLE `pmhub_project_file` (
  `id` varchar(64) NOT NULL COMMENT '主键id',
  `type` varchar(20) DEFAULT NULL COMMENT '文件归属类型 task 或者 project',
  `pt_id` varchar(64) NOT NULL COMMENT 'type是task 对应就是task的id type是project 对应就是project的id',
  `file_name` varchar(100) NOT NULL COMMENT '文件名称',
  `file_size` decimal(11,2) DEFAULT NULL COMMENT '文件大小',
  `extension` varchar(32) NOT NULL COMMENT '扩展名',
  `file_url` varchar(200) NOT NULL COMMENT '文件完整地址',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除 0-否 1-删除',
  `deleted_time` datetime DEFAULT NULL COMMENT '删除时间',
  `created_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `created_time` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `updated_time` datetime DEFAULT NULL COMMENT '更新时间',
  `user_id` bigint(20) DEFAULT NULL COMMENT '上传人id',
  `project_id` varchar(64) NOT NULL COMMENT '项目id',
  `path_name` varchar(200) DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目或任务附件表';

-- ----------------------------
-- Records of pmhub_project_file
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for pmhub_project_log
-- ----------------------------
DROP TABLE IF EXISTS `pmhub_project_log`;
CREATE TABLE `pmhub_project_log` (
  `id` varchar(64) NOT NULL COMMENT '主键id',
  `user_id` bigint(20) NOT NULL COMMENT '操作人id',
  `type` varchar(16) NOT NULL COMMENT '类型 project 或者 task',
  `operate_type` varchar(32) NOT NULL COMMENT '操作类型',
  `content` text COMMENT '操作内容',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `pt_id` varchar(64) NOT NULL COMMENT '项目或者任务id',
  `to_user_id` bigint(20) DEFAULT NULL,
  `created_by` varchar(64) DEFAULT NULL,
  `created_time` datetime DEFAULT NULL,
  `updated_by` varchar(64) DEFAULT NULL,
  `updated_time` datetime DEFAULT NULL,
  `log_type` tinyint(1) NOT NULL DEFAULT '1' COMMENT '1-动态 2-交付物 3-评论',
  `file_url` varchar(500) DEFAULT NULL COMMENT '文件地址',
  `icon` varchar(20) DEFAULT NULL,
  `project_id` varchar(64) NOT NULL COMMENT '项目id',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目-任务日志';

-- ----------------------------
-- Records of pmhub_project_log
-- ----------------------------
BEGIN;
INSERT INTO `pmhub_project_log` (`id`, `user_id`, `type`, `operate_type`, `content`, `remark`, `pt_id`, `to_user_id`, `created_by`, `created_time`, `updated_by`, `updated_time`, `log_type`, `file_url`, `icon`, `project_id`) VALUES ('032fac95f33172ae8df72333ba257e6d', 173, 'task', 'addTask', 'f', '参与了任务', '0414ef914297d4bef45bf55ec6bba599', NULL, 'laige', '2024-05-31 13:58:25', 'laige', '2024-05-31 13:58:25', 1, NULL, NULL, 'aa3a0a9c72c6322d893768c3b05615fa');
INSERT INTO `pmhub_project_log` (`id`, `user_id`, `type`, `operate_type`, `content`, `remark`, `pt_id`, `to_user_id`, `created_by`, `created_time`, `updated_by`, `updated_time`, `log_type`, `file_url`, `icon`, `project_id`) VALUES ('07b1a38dfae7d38f8abbff308551f813', 1, 'task', 'editTask', '[{\"field\":\"status\",\"fieldName\":\"任务状态\",\"newValue\":\"进行中\",\"oldValue\":\"未开始\"}]', '更新了任务状态', '6dab92bed8d03d904962cca5dace9893', NULL, 'admin', '2024-04-07 16:31:35', 'admin', '2024-04-07 16:31:35', 1, NULL, NULL, 'aa3a0a9c72c6322d893768c3b05615fa');
INSERT INTO `pmhub_project_log` (`id`, `user_id`, `type`, `operate_type`, `content`, `remark`, `pt_id`, `to_user_id`, `created_by`, `created_time`, `updated_by`, `updated_time`, `log_type`, `file_url`, `icon`, `project_id`) VALUES ('0a05e2fdbfa543eb51ec5a1088643c8c', 1, 'project', 'delete', '222', '删除了项目', 'c3e462c400f66fe0b68d9d2c1bef9ffc', NULL, 'admin', '2024-04-02 14:52:01', 'admin', '2024-04-02 14:52:01', 1, NULL, NULL, 'c3e462c400f66fe0b68d9d2c1bef9ffc');
INSERT INTO `pmhub_project_log` (`id`, `user_id`, `type`, `operate_type`, `content`, `remark`, `pt_id`, `to_user_id`, `created_by`, `created_time`, `updated_by`, `updated_time`, `log_type`, `file_url`, `icon`, `project_id`) VALUES ('0b22aabfc66eb9f88fff7e07d2254876', 1, 'project', 'create', '来个offer', '创建了项目', 'aa3a0a9c72c6322d893768c3b05615fa', NULL, 'admin', '2024-04-01 14:58:43', 'admin', '2024-04-01 14:58:43', 1, NULL, NULL, 'aa3a0a9c72c6322d893768c3b05615fa');
INSERT INTO `pmhub_project_log` (`id`, `user_id`, `type`, `operate_type`, `content`, `remark`, `pt_id`, `to_user_id`, `created_by`, `created_time`, `updated_by`, `updated_time`, `log_type`, `file_url`, `icon`, `project_id`) VALUES ('13b05ecff09164e096706c40ff392dde', 1, 'project', 'edit', '来个offer', '编辑了项目', 'aa3a0a9c72c6322d893768c3b05615fa', NULL, 'admin', '2024-05-31 15:26:47', 'admin', '2024-05-31 15:26:47', 1, NULL, NULL, 'aa3a0a9c72c6322d893768c3b05615fa');
INSERT INTO `pmhub_project_log` (`id`, `user_id`, `type`, `operate_type`, `content`, `remark`, `pt_id`, `to_user_id`, `created_by`, `created_time`, `updated_by`, `updated_time`, `log_type`, `file_url`, `icon`, `project_id`) VALUES ('1a99bd82bc42b36359b4e97a2d21963b', 1, 'project', 'edit', '来个offer', '编辑了项目', 'aa3a0a9c72c6322d893768c3b05615fa', NULL, 'admin', '2024-05-30 19:56:54', 'admin', '2024-05-30 19:56:54', 1, NULL, NULL, 'aa3a0a9c72c6322d893768c3b05615fa');
INSERT INTO `pmhub_project_log` (`id`, `user_id`, `type`, `operate_type`, `content`, `remark`, `pt_id`, `to_user_id`, `created_by`, `created_time`, `updated_by`, `updated_time`, `log_type`, `file_url`, `icon`, `project_id`) VALUES ('2aca1b1eb3854281ad02b3bbd68343c5', 1, 'task', 'addTask', '212', '参与了任务', '99b6803d35b3b474568bebf0f97722dc', NULL, 'admin', '2024-04-01 16:01:33', 'admin', '2024-04-01 16:01:33', 1, NULL, NULL, 'c3e462c400f66fe0b68d9d2c1bef9ffc');
INSERT INTO `pmhub_project_log` (`id`, `user_id`, `type`, `operate_type`, `content`, `remark`, `pt_id`, `to_user_id`, `created_by`, `created_time`, `updated_by`, `updated_time`, `log_type`, `file_url`, `icon`, `project_id`) VALUES ('31d5cec856454899238054b485e3f66f', 1, 'task', 'editTask', '[{\"field\":\"description\",\"fieldName\":\"描述\",\"newValue\":\"<p><span style=\\\"color: rgb(99, 108, 118);\\\">🚀来个offer，民间开源实战项目，助力学生党和工作党拿个更好的offer💪🏻，欢迎 Follow 关注我们 👉</span></p>\",\"oldValue\":\"<p><br></p>\"}]', '更新了描述', '6dab92bed8d03d904962cca5dace9893', NULL, 'admin', '2024-04-07 16:30:54', 'admin', '2024-04-07 16:30:54', 1, NULL, NULL, 'aa3a0a9c72c6322d893768c3b05615fa');
INSERT INTO `pmhub_project_log` (`id`, `user_id`, `type`, `operate_type`, `content`, `remark`, `pt_id`, `to_user_id`, `created_by`, `created_time`, `updated_by`, `updated_time`, `log_type`, `file_url`, `icon`, `project_id`) VALUES ('49d8fe1c298a2fdc21499907e6aa47e1', 1, 'task', 'addTask', '第一个任务', '参与了任务', '6dab92bed8d03d904962cca5dace9893', NULL, 'admin', '2024-04-01 15:15:40', 'admin', '2024-04-01 15:15:40', 1, NULL, NULL, 'aa3a0a9c72c6322d893768c3b05615fa');
INSERT INTO `pmhub_project_log` (`id`, `user_id`, `type`, `operate_type`, `content`, `remark`, `pt_id`, `to_user_id`, `created_by`, `created_time`, `updated_by`, `updated_time`, `log_type`, `file_url`, `icon`, `project_id`) VALUES ('4b686f7ab59f8a13eb84853b7f472a40', 1, 'project', 'edit', '来个offer', '编辑了项目', 'aa3a0a9c72c6322d893768c3b05615fa', NULL, 'admin', '2024-05-30 19:56:17', 'admin', '2024-05-30 19:56:17', 1, NULL, NULL, 'aa3a0a9c72c6322d893768c3b05615fa');
INSERT INTO `pmhub_project_log` (`id`, `user_id`, `type`, `operate_type`, `content`, `remark`, `pt_id`, `to_user_id`, `created_by`, `created_time`, `updated_by`, `updated_time`, `log_type`, `file_url`, `icon`, `project_id`) VALUES ('6142aaca5b2a9c38b53ef63c97d754c1', 1, 'project', 'edit', '来个offer', '编辑了项目', 'aa3a0a9c72c6322d893768c3b05615fa', NULL, 'admin', '2024-06-02 13:59:10', 'admin', '2024-06-02 13:59:10', 1, NULL, NULL, 'aa3a0a9c72c6322d893768c3b05615fa');
INSERT INTO `pmhub_project_log` (`id`, `user_id`, `type`, `operate_type`, `content`, `remark`, `pt_id`, `to_user_id`, `created_by`, `created_time`, `updated_by`, `updated_time`, `log_type`, `file_url`, `icon`, `project_id`) VALUES ('6176f2e8aed6113671c092d23f352708', 1, 'task', 'editTask', '[{\"field\":\"description\",\"fieldName\":\"描述\",\"newValue\":\"<p><br></p>\",\"oldValue\":\"<p>测试任务</p>\"}]', '更新了描述', '6dab92bed8d03d904962cca5dace9893', NULL, 'admin', '2024-04-07 16:30:39', 'admin', '2024-04-07 16:30:39', 1, NULL, NULL, 'aa3a0a9c72c6322d893768c3b05615fa');
INSERT INTO `pmhub_project_log` (`id`, `user_id`, `type`, `operate_type`, `content`, `remark`, `pt_id`, `to_user_id`, `created_by`, `created_time`, `updated_by`, `updated_time`, `log_type`, `file_url`, `icon`, `project_id`) VALUES ('77e6f86d17e5d41428de8bc5c9455b02', 1, 'project', 'edit', '来个offer', '编辑了项目', 'aa3a0a9c72c6322d893768c3b05615fa', NULL, 'admin', '2024-04-04 01:33:11', 'admin', '2024-04-04 01:33:11', 1, NULL, NULL, 'aa3a0a9c72c6322d893768c3b05615fa');
INSERT INTO `pmhub_project_log` (`id`, `user_id`, `type`, `operate_type`, `content`, `remark`, `pt_id`, `to_user_id`, `created_by`, `created_time`, `updated_by`, `updated_time`, `log_type`, `file_url`, `icon`, `project_id`) VALUES ('78fa3f7c80ca583f09b2c4883dcdf54f', 1, 'project', 'inviteMember', '超级管理员', '加入了项目', 'c3e462c400f66fe0b68d9d2c1bef9ffc', 1, 'admin', '2024-04-01 16:01:11', 'admin', '2024-04-01 16:01:11', 1, NULL, NULL, 'c3e462c400f66fe0b68d9d2c1bef9ffc');
INSERT INTO `pmhub_project_log` (`id`, `user_id`, `type`, `operate_type`, `content`, `remark`, `pt_id`, `to_user_id`, `created_by`, `created_time`, `updated_by`, `updated_time`, `log_type`, `file_url`, `icon`, `project_id`) VALUES ('8ff312302c1a4ffc52665d8ef79f619d', 1, 'project', 'inviteMember', '超级管理员', '加入了项目', 'aa3a0a9c72c6322d893768c3b05615fa', 1, 'admin', '2024-04-01 14:58:43', 'admin', '2024-04-01 14:58:43', 1, NULL, NULL, 'aa3a0a9c72c6322d893768c3b05615fa');
INSERT INTO `pmhub_project_log` (`id`, `user_id`, `type`, `operate_type`, `content`, `remark`, `pt_id`, `to_user_id`, `created_by`, `created_time`, `updated_by`, `updated_time`, `log_type`, `file_url`, `icon`, `project_id`) VALUES ('cbeb538a3ec25086f13927283967aa54', 1, 'project', 'create', '222', '创建了项目', 'c3e462c400f66fe0b68d9d2c1bef9ffc', NULL, 'admin', '2024-04-01 16:01:11', 'admin', '2024-04-01 16:01:11', 1, NULL, NULL, 'c3e462c400f66fe0b68d9d2c1bef9ffc');
INSERT INTO `pmhub_project_log` (`id`, `user_id`, `type`, `operate_type`, `content`, `remark`, `pt_id`, `to_user_id`, `created_by`, `created_time`, `updated_by`, `updated_time`, `log_type`, `file_url`, `icon`, `project_id`) VALUES ('d22127f1e8baea4efb1706f923537f4a', 1, 'task', 'editTask', '[{\"field\":\"executeStatus\",\"fieldName\":\"执行状态\",\"newValue\":\"进行中\",\"oldValue\":\"未开始\"}]', '更新了执行状态', '6dab92bed8d03d904962cca5dace9893', NULL, 'admin', '2024-04-07 16:31:32', 'admin', '2024-04-07 16:31:32', 1, NULL, NULL, 'aa3a0a9c72c6322d893768c3b05615fa');
INSERT INTO `pmhub_project_log` (`id`, `user_id`, `type`, `operate_type`, `content`, `remark`, `pt_id`, `to_user_id`, `created_by`, `created_time`, `updated_by`, `updated_time`, `log_type`, `file_url`, `icon`, `project_id`) VALUES ('de5710715cfd8793518e9ef69f5f64de', 1, 'task', 'addTask', '第一个任务', '参与了任务', 'f0ddb49790bba0cc58b833470de0d48c', NULL, 'admin', '2024-04-01 14:59:21', 'admin', '2024-04-01 14:59:21', 1, NULL, NULL, 'aa3a0a9c72c6322d893768c3b05615fa');
INSERT INTO `pmhub_project_log` (`id`, `user_id`, `type`, `operate_type`, `content`, `remark`, `pt_id`, `to_user_id`, `created_by`, `created_time`, `updated_by`, `updated_time`, `log_type`, `file_url`, `icon`, `project_id`) VALUES ('e77d004555f2ced2dce8b24d95b30560', 1, 'task', 'editTask', '[{\"field\":\"description\",\"fieldName\":\"描述\",\"newValue\":\"🚀来个offer，民间开源实战项目，助力学生党和工作党拿个更好的offer💪🏻，欢迎 Follow 关注我们 👉﻿\",\"oldValue\":\"<p><span style=\\\"color: rgb(99, 108, 118);\\\">🚀来个offer，民间开源实战项目，助力学生党和工作党拿个更好的offer💪🏻，欢迎 Follow 关注我们 👉</span></p>\"}]', '更新了描述', '6dab92bed8d03d904962cca5dace9893', NULL, 'admin', '2024-06-02 13:31:28', 'admin', '2024-06-02 13:31:28', 1, NULL, NULL, 'aa3a0a9c72c6322d893768c3b05615fa');
INSERT INTO `pmhub_project_log` (`id`, `user_id`, `type`, `operate_type`, `content`, `remark`, `pt_id`, `to_user_id`, `created_by`, `created_time`, `updated_by`, `updated_time`, `log_type`, `file_url`, `icon`, `project_id`) VALUES ('f3583a743885d0c707a2154bbb868f9a', 1, 'task', 'addTask', '第一个任务', '参与了任务', '4b3c00afaf7a6358b2108d465e0db48a', NULL, 'admin', '2024-04-01 14:59:40', 'admin', '2024-04-01 14:59:40', 1, NULL, NULL, 'aa3a0a9c72c6322d893768c3b05615fa');
INSERT INTO `pmhub_project_log` (`id`, `user_id`, `type`, `operate_type`, `content`, `remark`, `pt_id`, `to_user_id`, `created_by`, `created_time`, `updated_by`, `updated_time`, `log_type`, `file_url`, `icon`, `project_id`) VALUES ('f4d694793d04a4b12944517272b56862', 1, 'project', 'edit', '来个offer', '编辑了项目', 'aa3a0a9c72c6322d893768c3b05615fa', NULL, 'admin', '2024-04-04 01:32:44', 'admin', '2024-04-04 01:32:44', 1, NULL, NULL, 'aa3a0a9c72c6322d893768c3b05615fa');
INSERT INTO `pmhub_project_log` (`id`, `user_id`, `type`, `operate_type`, `content`, `remark`, `pt_id`, `to_user_id`, `created_by`, `created_time`, `updated_by`, `updated_time`, `log_type`, `file_url`, `icon`, `project_id`) VALUES ('f7b304aa2784877285cf5a66dc6c9bb6', 1, 'task', 'editTask', '[{\"field\":\"description\",\"fieldName\":\"描述\",\"newValue\":\"🚀来个offer，民间开源实战项目，助力学生党和工作党拿个更好的offer💪🏻，欢迎 Follow 关注我们 👉﻿7\",\"oldValue\":\"🚀来个offer，民间开源实战项目，助力学生党和工作党拿个更好的offer💪🏻，欢迎 Follow 关注我们 👉﻿\"}]', '更新了描述', '6dab92bed8d03d904962cca5dace9893', NULL, 'admin', '2024-06-02 13:34:01', 'admin', '2024-06-02 13:34:01', 1, NULL, NULL, 'aa3a0a9c72c6322d893768c3b05615fa');
COMMIT;

-- ----------------------------
-- Table structure for pmhub_project_member
-- ----------------------------
DROP TABLE IF EXISTS `pmhub_project_member`;
CREATE TABLE `pmhub_project_member` (
  `id` varchar(64) NOT NULL COMMENT '主键id',
  `pt_id` varchar(64) NOT NULL COMMENT '项目或者任务id',
  `user_id` bigint(20) NOT NULL COMMENT '用户id',
  `joined_time` datetime DEFAULT NULL COMMENT '加入时间',
  `created_by` varchar(100) DEFAULT NULL COMMENT '创建人',
  `created_time` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_by` varchar(100) DEFAULT NULL COMMENT '更新人',
  `updated_time` datetime DEFAULT NULL COMMENT '更新时间',
  `type` varchar(32) NOT NULL COMMENT '类型是项目还是任务 task project',
  `creator` tinyint(1) DEFAULT '0' COMMENT '是否创建者',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目-任务成员';

-- ----------------------------
-- Records of pmhub_project_member
-- ----------------------------
BEGIN;
INSERT INTO `pmhub_project_member` (`id`, `pt_id`, `user_id`, `joined_time`, `created_by`, `created_time`, `updated_by`, `updated_time`, `type`, `creator`) VALUES ('0c1d0483413d800a972f8f8598acc3d6', 'f0ddb49790bba0cc58b833470de0d48c', 1, '2024-04-01 14:59:21', 'admin', '2024-04-01 14:59:21', 'admin', '2024-04-01 14:59:21', 'task', 1);
INSERT INTO `pmhub_project_member` (`id`, `pt_id`, `user_id`, `joined_time`, `created_by`, `created_time`, `updated_by`, `updated_time`, `type`, `creator`) VALUES ('3de03dda428830dde4e8a573d293aa79', 'c3e462c400f66fe0b68d9d2c1bef9ffc', 1, '2024-04-01 16:01:11', 'admin', '2024-04-01 16:01:11', 'admin', '2024-04-01 16:01:11', 'project', 1);
INSERT INTO `pmhub_project_member` (`id`, `pt_id`, `user_id`, `joined_time`, `created_by`, `created_time`, `updated_by`, `updated_time`, `type`, `creator`) VALUES ('42c2b685177dd040587626f3f206325e', 'aa3a0a9c72c6322d893768c3b05615fa', 1, '2024-04-01 14:58:43', 'admin', '2024-04-01 14:58:43', 'admin', '2024-04-01 14:58:43', 'project', 1);
INSERT INTO `pmhub_project_member` (`id`, `pt_id`, `user_id`, `joined_time`, `created_by`, `created_time`, `updated_by`, `updated_time`, `type`, `creator`) VALUES ('5d64e940bb99f2fa3f649df30104b614', 'aa3a0a9c72c6322d893768c3b05615fa', 173, '2024-04-04 01:39:16', 'admin', '2024-04-04 01:39:16', 'admin', '2024-04-04 01:39:16', 'project', 0);
INSERT INTO `pmhub_project_member` (`id`, `pt_id`, `user_id`, `joined_time`, `created_by`, `created_time`, `updated_by`, `updated_time`, `type`, `creator`) VALUES ('8e3c1b48af1608c12b49b004c7580dc5', '0414ef914297d4bef45bf55ec6bba599', 173, '2024-05-31 13:58:25', 'laige', '2024-05-31 13:58:25', 'laige', '2024-05-31 13:58:25', 'task', 1);
INSERT INTO `pmhub_project_member` (`id`, `pt_id`, `user_id`, `joined_time`, `created_by`, `created_time`, `updated_by`, `updated_time`, `type`, `creator`) VALUES ('9142a2503746eb0dd082c8b9ef466df9', '6dab92bed8d03d904962cca5dace9893', 1, '2024-04-01 15:15:40', 'admin', '2024-04-01 15:15:40', 'admin', '2024-04-01 15:15:40', 'task', 1);
INSERT INTO `pmhub_project_member` (`id`, `pt_id`, `user_id`, `joined_time`, `created_by`, `created_time`, `updated_by`, `updated_time`, `type`, `creator`) VALUES ('936489d30d610e048cc4b9f8fd644401', '99b6803d35b3b474568bebf0f97722dc', 1, '2024-04-01 16:01:33', 'admin', '2024-04-01 16:01:33', 'admin', '2024-04-01 16:01:33', 'task', 1);
INSERT INTO `pmhub_project_member` (`id`, `pt_id`, `user_id`, `joined_time`, `created_by`, `created_time`, `updated_by`, `updated_time`, `type`, `creator`) VALUES ('9dac17d94f07e0db7e9a41b9774825ad', '4b3c00afaf7a6358b2108d465e0db48a', 1, '2024-04-01 14:59:40', 'admin', '2024-04-01 14:59:40', 'admin', '2024-04-01 14:59:40', 'task', 1);
COMMIT;

-- ----------------------------
-- Table structure for pmhub_project_stage
-- ----------------------------
DROP TABLE IF EXISTS `pmhub_project_stage`;
CREATE TABLE `pmhub_project_stage` (
  `id` varchar(64) NOT NULL COMMENT '主键id',
  `stage_code` int(11) NOT NULL COMMENT '阶段编码',
  `stage_name` varchar(100) NOT NULL COMMENT '阶段名称',
  `description` varchar(255) DEFAULT NULL COMMENT '阶段描述',
  `project_id` varchar(64) NOT NULL COMMENT '项目id',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除 0-否 1-删除',
  `created_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `created_time` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `updated_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目阶段';

-- ----------------------------
-- Records of pmhub_project_stage
-- ----------------------------
BEGIN;
INSERT INTO `pmhub_project_stage` (`id`, `stage_code`, `stage_name`, `description`, `project_id`, `deleted`, `created_by`, `created_time`, `updated_by`, `updated_time`) VALUES ('0daaf2b9de2b3c9bcb9d7c89eeff9c03', 3, '交付验收阶段', NULL, 'c3e462c400f66fe0b68d9d2c1bef9ffc', 0, 'admin', '2024-04-01 16:01:10', 'admin', '2024-04-01 16:01:10');
INSERT INTO `pmhub_project_stage` (`id`, `stage_code`, `stage_name`, `description`, `project_id`, `deleted`, `created_by`, `created_time`, `updated_by`, `updated_time`) VALUES ('4325973a30ce9b9d7d5f1e4fa074891f', 2, '研发实施阶段', NULL, 'c3e462c400f66fe0b68d9d2c1bef9ffc', 0, 'admin', '2024-04-01 16:01:10', 'admin', '2024-04-01 16:01:10');
INSERT INTO `pmhub_project_stage` (`id`, `stage_code`, `stage_name`, `description`, `project_id`, `deleted`, `created_by`, `created_time`, `updated_by`, `updated_time`) VALUES ('5a3eda6d32b85df82964021a7a042159', 0, '项目立项阶段', NULL, 'c3e462c400f66fe0b68d9d2c1bef9ffc', 0, 'admin', '2024-04-01 16:01:10', 'admin', '2024-04-01 16:01:10');
INSERT INTO `pmhub_project_stage` (`id`, `stage_code`, `stage_name`, `description`, `project_id`, `deleted`, `created_by`, `created_time`, `updated_by`, `updated_time`) VALUES ('5c40e5da93bd6fc343a0909a9f01c73c', 3, '交付验收阶段', NULL, 'aa3a0a9c72c6322d893768c3b05615fa', 0, 'admin', '2024-04-01 14:58:43', 'admin', '2024-04-01 14:58:43');
INSERT INTO `pmhub_project_stage` (`id`, `stage_code`, `stage_name`, `description`, `project_id`, `deleted`, `created_by`, `created_time`, `updated_by`, `updated_time`) VALUES ('6359652deefcbc760893e79f55b23a20', 4, '新产品导出阶段', NULL, 'aa3a0a9c72c6322d893768c3b05615fa', 0, 'admin', '2024-04-01 14:58:43', 'admin', '2024-04-01 14:58:43');
INSERT INTO `pmhub_project_stage` (`id`, `stage_code`, `stage_name`, `description`, `project_id`, `deleted`, `created_by`, `created_time`, `updated_by`, `updated_time`) VALUES ('64a586191a41de633fba33976af476ca', 1, '研发设计输入阶段', NULL, 'aa3a0a9c72c6322d893768c3b05615fa', 0, 'admin', '2024-04-01 14:58:43', 'admin', '2024-04-01 14:58:43');
INSERT INTO `pmhub_project_stage` (`id`, `stage_code`, `stage_name`, `description`, `project_id`, `deleted`, `created_by`, `created_time`, `updated_by`, `updated_time`) VALUES ('83641c127c6514ff68222eb666f3cadf', 1, '研发设计输入阶段', NULL, 'c3e462c400f66fe0b68d9d2c1bef9ffc', 0, 'admin', '2024-04-01 16:01:10', 'admin', '2024-04-01 16:01:10');
INSERT INTO `pmhub_project_stage` (`id`, `stage_code`, `stage_name`, `description`, `project_id`, `deleted`, `created_by`, `created_time`, `updated_by`, `updated_time`) VALUES ('8c7441c41bcc7e824a7a4b3c1cd72d24', 2, '研发实施阶段', NULL, 'aa3a0a9c72c6322d893768c3b05615fa', 0, 'admin', '2024-04-01 14:58:43', 'admin', '2024-04-01 14:58:43');
INSERT INTO `pmhub_project_stage` (`id`, `stage_code`, `stage_name`, `description`, `project_id`, `deleted`, `created_by`, `created_time`, `updated_by`, `updated_time`) VALUES ('936cf8a130ce76e39b73e6e0ebe36800', 0, '项目立项阶段', NULL, 'aa3a0a9c72c6322d893768c3b05615fa', 0, 'admin', '2024-04-01 14:58:43', 'admin', '2024-04-01 14:58:43');
INSERT INTO `pmhub_project_stage` (`id`, `stage_code`, `stage_name`, `description`, `project_id`, `deleted`, `created_by`, `created_time`, `updated_by`, `updated_time`) VALUES ('c459c5b373983888cf0a98055a95451f', 4, '新产品导出阶段', NULL, 'c3e462c400f66fe0b68d9d2c1bef9ffc', 0, 'admin', '2024-04-01 16:01:10', 'admin', '2024-04-01 16:01:10');
COMMIT;

-- ----------------------------
-- Table structure for pmhub_project_task
-- ----------------------------
DROP TABLE IF EXISTS `pmhub_project_task`;
CREATE TABLE `pmhub_project_task` (
  `id` varchar(64) NOT NULL COMMENT '主键id',
  `created_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `created_time` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `updated_time` datetime DEFAULT NULL COMMENT '更新时间',
  `task_name` varchar(100) DEFAULT NULL COMMENT '任务名称',
  `project_id` varchar(64) DEFAULT NULL COMMENT '项目id',
  `task_priority` tinyint(1) NOT NULL DEFAULT '0' COMMENT '任务优先级',
  `user_id` bigint(20) NOT NULL COMMENT '用户id',
  `project_stage_id` varchar(64) NOT NULL COMMENT '项目阶段id',
  `description` varchar(500) DEFAULT NULL COMMENT '任务描述',
  `begin_time` datetime DEFAULT NULL COMMENT '预计开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '预计结束时间',
  `close_time` datetime DEFAULT NULL COMMENT '截止时间',
  `task_pid` varchar(64) DEFAULT NULL COMMENT '任务父节点',
  `assign_to` varchar(64) DEFAULT NULL COMMENT '指派给谁',
  `status` tinyint(1) NOT NULL DEFAULT '0' COMMENT '任务状态',
  `execute_status` tinyint(1) NOT NULL DEFAULT '0' COMMENT '执行状态',
  `task_process` decimal(5,2) NOT NULL DEFAULT '0.00' COMMENT '任务进度',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
  `deleted_time` datetime DEFAULT NULL,
  `task_flow` varchar(200) DEFAULT NULL COMMENT '所属流程',
  `task_type_id` varchar(64) DEFAULT NULL COMMENT '任务类型id',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx` (`id`,`project_id`,`user_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目-任务表';

-- ----------------------------
-- Records of pmhub_project_task
-- ----------------------------
BEGIN;
INSERT INTO `pmhub_project_task` (`id`, `created_by`, `created_time`, `updated_by`, `updated_time`, `task_name`, `project_id`, `task_priority`, `user_id`, `project_stage_id`, `description`, `begin_time`, `end_time`, `close_time`, `task_pid`, `assign_to`, `status`, `execute_status`, `task_process`, `deleted`, `deleted_time`, `task_flow`, `task_type_id`) VALUES ('0414ef914297d4bef45bf55ec6bba599', 'laige', '2024-05-31 13:58:25', 'laige', '2024-05-31 13:58:25', 'f', 'aa3a0a9c72c6322d893768c3b05615fa', 4, 173, '', '', NULL, NULL, NULL, NULL, NULL, 0, 0, 0.00, 0, NULL, '', NULL);
INSERT INTO `pmhub_project_task` (`id`, `created_by`, `created_time`, `updated_by`, `updated_time`, `task_name`, `project_id`, `task_priority`, `user_id`, `project_stage_id`, `description`, `begin_time`, `end_time`, `close_time`, `task_pid`, `assign_to`, `status`, `execute_status`, `task_process`, `deleted`, `deleted_time`, `task_flow`, `task_type_id`) VALUES ('4b3c00afaf7a6358b2108d465e0db48a', 'admin', '2024-04-01 14:59:40', 'admin', '2024-04-01 14:59:40', '第一个任务', 'aa3a0a9c72c6322d893768c3b05615fa', 0, 1, '936cf8a130ce76e39b73e6e0ebe36800', '<p>测试任务</p>', NULL, NULL, NULL, NULL, NULL, 0, 0, 0.00, 1, '2024-04-04 01:39:26', '', NULL);
INSERT INTO `pmhub_project_task` (`id`, `created_by`, `created_time`, `updated_by`, `updated_time`, `task_name`, `project_id`, `task_priority`, `user_id`, `project_stage_id`, `description`, `begin_time`, `end_time`, `close_time`, `task_pid`, `assign_to`, `status`, `execute_status`, `task_process`, `deleted`, `deleted_time`, `task_flow`, `task_type_id`) VALUES ('6dab92bed8d03d904962cca5dace9893', 'admin', '2024-04-01 15:15:40', 'admin', '2024-06-02 13:43:56', '第一个任务', 'aa3a0a9c72c6322d893768c3b05615fa', 0, 1, '936cf8a130ce76e39b73e6e0ebe36800', '🚀来个offer，民间开源实战项目，助力学生党和工作党拿个更好的offer💪🏻，欢迎 Follow 关注我们 👉﻿7', NULL, NULL, NULL, NULL, NULL, 1, 1, 0.00, 0, NULL, '', NULL);
INSERT INTO `pmhub_project_task` (`id`, `created_by`, `created_time`, `updated_by`, `updated_time`, `task_name`, `project_id`, `task_priority`, `user_id`, `project_stage_id`, `description`, `begin_time`, `end_time`, `close_time`, `task_pid`, `assign_to`, `status`, `execute_status`, `task_process`, `deleted`, `deleted_time`, `task_flow`, `task_type_id`) VALUES ('99b6803d35b3b474568bebf0f97722dc', 'admin', '2024-04-01 16:01:33', 'admin', '2024-04-01 16:01:33', '212', 'c3e462c400f66fe0b68d9d2c1bef9ffc', 0, 1, '', '<p>12121</p>', NULL, NULL, NULL, NULL, NULL, 0, 0, 0.00, 1, '2024-04-02 14:51:49', '', NULL);
INSERT INTO `pmhub_project_task` (`id`, `created_by`, `created_time`, `updated_by`, `updated_time`, `task_name`, `project_id`, `task_priority`, `user_id`, `project_stage_id`, `description`, `begin_time`, `end_time`, `close_time`, `task_pid`, `assign_to`, `status`, `execute_status`, `task_process`, `deleted`, `deleted_time`, `task_flow`, `task_type_id`) VALUES ('f0ddb49790bba0cc58b833470de0d48c', 'admin', '2024-04-01 14:59:21', 'admin', '2024-04-01 14:59:21', '第一个任务', 'aa3a0a9c72c6322d893768c3b05615fa', 0, 1, '936cf8a130ce76e39b73e6e0ebe36800', '<p>测试任务</p>', NULL, NULL, NULL, NULL, NULL, 0, 0, 0.00, 1, '2024-04-04 01:39:33', '', NULL);
COMMIT;

-- ----------------------------
-- Table structure for pmhub_project_task_notify
-- ----------------------------
DROP TABLE IF EXISTS `pmhub_project_task_notify`;
CREATE TABLE `pmhub_project_task_notify` (
  `id` varchar(64) NOT NULL,
  `task_id` varchar(64) DEFAULT NULL COMMENT '任务id',
  `user_id` int(11) DEFAULT NULL COMMENT '用户id',
  `user_wx_name` varchar(64) DEFAULT NULL COMMENT '企业微信id',
  `project_id` varchar(64) DEFAULT NULL COMMENT '项目id',
  `overdue` tinyint(1) DEFAULT NULL COMMENT '是否逾期 0-否 1-是',
  `close_time` datetime DEFAULT NULL,
  `task_name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务提醒表';

-- ----------------------------
-- Records of pmhub_project_task_notify
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for pmhub_project_task_process
-- ----------------------------
-- ⚠️ 注意：pmhub_project_task_process 表已迁移到 pmhub-workflow 数据库
-- 此表属于工作流模块，由 pmhub-workflow 服务管理
-- 如需创建此表，请使用 sql/pmhub-workflow.sql 脚本
-- 此处的表定义已废弃，保留仅为向后兼容
-- ----------------------------
-- DROP TABLE IF EXISTS `pmhub_project_task_process`;
-- CREATE TABLE `pmhub_project_task_process` (
--   `id` varchar(64) NOT NULL,
--   `extra_id` varchar(64) DEFAULT NULL COMMENT '项目任务id',
--   `approved` varchar(10) DEFAULT NULL COMMENT '是否需要审批',
--   `instance_id` varchar(64) DEFAULT NULL COMMENT '流程实例id',
--   `deployment_id` varchar(64) DEFAULT NULL COMMENT '部署id',
--   `definition_id` varchar(64) DEFAULT NULL COMMENT '流程定义id',
--   `created_by` varchar(64) DEFAULT NULL,
--   `created_time` datetime DEFAULT NULL,
--   `updated_by` varchar(64) DEFAULT NULL,
--   `updated_time` datetime DEFAULT NULL,
--   `type` varchar(64) DEFAULT NULL COMMENT '类型task/project等',
--   `task_id` varchar(64) DEFAULT NULL COMMENT '流程任务id',
--   `url` varchar(1000) DEFAULT NULL COMMENT '详情地址',
--   PRIMARY KEY (`id`) USING BTREE
-- ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Records of pmhub_project_task_process
-- 已迁移到 pmhub-workflow 数据库
-- ----------------------------
-- BEGIN;
-- COMMIT;

-- ----------------------------
-- Table structure for pmhub_project_task_work_time
-- ----------------------------
DROP TABLE IF EXISTS `pmhub_project_task_work_time`;
CREATE TABLE `pmhub_project_task_work_time` (
  `id` varchar(64) NOT NULL COMMENT '主键id',
  `project_task_id` varchar(64) NOT NULL COMMENT '任务id',
  `user_id` varchar(64) NOT NULL COMMENT '用户id',
  `work_time` decimal(5,2) DEFAULT NULL COMMENT '工时',
  `project_id` varchar(64) NOT NULL COMMENT '项目id',
  `created_by` varchar(64) DEFAULT NULL,
  `created_time` datetime DEFAULT NULL,
  `updated_by` varchar(64) DEFAULT NULL,
  `updated_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务工时表';

-- ----------------------------
-- Records of pmhub_project_task_work_time
-- ----------------------------
BEGIN;
COMMIT;

-- for AT mode you must to init this sql for you business database. the seata server not need it.
CREATE TABLE IF NOT EXISTS `undo_log`
(
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
    `branch_id`     BIGINT       NOT NULL COMMENT 'branch transaction id',
    `xid`           VARCHAR(128) NOT NULL COMMENT 'global transaction id',
    `context`       VARCHAR(128) NOT NULL COMMENT 'undo_log context,such as serialization',
    `rollback_info` LONGBLOB     NOT NULL COMMENT 'rollback info',
    `log_status`    INT(11)      NOT NULL COMMENT '0:normal status,1:defense status',
    `log_created`   DATETIME(6)  NOT NULL COMMENT 'create datetime',
    `log_modified`  DATETIME(6)  NOT NULL COMMENT 'modify datetime',
    PRIMARY KEY (`id`),
    UNIQUE KEY `ux_undo_log` (`xid`, `branch_id`)
    ) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT ='AT transaction mode undo table';
ALTER TABLE `undo_log` ADD INDEX `ix_log_created` (`log_created`);

-- ----------------------------
-- Records of undo_log
-- ----------------------------
BEGIN;
COMMIT;


SET FOREIGN_KEY_CHECKS = 1;
