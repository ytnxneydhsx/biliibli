package com.bilibili.im.conversation.mapper;

import com.bilibili.im.conversation.model.entity.ChatConversationDO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface ChatConversationMapper {

    ChatConversationDO selectByOwnerTargetAndType(@Param("ownerUserId") Long ownerUserId,
                                                  @Param("targetId") Long targetId,
                                                  @Param("type") Integer type);

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
                                        @Param("lastMessageTime") java.time.LocalDateTime lastMessageTime);

    @Update("""
            UPDATE chat_conversation
            SET conversation_id = #{conversationId},
                last_message = #{lastMessage},
                last_message_time = #{lastMessageTime},
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
                                          @Param("lastMessageTime") java.time.LocalDateTime lastMessageTime);

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
