package com.bilibili.security;

import com.bilibili.common.auth.AuthenticatedUser;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

public class JwtTokenServiceTest {

    @Test
    public void generateAndParse_shouldReturnSameUid() {
        JwtTokenService jwtTokenService = new JwtTokenService();
        ReflectionTestUtils.setField(jwtTokenService, "secret", "12345678901234567890123456789012");
        ReflectionTestUtils.setField(jwtTokenService, "expireSeconds", 3600L);
        jwtTokenService.init();

        String token = jwtTokenService.generateToken(1001L);
        AuthenticatedUser user = jwtTokenService.parse(token);

        Assert.assertNotNull(token);
        Assert.assertNotNull(user);
        Assert.assertEquals(Long.valueOf(1001L), user.getUid());
    }

    @Test
    public void parse_invalidToken_shouldThrow() {
        JwtTokenService jwtTokenService = new JwtTokenService();
        ReflectionTestUtils.setField(jwtTokenService, "secret", "12345678901234567890123456789012");
        ReflectionTestUtils.setField(jwtTokenService, "expireSeconds", 3600L);
        jwtTokenService.init();

        Assert.assertThrows(RuntimeException.class, () -> jwtTokenService.parse("invalid.token"));
    }
}

