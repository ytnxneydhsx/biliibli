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
                conversation_type AS conversationType,
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
                conversation_type,
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
                #{conversationType},
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
                conversation_type AS conversationType,
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

    @Select("""
            <script>
            SELECT
                m.id,
                m.server_message_id AS serverMessageId,
                m.conversation_id AS conversationId,
                m.conversation_type AS conversationType,
                m.sender_id AS senderId,
                m.receiver_id AS receiverId,
                m.client_message_id AS clientMessageId,
                m.sender_location AS senderLocation,
                m.message_type AS messageType,
                m.content,
                m.send_time AS sendTime,
                m.status,
                m.create_time AS createTime,
                m.update_time AS updateTime
            FROM chat_group_message gm
            INNER JOIN chat_message m ON m.id = gm.message_id
            WHERE gm.group_id = #{groupId}
            <if test="beforeServerMessageId != null">
              AND m.server_message_id &lt; #{beforeServerMessageId}
            </if>
            ORDER BY gm.group_message_seq DESC
            LIMIT #{limit}
            </script>
            """)
    List<ChatMessageDO> selectGroupHistoryByGroupId(@Param("groupId") Long groupId,
                                                    @Param("beforeServerMessageId") Long beforeServerMessageId,
                                                    @Param("limit") Integer limit);
}
