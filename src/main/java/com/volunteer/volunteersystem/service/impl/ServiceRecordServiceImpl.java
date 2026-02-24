package com.volunteer.volunteersystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.volunteer.volunteersystem.Entity.Activity;
import com.volunteer.volunteersystem.Entity.ServiceRecord;
import com.volunteer.volunteersystem.Entity.User;
import com.volunteer.volunteersystem.mapper.ActivityMapper;
import com.volunteer.volunteersystem.mapper.ServiceRecordMapper;
import com.volunteer.volunteersystem.mapper.UserMapper;
import com.volunteer.volunteersystem.service.ServiceRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ServiceRecordServiceImpl extends ServiceImpl<ServiceRecordMapper, ServiceRecord> implements ServiceRecordService {

    @Autowired
    private ServiceRecordMapper serviceRecordMapper;

    @Autowired
    private ActivityMapper activityMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public Map<String, Object> getVolunteerStats(Long volunteerId) {
        User user = userMapper.selectById(volunteerId);
        Map<String, Object> map = new HashMap<>();
        map.put("totalHours", (user != null && user.getTotalHours() != null) ? user.getTotalHours() : 0);
        map.put("totalPoints", (user != null && user.getPoints() != null) ? user.getPoints() : 0); // 注意：这里返回的是当前余额

        Long count = serviceRecordMapper.selectCount(new LambdaQueryWrapper<ServiceRecord>()
                .eq(ServiceRecord::getVolunteerId, volunteerId));
        map.put("serviceCount", count);
        return map;
    }

    @Override
    public Page<Map<String, Object>> getRecordList(Long volunteerId, Integer pageNum, Integer pageSize) {
        Page<ServiceRecord> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ServiceRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ServiceRecord::getVolunteerId, volunteerId)
                .orderByDesc(ServiceRecord::getCreateTime);

        serviceRecordMapper.selectPage(page, wrapper);

        List<Map<String, Object>> resultList = new ArrayList<>();
        for (ServiceRecord record : page.getRecords()) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", record.getId());
            map.put("serviceDate", record.getServiceDate());
            map.put("serviceHours", record.getServiceHours());
            map.put("recordType", record.getRecordType());
            map.put("auditStatus", record.getAuditStatus());
            map.put("auditor", record.getAuditor());
            map.put("remark", record.getRemark());
            // 【新增】返回余额快照给前端
            map.put("balanceSnapshot", record.getBalanceSnapshot());

            int pointsEarned = 0;
            if (record.getActivityId() != null) {
                Activity activity = activityMapper.selectById(record.getActivityId());
                if (activity != null) {
                    map.put("activityName", activity.getTitle());

                    // 只有审核通过才计算并显示获得的积分
                    if (record.getAuditStatus() == 1) {
                        BigDecimal estimated = activity.getEstimatedHours();
                        Integer maxPoints = activity.getRewardPoints();
                        BigDecimal actual = record.getServiceHours();

                        if (estimated != null && estimated.doubleValue() > 0 && maxPoints != null) {
                            if (actual.compareTo(estimated) >= 0) {
                                pointsEarned = maxPoints;
                            } else {
                                pointsEarned = (int) (actual.doubleValue() / estimated.doubleValue() * maxPoints);
                            }
                        }
                    }
                } else {
                    map.put("activityName", "活动已删除");
                }
            } else {
                map.put("activityName", "系统调整");
            }
            map.put("pointsEarned", pointsEarned);

            resultList.add(map);
        }

        Page<Map<String, Object>> resultMap = new Page<>(pageNum, pageSize);
        resultMap.setRecords(resultList);
        resultMap.setTotal(page.getTotal());
        resultMap.setPages(page.getPages());
        return resultMap;
    }

    // (这个方法如果不使用可以保留空实现或删除)
    @Override
    public boolean applyMakeup(Long volunteerId, Long activityId, String startTime, String endTime, String reason, String proofImages) {
        return false;
    }

    /**
     * 【核心功能】审核服务记录并结算积分
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean auditServiceRecord(Long recordId, Integer status, String auditor) {
        // 1. 查询记录
        ServiceRecord record = serviceRecordMapper.selectById(recordId);
        if (record == null || record.getAuditStatus() != 0) {
            return false; // 记录不存在或已审核过
        }

        // 2. 设置基本状态
        record.setAuditStatus(status);
        record.setAuditor(auditor);

        // 3. 如果审核通过 (status == 1)，进行积分结算
        if (status == 1) {
            Activity activity = activityMapper.selectById(record.getActivityId());
            User user = userMapper.selectById(record.getVolunteerId());

            if (activity != null && user != null) {
                // --- 计算积分 ---
                int pointsToAdd = 0;
                BigDecimal estimatedHours = activity.getEstimatedHours();
                Integer maxPoints = activity.getRewardPoints();
                BigDecimal actualHours = record.getServiceHours();

                if (estimatedHours != null && estimatedHours.compareTo(BigDecimal.ZERO) > 0 && maxPoints != null) {
                    if (actualHours.compareTo(estimatedHours) >= 0) {
                        pointsToAdd = maxPoints;
                    } else {
                        double ratio = actualHours.doubleValue() / estimatedHours.doubleValue();
                        pointsToAdd = (int) (ratio * maxPoints);
                    }
                }

                // --- 更新用户数据 ---
                Integer currentPoints = user.getPoints() == null ? 0 : user.getPoints();
                int newBalance = currentPoints + pointsToAdd; // 计算新余额

                user.setPoints(newBalance);

                Integer currentTotalPoints = user.getTotalPoints() == null ? 0 : user.getTotalPoints();
                user.setTotalPoints(currentTotalPoints + pointsToAdd);

                BigDecimal currentTotalHours = user.getTotalHours() == null ? BigDecimal.ZERO : user.getTotalHours();
                user.setTotalHours(currentTotalHours.add(actualHours));

                userMapper.updateById(user);

                // 【核心修改】将新余额保存到记录快照中
                record.setBalanceSnapshot(newBalance);
            }
        }

        // 4. 最后保存记录 (确保状态和快照一起更新)
        serviceRecordMapper.updateById(record);
        return true;
    }
}