CREATE TABLE IF NOT EXISTS chat_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '消息主键',
    conversation_id BIGINT NOT NULL COMMENT '所属会话ID',
    sender_id BIGINT NOT NULL COMMENT '发送者用户ID',
    receiver_id BIGINT NOT NULL COMMENT '接收者用户ID（单聊时为对方用户ID）',
    message_type TINYINT NOT NULL DEFAULT 1 COMMENT '消息类型：1文本 2图片',
    content TEXT NOT NULL COMMENT '消息内容，文本消息存正文，图片消息可存URL',
    send_time DATETIME NOT NULL COMMENT '发送时间',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '消息状态：1成功 2失败 3撤回预留',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_conversation_time (conversation_id, send_time),
    INDEX idx_sender (sender_id),
    INDEX idx_receiver (receiver_id)
) COMMENT='聊天消息表';

CREATE TABLE IF NOT EXISTS chat_conversation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '会话摘要记录主键',
    conversation_id BIGINT NOT NULL COMMENT '会话ID，同一段聊天双方可共用',
    owner_user_id BIGINT NOT NULL COMMENT '这条会话记录属于哪个用户视角',
    target_id BIGINT NOT NULL COMMENT '聊天对象ID，单聊时是对方用户ID，群聊时可扩展为群ID',
    type TINYINT NOT NULL DEFAULT 1 COMMENT '会话类型：1单聊 2群聊',
    last_message VARCHAR(500) DEFAULT NULL COMMENT '最近一条消息摘要',
    last_message_time DATETIME DEFAULT NULL COMMENT '最近消息时间',
    unread_count INT NOT NULL DEFAULT 0 COMMENT '未读消息数',
    is_muted TINYINT NOT NULL DEFAULT 0 COMMENT '是否屏蔽：0否 1是',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_owner_target_type (owner_user_id, target_id, type),
    INDEX idx_owner_last_time (owner_user_id, last_message_time),
    INDEX idx_conversation_id (conversation_id)
) COMMENT='聊天会话表';

CREATE TABLE IF NOT EXISTS contact_relation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '关系主键',
    user_id BIGINT NOT NULL COMMENT '关系发起视角用户ID',
    target_user_id BIGINT NOT NULL COMMENT '目标用户ID',
    is_contact TINYINT NOT NULL DEFAULT 0 COMMENT '是否联系人：0否 1是',
    is_blocked TINYINT NOT NULL DEFAULT 0 COMMENT '是否拉黑对方：0否 1是',
    is_muted TINYINT NOT NULL DEFAULT 0 COMMENT '是否屏蔽对方：0否 1是',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_target (user_id, target_user_id),
    INDEX idx_target_user (target_user_id)
) COMMENT='联系人关系表';

CREATE TABLE IF NOT EXISTS user_privacy_setting (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '隐私设置主键',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    private_message_policy TINYINT NOT NULL DEFAULT 1 COMMENT '私信策略：1允许所有人 2仅联系人 3陌生人允许首条 4不允许任何人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_id (user_id)
) COMMENT='用户隐私设置表';
