package com.bilibili.config.core;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.FilterType;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Configuration
@ComponentScan(
        basePackages = "com.bilibili",
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = {
                        Controller.class,
                        RestController.class,
                        ControllerAdvice.class,
                        RestControllerAdvice.class
                }),
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.bilibili\\.config\\.web\\..*")
        }
)
@EnableTransactionManagement
@EnableAspectJAutoProxy
@EnableScheduling
public class AppConfig {
}
