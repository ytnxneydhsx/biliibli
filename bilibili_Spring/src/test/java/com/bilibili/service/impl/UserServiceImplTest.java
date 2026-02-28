package com.bilibili.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.bilibili.mapper.UserInfoMapper;
import com.bilibili.mapper.UserMapper;
import com.bilibili.model.dto.UserLoginDTO;
import com.bilibili.model.dto.UserProfileUpdateDTO;
import com.bilibili.model.dto.UserRegisterDTO;
import com.bilibili.model.entity.UserDO;
import com.bilibili.model.entity.UserInfoDO;
import com.bilibili.model.vo.UserLoginVO;
import com.bilibili.model.vo.UserProfileVO;
import com.bilibili.storage.StorageService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UserServiceImplTest {

    @BeforeClass
    public static void initMybatisPlusLambdaCache() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, UserDO.class);
        TableInfoHelper.initTableInfo(assistant, UserInfoDO.class);
    }

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserInfoMapper userInfoMapper;

    @Mock
    private StorageService storageService;

    @InjectMocks
    private UserServiceImpl userService;

    private UserRegisterDTO registerDTO;
    private UserLoginDTO loginDTO;

    @Before
    public void setUp() {
        registerDTO = new UserRegisterDTO();
        registerDTO.setUsername("tom");
        registerDTO.setNickname("Tom");
        registerDTO.setPassword("123456");
        registerDTO.setConfirmPassword("123456");

        loginDTO = new UserLoginDTO();
        loginDTO.setUsername("tom");
        loginDTO.setPassword("123456");
    }

    @Test
    public void registerSuccess_shouldInsertUserAndInfo() {
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(userMapper.insert(any(UserDO.class))).thenAnswer(invocation -> {
            UserDO user = invocation.getArgument(0);
            user.setId(1001L);
            return 1;
        });
        when(userInfoMapper.insert(any(UserInfoDO.class))).thenReturn(1);

        Long uid = userService.register(registerDTO);

        Assert.assertEquals(Long.valueOf(1001L), uid);
        verify(userMapper, times(1)).selectOne(any(LambdaQueryWrapper.class));
        verify(userMapper, times(1)).insert(any(UserDO.class));
        verify(userInfoMapper, times(1)).insert(any(UserInfoDO.class));
    }

    @Test
    public void registerDuplicateUsername_shouldThrowAndStop() {
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(new UserDO());

        IllegalArgumentException ex = Assert.assertThrows(
                IllegalArgumentException.class,
                () -> userService.register(registerDTO)
        );

        Assert.assertTrue(ex.getMessage().contains("username already exists"));
        verify(userMapper, times(1)).selectOne(any(LambdaQueryWrapper.class));
        verify(userMapper, never()).insert(any(UserDO.class));
        verify(userInfoMapper, never()).insert(any(UserInfoDO.class));
    }

    @Test
    public void loginSuccess_shouldHidePassword() {
        UserDO dbUser = new UserDO();
        dbUser.setId(1001L);
        dbUser.setUsername("tom");
        dbUser.setPassword("hashed");
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(dbUser);

        UserLoginVO result = userService.login(loginDTO);

        Assert.assertNotNull(result);
        Assert.assertEquals(Long.valueOf(1001L), result.getUid());
        Assert.assertEquals("tom", result.getUsername());
        verify(userMapper, times(1)).selectOne(any(LambdaQueryWrapper.class));
    }

    @Test
    public void loginFail_shouldThrow() {
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        IllegalArgumentException ex = Assert.assertThrows(
                IllegalArgumentException.class,
                () -> userService.login(loginDTO)
        );

        Assert.assertTrue(ex.getMessage().contains("username or password is incorrect"));
        verify(userMapper, times(1)).selectOne(any(LambdaQueryWrapper.class));
    }

    @Test
    public void getPublicProfileSuccess_shouldMapFields() {
        UserInfoDO userInfo = new UserInfoDO();
        userInfo.setUserId(1001L);
        userInfo.setNickname("Tom");
        userInfo.setAvatarUrl("https://a.com/avatar.png");
        userInfo.setSign("hello");
        userInfo.setFollowerCount(10);
        userInfo.setFollowingCount(5);
        when(userInfoMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(userInfo);

        UserProfileVO profile = userService.getPublicProfile(1001L);

        Assert.assertEquals(Long.valueOf(1001L), profile.getUid());
        Assert.assertEquals("Tom", profile.getNickname());
        Assert.assertEquals("https://a.com/avatar.png", profile.getAvatar());
        Assert.assertEquals("hello", profile.getSign());
        Assert.assertEquals(Integer.valueOf(10), profile.getFollowerCount());
        Assert.assertEquals(Integer.valueOf(5), profile.getFollowingCount());
        verify(userInfoMapper, times(1)).selectOne(any(LambdaQueryWrapper.class));
    }

    @Test
    public void updatePublicProfileSuccess_shouldUpdateOnce() {
        UserProfileUpdateDTO dto = new UserProfileUpdateDTO();
        dto.setNickname("Tom2");
        dto.setSign("new sign");
        when(userInfoMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        userService.updatePublicProfile(1001L, dto);

        verify(userInfoMapper, times(1)).update(isNull(), any(LambdaUpdateWrapper.class));
    }

    @Test
    public void updatePublicProfileNothingToUpdate_shouldThrow() {
        UserProfileUpdateDTO dto = new UserProfileUpdateDTO();
        dto.setNickname("   ");
        dto.setSign(" ");

        IllegalArgumentException ex = Assert.assertThrows(
                IllegalArgumentException.class,
                () -> userService.updatePublicProfile(1001L, dto)
        );

        Assert.assertTrue(ex.getMessage().contains("nothing to update"));
        verify(userInfoMapper, never()).update(isNull(), any(LambdaUpdateWrapper.class));
    }

    @Test
    public void updatePublicProfileUidNotFound_shouldThrow() {
        UserProfileUpdateDTO dto = new UserProfileUpdateDTO();
        dto.setNickname("Tom2");
        when(userInfoMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(0);

        IllegalArgumentException ex = Assert.assertThrows(
                IllegalArgumentException.class,
                () -> userService.updatePublicProfile(99999L, dto)
        );

        Assert.assertTrue(ex.getMessage().contains("user not found or no changes"));
        verify(userInfoMapper, times(1)).update(isNull(), any(LambdaUpdateWrapper.class));
    }
}
