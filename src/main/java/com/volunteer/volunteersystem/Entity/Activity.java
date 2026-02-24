package com.volunteer.volunteersystem.Entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 活动实体类
 */
@Data
@TableName("activity")
public class Activity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;  // 活动名称
    private String coverImage;  // 封面图
    private String activityType;  // 活动类型
    private String area;  // 服务区域
    private Integer recruitCount;  // 招募人数
    private Integer enrolledCount;  // 已报名人数
    private BigDecimal estimatedHours;  // 预计时长
    private Integer rewardPoints;  // 奖励积分
    private LocalDateTime signupStartTime;  // 报名开始
    private LocalDateTime signupEndTime;  // 报名结束
    private LocalDateTime activityStartTime;  // 活动开始
    private LocalDateTime activityEndTime;  // 活动结束
    private String content;  // 活动详情
    private Integer needAudit;  // 是否需要审核
    private Long organizerId;  // 组织者ID
    private Integer status;  // 状态 0-草稿 1-招募中 2-进行中 3-已结束

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}