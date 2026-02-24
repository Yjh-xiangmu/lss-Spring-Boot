package com.volunteer.volunteersystem.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.volunteer.volunteersystem.Entity.Activity;

import java.util.List;
import java.util.Map;

public interface ActivityService {
    /**
     * 分页查询活动列表
     */
    Page<Activity> getActivityList(Integer pageNum, Integer pageSize, String keyword, String activityType, Integer status);

    /**
     * 获取活动详情
     */
    Activity getActivityById(Long id);

    /**
     * 获取我的活动列表（带报名信息）
     */
    Page<Map<String, Object>> getMyActivities(Long volunteerId, Integer auditStatus, Integer pageNum, Integer pageSize);
}