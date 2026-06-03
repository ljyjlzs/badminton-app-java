package com.badminton.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("scores")
public class Score {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long activityId;

    private Long userId;

    private Long matchId;

    private String source;

    private Integer scoreChange;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
