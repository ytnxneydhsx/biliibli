package com.bilibili.im.privacy.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;

public interface UserPrivacySettingMapper {

    @Select("""
            SELECT private_message_policy
            FROM user_privacy_setting
            WHERE user_id = #{userId}
            LIMIT 1
            """)
    Integer selectPrivateMessagePolicyByUserId(@Param("userId") Long userId);

    @Insert("""
            INSERT IGNORE INTO user_privacy_setting (user_id, private_message_policy)
            VALUES (#{userId}, #{privateMessagePolicy})
            """)
    int insertIgnoreDefaultPolicy(@Param("userId") Long userId,
                                  @Param("privateMessagePolicy") Integer privateMessagePolicy);

    @Insert("""
            INSERT INTO user_privacy_setting (user_id, private_message_policy)
            VALUES (#{userId}, #{privateMessagePolicy})
            ON DUPLICATE KEY UPDATE
                private_message_policy = VALUES(private_message_policy),
                update_time = CURRENT_TIMESTAMP
            """)
    int upsertPrivateMessagePolicy(@Param("userId") Long userId,
                                   @Param("privateMessagePolicy") Integer privateMessagePolicy);
}
