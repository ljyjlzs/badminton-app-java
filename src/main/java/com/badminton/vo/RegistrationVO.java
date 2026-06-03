package com.badminton.vo;

import lombok.Data;

    @Data
public class RegistrationVO {

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
}
