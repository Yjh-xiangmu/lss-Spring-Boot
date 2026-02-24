package com.volunteer.volunteersystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.volunteer.volunteersystem.Entity.Activity;
import com.volunteer.volunteersystem.Entity.Enrollment;
import com.volunteer.volunteersystem.Entity.MakeupRecord;
import com.volunteer.volunteersystem.mapper.ActivityMapper;
import com.volunteer.volunteersystem.mapper.EnrollmentMapper;
import com.volunteer.volunteersystem.mapper.MakeupRecordMapper;
import com.volunteer.volunteersystem.service.OrganizerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class OrganizerServiceImpl implements OrganizerService {

    @Autowired
    private EnrollmentMapper enrollmentMapper;

    @Autowired
    private MakeupRecordMapper makeupRecordMapper;

    @Autowired
    private ActivityMapper activityMapper;

    @Override
    public Map<String, Object> getOrganizerDashboard(Long organizerId) {
        Map<String, Object> dashboard = new HashMap<>();

        // 待审核报名数
        LambdaQueryWrapper<Enrollment> enrollmentQuery = new LambdaQueryWrapper<>();
        enrollmentQuery.eq(Enrollment::getAuditStatus, 0);
        Long pendingEnrollments = enrollmentMapper.selectCount(enrollmentQuery);
        dashboard.put("pendingEnrollments", pendingEnrollments);

        // 待审核补录数
        LambdaQueryWrapper<MakeupRecord> makeupQuery = new LambdaQueryWrapper<>();
        makeupQuery.eq(MakeupRecord::getAuditStatus, 0);
        Long pendingMakeups = makeupRecordMapper.selectCount(makeupQuery);
        dashboard.put("pendingMakeups", pendingMakeups);

        // 今日活动数
        LambdaQueryWrapper<Activity> activityQuery = new LambdaQueryWrapper<>();
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().plusDays(1).atStartOfDay();
        activityQuery.between(Activity::getActivityStartTime, startOfDay, endOfDay);
        Long todayActivities = activityMapper.selectCount(activityQuery);
        dashboard.put("todayActivities", todayActivities);

        // 本月活动总数
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        activityQuery = new LambdaQueryWrapper<>();
        activityQuery.ge(Activity::getActivityStartTime, startOfMonth);
        Long monthActivities = activityMapper.selectCount(activityQuery);
        dashboard.put("monthActivities", monthActivities);

        return dashboard;
    }
}