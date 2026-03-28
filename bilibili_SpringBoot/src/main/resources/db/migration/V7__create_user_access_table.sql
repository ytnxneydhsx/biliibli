CREATE TABLE IF NOT EXISTS t_user_access (
    user_id BIGINT NOT NULL COMMENT '用户ID',
    like_enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否允许点赞：1允许 0禁止',
    comment_enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否允许评论：1允许 0禁止',
    im_message_send_enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否允许发送IM消息：1允许 0禁止',
    video_upload_enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否允许投稿：1允许 0禁止',
    profile_edit_enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否允许修改资料：1允许 0禁止',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (user_id)
) COMMENT='用户访问能力状态表';
