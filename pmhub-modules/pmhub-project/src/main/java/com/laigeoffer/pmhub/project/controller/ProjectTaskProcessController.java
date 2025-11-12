package com.laigeoffer.pmhub.project.controller;

import com.laigeoffer.pmhub.base.core.annotation.Log;
import com.laigeoffer.pmhub.base.core.core.domain.R;
import com.laigeoffer.pmhub.base.core.core.domain.entity.WfTaskProcess;
import com.laigeoffer.pmhub.base.core.enums.BusinessType;
import com.laigeoffer.pmhub.base.security.annotation.InnerAuth;
import com.laigeoffer.pmhub.project.domain.ProjectTask;
import com.laigeoffer.pmhub.project.service.ProjectTaskProcessService;
import com.laigeoffer.pmhub.project.service.ProjectTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 项目任务流程控制器
 *
 * @author canghe
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/project/taskProcess")
@Slf4j
public class ProjectTaskProcessController {

    private final ProjectTaskProcessService projectTaskProcessService;
    private final ProjectTaskService projectTaskService;

    /**
     * 根据 extraId 和 type 查询任务流程
     */
    @InnerAuth
    @GetMapping("/getByExtraIdAndType")
    public R<WfTaskProcess> getByExtraIdAndType(@RequestParam("extraId") String extraId,
                                                  @RequestParam("type") String type) {
        WfTaskProcess wfTaskProcess = projectTaskProcessService.getByExtraIdAndType(extraId, type);
        return R.ok(wfTaskProcess);
    }

    /**
     * 插入或更新任务流程
     */
    @InnerAuth
    @PostMapping("/insertOrUpdate")
    @Log(title = "任务流程", businessType = BusinessType.UPDATE)
    public R<WfTaskProcess> insertOrUpdate(@RequestBody WfTaskProcess wfTaskProcess) {
        WfTaskProcess result = projectTaskProcessService.insertOrUpdate(wfTaskProcess);
        return R.ok(result);
    }

    /**
     * 更新任务状态为进行中
     */
    @InnerAuth
    @PostMapping("/updateTaskStatus3")
    @Log(title = "任务流程", businessType = BusinessType.UPDATE)
    public R<Void> updateTaskStatus3(@RequestParam("extraId") String extraId) {
        projectTaskProcessService.updateTaskStatus3(extraId);
        return R.ok();
    }

    /**
     * 更新任务状态为已完成
     */
    @InnerAuth
    @PostMapping("/updateTaskStatus")
    @Log(title = "任务流程", businessType = BusinessType.UPDATE)
    public R<Void> updateTaskStatus(@RequestParam("extraId") String extraId) {
        projectTaskProcessService.updateTaskStatus(extraId);
        return R.ok();
    }

    /**
     * 重置任务状态为未开始
     */
    @InnerAuth
    @PostMapping("/resetTaskStatus")
    @Log(title = "任务流程", businessType = BusinessType.UPDATE)
    public R<Void> resetTaskStatus(@RequestParam("extraId") String extraId) {
        projectTaskProcessService.resetTaskStatus(extraId);
        return R.ok();
    }

    /**
     * 更新 approved 字段
     */
    @InnerAuth
    @PostMapping("/updateApproved")
    @Log(title = "任务流程", businessType = BusinessType.UPDATE)
    public R<Void> updateApproved(@RequestParam("extraId") String extraId,
                                  @RequestParam("type") String type,
                                  @RequestParam("approved") String approved) {
        projectTaskProcessService.updateApproved(extraId, type, approved);
        return R.ok();
    }

    /**
     * 根据任务ID查询任务执行状态
     */
    @InnerAuth
    @GetMapping("/getTaskExecuteStatus")
    public R<Integer> getTaskExecuteStatus(@RequestParam("taskId") String taskId) {
        Integer status = projectTaskProcessService.getTaskExecuteStatus(taskId);
        return R.ok(status);
    }

    /**
     * 根据任务ID查询任务状态
     */
    @InnerAuth
    @GetMapping("/getTaskStatus")
    public R<Integer> getTaskStatus(@RequestParam("taskId") String taskId) {
        Integer status = projectTaskProcessService.getTaskStatus(taskId);
        return R.ok(status);
    }

