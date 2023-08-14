package com.tct.itd.common.config;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @Description 日志框架初始化获取系统参数
 * @Author zhaoke
 * @Date 2021/12/4 11:48
 */
@Component
public class Log4j2InfoConfig implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    @Resource
    private Environment env;

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        //将服务名放入日志上下文中，用于日志文件拆分保存
        String appName = env.getProperty("spring.application.name");
        if (StringUtils.isNotBlank(appName)) {
            System.setProperty("appName", appName);
        }
    }
}
