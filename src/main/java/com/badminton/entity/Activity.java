package com.badminton.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("activities")
public class Activity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private LocalDateTime time;

    private String location;

    private BigDecimal latitude;

    private BigDecimal longitude;

    private Long organizerId;

    private String type;

    private String status;

    private Integer minPlayers;

    private Integer maxPlayers;

    private Integer currentPlayers;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
