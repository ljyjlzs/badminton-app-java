package com.badminton.vo;

import lombok.Data;

import java.util.List;

@Data
public class ChallengeDataVO {

    private List<ChallengeTeamVO> qualifiedTeams;

    private List<EliminatedPlayerVO> eliminatedPlayers;

    private ChallengeMatchVO challengeMatch;

    private Boolean isOrganizer;

    @Data
    public static class ChallengeTeamVO {
        private Long id;
        private Long activityId;
        private String name;
        private String members;
        private String memberNames;
        private List<String> memberAvatars;
        private Boolean isEliminated;
    }

    @Data
    public static class EliminatedPlayerVO {
        private Long userId;
        private String nickname;
        private String avatar;
        private Integer level;
    }

    @Data
    public static class ChallengeMatchVO {
        private Long id;
        private Long activityId;
        private String round;
        private Long team1Id;
        private Long team2Id;
        private Integer team1Score;
        private Integer team2Score;
        private String status;
        private String team1Name;
        private String team2Name;
        private String team1Members;
        private String team2Members;
    }
}
