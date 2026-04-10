CREATE TABLE IF NOT EXISTS t_im_sensitive_word (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '敏感词主键',
    word VARCHAR(255) NOT NULL COMMENT '敏感词',
    status TINYINT(1) NOT NULL DEFAULT 0 COMMENT '0 normal, 1 deleted',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_word (word),
    KEY idx_status_create (status, create_time),
    KEY idx_update_time (update_time)
) COMMENT = 'IM敏感词库表';
