package com.bilibili.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@ComponentScan("com.bilibili")
@Import({JdbcConfig.class, MybatisPlusConfig.class})
@EnableTransactionManagement
@EnableAspectJAutoProxy
public class AppConfig {
}
