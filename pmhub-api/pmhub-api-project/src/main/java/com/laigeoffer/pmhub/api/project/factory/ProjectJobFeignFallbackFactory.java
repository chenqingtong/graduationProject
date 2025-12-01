package com.laigeoffer.pmhub.api.project.factory;

import com.laigeoffer.pmhub.api.project.ProjectJobFeignService;
import com.laigeoffer.pmhub.base.core.core.domain.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 项目定时任务远程调用降级处理
 *
 * @author chenqingtong
 */
@Component
public class ProjectJobFeignFallbackFactory implements FallbackFactory<ProjectJobFeignService> {

    private static final Logger log = LoggerFactory.getLogger(ProjectJobFeignFallbackFactory.class);

    @Override
    public ProjectJobFeignService create(Throwable throwable) {
        log.error("ProjectJobFeignService 调用失败: {}", throwable.getMessage(), throwable);
        return new ProjectJobFeignService() {
            @Override
            public R<Void> runTaskNotify(String source) {
                return R.fail("任务待逾期提醒远程调用失败");
            }

            @Override
            public R<Void> runTaskOverdueNotify(String source) {
                return R.fail("任务已逾期提醒远程调用失败");
            }

            @Override
            public R<Void> runTaskOverdueStatus(String source) {
                return R.fail("任务已逾期状态刷新远程调用失败");
            }

            @Override
            public R<Void> runTaskOverdueWeekNotify(String source) {
                return R.fail("任务已逾期周提醒远程调用失败");
            }
        };
    }
}

