package com.bilibili.security;

import com.bilibili.common.auth.AuthenticatedUser;
import com.bilibili.common.enums.UserRole;
import com.bilibili.security.resolver.AuthenticatedUserResolver;
import com.bilibili.security.resolver.TokenResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenResolver tokenResolver;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    @Autowired
    public JwtAuthenticationFilter(TokenResolver tokenResolver,
                                   AuthenticatedUserResolver authenticatedUserResolver) {
        this.tokenResolver = tokenResolver;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = tokenResolver.resolve(request);

        if (StringUtils.hasText(token)) {
            try {
                AuthenticatedUser principal = authenticatedUserResolver.resolve(token);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                resolveAuthorities(principal)
                        );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception ex) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    private Collection<SimpleGrantedAuthority> resolveAuthorities(AuthenticatedUser principal) {
        UserRole role = principal == null ? UserRole.USER : principal.getRole();
        if (role == null) {
            role = UserRole.USER;
        }

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

        if (UserRole.ADMIN.equals(role)) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }
        return authorities;
    }
}
