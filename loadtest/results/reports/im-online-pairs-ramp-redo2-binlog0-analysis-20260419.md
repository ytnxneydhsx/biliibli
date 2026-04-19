# IM 在线用户对阶梯压测记录

生成时间：2026-04-19 Asia/Shanghai

## 数据范围

本文档汇总本次阶梯压测每个挡位的结果数据。每个挡位包含 k6 汇总、RabbitMQ 队列快照和采样、应用侧 Prometheus 指标，以及 MySQL Performance Schema / slow log 导出的统计。

原始结果目录：
- `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-redo2-binlog0-20260419-141348`

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
| 300 | 600 | 36000 | 36000 | 36000 | 36000 | 0 | 5.1 | 7.0 | 84.0 | - |
| 500 | 1000 | 60000 | 59995 | 59995 | 59995 | 0 | 10.3 | 24.0 | 145.0 | - |
| 600 | 1200 | 72000 | 72000 | 72000 | 72000 | 0 | 8.7 | 16.0 | 69.0 | - |
| 800 | 1600 | 96000 | 95997 | 95997 | 95997 | 0 | 24.5 | 68.0 | 359.0 | - |

## RabbitMQ 队列积压汇总

`最大积压` 是 `queue-samples.jsonl` 里该队列 `messages` 的最大值。`>=100 采样次数` 表示采样时该队列积压至少 100 条的次数。`Consumer 最小/最大/最后` 来自压测期间 RabbitMQ 队列采样，用于观察 Spring AMQP 动态扩容情况。`k6 结束后` 和 `20s 后` 来自对应的队列快照文件。

| 用户对 | 速率 | 队列 | Consumer 最小 | Consumer 最大 | Consumer 最后 | 最大积压 | >=100 采样次数 | k6 结束后 | 20s 后 |
|---:|---:|---|---:|---:|---:|---:|---:|---:|---:|
| 300 | 600 | `im.message.persist.queue` | 2 | 4 | 3 | 6 | 0 | 0 | 0 |
| 300 | 600 | `im.message.conversation.queue` | 2 | 6 | 5 | 6 | 0 | 0 | 0 |
| 300 | 600 | `im.message.conversation.redis.queue` | 4 | 8 | 7 | 6 | 0 | 0 | 0 |
| 300 | 600 | `im.message.recent.cache.queue` | 4 | 8 | 7 | 6 | 0 | 0 | 0 |
| 300 | 600 | `im.message.realtime.queue` | 2 | 4 | 3 | 2 | 0 | 0 | 0 |
| 500 | 1000 | `im.message.persist.queue` | 3 | 4 | 3 | 170 | 2 | 0 | 0 |
| 500 | 1000 | `im.message.conversation.queue` | 5 | 6 | 5 | 151 | 1 | 0 | 0 |
| 500 | 1000 | `im.message.conversation.redis.queue` | 7 | 8 | 7 | 10 | 0 | 0 | 0 |
| 500 | 1000 | `im.message.recent.cache.queue` | 7 | 8 | 7 | 7 | 0 | 0 | 0 |
| 500 | 1000 | `im.message.realtime.queue` | 3 | 4 | 3 | 7 | 0 | 0 | 0 |
| 600 | 1200 | `im.message.persist.queue` | 3 | 4 | 3 | 1399 | 8 | 0 | 0 |
| 600 | 1200 | `im.message.conversation.queue` | 5 | 6 | 5 | 820 | 7 | 0 | 0 |
| 600 | 1200 | `im.message.conversation.redis.queue` | 7 | 8 | 7 | 6 | 0 | 0 | 0 |
| 600 | 1200 | `im.message.recent.cache.queue` | 7 | 8 | 7 | 10 | 0 | 0 | 0 |
| 600 | 1200 | `im.message.realtime.queue` | 3 | 4 | 3 | 5 | 0 | 0 | 0 |
| 800 | 1600 | `im.message.persist.queue` | 3 | 4 | 4 | 60996 | 12 | 0 | 0 |
| 800 | 1600 | `im.message.conversation.queue` | 5 | 6 | 6 | 56334 | 12 | 0 | 0 |
| 800 | 1600 | `im.message.conversation.redis.queue` | 7 | 8 | 7 | 9 | 0 | 0 | 0 |
| 800 | 1600 | `im.message.recent.cache.queue` | 7 | 8 | 7 | 9 | 0 | 0 | 0 |
| 800 | 1600 | `im.message.realtime.queue` | 3 | 4 | 3 | 9 | 0 | 0 | 0 |

