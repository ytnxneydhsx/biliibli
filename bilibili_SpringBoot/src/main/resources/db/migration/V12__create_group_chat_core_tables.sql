CREATE TABLE IF NOT EXISTS chat_group (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '群主键',
    group_name VARCHAR(100) NOT NULL COMMENT '群名称',
    owner_user_id BIGINT NOT NULL COMMENT '群主用户ID',
    group_avatar VARCHAR(255) DEFAULT NULL COMMENT '群头像URL，可为空',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '群状态：1正常 2已解散',
    member_count INT NOT NULL DEFAULT 0 COMMENT '成员数',
    is_all_muted TINYINT NOT NULL DEFAULT 0 COMMENT '是否全员禁言：0否 1是',
    last_message VARCHAR(500) DEFAULT NULL COMMENT '群最近一条消息摘要',
    last_message_time DATETIME DEFAULT NULL COMMENT '群最近消息时间',
    last_server_message_id BIGINT DEFAULT NULL COMMENT '群最近消息全局ID',
    last_message_seq BIGINT NOT NULL DEFAULT 0 COMMENT '群最近消息群内序号',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_owner_user_id (owner_user_id)
) COMMENT='群资料表';

CREATE TABLE IF NOT EXISTS chat_group_member (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '群成员关系主键',
    group_id BIGINT NOT NULL COMMENT '群ID',
    user_id BIGINT NOT NULL COMMENT '成员用户ID',
    role TINYINT NOT NULL DEFAULT 3 COMMENT '角色：1群主 2管理员 3成员',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '成员状态：1在群 2退群 3被踢',
    is_muted TINYINT NOT NULL DEFAULT 0 COMMENT '是否被禁言：0否 1是',
    last_read_seq BIGINT NOT NULL DEFAULT 0 COMMENT '已读到的群内序号',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_group_user (group_id, user_id),
    INDEX idx_user_status (user_id, status),
    INDEX idx_group_status (group_id, status)
) COMMENT='群成员关系表';

CREATE TABLE IF NOT EXISTS chat_group_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '群消息映射主键',
    group_id BIGINT NOT NULL COMMENT '群ID',
    server_message_id BIGINT NOT NULL COMMENT '消息全局ID',
    group_message_seq BIGINT NOT NULL COMMENT '群内递增消息序号',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_group_seq (group_id, group_message_seq),
    UNIQUE KEY uk_group_server_message (group_id, server_message_id)
) COMMENT='群消息序号映射表';
