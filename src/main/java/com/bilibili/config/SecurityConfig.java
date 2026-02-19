package com.bilibili.config;

import com.bilibili.config.security.SecurityRuleContributor;
import com.bilibili.security.JwtAuthenticationFilter;
import com.bilibili.security.RequestLoggingFilter;
import com.bilibili.security.RestAccessDeniedHandler;
import com.bilibili.security.RestAuthenticationEntryPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.context.SecurityContextPersistenceFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final RestAccessDeniedHandler restAccessDeniedHandler;
    private final RequestLoggingFilter requestLoggingFilter;
    private final List<SecurityRuleContributor> securityRuleContributors;

    @Autowired
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          RestAuthenticationEntryPoint restAuthenticationEntryPoint,
                          RestAccessDeniedHandler restAccessDeniedHandler,
                          RequestLoggingFilter requestLoggingFilter,
                          List<SecurityRuleContributor> securityRuleContributors) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.restAuthenticationEntryPoint = restAuthenticationEntryPoint;
        this.restAccessDeniedHandler = restAccessDeniedHandler;
        this.requestLoggingFilter = requestLoggingFilter;
        this.securityRuleContributors = securityRuleContributors;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry registry = http
                .csrf().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .exceptionHandling()
                .authenticationEntryPoint(restAuthenticationEntryPoint)
                .accessDeniedHandler(restAccessDeniedHandler)
                .and()
                .authorizeRequests();

        applyAccessRules(registry);


        http.addFilterAfter(requestLoggingFilter, SecurityContextPersistenceFilter.class);
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    }

    private void applyAccessRules(ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry registry) {
        List<SecurityRuleContributor> orderedContributors = new ArrayList<>(securityRuleContributors);
        AnnotationAwareOrderComparator.sort(orderedContributors);
        for (SecurityRuleContributor contributor : orderedContributors) {
            contributor.contribute(registry);
        }
        // Secure-by-default: any path not explicitly listed above is rejected.
        registry.anyRequest().denyAll();
    }
}
