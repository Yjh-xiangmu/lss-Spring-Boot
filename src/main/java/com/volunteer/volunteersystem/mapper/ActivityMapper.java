package com.volunteer.volunteersystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.volunteer.volunteersystem.Entity.Activity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ActivityMapper extends BaseMapper<Activity> {
}