    /**
     * 根据任务ID查询任务名称
     */
    @InnerAuth
    @GetMapping("/getTaskNameById")
    public R<String> getTaskNameById(@RequestParam("taskId") String taskId) {
        if (log.isInfoEnabled()) {
            log.info("内部调用获取任务名称, taskId={}", taskId);
        }
        ProjectTask task = projectTaskService.getById(taskId);
        if (task == null) {
            log.warn("根据任务ID未查询到任务, taskId={}", taskId);
            return R.fail("任务不存在");
        }
        if (log.isInfoEnabled()) {
            log.info("查询任务名称成功, taskId={}, taskName={}", taskId, task.getTaskName());
        }
        return R.ok("查询任务名称成功", task.getTaskName());
    }

    /**
     * 根据实例ID查询任务流程
     */
    @InnerAuth
    @GetMapping("/getByInstanceId")
    public R<WfTaskProcess> getByInstanceId(@RequestParam("instanceId") String instanceId) {
        WfTaskProcess wfTaskProcess = projectTaskProcessService.getByInstanceId(instanceId);
        return R.ok(wfTaskProcess);
    }

    /**
     * 根据实例ID和类型查询任务流程
     */
    @InnerAuth
    @GetMapping("/getByInstanceIdAndType")
    public R<WfTaskProcess> getByInstanceIdAndType(@RequestParam("instanceId") String instanceId,
                                                   @RequestParam("type") String type) {
        WfTaskProcess wfTaskProcess = projectTaskProcessService.getByInstanceIdAndType(instanceId, type);
        return R.ok(wfTaskProcess);
    }

    /**
     * 根据类型查询任务流程列表
     */
    @InnerAuth
    @GetMapping("/listByType")
    public R<List<WfTaskProcess>> listByType(@RequestParam("type") String type) {
        List<WfTaskProcess> list = projectTaskProcessService.listByType(type);
        return R.ok(list);
    }

    /**
     * 根据业务ID集合查询任务流程
     */
    @InnerAuth
    @PostMapping("/listByExtraIds")
    public R<List<WfTaskProcess>> listByExtraIds(@RequestBody List<String> extraIds) {
        List<WfTaskProcess> list = projectTaskProcessService.listByExtraIds(extraIds);
        return R.ok(list);
    }

    /**
     * 根据业务ID集合和类型查询任务流程
     */
    @InnerAuth
    @PostMapping("/listByExtraIdsAndType")
    public R<List<WfTaskProcess>> listByExtraIdsAndType(@RequestBody Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return R.ok(java.util.Collections.emptyList());
        }
        Object extraIdsObj = params.get("extraIds");
        Object typeObj = params.get("type");
        List<String> extraIds = extraIdsObj instanceof List
                ? ((List<?>) extraIdsObj).stream().map(String::valueOf).collect(Collectors.toList())
                : java.util.Collections.emptyList();
        String type = typeObj != null ? typeObj.toString() : null;
        List<WfTaskProcess> list = projectTaskProcessService.listByExtraIdsAndType(extraIds, type);
        return R.ok(list);
    }

    /**
     * 根据主键ID删除任务流程
     */
    @InnerAuth
    @DeleteMapping("/{id}")
    public R<Void> deleteById(@PathVariable("id") String id) {
        boolean removed = projectTaskProcessService.deleteById(id);
        return removed ? R.ok() : R.fail("删除任务流程失败");
    }

    /**
     * 清除任务流程关联
     */
    @InnerAuth
    @PostMapping("/clearAssociation")
    public R<WfTaskProcess> clearAssociation(@RequestParam("extraId") String extraId,
                                             @RequestParam("type") String type,
                                             @RequestParam(value = "clearUrl", defaultValue = "false") boolean clearUrl) {
        WfTaskProcess wfTaskProcess = projectTaskProcessService.clearAssociation(extraId, type, clearUrl);
        return R.ok(wfTaskProcess);
    }

    /**
     * 更新定义ID和部署ID
     */
    @InnerAuth
    @PostMapping("/updateDefinitionByPrefix")
    public R<Void> updateDefinitionByPrefix(@RequestBody Map<String, String> params) {
        String definitionIdPrefix = params.get("definitionIdPrefix");
        String newDefinitionId = params.get("newDefinitionId");
        String newDeploymentId = params.get("newDeploymentId");
        projectTaskProcessService.updateDefinitionByPrefix(definitionIdPrefix, newDefinitionId, newDeploymentId);
        return R.ok();
    }
}

