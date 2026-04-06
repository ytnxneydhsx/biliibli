ALTER TABLE chat_group_conversation
    ADD COLUMN last_read_seq BIGINT NOT NULL DEFAULT 0 COMMENT '已读到的群内序号' AFTER is_muted;

UPDATE chat_group_conversation gc
INNER JOIN chat_group_member gm
    ON gm.group_id = gc.group_id
   AND gm.user_id = gc.owner_user_id
SET gc.last_read_seq = gm.last_read_seq;

ALTER TABLE chat_group_member
    DROP COLUMN last_read_seq;
