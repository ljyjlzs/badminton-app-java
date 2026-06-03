package com.badminton.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.badminton.common.BusinessException;
import com.badminton.dto.ScoreSubmitRequest;
import com.badminton.entity.*;
import com.badminton.mapper.MatchMapper;
import com.badminton.mapper.TeamMapper;
import com.badminton.service.*;
import com.badminton.vo.MatchVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchServiceImpl implements MatchService {

    private final MatchMapper matchMapper;
    private final TeamMapper teamMapper;
    private final ActivityService activityService;
    private final RegistrationService registrationService;
    private final ScoreService scoreService;
    private final UserService userService;

    @Override
    @Transactional
    public void startGrouping(Long activityId, Long userId) {
        Activity activity = activityService.getById(activityId);
        if (activity == null) {
            throw BusinessException.notFound("活动");
        }

        if (!activity.getOrganizerId().equals(userId)) {
            throw BusinessException.forbidden();
        }

        if (!"registering".equals(activity.getStatus())) {
            throw new BusinessException("活动状态不允许分组");
        }

        // 获取所有报名用户
        List<Registration> registrations = registrationService.getByActivityId(activityId);
        if (registrations.isEmpty()) {
            throw new BusinessException("没有报名用户");
        }

        // 验证双打模式需要偶数人数
        if (!"singles".equals(activity.getType()) && registrations.size() % 2 != 0) {
            throw new BusinessException("双打比赛需要偶数人数");
        }

        // 执行分组算法
        List<Team> teams = performGrouping(registrations, activity.getType());

        // 保存队伍
        for (Team team : teams) {
            team.setActivityId(activityId);
            teamMapper.insert(team);
        }

        // 创建比赛
        createMatches(activityId, teams);

        // 更新活动状态
        activityService.updateActivityStatus(activityId, userId, "grouping");

        log.info("分组完成: activityId={}, teams={}", activityId, teams.size());
    }

    @Override
    @Transactional
    public void startMatch(Long activityId, Long matchId, Long userId) {
        Activity activity = activityService.getById(activityId);
        if (activity == null) {
            throw BusinessException.notFound("活动");
        }

        if (!activity.getOrganizerId().equals(userId)) {
            throw BusinessException.forbidden();
        }

        Match match = matchMapper.selectById(matchId);
        if (match == null) {
            throw BusinessException.notFound("比赛");
        }

        if (!"pending".equals(match.getStatus())) {
            throw new BusinessException("比赛已经开始或结束");
        }

        match.setStatus("playing");
        matchMapper.updateById(match);

        log.info("比赛开始: matchId={}", matchId);
    }

    @Override
    @Transactional
    public void submitScore(Long userId, ScoreSubmitRequest request) {
        Match match = matchMapper.selectById(request.getMatchId());
        if (match == null) {
            throw BusinessException.notFound("比赛");
        }

        // 校验提交者是否是参赛队员
        boolean isTeamMember = isUserInTeam(userId, match.getTeam1Id()) || isUserInTeam(userId, match.getTeam2Id());
        if (!isTeamMember) {
            throw BusinessException.forbidden();
        }

        // 验证比分范围
        if (request.getTeam1Score() < 0 || request.getTeam1Score() > 30 ||
            request.getTeam2Score() < 0 || request.getTeam2Score() > 30) {
            throw new BusinessException("比分必须在0-30之间");
        }

        // 更新比赛比分，状态设为 confirming 等待对手确认
        match.setTeam1Score(request.getTeam1Score());
        match.setTeam2Score(request.getTeam2Score());
        match.setStatus("confirming");
        matchMapper.updateById(match);

        log.info("比分提交: matchId={}, {}-{}, 等待对手确认", request.getMatchId(), request.getTeam1Score(), request.getTeam2Score());
    }    private boolean isUserInTeam(Long userId, Long teamId) {
        if (teamId == null) return false;
        Team team = teamMapper.selectById(teamId);
        if (team == null || team.getMembers() == null) return false;
        // members 格式如 "[1,2]" 或 "[1]"
        String membersStr = team.getMembers().replaceAll("[\\[\\]\\s]", "");
        if (membersStr.isEmpty()) return false;
        for (String part : membersStr.split(",")) {
            if (Long.parseLong(part.trim()) == userId) {
                return true;
            }
        }
        return false;
    }

    @Override
    @Transactional
    public void confirmScore(Long userId, Long activityId, Long matchId, boolean confirmed) {
        Match match = matchMapper.selectById(matchId);
        if (match == null) {
            throw BusinessException.notFound("比赛");
        }

        if (!"confirming".equals(match.getStatus())) {
            throw new BusinessException("比赛状态不允许确认");
        }

        if (confirmed) {
            match.setStatus("confirmed");
            matchMapper.updateById(match);

            // 计算并保存积分
            scoreService.calculateAndSaveScore(
                    activityId,
                    matchId,
                    match.getRound(),
                    match.getTeam1Id(),
                    match.getTeam2Id(),
                    match.getTeam1Score(),
                    match.getTeam2Score()
            );

            log.info("比分确认: matchId={}", matchId);
        } else {
            match.setStatus("pending");
            match.setTeam1Score(null);
            match.setTeam2Score(null);
            matchMapper.updateById(match);

            log.info("比分拒绝: matchId={}", matchId);
        }
    }

    @Override
    public MatchVO getMatchDetail(Long matchId) {
        Match match = matchMapper.selectById(matchId);
        if (match == null) {
            throw BusinessException.notFound("比赛");
        }
        return toVO(match);
    }

    @Override
    public List<Match> getByActivityId(Long activityId) {
        return matchMapper.selectList(
                new LambdaQueryWrapper<Match>()
                        .eq(Match::getActivityId, activityId)
                        .orderByAsc(Match::getRoundOrder)
        );
    }    private List<Team> performGrouping(List<Registration> registrations, String activityType) {
        // 按等级降序排列
        registrations.sort((a, b) -> b.getLevel() - a.getLevel());

        List<Team> teams = new ArrayList<>();

        if ("singles".equals(activityType)) {
            // 单打模式：每人一个队伍
            for (Registration reg : registrations) {
                Team team = new Team();
                team.setMembers(List.of(reg.getUserId()).toString());
                teams.add(team);
            }
        } else {
            // 双打模式：最高+最低配对
            int left = 0;
            int right = registrations.size() - 1;

            while (left < right) {
                Team team = new Team();

    List<Long> members = new ArrayList<>();
                members.add(registrations.get(left).getUserId());
                members.add(registrations.get(right).getUserId());
                team.setMembers(members.toString());
                teams.add(team);
                left++;
                right--;
            }
        }

        return teams;
    }    private void createMatches(Long activityId, List<Team> teams) {
        // 按总等级排序
        teams.sort((a, b) -> {
            int levelA = getTeamLevel(a);

    int levelB = getTeamLevel(b);
            return levelB - levelA;
        });

        // 头尾配对
        int courts = 1;
        int left = 0;
        int right = teams.size() - 1;

        while (left < right) {
            Match match = new Match();
            match.setActivityId(activityId);
            match.setRound("group");
            match.setRoundOrder(courts);
            match.setCourt("场地" + (char)('A' + courts - 1));
            match.setTeam1Id(teams.get(left).getId());
            match.setTeam2Id(teams.get(right).getId());
            match.setStatus("pending");
            matchMapper.insert(match);

            left++;
            right--;
            courts++;
        }
    }

    private int getTeamLevel(Team team) {
        // 解析成员ID并计算总等级
        try {
            String membersStr = team.getMembers();
            membersStr = membersStr.replace("[", "").replace("]", "");

    String[] parts = membersStr.split(",");

    int totalLevel = 0;
            for (String part : parts) {
                Long userId = Long.parseLong(part.trim());
                User user = userService.getById(userId);
                if (user != null) {
                    totalLevel += user.getLevel();
                }
            }
            return totalLevel;
        } catch (Exception e) {
            return 0;
        }
    }

    private MatchVO toVO(Match match) {
        MatchVO vo = new MatchVO();
        vo.setId(match.getId());
        vo.setActivityId(match.getActivityId());
        vo.setRound(match.getRound());
        vo.setRoundOrder(match.getRoundOrder());
        vo.setCourt(match.getCourt());
        vo.setTeam1Id(match.getTeam1Id());
        vo.setTeam2Id(match.getTeam2Id());
        vo.setTeam1Score(match.getTeam1Score());
        vo.setTeam2Score(match.getTeam2Score());
        vo.setStatus(match.getStatus());

        // 获取队伍名称
        Team team1 = teamMapper.selectById(match.getTeam1Id());
        Team team2 = teamMapper.selectById(match.getTeam2Id());
        if (team1 != null) {
            vo.setTeam1Name(team1.getName() != null ? team1.getName() : "队伍" + team1.getId());
        }
        if (team2 != null) {
            vo.setTeam2Name(team2.getName() != null ? team2.getName() : "队伍" + team2.getId());
        }

        return vo;
    }
}
