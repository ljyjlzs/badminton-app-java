package com.badminton.controller;

import com.badminton.common.BusinessException;
import com.badminton.common.Result;
import com.badminton.dto.LoginRequest;
import com.badminton.interceptor.AuthInterceptor;
import com.badminton.service.UserService;
import com.badminton.vo.LoginVO;
import com.badminton.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        LoginVO loginVO = userService.login(request);
        return Result.success(loginVO);
    }

    @PutMapping("/info")
    public Result<Void> updateUserInfo(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute(AuthInterceptor.USER_ID_ATTRIBUTE);
        userService.updateUserInfo(userId, request.get("nickname"), request.get("avatar"));
        return Result.success();
    }

    @PutMapping("/avatar")
    public Result<Void> updateAvatar(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute(AuthInterceptor.USER_ID_ATTRIBUTE);
        userService.updateAvatar(userId, request.get("avatar"));
        return Result.success();
    }

    @PutMapping("/level")
    public Result<Void> updateLevel(@RequestBody Map<String, Integer> request, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute(AuthInterceptor.USER_ID_ATTRIBUTE);
        Integer level = request.get("level");
        if (level == null) {
            throw new BusinessException("level不能为空");
        }
        userService.updateLevel(userId, level);
        return Result.success();
    }
}
