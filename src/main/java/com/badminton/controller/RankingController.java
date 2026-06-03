package com.badminton.controller;

import com.badminton.common.Result;
import com.badminton.service.RankingService;
import com.badminton.vo.RankingVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ranking")
@RequiredArgsConstructor
public class RankingController {

    private final RankingService rankingService;

    @GetMapping("/{activityId}")
    public Result<RankingVO> getRankings(
            @PathVariable Long activityId,

    @RequestParam(defaultValue = "all") String type) {
        RankingVO rankings = rankingService.getRankings(activityId, type);
        return Result.success(rankings);
    }
}
