package com.bilibili.config;

import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * MyBatis-Plus 配置类 (负责 Session 工厂的注册)
 */
@Configuration
@MapperScan("com.bilibili.mapper")
public class MybatisPlusConfig {

    /**
     * 注册 SqlSessionFactory
     * 注意：参数里的 dataSource 就是 Spring 自动从 JdbcConfig 里拿过来的那个“管子”
     */
    @Bean
    public MybatisSqlSessionFactoryBean sqlSessionFactory(DataSource dataSource) {
        MybatisSqlSessionFactoryBean factoryBean = new MybatisSqlSessionFactoryBean();
        // 关键一步：把地基（数据源）交给 MyBatis 工厂管理
        factoryBean.setDataSource(dataSource);
        return factoryBean;
    }
}
