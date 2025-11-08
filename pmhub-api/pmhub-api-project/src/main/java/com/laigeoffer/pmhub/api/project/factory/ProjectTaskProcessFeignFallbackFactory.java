package com.laigeoffer.pmhub.api.project.factory;

import com.laigeoffer.pmhub.api.project.ProjectTaskProcessFeignService;
import com.laigeoffer.pmhub.base.core.core.domain.R;
import com.laigeoffer.pmhub.base.core.core.domain.entity.WfTaskProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 项目任务流程服务降级处理
 *
 * @author canghe
 */
@Component
public class ProjectTaskProcessFeignFallbackFactory implements FallbackFactory<ProjectTaskProcessFeignService> {
    private static final Logger log = LoggerFactory.getLogger(ProjectTaskProcessFeignFallbackFactory.class);

    @Override
    public ProjectTaskProcessFeignService create(Throwable throwable) {
        log.error("项目任务流程服务调用失败:{}", throwable.getMessage());
        return new ProjectTaskProcessFeignService() {
            @Override
            public R<WfTaskProcess> getByExtraIdAndType(String extraId, String type, String source) {
                return R.fail("根据 extraId 和 type 查询任务流程失败:" + throwable.getMessage());
            }

            @Override
            public R<WfTaskProcess> insertOrUpdate(WfTaskProcess wfTaskProcess, String source) {
                return R.fail("插入或更新任务流程失败:" + throwable.getMessage());
            }

            @Override
            public R<Void> updateTaskStatus3(String extraId, String source) {
                return R.fail("更新任务状态失败:" + throwable.getMessage());
            }

            @Override
            public R<Void> updateTaskStatus(String extraId, String source) {
                return R.fail("更新任务状态失败:" + throwable.getMessage());
            }

            @Override
            public R<Void> updateApproved(String extraId, String type, String approved, String source) {
                return R.fail("更新 approved 字段失败:" + throwable.getMessage());
            }

            @Override
            public R<Integer> getTaskExecuteStatus(String taskId, String source) {
                return R.fail("查询任务执行状态失败:" + throwable.getMessage());
            }
        };
    }
}

