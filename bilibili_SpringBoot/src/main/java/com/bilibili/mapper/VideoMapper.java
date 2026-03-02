package com.bilibili.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bilibili.model.entity.VideoDO;
import com.bilibili.model.vo.VideoVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface VideoMapper extends BaseMapper<VideoDO> {

    IPage<VideoVO> selectPublishedVideos(Page<VideoVO> page,
                                         @Param("title") String title);

    IPage<VideoVO> selectMyPublishedVideos(Page<VideoVO> page,
                                           @Param("uid") Long uid,
                                           @Param("title") String title);

    List<VideoVO> selectPublishedVideosByIds(@Param("ids") List<Long> ids);

    List<Long> selectPublishedVideoIdsByTitle(@Param("title") String title,
                                              @Param("limit") Integer limit);

    List<Long> selectPublishedVideoIdsByCategoryId(@Param("categoryId") Long categoryId,
                                                   @Param("limit") Integer limit);

    IPage<VideoVO> selectPublishedVideosByViewCount(Page<VideoVO> page);

    int updateViewCountByDelta(@Param("videoId") Long videoId,
                               @Param("delta") Long delta);
}
