CREATE TABLE IF NOT EXISTS `t_user` (
    `id` BIGINT NOT NULL COMMENT 'snowflake id',
    `username` VARCHAR(32) NOT NULL UNIQUE COMMENT 'login username',
    `password` VARCHAR(100) NOT NULL COMMENT 'hashed password',
    `status` TINYINT(1) DEFAULT 0 COMMENT '0 normal, 1 deleted',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `delete_time` DATETIME DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `t_user_info` (
    `id` BIGINT NOT NULL COMMENT 'snowflake id',
    `user_id` BIGINT NOT NULL COMMENT 't_user.id',
    `nickname` VARCHAR(32) DEFAULT NULL,
    `avatar_url` VARCHAR(255) DEFAULT NULL,
    `sign` VARCHAR(255) DEFAULT NULL,
    `following_count` INT DEFAULT 0,
    `follower_count` INT DEFAULT 0,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
