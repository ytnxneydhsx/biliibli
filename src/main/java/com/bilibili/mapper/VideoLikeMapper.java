package com.bilibili.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bilibili.model.entity.VideoLikeDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface VideoLikeMapper extends BaseMapper<VideoLikeDO> {
}
