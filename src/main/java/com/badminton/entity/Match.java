package com.badminton.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("matches")
public class Match {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long activityId;

    private String round;

    private Integer roundOrder;

    private String court;

    private Long team1Id;

    private Long team2Id;

    private Integer team1Score;

    private Integer team2Score;

    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
