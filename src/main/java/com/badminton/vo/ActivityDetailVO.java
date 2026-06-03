package com.badminton.vo;

import lombok.Data;

import java.util.List;

    @Data
public class ActivityDetailVO {

    private ActivityVO activity;

    private List<RegistrationVO> registrations;

    private List<MatchVO> matches;

    private RegistrationVO userRegistration;

    private Boolean isOrganizer;

    private Integer pendingCancelCount;
}
