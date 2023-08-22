package com.tct.itd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @author zhaoke
 * @Description 服务启动类
 * @Date 2020/8/11 14:03
 */
@EnableAsync
@EnableScheduling
@SpringBootApplication()
@EnableFeignClients()
public class ItdAsApplication {

    public static void main(String[] args) {
        SpringApplication springApplication = new SpringApplication(ItdAsApplication.class);
        ItdApplicationListener itdApplicationRunListener = new ItdApplicationListener();
        springApplication.addListeners(itdApplicationRunListener);
        springApplication.run(args);
    }
}
