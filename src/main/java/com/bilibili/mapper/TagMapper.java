package com.bilibili.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bilibili.model.entity.TagDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TagMapper extends BaseMapper<TagDO> {
}
