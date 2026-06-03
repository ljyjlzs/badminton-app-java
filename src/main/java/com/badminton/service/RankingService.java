package com.badminton.service;

import com.badminton.vo.RankingVO;

public interface RankingService {    RankingVO getRankings(Long activityId, String type);
}
