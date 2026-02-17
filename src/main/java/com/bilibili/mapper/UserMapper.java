package com.bilibili.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bilibili.model.entity.UserDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户核心 Mapper 接口
 * 
 * 这里的魔法在于继承了 MyBatis-Plus 的 BaseMapper<UserDO>
 * 只要写下这一行，你就瞬间拥有了 17 个常用的数据库操作方法：
 * - insert(entity) : 插入
 * - deleteById(id) : 根据ID删除（配合@TableLogic就是逻辑删除）
 * - updateById(entity) : 根据ID更新
 * - selectById(id) : 根据ID查询
 * - selectList(wrapper) : 条件查询列表
 * ...等等，一行 SQL 都不用自己写！
 */
@Mapper // 告诉 Spring 这是一个 MyBatis 的 Mapper 接口
public interface UserMapper extends BaseMapper<UserDO> {
    
    // 如果以后有 BaseMapper 搞不定的超级复杂联查，才在这里定义新方法
    // 比如：UserDO selectUserWithExtraInfo(Long id);
}
