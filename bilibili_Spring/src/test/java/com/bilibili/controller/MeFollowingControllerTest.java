package com.bilibili.controller;

import com.bilibili.common.exception.GlobalExceptionHandler;
import com.bilibili.controller.support.TestAuthenticatedUserArgumentResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import com.bilibili.service.FollowingService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(MockitoJUnitRunner.class)
public class MeFollowingControllerTest {

    @Mock
    private FollowingService followingService;

    @InjectMocks
    private MeFollowingController meFollowingController;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(meFollowingController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new TestAuthenticatedUserArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    public void follow_shouldUseCurrentUserUid() throws Exception {
        mockMvc.perform(post("/me/followings/2002")
                        .header(TestAuthenticatedUserArgumentResolver.UID_HEADER, "1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(followingService, times(1)).follow(eq(1001L), eq(2002L));
    }

    @Test
    public void unfollow_shouldUseCurrentUserUid() throws Exception {
        mockMvc.perform(delete("/me/followings/2002")
                        .header(TestAuthenticatedUserArgumentResolver.UID_HEADER, "1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(followingService, times(1)).unfollow(eq(1001L), eq(2002L));
    }
}
