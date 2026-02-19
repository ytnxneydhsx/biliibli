package com.bilibili.controller;

import com.bilibili.common.exception.GlobalExceptionHandler;
import com.bilibili.model.vo.FollowersQueryVO;
import com.bilibili.service.FollowingService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(MockitoJUnitRunner.class)
public class FollowingControllerTest {

    @Mock
    private FollowingService followingService;

    @InjectMocks
    private FollowingController followingController;

    @Test
    public void followers_shouldReturnList() throws Exception {
        MockMvc mockMvc = buildMockMvc();
        when(followingService.followersQuery(eq(1001L))).thenReturn(mockList(2002L, "Tom"));

        MvcResult mvcResult = mockMvc.perform(get("/users/1001/followers"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = readBody(mvcResult);
        assertEquals(0, root.get("code").asInt());
        assertEquals(2002L, root.get("data").get(0).get("uid").asLong());
        assertEquals("Tom", root.get("data").get(0).get("nickname").asText());
    }

    @Test
    public void followings_shouldReturnList() throws Exception {
        MockMvc mockMvc = buildMockMvc();
        when(followingService.followingsQuery(eq(1001L))).thenReturn(mockList(3003L, "Jerry"));

        MvcResult mvcResult = mockMvc.perform(get("/users/1001/followings"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = readBody(mvcResult);
        assertEquals(0, root.get("code").asInt());
        assertEquals(3003L, root.get("data").get(0).get("uid").asLong());
        assertEquals("Jerry", root.get("data").get(0).get("nickname").asText());
    }

    @Test
    public void friends_shouldReturnList() throws Exception {
        MockMvc mockMvc = buildMockMvc();
        when(followingService.friendsQuery(eq(1001L))).thenReturn(mockList(4004L, "Alice"));

        MvcResult mvcResult = mockMvc.perform(get("/users/1001/friends"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = readBody(mvcResult);
        assertEquals(0, root.get("code").asInt());
        assertEquals(4004L, root.get("data").get(0).get("uid").asLong());
        assertEquals("Alice", root.get("data").get(0).get("nickname").asText());
    }

    private MockMvc buildMockMvc() {
        ObjectMapper objectMapper = new ObjectMapper();
        return MockMvcBuilders.standaloneSetup(followingController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    private JsonNode readBody(MvcResult mvcResult) throws Exception {
        return new ObjectMapper().readTree(mvcResult.getResponse().getContentAsString());
    }

    private List<FollowersQueryVO> mockList(Long uid, String nickname) {
        FollowersQueryVO vo = new FollowersQueryVO();
        vo.setUid(uid);
        vo.setNickname(nickname);
        vo.setAvatar("http://localhost/avatar.png");
        vo.setSign("sign");
        return Collections.singletonList(vo);
    }
}

