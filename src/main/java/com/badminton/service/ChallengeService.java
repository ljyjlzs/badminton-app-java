package com.badminton.service;

import com.badminton.vo.ChallengeDataVO;

public interface ChallengeService {    void startChallenge(Long activityId, Long userId);

    void startFinal(Long activityId, Long userId);

    Long createChallengeMatch(Long activityId, Long challengerTeamId, Long userId);

    ChallengeDataVO getChallengeData(Long activityId, Long userId);
}
