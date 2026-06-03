package com.badminton.controller;

import com.badminton.common.BusinessException;
import com.badminton.common.Result;
import com.badminton.dto.ScoreSubmitRequest;
import com.badminton.interceptor.AuthInterceptor;
import com.badminton.service.MatchService;
import com.badminton.vo.MatchVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/match")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;

    @PostMapping("/grouping")
    public Result<Void> startGrouping(@RequestBody Map<String, Long> request, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute(AuthInterceptor.USER_ID_ATTRIBUTE);
        matchService.startGrouping(request.get("activityId"), userId);
        return Result.success();
    }

    @PostMapping("/start")
    public Result<Void> startMatch(@RequestBody Map<String, Long> request, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute(AuthInterceptor.USER_ID_ATTRIBUTE);
        matchService.startMatch(request.get("activityId"), request.get("matchId"), userId);
        return Result.success();
    }

    @PostMapping("/score")
    public Result<Void> submitScore(@Valid @RequestBody ScoreSubmitRequest request, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute(AuthInterceptor.USER_ID_ATTRIBUTE);
        matchService.submitScore(userId, request);
        return Result.success();
    }

    @PostMapping("/confirm-score")
    public Result<Void> confirmScore(@RequestBody Map<String, Object> request, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute(AuthInterceptor.USER_ID_ATTRIBUTE);

        Object activityIdObj = request.get("activityId");
        Object matchIdObj = request.get("matchId");
        Object confirmedObj = request.get("confirmed");
        if (activityIdObj == null || matchIdObj == null || confirmedObj == null) {
            throw new BusinessException("activityId、matchId、confirmed不能为空");
        }
        Long activityId = Long.valueOf(activityIdObj.toString());
        Long matchId = Long.valueOf(matchIdObj.toString());
        boolean confirmed = Boolean.parseBoolean(confirmedObj.toString());
        matchService.confirmScore(userId, activityId, matchId, confirmed);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<MatchVO> getMatchDetail(@PathVariable Long id) {
        MatchVO match = matchService.getMatchDetail(id);
        return Result.success(match);
    }
}
