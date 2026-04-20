# IM 在线用户对阶梯压测记录

生成时间：2026-04-21 Asia/Shanghai

## 数据范围

本文档汇总本次阶梯压测每个挡位的结果数据。每个挡位包含 k6 汇总、RabbitMQ 队列快照和采样、应用侧 Prometheus 指标，以及 MySQL Performance Schema / slow log 导出的统计。

原始结果目录：
- `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-002159`

每个挡位目录内包含的原始文件：`summary.json`、`k6.log`、`k6.err.log`、`k6-exit-code.txt`、`queues-before.json`、`queue-samples.jsonl`、`queues-after-k6.json`、`queues-after-20s.json`、`metrics-before.prom`、`metrics-after-k6.prom`、`metrics-after-20s.prom`、`mysql-digest.txt`、`mysql-slow-group.txt`、`mysql-waits.txt`。

## 压测场景

- 按挡位增加 sender/receiver 用户对数量。
- 每个 sender 每 0.5 秒发送 1 条消息，所以目标吞吐为 `用户对数量 * 2 msg/s`。
- 每个挡位在 receiver 预热后持续发送 60 秒。
- 每个挡位开始前会清理 Redis、清空 IM 队列、重置 MySQL Performance Schema / slow log 统计表，然后分别采集压测前后快照。
- 应用侧 Prometheus 指标是累计 counter，所以本文档中的每挡位数据按 `metrics-after-20s - metrics-before` 计算。

## K6 汇总

| 用户对 | 目标 msg/s | 已发送 | Accepted | 接收方收到 | 发送方回执 | Check 失败 | Accepted 平均 ms | Accepted P95 ms | 最大 ms | Exit |
|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| 300 | 600 | 36000 | 36000 | 36000 | 36000 | 0 | 4.5 | 5.0 | 1,427.0 | 0 |
| 500 | 1000 | 60000 | 60000 | 60000 | 60000 | 0 | 5.3 | 7.0 | 1,402.0 | 0 |
| 600 | 1200 | 71520 | 71520 | 71520 | 71520 | 4 | 6.2 | 10.0 | 1,411.0 | 0 |
| 800 | 1600 | 96000 | 96000 | 96000 | 96000 | 0 | 13.0 | 29.0 | 1,468.0 | 0 |

## RabbitMQ 队列积压汇总

`最大积压` 是 `queue-samples.jsonl` 里该队列 `messages` 的最大值。`>=100 采样次数` 表示采样时该队列积压至少 100 条的次数。`Consumer 最小/最大/最后` 来自压测期间 RabbitMQ 队列采样，用于观察 Spring AMQP 动态扩容情况。`k6 结束后` 和 `20s 后` 来自对应的队列快照文件。

