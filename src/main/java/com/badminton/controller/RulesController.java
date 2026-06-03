package com.badminton.controller;

import com.badminton.common.Result;
import com.badminton.service.RulesService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rules")
@RequiredArgsConstructor
public class RulesController {

    private final RulesService rulesService;

    @GetMapping
    public Result<List<Map<String, Object>>> getRules() {
        List<Map<String, Object>> rules = rulesService.getRules();
        return Result.success(rules);
    }

    @PostMapping("/init")
    public Result<Void> initRules() {
        rulesService.initRules();
        return Result.success();
    }
}
