package com.bilibili.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bilibili.model.entity.CommentLikeDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CommentLikeMapper extends BaseMapper<CommentLikeDO> {
}
