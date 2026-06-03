-- 羽毛球报名小程序数据库初始化脚本

CREATE DATABASE IF NOT EXISTS badminton DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE badminton;

-- 用户表
CREATE TABLE IF NOT EXISTS `users` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `openid` VARCHAR(64) NOT NULL COMMENT '微信openid',
  `union_id` VARCHAR(64) DEFAULT NULL COMMENT '微信union_id',
  `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
  `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
  `level` INT DEFAULT 5 COMMENT '等级(1-10)',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `role` VARCHAR(20) NOT NULL DEFAULT 'user' COMMENT '角色(user/admin)',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_openid` (`openid`),
  KEY `idx_union_id` (`union_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 活动表
CREATE TABLE IF NOT EXISTS `activities` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` VARCHAR(50) NOT NULL COMMENT '活动名称',
  `time` DATETIME NOT NULL COMMENT '活动时间',
  `location` VARCHAR(100) NOT NULL COMMENT '活动地点',
  `latitude` DECIMAL(10,7) DEFAULT NULL COMMENT '纬度',
  `longitude` DECIMAL(10,7) DEFAULT NULL COMMENT '经度',
  `organizer_id` BIGINT NOT NULL COMMENT '组织者ID',
  `type` VARCHAR(20) NOT NULL DEFAULT 'doubles' COMMENT '类型(singles/doubles/fixed-doubles)',
  `status` VARCHAR(20) NOT NULL DEFAULT 'registering' COMMENT '状态',
  `min_players` INT DEFAULT 4 COMMENT '最少人数',
  `max_players` INT DEFAULT 100 COMMENT '最多人数',
  `current_players` INT DEFAULT 0 COMMENT '当前报名人数',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_organizer_id` (`organizer_id`),
  KEY `idx_status` (`status`),
  KEY `idx_time` (`time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动表';

-- 报名表
CREATE TABLE IF NOT EXISTS `registrations` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `activity_id` BIGINT NOT NULL COMMENT '活动ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `nickname` VARCHAR(50) NOT NULL COMMENT '报名时昵称',
  `avatar` VARCHAR(255) DEFAULT NULL COMMENT '报名时头像',
  `level` INT NOT NULL DEFAULT 5 COMMENT '报名时等级',
  `partner_id` BIGINT DEFAULT NULL COMMENT '搭档用户ID(固搭模式)',
  `team_id` BIGINT DEFAULT NULL COMMENT '队伍ID',
  `is_eliminated` TINYINT(1) DEFAULT 0 COMMENT '是否被淘汰',
  `cancel_status` VARCHAR(20) DEFAULT NULL COMMENT '取消状态(pending/approved/rejected)',
  `cancel_reason` VARCHAR(200) DEFAULT NULL COMMENT '取消原因',
  `cancel_requested_at` DATETIME DEFAULT NULL COMMENT '申请取消时间',
  `cancel_processed_at` DATETIME DEFAULT NULL COMMENT '处理取消时间',
  `cancel_processed_by` BIGINT DEFAULT NULL COMMENT '处理人ID',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_activity_user` (`activity_id`, `user_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_team_id` (`team_id`),
  KEY `idx_cancel_status` (`cancel_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报名表';

-- 队伍表
CREATE TABLE IF NOT EXISTS `teams` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `activity_id` BIGINT NOT NULL COMMENT '活动ID',
  `name` VARCHAR(50) DEFAULT NULL COMMENT '队伍名称',
  `members` JSON DEFAULT NULL COMMENT '成员ID列表',
  `is_eliminated` TINYINT(1) DEFAULT 0 COMMENT '是否被淘汰',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_activity_id` (`activity_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='队伍表';

-- 比赛表
CREATE TABLE IF NOT EXISTS `matches` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `activity_id` BIGINT NOT NULL COMMENT '活动ID',
  `round` VARCHAR(20) NOT NULL COMMENT '轮次(group/challenge/final)',
  `round_order` INT DEFAULT 0 COMMENT '轮次顺序',
  `court` VARCHAR(20) DEFAULT NULL COMMENT '场地',
  `team1_id` BIGINT NOT NULL COMMENT '队伍1ID',
  `team2_id` BIGINT NOT NULL COMMENT '队伍2ID',
  `team1_score` INT DEFAULT NULL COMMENT '队伍1得分',
  `team2_score` INT DEFAULT NULL COMMENT '队伍2得分',
  `status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '状态(pending/confirming/confirmed)',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_activity_id` (`activity_id`),
  KEY `idx_team1_id` (`team1_id`),
  KEY `idx_team2_id` (`team2_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='比赛表';

-- 积分表
CREATE TABLE IF NOT EXISTS `scores` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `activity_id` BIGINT NOT NULL COMMENT '活动ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `match_id` BIGINT NOT NULL COMMENT '比赛ID',
  `source` VARCHAR(20) NOT NULL COMMENT '来源(group/challenge/final)',
  `score_change` INT NOT NULL COMMENT '积分变化',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_activity_id` (`activity_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_match_id` (`match_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分表';

-- AI消息表
CREATE TABLE IF NOT EXISTS `ai_messages` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `role` VARCHAR(20) NOT NULL COMMENT '角色(user/assistant/system_feedback)',
  `content` TEXT NOT NULL COMMENT '消息内容',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI消息表';

-- 规则表
CREATE TABLE IF NOT EXISTS `rules` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `type` VARCHAR(20) NOT NULL DEFAULT 'rules' COMMENT '类型',
  `title` VARCHAR(50) NOT NULL COMMENT '标题',
  `sections` JSON NOT NULL COMMENT '规则内容',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='规则表';
