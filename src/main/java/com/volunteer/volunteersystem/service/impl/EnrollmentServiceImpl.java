package com.volunteer.volunteersystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.volunteer.volunteersystem.Entity.Activity;
import com.volunteer.volunteersystem.Entity.Enrollment;
import com.volunteer.volunteersystem.mapper.ActivityMapper;
import com.volunteer.volunteersystem.mapper.EnrollmentMapper;
import com.volunteer.volunteersystem.service.EnrollmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnrollmentServiceImpl implements EnrollmentService {

    @Autowired
    private EnrollmentMapper enrollmentMapper;

    @Autowired
    private ActivityMapper activityMapper;

    @Override
    @Transactional
    public boolean enrollActivity(Long activityId, Long volunteerId) {
        // 1. 检查是否已报名
        if (checkEnrolled(activityId, volunteerId)) {
            throw new RuntimeException("您已报名该活动");
        }

        // 2. 检查活动是否存在
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new RuntimeException("活动不存在");
        }

        // 3. 检查是否满员
        if (activity.getEnrolledCount() >= activity.getRecruitCount()) {
            throw new RuntimeException("活动已满员");
        }

        // 4. 创建报名记录
        Enrollment enrollment = new Enrollment();
        enrollment.setActivityId(activityId);
        enrollment.setVolunteerId(volunteerId);
        enrollment.setAuditStatus(activity.getNeedAudit() == 1 ? 0 : 1);  // 如果需要审核则为待审核，否则直接通过
        enrollment.setRewardStatus(0);

        int result = enrollmentMapper.insert(enrollment);

        // 5. 更新活动已报名人数
        if (result > 0) {
            activity.setEnrolledCount(activity.getEnrolledCount() + 1);
            activityMapper.updateById(activity);
        }

        return result > 0;
    }

    @Override
    @Transactional
    public boolean cancelEnrollment(Long activityId, Long volunteerId) {
        // 1. 查找报名记录
        LambdaQueryWrapper<Enrollment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Enrollment::getActivityId, activityId)
                .eq(Enrollment::getVolunteerId, volunteerId);
        Enrollment enrollment = enrollmentMapper.selectOne(queryWrapper);

        if (enrollment == null) {
            throw new RuntimeException("未找到报名记录");
        }

        // 2. 检查是否已签到（已签到不能取消）
        if (enrollment.getSignInTime() != null) {
            throw new RuntimeException("已签到的活动不能取消报名");
        }

        // 3. 删除报名记录
        int result = enrollmentMapper.deleteById(enrollment.getId());

        // 4. 更新活动已报名人数
        if (result > 0) {
            Activity activity = activityMapper.selectById(activityId);
            if (activity != null && activity.getEnrolledCount() > 0) {
                activity.setEnrolledCount(activity.getEnrolledCount() - 1);
                activityMapper.updateById(activity);
            }
        }

        return result > 0;
    }

    @Override
    public boolean checkEnrolled(Long activityId, Long volunteerId) {
        LambdaQueryWrapper<Enrollment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Enrollment::getActivityId, activityId)
                .eq(Enrollment::getVolunteerId, volunteerId);
        return enrollmentMapper.selectCount(queryWrapper) > 0;
    }
}