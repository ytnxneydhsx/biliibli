package com.bilibili.config.security;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer;
import org.springframework.stereotype.Component;

@Component
@Order(10)
public class AnonymousRuleContributor implements SecurityRuleContributor {

    @Override
    public void contribute(ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry registry) {
        registry.antMatchers(ApiPaths.AUTH_FREE).permitAll();
        registry.antMatchers(HttpMethod.OPTIONS, "/**").permitAll();
        registry.antMatchers(HttpMethod.GET, ApiPaths.PUBLIC_GET).permitAll();
    }
}
