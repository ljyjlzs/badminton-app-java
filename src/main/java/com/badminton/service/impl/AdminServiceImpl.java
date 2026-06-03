package com.badminton.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.badminton.common.BusinessException;
import com.badminton.entity.*;
import com.badminton.mapper.*;
import com.badminton.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserMapper userMapper;
    private final ActivityMapper activityMapper;
    private final RegistrationMapper registrationMapper;
    private final TeamMapper teamMapper;
    private final MatchMapper matchMapper;
    private final ScoreMapper scoreMapper;
    private final AiMessageMapper aiMessageMapper;
    private final RulesMapper rulesMapper;

    @Value("${admin.confirm-code}")
    private String confirmCode;

    @Override
    @Transactional
    public Map<String, Object> clearDatabase(Long userId, String confirmCode) {
        User user = userMapper.selectById(userId);
        if (user == null || !"admin".equals(user.getRole())) {
            throw BusinessException.forbidden();
        }

        if (!this.confirmCode.equals(confirmCode)) {
            throw new BusinessException("确认码错误");
        }

        Map<String, Object> results = new LinkedHashMap<>();

        log.warn("开始清空所有数据...");

        // 按照外键依赖顺序删除
        results.put("scores", clearTable("scores", scoreMapper, Score.class));
        results.put("matches", clearTable("matches", matchMapper, Match.class));
        results.put("teams", clearTable("teams", teamMapper, Team.class));
        results.put("registrations", clearTable("registrations", registrationMapper, Registration.class));
        results.put("ai_messages", clearTable("ai_messages", aiMessageMapper, AiMessage.class));
        results.put("activities", clearTable("activities", activityMapper, Activity.class));
        results.put("rules", clearTable("rules", rulesMapper, Rules.class));
        results.put("users", clearTable("users", userMapper, User.class));

        log.warn("数据清空完成");

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "数据清空完成");
        response.put("details", results);
        return response;
    }

    private <T> Map<String, Object> clearTable(String tableName, com.baomidou.mybatisplus.core.mapper.BaseMapper<T> mapper, Class<T> entityClass) {
        try {
            // 使用 delete(null) 删除所有记录
            long count = mapper.selectCount(null);
            if (count > 0) {
                mapper.delete(null);
            }
            log.info("清空表 {}: 删除 {} 条记录", tableName, count);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("deleted", count);
            return result;
        } catch (Exception e) {
            log.error("清空表 {} 失败: {}", tableName, e.getMessage());
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", e.getMessage());
            return result;
        }
    }
}
