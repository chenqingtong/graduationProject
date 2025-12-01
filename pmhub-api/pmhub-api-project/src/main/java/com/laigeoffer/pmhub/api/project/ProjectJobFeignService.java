package com.laigeoffer.pmhub.api.project;

import com.laigeoffer.pmhub.api.project.factory.ProjectJobFeignFallbackFactory;
import com.laigeoffer.pmhub.base.core.constant.SecurityConstants;
import com.laigeoffer.pmhub.base.core.constant.ServiceNameConstants;
import com.laigeoffer.pmhub.base.core.core.domain.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * 项目定时任务远程调用
 *
 * @author chenqingtong
 */
@FeignClient(contextId = "projectJobFeignService", value = ServiceNameConstants.PROJECT_SERVICE,
    fallbackFactory = ProjectJobFeignFallbackFactory.class)
public interface ProjectJobFeignService {

    /**
     * 任务待逾期提醒
     */
    @PostMapping("/project/job/inner/runTaskNotify")
    R<Void> runTaskNotify(@RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 任务已逾期提醒
     */
    @PostMapping("/project/job/inner/runTaskOverdueNotify")
    R<Void> runTaskOverdueNotify(@RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 任务已逾期状态刷新
     */
    @PostMapping("/project/job/inner/runTaskOverdueStatus")
    R<Void> runTaskOverdueStatus(@RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 任务已逾期一周提醒
     */
    @PostMapping("/project/job/inner/runTaskOverdueWeekNotify")
    R<Void> runTaskOverdueWeekNotify(@RequestHeader(SecurityConstants.FROM_SOURCE) String source);
}

