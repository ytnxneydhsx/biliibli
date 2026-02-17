package com.bilibili.config;

import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

public class WebInit extends AbstractAnnotationConfigDispatcherServletInitializer {

    // 指定 Spring 的根配置类
    @Override
    protected Class<?>[] getRootConfigClasses() {
        return new Class[]{AppConfig.class};
    }

    // 指定 Spring MVC 的配置类 (也就是你刚才写的 AppMvcConfig)
    @Override
    protected Class<?>[] getServletConfigClasses() {
        return new Class[]{AppMvcConfig.class};
    }

    // 哪些请求交给 Spring 处理？"/" 代表拦截所有请求
    @Override
    protected String[] getServletMappings() {
        return new String[]{"/"};
    }
}