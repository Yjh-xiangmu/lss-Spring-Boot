package com.volunteer.volunteersystem.Entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 资讯文章实体类
 */
@Data
@TableName("article")
public class Article {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;       // 标题

    // 对应数据库 cover_image (注意驼峰命名)
    private String coverImage;

    private String content;     // 内容

    // 对应数据库 article_type
    private Integer articleType;

    // 对应数据库 is_top
    private Integer isTop;

    // 【修复关键】对应数据库 views
    private Integer views;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    // 【修复关键】对应数据库 update_time
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}