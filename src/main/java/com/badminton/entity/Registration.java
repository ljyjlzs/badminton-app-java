package com.badminton.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("registrations")
public class Registration {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long activityId;

    private Long userId;

    private String nickname;

    private String avatar;

    private Integer level;

    private Long partnerId;

    private Long teamId;

    private Boolean isEliminated;

    private String cancelStatus;

    private String cancelReason;

    private LocalDateTime cancelRequestedAt;

    private LocalDateTime cancelProcessedAt;

    private Long cancelProcessedBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