## 应用侧 DB 操作指标

以下数据来自 `im_db_operation_duration_seconds_*`，按每个挡位的前后差值计算。

| 用户对 | 速率 | 操作 | 次数 | 平均 ms | 总耗时 s |
|---:|---:|---|---:|---:|---:|
| 300 | 600 | `chat_message_insert` | 36000 | 0.516 | 18.572 |
| 300 | 600 | `contact_relation_upsert` | 300 | 0.800 | 0.240 |
| 500 | 1000 | `chat_message_insert` | 59995 | 0.513 | 30.795 |
| 500 | 1000 | `contact_relation_upsert` | 500 | 0.541 | 0.270 |
| 600 | 1200 | `chat_message_insert` | 72000 | 0.525 | 37.795 |
| 600 | 1200 | `contact_relation_upsert` | 600 | 0.378 | 0.227 |
| 800 | 1600 | `chat_message_insert` | 95997 | 0.460 | 44.172 |
| 800 | 1600 | `contact_relation_upsert` | 800 | 0.412 | 0.329 |

## 应用侧 MQ Consumer 指标

以下数据来自 `im_mq_consumer_duration_seconds_*`，按每个挡位的前后差值计算。

| 用户对 | 速率 | Consumer | 队列 | 次数 | 平均 ms | 总耗时 s |
|---:|---:|---|---|---:|---:|---:|
| 300 | 600 | `single_conversation_persist` | `im.message.conversation.queue` | 36000 | 2.206 | 79.426 |
| 300 | 600 | `single_conversation_redis_projection` | `im.message.conversation.redis.queue` | 36000 | 0.546 | 19.654 |
| 300 | 600 | `single_message_persist` | `im.message.persist.queue` | 36000 | 0.912 | 32.829 |
| 300 | 600 | `single_realtime_push` | `im.message.realtime.queue` | 36000 | 0.337 | 12.116 |
| 300 | 600 | `single_recent_message_cache_projection` | `im.message.recent.cache.queue` | 36000 | 0.299 | 10.770 |
| 500 | 1000 | `single_conversation_persist` | `im.message.conversation.queue` | 59995 | 2.305 | 138.266 |
| 500 | 1000 | `single_conversation_redis_projection` | `im.message.conversation.redis.queue` | 59995 | 0.701 | 42.075 |
| 500 | 1000 | `single_message_persist` | `im.message.persist.queue` | 59995 | 0.941 | 56.431 |
| 500 | 1000 | `single_realtime_push` | `im.message.realtime.queue` | 59995 | 0.500 | 29.981 |
| 500 | 1000 | `single_recent_message_cache_projection` | `im.message.recent.cache.queue` | 59995 | 0.380 | 22.814 |
| 600 | 1200 | `single_conversation_persist` | `im.message.conversation.queue` | 72000 | 2.392 | 172.235 |
| 600 | 1200 | `single_conversation_redis_projection` | `im.message.conversation.redis.queue` | 72000 | 0.742 | 53.392 |
| 600 | 1200 | `single_message_persist` | `im.message.persist.queue` | 72000 | 1.000 | 72.033 |
| 600 | 1200 | `single_realtime_push` | `im.message.realtime.queue` | 72000 | 0.534 | 38.447 |
| 600 | 1200 | `single_recent_message_cache_projection` | `im.message.recent.cache.queue` | 72000 | 0.403 | 29.013 |
| 800 | 1600 | `single_conversation_persist` | `im.message.conversation.queue` | 95997 | 2.121 | 203.630 |
| 800 | 1600 | `single_conversation_redis_projection` | `im.message.conversation.redis.queue` | 95997 | 0.845 | 81.082 |
| 800 | 1600 | `single_message_persist` | `im.message.persist.queue` | 95997 | 0.854 | 82.015 |
| 800 | 1600 | `single_realtime_push` | `im.message.realtime.queue` | 95997 | 0.647 | 62.142 |
| 800 | 1600 | `single_recent_message_cache_projection` | `im.message.recent.cache.queue` | 95997 | 0.461 | 44.268 |

