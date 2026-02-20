package com.bilibili.security;

import com.bilibili.config.SecurityConfig;
import com.bilibili.config.security.AnonymousRuleContributor;
import com.bilibili.config.security.UserRuleContributor;
import com.bilibili.controller.FollowingController;
import com.bilibili.controller.MeFollowingController;
import com.bilibili.controller.MeUserController;
import com.bilibili.controller.UserController;
import com.bilibili.model.vo.UserProfileVO;
import com.bilibili.service.FollowingService;
import com.bilibili.service.UserService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = SecurityConfigIntegrationTest.TestConfig.class)
public class SecurityConfigIntegrationTest {

    @Configuration
    @EnableWebMvc
    @Import(SecurityConfig.class)
    @ComponentScan(basePackageClasses = {
            JwtAuthenticationFilter.class,
            JwtTokenService.class,
            RestAuthenticationEntryPoint.class,
            RestAccessDeniedHandler.class,
            AnonymousRuleContributor.class,
            UserRuleContributor.class
    })
    static class TestConfig {

        @Bean
        public UserService userService() {
            return Mockito.mock(UserService.class);
        }

        @Bean
        public FollowingService followingService() {
            return Mockito.mock(FollowingService.class);
        }

        @Bean
        public UserController userController(UserService userService, JwtTokenService jwtTokenService) {
            return new UserController(userService, jwtTokenService);
        }

        @Bean
        public FollowingController followingController(FollowingService followingService) {
            return new FollowingController(followingService);
        }

        @Bean
        public MeUserController meUserController(UserService userService) {
            return new MeUserController(userService);
        }

        @Bean
        public MeFollowingController meFollowingController(FollowingService followingService) {
            return new MeFollowingController(followingService);
        }
    }

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private FilterChainProxy springSecurityFilterChain;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtTokenService jwtTokenService;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilters(springSecurityFilterChain)
                .build();
    }

    @Test
    public void meProfileWithoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(put("/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"Tom2\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void publicProfileWithoutToken_shouldBeAccessible() throws Exception {
        UserProfileVO profile = new UserProfileVO();
        profile.setUid(1001L);
        profile.setNickname("Tom");
        when(userService.getPublicProfile(eq(1001L))).thenReturn(profile);

        mockMvc.perform(get("/users/1001"))
                .andExpect(status().isOk());
    }

    @Test
    public void meFollowingWithoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(post("/me/followings/2002"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void meFollowingWithToken_shouldNotReturn401() throws Exception {
        String token = jwtTokenService.generateToken(1001L);

        MvcResult mvcResult = mockMvc.perform(post("/me/followings/2002")
                        .header("Authorization", "Bearer " + token))
                .andReturn();

        // Controller and security should pass auth; any non-401 is acceptable here.
        Assert.assertNotEquals(401, mvcResult.getResponse().getStatus());
    }

    @Test
    public void unknownPath_shouldBeRejectedByDefaultRule() throws Exception {
        mockMvc.perform(get("/not/exist/path"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void corsPreflightForLogin_shouldReturn200() throws Exception {
        mockMvc.perform(options("/users/login")
                        .header("Origin", "http://localhost:63342")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Content-Type,Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:63342"));
    }

    @Test
    public void videoViewWithoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(post("/videos/1/views"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void videoViewWithToken_shouldNotReturn401() throws Exception {
        String token = jwtTokenService.generateToken(1001L);

        MvcResult mvcResult = mockMvc.perform(post("/videos/1/views")
                        .header("Authorization", "Bearer " + token))
                .andReturn();

        Assert.assertNotEquals(401, mvcResult.getResponse().getStatus());
    }
}
