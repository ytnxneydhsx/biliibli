package com.bilibili.security;

import com.bilibili.common.auth.AuthenticatedUser;
import com.bilibili.common.enums.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenService {

    @Value("${jwt.secret:change-this-secret-at-least-32-bytes-long}")
    private String secret;

    @Value("${jwt.expireSeconds:604800}")
    private long expireSeconds;

    private Key key;

    @PostConstruct
    public void init() {
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.key = Keys.hmacShaKeyFor(secretBytes);
    }

    public String generateToken(Long uid) {
        return generateToken(uid, UserRole.defaultRole());
    }

    public String generateToken(Long uid, UserRole role) {
        Date now = new Date();
        Date expireAt = new Date(now.getTime() + expireSeconds * 1000L);
        UserRole normalizedRole = role == null ? UserRole.defaultRole() : role;
        return Jwts.builder()
                .setSubject(String.valueOf(uid))
                .claim(UserRole.ROLE_CODE_CLAIM, normalizedRole.code())
                .setIssuedAt(now)
                .setExpiration(expireAt)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public AuthenticatedUser parse(String token) {
        Jws<Claims> jws = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);

        Claims claims = jws.getBody();
        Long uid = Long.valueOf(claims.getSubject());
        Number roleCode = claims.get(UserRole.ROLE_CODE_CLAIM, Number.class);
        UserRole role = UserRole.fromCodeOrDefault(roleCode == null ? null : roleCode.intValue());
        return new AuthenticatedUser(uid, role);
    }
}
