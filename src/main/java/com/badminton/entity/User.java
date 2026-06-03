package com.badminton.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

    @Data
@TableName("users")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String openid;

    private String unionId;

    private String nickname;

    private String avatar;

    private Integer level;

    private String phone;

    private String role;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
