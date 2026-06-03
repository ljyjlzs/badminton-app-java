package com.badminton.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.badminton.entity.Score;
import com.badminton.entity.Team;
import com.badminton.entity.User;
import com.badminton.mapper.ScoreMapper;
import com.badminton.mapper.TeamMapper;
import com.badminton.service.ScoreService;
import com.badminton.service.UserService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScoreServiceImpl implements ScoreService {

    private final ScoreMapper scoreMapper;
    private final TeamMapper teamMapper;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void calculateAndSaveScore(Long activityId, Long matchId, String round,
                                       Long team1Id, Long team2Id,
                                       Integer team1Score, Integer team2Score) {
        // 获取队伍成员
        List<Long> team1Members = getTeamMembers(team1Id);

    List<Long> team2Members = getTeamMembers(team2Id);

        // 计算积分
        int team1ScoreChange = calculateScore(team1Score, team2Score, round);

    int team2ScoreChange = calculateScore(team2Score, team1Score, round);

        // 保存队伍1成员的积分
        for (Long userId : team1Members) {
            Score score = new Score();
            score.setActivityId(activityId);
            score.setUserId(userId);
            score.setMatchId(matchId);
            score.setSource(round);
            score.setScoreChange(team1ScoreChange);
            scoreMapper.insert(score);
        }

        // 保存队伍2成员的积分
        for (Long userId : team2Members) {
            Score score = new Score();
            score.setActivityId(activityId);
            score.setUserId(userId);
            score.setMatchId(matchId);
            score.setSource(round);
            score.setScoreChange(team2ScoreChange);
            scoreMapper.insert(score);
        }

        log.info("积分计算完成: matchId={}, team1Score={}, team2Score={}, round={}",
                matchId, team1ScoreChange, team2ScoreChange, round);
    }

    @Override
    public int calculateScore(int myScore, int opponentScore, String round) {
        switch (round) {
            case "group":
                // 小组赛：胜者+分差，败者-分差
                return myScore - opponentScore;
            case "challenge":
                // 挑战赛：胜者+10，败者-10
                return myScore > opponentScore ? 10 : -10;
            case "final":
                // 决赛：胜者+15，败者-15
                return myScore > opponentScore ? 15 : -15;
            default:
                return 0;
        }
    }

    @Override
    public Map<Long, Integer> getUserScores(Long activityId) {
        List<Map<String, Object>> scores = scoreMapper.getUserScores(activityId);

    Map<Long, Integer> result = new HashMap<>();
        for (Map<String, Object> item : scores) {
            Long userId = ((Number) item.get("user_id")).longValue();
            Integer totalScore = ((Number) item.get("total_score")).intValue();
            result.put(userId, totalScore);
        }
        return result;
    }

    @Override
    public Map<Long, Integer> getUserGroupScores(Long activityId) {
        List<Map<String, Object>> scores = scoreMapper.getUserGroupScores(activityId);
        Map<Long, Integer> result = new HashMap<>();
        for (Map<String, Object> item : scores) {
            Long userId = ((Number) item.get("user_id")).longValue();
            Integer totalScore = ((Number) item.get("total_score")).intValue();
            result.put(userId, totalScore);
        }
        return result;
    }

    @Override
    public List<Score> getByActivityAndUser(Long activityId, Long userId) {
        return scoreMapper.selectList(
                new LambdaQueryWrapper<Score>()
                        .eq(Score::getActivityId, activityId)
                        .eq(Score::getUserId, userId)
                        .orderByDesc(Score::getCreatedAt)
        );
    }

    private List<Long> getTeamMembers(Long teamId) {
        Team team = teamMapper.selectById(teamId);
        if (team == null || team.getMembers() == null) {
            return List.of();
        }

        try {
            return objectMapper.readValue(team.getMembers(), new TypeReference<List<Long>>() {});
        } catch (Exception e) {
            log.error("解析队伍成员失败: teamId={}", teamId, e);
            return List.of();
        }
    }
}
