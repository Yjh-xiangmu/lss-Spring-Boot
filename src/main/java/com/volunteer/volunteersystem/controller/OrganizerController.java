package com.volunteer.volunteersystem.controller;

import com.volunteer.volunteersystem.common.Result;
import com.volunteer.volunteersystem.service.OrganizerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/organizer")
@CrossOrigin
public class OrganizerController {

    @Autowired
    private OrganizerService organizerService;

    /**
     * 获取组织者工作台数据
     */
    @GetMapping("/dashboard/{organizerId}")
    public Result<Map<String, Object>> getDashboard(@PathVariable Long organizerId) {
        try {
            Map<String, Object> dashboard = organizerService.getOrganizerDashboard(organizerId);
            return Result.success(dashboard);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("加载失败：" + e.getMessage());
        }
    }
}