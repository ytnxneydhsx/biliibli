package com.bilibili.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bilibili.model.entity.VideoUploadTaskDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface VideoUploadTaskMapper extends BaseMapper<VideoUploadTaskDO> {
}
