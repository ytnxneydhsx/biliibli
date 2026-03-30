package com.bilibili.im.conversation.mapper;

import com.bilibili.im.conversation.model.entity.ChatConversationDO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface ChatConversationMapper {

    ChatConversationDO selectByOwnerTargetAndType(@Param("ownerUserId") Long ownerUserId,
                                                  @Param("targetId") Long targetId,
                                                  @Param("type") Integer type);

    @Select("""
            SELECT
                id,
                conversation_id,
                owner_user_id,
                target_id,
                type,
                last_message,
                last_message_time,
                last_server_message_id,
                unread_count,
                is_muted,
                create_time,
                update_time
            FROM chat_conversation
            WHERE owner_user_id = #{ownerUserId}
              AND type = #{type}
            ORDER BY
                CASE WHEN last_message_time IS NULL THEN 1 ELSE 0 END ASC,
                last_message_time DESC,
                update_time DESC,
                id DESC
            LIMIT #{limit}
            """)
    List<ChatConversationDO> selectRecentByOwnerAndType(@Param("ownerUserId") Long ownerUserId,
                                                        @Param("type") Integer type,
                                                        @Param("limit") Integer limit);

    int insertIgnoreConversation(@Param("conversationId") String conversationId,
                                 @Param("ownerUserId") Long ownerUserId,
                                 @Param("targetId") Long targetId,
                                 @Param("type") Integer type,
                                 @Param("unreadCount") Integer unreadCount,
                                 @Param("isMuted") Integer isMuted);

    @Update("""
            UPDATE chat_conversation
            SET conversation_id = #{conversationId},
                last_message = #{lastMessage},
                last_message_time = #{lastMessageTime},
                last_server_message_id = #{lastServerMessageId},
                update_time = CURRENT_TIMESTAMP
            WHERE owner_user_id = #{ownerUserId}
              AND target_id = #{targetId}
              AND type = #{type}
            """)
    int updateSenderConversationSummary(@Param("conversationId") String conversationId,
                                        @Param("ownerUserId") Long ownerUserId,
                                        @Param("targetId") Long targetId,
                                        @Param("type") Integer type,
                                        @Param("lastMessage") String lastMessage,
                                        @Param("lastMessageTime") java.time.LocalDateTime lastMessageTime,
                                        @Param("lastServerMessageId") Long lastServerMessageId);

    @Update("""
            UPDATE chat_conversation
            SET conversation_id = #{conversationId},
                last_message = #{lastMessage},
                last_message_time = #{lastMessageTime},
                last_server_message_id = #{lastServerMessageId},
                unread_count = unread_count + 1,
                update_time = CURRENT_TIMESTAMP
            WHERE owner_user_id = #{ownerUserId}
              AND target_id = #{targetId}
              AND type = #{type}
            """)
    int updateReceiverConversationSummary(@Param("conversationId") String conversationId,
                                          @Param("ownerUserId") Long ownerUserId,
                                          @Param("targetId") Long targetId,
                                          @Param("type") Integer type,
                                          @Param("lastMessage") String lastMessage,
                                          @Param("lastMessageTime") java.time.LocalDateTime lastMessageTime,
                                          @Param("lastServerMessageId") Long lastServerMessageId);

    @Update("""
            UPDATE chat_conversation
            SET unread_count = 0,
                update_time = CURRENT_TIMESTAMP
            WHERE owner_user_id = #{ownerUserId}
              AND target_id = #{targetId}
              AND type = #{type}
            """)
    int resetUnreadCount(@Param("ownerUserId") Long ownerUserId,
                         @Param("targetId") Long targetId,
                         @Param("type") Integer type);
}
