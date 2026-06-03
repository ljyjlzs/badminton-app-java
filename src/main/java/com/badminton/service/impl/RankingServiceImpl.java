package com.badminton.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.badminton.entity.Registration;
import com.badminton.entity.Score;
import com.badminton.entity.Team;
import com.badminton.entity.User;
import com.badminton.mapper.ScoreMapper;
import com.badminton.mapper.TeamMapper;
import com.badminton.mapper.UserMapper;
import com.badminton.service.RankingService;
import com.badminton.service.RegistrationService;
import com.badminton.vo.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RankingServiceImpl implements RankingService {

    private final RegistrationService registrationService;
    private final ScoreMapper scoreMapper;
    private final TeamMapper teamMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    @Override
    public RankingVO getRankings(Long activityId, String type) {
        RankingVO result = new RankingVO();

        // 1. 获取活动的所有报名用户
        List<Registration> registrations = registrationService.getByActivityId(activityId);

        // 构建报名用户ID -> 昵称映射
        Map<Long, String> regNicknames = new HashMap<>();
        for (Registration reg : registrations) {
            regNicknames.put(reg.getUserId(), reg.getNickname());
        }

        // 获取报名用户的用户信息
        Set<Long> userIds = registrations.stream()
                .map(Registration::getUserId)
                .collect(Collectors.toSet());
        Map<Long, User> usersMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(userIds);
            for (User user : users) {
                usersMap.put(user.getId(), user);
            }
        }

        // 2. 获取所有分数记录
        List<Score> scores = scoreMapper.selectList(
                new LambdaQueryWrapper<Score>()
                        .eq(Score::getActivityId, activityId)
        );

        // 3. 计算个人总分（按 source 分类）
        Map<Long, int[]> userTotalScores = new HashMap<>(); // [total, group, challenge, final]
        for (Score s : scores) {
            int[] arr = userTotalScores.computeIfAbsent(s.getUserId(), k -> new int[4]);
            arr[0] += s.getScoreChange();
            switch (s.getSource()) {
                case "group":
                    arr[1] += s.getScoreChange();
                    break;
                case "challenge":
                    arr[2] += s.getScoreChange();
                    break;
                case "final":
                    arr[3] += s.getScoreChange();
                    break;
            }
        }

        // 构建个人排行榜
        if ("individual".equals(type) || "all".equals(type)) {
            List<IndividualRankingVO> individualRankings = new ArrayList<>();
            for (Registration reg : registrations) {
                int[] scoresArr = userTotalScores.getOrDefault(reg.getUserId(), new int[4]);
                User user = usersMap.get(reg.getUserId());

                IndividualRankingVO vo = new IndividualRankingVO();
                vo.setUserId(reg.getUserId());
                vo.setNickname(reg.getNickname() != null ? reg.getNickname() :
                        (user != null ? user.getNickname() : ""));
                vo.setAvatar(reg.getAvatar() != null ? reg.getAvatar() :
                        (user != null ? user.getAvatar() : ""));
                vo.setLevel(reg.getLevel() != null ? reg.getLevel() : 5);
                vo.setTeamId(reg.getTeamId());
                vo.setGroupScore(scoresArr[1]);
                vo.setChallengeScore(scoresArr[2]);
                vo.setFinalScore(scoresArr[3]);
                vo.setTotalScore(scoresArr[0]);
                individualRankings.add(vo);
            }

            // 排序: 总分降序，昵称升序
            individualRankings.sort((a, b) -> {
                if (!Objects.equals(b.getTotalScore(), a.getTotalScore())) {
                    return b.getTotalScore() - a.getTotalScore();
                }
                return String.CASE_INSENSITIVE_ORDER.compare(
                        a.getNickname() != null ? a.getNickname() : "",
                        b.getNickname() != null ? b.getNickname() : "");
            });

            // 添加排名
            for (int i = 0; i < individualRankings.size(); i++) {
                individualRankings.get(i).setRank(i + 1);
            }
            result.setIndividualRankings(individualRankings);
        }

        // 4. 计算队伍总分
        if ("team".equals(type) || "all".equals(type)) {
            List<Team> teams = teamMapper.selectList(
                    new LambdaQueryWrapper<Team>()
                            .eq(Team::getActivityId, activityId)
            );

            // 构建 userId -> teamId 映射（通过 registrations）
            Map<Long, Long> userTeamMap = new HashMap<>();
            for (Registration reg : registrations) {
                if (reg.getTeamId() != null) {
                    userTeamMap.put(reg.getUserId(), reg.getTeamId());
                }
            }

            List<TeamRankingVO> teamRankings = new ArrayList<>();
            for (Team team : teams) {
                // 解析队伍成员
                List<Long> memberIds = parseTeamMembers(team.getMembers());

                // 计算队伍分数（按成员去重累加）
                Map<Long, Integer> uniqueUserScores = new HashMap<>();
                Map<Long, String> userSourceMap = new HashMap<>();
                for (Score s : scores) {
                    if (memberIds.contains(s.getUserId())) {
                        uniqueUserScores.merge(s.getUserId(), s.getScoreChange(), Integer::sum);
                        userSourceMap.putIfAbsent(s.getUserId(), s.getSource());
                    }
                }

                int totalScore = 0;
                int groupScore = 0;
                int challengeScore = 0;
                for (Map.Entry<Long, Integer> entry : uniqueUserScores.entrySet()) {
                    int scoreVal = entry.getValue();
                    totalScore += scoreVal;
                    String source = userSourceMap.getOrDefault(entry.getKey(), "group");
                    if ("group".equals(source)) {
                        groupScore += scoreVal;
                    } else if ("challenge".equals(source)) {
                        challengeScore += scoreVal;
                    }
                }

                // 构建成员信息
                List<TeamRankingMemberVO> members = new ArrayList<>();
                for (Long memberId : memberIds) {
                    TeamRankingMemberVO memberVO = new TeamRankingMemberVO();
                    memberVO.setUserId(memberId);
                    memberVO.setNickname(regNicknames.getOrDefault(memberId,
                            usersMap.containsKey(memberId) ? usersMap.get(memberId).getNickname() : "未知用户"));
                    // 查找该成员的报名记录获取 avatar 和 level
                    Registration memberReg = registrations.stream()
                            .filter(r -> r.getUserId().equals(memberId))
                            .findFirst().orElse(null);
                    memberVO.setAvatar(memberReg != null && memberReg.getAvatar() != null ? memberReg.getAvatar() :
                            (usersMap.containsKey(memberId) ? usersMap.get(memberId).getAvatar() : ""));
                    memberVO.setLevel(memberReg != null && memberReg.getLevel() != null ? memberReg.getLevel() :
                            (usersMap.containsKey(memberId) ? usersMap.get(memberId).getLevel() : 5));
                    members.add(memberVO);
                }

                TeamRankingVO teamVO = new TeamRankingVO();
                teamVO.setTeamId(team.getId());
                teamVO.setName(team.getName() != null ? team.getName() : "待命名队伍");
                teamVO.setMembers(members);
                teamVO.setTotalScore(totalScore);
                teamVO.setGroupScore(groupScore);
                teamVO.setChallengeScore(challengeScore);
                teamRankings.add(teamVO);
            }

            // 排序: 总分降序，名称升序
            teamRankings.sort((a, b) -> {
                if (!Objects.equals(b.getTotalScore(), a.getTotalScore())) {
                    return b.getTotalScore() - a.getTotalScore();
                }
                return String.CASE_INSENSITIVE_ORDER.compare(
                        a.getName() != null ? a.getName() : "",
                        b.getName() != null ? b.getName() : "");
            });

            // 添加排名
            for (int i = 0; i < teamRankings.size(); i++) {
                teamRankings.get(i).setRank(i + 1);
            }
            result.setTeamRankings(teamRankings);
        }

        return result;
    }    private List<Long> parseTeamMembers(String membersJson) {
        if (membersJson == null || membersJson.isEmpty()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(membersJson, new TypeReference<List<Long>>() {});
        } catch (Exception e) {
            log.error("解析队伍成员失败: {}", membersJson, e);
            return List.of();
        }
    }
}
