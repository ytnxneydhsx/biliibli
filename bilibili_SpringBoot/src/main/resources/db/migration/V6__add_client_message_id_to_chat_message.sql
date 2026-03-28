ALTER TABLE chat_message
    ADD COLUMN client_message_id BIGINT NULL COMMENT '客户端消息ID，前端可使用雪花ID生成' AFTER receiver_id;

ALTER TABLE chat_message
    ADD UNIQUE KEY uk_sender_client_message (sender_id, client_message_id);
