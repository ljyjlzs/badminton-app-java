package com.badminton.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.badminton.common.BusinessException;
import com.badminton.dto.ActivityCreateRequest;
import com.badminton.entity.*;
import com.badminton.entity.Registration;
import com.badminton.mapper.*;
import com.badminton.service.*;
import com.badminton.service.RegistrationService;
import com.badminton.vo.ActivityDetailVO;
import com.badminton.vo.ActivityVO;
import com.badminton.vo.RegistrationVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityServiceImpl implements ActivityService {

    private final ActivityMapper activityMapper;
    private final RegistrationMapper registrationMapper;
    private final MatchMapper matchMapper;
    private final TeamMapper teamMapper;
    private final ScoreMapper scoreMapper;
    private final UserService userService;
    private final RegistrationService registrationService;
    private MatchService matchService;

    @Lazy
    @Autowired
    public void setMatchService(MatchService matchService) {
        this.matchService = matchService;
    }

    @Override
    @Transactional
    public Long createActivity(Long userId, ActivityCreateRequest request) {
        // 验证时间必须是未来时间
        if (request.getTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("活动时间必须是未来的时间");
        }

        Activity activity = new Activity();
        activity.setName(request.getName());
        activity.setTime(request.getTime());
        activity.setLocation(request.getLocation());
        activity.setLatitude(request.getLatitude());
        activity.setLongitude(request.getLongitude());
        activity.setOrganizerId(userId);
        activity.setType(request.getType() != null ? request.getType() : "doubles");
        activity.setStatus("registering");
        activity.setMinPlayers(activity.getType().equals("singles") ? 3 : 4);
        activity.setMaxPlayers(100);
        activity.setCurrentPlayers(0);

        activityMapper.insert(activity);
        log.info("活动创建成功: id={}, name={}", activity.getId(), activity.getName());

        return activity.getId();
    }

    @Override
    public List<ActivityVO> getActivities(Long userId, String type, String keyword) {
        List<Activity> activities;

        switch (type) {
            case "organized":
                activities = activityMapper.selectList(
                        new LambdaQueryWrapper<Activity>()
                                .eq(Activity::getOrganizerId, userId)
                                .orderByDesc(Activity::getCreatedAt)
                );
                break;

            case "joined":
                // 查询用户报名的活动ID
                List<Long> activityIds = registrationService.getByUserId(userId)
                        .stream()
                        .map(Registration::getActivityId)
                        .distinct()
                        .collect(Collectors.toList());

                if (activityIds.isEmpty()) {
                    return List.of();
                }

                activities = activityMapper.selectList(
                        new LambdaQueryWrapper<Activity>()
                                .in(Activity::getId, activityIds)
                                .ne(Activity::getOrganizerId, userId)
                                .orderByDesc(Activity::getCreatedAt)
                );
                break;

            case "available":
                if (keyword != null && !keyword.trim().isEmpty()) {
                    activities = activityMapper.searchActivities(keyword.trim());
                } else {
                    activities = activityMapper.selectList(
                            new LambdaQueryWrapper<Activity>()
                                    .eq(Activity::getStatus, "registering")
                                    .orderByAsc(Activity::getTime)
                    );
                }
                break;

            case "unavailable":
                activities = activityMapper.selectList(
                        new LambdaQueryWrapper<Activity>()
                                .in(Activity::getStatus, "grouping", "playing", "challenge", "final", "finished")
                                .orderByDesc(Activity::getTime)
                );
                break;

            default:
                throw new BusinessException("无效的查询类型: " + type);
        }

        return activities.stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    public ActivityDetailVO getActivityDetail(Long activityId, Long userId) {
        Activity activity = getById(activityId);
        if (activity == null) {
            throw BusinessException.notFound("活动");
        }

        ActivityDetailVO detail = new ActivityDetailVO();
        detail.setActivity(toVO(activity));
        detail.setIsOrganizer(activity.getOrganizerId().equals(userId));

        // 获取报名列表
        List<Registration> registrations = registrationService.getByActivityId(activityId);

    List<RegistrationVO> registrationVOs = registrations.stream()
                .map(reg -> {
                    RegistrationVO vo = new RegistrationVO();
                    vo.setId(reg.getId());
                    vo.setActivityId(reg.getActivityId());
                    vo.setUserId(reg.getUserId());
                    vo.setNickname(reg.getNickname());
                    vo.setAvatar(reg.getAvatar());
                    vo.setLevel(reg.getLevel());
                    vo.setPartnerId(reg.getPartnerId());
                    vo.setTeamId(reg.getTeamId());
                    vo.setIsEliminated(reg.getIsEliminated());
                    vo.setCancelStatus(reg.getCancelStatus());
                    return vo;
                })
                .collect(Collectors.toList());
        detail.setRegistrations(registrationVOs);

        // 获取用户的报名记录
        Registration userRegistration = registrationService.getByActivityAndUser(activityId, userId);
        detail.setUserRegistration(userRegistration != null ? registrationVOs.stream()
                .filter(r -> r.getUserId().equals(userId))
                .findFirst()
                .orElse(null) : null);

        // 统计待处理的取消请求
        long pendingCancelCount = registrations.stream()
                .filter(r -> "pending".equals(r.getCancelStatus()))
                .count();
        detail.setPendingCancelCount((int) pendingCancelCount);

        return detail;
    }

    @Override
    @Transactional
    public void deleteActivity(Long activityId, Long userId) {
        Activity activity = getById(activityId);
        if (activity == null) {
            throw BusinessException.notFound("活动");
        }

        if (!activity.getOrganizerId().equals(userId)) {
            throw BusinessException.forbidden();
        }

        // 级联删除关联数据
        scoreMapper.delete(new LambdaQueryWrapper<Score>().eq(Score::getActivityId, activityId));
        matchMapper.delete(new LambdaQueryWrapper<Match>().eq(Match::getActivityId, activityId));
        teamMapper.delete(new LambdaQueryWrapper<Team>().eq(Team::getActivityId, activityId));
        registrationMapper.delete(new LambdaQueryWrapper<Registration>().eq(Registration::getActivityId, activityId));

        activityMapper.deleteById(activityId);
        log.info("活动删除成功（级联删除关联数据）: id={}", activityId);
    }

    @Override
    @Transactional
    public void updateActivityStatus(Long activityId, Long userId, String status) {
        Activity activity = getById(activityId);
        if (activity == null) {
            throw BusinessException.notFound("活动");
        }

        if (!activity.getOrganizerId().equals(userId)) {
            throw BusinessException.forbidden();
        }

        activity.setStatus(status);
        activityMapper.updateById(activity);
        log.info("活动状态更新: id={}, status={}", activityId, status);
    }

    @Override
    public Activity getById(Long id) {
        return activityMapper.selectById(id);
    }

    @Override
    public ActivityVO toVO(Activity activity) {
        if (activity == null) {
            return null;
        }

        ActivityVO vo = new ActivityVO();
        vo.setId(activity.getId());
        vo.setName(activity.getName());
        vo.setTime(activity.getTime());
        vo.setLocation(activity.getLocation());
        vo.setLatitude(activity.getLatitude());
        vo.setLongitude(activity.getLongitude());
        vo.setOrganizerId(activity.getOrganizerId());
        vo.setType(activity.getType());
        vo.setStatus(activity.getStatus());
        vo.setMinPlayers(activity.getMinPlayers());
        vo.setMaxPlayers(activity.getMaxPlayers());
        vo.setCurrentPlayers(activity.getCurrentPlayers());

        // 获取组织者名称
        User organizer = userService.getById(activity.getOrganizerId());
        if (organizer != null) {
            vo.setOrganizerName(organizer.getNickname());
        }

        return vo;
    }
}
