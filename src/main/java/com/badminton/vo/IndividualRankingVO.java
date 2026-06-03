package com.badminton.vo;

import lombok.Data;

    @Data
public class IndividualRankingVO {

    private Long userId;

    private String nickname;

    private String avatar;

    private Integer level;

    private Long teamId;

    private Integer groupScore;

    private Integer challengeScore;

    private Integer finalScore;

    private Integer totalScore;

    private Integer rank;
}
