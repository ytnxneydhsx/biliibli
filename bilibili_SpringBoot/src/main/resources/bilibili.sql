CREATE DATABASE IF NOT EXISTS `bilibili` DEFAULT CHARACTER SET utf8mb4;
USE `bilibili`;

CREATE TABLE IF NOT EXISTS `t_user` (
    `id` BIGINT NOT NULL COMMENT 'snowflake id',
    `username` VARCHAR(32) NOT NULL COMMENT 'login username',
    `password` VARCHAR(100) NOT NULL COMMENT 'hashed password',
    `status` TINYINT(1) DEFAULT 0 COMMENT '0 normal, 1 deleted',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `delete_time` DATETIME DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_update_time` (`update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `t_user_info` (
    `id` BIGINT NOT NULL COMMENT 'snowflake id',
    `user_id` BIGINT NOT NULL COMMENT 't_user.id',
    `nickname` VARCHAR(32) DEFAULT NULL,
    `avatar_url` VARCHAR(255) DEFAULT NULL,
    `sign` VARCHAR(255) DEFAULT NULL,
    `following_count` INT NOT NULL DEFAULT 0,
    `follower_count` INT NOT NULL DEFAULT 0,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`),
    KEY `idx_nickname` (`nickname`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_update_time` (`update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `t_video` (
    `id` BIGINT NOT NULL COMMENT 'snowflake id',
    `user_id` BIGINT NOT NULL COMMENT 'author uid',
    `title` VARCHAR(100) NOT NULL,
    `description` TEXT,
    `cover_url` VARCHAR(255),
    `video_url` VARCHAR(255),
    `duration` BIGINT NOT NULL DEFAULT 0 COMMENT 'seconds',
    `view_count` BIGINT NOT NULL DEFAULT 0,
    `like_count` BIGINT NOT NULL DEFAULT 0,
    `comment_count` BIGINT NOT NULL DEFAULT 0,
    `status` TINYINT(1) DEFAULT 0,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_title` (`title`),
    KEY `idx_update_time` (`update_time`),
    KEY `idx_view_count` (`view_count`),
    KEY `idx_like_count` (`like_count`),
    KEY `idx_comment_count` (`comment_count`),
    KEY `idx_status_create` (`status`, `create_time`),
    KEY `idx_user_status_create` (`user_id`, `status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `t_danmaku` (
    `id` BIGINT NOT NULL COMMENT 'snowflake id',
    `video_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `content` TEXT NOT NULL,
    `show_time` BIGINT NOT NULL COMMENT 'ms in video',
    `like_count` BIGINT NOT NULL DEFAULT 0,
    `status` TINYINT(1) DEFAULT 0 COMMENT '0 normal, 1 deleted',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_video_id` (`video_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_like_count` (`like_count`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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

CREATE TABLE IF NOT EXISTS `t_comment` (
    `id` BIGINT NOT NULL,
    `video_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `content` TEXT NOT NULL,
    `parent_id` BIGINT DEFAULT 0,
    `root_id` BIGINT NOT NULL DEFAULT 0,
    `like_count` BIGINT NOT NULL DEFAULT 0,
    `reply_count` INT NOT NULL DEFAULT 0,
    `status` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '0 normal, 1 deleted',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_like_count` (`like_count`),
    KEY `idx_video_status_parent_create` (`video_id`, `status`, `parent_id`, `create_time`),
    KEY `idx_root_create` (`root_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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

CREATE TABLE IF NOT EXISTS `t_following` (
    `id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `following_user_id` BIGINT NOT NULL,
    `status` TINYINT(1) DEFAULT 0,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_following` (`user_id`, `following_user_id`),
    KEY `idx_user_status_following` (`user_id`, `status`, `following_user_id`),
    KEY `idx_following_status_user` (`following_user_id`, `status`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `t_video_upload_task` (
    `id` BIGINT NOT NULL COMMENT 'snowflake id',
    `upload_id` VARCHAR(64) NOT NULL COMMENT 'upload session id',
    `user_id` BIGINT NOT NULL COMMENT 'owner uid',
    `file_name` VARCHAR(255) NOT NULL COMMENT 'original file name',
    `content_type` VARCHAR(100) DEFAULT NULL COMMENT 'mime type',
    `file_size` BIGINT NOT NULL COMMENT 'total bytes',
    `chunk_size` INT NOT NULL COMMENT 'part size',
    `total_chunks` INT NOT NULL COMMENT 'total part count',
    `status` TINYINT(1) DEFAULT 0 COMMENT '0 uploading,1 completing,2 done,3 expired,4 failed,5 cancelled',
    `object_key` VARCHAR(255) NOT NULL COMMENT 'final object key in minio',
    `multipart_upload_id` VARCHAR(255) NOT NULL COMMENT 'minio multipart upload id',
    `final_video_id` BIGINT DEFAULT NULL COMMENT 'final video id',
    `final_video_url` VARCHAR(255) DEFAULT NULL COMMENT 'final video url',
    `error_msg` VARCHAR(255) DEFAULT NULL COMMENT 'error message',
    `expire_time` DATETIME NOT NULL COMMENT 'expire time',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_upload_id` (`upload_id`),
    KEY `idx_user_status` (`user_id`, `status`),
    KEY `idx_expire_time` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `t_tag` (
    `id` BIGINT NOT NULL COMMENT 'snowflake id',
    `name` VARCHAR(32) NOT NULL COMMENT 'tag name',
    `use_count` INT NOT NULL DEFAULT 0 COMMENT 'usage count',
    `status` TINYINT(1) DEFAULT 0,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_update_time` (`update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `t_video_tag` (
    `id` BIGINT NOT NULL COMMENT 'snowflake id',
    `video_id` BIGINT NOT NULL,
    `tag_id` BIGINT NOT NULL,
    `status` TINYINT(1) DEFAULT 0,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_video_id` (`video_id`),
    KEY `idx_tag_id` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
