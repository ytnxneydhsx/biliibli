package com.bilibili.user.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bilibili.user.model.entity.UserDO;
import com.bilibili.search.model.vo.UserSearchVO;
import org.apache.ibatis.annotations.Param;

public interface UserMapper extends BaseMapper<UserDO> {

    IPage<UserSearchVO> selectUsersByNickname(Page<UserSearchVO> page,
                                              @Param("nickname") String nickname,
                                              @Param("desc") boolean desc);
}
