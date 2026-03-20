package com.bilibili.user.controller;

import com.bilibili.user.model.dto.UserLoginDTO;
import com.bilibili.user.model.vo.UserLoginVO;
import com.bilibili.user.model.vo.UserProfileVO;
import com.bilibili.security.JwtTokenService;
import com.bilibili.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc
class UserControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtTokenService jwtTokenService;

    @Test
    void login_shouldReturnTokenPayload() throws Exception {
        UserLoginVO loginVO = new UserLoginVO();
        loginVO.setUid(1001L);
        loginVO.setUsername("u1001");
        when(userService.login(any(UserLoginDTO.class))).thenReturn(loginVO);
        when(jwtTokenService.generateToken(1001L)).thenReturn("jwt-token-1001");

        mockMvc.perform(post("/users/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"u1001",
                                  "password":"11447"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data.uid").value(1001L))
                .andExpect(jsonPath("$.data.username").value("u1001"))
                .andExpect(jsonPath("$.data.token").value("jwt-token-1001"));

        verify(userService).login(any(UserLoginDTO.class));
        verify(jwtTokenService).generateToken(1001L);
    }

    @Test
    void getPublicProfile_shouldReturnUserProfile() throws Exception {
        UserProfileVO profile = new UserProfileVO();
        profile.setUid(1001L);
        profile.setNickname("tom");
        profile.setFollowerCount(12);
        profile.setFollowingCount(7);
        when(userService.getPublicProfile(1001L)).thenReturn(profile);

        mockMvc.perform(get("/users/1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.uid").value(1001L))
                .andExpect(jsonPath("$.data.nickname").value("tom"))
                .andExpect(jsonPath("$.data.followerCount").value(12))
                .andExpect(jsonPath("$.data.followingCount").value(7));

        verify(userService).getPublicProfile(1001L);
    }
}
