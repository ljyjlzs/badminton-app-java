package com.badminton.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.badminton.common.BusinessException;
import com.badminton.dto.JoinActivityRequest;
import com.badminton.entity.Activity;
import com.badminton.entity.Registration;
import com.badminton.mapper.ActivityMapper;
import com.badminton.mapper.RegistrationMapper;
import com.badminton.service.ActivityService;
import com.badminton.service.RegistrationService;
import com.badminton.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class RegistrationServiceImpl implements RegistrationService {

    @Autowired
    private RegistrationMapper registrationMapper;

    @Autowired
    private ActivityMapper activityMapper;

    @Lazy
    @Autowired
    private ActivityService activityService;

    @Autowired
    private UserService userService;

    @Override
    @Transactional
    public Long joinActivity(Long userId, JoinActivityRequest request) {
        // 解析活动ID
        Long activityId = request.getActivityId();
        if (activityId == null && request.getActivityName() != null) {
            // 按名称查找活动
            Activity activity = activityService.getActivities(userId, "available", request.getActivityName())
                    .stream()
                    .filter(a -> a.getName().equals(request.getActivityName()))
                    .findFirst()
                    .map(a -> activityService.getById(a.getId()))
                    .orElse(null);

            if (activity != null) {
                activityId = activity.getId();
            }
        }

        if (activityId == null) {
            throw new BusinessException("未找到可报名的活动");
        }

        Activity activity = activityService.getById(activityId);
        if (activity == null) {
            throw BusinessException.notFound("活动");
        }

        // 验证活动状态
        if (!"registering".equals(activity.getStatus())) {
            throw new BusinessException("活动已开始或已结束，无法报名");
        }

        // 验证人数
        int currentCount = countValidRegistrations(activityId);
        if (currentCount >= activity.getMaxPlayers()) {
            throw new BusinessException("活动报名已满");
        }

        // 验证是否重复报名
        Registration existing = getByActivityAndUser(activityId, userId);
        if (existing != null && !"approved".equals(existing.getCancelStatus())) {
            throw new BusinessException("您已报名此活动");
        }

        // 固搭模式验证搭档
        if ("fixed-doubles".equals(activity.getType()) && request.getPartnerId() != null) {
            Registration partnerReg = getByActivityAndUser(activityId, request.getPartnerId());
            if (partnerReg == null || "approved".equals(partnerReg.getCancelStatus())) {
                throw new BusinessException("所选搭档尚未报名");
            }
        }

        Registration registration;
        if (existing != null && "approved".equals(existing.getCancelStatus())) {
            // 复用已取消的记录
            registration = existing;
            registration.setCancelStatus(null);
            registration.setCancelReason(null);
            registration.setCancelRequestedAt(null);
            registration.setCancelProcessedAt(null);
            registration.setCancelProcessedBy(null);
            registrationMapper.resetCancelStatus(registration.getId());
        } else {
            // 创建新报名
            registration = new Registration();
            registration.setActivityId(activityId);
            registration.setUserId(userId);
            registration.setNickname(request.getNickname());
            registration.setAvatar(request.getAvatar());
            registration.setLevel(request.getLevel());
            registration.setPartnerId(request.getPartnerId());
            registrationMapper.insert(registration);
        }

        // 更新活动人数
        activity.setCurrentPlayers(activity.getCurrentPlayers() + 1);
        activityMapper.updateById(activity);

        log.info("报名成功: userId={}, activityId={}", userId, activityId);
        return registration.getId();
    }

    @Override
    @Transactional
    public void cancelRegistration(Long userId, Long activityId, String reason) {
        Registration registration = getByActivityAndUser(activityId, userId);
        if (registration == null) {
            throw new BusinessException("您尚未报名此活动");
        }

        if ("pending".equals(registration.getCancelStatus())) {
            throw new BusinessException("您已有待审批的取消申请");
        }

        registration.setCancelStatus("pending");
        registration.setCancelReason(reason);
        registration.setCancelRequestedAt(LocalDateTime.now());
        registrationMapper.updateById(registration);

        log.info("取消报名申请: userId={}, activityId={}", userId, activityId);
    }

    @Override
    @Transactional
    public void handleCancelRequest(Long registrationId, Long operatorId, String action) {
        Registration registration = registrationMapper.selectById(registrationId);
        if (registration == null) {
            throw BusinessException.notFound("报名记录");
        }

        if (!"pending".equals(registration.getCancelStatus())) {
            throw new BusinessException("该取消请求已处理");
        }

        if ("approve".equals(action)) {
            registration.setCancelStatus("approved");
            registration.setCancelProcessedAt(LocalDateTime.now());
            registration.setCancelProcessedBy(operatorId);
            registrationMapper.updateById(registration);

            // 减少活动人数
            Activity activity = activityService.getById(registration.getActivityId());
            if (activity != null) {
                activity.setCurrentPlayers(Math.max(0, activity.getCurrentPlayers() - 1));
                activityMapper.updateById(activity);
            }

            log.info("取消报名已批准: registrationId={}", registrationId);
        } else if ("reject".equals(action)) {
            registration.setCancelStatus("rejected");
            registration.setCancelProcessedAt(LocalDateTime.now());
            registration.setCancelProcessedBy(operatorId);
            registrationMapper.updateById(registration);

            log.info("取消报名已拒绝: registrationId={}", registrationId);
        } else {
            throw new BusinessException("无效的操作: " + action);
        }
    }

    @Override
    public Registration getByActivityAndUser(Long activityId, Long userId) {
        return registrationMapper.selectOne(
                new LambdaQueryWrapper<Registration>()
                        .eq(Registration::getActivityId, activityId)
                        .eq(Registration::getUserId, userId)
                        .orderByDesc(Registration::getCreatedAt)
                        .last("LIMIT 1")
        );
    }

    @Override
    public List<Registration> getByActivityId(Long activityId) {
        return registrationMapper.selectList(
                new LambdaQueryWrapper<Registration>()
                        .eq(Registration::getActivityId, activityId)
                        .and(w -> w.ne(Registration::getCancelStatus, "approved").or().isNull(Registration::getCancelStatus))
                        .orderByAsc(Registration::getCreatedAt)
        );
    }

    @Override
    public List<Registration> getByUserId(Long userId) {
        return registrationMapper.selectList(
                new LambdaQueryWrapper<Registration>()
                        .eq(Registration::getUserId, userId)
                        .and(w -> w.ne(Registration::getCancelStatus, "approved").or().isNull(Registration::getCancelStatus))
                        .orderByDesc(Registration::getCreatedAt)
        );
    }

    @Override
    public int countValidRegistrations(Long activityId) {
        return registrationMapper.countValidRegistrations(activityId);
    }
}
