package com.bilibili.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bilibili.model.entity.UserInfoDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper // 告诉 Spring 这是一个 MyBatis 的 Mapper 接口
public interface UserInfoMapper extends BaseMapper<UserInfoDO> {
    
    // 如果以后有 BaseMapper 搞不定的超级复杂联查，才在这里定义新方法
    // 比如：UserDO selectUserWithExtraInfo(Long id);
}