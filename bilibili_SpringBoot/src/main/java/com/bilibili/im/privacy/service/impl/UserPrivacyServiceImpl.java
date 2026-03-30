package com.bilibili.im.privacy.service.impl;

import com.bilibili.im.privacy.mapper.UserPrivacySettingMapper;
import com.bilibili.im.privacy.model.dto.UpdatePrivateMessagePolicyDTO;
import com.bilibili.im.privacy.model.enums.PrivateMessagePolicy;
import com.bilibili.im.privacy.model.vo.UserPrivacySettingVO;
import com.bilibili.im.privacy.service.UserPrivacyService;
import org.springframework.stereotype.Service;

@Service
public class UserPrivacyServiceImpl implements UserPrivacyService {

    private final UserPrivacySettingMapper userPrivacySettingMapper;

    public UserPrivacyServiceImpl(UserPrivacySettingMapper userPrivacySettingMapper) {
        this.userPrivacySettingMapper = userPrivacySettingMapper;
    }

    @Override
    public void initializeDefaultPrivacySetting(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId is invalid");
        }
        userPrivacySettingMapper.insertIgnoreDefaultPolicy(userId, PrivateMessagePolicy.ALLOW_ALL.getCode());
    }

    @Override
    public PrivateMessagePolicy getPrivateMessagePolicy(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId is invalid");
        }
        Integer policyCode = userPrivacySettingMapper.selectPrivateMessagePolicyByUserId(userId);
        return PrivateMessagePolicy.fromCode(policyCode);
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
