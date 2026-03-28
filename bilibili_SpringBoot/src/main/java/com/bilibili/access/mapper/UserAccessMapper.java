package com.bilibili.access.mapper;

import com.bilibili.access.model.entity.UserAccessDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;

public interface UserAccessMapper {

    @Select("""
            SELECT
                user_id AS userId,
                like_enabled AS likeEnabled,
                comment_enabled AS commentEnabled,
                im_message_send_enabled AS imMessageSendEnabled,
                video_upload_enabled AS videoUploadEnabled,
                profile_edit_enabled AS profileEditEnabled,
                create_time AS createTime,
                update_time AS updateTime
            FROM t_user_access
            WHERE user_id = #{userId}
            """)
    UserAccessDO selectByUserId(Long userId);

    @Insert("""
            INSERT INTO t_user_access (
                user_id,
                like_enabled,
                comment_enabled,
                video_upload_enabled
            ) VALUES (
                #{userId},
                0,
                0,
                0
            )
            ON DUPLICATE KEY UPDATE
                like_enabled = 0,
                comment_enabled = 0,
                video_upload_enabled = 0
            """)
    int upsertVideoBusinessBanned(Long userId);

    @Insert("""
            INSERT INTO t_user_access (
                user_id,
                im_message_send_enabled
            ) VALUES (
                #{userId},
                0
            )
            ON DUPLICATE KEY UPDATE
                im_message_send_enabled = 0
            """)
    int upsertImMessageSendBanned(Long userId);
}
