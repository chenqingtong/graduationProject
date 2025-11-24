package com.laigeoffer.pmhub.workflow;

import com.laigeoffer.pmhub.base.security.annotation.EnableCustomConfig;
import com.laigeoffer.pmhub.base.security.annotation.EnableDistributedLock;
import com.laigeoffer.pmhub.base.security.annotation.EnablePmFeignClients;
import com.laigeoffer.pmhub.base.swagger.annotation.EnableCustomSwagger2;
import com.laigeoffer.pmhub.workflow.config.ProjectTaskProcessFeignConfig;
import org.flowable.spring.boot.EndpointAutoConfiguration;
import org.flowable.spring.boot.ProcessEngineAutoConfiguration;
import org.flowable.spring.boot.ProcessEngineServicesAutoConfiguration;
import org.flowable.spring.boot.eventregistry.EventRegistryServicesAutoConfiguration;
import org.flowable.spring.boot.form.FormEngineAutoConfiguration;
import org.flowable.spring.boot.content.ContentEngineAutoConfiguration;
import org.flowable.spring.boot.dmn.DmnEngineAutoConfiguration;
import org.flowable.spring.boot.idm.IdmEngineAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @author chenqingtong
 * @description 工作流服务
 * @create 2024-04-25-17:45
 */
@EnableCustomConfig
@EnablePmFeignClients(defaultConfiguration = ProjectTaskProcessFeignConfig.class)
@EnableCustomSwagger2
@EnableDistributedLock // 启用Redisson分布式锁
@EnableScheduling
@SpringBootApplication(exclude = {
    ProcessEngineAutoConfiguration.class,
    ProcessEngineServicesAutoConfiguration.class,
    EndpointAutoConfiguration.class,
    EventRegistryServicesAutoConfiguration.class,
    FormEngineAutoConfiguration.class,
    ContentEngineAutoConfiguration.class,
    DmnEngineAutoConfiguration.class,
    IdmEngineAutoConfiguration.class
})
public class PmHubWorkflowApplication {
    public static void main(String[] args) {
        SpringApplication.run(PmHubWorkflowApplication.class, args);
    }
}