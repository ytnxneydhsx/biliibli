package com.bilibili.controller;

import com.bilibili.common.auth.AuthenticatedUser;
import com.bilibili.service.FollowingService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class MeFollowingControllerTest {

    @Mock
    private FollowingService followingService;

    @InjectMocks
    private MeFollowingController meFollowingController;

    @Test
    public void follow_shouldUseCurrentUserUid() {
        AuthenticatedUser currentUser = new AuthenticatedUser(1001L);

        meFollowingController.follow(currentUser, 2002L);

        verify(followingService, times(1)).follow(eq(1001L), eq(2002L));
    }

    @Test
    public void unfollow_shouldUseCurrentUserUid() {
        AuthenticatedUser currentUser = new AuthenticatedUser(1001L);

        meFollowingController.unfollow(currentUser, 2002L);

        verify(followingService, times(1)).unfollow(eq(1001L), eq(2002L));
    }
}

