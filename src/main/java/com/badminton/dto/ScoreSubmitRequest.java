package com.badminton.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

    @Data
public class ScoreSubmitRequest {

    @NotNull(message = "活动ID不能为空")
    private Long activityId;

    @NotNull(message = "比赛ID不能为空")
    private Long matchId;

    @NotNull(message = "队伍1得分不能为空")
    @Min(value = 0, message = "得分最小为0")
    @Max(value = 30, message = "得分最大为30")
    private Integer team1Score;

    @NotNull(message = "队伍2得分不能为空")
    @Min(value = 0, message = "得分最小为0")
    @Max(value = 30, message = "得分最大为30")
    private Integer team2Score;
}