| 用户对 | 速率 | 队列 | Consumer 最小 | Consumer 最大 | Consumer 最后 | 最大积压 | >=100 采样次数 | k6 结束后 | 20s 后 |
|---:|---:|---|---:|---:|---:|---:|---:|---:|---:|
| 300 | 600 | `im.message.persist.queue` | 2 | 4 | 3 | 5 | 0 | 0 | 0 |
| 300 | 600 | `im.message.conversation.queue` | 2 | 6 | 5 | 5 | 0 | 0 | 0 |
| 300 | 600 | `im.message.conversation.redis.queue` | 4 | 8 | 7 | 3 | 0 | 0 | 0 |
| 300 | 600 | `im.message.recent.cache.queue` | 4 | 8 | 7 | 1 | 0 | 0 | 0 |
| 300 | 600 | `im.message.realtime.queue` | 2 | 4 | 3 | 2 | 0 | 0 | 0 |
| 500 | 1000 | `im.message.persist.queue` | 3 | 4 | 3 | 19 | 0 | 0 | 0 |
| 500 | 1000 | `im.message.conversation.queue` | 5 | 6 | 5 | 15 | 0 | 0 | 0 |
| 500 | 1000 | `im.message.conversation.redis.queue` | 7 | 8 | 7 | 4 | 0 | 0 | 0 |
| 500 | 1000 | `im.message.recent.cache.queue` | 7 | 8 | 7 | 5 | 0 | 0 | 0 |
| 500 | 1000 | `im.message.realtime.queue` | 3 | 4 | 3 | 4 | 0 | 0 | 0 |
| 600 | 1200 | `im.message.persist.queue` | 3 | 4 | 3 | 119 | 3 | 0 | 0 |
| 600 | 1200 | `im.message.conversation.queue` | 5 | 6 | 5 | 66 | 0 | 0 | 0 |
| 600 | 1200 | `im.message.conversation.redis.queue` | 7 | 8 | 7 | 11 | 0 | 0 | 0 |
| 600 | 1200 | `im.message.recent.cache.queue` | 7 | 8 | 7 | 8 | 0 | 0 | 0 |
| 600 | 1200 | `im.message.realtime.queue` | 3 | 4 | 3 | 10 | 0 | 0 | 0 |
| 800 | 1600 | `im.message.persist.queue` | 3 | 4 | 4 | 26967 | 11 | 0 | 0 |
| 800 | 1600 | `im.message.conversation.queue` | 5 | 6 | 5 | 8213 | 10 | 0 | 0 |
| 800 | 1600 | `im.message.conversation.redis.queue` | 7 | 8 | 7 | 14 | 0 | 0 | 0 |
| 800 | 1600 | `im.message.recent.cache.queue` | 7 | 8 | 7 | 9 | 0 | 0 | 0 |
| 800 | 1600 | `im.message.realtime.queue` | 3 | 4 | 3 | 9 | 0 | 0 | 0 |

## 应用侧 DB 操作指标

以下数据来自 `im_db_operation_duration_seconds_*`，按每个挡位的前后差值计算。

| 用户对 | 速率 | 操作 | 次数 | 平均 ms | 总耗时 s |
|---:|---:|---|---:|---:|---:|
| 300 | 600 | `chat_message_insert` | 36000 | 0.489 | 17.594 |
| 300 | 600 | `contact_relation_upsert` | 300 | 0.660 | 0.198 |
| 500 | 1000 | `chat_message_insert` | 60000 | 0.494 | 29.655 |
| 500 | 1000 | `contact_relation_upsert` | 500 | 0.423 | 0.212 |
| 600 | 1200 | `chat_message_insert` | 71520 | 0.485 | 34.656 |
| 600 | 1200 | `contact_relation_upsert` | 596 | 0.371 | 0.221 |
| 800 | 1600 | `chat_message_insert` | 96000 | 0.451 | 43.290 |
| 800 | 1600 | `contact_relation_upsert` | 800 | 0.374 | 0.299 |

## 应用侧 MQ Consumer 指标

以下数据来自 `im_mq_consumer_duration_seconds_*`，按每个挡位的前后差值计算。

