package com.volunteer.volunteersystem.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.volunteer.volunteersystem.Entity.ServiceRecord;
import java.util.Map;

/**
 * 服务记录 Service 接口
 */
public interface ServiceRecordService extends IService<ServiceRecord> {

    // 统计志愿者数据
    Map<String, Object> getVolunteerStats(Long volunteerId);

    // 获取记录列表
    Page<Map<String, Object>> getRecordList(Long volunteerId, Integer pageNum, Integer pageSize);

    // 申请补录
    boolean applyMakeup(Long volunteerId, Long activityId, String startTime, String endTime, String reason, String proofImages);

    // 【新增】审核服务记录（解决 Controller 报错的关键）
    boolean auditServiceRecord(Long recordId, Integer status, String auditor);
}