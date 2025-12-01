package com.laigeoffer.pmhub.project.controller;

import com.laigeoffer.pmhub.base.core.core.domain.R;
import com.laigeoffer.pmhub.base.security.annotation.InnerAuth;
import com.laigeoffer.pmhub.project.job.TaskNotifyJob;
import com.laigeoffer.pmhub.project.job.TaskOverdueNotifyJob;
import com.laigeoffer.pmhub.project.job.TaskOverdueStatusJob;
import com.laigeoffer.pmhub.project.job.TaskOverdueWeekNotifyJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 提供给内部模块的定时任务执行接口
 *
 * @author chenqingtong
 */
@Slf4j
@RestController
@RequestMapping("/project/job/inner")
@RequiredArgsConstructor
public class ProjectJobInnerController {

    private final TaskNotifyJob taskNotifyJob;
    private final TaskOverdueNotifyJob taskOverdueNotifyJob;
    private final TaskOverdueStatusJob taskOverdueStatusJob;
    private final TaskOverdueWeekNotifyJob taskOverdueWeekNotifyJob;

    @InnerAuth
    @PostMapping("/runTaskNotify")
    public R<Void> runTaskNotify() {
        log.info("内部调用：任务待逾期提醒开始");
        taskNotifyJob.taskNotify();
        log.info("内部调用：任务待逾期提醒结束");
        return R.ok("任务待逾期提醒执行成功");
    }

    @InnerAuth
    @PostMapping("/runTaskOverdueNotify")
    public R<Void> runTaskOverdueNotify() {
        log.info("内部调用：任务已逾期提醒开始");
        taskOverdueNotifyJob.taskNotify();
        log.info("内部调用：任务已逾期提醒结束");
        return R.ok("任务已逾期提醒执行成功");
    }

    @InnerAuth
    @PostMapping("/runTaskOverdueStatus")
    public R<Void> runTaskOverdueStatus() {
        log.info("内部调用：任务已逾期状态刷新开始");
        taskOverdueStatusJob.taskNotify();
        log.info("内部调用：任务已逾期状态刷新结束");
        return R.ok("任务已逾期状态刷新执行成功");
    }

    @InnerAuth
    @PostMapping("/runTaskOverdueWeekNotify")
    public R<Void> runTaskOverdueWeekNotify() {
        log.info("内部调用：任务已逾期周提醒开始");
        taskOverdueWeekNotifyJob.taskNotify();
        log.info("内部调用：任务已逾期周提醒结束");
        return R.ok("任务已逾期周提醒执行成功");
    }
}

