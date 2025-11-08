package com.laigeoffer.pmhub.workflow.domain.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 简化审批完成DTO
 * 
 * @author system
 * @date 2024
 */
@Data
public class ApprovalCompleteDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 审批任务ID
     */
    private String approvalTaskId;

    /**
     * 是否通过：true-通过，false-拒绝
     */
    private Boolean approved;

    /**
     * 审批意见
     */
    private String comment;

    /**
     * 抄送人ID（多个用逗号分隔）
     */
    private String copyUserIds;
}