## MySQL Digest Top 语句

以下 Top 行来自每个挡位的 `mysql-digest.txt`。

### 用户对 300，速率 600 msg/s

| 次数 | 总耗时 s | 平均 ms | 最大 ms | Digest |
|---:|---:|---:|---:|---|
| 143979 | 17.8432 | 0.1239 | 18446744071.8132 | `SELECT `id` , `conversation_id` , `owner_user_id` , `target_id` , TYPE , `last_message` , `last_message_time` , `last_server_message_id` , `unread_count` , `is_muted` , `create_tim...` |
| 217151 | 12.8507 | 0.0592 | 18446744071.8770 | `SET `autocommit` = ?` |
| 71984 | 12.8172 | 0.1781 | 7.9651 | `UPDATE `chat_conversation` SET `conversation_id` = ? , `last_message` = ? , `last_message_time` = ? , `last_server_message_id` = ? , `update_time` = NOW WHERE `owner_user_id` = ? A...` |
| 108587 | 10.7070 | 0.0986 | 10.3899 | `COMMIT` |
| 146067 | 9.6104 | 0.0658 | 2.1115 | `SELECT @@SESSION . `transaction_read_only`` |
| 71985 | 8.1702 | 0.1135 | 6.2596 | `SELECT `id` , `username` , PASSWORD , `role_code` , STATUS , `create_time` , `update_time` , `delete_time` FROM `t_user` WHERE `id` = ? AND STATUS = ?` |
| 71988 | 7.9114 | 0.1099 | 7.7848 | `SELECT `user_id` AS `userId` , `target_user_id` AS `targetUserId` , `is_contact` AS `isContact` , `is_dm_contact` AS `isDmContact` , `is_blocked` AS `isBlocked` , `is_muted` AS `is...` |
| 35978 | 7.1635 | 0.1991 | 15.3515 | `INSERT INTO `chat_message` ( `server_message_id` , `conversation_id` , `conversation_type` , `sender_id` , `receiver_id` , `client_message_id` , `sender_location` , `message_type` ...` |

### 用户对 500，速率 1000 msg/s

| 次数 | 总耗时 s | 平均 ms | 最大 ms | Digest |
|---:|---:|---:|---:|---|
| 239938 | 31.9176 | 0.1330 | 18446744072.3170 | `SELECT `id` , `conversation_id` , `owner_user_id` , `target_id` , TYPE , `last_message` , `last_message_time` , `last_server_message_id` , `unread_count` , `is_muted` , `create_tim...` |
| 119981 | 22.4813 | 0.1874 | 11.8296 | `UPDATE `chat_conversation` SET `conversation_id` = ? , `last_message` = ? , `last_message_time` = ? , `last_server_message_id` = ? , `update_time` = NOW WHERE `owner_user_id` = ? A...` |
| 361898 | 22.3310 | 0.0617 | 18446744070.7073 | `SET `autocommit` = ?` |
| 180960 | 19.7092 | 0.1089 | 50.4023 | `COMMIT` |
| 243441 | 16.3313 | 0.0671 | 2.3345 | `SELECT @@SESSION . `transaction_read_only`` |
| 119987 | 15.1188 | 0.1260 | 4.0973 | `SELECT `id` , `username` , PASSWORD , `role_code` , STATUS , `create_time` , `update_time` , `delete_time` FROM `t_user` WHERE `id` = ? AND STATUS = ?` |
| 119981 | 14.8268 | 0.1236 | 18446744070.7965 | `SELECT `user_id` AS `userId` , `target_user_id` AS `targetUserId` , `is_contact` AS `isContact` , `is_dm_contact` AS `isDmContact` , `is_blocked` AS `isBlocked` , `is_muted` AS `is...` |
| 59991 | 12.1537 | 0.2026 | 13.2476 | `INSERT INTO `chat_message` ( `server_message_id` , `conversation_id` , `conversation_type` , `sender_id` , `receiver_id` , `client_message_id` , `sender_location` , `message_type` ...` |

