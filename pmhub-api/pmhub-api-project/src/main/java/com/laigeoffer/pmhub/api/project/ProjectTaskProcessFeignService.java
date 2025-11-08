package com.laigeoffer.pmhub.api.project;

import com.laigeoffer.pmhub.api.project.factory.ProjectTaskProcessFeignFallbackFactory;
import com.laigeoffer.pmhub.base.core.constant.SecurityConstants;
import com.laigeoffer.pmhub.base.core.constant.ServiceNameConstants;
import com.laigeoffer.pmhub.base.core.core.domain.R;
import com.laigeoffer.pmhub.base.core.core.domain.entity.WfTaskProcess;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * 项目任务流程 Feign 服务
 *
 * @author canghe
 */
@FeignClient(contextId = "projectTaskProcessFeignService", value = ServiceNameConstants.PROJECT_SERVICE, fallbackFactory = ProjectTaskProcessFeignFallbackFactory.class)
public interface ProjectTaskProcessFeignService {

    /**
     * 根据 extraId 和 type 查询任务流程
     *
     * @param extraId 业务ID
     * @param type 类型
     * @param source 请求来源
     * @return 任务流程
     */
    @GetMapping("/project/taskProcess/getByExtraIdAndType")
    R<WfTaskProcess> getByExtraIdAndType(@RequestParam("extraId") String extraId,
                                         @RequestParam("type") String type,
                                         @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 插入或更新任务流程
     *
     * @param wfTaskProcess 任务流程
     * @param source 请求来源
     * @return 任务流程
     */
    @PostMapping("/project/taskProcess/insertOrUpdate")
    R<WfTaskProcess> insertOrUpdate(@RequestBody WfTaskProcess wfTaskProcess,
                                    @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 更新任务状态为进行中
     *
     * @param extraId 业务ID
     * @param source 请求来源
     */
    @PostMapping("/project/taskProcess/updateTaskStatus3")
    R<Void> updateTaskStatus3(@RequestParam("extraId") String extraId,
                             @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 更新任务状态为已完成
     *
     * @param extraId 业务ID
     * @param source 请求来源
     */
    @PostMapping("/project/taskProcess/updateTaskStatus")
    R<Void> updateTaskStatus(@RequestParam("extraId") String extraId,
                             @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 更新 approved 字段
     *
     * @param extraId 业务ID
     * @param type 类型
     * @param approved 审批状态
     * @param source 请求来源
     */
    @PostMapping("/project/taskProcess/updateApproved")
    R<Void> updateApproved(@RequestParam("extraId") String extraId,
                           @RequestParam("type") String type,
                           @RequestParam("approved") String approved,
                           @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 根据任务ID查询任务执行状态
     *
     * @param taskId 任务ID
     * @param source 请求来源
     * @return 任务执行状态
     */
    @GetMapping("/project/taskProcess/getTaskExecuteStatus")
    R<Integer> getTaskExecuteStatus(@RequestParam("taskId") String taskId,
                                    @RequestHeader(SecurityConstants.FROM_SOURCE) String source);
}

