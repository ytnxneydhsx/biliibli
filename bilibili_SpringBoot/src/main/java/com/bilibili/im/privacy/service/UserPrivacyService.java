package com.bilibili.im.privacy.service;

import com.bilibili.im.privacy.model.dto.UpdatePrivateMessagePolicyDTO;
import com.bilibili.im.privacy.model.enums.PrivateMessagePolicy;
import com.bilibili.im.privacy.model.vo.UserPrivacySettingVO;

public interface UserPrivacyService {

    void initializeDefaultPrivacySetting(Long userId);

    PrivateMessagePolicy getPrivateMessagePolicy(Long userId);

    UserPrivacySettingVO getPrivacySetting(Long userId);

    void updatePrivateMessagePolicy(Long userId, UpdatePrivateMessagePolicyDTO dto);
}
