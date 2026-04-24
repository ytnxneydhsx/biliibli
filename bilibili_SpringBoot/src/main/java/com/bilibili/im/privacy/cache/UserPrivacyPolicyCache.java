package com.bilibili.im.privacy.cache;

import com.bilibili.im.privacy.model.enums.PrivateMessagePolicy;

public interface UserPrivacyPolicyCache {

    PrivateMessagePolicy get(Long userId);
}
