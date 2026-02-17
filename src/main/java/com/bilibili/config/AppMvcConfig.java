package com.bilibili.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 配置类 (处理 Web 请求逻辑)
 */
@Configuration
@EnableWebMvc // 开启 MVC 核心功能 (比如 JSON 解析、参数绑定)
@ComponentScan("com.bilibili") //
public class AppMvcConfig implements WebMvcConfigurer {
    // 基础配置到这里就够了，以后如果需要处理静态资源（图片），我们再回来加代码
}