package com.bilibili.im.contact.mapper;

import com.bilibili.im.contact.model.entity.ContactRelationDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface ContactRelationMapper {

    @Select("""
            SELECT
                user_id AS userId,
                target_user_id AS targetUserId,
                is_contact AS isContact,
                is_dm_contact AS isDmContact,
                is_blocked AS isBlocked,
                is_muted AS isMuted
            FROM contact_relation
            WHERE user_id = #{userId}
              AND target_user_id = #{targetUserId}
            LIMIT 1
            """)
    ContactRelationDO selectByUserIdAndTargetUserId(@Param("userId") Long userId,
                                                    @Param("targetUserId") Long targetUserId);

    @Insert("""
            INSERT INTO contact_relation (
                user_id,
                target_user_id,
                is_dm_contact
            ) VALUES (
                #{userId},
                #{targetUserId},
                1
            )
            ON DUPLICATE KEY UPDATE
                is_dm_contact = 1,
                update_time = CURRENT_TIMESTAMP
            """)
    int upsertDmContact(@Param("userId") Long userId,
                        @Param("targetUserId") Long targetUserId);
}
