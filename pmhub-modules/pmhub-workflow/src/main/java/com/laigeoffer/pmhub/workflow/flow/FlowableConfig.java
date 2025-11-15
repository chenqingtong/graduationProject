package com.laigeoffer.pmhub.workflow.flow;

import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.EngineConfigurationConfigurer;
// import org.springframework.context.annotation.Configuration;  // 已注释，未使用


/**
 * @author chenqingtong
 * @date 2021/4/5 01:32
 * 
 * 注意：由于已禁用 Flowable 自动配置，此配置类已注释
 * 如果后续需要使用 Flowable，请取消注释并确保数据库表存在
 */
// @Configuration
public class FlowableConfig implements EngineConfigurationConfigurer<SpringProcessEngineConfiguration> {

    @Override
    public void configure(SpringProcessEngineConfiguration engineConfiguration) {
        engineConfiguration.setActivityFontName("宋体");
        engineConfiguration.setLabelFontName("宋体");
        engineConfiguration.setAnnotationFontName("宋体");

    }
}