### 用户对 600，速率 1200 msg/s

| 次数 | 总耗时 s | 平均 ms | 最大 ms | Digest |
|---:|---:|---:|---:|---|
| 287965 | 40.0158 | 0.1390 | 18446744070.9851 | `SELECT `id` , `conversation_id` , `owner_user_id` , `target_id` , TYPE , `last_message` , `last_message_time` , `last_server_message_id` , `unread_count` , `is_muted` , `create_tim...` |
| 143985 | 27.9665 | 0.1942 | 18446744071.0406 | `UPDATE `chat_conversation` SET `conversation_id` = ? , `last_message` = ? , `last_message_time` = ? , `last_server_message_id` = ? , `update_time` = NOW WHERE `owner_user_id` = ? A...` |
| 434325 | 27.8432 | 0.0641 | 18446744070.7657 | `SET `autocommit` = ?` |
| 217181 | 24.7272 | 0.1139 | 18446744070.9524 | `COMMIT` |
| 292158 | 20.1501 | 0.0690 | 18446744070.9377 | `SELECT @@SESSION . `transaction_read_only`` |
| 143993 | 18.6929 | 0.1298 | 4.6488 | `SELECT `id` , `username` , PASSWORD , `role_code` , STATUS , `create_time` , `update_time` , `delete_time` FROM `t_user` WHERE `id` = ? AND STATUS = ?` |
| 143990 | 18.2812 | 0.1270 | 5.4772 | `SELECT `user_id` AS `userId` , `target_user_id` AS `targetUserId` , `is_contact` AS `isContact` , `is_dm_contact` AS `isDmContact` , `is_blocked` AS `isBlocked` , `is_muted` AS `is...` |
| 71992 | 14.7742 | 0.2052 | 11.7153 | `INSERT INTO `chat_message` ( `server_message_id` , `conversation_id` , `conversation_type` , `sender_id` , `receiver_id` , `client_message_id` , `sender_location` , `message_type` ...` |

### 用户对 800，速率 1600 msg/s

| 次数 | 总耗时 s | 平均 ms | 最大 ms | Digest |
|---:|---:|---:|---:|---|
| 383942 | 49.2343 | 0.1282 | 7.3833 | `SELECT `id` , `conversation_id` , `owner_user_id` , `target_id` , TYPE , `last_message` , `last_message_time` , `last_server_message_id` , `unread_count` , `is_muted` , `create_tim...` |
| 289564 | 36.8599 | 0.1273 | 18446744070.8263 | `COMMIT` |
| 579079 | 35.0104 | 0.0605 | 18446744071.6903 | `SET `autocommit` = ?` |
| 191976 | 33.0652 | 0.1722 | 18446744072.1717 | `UPDATE `chat_conversation` SET `conversation_id` = ? , `last_message` = ? , `last_message_time` = ? , `last_server_message_id` = ? , `update_time` = NOW WHERE `owner_user_id` = ? A...` |
| 191975 | 26.2749 | 0.1369 | 18446744071.3204 | `SELECT `id` , `username` , PASSWORD , `role_code` , STATUS , `create_time` , `update_time` , `delete_time` FROM `t_user` WHERE `id` = ? AND STATUS = ?` |
| 191983 | 25.6227 | 0.1335 | 7.1623 | `SELECT `user_id` AS `userId` , `target_user_id` AS `targetUserId` , `is_contact` AS `isContact` , `is_dm_contact` AS `isDmContact` , `is_blocked` AS `isBlocked` , `is_muted` AS `is...` |
| 389510 | 23.8710 | 0.0613 | 18446744070.8236 | `SELECT @@SESSION . `transaction_read_only`` |
| 95992 | 17.1436 | 0.1786 | 18446744071.7109 | `INSERT INTO `chat_message` ( `server_message_id` , `conversation_id` , `conversation_type` , `sender_id` , `receiver_id` , `client_message_id` , `sender_location` , `message_type` ...` |

