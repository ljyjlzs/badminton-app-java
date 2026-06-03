package com.badminton.vo;

import lombok.Data;

    @Data
public class MatchVO {

    private Long id;

    private Long activityId;

    private String round;

    private Integer roundOrder;

    private String court;

    private Long team1Id;

    private Long team2Id;

    private String team1Name;

    private String team2Name;

    private Integer team1Score;

    private Integer team2Score;

    private String status;
}
