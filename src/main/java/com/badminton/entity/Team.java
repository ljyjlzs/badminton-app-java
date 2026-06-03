package com.badminton.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

    @Data
@TableName("teams")
public class Team {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long activityId;

    private String name;

    private String members;

    private Boolean isEliminated;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
