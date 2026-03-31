package com.bilibili.im.message.mapper;

import com.bilibili.im.message.model.entity.ChatMessageDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface ChatMessageMapper {

    @Select("""
            SELECT
                id,
                server_message_id AS serverMessageId,
                conversation_id AS conversationId,
                sender_id AS senderId,
                receiver_id AS receiverId,
                client_message_id AS clientMessageId,
                sender_location AS senderLocation,
                message_type AS messageType,
                content,
                send_time AS sendTime,
                status,
                create_time AS createTime,
                update_time AS updateTime
            FROM chat_message
            WHERE sender_id = #{senderId}
              AND client_message_id = #{clientMessageId}
            LIMIT 1
            """)
    ChatMessageDO selectBySenderAndClientMessageId(@Param("senderId") Long senderId,
                                                   @Param("clientMessageId") Long clientMessageId);

    @Insert("""
            INSERT INTO chat_message (
                server_message_id,
                conversation_id,
                sender_id,
                receiver_id,
                client_message_id,
                sender_location,
                message_type,
                content,
                send_time,
                status
            ) VALUES (
                #{serverMessageId},
                #{conversationId},
                #{senderId},
                #{receiverId},
                #{clientMessageId},
                #{senderLocation},
                #{messageType},
                #{content},
                #{sendTime},
                #{status}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ChatMessageDO message);

    @Select("""
            <script>
            SELECT
                id,
                server_message_id AS serverMessageId,
                conversation_id AS conversationId,
                sender_id AS senderId,
                receiver_id AS receiverId,
                client_message_id AS clientMessageId,
                sender_location AS senderLocation,
                message_type AS messageType,
                content,
                send_time AS sendTime,
                status,
                create_time AS createTime,
                update_time AS updateTime
            FROM chat_message
            WHERE conversation_id = #{conversationId}
            <if test="beforeServerMessageId != null">
              AND server_message_id &lt; #{beforeServerMessageId}
            </if>
            ORDER BY server_message_id DESC
            LIMIT #{limit}
            </script>
            """)
    List<ChatMessageDO> selectHistoryByConversationId(@Param("conversationId") String conversationId,
                                                      @Param("beforeServerMessageId") Long beforeServerMessageId,
                                                      @Param("limit") Integer limit);
}
