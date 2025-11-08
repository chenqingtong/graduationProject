package com.laigeoffer.pmhub.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.laigeoffer.pmhub.base.core.core.domain.entity.WfTaskProcess;
import com.laigeoffer.pmhub.project.domain.ProjectTaskProcess;

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
}

