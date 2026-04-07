package com.bilibili.im.conversation.mapper;

import com.bilibili.im.conversation.model.entity.ChatGroupConversationDO;
import com.bilibili.im.conversation.model.vo.GroupConversationWindowVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface ChatGroupConversationMapper {

    @Insert("""
            INSERT INTO chat_group_conversation (
                conversation_id,
                owner_user_id,
                group_id,
                status,
                is_muted,
                last_read_seq
            ) VALUES (
                #{conversationId},
                #{ownerUserId},
                #{groupId},
                #{status},
                #{isMuted},
                #{lastReadSeq}
            )
            ON DUPLICATE KEY UPDATE
                conversation_id = VALUES(conversation_id),
                status = VALUES(status),
                last_read_seq = VALUES(last_read_seq),
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
                last_read_seq AS lastReadSeq,
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
            SET last_read_seq = GREATEST(
                    COALESCE(last_read_seq, 0),
                    COALESCE((SELECT g.last_message_seq FROM chat_group g WHERE g.id = #{groupId}), 0)
                ),
                update_time = CURRENT_TIMESTAMP
            WHERE owner_user_id = #{ownerUserId}
              AND group_id = #{groupId}
            """)
    int advanceLastReadSeq(@Param("ownerUserId") Long ownerUserId,
                           @Param("groupId") Long groupId);

    @Update("""
            UPDATE chat_group_conversation
            SET status = #{status},
                update_time = CURRENT_TIMESTAMP
            WHERE group_id = #{groupId}
            """)
    int batchUpdateConversationStatusByGroupId(@Param("groupId") Long groupId,
                                               @Param("status") Integer status);

    @Select("""
            SELECT
                id,
                conversation_id AS conversationId,
                owner_user_id AS ownerUserId,
                group_id AS groupId,
                status,
                is_muted AS isMuted,
                last_read_seq AS lastReadSeq,
                create_time AS createTime,
                update_time AS updateTime
            FROM chat_group_conversation
            WHERE owner_user_id = #{ownerUserId}
              AND status = #{conversationStatus}
            ORDER BY update_time DESC, id DESC
            """)
    List<ChatGroupConversationDO> selectVisibleConversationsByOwnerUserId(@Param("ownerUserId") Long ownerUserId,
                                                                          @Param("conversationStatus") Integer conversationStatus);

    @Select("""
            SELECT
                gc.conversation_id AS conversationId,
                gc.group_id AS groupId,
                g.group_name AS groupName,
                g.group_avatar AS groupAvatar,
                g.status AS status,
                g.member_count AS memberCount,
                g.is_all_muted AS isAllMuted,
                g.last_message AS lastMessage,
                g.last_message_time AS lastMessageTime,
                g.last_server_message_id AS lastServerMessageId,
                g.last_message_seq AS lastMessageSeq,
                gc.last_read_seq AS lastReadSeq,
                GREATEST(g.last_message_seq - gc.last_read_seq, 0) AS unreadCount,
                gc.is_muted AS isMuted,
                COALESCE(g.last_message_time, gc.update_time) AS sortTime
            FROM chat_group_conversation gc
            INNER JOIN chat_group g ON g.id = gc.group_id
            WHERE gc.owner_user_id = #{ownerUserId}
              AND gc.status = #{conversationStatus}
              AND g.status = #{groupStatus}
            ORDER BY COALESCE(g.last_message_time, gc.update_time) DESC, gc.id DESC
            """)
    List<GroupConversationWindowVO> selectVisibleGroupWindowsByOwnerUserId(@Param("ownerUserId") Long ownerUserId,
                                                                           @Param("conversationStatus") Integer conversationStatus,
                                                                           @Param("groupStatus") Integer groupStatus);
}