## MySQL Waits Top 事件

以下 Top 行来自每个挡位的 `mysql-waits.txt`，已排除 `idle`。

### 用户对 300，速率 600 msg/s

| 事件 | 次数 | 总耗时 s | 平均 ms | 最大 ms |
|---|---:|---:|---:|---:|
| `wait/io/table/sql/handler` | 615300 | 8.6150 | 0.0140 | 15.1034 |
| `wait/io/file/innodb/innodb_data_file` | 12120 | 3.6500 | 0.3012 | 10.8070 |
| `wait/io/file/innodb/innodb_dblwr_file` | 4004 | 2.3950 | 0.5982 | 10.7245 |
| `wait/io/file/innodb/innodb_log_file` | 928973 | 1.9086 | 0.0021 | 12.3759 |
| `wait/io/file/sql/binlog` | 71087 | 0.5643 | 0.0079 | 1.1033 |
| `wait/lock/table/sql/handler` | 507300 | 0.2696 | 0.0005 | 0.3461 |
| `wait/io/file/csv/metadata` | 7 | 0.0006 | 0.0873 | 0.3714 |
| `wait/io/file/csv/data` | 6 | 0.0001 | 0.0103 | 0.0156 |

### 用户对 500，速率 1000 msg/s

| 事件 | 次数 | 总耗时 s | 平均 ms | 最大 ms |
|---|---:|---:|---:|---:|
| `wait/io/table/sql/handler` | 1025450 | 15.4440 | 0.0151 | 16.4554 |
| `wait/io/file/innodb/innodb_data_file` | 20441 | 5.9767 | 0.2924 | 10.2366 |
| `wait/io/file/innodb/innodb_dblwr_file` | 6872 | 3.9409 | 0.5735 | 12.8916 |
| `wait/io/file/innodb/innodb_log_file` | 1548352 | 2.8174 | 0.0018 | 8.8556 |
| `wait/io/file/sql/binlog` | 119079 | 0.9153 | 0.0077 | 3.7440 |
| `wait/lock/table/sql/handler` | 845465 | 0.4561 | 0.0005 | 0.5253 |
| `wait/io/file/csv/data` | 4 | 0.0001 | 0.0207 | 0.0447 |

### 用户对 600，速率 1200 msg/s

| 事件 | 次数 | 总耗时 s | 平均 ms | 最大 ms |
|---|---:|---:|---:|---:|
| `wait/io/table/sql/handler` | 1230600 | 18.6024 | 0.0151 | 12.0671 |
| `wait/io/file/innodb/innodb_data_file` | 23288 | 9.4244 | 0.4047 | 13.5934 |
| `wait/io/file/innodb/innodb_dblwr_file` | 7836 | 6.7155 | 0.8570 | 22.3596 |
| `wait/io/file/innodb/innodb_log_file` | 1826313 | 3.4444 | 0.0019 | 22.1853 |
| `wait/io/file/sql/binlog` | 143094 | 1.1055 | 0.0077 | 1.1768 |
| `wait/lock/table/sql/handler` | 1014600 | 0.5698 | 0.0006 | 0.6743 |
| `wait/io/file/csv/data` | 4 | 0.0001 | 0.0158 | 0.0361 |

### 用户对 800，速率 1600 msg/s

| 事件 | 次数 | 总耗时 s | 平均 ms | 最大 ms |
|---|---:|---:|---:|---:|
| `wait/io/table/sql/handler` | 1640770 | 22.4747 | 0.0137 | 23.1779 |
| `wait/io/file/innodb/innodb_data_file` | 30856 | 10.7049 | 0.3469 | 17.8525 |
| `wait/io/file/innodb/innodb_dblwr_file` | 10374 | 6.9664 | 0.6715 | 18.8816 |
| `wait/io/file/innodb/innodb_log_file` | 2422483 | 3.6103 | 0.0015 | 18.5873 |
| `wait/io/file/sql/binlog` | 188664 | 1.5118 | 0.0080 | 2.8322 |
| `wait/lock/table/sql/handler` | 1352779 | 0.7268 | 0.0005 | 1.0053 |
| `wait/io/file/csv/data` | 4 | 0.0004 | 0.1007 | 0.2821 |

