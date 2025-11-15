package com.laigeoffer.pmhub.workflow.factory;

import lombok.Getter;
import org.flowable.engine.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
// import org.springframework.stereotype.Component;  // 已注释，未使用

/**
 * flowable 引擎注入封装
 * @author chenqingtong
 * @date 2021-04-03
 * 
 * 注意：由于已禁用 Flowable 自动配置，此类的 @Component 注解已注释
 * 如果后续需要使用 Flowable，请取消注释并确保数据库表存在
 */
// @Component
@Getter
public class FlowServiceFactory {

    @Autowired(required = false)
    protected RepositoryService repositoryService;

    @Autowired(required = false)
    protected RuntimeService runtimeService;

    @Autowired(required = false)
    protected IdentityService identityService;

    @Autowired(required = false)
    protected TaskService taskService;

    @Autowired(required = false)
    protected FormService formService;

    @Autowired(required = false)
    protected HistoryService historyService;

    @Autowired(required = false)
    protected ManagementService managementService;

    @Autowired(required = false)
    @Qualifier("processEngine")
    protected ProcessEngine processEngine;

}
