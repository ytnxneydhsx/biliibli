ALTER TABLE contact_relation
    ADD COLUMN is_dm_contact TINYINT NOT NULL DEFAULT 0 COMMENT '是否已建立当前方向私信资格：0否 1是'
    AFTER is_contact;
