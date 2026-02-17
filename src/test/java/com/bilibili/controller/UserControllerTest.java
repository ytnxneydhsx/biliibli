package com.bilibili.controller;

import com.bilibili.common.exception.GlobalExceptionHandler;
import com.bilibili.model.dto.UserLoginDTO;
import com.bilibili.model.dto.UserProfileUpdateDTO;
import com.bilibili.model.dto.UserRegisterDTO;
import com.bilibili.model.vo.UserLoginVO;
import com.bilibili.model.vo.UserProfileVO;
import com.bilibili.service.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(MockitoJUnitRunner.class)
public class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Before
    public void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    public void loginSuccess_shouldReturnOkResult() throws Exception {
        UserLoginVO user = new UserLoginVO();
        user.setUid(1001L);
        user.setUsername("tom");
        when(userService.login(any(UserLoginDTO.class))).thenReturn(user);

        UserLoginDTO dto = new UserLoginDTO();
        dto.setUsername("tom");
        dto.setPassword("123456");

        MvcResult mvcResult = mockMvc.perform(post("/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = readBody(mvcResult);
        assertEquals(0, root.get("code").asInt());
        assertEquals("OK", root.get("message").asText());
        assertEquals(1001L, root.get("data").get("uid").asLong());
        assertEquals("tom", root.get("data").get("username").asText());
    }

    @Test
    public void registerSuccess_shouldReturnUid() throws Exception {
        when(userService.register(any(UserRegisterDTO.class))).thenReturn(1001L);

        UserRegisterDTO dto = new UserRegisterDTO();
        dto.setUsername("tom");
        dto.setNickname("Tom");
        dto.setPassword("123456");
        dto.setConfirmPassword("123456");

        MvcResult mvcResult = mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = readBody(mvcResult);
        assertEquals(0, root.get("code").asInt());
        assertEquals(1001L, root.get("data").asLong());
    }

    @Test
    public void getPublicProfileSuccess_shouldReturnProfile() throws Exception {
        UserProfileVO vo = new UserProfileVO();
        vo.setUid(1001L);
        vo.setNickname("Tom");
        vo.setSign("hello");
        vo.setFollowerCount(10);
        vo.setFollowingCount(5);
        when(userService.getPublicProfile(eq(1001L))).thenReturn(vo);

        MvcResult mvcResult = mockMvc.perform(get("/users/1001"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = readBody(mvcResult);
        assertEquals(0, root.get("code").asInt());
        assertEquals(1001L, root.get("data").get("uid").asLong());
        assertEquals("Tom", root.get("data").get("nickname").asText());
        assertEquals("hello", root.get("data").get("sign").asText());
    }

    @Test
    public void updatePublicProfileSuccess_shouldReturnOk() throws Exception {
        UserProfileUpdateDTO dto = new UserProfileUpdateDTO();
        dto.setNickname("Tom2");
        dto.setSign("new sign");

        MvcResult mvcResult = mockMvc.perform(put("/users/1001/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = readBody(mvcResult);
        assertEquals(0, root.get("code").asInt());
        assertEquals("OK", root.get("message").asText());
    }

    @Test
    public void loginBadRequest_shouldMapTo400() throws Exception {
        doThrow(new IllegalArgumentException("username/password cannot be blank"))
                .when(userService).login(any(UserLoginDTO.class));

        UserLoginDTO dto = new UserLoginDTO();
        dto.setUsername(" ");
        dto.setPassword(" ");

        MvcResult mvcResult = mockMvc.perform(post("/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andReturn();

        JsonNode root = readBody(mvcResult);
        assertEquals(400, root.get("code").asInt());
        assertEquals("username/password cannot be blank", root.get("message").asText());
    }

    private JsonNode readBody(MvcResult mvcResult) throws Exception {
        String body = mvcResult.getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }
}
