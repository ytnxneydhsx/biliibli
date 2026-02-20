package com.bilibili.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bilibili.model.entity.VideoDO;
import com.bilibili.model.vo.VideoVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface VideoMapper extends BaseMapper<VideoDO> {

    List<VideoVO> selectPublishedVideos(@Param("title") String title,
                                        @Param("offset") Integer offset,
                                        @Param("pageSize") Integer pageSize);

    List<VideoVO> selectMyPublishedVideos(@Param("uid") Long uid,
                                          @Param("title") String title,
                                          @Param("offset") Integer offset,
                                          @Param("pageSize") Integer pageSize);
}
