package com.bilibili.im.group.mapper;

import com.bilibili.im.group.model.entity.ChatGroupDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface ChatGroupMapper {

    @Insert("""
            INSERT INTO chat_group (
                group_name,
                owner_user_id,
                group_avatar,
                status,
                member_count,
                is_all_muted,
                last_message,
                last_message_time,
                last_server_message_id,
                last_message_seq
            ) VALUES (
                #{groupName},
                #{ownerUserId},
                #{groupAvatar},
                #{status},
                #{memberCount},
                #{isAllMuted},
                #{lastMessage},
                #{lastMessageTime},
                #{lastServerMessageId},
                #{lastMessageSeq}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ChatGroupDO group);

    @Select("""
            SELECT
                id,
                group_name AS groupName,
                owner_user_id AS ownerUserId,
                group_avatar AS groupAvatar,
                status,
                member_count AS memberCount,
                is_all_muted AS isAllMuted,
                last_message AS lastMessage,
                last_message_time AS lastMessageTime,
                last_server_message_id AS lastServerMessageId,
                last_message_seq AS lastMessageSeq,
                create_time AS createTime,
                update_time AS updateTime
            FROM chat_group
            WHERE id = #{groupId}
            LIMIT 1
            """)
    ChatGroupDO selectById(@Param("groupId") Long groupId);
}
