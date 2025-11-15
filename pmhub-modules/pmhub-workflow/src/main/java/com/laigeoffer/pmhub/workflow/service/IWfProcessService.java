package com.laigeoffer.pmhub.workflow.service;

import com.laigeoffer.pmhub.base.core.core.domain.PageQuery;
import com.laigeoffer.pmhub.base.core.core.page.Table2DataInfo;
import com.laigeoffer.pmhub.workflow.core.domain.ProcessQuery;
import com.laigeoffer.pmhub.workflow.domain.vo.WfDefinitionVo;
import com.laigeoffer.pmhub.workflow.domain.vo.WfDetailVo;
import com.laigeoffer.pmhub.workflow.domain.vo.WfTaskVo;

import java.util.List;
import java.util.Map;

/**
 * @author chenqingtong
 * @createTime 2024/3/24 18:57
 */
public interface IWfProcessService {

    /**
     * 查询可发起流程列表
     * @param pageQuery 分页参数
     * @return
     */
    Table2DataInfo<WfDefinitionVo> selectPageStartProcessList(ProcessQuery processQuery, PageQuery pageQuery);

    /**
     * 查询可发起流程列表
     */
    List<WfDefinitionVo> selectStartProcessList(ProcessQuery processQuery);

    /**
     * 查询代办任务列表
     * @param pageQuery 分页参数
     */
    Table2DataInfo<WfTaskVo> selectPageTodoProcessList(ProcessQuery processQuery, PageQuery pageQuery);

    /**
     * 查询代办任务列表
     */
    List<WfTaskVo> selectTodoProcessList(ProcessQuery processQuery);

    /**
     * 查询已办任务列表
     * @param pageQuery 分页参数
     */
    Table2DataInfo<WfTaskVo> selectPageFinishedProcessList(ProcessQuery processQuery, PageQuery pageQuery);

    /**
     * 查询已办任务列表
     */
    List<WfTaskVo> selectFinishedProcessList(ProcessQuery processQuery);

    /**
     * 启动任务审批流程实例
     * @param procDefId 流程定义ID
     * @param variables 扩展参数
     */
    void startTaskProcessByDefId(String taskId, String procDefId, String url, Map<String, Object> variables);

    /**
     * 启动项目发布流程实例
     * @param projectId
     * @param procDefId
     * @param url
     * @param variables
     */
    int startProjectProcessByDefId(String projectId, String procDefId, String url, Map<String, Object> variables);

    /**
     * 启动采购入库流程实例
     * @param inboundId
     * @param procDefId
     * @param url
     * @param variables
     */
    void startInboundProcessByDefId(String inboundId, String procDefId, String url, Map<String, Object> variables);

    /**
     * 启动采购退货出库流程实例
     * @param outboundId
     * @param procDefId
     * @param url
     * @param variables
     */
    void startOutboundProcessByDefId(String outboundId, String procDefId, String url, Map<String, Object> variables);

    void startProviderProcessByDefId(String providerId, String procDefId, String url, Map<String, Object> variables);
    /**
     * 通过DefinitionKey启动流程
     * @param procDefKey 流程定义Key
     * @param variables 扩展参数
     */
    void startProcessByDefKey(String procDefKey, Map<String, Object> variables);


    /**
     * 读取xml文件
     * @param processDefId 流程定义ID
     */
    String queryBpmnXmlById(String processDefId);


    /**
     * 查询流程任务详情信息
     * @param procInsId 流程实例ID
     * @param deployId 流程部署ID
     * @param taskId 任务ID
     */
    WfDetailVo queryProcessDetail(String procInsId, String deployId, String taskId);

    void startOtherIntoProcessByDefId(String otherIntoId, String processDefId, String url, Map<String, Object> variables);

    void startOtherOutProcessByDefId(String otherOutId, String processDefId, String url, Map<String, Object> variables);

    void startReturnIntoProcessByDefId(String returnIntoId, String processDefId, String url, Map<String, Object> variables);

    void startScrappedProcessByDefId(String materialsIds, String processDefId, String url, Map<String, Object> variables);

    /**
     * 完成简化审批任务
     * @param dto 审批完成DTO
     */
    void completeSimplifiedApproval(com.laigeoffer.pmhub.workflow.domain.dto.ApprovalCompleteDTO dto);

    /**
     * 查询审批任务列表（用于显示审批进度）
     * @param extraId 业务ID（如任务ID）
     * @param type 审批类型（如task）
     * @return 审批任务列表
     */
    List<com.laigeoffer.pmhub.workflow.domain.WfApprovalTask> getApprovalTaskList(String extraId, String type);
}
