package com.badminton.vo;

import lombok.Data;

import java.util.List;

    @Data
public class TeamRankingVO {

    private Long teamId;

    private String name;

    private Long captainId;

    private List<TeamRankingMemberVO> members;

    private Integer totalScore;

    private Integer groupScore;

    private Integer challengeScore;

    private Integer rank;
}
