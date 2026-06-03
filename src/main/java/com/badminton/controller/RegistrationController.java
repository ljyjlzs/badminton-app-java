package com.badminton.controller;

import com.badminton.common.BusinessException;
import com.badminton.common.Result;
import com.badminton.dto.JoinActivityRequest;
import com.badminton.interceptor.AuthInterceptor;
import com.badminton.service.RegistrationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/registration")
@RequiredArgsConstructor
public class RegistrationController {

    private final RegistrationService registrationService;

    @PostMapping("/join")
    public Result<Long> joinActivity(@Valid @RequestBody JoinActivityRequest request, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute(AuthInterceptor.USER_ID_ATTRIBUTE);

    Long registrationId = registrationService.joinActivity(userId, request);
        return Result.success(registrationId);
    }

    @PostMapping("/cancel")
    public Result<Void> cancelRegistration(@RequestBody Map<String, Object> request, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute(AuthInterceptor.USER_ID_ATTRIBUTE);

        Object activityIdObj = request.get("activityId");
        if (activityIdObj == null) {
            throw new BusinessException("activityId不能为空");
        }
        Long activityId = Long.valueOf(activityIdObj.toString());
        String reason = request.get("reason") != null ? request.get("reason").toString() : null;
        registrationService.cancelRegistration(userId, activityId, reason);
        return Result.success();
    }

    @PostMapping("/handle-cancel")
    public Result<Void> handleCancelRequest(@RequestBody Map<String, Object> request, HttpServletRequest httpRequest) {
        Long operatorId = (Long) httpRequest.getAttribute(AuthInterceptor.USER_ID_ATTRIBUTE);

        Object registrationIdObj = request.get("registrationId");
        Object actionObj = request.get("action");
        if (registrationIdObj == null || actionObj == null) {
            throw new BusinessException("registrationId、action不能为空");
        }
        Long registrationId = Long.valueOf(registrationIdObj.toString());
        String action = actionObj.toString();
        registrationService.handleCancelRequest(registrationId, operatorId, action);
        return Result.success();
    }
}
