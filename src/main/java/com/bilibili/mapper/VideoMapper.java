package com.bilibili.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bilibili.model.entity.VideoDO;
import com.bilibili.model.vo.VideoVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface VideoMapper extends BaseMapper<VideoDO> {

    List<VideoVO> selectPublishedVideos(@Param("title") String title,
                                        @Param("offset") Integer offset,
                                        @Param("pageSize") Integer pageSize);

    List<VideoVO> selectMyPublishedVideos(@Param("uid") Long uid,
                                          @Param("title") String title,
                                          @Param("offset") Integer offset,
                                          @Param("pageSize") Integer pageSize);

    List<VideoVO> selectPublishedVideosByIds(@Param("ids") List<Long> ids);

    List<Long> selectPublishedVideoIdsByTitle(@Param("title") String title,
                                              @Param("limit") Integer limit);

    List<Long> selectPublishedVideoIdsByCategoryId(@Param("categoryId") Long categoryId,
                                                   @Param("limit") Integer limit);

    List<VideoVO> selectPublishedVideosByViewCount(@Param("offset") Integer offset,
                                                   @Param("pageSize") Integer pageSize);

    int updateViewCountByDelta(@Param("videoId") Long videoId,
                               @Param("delta") Long delta);
}
