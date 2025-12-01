package com.sxsd.quartz.task;

import com.laigeoffer.pmhub.base.core.utils.spring.SpringUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * 任务已逾期状态修改任务
 * Quartz 调度器适配器类
 * 
 * 注意：由于 pmhub-job 和 pmhub-project 是独立的微服务应用，
 * 此任务的实际执行逻辑在 pmhub-project 模块的 TaskOverdueStatusJob 中通过 @Scheduled 执行。
 * 此适配器类主要用于避免 Quartz 调度器报错。
 * 
 * 如果需要在 Quartz 中执行相同逻辑，需要：
 * 1. 添加 pmhub-api-project 依赖
 * 2. 创建 Feign 接口调用项目服务
 * 3. 或者在此类中直接实现逻辑（需要添加项目相关依赖）
 *
 * @author chenqingtong
 * @date 2023-10-12
 */
@Slf4j
public class TaskOverdueStatusTask {

    /**
     * 任务已逾期状态修改
     * 
     * 注意：此方法目前仅用于避免 ClassNotFoundException 错误。
     * 实际的任务逾期状态修改逻辑在 pmhub-project 模块的 TaskOverdueStatusJob 中执行。
     */
    public void taskNotify() {
        try {
            log.info("TaskOverdueStatusTask 开始执行");
            // 尝试通过 Spring Bean 名称获取 TaskOverdueStatusJob 实例
            // 如果 Bean 不存在（因为 pmhub-job 和 pmhub-project 是独立的微服务），则记录日志
            try {
                Object job = SpringUtils.getBean("taskOverdueStatusJob");
                // 调用 taskNotify 方法
                job.getClass().getMethod("taskNotify").invoke(job);
                log.info("TaskOverdueStatusTask 通过 Bean 调用执行完成");
            } catch (org.springframework.beans.factory.NoSuchBeanDefinitionException e) {
                // Bean 不存在是正常的，因为 pmhub-job 和 pmhub-project 是独立的微服务
                log.warn("TaskOverdueStatusJob Bean 不存在，任务逾期状态修改逻辑由 pmhub-project 模块的 @Scheduled 任务执行");
                log.info("TaskOverdueStatusTask 执行完成（跳过，实际逻辑在 pmhub-project 模块执行）");
            }
        } catch (Exception e) {
            log.error("TaskOverdueStatusTask 执行异常", e);
            // 不抛出异常，避免影响其他 Quartz 任务
            log.warn("TaskOverdueStatusTask 执行失败，但不会影响其他任务。实际的任务逾期状态修改逻辑由 pmhub-project 模块的 @Scheduled 任务执行");
        }
    }
}

