package com.laigeoffer.pmhub.workflow.domain.vo;

import lombok.Data;

/**
 * @author chenqingtong
 * @date 2023-04-23 13:55
 */
@Data
public class MaterialsApprovalSetVO {

    private String type;
    private String approved;
    private String definitionId;
    private String deploymentId;
    /**
     * 审批人信息（JSON字符串）
     */
    private String approvalInfo;
}
