package com.badminton.controller;

import com.badminton.common.Result;
import com.badminton.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Profile("dev")
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {

    private final JwtUtil jwtUtil;

    @PostMapping("/token")
    public Result<Map<String, String>> generateToken(@RequestBody Map<String, Object> request) {
        Long userId = Long.valueOf(request.get("userId").toString());
        String openid = request.get("openid").toString();

    String token = jwtUtil.generateToken(userId, openid);
        return Result.success(Map.of("token", token));
    }
}
