package com.badminton.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.badminton.common.BusinessException;
import com.badminton.entity.*;
import com.badminton.mapper.MatchMapper;
import com.badminton.mapper.RegistrationMapper;
import com.badminton.mapper.TeamMapper;
import com.badminton.service.*;
import com.badminton.vo.ChallengeDataVO;
import com.badminton.vo.ChallengeDataVO.ChallengeMatchVO;
import com.badminton.vo.ChallengeDataVO.ChallengeTeamVO;
import com.badminton.vo.ChallengeDataVO.EliminatedPlayerVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChallengeServiceImpl implements ChallengeService {

    private final MatchMapper matchMapper;
    private final TeamMapper teamMapper;
    private final RegistrationMapper registrationMapper;
    private final ActivityService activityService;
    private final RegistrationService registrationService;
    private final ScoreService scoreService;
    private final MatchService matchService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void startChallenge(Long activityId, Long userId) {
        Activity activity = activityService.getById(activityId);
        if (activity == null) {
            throw BusinessException.notFound("活动");
        }

        if (!activity.getOrganizerId().equals(userId)) {
            throw BusinessException.forbidden();
        }

        // 验证所有小组赛已完成
        List<Match> groupMatches = matchMapper.selectList(
                new LambdaQueryWrapper<Match>()
                        .eq(Match::getActivityId, activityId)
                        .eq(Match::getRound, "group")
        );

        boolean allConfirmed = groupMatches.stream()
                .allMatch(m -> "confirmed".equals(m.getStatus()));

        if (!allConfirmed) {
            throw new BusinessException("小组赛尚未全部完成");
        }

        // 计算积分排名
        Map<Long, Integer> userScores = scoreService.getUserScores(activityId);

        // 找出积分最高的2人
        List<Long> topUsers = userScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                .limit(2)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // 创建晋级队伍
        Team advanceTeam = new Team();
        advanceTeam.setActivityId(activityId);
        advanceTeam.setName("晋级队");
        advanceTeam.setMembers(topUsers.toString());
        advanceTeam.setIsEliminated(false);
        teamMapper.insert(advanceTeam);

        // 标记其他用户为淘汰
        List<Registration> registrations = registrationService.getByActivityId(activityId);
        for (Registration reg : registrations) {
            if (!topUsers.contains(reg.getUserId())) {
                reg.setIsEliminated(true);
                registrationMapper.updateById(reg);
            }
        }

        // 创建挑战赛比赛（留空，等待淘汰者组队）
        // 挑战赛比赛需要在淘汰者组队后创建

        // 更新活动状态
        activityService.updateActivityStatus(activityId, userId, "challenge");

        log.info("挑战赛开始: activityId={}, topUsers={}", activityId, topUsers);
    }

    @Override
    @Transactional
    public void startFinal(Long activityId, Long userId) {
        Activity activity = activityService.getById(activityId);
        if (activity == null) {
            throw BusinessException.notFound("活动");
        }

        if (!activity.getOrganizerId().equals(userId)) {
            throw BusinessException.forbidden();
        }

        // 验证所有挑战赛已完成
        List<Match> challengeMatches = matchMapper.selectList(
                new LambdaQueryWrapper<Match>()
                        .eq(Match::getActivityId, activityId)
                        .eq(Match::getRound, "challenge")
        );

        boolean allConfirmed = challengeMatches.stream()
                .allMatch(m -> "confirmed".equals(m.getStatus()));

        if (!allConfirmed) {
            throw new BusinessException("挑战赛尚未全部完成");
        }

        // 使用总积分来决定决赛队伍
        Map<Long, Integer> totalScores = scoreService.getUserScores(activityId);

        List<Team> qualifiedTeams = teamMapper.selectList(
                new LambdaQueryWrapper<Team>()
                        .eq(Team::getActivityId, activityId)
                        .eq(Team::getIsEliminated, false)
                        .orderByDesc(Team::getId)
        );

        if (qualifiedTeams.size() < 2) {
            throw new BusinessException("晋级队伍不足，无法开始决赛");
        }

        // 按总积分排序，取积分最高的两支队伍
        qualifiedTeams.sort((t1, t2) -> {
            int score1 = getTeamScore(t1, totalScores);
            int score2 = getTeamScore(t2, totalScores);
            return Integer.compare(score2, score1);
        });

        Team team1 = qualifiedTeams.get(0);
        Team team2 = qualifiedTeams.get(1);

        // 创建决赛比赛
        Match finalMatch = new Match();
        finalMatch.setActivityId(activityId);
        finalMatch.setRound("final");
        finalMatch.setRoundOrder(1);
        finalMatch.setCourt("决赛场地");
        finalMatch.setTeam1Id(team1.getId());
        finalMatch.setTeam2Id(team2.getId());
        finalMatch.setStatus("pending");
        matchMapper.insert(finalMatch);

        // 更新活动状态
        activityService.updateActivityStatus(activityId, "final");

        log.info("决赛开始: activityId={}, team1={}, team2={}", activityId, team1.getId(), team2.getId());
    }

    private List<Team> getChallengeWinners(Long activityId) {
        List<Match> challengeMatches = matchMapper.selectList(
                new LambdaQueryWrapper<Match>()
                        .eq(Match::getActivityId, activityId)
                        .eq(Match::getRound, "challenge")
                        .eq(Match::getStatus, "confirmed")
        );

        Set<Long> winnerTeamIds = new HashSet<>();
        for (Match match : challengeMatches) {
            if (match.getTeam1Score() != null && match.getTeam2Score() != null) {
                if (match.getTeam1Score() > match.getTeam2Score()) {
                    winnerTeamIds.add(match.getTeam1Id());
                } else {
                    winnerTeamIds.add(match.getTeam2Id());
                }
            }
        }

        return winnerTeamIds.stream()
                .map(teamMapper::selectById)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Long createChallengeMatch(Long activityId, Long challengerTeamId, Long userId) {
        // 1. 验证活动存在且当前用户是组织者
        Activity activity = activityService.getById(activityId);
        if (activity == null) {
            throw BusinessException.notFound("活动");
        }
        if (!activity.getOrganizerId().equals(userId)) {
            throw BusinessException.forbidden();
        }

        // 2. 获取积分最高的晋级队伍（未淘汰的队伍，按积分降序取第一个）
        // 优先从 teams 表查找未淘汰且 group_score 最高的队伍
        List<Team> qualifiedTeams = teamMapper.selectList(
                new LambdaQueryWrapper<Team>()
                        .eq(Team::getActivityId, activityId)
                        .eq(Team::getIsEliminated, false)
                        .orderByDesc(Team::getId)
        );

        if (qualifiedTeams.isEmpty()) {
            throw new BusinessException("未找到晋级队伍");
        }

        // 如果有多个晋级队伍，按积分排序取最高的
        if (qualifiedTeams.size() > 1) {
            Map<Long, Integer> userScores = scoreService.getUserScores(activityId);
            qualifiedTeams.sort((t1, t2) -> {
                int score1 = getTeamScore(t1, userScores);

    int score2 = getTeamScore(t2, userScores);
                return Integer.compare(score2, score1);
            });
        }

        Team qualifiedTeam = qualifiedTeams.get(0);

        // 3. 创建挑战赛比赛
        Match match = new Match();
        match.setActivityId(activityId);
        match.setRound("challenge");
        match.setTeam1Id(qualifiedTeam.getId());
        match.setTeam2Id(challengerTeamId);
        match.setTeam1Score(null);
        match.setTeam2Score(null);
        match.setStatus("pending");
        matchMapper.insert(match);

        log.info("创建挑战赛比赛: activityId={}, matchId={}, team1={}, team2={}",
                activityId, match.getId(), qualifiedTeam.getId(), challengerTeamId);

        return match.getId();
    }

    @Override
    public ChallengeDataVO getChallengeData(Long activityId, Long userId) {
        ChallengeDataVO result = new ChallengeDataVO();

        // 1. 获取活动信息，判断是否是组织者
        Activity activity = activityService.getById(activityId);
        if (activity == null) {
            throw BusinessException.notFound("活动");
        }
        result.setIsOrganizer(activity.getOrganizerId().equals(userId));

        // 2. 获取所有报名记录（用于获取昵称、头像信息）
        List<Registration> registrations = registrationService.getByActivityId(activityId);

    Map<Long, String> regNicknames = new HashMap<>();
        Map<Long, String> regAvatars = new HashMap<>();
        Map<Long, Registration> regMap = new HashMap<>();
        for (Registration reg : registrations) {
            regNicknames.put(reg.getUserId(), reg.getNickname());
            regAvatars.put(reg.getUserId(), reg.getAvatar() != null ? reg.getAvatar() : "");
            regMap.put(reg.getUserId(), reg);
        }

        // 3. 获取所有队伍
        List<Team> teams = teamMapper.selectList(
                new LambdaQueryWrapper<Team>()
                        .eq(Team::getActivityId, activityId)
        );

        // 4. 晋级队伍列表（未淘汰）
        List<ChallengeTeamVO> qualifiedTeams = teams.stream()
                .filter(t -> !Boolean.TRUE.equals(t.getIsEliminated()))
                .map(t -> toChallengeTeamVO(t, regNicknames, regAvatars))
                .collect(Collectors.toList());
        result.setQualifiedTeams(qualifiedTeams);

        // 5. 淘汰选手列表
        List<EliminatedPlayerVO> eliminatedPlayers = new ArrayList<>();
        List<Team> eliminatedTeams = teams.stream()
                .filter(t -> Boolean.TRUE.equals(t.getIsEliminated()))
                .collect(Collectors.toList());

        Set<Long> eliminatedPlayerIds = new HashSet<>();
        for (Team team : eliminatedTeams) {
            List<Long> memberIds = parseMembers(team.getMembers());
            eliminatedPlayerIds.addAll(memberIds);
        }

        for (Long pid : eliminatedPlayerIds) {
            EliminatedPlayerVO playerVO = new EliminatedPlayerVO();
            playerVO.setUserId(pid);
            playerVO.setNickname(regNicknames.getOrDefault(pid, "未知用户"));
            playerVO.setAvatar(regAvatars.getOrDefault(pid, ""));
            Registration reg = regMap.get(pid);
            playerVO.setLevel(reg != null && reg.getLevel() != null ? reg.getLevel() : 5);
            eliminatedPlayers.add(playerVO);
        }
        result.setEliminatedPlayers(eliminatedPlayers);

        // 6. 当前挑战赛比赛信息
        List<Match> challengeMatches = matchMapper.selectList(
                new LambdaQueryWrapper<Match>()
                        .eq(Match::getActivityId, activityId)
                        .eq(Match::getRound, "challenge")
        );

        if (!challengeMatches.isEmpty()) {
            Match match = challengeMatches.get(0);
            ChallengeMatchVO matchVO = new ChallengeMatchVO();
            matchVO.setId(match.getId());
            matchVO.setActivityId(match.getActivityId());
            matchVO.setRound(match.getRound());
            matchVO.setTeam1Id(match.getTeam1Id());
            matchVO.setTeam2Id(match.getTeam2Id());
            matchVO.setTeam1Score(match.getTeam1Score());
            matchVO.setTeam2Score(match.getTeam2Score());
            matchVO.setStatus(match.getStatus());

            // 填充队伍名称和成员名称
            Team team1 = teams.stream().filter(t -> t.getId().equals(match.getTeam1Id())).findFirst().orElse(null);

    Team team2 = teams.stream().filter(t -> t.getId().equals(match.getTeam2Id())).findFirst().orElse(null);

            matchVO.setTeam1Name(team1 != null ? team1.getName() : "队伍1");
            matchVO.setTeam2Name(team2 != null ? team2.getName() : "队伍2");
            matchVO.setTeam1Members(team1 != null ? getMemberNames(team1.getMembers(), regNicknames) : "");
            matchVO.setTeam2Members(team2 != null ? getMemberNames(team2.getMembers(), regNicknames) : "");

            result.setChallengeMatch(matchVO);
        }

        return result;
    }    private int getTeamScore(Team team, Map<Long, Integer> userScores) {
        List<Long> memberIds = parseMembers(team.getMembers());
        return memberIds.stream()
                .mapToInt(id -> userScores.getOrDefault(id, 0))
                .sum();
    }    private List<Long> parseMembers(String members) {
        if (members == null || members.isEmpty()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(members, new TypeReference<List<Long>>() {});
        } catch (Exception e) {
            log.warn("解析队伍成员失败: {}", members, e);
            return List.of();
        }
    }    private String getMemberNames(String members, Map<Long, String> regNicknames) {
        List<Long> memberIds = parseMembers(members);
        return memberIds.stream()
                .map(id -> regNicknames.getOrDefault(id, String.valueOf(id)))
                .collect(Collectors.joining(" / "));
    }    private ChallengeTeamVO toChallengeTeamVO(Team team, Map<Long, String> regNicknames, Map<Long, String> regAvatars) {
        ChallengeTeamVO vo = new ChallengeTeamVO();
        vo.setId(team.getId());
        vo.setActivityId(team.getActivityId());
        vo.setName(team.getName());
        vo.setMembers(team.getMembers());
        vo.setIsEliminated(team.getIsEliminated());

        List<Long> memberIds = parseMembers(team.getMembers());
        vo.setMemberNames(memberIds.stream()
                .map(id -> regNicknames.getOrDefault(id, String.valueOf(id)))
                .collect(Collectors.joining(" / ")));
        vo.setMemberAvatars(memberIds.stream()
                .map(id -> regAvatars.getOrDefault(id, ""))
                .collect(Collectors.toList()));

        return vo;
    }
}
