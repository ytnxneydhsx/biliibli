package com.bilibili.admin.mapper;

import com.bilibili.admin.model.vo.AdminPendingVideoVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface AdminVideoMapper {

    @Select("""
            <script>
            SELECT
                v.id,
                v.user_id AS author_uid,
                v.title,
                v.description,
                v.cover_url,
                v.video_url,
                v.duration,
                v.create_time,
                u.nickname
            FROM t_video v
            LEFT JOIN t_user_info u ON v.user_id = u.user_id
            WHERE v.status = #{status}
            <if test="cursor != null">
                AND v.id &lt; #{cursor}
            </if>
            ORDER BY v.id DESC
            LIMIT #{size}
            </script>
            """)
    List<AdminPendingVideoVO> selectVideosByCursor(@Param("status") int status,
                                                   @Param("cursor") Long cursor,
                                                   @Param("size") int size);

    @Update("""
            UPDATE t_video
            SET status = #{newStatus}
            WHERE id = #{videoId}
              AND status = #{oldStatus}
            """)
    int updateVideoStatus(@Param("videoId") Long videoId,
                          @Param("oldStatus") int oldStatus,
                          @Param("newStatus") int newStatus);
}