## 原始文件索引

### 用户对 300，速率 600 msg/s

目录：`D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-redo2-binlog0-20260419-141348\pairs-300-rate-600`

- `summary.json` (154297 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-redo2-binlog0-20260419-141348\pairs-300-rate-600\summary.json`
- `k6.log` (18139 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-redo2-binlog0-20260419-141348\pairs-300-rate-600\k6.log`
- `k6.err.log` (0 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-redo2-binlog0-20260419-141348\pairs-300-rate-600\k6.err.log`
- `k6-exit-code.txt` (3 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-redo2-binlog0-20260419-141348\pairs-300-rate-600\k6-exit-code.txt`
- `queues-before.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-redo2-binlog0-20260419-141348\pairs-300-rate-600\queues-before.json`
- `queue-samples.jsonl` (27254 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-redo2-binlog0-20260419-141348\pairs-300-rate-600\queue-samples.jsonl`
- `queues-after-k6.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-redo2-binlog0-20260419-141348\pairs-300-rate-600\queues-after-k6.json`
- `queues-after-20s.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-redo2-binlog0-20260419-141348\pairs-300-rate-600\queues-after-20s.json`
- `metrics-before.prom` (80703 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-redo2-binlog0-20260419-141348\pairs-300-rate-600\metrics-before.prom`
- `metrics-after-k6.prom` (449546 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-redo2-binlog0-20260419-141348\pairs-300-rate-600\metrics-after-k6.prom`
- `metrics-after-20s.prom` (449547 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-redo2-binlog0-20260419-141348\pairs-300-rate-600\metrics-after-20s.prom`
- `mysql-digest.txt` (3237 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-redo2-binlog0-20260419-141348\pairs-300-rate-600\mysql-digest.txt`
- `mysql-slow-group.txt` (0 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-redo2-binlog0-20260419-141348\pairs-300-rate-600\mysql-slow-group.txt`
- `mysql-waits.txt` (555 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-redo2-binlog0-20260419-141348\pairs-300-rate-600\mysql-waits.txt`

### 用户对 500，速率 1000 msg/s

目录：`D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-redo2-binlog0-20260419-141348\pairs-500-rate-1000`

- `summary.json` (252361 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-redo2-binlog0-20260419-141348\pairs-500-rate-1000\summary.json`
- `k6.log` (20406 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-redo2-binlog0-20260419-141348\pairs-500-rate-1000\k6.log`
- `k6.err.log` (0 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-redo2-binlog0-20260419-141348\pairs-500-rate-1000\k6.err.log`
- `k6-exit-code.txt` (3 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-redo2-binlog0-20260419-141348\pairs-500-rate-1000\k6-exit-code.txt`
- `queues-before.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-redo2-binlog0-20260419-141348\pairs-500-rate-1000\queues-before.json`
- `queue-samples.jsonl` (28889 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-redo2-binlog0-20260419-141348\pairs-500-rate-1000\queue-samples.jsonl`
- `queues-after-k6.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-redo2-binlog0-20260419-141348\pairs-500-rate-1000\queues-after-k6.json`
- `queues-after-20s.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-redo2-binlog0-20260419-141348\pairs-500-rate-1000\queues-after-20s.json`
- `metrics-before.prom` (449545 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-redo2-binlog0-20260419-141348\pairs-500-rate-1000\metrics-before.prom`
- `metrics-after-k6.prom` (476013 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-redo2-binlog0-20260419-141348\pairs-500-rate-1000\metrics-after-k6.prom`
- `metrics-after-20s.prom` (476012 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-redo2-binlog0-20260419-141348\pairs-500-rate-1000\metrics-after-20s.prom`
- `mysql-digest.txt` (3293 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-redo2-binlog0-20260419-141348\pairs-500-rate-1000\mysql-digest.txt`
- `mysql-slow-group.txt` (0 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-redo2-binlog0-20260419-141348\pairs-500-rate-1000\mysql-slow-group.txt`
- `mysql-waits.txt` (508 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-redo2-binlog0-20260419-141348\pairs-500-rate-1000\mysql-waits.txt`

### 用户对 600，速率 1200 msg/s

目录：`D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-redo2-binlog0-20260419-141348\pairs-600-rate-1200`

- `summary.json` (301012 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-redo2-binlog0-20260419-141348\pairs-600-rate-1200\summary.json`
- `k6.log` (18506 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-redo2-binlog0-20260419-141348\pairs-600-rate-1200\k6.log`
- `k6.err.log` (0 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-redo2-binlog0-20260419-141348\pairs-600-rate-1200\k6.err.log`
- `k6-exit-code.txt` (3 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-redo2-binlog0-20260419-141348\pairs-600-rate-1200\k6-exit-code.txt`
- `queues-before.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-redo2-binlog0-20260419-141348\pairs-600-rate-1200\queues-before.json`
- `queue-samples.jsonl` (25738 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-redo2-binlog0-20260419-141348\pairs-600-rate-1200\queue-samples.jsonl`
- `queues-after-k6.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-redo2-binlog0-20260419-141348\pairs-600-rate-1200\queues-after-k6.json`
- `queues-after-20s.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-redo2-binlog0-20260419-141348\pairs-600-rate-1200\queues-after-20s.json`
- `metrics-before.prom` (476003 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-redo2-binlog0-20260419-141348\pairs-600-rate-1200\metrics-before.prom`
- `metrics-after-k6.prom` (480874 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-redo2-binlog0-20260419-141348\pairs-600-rate-1200\metrics-after-k6.prom`
- `metrics-after-20s.prom` (480878 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-redo2-binlog0-20260419-141348\pairs-600-rate-1200\metrics-after-20s.prom`
- `mysql-digest.txt` (3277 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-redo2-binlog0-20260419-141348\pairs-600-rate-1200\mysql-digest.txt`
- `mysql-slow-group.txt` (0 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-redo2-binlog0-20260419-141348\pairs-600-rate-1200\mysql-slow-group.txt`
- `mysql-waits.txt` (510 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-redo2-binlog0-20260419-141348\pairs-600-rate-1200\mysql-waits.txt`

### 用户对 800，速率 1600 msg/s

目录：`D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-redo2-binlog0-20260419-141348\pairs-800-rate-1600`

- `summary.json` (399036 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-redo2-binlog0-20260419-141348\pairs-800-rate-1600\summary.json`
- `k6.log` (20585 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-redo2-binlog0-20260419-141348\pairs-800-rate-1600\k6.log`
- `k6.err.log` (0 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-redo2-binlog0-20260419-141348\pairs-800-rate-1600\k6.err.log`
- `k6-exit-code.txt` (3 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-redo2-binlog0-20260419-141348\pairs-800-rate-1600\k6-exit-code.txt`
- `queues-before.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-redo2-binlog0-20260419-141348\pairs-800-rate-1600\queues-before.json`
- `queue-samples.jsonl` (27474 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-redo2-binlog0-20260419-141348\pairs-800-rate-1600\queue-samples.jsonl`
- `queues-after-k6.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-redo2-binlog0-20260419-141348\pairs-800-rate-1600\queues-after-k6.json`
- `queues-after-20s.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-redo2-binlog0-20260419-141348\pairs-800-rate-1600\queues-after-20s.json`
- `metrics-before.prom` (480855 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-redo2-binlog0-20260419-141348\pairs-800-rate-1600\metrics-before.prom`
- `metrics-after-k6.prom` (480941 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-redo2-binlog0-20260419-141348\pairs-800-rate-1600\metrics-after-k6.prom`
- `metrics-after-20s.prom` (480945 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-redo2-binlog0-20260419-141348\pairs-800-rate-1600\metrics-after-20s.prom`
- `mysql-digest.txt` (3341 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-redo2-binlog0-20260419-141348\pairs-800-rate-1600\mysql-digest.txt`
- `mysql-slow-group.txt` (0 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-redo2-binlog0-20260419-141348\pairs-800-rate-1600\mysql-slow-group.txt`
- `mysql-waits.txt` (512 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-redo2-binlog0-20260419-141348\pairs-800-rate-1600\mysql-waits.txt`


