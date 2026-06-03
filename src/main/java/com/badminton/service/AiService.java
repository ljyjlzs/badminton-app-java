package com.badminton.service;

import java.util.List;
import java.util.Map;

public interface AiService {    String chat(Long userId, String message, String actionResult);

    List<Map<String, Object>> getHistory(Long userId);

    void clearHistory(Long userId);
}
