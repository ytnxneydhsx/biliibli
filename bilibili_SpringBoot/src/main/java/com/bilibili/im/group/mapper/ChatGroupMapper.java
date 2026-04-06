package com.bilibili.im.group.mapper;

import com.bilibili.im.group.model.entity.ChatGroupDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

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

    @Update("""
            UPDATE chat_group
            SET member_count = #{memberCount},
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{groupId}
            """)
    int updateMemberCount(@Param("groupId") Long groupId,
                          @Param("memberCount") Integer memberCount);

    @Update("""
            UPDATE chat_group
            SET status = #{status},
                member_count = #{memberCount},
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{groupId}
            """)
    int updateGroupStatusAndMemberCount(@Param("groupId") Long groupId,
                                        @Param("status") Integer status,
                                        @Param("memberCount") Integer memberCount);

    @Update("""
            UPDATE chat_group
            SET group_avatar = #{groupAvatar},
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{groupId}
            """)
    int updateGroupAvatar(@Param("groupId") Long groupId,
                          @Param("groupAvatar") String groupAvatar);

    @Update("""
            UPDATE chat_group
            SET group_name = #{groupName},
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{groupId}
            """)
    int updateGroupName(@Param("groupId") Long groupId,
                        @Param("groupName") String groupName);

    @Update("""
            UPDATE chat_group
            SET is_all_muted = #{isMuted},
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{groupId}
            """)
    int updateGroupMuteStatus(@Param("groupId") Long groupId,
                              @Param("isMuted") Integer isMuted);
}
