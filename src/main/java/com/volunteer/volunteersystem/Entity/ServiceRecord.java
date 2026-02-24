package com.volunteer.volunteersystem.Entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 服务记录实体类
 */
@Data
@TableName("service_record")
public class ServiceRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long volunteerId;  // 志愿者ID
    private Long activityId;  // 活动ID
    private BigDecimal serviceHours;  // 服务时长
    private LocalDate serviceDate;  // 服务日期
    private Integer recordType;  // 记录类型 1-系统发放 2-补录
    private Integer auditStatus;  // 审核状态 0-待审核 1-已通过 2-已拒绝
    private String auditor;  // 认定人
    private String remark;  // 备注
    private Integer balanceSnapshot;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}