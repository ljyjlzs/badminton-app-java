package com.badminton.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

    @Data
public class ActivityCreateRequest {

    @NotBlank(message = "活动名称不能为空")
    @Size(max = 50, message = "活动名称不能超过50个字符")
    private String name;

    @NotNull(message = "活动时间不能为空")
    private LocalDateTime time;

    @NotBlank(message = "活动地点不能为空")
    @Size(max = 100, message = "活动地点不能超过100个字符")
    private String location;

    private BigDecimal latitude;

    private BigDecimal longitude;

    private String type = "doubles";
}
