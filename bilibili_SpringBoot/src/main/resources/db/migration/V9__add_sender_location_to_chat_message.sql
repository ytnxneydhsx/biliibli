ALTER TABLE chat_message
    ADD COLUMN sender_location VARCHAR(64) NULL COMMENT '发送时IP属地快照' AFTER client_message_id;
