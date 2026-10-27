package com.mall.web;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Spring Boot 应用入口，同时启用定时任务和业务 Mapper 接口扫描。 */
@SpringBootApplication(scanBasePackages = "com.mall")
@EnableScheduling
@MapperScan("com.mall.**.mapper")
public class MallApplication {
    /** 启动 Spring 应用上下文。 */
    public static void main(String[] args) {
        SpringApplication.run(MallApplication.class, args);
    }
}
