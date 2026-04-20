package com.bilibili.im.conversation.mapper;

import com.bilibili.im.conversation.model.entity.ChatConversationDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface ChatConversationMapper {

    ChatConversationDO selectByOwnerTargetAndType(@Param("ownerUserId") Long ownerUserId,
                                                  @Param("targetId") Long targetId,
                                                  @Param("type") Integer type);

    List<ChatConversationDO> selectRecentByOwnerAndType(@Param("ownerUserId") Long ownerUserId,
                                                        @Param("type") Integer type,
                                                        @Param("limit") Integer limit);

    int insertIgnoreConversation(@Param("conversationId") String conversationId,
                                 @Param("ownerUserId") Long ownerUserId,
                                 @Param("targetId") Long targetId,
                                 @Param("type") Integer type,
                                 @Param("unreadCount") Integer unreadCount,
                                 @Param("isMuted") Integer isMuted);

    @Insert("""
            INSERT INTO chat_conversation (
                conversation_id,
                owner_user_id,
                target_id,
                type,
                last_message,
                last_message_time,
                last_server_message_id,
                unread_count,
                is_muted
            ) VALUES (
                #{conversationId},
                #{ownerUserId},
                #{targetId},
                #{type},
                #{lastMessage},
                #{lastMessageTime},
                #{lastServerMessageId},
                0,
                0
            )
            ON DUPLICATE KEY UPDATE
                last_message = CASE
                    WHEN last_server_message_id IS NULL OR last_server_message_id < #{lastServerMessageId}
                    THEN #{lastMessage}
                    ELSE last_message
                END,
                last_message_time = CASE
                    WHEN last_server_message_id IS NULL OR last_server_message_id < #{lastServerMessageId}
                    THEN #{lastMessageTime}
                    ELSE last_message_time
                END,
                last_server_message_id = CASE
                    WHEN last_server_message_id IS NULL OR last_server_message_id < #{lastServerMessageId}
                    THEN #{lastServerMessageId}
                    ELSE last_server_message_id
                END,
                update_time = CURRENT_TIMESTAMP
            """)
    int updateSenderConversationSummary(@Param("conversationId") String conversationId,
                                        @Param("ownerUserId") Long ownerUserId,
                                        @Param("targetId") Long targetId,
                                        @Param("type") Integer type,
                                        @Param("lastMessage") String lastMessage,
                                        @Param("lastMessageTime") java.time.LocalDateTime lastMessageTime,
                                        @Param("lastServerMessageId") Long lastServerMessageId);

    @Insert("""
            INSERT INTO chat_conversation (
                conversation_id,
                owner_user_id,
                target_id,
                type,
                last_message,
                last_message_time,
                last_server_message_id,
                unread_count,
                is_muted
            ) VALUES (
                #{conversationId},
                #{ownerUserId},
                #{targetId},
                #{type},
                #{lastMessage},
                #{lastMessageTime},
                #{lastServerMessageId},
                1,
                0
            )
            ON DUPLICATE KEY UPDATE
                unread_count = COALESCE(unread_count, 0) + 1,
                last_message = CASE
                    WHEN last_server_message_id IS NULL OR last_server_message_id < #{lastServerMessageId}
                    THEN #{lastMessage}
                    ELSE last_message
                END,
                last_message_time = CASE
                    WHEN last_server_message_id IS NULL OR last_server_message_id < #{lastServerMessageId}
                    THEN #{lastMessageTime}
                    ELSE last_message_time
                END,
                last_server_message_id = CASE
                    WHEN last_server_message_id IS NULL OR last_server_message_id < #{lastServerMessageId}
                    THEN #{lastServerMessageId}
                    ELSE last_server_message_id
                END,
                update_time = CURRENT_TIMESTAMP
            """)
    int upsertReceiverConversationSummary(@Param("conversationId") String conversationId,
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
