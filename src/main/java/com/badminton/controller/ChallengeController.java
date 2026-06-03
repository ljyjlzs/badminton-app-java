package com.badminton.controller;

import com.badminton.common.Result;
import com.badminton.interceptor.AuthInterceptor;
import com.badminton.service.ChallengeService;
import com.badminton.vo.ChallengeDataVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/challenge")
@RequiredArgsConstructor
public class ChallengeController {

    private final ChallengeService challengeService;

    @PostMapping("/start")
    public Result<Void> startChallenge(@RequestBody Map<String, Long> request, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute(AuthInterceptor.USER_ID_ATTRIBUTE);
        challengeService.startChallenge(request.get("activityId"), userId);
        return Result.success();
    }

    @PostMapping("/final")
    public Result<Void> startFinal(@RequestBody Map<String, Long> request, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute(AuthInterceptor.USER_ID_ATTRIBUTE);
        challengeService.startFinal(request.get("activityId"), userId);
        return Result.success();
    }

    @PostMapping("/match")
    public Result<Long> createChallengeMatch(@RequestBody Map<String, Long> request, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute(AuthInterceptor.USER_ID_ATTRIBUTE);

    Long matchId = challengeService.createChallengeMatch(
                request.get("activityId"),
                request.get("challengerTeamId"),
                userId
        );
        return Result.success(matchId);
    }

    @GetMapping("/data/{activityId}")
    public Result<ChallengeDataVO> getChallengeData(@PathVariable Long activityId, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute(AuthInterceptor.USER_ID_ATTRIBUTE);

    ChallengeDataVO data = challengeService.getChallengeData(activityId, userId);
        return Result.success(data);
    }
}
