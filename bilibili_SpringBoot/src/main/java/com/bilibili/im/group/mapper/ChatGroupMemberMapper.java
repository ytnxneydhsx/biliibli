package com.bilibili.im.group.mapper;

import com.bilibili.im.group.model.entity.ChatGroupMemberDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface ChatGroupMemberMapper {

    @Insert("""
            INSERT INTO chat_group_member (
                group_id,
                user_id,
                role,
                status,
                is_muted,
                last_read_seq
            ) VALUES (
                #{groupId},
                #{userId},
                #{role},
                #{status},
                #{isMuted},
                #{lastReadSeq}
            )
            ON DUPLICATE KEY UPDATE
                create_time = CURRENT_TIMESTAMP,
                role = VALUES(role),
                status = VALUES(status),
                is_muted = VALUES(is_muted),
                last_read_seq = VALUES(last_read_seq),
                update_time = CURRENT_TIMESTAMP
            """)
    int createOrReactivateMember(ChatGroupMemberDO member);

    @Select("""
            SELECT
                id,
                group_id AS groupId,
                user_id AS userId,
                role,
                status,
                is_muted AS isMuted,
                last_read_seq AS lastReadSeq,
                create_time AS createTime,
                update_time AS updateTime
            FROM chat_group_member
            WHERE group_id = #{groupId}
              AND user_id = #{userId}
            LIMIT 1
            """)
    ChatGroupMemberDO selectByGroupIdAndUserId(@Param("groupId") Long groupId,
                                               @Param("userId") Long userId);

    @Select("""
            SELECT
                id,
                group_id AS groupId,
                user_id AS userId,
                role,
                status,
                is_muted AS isMuted,
                last_read_seq AS lastReadSeq,
                create_time AS createTime,
                update_time AS updateTime
            FROM chat_group_member
            WHERE group_id = #{groupId}
              AND status = #{status}
            ORDER BY id ASC
            """)
    List<ChatGroupMemberDO> selectByGroupIdAndStatus(@Param("groupId") Long groupId,
                                                     @Param("status") Integer status);

    @Select("""
            SELECT COUNT(1)
            FROM chat_group_member
            WHERE group_id = #{groupId}
              AND status = #{status}
            """)
    int countByGroupIdAndStatus(@Param("groupId") Long groupId,
                                @Param("status") Integer status);

    @Update("""
            UPDATE chat_group_member
            SET status = #{status},
                update_time = CURRENT_TIMESTAMP
            WHERE group_id = #{groupId}
              AND user_id = #{userId}
            """)
    int updateMemberStatus(@Param("groupId") Long groupId,
                           @Param("userId") Long userId,
                           @Param("status") Integer status);

    @Update("""
            UPDATE chat_group_member
            SET status = #{status},
                update_time = CURRENT_TIMESTAMP
            WHERE group_id = #{groupId}
              AND status = #{fromStatus}
            """)
    int batchUpdateMemberStatusByGroupId(@Param("groupId") Long groupId,
                                         @Param("fromStatus") Integer fromStatus,
                                         @Param("status") Integer status);
}
