package com.badminton.vo;

import lombok.Data;

import java.util.List;

    @Data
public class RankingVO {

    private List<IndividualRankingVO> individualRankings;

    private List<TeamRankingVO> teamRankings;
}
