package com.bilibili.im.privacy.cache.impl;

import com.bilibili.im.privacy.cache.UserPrivacyCacheNames;
import com.bilibili.im.privacy.cache.UserPrivacyPolicyCache;
import com.bilibili.im.privacy.mapper.UserPrivacySettingMapper;
import com.bilibili.im.privacy.model.enums.PrivateMessagePolicy;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
public class SpringUserPrivacyPolicyCache implements UserPrivacyPolicyCache {

    private final UserPrivacySettingMapper userPrivacySettingMapper;

    public SpringUserPrivacyPolicyCache(UserPrivacySettingMapper userPrivacySettingMapper) {
        this.userPrivacySettingMapper = userPrivacySettingMapper;
    }

    @Override
    @Cacheable(cacheNames = UserPrivacyCacheNames.USER_PRIVACY_POLICY, key = "#p0")
    public PrivateMessagePolicy get(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId is invalid");
        }
        Integer policyCode = userPrivacySettingMapper.selectPrivateMessagePolicyByUserId(userId);
        return PrivateMessagePolicy.fromCode(policyCode);
    }
}
