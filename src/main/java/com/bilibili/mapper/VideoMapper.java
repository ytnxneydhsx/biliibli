package com.bilibili.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bilibili.model.entity.VideoDO;
import com.bilibili.model.vo.VideoVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface VideoMapper extends BaseMapper<VideoDO> {

    /**
     * 自定义联表查询：获取视频列表（带出作者昵称）
     */
    List<VideoVO> selectVideoList(@Param("title") String title);
}
