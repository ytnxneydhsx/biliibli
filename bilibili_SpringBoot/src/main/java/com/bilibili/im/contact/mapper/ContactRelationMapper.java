package com.bilibili.im.contact.mapper;

import com.bilibili.im.contact.model.entity.ContactRelationDO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface ContactRelationMapper {

    @Select("""
            SELECT
                user_id AS userId,
                target_user_id AS targetUserId,
                is_contact AS isContact,
                is_blocked AS isBlocked,
                is_muted AS isMuted
            FROM contact_relation
            WHERE user_id = #{userId}
              AND target_user_id = #{targetUserId}
            LIMIT 1
            """)
    ContactRelationDO selectByUserIdAndTargetUserId(@Param("userId") Long userId,
                                                    @Param("targetUserId") Long targetUserId);
}
