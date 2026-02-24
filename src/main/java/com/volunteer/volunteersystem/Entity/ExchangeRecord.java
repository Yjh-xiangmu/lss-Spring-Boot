package com.volunteer.volunteersystem.Entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 兑换记录实体类
 */
@Data
@TableName("exchange_record")
public class ExchangeRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;       // 订单号
    private Long volunteerId;     // 志愿者ID
    private Long productId;       // 商品ID
    private String productName;   // 商品名称快照
    private Integer quantity;     // 兑换数量
    private Integer totalPoints;  // 消耗总积分

    // 0-待发货 1-已发货/已领取
    private Integer receiveStatus;

    private String receiverName;    // 收货人
    private String receiverPhone;   // 电话
    private String receiverAddress; // 地址

    // 【新增】创建时间
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    // 【新增】更新时间 (解决报错的关键)
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}