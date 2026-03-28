package com.bilibili.access.authorization;

import com.bilibili.access.service.UserAccessService;
import com.bilibili.common.auth.AuthenticatedUser;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service("accessAuthz")
public class AccessAuthzService {

    private final UserAccessService userAccessService;

    public AccessAuthzService(UserAccessService userAccessService) {
        this.userAccessService = userAccessService;
    }

    public boolean canLike(Authentication authentication) {
        Long userId = resolveUid(authentication);
        return userId != null && userAccessService.canLike(userId);
    }

    public boolean canComment(Authentication authentication) {
        Long userId = resolveUid(authentication);
        return userId != null && userAccessService.canComment(userId);
    }

    public boolean canSendImMessage(Authentication authentication) {
        Long userId = resolveUid(authentication);
        return userId != null && userAccessService.canSendImMessage(userId);
    }

    public boolean canUploadVideo(Authentication authentication) {
        Long userId = resolveUid(authentication);
        return userId != null && userAccessService.canUploadVideo(userId);
    }

    public boolean canEditProfile(Authentication authentication) {
        Long userId = resolveUid(authentication);
        return userId != null && userAccessService.canEditProfile(userId);
    }

    private Long resolveUid(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthenticatedUser) {
            return ((AuthenticatedUser) principal).getUid();
        }
        return null;
    }
}
