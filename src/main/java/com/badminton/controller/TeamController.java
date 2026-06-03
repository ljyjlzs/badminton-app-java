package com.badminton.controller;

import com.badminton.common.BusinessException;
import com.badminton.common.Result;
import com.badminton.interceptor.AuthInterceptor;
import com.badminton.service.TeamService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/team")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @PutMapping("/name")
    public Result<Void> setTeamName(@RequestBody Map<String, Object> request, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute(AuthInterceptor.USER_ID_ATTRIBUTE);

        Object teamIdObj = request.get("teamId");
        Object nameObj = request.get("name");
        Object activityIdObj = request.get("activityId");
        if (teamIdObj == null || nameObj == null || activityIdObj == null) {
            throw new BusinessException("teamId、name、activityId不能为空");
        }
        Long teamId = Long.valueOf(teamIdObj.toString());
        String name = nameObj.toString();
        Long activityId = Long.valueOf(activityIdObj.toString());

        teamService.setTeamName(teamId, name, activityId, userId);
        return Result.success();
    }
}
