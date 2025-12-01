package com.laigeoffer.pmhub.project.controller;

import com.laigeoffer.pmhub.base.core.core.domain.AjaxResult;
import com.laigeoffer.pmhub.base.security.annotation.RequiresPermissions;
import com.laigeoffer.pmhub.project.job.TaskNotifyJob;
import com.laigeoffer.pmhub.project.job.TaskOverdueNotifyJob;
import com.laigeoffer.pmhub.project.job.TaskOverdueStatusJob;
import com.laigeoffer.pmhub.project.job.TaskOverdueWeekNotifyJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 定时任务手动执行控制器
 * 
 * @author chenqingtong
 * @date 2024-12-20
 */
@Slf4j
@RestController
@RequestMapping("/project/job")
public class JobController {

    @Autowired
    private TaskNotifyJob taskNotifyJob;
    
    @Autowired
    private TaskOverdueNotifyJob taskOverdueNotifyJob;
    
    @Autowired
    private TaskOverdueStatusJob taskOverdueStatusJob;
    
    @Autowired
    private TaskOverdueWeekNotifyJob taskOverdueWeekNotifyJob;

    /**
     * 手动执行任务待逾期提醒
     */
    @RequiresPermissions("project:job:run")
    @PostMapping("/runTaskNotify")
    public AjaxResult runTaskNotify() {
        try {
            log.info("手动执行任务待逾期提醒开始");
            taskNotifyJob.taskNotify();
            log.info("手动执行任务待逾期提醒结束");
            return AjaxResult.success("任务待逾期提醒执行成功");
        } catch (Exception e) {
            log.error("手动执行任务待逾期提醒失败", e);
            return AjaxResult.error("执行失败：" + e.getMessage());
        }
    }

    /**
     * 手动执行任务已逾期提醒
     */
    @RequiresPermissions("project:job:run")
    @PostMapping("/runTaskOverdueNotify")
    public AjaxResult runTaskOverdueNotify() {
        try {
            log.info("手动执行任务已逾期提醒开始");
            taskOverdueNotifyJob.taskNotify();
            log.info("手动执行任务已逾期提醒结束");
            return AjaxResult.success("任务已逾期提醒执行成功");
        } catch (Exception e) {
            log.error("手动执行任务已逾期提醒失败", e);
            return AjaxResult.error("执行失败：" + e.getMessage());
        }
    }

    /**
     * 手动执行任务已逾期状态修改
     */
    @RequiresPermissions("project:job:run")
    @PostMapping("/runTaskOverdueStatus")
    public AjaxResult runTaskOverdueStatus() {
        try {
            log.info("手动执行任务已逾期状态修改开始");
            taskOverdueStatusJob.taskNotify();
            log.info("手动执行任务已逾期状态修改结束");
            return AjaxResult.success("任务已逾期状态修改执行成功");
        } catch (Exception e) {
            log.error("手动执行任务已逾期状态修改失败", e);
            return AjaxResult.error("执行失败：" + e.getMessage());
        }
    }

    /**
     * 手动执行任务已逾期周提醒
     */
    @RequiresPermissions("project:job:run")
    @PostMapping("/runTaskOverdueWeekNotify")
    public AjaxResult runTaskOverdueWeekNotify() {
        try {
            log.info("手动执行任务已逾期周提醒开始");
            taskOverdueWeekNotifyJob.taskNotify();
            log.info("手动执行任务已逾期周提醒结束");
            return AjaxResult.success("任务已逾期周提醒执行成功");
        } catch (Exception e) {
            log.error("手动执行任务已逾期周提醒失败", e);
            return AjaxResult.error("执行失败：" + e.getMessage());
        }
    }
}

