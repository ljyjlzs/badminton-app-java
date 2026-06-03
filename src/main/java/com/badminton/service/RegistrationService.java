package com.badminton.service;

import com.badminton.dto.JoinActivityRequest;
import com.badminton.entity.Registration;

import java.util.List;

public interface RegistrationService {    Long joinActivity(Long userId, JoinActivityRequest request);

    void cancelRegistration(Long userId, Long activityId, String reason);

    void handleCancelRequest(Long registrationId, Long operatorId, String action);

    Registration getByActivityAndUser(Long activityId, Long userId);

    List<Registration> getByActivityId(Long activityId);

    List<Registration> getByUserId(Long userId);

    int countValidRegistrations(Long activityId);
}
