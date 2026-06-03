package com.badminton.service.impl;

import com.badminton.common.BusinessException;
import com.badminton.entity.Activity;
import com.badminton.entity.Team;
import com.badminton.mapper.TeamMapper;
import com.badminton.service.ActivityService;
import com.badminton.service.TeamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeamServiceImpl implements TeamService {

    private final TeamMapper teamMapper;
    private final ActivityService activityService;

    @Override
    @Transactional
    public void setTeamName(Long teamId, String name, Long activityId, Long userId) {
        // 验证队名长度
        if (name == null || name.trim().isEmpty()) {
            throw new BusinessException("队名不能为空");
        }
        if (name.length() > 20) {
            throw new BusinessException("队名长度不能超过20个字符");
        }

        // 验证队伍存在
        Team team = teamMapper.selectById(teamId);
        if (team == null) {
            throw BusinessException.notFound("队伍");
        }

        // 验证队伍属于该活动
        if (!team.getActivityId().equals(activityId)) {
            throw new BusinessException("队伍不属于该活动");
        }

        // 验证当前用户是队长或活动组织者
        boolean isCaptain = isTeamCaptain(team, userId);

    boolean isOrganizer = isActivityOrganizer(activityId, userId);

        if (!isCaptain && !isOrganizer) {
            throw BusinessException.forbidden();
        }

        // 更新队伍名称
        team.setName(name.trim());
        teamMapper.updateById(team);
        log.info("队伍名称更新成功: teamId={}, name={}", teamId, name.trim());
    }    private boolean isTeamCaptain(Team team, Long userId) {
        String members = team.getMembers();
        if (members == null || members.isEmpty()) {
            return false;
        }

        try {
            // members 格式: [1, 2] 或 [1,2]
            String cleaned = members.replace("[", "").replace("]", "").trim();

    String[] parts = cleaned.split(",");
            if (parts.length > 0) {
                Long captainId = Long.parseLong(parts[0].trim());
                return captainId.equals(userId);
            }
        } catch (NumberFormatException e) {
            log.warn("解析队伍成员失败: teamId={}, members={}", team.getId(), members);
        }

        return false;
    }    private boolean isActivityOrganizer(Long activityId, Long userId) {
        Activity activity = activityService.getById(activityId);
        return activity != null && activity.getOrganizerId().equals(userId);
    }
}
