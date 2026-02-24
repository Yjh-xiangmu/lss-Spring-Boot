package com.volunteer.volunteersystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.volunteer.volunteersystem.Entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户数据访问层
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
    // MyBatis-Plus 已经提供了基础的增删改查方法，不需要写SQL
}