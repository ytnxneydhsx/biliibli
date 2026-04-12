package com.bilibili.admin.mapper;

import org.apache.ibatis.annotations.Insert;

public interface AdminUserAccessMapper {

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
                like_enabled,
                comment_enabled,
                video_upload_enabled
            ) VALUES (
                #{userId},
                1,
                1,
                1
            )
            ON DUPLICATE KEY UPDATE
                like_enabled = 1,
                comment_enabled = 1,
                video_upload_enabled = 1
            """)
    int upsertVideoBusinessEnabled(Long userId);
}
