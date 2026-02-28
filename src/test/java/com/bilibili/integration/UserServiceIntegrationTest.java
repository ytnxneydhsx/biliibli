package com.bilibili.integration;

import com.bilibili.config.data.JdbcConfig;
import com.bilibili.config.data.MybatisPlusConfig;
import com.bilibili.config.properties.StorageProperties;
import com.bilibili.model.dto.UserLoginDTO;
import com.bilibili.model.dto.UserProfileUpdateDTO;
import com.bilibili.model.dto.UserRegisterDTO;
import com.bilibili.model.vo.UserLoginVO;
import com.bilibili.model.vo.UserProfileVO;
import com.bilibili.service.UserService;
import com.bilibili.service.impl.UserServiceImpl;
import com.bilibili.storage.LocalStorageService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = UserServiceIntegrationTest.TestConfig.class)
@Transactional
public class UserServiceIntegrationTest {

    @Configuration
    @EnableTransactionManagement
    @Import({JdbcConfig.class, MybatisPlusConfig.class, StorageProperties.class})
    @ComponentScan(
            basePackageClasses = {UserServiceImpl.class, LocalStorageService.class},
            useDefaultFilters = false,
            includeFilters = @ComponentScan.Filter(
                    type = FilterType.ASSIGNABLE_TYPE,
                    classes = {UserServiceImpl.class, LocalStorageService.class}
            )
    )
    static class TestConfig {
    }

    @Autowired
    private UserService userService;

    @Test
    public void registerLoginAndProfileFlow_shouldWork() {
        String username = "it_user_" + System.currentTimeMillis();
        String nickname = "ITUser";
        String password = "123456";

        UserRegisterDTO registerDTO = new UserRegisterDTO();
        registerDTO.setUsername(username);
        registerDTO.setNickname(nickname);
        registerDTO.setPassword(password);
        registerDTO.setConfirmPassword(password);

        Long uid = userService.register(registerDTO);
        Assert.assertNotNull(uid);
        Assert.assertTrue(uid > 0);

        UserLoginDTO loginDTO = new UserLoginDTO();
        loginDTO.setUsername(username);
        loginDTO.setPassword(password);
        UserLoginVO loginUser = userService.login(loginDTO);
        Assert.assertNotNull(loginUser);
        Assert.assertEquals(uid, loginUser.getUid());
        Assert.assertEquals(username, loginUser.getUsername());

        UserProfileVO profileBefore = userService.getPublicProfile(uid);
        Assert.assertEquals(uid, profileBefore.getUid());
        Assert.assertEquals(nickname, profileBefore.getNickname());

        UserProfileUpdateDTO updateDTO = new UserProfileUpdateDTO();
        updateDTO.setNickname("ITUser2");
        updateDTO.setSign("integration test sign");
        userService.updatePublicProfile(uid, updateDTO);

        UserProfileVO profileAfter = userService.getPublicProfile(uid);
        Assert.assertEquals("ITUser2", profileAfter.getNickname());
        Assert.assertEquals("integration test sign", profileAfter.getSign());
    }
}
