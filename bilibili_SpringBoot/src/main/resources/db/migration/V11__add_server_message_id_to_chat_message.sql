ALTER TABLE chat_message
    ADD COLUMN server_message_id BIGINT NULL COMMENT '服务端消息ID，用于排序与游标' AFTER id;

UPDATE chat_message
SET server_message_id = id
WHERE server_message_id IS NULL;

ALTER TABLE chat_message
    MODIFY COLUMN server_message_id BIGINT NOT NULL COMMENT '服务端消息ID，用于排序与游标';

ALTER TABLE chat_message
    ADD UNIQUE KEY uk_chat_message_server_message_id (server_message_id);

ALTER TABLE chat_message
    ADD INDEX idx_conversation_server_message_id (conversation_id, server_message_id);
