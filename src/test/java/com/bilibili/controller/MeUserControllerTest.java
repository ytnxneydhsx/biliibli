package com.bilibili.controller;

import com.bilibili.common.auth.AuthenticatedUser;
import com.bilibili.model.dto.UserProfileUpdateDTO;
import com.bilibili.service.UserService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mock.web.MockMultipartFile;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MeUserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private MeUserController meUserController;

    @Test
    public void updateMyProfile_shouldUseCurrentUserUid() {
        AuthenticatedUser currentUser = new AuthenticatedUser(1001L);
        UserProfileUpdateDTO dto = new UserProfileUpdateDTO();
        dto.setNickname("Tom2");
        dto.setSign("hello");

        meUserController.updateMyProfile(currentUser, dto);

        verify(userService, times(1)).updatePublicProfile(eq(1001L), eq(dto));
    }

    @Test
    public void uploadMyAvatar_shouldUseCurrentUserUidAndReturnUrl() {
        AuthenticatedUser currentUser = new AuthenticatedUser(1001L);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                new byte[]{1, 2, 3}
        );
        when(userService.uploadAvatar(eq(1001L), any())).thenReturn("http://localhost:9000/media/avatar/a.png");

        String url = meUserController.uploadMyAvatar(currentUser, file).getData();

        Assert.assertEquals("http://localhost:9000/media/avatar/a.png", url);
        verify(userService, times(1)).uploadAvatar(eq(1001L), any());
    }
}

