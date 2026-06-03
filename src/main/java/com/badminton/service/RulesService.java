package com.badminton.service;

import java.util.List;
import java.util.Map;

public interface RulesService {    List<Map<String, Object>> getRules();

    void initRules();
}
