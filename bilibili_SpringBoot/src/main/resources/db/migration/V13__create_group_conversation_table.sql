CREATE TABLE IF NOT EXISTS chat_group_conversation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '群窗口主键',
    conversation_id VARCHAR(64) NOT NULL COMMENT '群共享会话ID，默认规则为 g_{groupId}',
    owner_user_id BIGINT NOT NULL COMMENT '窗口所属用户ID',
    group_id BIGINT NOT NULL COMMENT '群ID',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '窗口状态：1正常显示 2退出隐藏',
    is_muted TINYINT NOT NULL DEFAULT 0 COMMENT '是否免打扰：0否 1是',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_owner_group (owner_user_id, group_id),
    INDEX idx_owner_status_update_time (owner_user_id, status, update_time),
    INDEX idx_conversation_id (conversation_id)
) COMMENT='群会话窗口表';
