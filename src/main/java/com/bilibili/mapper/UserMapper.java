package com.bilibili.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bilibili.model.entity.UserDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper 
public interface UserMapper extends BaseMapper<UserDO> {
    
    
    
}
