package com.bilibili.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bilibili.model.entity.VideoTagDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface VideoTagMapper extends BaseMapper<VideoTagDO> {
}
