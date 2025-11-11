package com.laigeoffer.pmhub.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.laigeoffer.pmhub.base.core.core.domain.entity.WfTaskProcess;
import com.laigeoffer.pmhub.project.domain.ProjectTaskProcess;

import java.util.List;

/**
 * 项目任务流程服务接口
 *
 * @author canghe
 */
public interface ProjectTaskProcessService extends IService<ProjectTaskProcess> {

    /**
     * 根据 extraId 和 type 查询任务流程
     *
     * @param extraId 业务ID
     * @param type 类型
     * @return 任务流程
     */
    WfTaskProcess getByExtraIdAndType(String extraId, String type);

    /**
     * 插入或更新任务流程
     *
     * @param wfTaskProcess 任务流程
     * @return 任务流程
     */
    WfTaskProcess insertOrUpdate(WfTaskProcess wfTaskProcess);

    /**
     * 更新任务状态为进行中
     *
     * @param extraId 业务ID
     */
    void updateTaskStatus3(String extraId);

    /**
     * 更新任务状态为已完成
     *
     * @param extraId 业务ID
     */
    void updateTaskStatus(String extraId);

    /**
     * 更新 approved 字段
     *
     * @param extraId 业务ID
     * @param type 类型
     * @param approved 审批状态
     */
    void updateApproved(String extraId, String type, String approved);

    /**
     * 根据任务ID查询任务执行状态
     *
     * @param taskId 任务ID
     * @return 任务执行状态
     */
    Integer getTaskExecuteStatus(String taskId);

    /**
     * 将任务状态重置为未开始
     *
     * @param extraId 业务ID
     */
    void resetTaskStatus(String extraId);

    /**
     * 根据任务ID查询任务状态
     *
     * @param taskId 任务ID
     * @return 任务状态
     */
    Integer getTaskStatus(String taskId);

    /**
     * 根据流程实例ID查询任务流程
     *
     * @param instanceId 流程实例ID
     * @return 任务流程
     */
    WfTaskProcess getByInstanceId(String instanceId);

    /**
     * 根据流程实例ID和类型查询任务流程
     *
     * @param instanceId 流程实例ID
     * @param type 类型
     * @return 任务流程
     */
    WfTaskProcess getByInstanceIdAndType(String instanceId, String type);

    /**
     * 根据类型查询任务流程列表
     *
     * @param type 类型
     * @return 任务流程列表
     */
    List<WfTaskProcess> listByType(String type);

    /**
     * 根据业务ID集合查询任务流程
     *
     * @param extraIds 业务ID集合
     * @return 任务流程列表
     */
    List<WfTaskProcess> listByExtraIds(List<String> extraIds);

    /**
     * 根据业务ID集合和类型查询任务流程
     *
     * @param extraIds 业务ID集合
     * @param type 类型
     * @return 任务流程列表
     */
    List<WfTaskProcess> listByExtraIdsAndType(List<String> extraIds, String type);

    /**
     * 根据主键ID删除任务流程
     *
     * @param id 主键ID
     * @return 是否删除成功
     */
    boolean deleteById(String id);

    /**
     * 清除任务流程与流程实例的关联关系
     *
     * @param extraId 业务ID
     * @param type 类型
     * @param clearUrl 是否清空URL
     * @return 更新后的任务流程
     */
    WfTaskProcess clearAssociation(String extraId, String type, boolean clearUrl);

    /**
     * 根据流程定义ID前缀批量更新任务流程的定义ID和部署ID
     *
     * @param definitionIdPrefix 流程定义ID前缀
     * @param newDefinitionId 新流程定义ID
     * @param newDeploymentId 新部署ID
     */
    void updateDefinitionByPrefix(String definitionIdPrefix, String newDefinitionId, String newDeploymentId);
}

