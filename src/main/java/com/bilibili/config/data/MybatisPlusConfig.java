package com.bilibili.config.data;

import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;

@Configuration
@MapperScan("com.bilibili.mapper")
public class MybatisPlusConfig {

    
    @Bean
    public MybatisSqlSessionFactoryBean sqlSessionFactory(DataSource dataSource) {
        MybatisSqlSessionFactoryBean factoryBean = new MybatisSqlSessionFactoryBean();
        
        factoryBean.setDataSource(dataSource);
        try {
            Resource[] mapperXmlResources = new PathMatchingResourcePatternResolver()
                    .getResources("classpath*:mapper/*.xml");
            factoryBean.setMapperLocations(mapperXmlResources);
        } catch (Exception e) {
            throw new IllegalStateException("load mybatis mapper xml failed", e);
        }
        return factoryBean;
    }
}
