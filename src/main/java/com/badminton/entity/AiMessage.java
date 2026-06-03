package com.badminton.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_messages")
public class AiMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String role;

    private String content;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
