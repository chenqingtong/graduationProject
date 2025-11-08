package com.laigeoffer.pmhub.project.controller;

import com.laigeoffer.pmhub.base.core.annotation.Log;
import com.laigeoffer.pmhub.base.core.core.domain.R;
import com.laigeoffer.pmhub.base.core.core.domain.entity.WfTaskProcess;
import com.laigeoffer.pmhub.base.core.enums.BusinessType;
import com.laigeoffer.pmhub.base.core.utils.StringUtils;
import com.laigeoffer.pmhub.base.security.annotation.InnerAuth;
import com.laigeoffer.pmhub.project.service.ProjectTaskProcessService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 项目任务流程控制器
 *
 * @author canghe
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/project/taskProcess")
public class ProjectTaskProcessController {

    private final ProjectTaskProcessService projectTaskProcessService;

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
}

