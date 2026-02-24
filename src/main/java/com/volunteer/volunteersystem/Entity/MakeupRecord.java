package com.volunteer.volunteersystem.Entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 补录申请实体类
 */
@Data
@TableName("makeup_record")
public class MakeupRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long volunteerId;
    private Long activityId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String reason;
    private String proofImages;
    private Integer auditStatus;  // 0-待审核 1-通过 2-拒绝
    private String rejectReason;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}