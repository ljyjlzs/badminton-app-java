package com.badminton.service;

import com.badminton.entity.Score;

import java.util.List;
import java.util.Map;

public interface ScoreService {    void calculateAndSaveScore(Long activityId, Long matchId, String round,
                               Long team1Id, Long team2Id,
                               Integer team1Score, Integer team2Score);

    int calculateScore(int myScore, int opponentScore, String round);

    Map<Long, Integer> getUserScores(Long activityId);

    List<Score> getByActivityAndUser(Long activityId, Long userId);
}
