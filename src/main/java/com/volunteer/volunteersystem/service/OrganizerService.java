package com.volunteer.volunteersystem.service;

import java.util.Map;

public interface OrganizerService {
    /**
     * 获取组织者工作台数据
     */
    Map<String, Object> getOrganizerDashboard(Long organizerId);
}