CREATE DATABASE IF NOT EXISTS `bilibili` DEFAULT CHARACTER SET utf8mb4;
USE `bilibili`;

-- 用户核心表
CREATE TABLE IF NOT EXISTS `t_user` (
    `id` BIGINT NOT NULL COMMENT '雪花ID',
    `username` VARCHAR(32) NOT NULL UNIQUE COMMENT '登录账号',
    `password` VARCHAR(100) NOT NULL COMMENT '加密密码',
    `status` TINYINT(1) DEFAULT 0 COMMENT '0正常, 1注销',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `delete_time` DATETIME DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_update_time` (`update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 用户详情表
CREATE TABLE IF NOT EXISTS `t_user_info` (
    `id` BIGINT NOT NULL COMMENT '雪花ID',
    `user_id` BIGINT NOT NULL COMMENT '关联 t_user.id',
    `nickname` VARCHAR(32) DEFAULT NULL,
    `avatar_url` VARCHAR(255) DEFAULT NULL,
    `sign` VARCHAR(255) DEFAULT NULL COMMENT '个人介绍',
    `following_count` INT DEFAULT 0,
    `follower_count` INT DEFAULT 0,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`),
    KEY `idx_nickname` (`nickname`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_update_time` (`update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 视频表
CREATE TABLE IF NOT EXISTS `t_video` (
    `id` BIGINT NOT NULL COMMENT '雪花ID',
    `user_id` BIGINT NOT NULL COMMENT '发布者ID',
    `title` VARCHAR(100) NOT NULL,
    `description` TEXT,
    `cover_url` VARCHAR(255),
    `video_url` VARCHAR(255),
    `duration` BIGINT DEFAULT 0 COMMENT '视频时长(秒)',
    `view_count` BIGINT DEFAULT 0,
    `like_count` BIGINT DEFAULT 0,
    `status` TINYINT(1) DEFAULT 0,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_title` (`title`),
    KEY `idx_update_time` (`update_time`),
    KEY `idx_view_count` (`view_count`),
    KEY `idx_like_count` (`like_count`),
    KEY `idx_status_create` (`status`, `create_time`),
    KEY `idx_user_status_create` (`user_id`, `status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 弹幕表
CREATE TABLE IF NOT EXISTS `t_danmaku` (
    `id` BIGINT NOT NULL COMMENT '雪花ID',
    `video_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `content` TEXT NOT NULL,
    `show_time` BIGINT NOT NULL COMMENT '弹幕在视频中出现的时间点(毫秒)',
    `like_count` BIGINT DEFAULT 0,
    `status` TINYINT(1) DEFAULT 0 COMMENT '0正常, 1删除',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_video_id` (`video_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_like_count` (`like_count`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 弹幕点赞表
CREATE TABLE IF NOT EXISTS `t_danmaku_like` (
    `id` BIGINT NOT NULL,
    `danmaku_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `status` TINYINT(1) DEFAULT 0,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_danmaku` (`user_id`, `danmaku_id`),
    KEY `idx_danmaku_id` (`danmaku_id`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_update_time` (`update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 视频点赞表
CREATE TABLE IF NOT EXISTS `t_video_like` (
    `id` BIGINT NOT NULL,
    `video_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `status` TINYINT(1) DEFAULT 0,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_video` (`user_id`, `video_id`),
    KEY `idx_video_id` (`video_id`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_update_time` (`update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 评论表
CREATE TABLE IF NOT EXISTS `t_comment` (
    `id` BIGINT NOT NULL,
    `video_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `content` TEXT NOT NULL,
    `parent_id` BIGINT DEFAULT 0,
    `like_count` BIGINT DEFAULT 0,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_like_count` (`like_count`),
    KEY `idx_video_parent_create` (`video_id`, `parent_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 评论点赞表
CREATE TABLE IF NOT EXISTS `t_comment_like` (
    `id` BIGINT NOT NULL,
    `comment_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `status` TINYINT(1) DEFAULT 0,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_comment` (`user_id`, `comment_id`),
    KEY `idx_comment_id` (`comment_id`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_update_time` (`update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 关注关系表
CREATE TABLE IF NOT EXISTS `t_following` (
    `id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `following_user_id` BIGINT NOT NULL,
    `status` TINYINT(1) DEFAULT 0,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_following` (`user_id`, `following_user_id`),
    KEY `idx_following_user_id` (`following_user_id`),
    KEY `idx_user_status` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 标签字典表
CREATE TABLE IF NOT EXISTS `t_tag` (
    `id` BIGINT NOT NULL COMMENT '雪花ID',
    `name` VARCHAR(32) NOT NULL COMMENT '标签名称',
    `use_count` INT DEFAULT 0 COMMENT '使用计数',
    `status` TINYINT(1) DEFAULT 0,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_update_time` (`update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 视频标签关系表
CREATE TABLE IF NOT EXISTS `t_video_tag` (
    `id` BIGINT NOT NULL COMMENT '雪花ID',
    `video_id` BIGINT NOT NULL,
    `tag_id` BIGINT NOT NULL,
    `status` TINYINT(1) DEFAULT 0,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_video_id` (`video_id`),
    KEY `idx_tag_id` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
