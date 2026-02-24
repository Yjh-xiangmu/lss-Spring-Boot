package com.volunteer.volunteersystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.volunteer.volunteersystem.Entity.Activity;
import com.volunteer.volunteersystem.Entity.Enrollment;
import com.volunteer.volunteersystem.common.Result;
import com.volunteer.volunteersystem.mapper.ActivityMapper;
import com.volunteer.volunteersystem.mapper.EnrollmentMapper;
import com.volunteer.volunteersystem.service.ActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/activity")
@CrossOrigin
public class ActivityController {

    @Autowired
    private ActivityService activityService;

    @Autowired
    private ActivityMapper activityMapper;

    @Autowired
    private EnrollmentMapper enrollmentMapper;

    // ================== 这是为你加的获取活动类型的接口 ==================
    @Autowired
    private com.volunteer.volunteersystem.mapper.ActivityTypeMapper activityTypeMapper;

    @GetMapping("/type-list")
    public Result<List<com.volunteer.volunteersystem.Entity.ActivityType>> getTypeList() {
        LambdaQueryWrapper<com.volunteer.volunteersystem.Entity.ActivityType> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(com.volunteer.volunteersystem.Entity.ActivityType::getStatus, 1)
                .orderByAsc(com.volunteer.volunteersystem.Entity.ActivityType::getSort);
        return Result.success(activityTypeMapper.selectList(wrapper));
    }
    // =================================================================

    /**
     * 分页查询活动列表（志愿者端）
     */
    @GetMapping("/list")
    public Result<Page<Activity>> getActivityList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "9") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String activityType,
            @RequestParam(required = false) Integer status
    ) {
        Page<Activity> page = activityService.getActivityList(pageNum, pageSize, keyword, activityType, status);
        fillRealEnrolledCount(page.getRecords());
        return Result.success(page);
    }

    /**
     * 获取活动详情
     */
    @GetMapping("/{id}")
    public Result<Activity> getActivityById(@PathVariable Long id) {
        Activity activity = activityService.getActivityById(id);
        if (activity == null) {
            return Result.error("活动不存在");
        }
        Long count = enrollmentMapper.selectCount(new LambdaQueryWrapper<Enrollment>()
                .eq(Enrollment::getActivityId, id)
                .eq(Enrollment::getAuditStatus, 1));
        activity.setEnrolledCount(count.intValue());
        return Result.success(activity);
    }

    /**
     * 获取我的活动列表
     */
    @GetMapping("/my")
    public Result<Page<Map<String, Object>>> getMyActivities(
            @RequestParam Long volunteerId,
            @RequestParam(required = false) Integer auditStatus,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        return Result.success(activityService.getMyActivities(volunteerId, auditStatus, pageNum, pageSize));
    }

    /**
     * 获取组织者的活动列表（组织者端）
     */
    @GetMapping("/organizer/{organizerId}")
    public Result<Page<Activity>> getOrganizerActivities(
            @PathVariable Long organizerId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        try {
            Page<Activity> page = new Page<>(pageNum, pageSize);
            LambdaQueryWrapper<Activity> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Activity::getOrganizerId, organizerId);
            if (keyword != null && !keyword.trim().isEmpty()) {
                queryWrapper.like(Activity::getTitle, keyword);
            }
            if (status != null) {
                queryWrapper.eq(Activity::getStatus, status);
            }
            queryWrapper.orderByDesc(Activity::getCreateTime);

            page = activityMapper.selectPage(page, queryWrapper);
            fillRealEnrolledCount(page.getRecords());
            for (Activity act : page.getRecords()) {
                checkAndUpdateStatus(act);
            }
            return Result.success(page);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("加载失败：" + e.getMessage());
        }
    }

    private void checkAndUpdateStatus(Activity activity) {
        if (activity == null || activity.getStatus() == 0 || activity.getStatus() == 4) return;
        LocalDateTime now = LocalDateTime.now();
        int newStatus = activity.getStatus();
        if (activity.getActivityEndTime() != null && now.isAfter(activity.getActivityEndTime())) {
            newStatus = 3;
        } else if (activity.getActivityStartTime() != null && now.isAfter(activity.getActivityStartTime())) {
            newStatus = 2;
        }
        if (newStatus != activity.getStatus()) {
            activity.setStatus(newStatus);
            activityMapper.updateById(activity);
        }
    }

    private void fillRealEnrolledCount(List<Activity> activities) {
        if (activities == null || activities.isEmpty()) return;
        for (Activity activity : activities) {
            try {
                Long count = enrollmentMapper.selectCount(new LambdaQueryWrapper<Enrollment>()
                        .eq(Enrollment::getActivityId, activity.getId())
                        .eq(Enrollment::getAuditStatus, 1));
                activity.setEnrolledCount(count.intValue());
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    @PostMapping("/create")
    public Result<String> createActivity(@RequestBody Activity activity) {
        try {
            int result = activityMapper.insert(activity);
            return result > 0 ? Result.success("创建成功") : Result.error("创建失败");
        } catch (Exception e) { return Result.error(e.getMessage()); }
    }

    @PutMapping("/update")
    public Result<String> updateActivity(@RequestBody Activity activity) {
        try {
            int result = activityMapper.updateById(activity);
            return result > 0 ? Result.success("更新成功") : Result.error("更新失败");
        } catch (Exception e) { return Result.error(e.getMessage()); }
    }

    @DeleteMapping("/{id}")
    public Result<String> deleteActivity(@PathVariable Long id) {
        try {
            int result = activityMapper.deleteById(id);
            return result > 0 ? Result.success("删除成功") : Result.error("删除失败");
        } catch (Exception e) { return Result.error(e.getMessage()); }
    }

    @PutMapping("/publish/{id}")
    public Result<String> publishActivity(@PathVariable Long id) {
        try {
            Activity activity = activityMapper.selectById(id);
            if (activity == null) return Result.error("活动不存在");
            activity.setStatus(1);
            activityMapper.updateById(activity);
            return Result.success("发布成功");
        } catch (Exception e) { return Result.error(e.getMessage()); }
    }

    @PutMapping("/cancel/{id}")
    public Result<String> cancelActivity(@PathVariable Long id) {
        try {
            Activity activity = activityMapper.selectById(id);
            if (activity == null) return Result.error("活动不存在");
            if (activity.getStatus() != 1 && activity.getStatus() != 2) return Result.error("无法取消");
            activity.setStatus(4);
            activityMapper.updateById(activity);
            return Result.success("活动已取消");
        } catch (Exception e) { return Result.error(e.getMessage()); }
    }
}