SET @drop_temp_dir = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 't_video_upload_task'
              AND column_name = 'temp_dir'
        ),
        'ALTER TABLE `t_video_upload_task` DROP COLUMN `temp_dir`',
        'SELECT 1'
    )
);
PREPARE stmt FROM @drop_temp_dir;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_object_key = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 't_video_upload_task'
              AND column_name = 'object_key'
        ),
        'SELECT 1',
        'ALTER TABLE `t_video_upload_task` ADD COLUMN `object_key` VARCHAR(255) NOT NULL COMMENT ''final object key in minio'' AFTER `status`'
    )
);
PREPARE stmt FROM @add_object_key;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_multipart_upload_id = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 't_video_upload_task'
              AND column_name = 'multipart_upload_id'
        ),
        'SELECT 1',
        'ALTER TABLE `t_video_upload_task` ADD COLUMN `multipart_upload_id` VARCHAR(255) NOT NULL COMMENT ''minio multipart upload id'' AFTER `object_key`'
    )
);
PREPARE stmt FROM @add_multipart_upload_id;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
