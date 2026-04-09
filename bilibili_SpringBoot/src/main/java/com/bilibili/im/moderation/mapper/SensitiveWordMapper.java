package com.bilibili.im.moderation.mapper;

import com.bilibili.im.moderation.model.entity.SensitiveWordDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SensitiveWordMapper {

    @Insert("""
            INSERT INTO t_im_sensitive_word (
                word
            ) VALUES (
                #{word}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SensitiveWordDO sensitiveWord);

    @Select("""
            SELECT
                id,
                word,
                status,
                create_time AS createTime,
                update_time AS updateTime
            FROM t_im_sensitive_word
            ORDER BY id ASC
            """)
    List<SensitiveWordDO> selectAllOrderByIdAsc();

    @Select("""
            SELECT
                id,
                word,
                status,
                create_time AS createTime,
                update_time AS updateTime
            FROM t_im_sensitive_word
            WHERE status = 0
            ORDER BY id ASC
            """)
    List<SensitiveWordDO> selectActiveOrderByIdAsc();

    @Update("""
            UPDATE t_im_sensitive_word
            SET word = #{word},
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{id}
              AND status = 0
            """)
    int updateWordById(@Param("id") Long id, @Param("word") String word);

    @Update("""
            UPDATE t_im_sensitive_word
            SET status = 1,
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{id}
              AND status = 0
            """)
    int softDeleteById(Long id);
}
