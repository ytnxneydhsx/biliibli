package com.bilibili.im.privacy.service.impl;

import com.bilibili.im.privacy.cache.UserPrivacyCacheNames;
import com.bilibili.im.privacy.cache.UserPrivacyPolicyCache;
import com.bilibili.im.privacy.mapper.UserPrivacySettingMapper;
import com.bilibili.im.privacy.model.dto.UpdatePrivateMessagePolicyDTO;
import com.bilibili.im.privacy.model.enums.PrivateMessagePolicy;
import com.bilibili.im.privacy.model.vo.UserPrivacySettingVO;
import com.bilibili.im.privacy.service.UserPrivacyService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

@Service
public class UserPrivacyServiceImpl implements UserPrivacyService {

    private final UserPrivacySettingMapper userPrivacySettingMapper;
    private final UserPrivacyPolicyCache userPrivacyPolicyCache;

    public UserPrivacyServiceImpl(UserPrivacySettingMapper userPrivacySettingMapper,
                                  UserPrivacyPolicyCache userPrivacyPolicyCache) {
        this.userPrivacySettingMapper = userPrivacySettingMapper;
        this.userPrivacyPolicyCache = userPrivacyPolicyCache;
    }

    @Override
    @CacheEvict(cacheNames = UserPrivacyCacheNames.USER_PRIVACY_POLICY, key = "#p0")
    public void initializeDefaultPrivacySetting(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId is invalid");
        }
        userPrivacySettingMapper.insertIgnoreDefaultPolicy(userId, PrivateMessagePolicy.ALLOW_ALL.getCode());
    }

    @Override
    public PrivateMessagePolicy getPrivateMessagePolicy(Long userId) {
        return userPrivacyPolicyCache.get(userId);
    }

    @Override
    public UserPrivacySettingVO getPrivacySetting(Long userId) {
        PrivateMessagePolicy policy = getPrivateMessagePolicy(userId);
        UserPrivacySettingVO vo = new UserPrivacySettingVO();
        vo.setUserId(userId);
        vo.setPrivateMessagePolicy(policy.getCode());
        return vo;
    }

    @Override
    @CacheEvict(cacheNames = UserPrivacyCacheNames.USER_PRIVACY_POLICY, key = "#p0")
    public void updatePrivateMessagePolicy(Long userId, UpdatePrivateMessagePolicyDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("dto is invalid");
        }
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId is invalid");
        }
        if (!PrivateMessagePolicy.supports(dto.getPrivateMessagePolicy())) {
            throw new IllegalArgumentException("privateMessagePolicy is invalid");
        }
        PrivateMessagePolicy policy = PrivateMessagePolicy.fromCode(dto.getPrivateMessagePolicy());
        int rows = userPrivacySettingMapper.upsertPrivateMessagePolicy(userId, policy.getCode());
        if (rows <= 0) {
            throw new RuntimeException("update private message policy failed");
        }
    }
}
