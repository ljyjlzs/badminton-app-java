package com.badminton.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class JoinActivityRequest {

    private Long activityId;

    private String activityName;

    @NotBlank(message = "昵称不能为空")
    private String nickname;

    @NotNull(message = "等级不能为空")
    @Min(value = 1, message = "等级最小为1")
    @Max(value = 10, message = "等级最大为10")
    private Integer level;

    private String avatar;

    private Long partnerId;
}
