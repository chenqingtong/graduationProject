package com.laigeoffer.pmhub.workflow.controller;

import com.laigeoffer.pmhub.base.core.core.domain.AjaxResult;
import com.laigeoffer.pmhub.base.security.annotation.RequiresPermissions;
import com.laigeoffer.pmhub.workflow.job.TodoRemindJob;
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
@RequestMapping("/workflow/job")
public class JobController {

    @Autowired
    private TodoRemindJob todoRemindJob;

    /**
     * 手动执行代办任务提醒
     */
    @RequiresPermissions("workflow:job:run")
    @PostMapping("/runTodoRemind")
    public AjaxResult runTodoRemind() {
        try {
            log.info("手动执行代办任务提醒开始");
            todoRemindJob.sayWord();
            log.info("手动执行代办任务提醒结束");
            return AjaxResult.success("代办任务提醒执行成功");
        } catch (Exception e) {
            log.error("手动执行代办任务提醒失败", e);
            return AjaxResult.error("执行失败：" + e.getMessage());
        }
    }
}

