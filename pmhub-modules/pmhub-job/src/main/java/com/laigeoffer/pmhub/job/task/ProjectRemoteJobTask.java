package com.laigeoffer.pmhub.job.task;

import com.laigeoffer.pmhub.api.project.ProjectJobFeignService;
import com.laigeoffer.pmhub.base.core.constant.SecurityConstants;
import com.laigeoffer.pmhub.base.core.core.domain.R;
import com.laigeoffer.pmhub.base.core.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * 通过 Feign 调度项目模块中的任务提醒逻辑
 *
 * @author chenqingtong
 */
@Slf4j
@Component("projectRemoteJobTask")
@RequiredArgsConstructor
public class ProjectRemoteJobTask {

    private final ProjectJobFeignService projectJobFeignService;

    /**
     * 任务待逾期提醒
     */
    public void runTaskNotify() {
        invoke(() -> projectJobFeignService.runTaskNotify(SecurityConstants.INNER), "任务待逾期提醒");
    }

    /**
     * 任务已逾期提醒
     */
    public void runTaskOverdueNotify() {
        invoke(() -> projectJobFeignService.runTaskOverdueNotify(SecurityConstants.INNER), "任务已逾期提醒");
    }

    /**
     * 任务已逾期状态刷新
     */
    public void runTaskOverdueStatus() {
        invoke(() -> projectJobFeignService.runTaskOverdueStatus(SecurityConstants.INNER), "任务已逾期状态刷新");
    }

    /**
     * 任务已逾期一周提醒
     */
    public void runTaskOverdueWeekNotify() {
        invoke(() -> projectJobFeignService.runTaskOverdueWeekNotify(SecurityConstants.INNER), "任务已逾期周提醒");
    }

    private void invoke(Supplier<R<Void>> supplier, String description) {
        R<Void> response = supplier.get();
        if (response == null) {
            throw new ServiceException(description + "远程调用无响应");
        }
        if (!R.isSuccess(response)) {
            throw new ServiceException(description + "执行失败: " + response.getMsg());
        }
        log.info("{}执行完成：{}", description, response.getMsg());
    }
}

