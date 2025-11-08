package com.laigeoffer.pmhub.workflow.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 简化审批任务实体类（不依赖Flowable流程定义）
 * 
 * @author system
 * @date 2024
 */
@Data
@TableName("pmhub_wf_approval_task")
public class WfApprovalTask implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 审批任务ID
     */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 关联的业务ID（如任务ID、项目ID等）
     */
    private String extraId;

    /**
     * 审批类型（task/project等）
     */
    private String type;

    /**
     * 审批任务标题
     */
    private String title;

    /**
     * 详情页URL
     */
    private String url;

    /**
     * 审批状态：pending-待审批, approved-已通过, rejected-已拒绝, cancelled-已取消
     */
    private String status;

    /**
     * 审批人用户ID
     */
    private String approverId;

    /**
     * 审批人姓名
     */
    private String approverName;

    /**
     * 审批人类型：user-指定用户, role-角色, dept-部门
     */
    private String approverType;

    /**
     * 审批人值（用户ID/角色ID/部门ID，多个用逗号分隔）
     */
    private String approverValue;

    /**
     * 发起人用户ID
     */
    private String initiatorId;

    /**
     * 发起人姓名
     */
    private String initiatorName;

    /**
     * 审批意见
     */
    private String approvalComment;

    /**
     * 审批时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date approvalTime;

    /**
     * 创建人
     */
    private String createdBy;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createdTime;

    /**
     * 更新人
     */
    private String updatedBy;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updatedTime;
}

