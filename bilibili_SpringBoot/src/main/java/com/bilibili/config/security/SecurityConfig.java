package com.bilibili.config.security;

import com.bilibili.security.JwtAuthenticationFilter;
import com.bilibili.security.RequestLoggingFilter;
import com.bilibili.security.RestAccessDeniedHandler;
import com.bilibili.security.RestAuthenticationEntryPoint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.SecurityContextHolderFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] DOC_PATHS = {
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/v3/api-docs.yaml"
    };

    private static final String[] PUBLIC_PATHS = {
            "/users/login",
            "/users/register"
    };

    private static final String[] PUBLIC_GET_PATHS = {
            "/videos",
            "/videos/*",
            "/videos/*/comments",
            "/search/**",
            "/users/*",
            "/users/*/videos",
            "/users/*/followers",
            "/users/*/followings",
            "/users/*/friends"
    };

    private static final String[] PUBLIC_POST_PATHS = {
            "/videos/*/views"
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final RestAccessDeniedHandler restAccessDeniedHandler;
    private final RequestLoggingFilter requestLoggingFilter;
    private final boolean docsPublic;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          RestAuthenticationEntryPoint restAuthenticationEntryPoint,
                          RestAccessDeniedHandler restAccessDeniedHandler,
                          RequestLoggingFilter requestLoggingFilter,
                          @Value("${app.docs.public:false}") boolean docsPublic) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.restAuthenticationEntryPoint = restAuthenticationEntryPoint;
        this.restAccessDeniedHandler = restAccessDeniedHandler;
        this.requestLoggingFilter = requestLoggingFilter;
        this.docsPublic = docsPublic;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(restAuthenticationEntryPoint)
                        .accessDeniedHandler(restAccessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> {
                    if (docsPublic) {
                        auth.requestMatchers(DOC_PATHS).permitAll();
                    }
                    auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
                    auth.requestMatchers(PUBLIC_PATHS).permitAll();
                    auth.requestMatchers(HttpMethod.GET, PUBLIC_GET_PATHS).permitAll();
                    auth.requestMatchers(HttpMethod.POST, PUBLIC_POST_PATHS).permitAll();
                    auth.anyRequest().authenticated();
                })
                .addFilterAfter(requestLoggingFilter, SecurityContextHolderFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
