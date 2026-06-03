package com.badminton.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.badminton.entity.Activity;
import com.badminton.entity.AiMessage;
import com.badminton.entity.Registration;
import com.badminton.entity.User;
import com.badminton.mapper.AiMessageMapper;
import com.badminton.service.ActivityService;
import com.badminton.service.AiService;
import com.badminton.service.RegistrationService;
import com.badminton.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final AiMessageMapper aiMessageMapper;
    private final UserService userService;
    private final ActivityService activityService;
    private final RegistrationService registrationService;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Value("${ai.api-key}")
    private String apiKey;

    @Value("${ai.api-url}")
    private String apiUrl;

    @Override
    @Transactional
    public String chat(Long userId, String message, String actionResult) {
        // 保存用户消息
        AiMessage userMsg = new AiMessage();
        userMsg.setUserId(userId);
        userMsg.setRole("user");
        userMsg.setContent(actionResult != null ? "[系统通知] 操作执行结果：" + actionResult : message);
        aiMessageMapper.insert(userMsg);

        // 获取历史消息
        List<AiMessage> history = aiMessageMapper.selectList(
                new LambdaQueryWrapper<AiMessage>()
                        .eq(AiMessage::getUserId, userId)
                        .orderByDesc(AiMessage::getCreatedAt)
                        .last("LIMIT 12")
        );
        Collections.reverse(history);

        // 构建上下文
        String context = buildUserContext(userId);

        // 构建API请求
        List<Map<String, String>> messages = new ArrayList<>();

        // 系统消息
        Map<String, String> systemMsg = new HashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", context);
        messages.add(systemMsg);

        // 历史消息
        for (AiMessage msg : history) {
            Map<String, String> apiMsg = new HashMap<>();
            apiMsg.put("role", msg.getRole());
            apiMsg.put("content", msg.getContent());
            messages.add(apiMsg);
        }

        // 调用AI API
        String aiReply = callAiApi(messages);

        // 保存AI回复
        AiMessage aiMsg = new AiMessage();
        aiMsg.setUserId(userId);
        aiMsg.setRole("assistant");
        aiMsg.setContent(aiReply);
        aiMessageMapper.insert(aiMsg);

        return aiReply;
    }

    @Override
    public List<Map<String, Object>> getHistory(Long userId) {
        List<AiMessage> messages = aiMessageMapper.selectList(
                new LambdaQueryWrapper<AiMessage>()
                        .eq(AiMessage::getUserId, userId)
                        .ne(AiMessage::getRole, "system_feedback")
                        .orderByAsc(AiMessage::getCreatedAt)
        );

        return messages.stream()
                .map(msg -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("role", msg.getRole());
                    map.put("content", msg.getContent());
                    map.put("created_at", msg.getCreatedAt());
                    return map;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void clearHistory(Long userId) {
        aiMessageMapper.delete(
                new LambdaQueryWrapper<AiMessage>()
                        .eq(AiMessage::getUserId, userId)
        );
    }

    private String buildUserContext(Long userId) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是小羽，一个智能AI助手。你既可以回答各种日常问题，也可以帮助用户管理羽毛球活动。\n\n");
        sb.append("你的能力：\n");
        sb.append("1. 回答各种问题：知识问答、聊天、写作、翻译、计算等\n");
        sb.append("2. 管理羽毛球活动：查看活动、报名、查看比分、查看排名等\n\n");
        sb.append("请用友好、简洁的语气回答。如果是羽毛球相关问题，可以主动提供活动信息。\n\n");

        // 用户信息
        User user = userService.getById(userId);
        if (user != null) {
            sb.append("【用户信息】昵称：").append(user.getNickname() != null ? user.getNickname() : "未知");
            sb.append("，等级：").append(user.getLevel()).append("\n\n");
        }

        // 可报名的活动
        List<Activity> activities = activityService.getActivities(userId, "available", null)
                .stream()
                .map(vo -> activityService.getById(vo.getId()))
                .collect(Collectors.toList());

        if (!activities.isEmpty()) {
            sb.append("【可报名的活动】(").append(activities.size()).append("个)\n");
            for (Activity act : activities) {
                sb.append("- 活动ID: ").append(act.getId()).append("\n");
                sb.append("  名称: ").append(act.getName());
                sb.append("，类型: ").append(act.getType());
                sb.append("，时间: ").append(act.getTime());
                sb.append("，地点: ").append(act.getLocation()).append("\n");
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    private String callAiApi(List<Map<String, String>> messages) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

    Map<String, Object> body = new HashMap<>();
            body.put("model", "mimo-v2.5-pro");
            body.put("messages", messages);
            body.put("temperature", 0.7);
            body.put("max_tokens", 1000);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.exchange(apiUrl, HttpMethod.POST, request, Map.class);

            if (response.getBody() != null) {
                List<Map> choices = (List<Map>) response.getBody().get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    return (String) message.get("content");
                }
            }

            return "抱歉，AI暂时无法响应，请稍后重试。";
        } catch (Exception e) {
            log.error("调用AI API失败", e);
            return "抱歉，AI服务异常，请稍后重试。";
        }
    }
}
