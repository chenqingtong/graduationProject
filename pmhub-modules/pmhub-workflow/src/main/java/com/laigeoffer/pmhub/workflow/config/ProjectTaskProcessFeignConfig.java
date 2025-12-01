package com.laigeoffer.pmhub.workflow.config;

import feign.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 项目任务流程 Feign 配置
 *
 * @author
 */
@Configuration
public class ProjectTaskProcessFeignConfig {

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }
}

