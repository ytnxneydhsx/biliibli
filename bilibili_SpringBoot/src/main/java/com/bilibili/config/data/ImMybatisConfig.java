package com.bilibili.config.data;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;

@Configuration
@MapperScan(
        basePackages = {
                "com.bilibili.access.mapper",
                "com.bilibili.im.privacy.mapper",
                "com.bilibili.im.contact.mapper",
                "com.bilibili.im.conversation.mapper",
                "com.bilibili.im.message.mapper"
        },
        sqlSessionFactoryRef = "imSqlSessionFactory"
)
public class ImMybatisConfig {

    @Bean
    public SqlSessionFactory imSqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        Resource[] mapperXmlResources = new PathMatchingResourcePatternResolver()
                .getResources("classpath*:mapper/im/**/*.xml");
        factoryBean.setMapperLocations(mapperXmlResources);
        return factoryBean.getObject();
    }
}
