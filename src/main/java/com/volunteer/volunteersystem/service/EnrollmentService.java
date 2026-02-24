package com.volunteer.volunteersystem.service;

import com.volunteer.volunteersystem.Entity.Enrollment;

public interface EnrollmentService {
    /**
     * 报名活动
     */
    boolean enrollActivity(Long activityId, Long volunteerId);

    /**
     * 取消报名
     */
    boolean cancelEnrollment(Long activityId, Long volunteerId);

    /**
     * 检查是否已报名
     */
    boolean checkEnrolled(Long activityId, Long volunteerId);
}