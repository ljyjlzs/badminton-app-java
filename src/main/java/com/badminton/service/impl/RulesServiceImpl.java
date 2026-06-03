package com.badminton.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.badminton.entity.Rules;
import com.badminton.mapper.RulesMapper;
import com.badminton.service.RulesService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RulesServiceImpl implements RulesService {

    private final RulesMapper rulesMapper;
    private final ObjectMapper objectMapper;

    // 默认规则数据
    private static final List<Map<String, Object>> DEFAULT_SECTIONS = List.of(
            createSection("singles", "单打", "🏸",
                    List.of(
                            "每场比赛采用21分制",
                            "先得21分者获胜",
                            "每人单独对阵，无搭档",
                            "比分需要双方确认后生效",
                            "比赛采用单循环赛制"
                    ),
                    List.of(
                            "胜者积分 = 自己的分数 - 对手的分数",
                            "败者积分 = 自己的分数 - 对手的分数（负数）",
                            "示例：比分21-15，胜者得+6分，败者得-6分",
                            "示例：比分21-19，胜者得+2分，败者得-2分"
                    ),
                    List.of(
                            "所有小组赛结束后，积分最高的选手进入挑战赛",
                            "挑战赛：胜者+10分，败者-10分",
                            "挑战赛后进入决赛：胜者+15分，败者-15分"
                    )
            ),
            createSection("doubles", "双打轮换", "🏸",
                    List.of(
                            "每场比赛采用21分制",
                            "系统自动均衡分组：高等级+低等级配对",
                            "每场比赛4人（2支队伍，每队2人）",
                            "比分需要双方确认后生效",
                            "搭档由系统随机分配，每轮可能不同"
                    ),
                    List.of(
                            "个人积分 = 自己的分数 - 对手的分数",
                            "队伍积分 = 队伍成员积分总和",
                            "示例：队伍A以21-15获胜，每人得+6分",
                            "示例：队伍B以15-21落败，每人得-6分"
                    ),
                    List.of(
                            "分组算法：按等级排序，最高+最低配对",
                            "所有小组赛结束后，积分最高的2人进入挑战赛",
                            "挑战赛：胜者+10分，败者-10分",
                            "挑战赛后进入决赛：胜者+15分，败者-15分"
                    )
            ),
            createSection("fixed-doubles", "双打固搭", "🤝",
                    List.of(
                            "每场比赛采用21分制",
                            "报名时自选搭档，全程固定搭配",
                            "每场比赛4人（2支队伍，每队2人）",
                            "比分需要双方确认后生效",
                            "搭档关系在整个活动中不变"
                    ),
                    List.of(
                            "个人积分 = 自己的分数 - 对手的分数",
                            "队伍积分 = 队伍成员积分总和",
                            "示例：队伍A以21-15获胜，每人得+6分",
                            "示例：队伍B以15-21落败，每人得-6分"
                    ),
                    List.of(
                            "报名时需选择搭档，搭档需已报名",
                            "所有小组赛结束后，积分最高的2人进入挑战赛",
                            "挑战赛：胜者+10分，败者-10分",
                            "挑战赛后进入决赛：胜者+15分，败者-15分"
                    )
            )
    );

    @Override
    public List<Map<String, Object>> getRules() {
        try {
            Rules rules = rulesMapper.selectOne(
                    new LambdaQueryWrapper<Rules>().eq(Rules::getType, "rules")
            );

            if (rules != null && rules.getSections() != null) {
                return objectMapper.readValue(rules.getSections(), new TypeReference<List<Map<String, Object>>>() {});
            }
        } catch (Exception e) {
            log.warn("从数据库读取规则失败，使用默认规则", e);
        }

        return DEFAULT_SECTIONS;
    }

    @Override
    @Transactional
    public void initRules() {
        try {
            // 检查是否已存在规则数据
            Rules existing = rulesMapper.selectOne(
                    new LambdaQueryWrapper<Rules>().eq(Rules::getType, "rules")
            );

            String sectionsJson = objectMapper.writeValueAsString(DEFAULT_SECTIONS);

            if (existing != null) {
                // 更新现有数据
                existing.setTitle("比赛规则");
                existing.setSections(sectionsJson);
                existing.setUpdatedAt(LocalDateTime.now());
                rulesMapper.updateById(existing);
                log.info("规则数据已更新");
            } else {
                // 创建新数据
                Rules rules = new Rules();
                rules.setType("rules");
                rules.setTitle("比赛规则");
                rules.setSections(sectionsJson);
                rules.setCreatedAt(LocalDateTime.now());
                rules.setUpdatedAt(LocalDateTime.now());
                rulesMapper.insert(rules);
                log.info("规则数据已初始化");
            }
        } catch (Exception e) {
            log.error("初始化规则失败", e);
            throw new RuntimeException("初始化规则失败: " + e.getMessage());
        }
    }

    private static Map<String, Object> createSection(String id, String name, String icon,
                                                      List<String> matchRules,
                                                      List<String> scoringRules,
                                                      List<String> commonRules) {
        Map<String, Object> section = new HashMap<>();
        section.put("id", id);
        section.put("name", name);
        section.put("icon", icon);
        section.put("match_rules", matchRules);
        section.put("scoring_rules", scoringRules);
        section.put("common_rules", commonRules);
        return section;
    }
}
