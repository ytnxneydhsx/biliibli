package com.bilibili.im.conversation.mapper;

import com.bilibili.im.conversation.model.entity.ChatGroupConversationDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface ChatGroupConversationMapper {

    @Insert("""
            INSERT INTO chat_group_conversation (
                conversation_id,
                owner_user_id,
                group_id,
                status,
                is_muted
            ) VALUES (
                #{conversationId},
                #{ownerUserId},
                #{groupId},
                #{status},
                #{isMuted}
            )
            ON DUPLICATE KEY UPDATE
                conversation_id = VALUES(conversation_id),
                status = VALUES(status),
                update_time = CURRENT_TIMESTAMP
            """)
    int createOrShowConversation(ChatGroupConversationDO conversation);

    @Select("""
            SELECT
                id,
                conversation_id AS conversationId,
                owner_user_id AS ownerUserId,
                group_id AS groupId,
                status,
                is_muted AS isMuted,
                create_time AS createTime,
                update_time AS updateTime
            FROM chat_group_conversation
            WHERE owner_user_id = #{ownerUserId}
              AND group_id = #{groupId}
            LIMIT 1
            """)
    ChatGroupConversationDO selectByOwnerUserIdAndGroupId(@Param("ownerUserId") Long ownerUserId,
                                                          @Param("groupId") Long groupId);

    @Update("""
            UPDATE chat_group_conversation
            SET status = #{status},
                update_time = CURRENT_TIMESTAMP
            WHERE owner_user_id = #{ownerUserId}
              AND group_id = #{groupId}
            """)
    int updateConversationStatus(@Param("ownerUserId") Long ownerUserId,
                                 @Param("groupId") Long groupId,
                                 @Param("status") Integer status);

    @Update("""
            UPDATE chat_group_conversation
            SET status = #{status},
                update_time = CURRENT_TIMESTAMP
            WHERE group_id = #{groupId}
            """)
    int batchUpdateConversationStatusByGroupId(@Param("groupId") Long groupId,
                                               @Param("status") Integer status);
}
