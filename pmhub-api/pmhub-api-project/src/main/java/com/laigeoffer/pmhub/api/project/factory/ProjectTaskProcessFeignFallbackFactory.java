package com.laigeoffer.pmhub.api.project.factory;

import com.laigeoffer.pmhub.api.project.ProjectTaskProcessFeignService;
import com.laigeoffer.pmhub.base.core.core.domain.R;
import com.laigeoffer.pmhub.base.core.core.domain.entity.WfTaskProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 项目任务流程服务降级处理
 *
 * @author chenqingtong
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
            public R<Void> resetTaskStatus(String extraId, String source) {
                return R.fail("重置任务状态失败:" + throwable.getMessage());
            }

            @Override
            public R<Void> updateApproved(String extraId, String type, String approved, String source) {
                return R.fail("更新 approved 字段失败:" + throwable.getMessage());
            }

            @Override
            public R<Integer> getTaskExecuteStatus(String taskId, String source) {
                return R.fail("查询任务执行状态失败:" + throwable.getMessage());
            }

            @Override
            public R<Integer> getTaskStatus(String taskId, String source) {
                return R.fail("查询任务状态失败:" + throwable.getMessage());
            }

            @Override
            public R<String> getTaskNameById(String taskId, String source) {
                return R.fail("查询任务名称失败:" + throwable.getMessage());
            }

            @Override
            public R<WfTaskProcess> getByInstanceId(String instanceId, String source) {
                return R.fail("根据实例ID查询任务流程失败:" + throwable.getMessage());
            }

            @Override
            public R<WfTaskProcess> getByInstanceIdAndType(String instanceId, String type, String source) {
                return R.fail("根据实例ID和类型查询任务流程失败:" + throwable.getMessage());
            }

            @Override
            public R<List<WfTaskProcess>> listByType(String type, String source) {
                return R.fail("根据类型查询任务流程列表失败:" + throwable.getMessage());
            }

            @Override
            public R<List<WfTaskProcess>> listByExtraIds(List<String> extraIds, String source) {
                return R.fail("根据业务ID集合查询任务流程失败:" + throwable.getMessage());
            }

            @Override
            public R<List<WfTaskProcess>> listByExtraIdsAndType(Map<String, Object> params, String source) {
                return R.fail("根据业务ID集合和类型查询任务流程失败:" + throwable.getMessage());
            }

            @Override
            public R<Void> deleteById(String id, String source) {
                return R.fail("删除任务流程失败:" + throwable.getMessage());
            }

            @Override
            public R<WfTaskProcess> clearAssociation(String extraId, String type, boolean clearUrl, String source) {
                return R.fail("清除任务流程关联失败:" + throwable.getMessage());
            }

            @Override
            public R<Void> updateDefinitionByPrefix(Map<String, String> params, String source) {
                return R.fail("根据定义ID前缀更新任务流程失败:" + throwable.getMessage());
            }
        };
    }
}

