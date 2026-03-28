package com.bilibili.im.message.mapper;

import com.bilibili.im.message.model.entity.ChatMessageDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface ChatMessageMapper {

    @Select("""
            SELECT
                id,
                conversation_id AS conversationId,
                sender_id AS senderId,
                receiver_id AS receiverId,
                client_message_id AS clientMessageId,
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
                conversation_id,
                sender_id,
                receiver_id,
                client_message_id,
                message_type,
                content,
                send_time,
                status
            ) VALUES (
                #{conversationId},
                #{senderId},
                #{receiverId},
                #{clientMessageId},
                #{messageType},
                #{content},
                #{sendTime},
                #{status}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ChatMessageDO message);
}
