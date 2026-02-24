package com.volunteer.volunteersystem.Entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 报名实体类
 */
@Data
@TableName("enrollment")
public class Enrollment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long activityId;  // 活动ID
    private Long volunteerId;  // 志愿者ID
    private Integer auditStatus;  // 审核状态 0-待审核 1-通过 2-拒绝
    private String auditRemark;  // 审核备注
    private LocalDateTime signInTime;  // 签到时间
    private LocalDateTime signOutTime;  // 签退时间
    private BigDecimal actualHours;  // 实际时长
    private Integer rewardStatus;  // 奖励发放状态

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}