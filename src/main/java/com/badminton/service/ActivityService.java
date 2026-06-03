package com.badminton.service;

import com.badminton.dto.ActivityCreateRequest;
import com.badminton.entity.Activity;
import com.badminton.vo.ActivityDetailVO;
import com.badminton.vo.ActivityVO;

import java.util.List;

public interface ActivityService {

    Long createActivity(Long userId, ActivityCreateRequest request);

    List<ActivityVO> getActivities(Long userId, String type, String keyword);

    ActivityDetailVO getActivityDetail(Long activityId, Long userId);

    void deleteActivity(Long activityId, Long userId);

    void updateActivityStatus(Long activityId, Long userId, String status);

    Activity getById(Long id);

    ActivityVO toVO(Activity activity);
}
