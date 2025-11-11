package com.laigeoffer.pmhub.api.project;

import com.laigeoffer.pmhub.api.project.factory.ProjectTaskProcessFeignFallbackFactory;
import com.laigeoffer.pmhub.base.core.constant.SecurityConstants;
import com.laigeoffer.pmhub.base.core.constant.ServiceNameConstants;
import com.laigeoffer.pmhub.base.core.core.domain.R;
import com.laigeoffer.pmhub.base.core.core.domain.entity.WfTaskProcess;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
     * 将任务状态重置为未开始
     *
     * @param extraId 业务ID
     * @param source 请求来源
     */
    @PostMapping("/project/taskProcess/resetTaskStatus")
    R<Void> resetTaskStatus(@RequestParam("extraId") String extraId,
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

    /**
     * 根据任务ID查询任务状态
     *
     * @param taskId 任务ID
     * @param source 请求来源
     * @return 任务状态
     */
    @GetMapping("/project/taskProcess/getTaskStatus")
    R<Integer> getTaskStatus(@RequestParam("taskId") String taskId,
                             @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 根据流程实例ID查询任务流程
     */
    @GetMapping("/project/taskProcess/getByInstanceId")
    R<WfTaskProcess> getByInstanceId(@RequestParam("instanceId") String instanceId,
                                     @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 根据流程实例ID和类型查询任务流程
     */
    @GetMapping("/project/taskProcess/getByInstanceIdAndType")
    R<WfTaskProcess> getByInstanceIdAndType(@RequestParam("instanceId") String instanceId,
                                            @RequestParam("type") String type,
                                            @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 根据类型查询任务流程列表
     */
    @GetMapping("/project/taskProcess/listByType")
    R<List<WfTaskProcess>> listByType(@RequestParam("type") String type,
                                      @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 根据业务ID集合查询任务流程
     */
    @PostMapping("/project/taskProcess/listByExtraIds")
    R<List<WfTaskProcess>> listByExtraIds(@RequestBody List<String> extraIds,
                                          @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 根据业务ID集合和类型查询任务流程
     */
    @PostMapping("/project/taskProcess/listByExtraIdsAndType")
    R<List<WfTaskProcess>> listByExtraIdsAndType(@RequestBody Map<String, Object> params,
                                                 @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 根据主键ID删除任务流程
     */
    @DeleteMapping("/project/taskProcess/{id}")
    R<Void> deleteById(@PathVariable("id") String id,
                       @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 清除任务流程关联
     */
    @PostMapping("/project/taskProcess/clearAssociation")
    R<WfTaskProcess> clearAssociation(@RequestParam("extraId") String extraId,
                                      @RequestParam("type") String type,
                                      @RequestParam(value = "clearUrl", defaultValue = "false") boolean clearUrl,
                                      @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 根据定义ID前缀更新任务流程
     */
    @PostMapping("/project/taskProcess/updateDefinitionByPrefix")
    R<Void> updateDefinitionByPrefix(@RequestBody Map<String, String> params,
                                     @RequestHeader(SecurityConstants.FROM_SOURCE) String source);
}

