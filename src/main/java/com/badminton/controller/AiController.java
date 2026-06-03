package com.badminton.controller;

import com.badminton.common.Result;
import com.badminton.interceptor.AuthInterceptor;
import com.badminton.service.AiService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @PostMapping("/chat")
    public Result<Map<String, String>> chat(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute(AuthInterceptor.USER_ID_ATTRIBUTE);

    String message = request.get("message");

    String actionResult = request.get("action_result");

    String reply = aiService.chat(userId, message, actionResult);
        return Result.success(Map.of("content", reply));
    }

    @GetMapping("/history")
    public Result<List<Map<String, Object>>> getHistory(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute(AuthInterceptor.USER_ID_ATTRIBUTE);

    List<Map<String, Object>> history = aiService.getHistory(userId);
        return Result.success(history);
    }

    @DeleteMapping("/history")
    public Result<Void> clearHistory(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute(AuthInterceptor.USER_ID_ATTRIBUTE);
        aiService.clearHistory(userId);
        return Result.success();
    }
}
