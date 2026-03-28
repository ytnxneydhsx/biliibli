CREATE TABLE IF NOT EXISTS chat_session (
    conversation_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '会话ID',
    user_low_id BIGINT NOT NULL COMMENT '会话成员中较小的用户ID',
    user_high_id BIGINT NOT NULL COMMENT '会话成员中较大的用户ID',
    type TINYINT NOT NULL DEFAULT 1 COMMENT '会话类型：1单聊 2群聊',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_pair_type (user_low_id, user_high_id, type)
) COMMENT='会话配对表';