| 用户对 | 速率 | Consumer | 队列 | 次数 | 平均 ms | 总耗时 s |
|---:|---:|---|---|---:|---:|---:|
| 300 | 600 | `single_conversation_persist` | `im.message.conversation.queue` | 36000 | 1.202 | 43.286 |
| 300 | 600 | `single_conversation_redis_projection` | `im.message.conversation.redis.queue` | 36000 | 0.488 | 17.558 |
| 300 | 600 | `single_message_persist` | `im.message.persist.queue` | 36000 | 0.827 | 29.763 |
| 300 | 600 | `single_realtime_push` | `im.message.realtime.queue` | 36000 | 0.286 | 10.284 |
| 300 | 600 | `single_recent_message_cache_projection` | `im.message.recent.cache.queue` | 36000 | 0.261 | 9.388 |
| 500 | 1000 | `single_conversation_persist` | `im.message.conversation.queue` | 60000 | 1.297 | 77.837 |
| 500 | 1000 | `single_conversation_redis_projection` | `im.message.conversation.redis.queue` | 60000 | 0.570 | 34.227 |
| 500 | 1000 | `single_message_persist` | `im.message.persist.queue` | 60000 | 0.875 | 52.502 |
| 500 | 1000 | `single_realtime_push` | `im.message.realtime.queue` | 60000 | 0.358 | 21.456 |
| 500 | 1000 | `single_recent_message_cache_projection` | `im.message.recent.cache.queue` | 60000 | 0.306 | 18.348 |
| 600 | 1200 | `single_conversation_persist` | `im.message.conversation.queue` | 71520 | 1.325 | 94.738 |
| 600 | 1200 | `single_conversation_redis_projection` | `im.message.conversation.redis.queue` | 71520 | 0.617 | 44.132 |
| 600 | 1200 | `single_message_persist` | `im.message.persist.queue` | 71520 | 0.896 | 64.060 |
| 600 | 1200 | `single_realtime_push` | `im.message.realtime.queue` | 71520 | 0.411 | 29.366 |
| 600 | 1200 | `single_recent_message_cache_projection` | `im.message.recent.cache.queue` | 71520 | 0.329 | 23.538 |
| 800 | 1600 | `single_conversation_persist` | `im.message.conversation.queue` | 96000 | 1.356 | 130.160 |
| 800 | 1600 | `single_conversation_redis_projection` | `im.message.conversation.redis.queue` | 96000 | 0.768 | 73.681 |
| 800 | 1600 | `single_message_persist` | `im.message.persist.queue` | 96000 | 0.859 | 82.451 |
| 800 | 1600 | `single_realtime_push` | `im.message.realtime.queue` | 96000 | 0.539 | 51.781 |
| 800 | 1600 | `single_recent_message_cache_projection` | `im.message.recent.cache.queue` | 96000 | 0.415 | 39.864 |

## MySQL Digest Top 语句

以下 Top 行来自每个挡位的 `mysql-digest.txt`。

### 用户对 300，速率 600 msg/s

| 次数 | 总耗时 s | 平均 ms | 最大 ms | Digest |
|---:|---:|---:|---:|---|
| 108589 | 12.9422 | 0.1192 | 1423.2380 | `COMMIT` |
| 217138 | 11.7797 | 0.0542 | 1.2387 | `SET `autocommit` = ?` |
| 36293 | 8.7472 | 0.2410 | 1392.9444 | `UPDATE `chat_conversation` SET `last_message` = CASE WHEN `last_server_message_id` IS NULL OR `last_server_message_id` < ? THEN ? ELSE `last_message` END , `last_message_time` = CA...` |
| 35988 | 7.7270 | 0.2147 | 847.6933 | `INSERT INTO `chat_message` ( `server_message_id` , `conversation_id` , `conversation_type` , `sender_id` , `receiver_id` , `client_message_id` , `sender_location` , `message_type` ...` |
| 71985 | 7.6408 | 0.1061 | 1.1537 | `SELECT `id` , `username` , PASSWORD , `role_code` , STATUS , `create_time` , `update_time` , `delete_time` FROM `t_user` WHERE `id` = ? AND STATUS = ?` |
| 71988 | 7.2301 | 0.1004 | 1.3838 | `SELECT `user_id` AS `userId` , `target_user_id` AS `targetUserId` , `is_contact` AS `isContact` , `is_dm_contact` AS `isDmContact` , `is_blocked` AS `isBlocked` , `is_muted` AS `is...` |
| 110693 | 7.0633 | 0.0638 | 0.6039 | `SELECT @@SESSION . `transaction_read_only`` |
| 36292 | 6.1826 | 0.1704 | 5.3957 | `UPDATE `chat_conversation` SET `unread_count` = COALESCE ( `unread_count` , ? ) + ? , `last_message` = CASE WHEN `last_server_message_id` IS NULL OR `last_server_message_id` < ? TH...` |

### 用户对 500，速率 1000 msg/s

| 次数 | 总耗时 s | 平均 ms | 最大 ms | Digest |
|---:|---:|---:|---:|---|
| 361898 | 22.0028 | 0.0608 | 1395.6116 | `SET `autocommit` = ?` |
| 180985 | 18.6547 | 0.1031 | 102.8179 | `COMMIT` |
| 119967 | 13.6256 | 0.1136 | 3.2044 | `SELECT `id` , `username` , PASSWORD , `role_code` , STATUS , `create_time` , `update_time` , `delete_time` FROM `t_user` WHERE `id` = ? AND STATUS = ?` |
| 119969 | 13.1283 | 0.1094 | 6.9839 | `SELECT `user_id` AS `userId` , `target_user_id` AS `targetUserId` , `is_contact` AS `isContact` , `is_dm_contact` AS `isDmContact` , `is_blocked` AS `isBlocked` , `is_muted` AS `is...` |
| 60488 | 12.7123 | 0.2102 | 9.9489 | `UPDATE `chat_conversation` SET `last_message` = CASE WHEN `last_server_message_id` IS NULL OR `last_server_message_id` < ? THEN ? ELSE `last_message` END , `last_message_time` = CA...` |
| 60487 | 12.4343 | 0.2056 | 1397.0934 | `UPDATE `chat_conversation` SET `unread_count` = COALESCE ( `unread_count` , ? ) + ? , `last_message` = CASE WHEN `last_server_message_id` IS NULL OR `last_server_message_id` < ? TH...` |
| 184488 | 12.1186 | 0.0657 | 7.4254 | `SELECT @@SESSION . `transaction_read_only`` |
| 59987 | 11.8810 | 0.1981 | 9.6532 | `INSERT INTO `chat_message` ( `server_message_id` , `conversation_id` , `conversation_type` , `sender_id` , `receiver_id` , `client_message_id` , `sender_location` , `message_type` ...` |

