package com.bilibili.security;

import com.bilibili.authorization.AuthzService;
import com.bilibili.config.security.SecurityConfig;
import com.bilibili.controller.CommentController;
import com.bilibili.controller.FollowingController;
import com.bilibili.controller.MeCommentController;
import com.bilibili.controller.MeFollowingController;
import com.bilibili.controller.MeVideoLikeController;
import com.bilibili.controller.MeUserController;
import com.bilibili.controller.SearchController;
import com.bilibili.controller.UserController;
import com.bilibili.controller.VideoController;
import com.bilibili.service.CommentService;
import com.bilibili.model.vo.UserProfileVO;
import com.bilibili.service.FollowingService;
import com.bilibili.service.SearchService;
import com.bilibili.service.UserService;
import com.bilibili.service.VideoAppService;
import com.bilibili.service.VideoService;
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
import static org.mockito.ArgumentMatchers.any;
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
            RestAccessDeniedHandler.class
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
        public CommentService commentService() {
            return Mockito.mock(CommentService.class);
        }

        @Bean
        public VideoService videoService() {
            return Mockito.mock(VideoService.class);
        }

        @Bean
        public VideoAppService videoAppService() {
            return Mockito.mock(VideoAppService.class);
        }

        @Bean
        public SearchService searchService() {
            return Mockito.mock(SearchService.class);
        }

        @Bean(name = "authz")
        public AuthzService authzService() {
            AuthzService authz = Mockito.mock(AuthzService.class);
            when(authz.canDeleteComment(any(), any())).thenReturn(true);
            when(authz.canAccessUploadTask(any(), any())).thenReturn(true);
            return authz;
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

        @Bean
        public CommentController commentController(CommentService commentService) {
            return new CommentController(commentService);
        }

        @Bean
        public MeCommentController meCommentController(CommentService commentService) {
            return new MeCommentController(commentService);
        }

        @Bean
        public MeVideoLikeController meVideoLikeController(VideoService videoService) {
            return new MeVideoLikeController(videoService);
        }

        @Bean
        public VideoController videoController(VideoAppService videoAppService) {
            return new VideoController(videoAppService);
        }

        @Bean
        public SearchController searchController(SearchService searchService) {
            return new SearchController(searchService);
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
    public void unknownPath_shouldReturn401ByDefaultRule() throws Exception {
        mockMvc.perform(get("/not/exist/path"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void unknownPath_withToken_shouldReturn404() throws Exception {
        String token = jwtTokenService.generateToken(1001L);
        mockMvc.perform(get("/not/exist/path")
                        .header("Authorization", "Bearer " + token))
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
    public void videoViewWithoutToken_shouldBeAccessible() throws Exception {
        mockMvc.perform(post("/videos/1/views"))
                .andExpect(status().isOk());
    }

    @Test
    public void searchWithoutToken_shouldBeAccessible() throws Exception {
        mockMvc.perform(get("/search/videos")
                        .param("keyword", "java"))
                .andExpect(status().isOk());
    }

    @Test
    public void searchHistoryWithoutToken_shouldBeAccessible() throws Exception {
        mockMvc.perform(get("/search/videos/history"))
                .andExpect(status().isOk());
    }

    @Test
    public void videoViewWithToken_shouldNotReturn401() throws Exception {
        String token = jwtTokenService.generateToken(1001L);

        MvcResult mvcResult = mockMvc.perform(post("/videos/1/views")
                        .header("Authorization", "Bearer " + token))
                .andReturn();

        Assert.assertNotEquals(401, mvcResult.getResponse().getStatus());
    }

    @Test
    public void listCommentsWithoutToken_shouldBeAccessible() throws Exception {
        mockMvc.perform(get("/videos/1/comments"))
                .andExpect(status().isOk());
    }

    @Test
    public void createCommentWithoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(post("/me/videos/1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"hello\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void meVideoLikeWithoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(post("/me/videos/1/likes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void meVideoLikeWithToken_shouldNotReturn401() throws Exception {
        String token = jwtTokenService.generateToken(1001L);

        MvcResult mvcResult = mockMvc.perform(post("/me/videos/1/likes")
                        .header("Authorization", "Bearer " + token))
                .andReturn();

        Assert.assertNotEquals(401, mvcResult.getResponse().getStatus());
    }

    @Test
    public void userLogoutWithoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(post("/users/logout"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void userLogoutWithToken_shouldNotReturn401() throws Exception {
        String token = jwtTokenService.generateToken(1001L);

        MvcResult mvcResult = mockMvc.perform(post("/users/logout")
                        .header("Authorization", "Bearer " + token))
                .andReturn();

        Assert.assertNotEquals(401, mvcResult.getResponse().getStatus());
    }

    @Test
    public void likeCommentWithoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(post("/me/comments/1/likes"))
                .andExpect(status().isUnauthorized());
    }
}
