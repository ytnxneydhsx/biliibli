package com.bilibili.config.security;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer;
import org.springframework.stereotype.Component;

@Component
@Order(20)
public class UserRuleContributor implements SecurityRuleContributor {

    @Override
    public void contribute(ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry registry) {
        registry.antMatchers(ApiPaths.USER_ONLY).authenticated();
        registry.antMatchers(HttpMethod.POST, ApiPaths.USER_LOGOUT).authenticated();
    }
}

