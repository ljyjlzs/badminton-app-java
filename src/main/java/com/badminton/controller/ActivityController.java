package com.badminton.controller;

import com.badminton.common.Result;
import com.badminton.dto.ActivityCreateRequest;
import com.badminton.interceptor.AuthInterceptor;
import com.badminton.service.ActivityService;
import com.badminton.vo.ActivityDetailVO;
import com.badminton.vo.ActivityVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/activity")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    @PostMapping
    public Result<Long> createActivity(@Valid @RequestBody ActivityCreateRequest request, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute(AuthInterceptor.USER_ID_ATTRIBUTE);
        Long activityId = activityService.createActivity(userId, request);
        return Result.success(activityId);
    }

    @GetMapping("/list")
    public Result<List<ActivityVO>> getActivities(
            @RequestParam String type,
            @RequestParam(required = false) String keyword,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute(AuthInterceptor.USER_ID_ATTRIBUTE);
        List<ActivityVO> activities = activityService.getActivities(userId, type, keyword);
        return Result.success(activities);
    }

    @GetMapping("/{id}")
    public Result<ActivityDetailVO> getActivityDetail(@PathVariable Long id, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute(AuthInterceptor.USER_ID_ATTRIBUTE);
        ActivityDetailVO detail = activityService.getActivityDetail(id, userId);
        return Result.success(detail);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteActivity(@PathVariable Long id, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute(AuthInterceptor.USER_ID_ATTRIBUTE);
        activityService.deleteActivity(id, userId);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateActivityStatus(@PathVariable Long id, @RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute(AuthInterceptor.USER_ID_ATTRIBUTE);
        activityService.updateActivityStatus(id, userId, request.get("status"));
        return Result.success();
    }
}
