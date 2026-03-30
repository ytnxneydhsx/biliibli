ALTER TABLE chat_conversation
    ADD COLUMN last_server_message_id BIGINT NULL COMMENT '窗口最后一条服务端消息ID' AFTER last_message_time;