### 用户对 600，速率 1200 msg/s

| 次数 | 总耗时 s | 平均 ms | 最大 ms | Digest |
|---:|---:|---:|---:|---|
| 431440 | 26.9352 | 0.0624 | 1402.3078 | `SET `autocommit` = ?` |
| 215727 | 24.5361 | 0.1137 | 1404.4887 | `COMMIT` |
| 143020 | 19.0051 | 0.1329 | 1402.3491 | `SELECT `user_id` AS `userId` , `target_user_id` AS `targetUserId` , `is_contact` AS `isContact` , `is_dm_contact` AS `isDmContact` , `is_blocked` AS `isBlocked` , `is_muted` AS `is...` |
| 143020 | 17.9647 | 0.1256 | 1402.3284 | `SELECT `id` , `username` , PASSWORD , `role_code` , STATUS , `create_time` , `update_time` , `delete_time` FROM `t_user` WHERE `id` = ? AND STATUS = ?` |
| 72112 | 16.4704 | 0.2284 | 1402.5605 | `UPDATE `chat_conversation` SET `last_message` = CASE WHEN `last_server_message_id` IS NULL OR `last_server_message_id` < ? THEN ? ELSE `last_message` END , `last_message_time` = CA...` |
| 71509 | 15.8493 | 0.2216 | 1402.4358 | `INSERT INTO `chat_message` ( `server_message_id` , `conversation_id` , `conversation_type` , `sender_id` , `receiver_id` , `client_message_id` , `sender_location` , `message_type` ...` |
| 219922 | 15.2356 | 0.0693 | 833.0458 | `SELECT @@SESSION . `transaction_read_only`` |
| 72105 | 14.3218 | 0.1986 | 833.1960 | `UPDATE `chat_conversation` SET `unread_count` = COALESCE ( `unread_count` , ? ) + ? , `last_message` = CASE WHEN `last_server_message_id` IS NULL OR `last_server_message_id` < ? TH...` |

### 用户对 800，速率 1600 msg/s

| 次数 | 总耗时 s | 平均 ms | 最大 ms | Digest |
|---:|---:|---:|---:|---|
| 579086 | 40.5219 | 0.0700 | 1406.4193 | `SET `autocommit` = ?` |
| 289568 | 29.0005 | 0.1002 | 53.2900 | `COMMIT` |
| 191981 | 25.7102 | 0.1339 | 1406.6604 | `SELECT `id` , `username` , PASSWORD , `role_code` , STATUS , `create_time` , `update_time` , `delete_time` FROM `t_user` WHERE `id` = ? AND STATUS = ?` |
| 191981 | 23.6753 | 0.1233 | 6.2720 | `SELECT `user_id` AS `userId` , `target_user_id` AS `targetUserId` , `is_contact` AS `isContact` , `is_dm_contact` AS `isDmContact` , `is_blocked` AS `isBlocked` , `is_muted` AS `is...` |
| 96797 | 20.9998 | 0.2169 | 818.7712 | `UPDATE `chat_conversation` SET `last_message` = CASE WHEN `last_server_message_id` IS NULL OR `last_server_message_id` < ? THEN ? ELSE `last_message` END , `last_message_time` = CA...` |
| 96789 | 20.2113 | 0.2088 | 823.2699 | `UPDATE `chat_conversation` SET `unread_count` = COALESCE ( `unread_count` , ? ) + ? , `last_message` = CASE WHEN `last_server_message_id` IS NULL OR `last_server_message_id` < ? TH...` |
| 295174 | 19.0743 | 0.0646 | 4.4369 | `SELECT @@SESSION . `transaction_read_only`` |
| 95988 | 17.6960 | 0.1844 | 818.7902 | `INSERT INTO `chat_message` ( `server_message_id` , `conversation_id` , `conversation_type` , `sender_id` , `receiver_id` , `client_message_id` , `sender_location` , `message_type` ...` |

## MySQL Waits Top 事件

以下 Top 行来自每个挡位的 `mysql-waits.txt`，已排除 `idle`。

### 用户对 300，速率 600 msg/s

| 事件 | 次数 | 总耗时 s | 平均 ms | 最大 ms |
|---|---:|---:|---:|---:|
| `wait/io/table/sql/handler` | 399854 | 6.6661 | 0.0167 | 12.5697 |
| `wait/io/file/innodb/innodb_data_file` | 15511 | 4.0235 | 0.2594 | 10.4918 |
| `wait/io/file/innodb/innodb_dblwr_file` | 5234 | 2.4708 | 0.4721 | 10.4403 |
| `wait/io/file/innodb/innodb_log_file` | 827909 | 1.3604 | 0.0016 | 4.1359 |
| `wait/io/file/sql/binlog` | 71031 | 0.5515 | 0.0078 | 0.4495 |
| `wait/lock/table/sql/handler` | 327910 | 0.1943 | 0.0006 | 0.2251 |
| `wait/io/file/csv/metadata` | 7 | 0.0012 | 0.1663 | 0.6499 |
| `wait/io/file/csv/data` | 6 | 0.0001 | 0.0102 | 0.0153 |

### 用户对 500，速率 1000 msg/s

| 事件 | 次数 | 总耗时 s | 平均 ms | 最大 ms |
|---|---:|---:|---:|---:|
| `wait/io/table/sql/handler` | 666427 | 11.8512 | 0.0178 | 10.4533 |
| `wait/io/file/innodb/innodb_data_file` | 20022 | 6.2468 | 0.3120 | 8.4217 |
| `wait/io/file/innodb/innodb_dblwr_file` | 6656 | 4.0700 | 0.6115 | 19.1820 |
| `wait/io/file/innodb/innodb_log_file` | 1430041 | 2.2947 | 0.0016 | 19.7356 |
| `wait/io/file/sql/binlog` | 117669 | 0.8368 | 0.0071 | 0.6809 |
| `wait/lock/table/sql/handler` | 546512 | 0.3304 | 0.0006 | 0.4898 |
| `wait/io/file/csv/data` | 4 | 0.0001 | 0.0127 | 0.0229 |

### 用户对 600，速率 1200 msg/s

| 事件 | 次数 | 总耗时 s | 平均 ms | 最大 ms |
|---|---:|---:|---:|---:|
| `wait/io/table/sql/handler` | 794499 | 13.6024 | 0.0171 | 11.3717 |
| `wait/io/file/innodb/innodb_data_file` | 22704 | 7.6511 | 0.3370 | 7.0851 |
| `wait/io/file/innodb/innodb_dblwr_file` | 7612 | 5.1992 | 0.6830 | 25.8393 |
| `wait/io/file/innodb/innodb_log_file` | 1703595 | 2.6635 | 0.0016 | 10.8279 |
| `wait/io/file/sql/binlog` | 140799 | 1.0083 | 0.0072 | 23.0525 |
| `wait/lock/table/sql/handler` | 651474 | 0.3988 | 0.0006 | 0.7901 |
| `wait/io/file/sql/binlog_index` | 18 | 0.0007 | 0.0403 | 0.5081 |
| `wait/io/file/csv/data` | 4 | 0.0000 | 0.0095 | 0.0172 |

### 用户对 800，速率 1600 msg/s

| 事件 | 次数 | 总耗时 s | 平均 ms | 最大 ms |
|---|---:|---:|---:|---:|
| `wait/io/table/sql/handler` | 1066412 | 17.5479 | 0.0165 | 17.0551 |
| `wait/io/file/innodb/innodb_data_file` | 27926 | 10.0774 | 0.3609 | 8.3403 |
| `wait/io/file/innodb/innodb_dblwr_file` | 9604 | 6.9641 | 0.7251 | 23.5521 |
| `wait/io/file/innodb/innodb_log_file` | 2299670 | 3.4298 | 0.0015 | 25.6575 |
| `wait/io/file/sql/binlog` | 189366 | 1.2708 | 0.0067 | 2.4996 |
| `wait/lock/table/sql/handler` | 874412 | 0.5504 | 0.0006 | 0.7860 |
| `wait/io/file/csv/data` | 4 | 0.0000 | 0.0103 | 0.0194 |

## 原始文件索引

### 用户对 300，速率 600 msg/s

目录：`D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-002159\pairs-300-rate-600`

- `summary.json` (154270 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-002159\pairs-300-rate-600\summary.json`
- `k6.log` (16797 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-002159\pairs-300-rate-600\k6.log`
- `k6.err.log` (0 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-002159\pairs-300-rate-600\k6.err.log`
- `k6-exit-code.txt` (6 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-002159\pairs-300-rate-600\k6-exit-code.txt`
- `queues-before.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-002159\pairs-300-rate-600\queues-before.json`
- `queue-samples.jsonl` (27254 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-002159\pairs-300-rate-600\queue-samples.jsonl`
- `queues-after-k6.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-002159\pairs-300-rate-600\queues-after-k6.json`
- `queues-after-20s.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-002159\pairs-300-rate-600\queues-after-20s.json`
- `metrics-before.prom` (79834 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-002159\pairs-300-rate-600\metrics-before.prom`
- `metrics-after-k6.prom` (501030 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-002159\pairs-300-rate-600\metrics-after-k6.prom`
- `metrics-after-20s.prom` (501031 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-002159\pairs-300-rate-600\metrics-after-20s.prom`
- `mysql-digest.txt` (3659 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-002159\pairs-300-rate-600\mysql-digest.txt`
- `mysql-slow-group.txt` (0 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-002159\pairs-300-rate-600\mysql-slow-group.txt`
- `mysql-waits.txt` (548 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-002159\pairs-300-rate-600\mysql-waits.txt`

### 用户对 500，速率 1000 msg/s

目录：`D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-002159\pairs-500-rate-1000`

- `summary.json` (252101 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-002159\pairs-500-rate-1000\summary.json`
- `k6.log` (17302 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-002159\pairs-500-rate-1000\k6.log`
- `k6.err.log` (0 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-002159\pairs-500-rate-1000\k6.err.log`
- `k6-exit-code.txt` (6 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-002159\pairs-500-rate-1000\k6-exit-code.txt`
- `queues-before.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-002159\pairs-500-rate-1000\queues-before.json`
- `queue-samples.jsonl` (27258 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-002159\pairs-500-rate-1000\queue-samples.jsonl`
- `queues-after-k6.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-002159\pairs-500-rate-1000\queues-after-k6.json`
- `queues-after-20s.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-002159\pairs-500-rate-1000\queues-after-20s.json`
- `metrics-before.prom` (501029 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-002159\pairs-500-rate-1000\metrics-before.prom`
- `metrics-after-k6.prom` (501514 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-002159\pairs-500-rate-1000\metrics-after-k6.prom`
- `metrics-after-20s.prom` (501514 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-002159\pairs-500-rate-1000\metrics-after-20s.prom`
- `mysql-digest.txt` (3673 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-002159\pairs-500-rate-1000\mysql-digest.txt`
- `mysql-slow-group.txt` (0 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-002159\pairs-500-rate-1000\mysql-slow-group.txt`
- `mysql-waits.txt` (501 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-002159\pairs-500-rate-1000\mysql-waits.txt`

### 用户对 600，速率 1200 msg/s

目录：`D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-002159\pairs-600-rate-1200`

- `summary.json` (301002 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-002159\pairs-600-rate-1200\summary.json`
- `k6.log` (17344 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-002159\pairs-600-rate-1200\k6.log`
- `k6.err.log` (0 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-002159\pairs-600-rate-1200\k6.err.log`
- `k6-exit-code.txt` (6 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-002159\pairs-600-rate-1200\k6-exit-code.txt`
- `queues-before.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-002159\pairs-600-rate-1200\queues-before.json`
- `queue-samples.jsonl` (25685 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-002159\pairs-600-rate-1200\queue-samples.jsonl`
- `queues-after-k6.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-002159\pairs-600-rate-1200\queues-after-k6.json`
- `queues-after-20s.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-002159\pairs-600-rate-1200\queues-after-20s.json`
- `metrics-before.prom` (501513 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-002159\pairs-600-rate-1200\metrics-before.prom`
- `metrics-after-k6.prom` (506738 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-002159\pairs-600-rate-1200\metrics-after-k6.prom`
- `metrics-after-20s.prom` (506744 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-002159\pairs-600-rate-1200\metrics-after-20s.prom`
- `mysql-digest.txt` (3685 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-002159\pairs-600-rate-1200\mysql-digest.txt`
- `mysql-slow-group.txt` (0 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-002159\pairs-600-rate-1200\mysql-slow-group.txt`
- `mysql-waits.txt` (557 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-002159\pairs-600-rate-1200\mysql-waits.txt`

### 用户对 800，速率 1600 msg/s

目录：`D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-002159\pairs-800-rate-1600`

- `summary.json` (398855 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-002159\pairs-800-rate-1600\summary.json`
- `k6.log` (17412 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-002159\pairs-800-rate-1600\k6.log`
- `k6.err.log` (0 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-002159\pairs-800-rate-1600\k6.err.log`
- `k6-exit-code.txt` (6 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-002159\pairs-800-rate-1600\k6-exit-code.txt`
- `queues-before.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-002159\pairs-800-rate-1600\queues-before.json`
- `queue-samples.jsonl` (25824 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-002159\pairs-800-rate-1600\queue-samples.jsonl`
- `queues-after-k6.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-002159\pairs-800-rate-1600\queues-after-k6.json`
- `queues-after-20s.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-002159\pairs-800-rate-1600\queues-after-20s.json`
- `metrics-before.prom` (506744 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-002159\pairs-800-rate-1600\metrics-before.prom`
- `metrics-after-k6.prom` (506794 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-002159\pairs-800-rate-1600\metrics-after-k6.prom`
- `metrics-after-20s.prom` (506783 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-002159\pairs-800-rate-1600\metrics-after-20s.prom`
- `mysql-digest.txt` (3680 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-002159\pairs-800-rate-1600\mysql-digest.txt`
- `mysql-slow-group.txt` (0 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-002159\pairs-800-rate-1600\mysql-slow-group.txt`
- `mysql-waits.txt` (503 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-002159\pairs-800-rate-1600\mysql-waits.txt`

