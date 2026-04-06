ALTER TABLE chat_message
    ADD COLUMN conversation_type TINYINT NOT NULL DEFAULT 1 COMMENT '会话类型：1单聊 2群聊' AFTER conversation_id;

UPDATE chat_message
SET conversation_type = 1;

ALTER TABLE chat_message
    ADD INDEX idx_conversation_type_conversation_time (conversation_type, conversation_id, server_message_id);
