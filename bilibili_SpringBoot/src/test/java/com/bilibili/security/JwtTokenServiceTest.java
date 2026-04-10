package com.bilibili.security;

import com.bilibili.common.auth.AuthenticatedUser;
import com.bilibili.common.enums.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtTokenServiceTest {

    @Test
    void generateAndParse_shouldRoundTripUidAndRoleCode() {
        JwtTokenService tokenService = new JwtTokenService();
        ReflectionTestUtils.setField(tokenService, "secret", "01234567890123456789012345678901");
        ReflectionTestUtils.setField(tokenService, "expireSeconds", 600L);
        tokenService.init();

        String token = tokenService.generateToken(1001L, UserRole.ADMIN);
        AuthenticatedUser user = tokenService.parse(token);

        assertEquals(1001L, user.getUid());
        assertEquals(UserRole.ADMIN, user.getRole());
        assertEquals(UserRole.ADMIN.code(), user.getRoleCode());
    }

    @Test
    void parse_shouldThrowForInvalidToken() {
        JwtTokenService tokenService = new JwtTokenService();
        ReflectionTestUtils.setField(tokenService, "secret", "01234567890123456789012345678901");
        ReflectionTestUtils.setField(tokenService, "expireSeconds", 600L);
        tokenService.init();

        assertThrows(Exception.class, () -> tokenService.parse("not-a-jwt-token"));
    }
}
