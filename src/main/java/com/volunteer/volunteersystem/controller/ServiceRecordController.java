package com.volunteer.volunteersystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.volunteer.volunteersystem.Entity.Activity;
import com.volunteer.volunteersystem.Entity.ServiceRecord;
import com.volunteer.volunteersystem.Entity.User;
import com.volunteer.volunteersystem.common.Result;
import com.volunteer.volunteersystem.mapper.ActivityMapper;
import com.volunteer.volunteersystem.mapper.ServiceRecordMapper;
import com.volunteer.volunteersystem.mapper.UserMapper;
import com.volunteer.volunteersystem.service.ServiceRecordService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/service-record")
@CrossOrigin
public class ServiceRecordController {

    @Autowired
    private ServiceRecordService serviceRecordService;

    @Autowired
    private ServiceRecordMapper serviceRecordMapper;

    @Autowired
    private ActivityMapper activityMapper;

    @Autowired
    private UserMapper userMapper;

    // ================= 志愿者端接口 =================

    @GetMapping("/stats/{volunteerId}")
    public Result<Map<String, Object>> getVolunteerStats(@PathVariable Long volunteerId) {
        return Result.success(serviceRecordService.getVolunteerStats(volunteerId));
    }

    @GetMapping("/list")
    public Result<Page<Map<String, Object>>> getRecordList(
            @RequestParam Long volunteerId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(serviceRecordService.getRecordList(volunteerId, pageNum, pageSize));
    }

    @PostMapping("/makeup")
    public Result<String> applyMakeup(@RequestBody MakeupRequest request) {
        try {
            boolean success = serviceRecordService.applyMakeup(
                    request.getVolunteerId(), request.getActivityId(),
                    request.getStartTime(), request.getEndTime(),
                    request.getReason(), request.getProofImages());
            return success ? Result.success("申请已提交，请等待审核") : Result.error("申请失败");
        } catch (Exception e) {
            return Result.error("系统错误：" + e.getMessage());
        }
    }

    // ================= 组织者端接口 =================

    /**
     * 获取待审核列表
     */
    @GetMapping("/organizer/pending/{organizerId}")
    public Result<List<Map<String, Object>>> getPendingMakeupList(@PathVariable Long organizerId) {
        // 1. 查该组织者的所有活动
        List<Activity> activities = activityMapper.selectList(new LambdaQueryWrapper<Activity>()
                .eq(Activity::getOrganizerId, organizerId));

        if (activities.isEmpty()) {
            return Result.success(new ArrayList<>());
        }

        List<Long> activityIds = activities.stream().map(Activity::getId).collect(Collectors.toList());

        // 2. 查这些活动的待审核补录
        List<ServiceRecord> records = serviceRecordMapper.selectList(new LambdaQueryWrapper<ServiceRecord>()
                .in(ServiceRecord::getActivityId, activityIds)
                .eq(ServiceRecord::getRecordType, 2)
                .eq(ServiceRecord::getAuditStatus, 0)
                .orderByDesc(ServiceRecord::getCreateTime));

        // 3. 组装显示数据
        List<Map<String, Object>> result = new ArrayList<>();
        for (ServiceRecord r : records) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", r.getId());
            map.put("serviceHours", r.getServiceHours());
            map.put("reason", r.getRemark());
            map.put("createTime", r.getCreateTime());

            Activity act = activityMapper.selectById(r.getActivityId());
            map.put("activityName", act != null ? act.getTitle() : "未知活动");

            User user = userMapper.selectById(r.getVolunteerId());
            map.put("volunteerName", user != null ? user.getRealName() : "未知志愿者");
            map.put("volunteerAvatar", user != null ? user.getAvatar() : "");

            result.add(map);
        }
        return Result.success(result);
    }

    /**
     * 审核接口：直接调用 Service 的逻辑
     */
    @PutMapping("/audit")
    public Result<String> auditMakeup(@RequestBody AuditRequest req) {
        // 调用我们在 Service 接口里新加的方法
        boolean success = serviceRecordService.auditServiceRecord(req.getId(), req.getStatus(), "组织者");

        if (success) {
            return Result.success(req.getStatus() == 1 ? "审核通过" : "已驳回");
        } else {
            return Result.error("操作失败");
        }
    }

    // ================= DTO 类 =================
    @Data
    public static class MakeupRequest {
        private Long volunteerId;
        private Long activityId;
        private String startTime;
        private String endTime;
        private String reason;
        private String proofImages;
    }

    @Data
    public static class AuditRequest {
        private Long id;
        private Integer status; // 1-通过 2-驳回
    }
}