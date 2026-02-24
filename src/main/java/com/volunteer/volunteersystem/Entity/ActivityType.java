package com.volunteer.volunteersystem.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("activity_type")
public class ActivityType {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String typeName;
    private Integer sort;
    private Integer status;
}