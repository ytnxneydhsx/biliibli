ALTER TABLE chat_group_message
    ADD COLUMN message_id BIGINT NULL COMMENT '消息主键ID' AFTER group_id;

UPDATE chat_group_message gm
JOIN chat_message cm ON cm.server_message_id = gm.server_message_id
SET gm.message_id = cm.id
WHERE gm.message_id IS NULL;

ALTER TABLE chat_group_message
    DROP INDEX uk_group_server_message,
    ADD UNIQUE KEY uk_group_message (group_id, message_id);

ALTER TABLE chat_group_message
    MODIFY COLUMN message_id BIGINT NOT NULL COMMENT '消息主键ID';

ALTER TABLE chat_group_message
    DROP COLUMN server_message_id;
