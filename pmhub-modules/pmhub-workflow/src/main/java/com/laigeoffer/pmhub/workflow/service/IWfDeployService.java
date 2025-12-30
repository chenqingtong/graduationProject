package com.laigeoffer.pmhub.workflow.service;

import com.laigeoffer.pmhub.workflow.domain.WfMaterialsScrappedProcess;
import com.laigeoffer.pmhub.base.core.core.domain.entity.WfTaskProcess;
import com.laigeoffer.pmhub.base.core.core.domain.dto.ApprovalSetDTO;
import com.laigeoffer.pmhub.workflow.domain.dto.MaterialsApprovalSetDTO;
import com.laigeoffer.pmhub.workflow.domain.vo.MaterialsApprovalSetVO;

import java.util.List;

/**
 * 审批设置服务接口
 * 提供审批流程配置相关的业务逻辑处理
 *
 * 主要功能：
 * 1. 审批设置的查询、创建、更新
 * 2. 审批流程模板的管理
 * 3. 审批人信息的配置
 *
 * 注意：已删除 Flowable 流程部署相关方法，仅保留审批设置相关方法
 *
 * @author chenqingtong
 * @createTime 2024/6/30 9:03
 */
public interface IWfDeployService {

    /**
     * 新增或更新审批设置
     *
     * @param approvalSetDTO 审批设置数据传输对象
     * @param type 审批类型
     */
    void approvalSet(MaterialsApprovalSetDTO approvalSetDTO, String type);

    /**
     * 根据类型查询审批设置
     * 用于获取指定类型和任务ID的审批配置信息
     *
     * @param type 审批类型 ("task", "project", "template" 等)
     * @param taskId 任务ID，可选参数
     * @return MaterialsApprovalSetVO 审批设置信息
     */
    MaterialsApprovalSetVO queryApprovalSet(String type, String taskId);
    boolean updateApprovalSet(ApprovalSetDTO approvalSetDTO, String type);
    boolean updateApprovalSet2(ApprovalSetDTO approvalSetDTO, String type);
    boolean insertApprovalSet();
    WfTaskProcess insertWfTaskProcess(String extraId, String type, String approved, String definitionId, String deploymentId);
    boolean insertOrUpdateApprovalSet(String extraId, String type, String approved, String definitionId, String deploymentId);
    boolean insertOrUpdateApprovalSet(ApprovalSetDTO approvalSetDTO);
    List<WfMaterialsScrappedProcess> insertScrappedProcess(List<String> ids, MaterialsApprovalSetVO materialsApprovalSetVO);
    List<WfTaskProcess> selectList(List<String> taskId);
    List<WfTaskProcess> selectWfTaskProcessList(List<String> extraId, String type);
    void updateProviderApproval(String providerId);
    List<WfMaterialsScrappedProcess> selectScrappedList(List<String> ids);
}
