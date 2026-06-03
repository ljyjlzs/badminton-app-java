package com.badminton.controller;

import com.badminton.common.Result;
import com.badminton.interceptor.AuthInterceptor;
import com.badminton.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @PostMapping("/clear-database")
    public Result<Map<String, Object>> clearDatabase(@RequestBody Map<String, String> request,
                                                     HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute(AuthInterceptor.USER_ID_ATTRIBUTE);
        String confirmCode = request.get("confirmCode");

        Map<String, Object> result = adminService.clearDatabase(userId, confirmCode);
        return Result.success(result);
    }
}
