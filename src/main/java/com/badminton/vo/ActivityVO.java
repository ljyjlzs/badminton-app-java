package com.badminton.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

    @Data
public class ActivityVO {

    private Long id;

    private String name;

    private LocalDateTime time;

    private String location;

    private BigDecimal latitude;

    private BigDecimal longitude;

    private Long organizerId;

    private String organizerName;

    private String type;

    private String status;

    private Integer minPlayers;

    private Integer maxPlayers;

    private Integer currentPlayers;
}
