package com.badminton.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

    @Data
@TableName("rules")
public class Rules {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String type;

    private String title;

    private String sections;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
