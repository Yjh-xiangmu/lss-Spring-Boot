package com.volunteer.volunteersystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.volunteer.volunteersystem.Entity.ActivityType;
import com.volunteer.volunteersystem.mapper.ActivityTypeMapper;
import com.volunteer.volunteersystem.service.ActivityTypeService;
import org.springframework.stereotype.Service;

@Service
public class ActivityTypeServiceImpl extends ServiceImpl<ActivityTypeMapper, ActivityType> implements ActivityTypeService {
}