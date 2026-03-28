ALTER TABLE chat_message
    MODIFY COLUMN conversation_id VARCHAR(64) NOT NULL COMMENT '所属共享会话ID';

ALTER TABLE chat_conversation
    MODIFY COLUMN conversation_id VARCHAR(64) NOT NULL COMMENT '共享会话ID，单聊格式为 single_low_high';

DROP TABLE IF EXISTS chat_session;
