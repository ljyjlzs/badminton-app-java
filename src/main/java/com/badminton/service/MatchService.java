package com.badminton.service;

import com.badminton.dto.ScoreSubmitRequest;
import com.badminton.entity.Match;
import com.badminton.vo.MatchVO;

import java.util.List;

public interface MatchService {
    void startGrouping(Long activityId, Long userId);

    void startMatch(Long activityId, Long matchId, Long userId);

    void submitScore(Long userId, ScoreSubmitRequest request);

    void confirmScore(Long userId, Long activityId, Long matchId, boolean confirmed);

    void modifyScore(Long userId, ScoreSubmitRequest request);

    MatchVO getMatchDetail(Long matchId);

    List<Match> getByActivityId(Long activityId);

    List<MatchVO> getMatchList(Long activityId);
}
