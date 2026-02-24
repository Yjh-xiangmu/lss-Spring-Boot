package com.volunteer.volunteersystem.Entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户实体类
 */
@Data
@TableName("user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;  // 用户ID

    private String username;  // 用户名
    private String password;  // 密码
    private String realName;  // 真实姓名
    private String phone;  // 手机号
    private String idCard;  // 身份证号
    private Integer userType;  // 用户类型 1-志愿者 2-组织者 3-管理员
    private String community;  // 居住社区
    private String skills;  // 擅长技能
    private String avatar;  // 头像
    private String email;  // 邮箱
    private String address;  // 地址
    private BigDecimal totalHours;  // 累计服务时长
    private Integer totalPoints;  // 累计积分
    private Integer starLevel;  // 星级
    private Integer status;  // 状态 1-正常 0-禁用

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;  // 创建时间

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;  // 更新时间
    private Integer points;        // 总积分
}