package com.volunteer.volunteersystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.volunteer.volunteersystem.Entity.Activity;
import com.volunteer.volunteersystem.Entity.Enrollment;
import com.volunteer.volunteersystem.Entity.ServiceRecord;
import com.volunteer.volunteersystem.Entity.User;
import com.volunteer.volunteersystem.common.Result;
import com.volunteer.volunteersystem.mapper.ActivityMapper;
import com.volunteer.volunteersystem.mapper.EnrollmentMapper;
import com.volunteer.volunteersystem.mapper.ServiceRecordMapper;
import com.volunteer.volunteersystem.mapper.UserMapper;
import com.volunteer.volunteersystem.service.EnrollmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 报名与结算管理控制器
 * 包含：报名、审核、签到、签退、时长修正、积分结算
 */
@RestController
@RequestMapping("/api/enrollment")
@CrossOrigin
public class EnrollmentController {

    @Autowired
    private EnrollmentService enrollmentService;

    @Autowired
    private EnrollmentMapper enrollmentMapper;

    @Autowired
    private ActivityMapper activityMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired(required = false)
    private ServiceRecordMapper serviceRecordMapper;

    // =================================================================================
    // 1. 志愿者报名相关接口
    // =================================================================================

    /**
     * 志愿者报名活动
     * 修复逻辑：如果是免审核活动(needAudit=0)，报名后直接设为“已通过(1)”
     */
    @PostMapping("/enroll")
    public Result<String> enrollActivity(@RequestBody EnrollRequest request) {
        try {
            // 1. 先执行基础报名（数据库插入一条 status=0 的记录）
            boolean success = enrollmentService.enrollActivity(request.getActivityId(), request.getVolunteerId());

            if (success) {
                // 2. 检查活动属性
                Activity activity = activityMapper.selectById(request.getActivityId());

                // 3. 判断是否免审核：活动存在 && 不需要审核 && 状态是招募或进行中
                if (activity != null && activity.getNeedAudit() == 0 && (activity.getStatus() == 1 || activity.getStatus() == 2)) {

                    // 自动更新为 1 (已通过)
                    LambdaUpdateWrapper<Enrollment> updateWrapper = new LambdaUpdateWrapper<>();
                    updateWrapper.eq(Enrollment::getActivityId, request.getActivityId())
                            .eq(Enrollment::getVolunteerId, request.getVolunteerId())
                            .set(Enrollment::getAuditStatus, 1);

                    enrollmentMapper.update(null, updateWrapper);
                    return Result.success("报名成功！该活动免审核，您已直接获得参与资格。");
                }

                return Result.success("报名申请已提交，请等待组织者审核");
            } else {
                return Result.error("报名失败，可能已报名或人数已满");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("系统错误：" + e.getMessage());
        }
    }

    /**
     * 志愿者取消报名
     */
    @PostMapping("/cancel")
    public Result<String> cancelEnrollment(@RequestBody EnrollRequest request) {
        try {
            // 1. 查询当前报名状态
            LambdaQueryWrapper<Enrollment> query = new LambdaQueryWrapper<>();
            query.eq(Enrollment::getActivityId, request.getActivityId())
                    .eq(Enrollment::getVolunteerId, request.getVolunteerId());
            Enrollment e = enrollmentMapper.selectOne(query);

            if(e == null) {
                return Result.error("未找到报名记录");
            }

            // 2. 逻辑检查：如果已经签到了，严禁取消
            if(e.getSignInTime() != null) {
                return Result.error("您已签到参与活动，无法取消报名");
            }

            // 3. 执行取消
            boolean success = enrollmentService.cancelEnrollment(request.getActivityId(), request.getVolunteerId());
            return success ? Result.success("已取消报名") : Result.error("取消失败");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 检查是否已报名 (用于前端按钮状态判断)
     */
    @GetMapping("/check")
    public Result<Boolean> checkEnrolled(@RequestParam Long activityId, @RequestParam Long volunteerId) {
        boolean isEnrolled = enrollmentService.checkEnrolled(activityId, volunteerId);
        return Result.success(isEnrolled);
    }

    // =================================================================================
    // 2. 组织者管理相关接口
    // =================================================================================

    /**
     * 获取某活动的详细报名名单 (包含志愿者信息、签到状态、时长)
     * 这里保留了完整的字段映射，保证前端能获取到所有信息
     */
    @GetMapping("/activity/{activityId}")
    public Result<List<Map<String, Object>>> getActivityEnrollments(@PathVariable Long activityId) {
        try {
            // 1. 查询报名表
            LambdaQueryWrapper<Enrollment> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Enrollment::getActivityId, activityId);
            wrapper.orderByDesc(Enrollment::getCreateTime);
            List<Enrollment> enrollmentList = enrollmentMapper.selectList(wrapper);

            // 2. 组装详细数据
            List<Map<String, Object>> resultList = new ArrayList<>();
            for (Enrollment enrollment : enrollmentList) {
                Map<String, Object> map = new HashMap<>();

                // 基础报名信息
                map.put("id", enrollment.getId());
                map.put("volunteerId", enrollment.getVolunteerId());
                map.put("auditStatus", enrollment.getAuditStatus()); // 0-待审 1-通过 2-拒
                map.put("auditRemark", enrollment.getAuditRemark());
                map.put("createTime", enrollment.getCreateTime());

                // 签到与时长信息 (关键)
                map.put("signInTime", enrollment.getSignInTime());
                map.put("signOutTime", enrollment.getSignOutTime());
                map.put("actualHours", enrollment.getActualHours());
                map.put("rewardStatus", enrollment.getRewardStatus()); // 0-未发 1-已发

                // 3. 关联查询志愿者个人信息 (姓名、电话)
                if (userMapper != null) {
                    User user = userMapper.selectById(enrollment.getVolunteerId());
                    if (user != null) {
                        // 优先显示真实姓名，没有则显示用户名
                        String name = user.getRealName();
                        if (name == null || name.isEmpty()) {
                            name = user.getUsername();
                        }
                        map.put("volunteerName", name);
                        map.put("volunteerPhone", user.getPhone());
                        map.put("volunteerAvatar", user.getAvatar());
                    } else {
                        map.put("volunteerName", "未知用户");
                        map.put("volunteerPhone", "无");
                    }
                }
                resultList.add(map);
            }
            return Result.success(resultList);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("加载名单失败：" + e.getMessage());
        }
    }

    /**
     * 报名审核 (通过/驳回)
     */
    @PutMapping("/audit")
    public Result<String> auditEnrollment(@RequestBody AuditRequest request) {
        try {
            Enrollment enrollment = enrollmentMapper.selectById(request.getId());
            if (enrollment == null) {
                return Result.error("记录不存在");
            }

            enrollment.setAuditStatus(request.getStatus());
            enrollment.setAuditRemark(request.getRemark());
            enrollmentMapper.updateById(enrollment);

            return Result.success("审核操作成功");
        } catch (Exception e) {
            return Result.error("系统错误：" + e.getMessage());
        }
    }

    // =================================================================================
    // 3. 时长管理与签到流程 (核心修复区)
    // =================================================================================

    /**
     * 志愿者签到
     * 修复：包含旧数据自动修复逻辑 (如果状态是0，签到时自动改为1)
     */
    @PutMapping("/checkIn/{id}")
    public Result<String> checkIn(@PathVariable Long id) {
        Enrollment e = enrollmentMapper.selectById(id);
        if (e == null) return Result.error("记录不存在");

        // 设置签到时间
        e.setSignInTime(LocalDateTime.now());

        // 【自动修复】如果状态还是待审核(0)，说明是历史遗留的免审数据，顺手改为已通过(1)
        if (e.getAuditStatus() == 0) {
            e.setAuditStatus(1);
        }

        enrollmentMapper.updateById(e);
        return Result.success("签到成功！");
    }

    /**
     * 志愿者签退 (自动计算时长)
     */
    @PutMapping("/checkOut/{id}")
    public Result<String> checkOut(@PathVariable Long id) {
        Enrollment e = enrollmentMapper.selectById(id);
        if (e == null || e.getSignInTime() == null) {
            return Result.error("您尚未签到，无法签退");
        }

        e.setSignOutTime(LocalDateTime.now());

        // 计算时长 (单位：小时，保留1位小数)
        long minutes = Duration.between(e.getSignInTime(), e.getSignOutTime()).toMinutes();
        // 算法：分钟 / 60.0 -> 四舍五入保留1位
        double hours = Math.round((minutes / 60.0) * 10.0) / 10.0;

        if (hours < 0) hours = 0; // 防止时间倒流异常

        e.setActualHours(BigDecimal.valueOf(hours));
        enrollmentMapper.updateById(e);

        return Result.success("签退成功！本次服务时长：" + hours + "小时");
    }

    /**
     * 组织者手动修改时长
     */
    @PutMapping("/updateHours")
    public Result<String> updateHours(@RequestBody UpdateHoursRequest req) {
        Enrollment e = enrollmentMapper.selectById(req.getId());
        if (e == null) return Result.error("记录不存在");

        e.setActualHours(BigDecimal.valueOf(req.getHours()));
        enrollmentMapper.updateById(e);

        return Result.success("时长已修正");
    }

    /**
     * 【核心结算接口】组织者确认发放时长和积分
     * 包含：
     * 1. 积分计算 (按比例或全额)
     * 2. 写入 ServiceRecord 流水
     * 3. 增加 User 余额
     * 4. 更新 Enrollment 状态
     */
    @PutMapping("/confirmHours/{id}")
    @Transactional(rollbackFor = Exception.class)
    public Result<String> confirmHours(@PathVariable Long id) {
        // 1. 查报名记录
        Enrollment e = enrollmentMapper.selectById(id);
        if (e == null) return Result.error("记录不存在");
        if (e.getRewardStatus() != null && e.getRewardStatus() == 1) return Result.error("已发放过");

        // 2. 查活动信息
        Activity act = activityMapper.selectById(e.getActivityId());
        if (act == null) return Result.error("活动异常");

        // 3. 准备数据
        BigDecimal actualHours = e.getActualHours(); // 实际干了多久
        if (actualHours == null || actualHours.doubleValue() <= 0) return Result.error("时长无效");

        BigDecimal estimatedHours = act.getEstimatedHours(); // 预计要干多久
        Integer maxPoints = act.getRewardPoints(); // 满分是多少

        // 4. 【计算积分】(简单数学，不用导包)
        int pointsToAdd = 0;
        if (maxPoints != null && maxPoints > 0) {
            double actual = actualHours.doubleValue();
            double estimated = (estimatedHours != null) ? estimatedHours.doubleValue() : 0.0;

            // 逻辑：如果没填预计时间，或者干得比预计久 => 给满分
            if (estimated == 0 || actual >= estimated) {
                pointsToAdd = maxPoints;
            } else {
                // 逻辑：干得少 => 按比例打折 (例如：干了一半时间，给一半分)
                // 这里的 (int) 会自动把小数去掉，变成整数
                pointsToAdd = (int) ((actual / estimated) * maxPoints);
            }
        }

        // 5. 【核心修改】更新用户 (时长和积分完全一样的加法逻辑)
        User user = userMapper.selectById(e.getVolunteerId());
        if (user != null) {
            // --- A. 加时长 ---
            // 如果原来是空，就当0
            BigDecimal oldHours = (user.getTotalHours() == null) ? BigDecimal.ZERO : user.getTotalHours();
            // 存入 = 旧 + 新
            user.setTotalHours(oldHours.add(actualHours));

            // --- B. 加积分 ---
            // 如果原来是空，就当0
            int oldPoints = (user.getPoints() == null) ? 0 : user.getPoints();
            // 存入 = 旧 + 新
            user.setPoints(oldPoints + pointsToAdd);

            // --- C. 保存进数据库 ---
            userMapper.updateById(user);
        }

        // 6. 记流水 (ServiceRecord)
        if (serviceRecordMapper != null) {
            ServiceRecord r = new ServiceRecord();
            r.setVolunteerId(e.getVolunteerId());
            r.setActivityId(e.getActivityId());
            r.setServiceHours(actualHours);
            r.setServiceDate(LocalDate.now());
            r.setRecordType(1);
            r.setAuditStatus(1);
            r.setAuditor("系统");
            r.setRemark("积分+" + pointsToAdd); // 备注里写清楚加了多少
            r.setCreateTime(LocalDateTime.now());
            serviceRecordMapper.insert(r);
        }

        // 7. 盖章 (标记为已发放)
        e.setRewardStatus(1);
        enrollmentMapper.updateById(e);

        return Result.success("结算完成！时长+" + actualHours + "，积分+" + pointsToAdd);
    }

    // =================================================================================
    // 4. DTO 数据传输对象
    // =================================================================================

    @lombok.Data
    public static class EnrollRequest {
        private Long activityId;
        private Long volunteerId;
    }

    @lombok.Data
    public static class AuditRequest {
        private Long id;
        private Integer status;
        private String remark;
    }

    @lombok.Data
    public static class UpdateHoursRequest {
        private Long id;
        private Double hours;
    }
}