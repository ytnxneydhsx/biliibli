ALTER TABLE t_user
    ADD COLUMN role_code TINYINT NOT NULL DEFAULT 0 COMMENT '0 user, 1 reviewer, 2 admin'
    AFTER password;
