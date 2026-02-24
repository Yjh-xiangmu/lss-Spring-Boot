package com.volunteer.volunteersystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.volunteer.volunteersystem.Entity.Activity;
import com.volunteer.volunteersystem.Entity.Enrollment;
import com.volunteer.volunteersystem.mapper.ActivityMapper;
import com.volunteer.volunteersystem.mapper.EnrollmentMapper;
import com.volunteer.volunteersystem.service.ActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ActivityServiceImpl implements ActivityService {

    @Autowired
    private ActivityMapper activityMapper;

    @Autowired
    private EnrollmentMapper enrollmentMapper;

    @Override
    public Page<Activity> getActivityList(Integer pageNum, Integer pageSize, String keyword, String activityType, Integer status) {
        Page<Activity> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Activity> queryWrapper = new LambdaQueryWrapper<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            queryWrapper.and(wrapper -> wrapper.like(Activity::getTitle, keyword).or().like(Activity::getArea, keyword));
        }
        if (activityType != null && !activityType.trim().isEmpty()) {
            queryWrapper.eq(Activity::getActivityType, activityType);
        }

        // 注意：如果要根据时间自动变状态，这里先不查 Status，或者查出来再过滤
        // ... (前面的代码保持不变)

        if (status != null) {
            queryWrapper.eq(Activity::getStatus, status);
        } else {
            // 【修改点】默认只查询：1-招募中 和 2-进行中
            // 自动过滤掉 3-已结束 和 4-已取消
            queryWrapper.in(Activity::getStatus, 1, 2);
        }

        queryWrapper.orderByDesc(Activity::getCreateTime);

        // ... (后面的代码保持不变)

        queryWrapper.orderByDesc(Activity::getCreateTime);

        // 执行查询
        activityMapper.selectPage(page, queryWrapper);

        // 【关键修复】遍历结果，根据时间更新状态
        for (Activity activity : page.getRecords()) {
            updateActivityStatus(activity);
        }

        return page;
    }

    @Override
    public Activity getActivityById(Long id) {
        Activity activity = activityMapper.selectById(id);
        if (activity != null) {
            updateActivityStatus(activity);
        }
        return activity;
    }

    @Override
    public Page<Map<String, Object>> getMyActivities(Long volunteerId, Integer auditStatus, Integer pageNum, Integer pageSize) {
        // 1. 查询报名记录
        LambdaQueryWrapper<Enrollment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Enrollment::getVolunteerId, volunteerId);
        queryWrapper.orderByDesc(Enrollment::getCreateTime);

        Page<Enrollment> enrollmentPage = new Page<>(pageNum, pageSize);
        enrollmentPage = enrollmentMapper.selectPage(enrollmentPage, queryWrapper);

        // 2. 组装活动信息
        List<Map<String, Object>> resultList = enrollmentPage.getRecords().stream()
                .map(enrollment -> {
                    Activity activity = activityMapper.selectById(enrollment.getActivityId());
                    if (activity == null) return null;

                    // 顺便更新一下活动状态显示
                    updateActivityStatus(activity);

                    // 免审核逻辑
                    if ((activity.getNeedAudit() == null || activity.getNeedAudit() == 0)
                            && enrollment.getAuditStatus() == 0) {
                        enrollment.setAuditStatus(1);
                    }

                    Map<String, Object> map = new HashMap<>();
                    map.put("enrollment", enrollment);
                    map.put("activity", activity);
                    return map;
                })
                .filter(Objects::nonNull)
                .filter(map -> {
                    if (auditStatus == null) return true;
                    Enrollment e = (Enrollment) map.get("enrollment");
                    return e.getAuditStatus().equals(auditStatus);
                })
                .collect(Collectors.toList());

        Page<Map<String, Object>> resultPage = new Page<>(pageNum, pageSize);
        resultPage.setRecords(resultList);
        resultPage.setTotal(enrollmentPage.getTotal());
        resultPage.setPages(enrollmentPage.getPages());

        return resultPage;
    }

    /**
     * 【核心方法】根据当前时间，自动修正活动状态
     * 规则：
     * 1. 如果现在 > 活动结束时间 -> 状态变 3 (已结束)
     * 2. 如果现在 > 活动开始时间 且 < 结束时间 -> 状态变 2 (进行中)
     * 3. 如果现在 > 报名结束时间 且 < 活动开始时间 -> 状态变 1 (或者叫报名截止)
     * 注意：这里只更新内存对象显示给前端，如果需要持久化到数据库，可以解开注释
     */
    private void updateActivityStatus(Activity activity) {
        if (activity == null || activity.getStatus() == 0 || activity.getStatus() == 4) {
            return; // 草稿(0)和已取消(4)的不自动变
        }

        LocalDateTime now = LocalDateTime.now();

        // 状态定义：1-招募中, 2-进行中, 3-已结束
        int newStatus = activity.getStatus();

        if (activity.getActivityEndTime() != null && now.isAfter(activity.getActivityEndTime())) {
            newStatus = 3; // 已结束
        } else if (activity.getActivityStartTime() != null && now.isAfter(activity.getActivityStartTime())) {
            newStatus = 2; // 进行中
        } else if (activity.getSignupEndTime() != null && now.isAfter(activity.getSignupEndTime())) {
            // 报名结束了，但活动还没开始，这里依然算招募结束，或者你可以定义个新状态
            // 为了简单，我们暂且认为只要活动没开始，就还是显示招募中(1)，或者你可以改成 2
            // 这里维持原状
        }

        // 如果状态变了，更新对象
        if (newStatus != activity.getStatus()) {
            activity.setStatus(newStatus);
            // 【可选】如果你希望永久保存到数据库，取消下面这行的注释
            activityMapper.updateById(activity);
        }
    }
}