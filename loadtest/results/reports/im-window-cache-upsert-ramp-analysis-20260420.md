# IM 在线用户对阶梯压测记录

生成时间：2026-04-20 Asia/Shanghai

## 数据范围

本文档汇总本次阶梯压测每个挡位的结果数据。每个挡位包含 k6 汇总、RabbitMQ 队列快照和采样、应用侧 Prometheus 指标，以及 MySQL Performance Schema / slow log 导出的统计。

原始结果目录：
- `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407`

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
| 100 | 200 | 12000 | 12000 | 12000 | 12000 | 0 | 4.5 | 6.0 | 1,196.0 | - |
| 200 | 400 | 24000 | 24000 | 24000 | 24000 | 0 | 4.2 | 5.0 | 1,187.0 | - |
| 300 | 600 | 36000 | 36000 | 36000 | 36000 | 0 | 4.7 | 6.0 | 1,189.0 | - |
| 400 | 800 | 48000 | 48000 | 48000 | 48000 | 0 | 5.2 | 7.0 | 1,193.0 | - |
| 600 | 1200 | 72000 | 72000 | 72000 | 72000 | 0 | 9.7 | 20.0 | 815.0 | - |
| 800 | 1600 | 78240 | 78240 | 55440 | 78240 | 994 | 16.8 | 40.0 | 1,451.0 | - |

## RabbitMQ 队列积压汇总

`最大积压` 是 `queue-samples.jsonl` 里该队列 `messages` 的最大值。`>=100 采样次数` 表示采样时该队列积压至少 100 条的次数。`Consumer 最小/最大/最后` 来自压测期间 RabbitMQ 队列采样，用于观察 Spring AMQP 动态扩容情况。`k6 结束后` 和 `20s 后` 来自对应的队列快照文件。

| 用户对 | 速率 | 队列 | Consumer 最小 | Consumer 最大 | Consumer 最后 | 最大积压 | >=100 采样次数 | k6 结束后 | 20s 后 |
|---:|---:|---|---:|---:|---:|---:|---:|---:|---:|
| 100 | 200 | `im.message.persist.queue` | 2 | 4 | 3 | 2 | 0 | 0 | 0 |
| 100 | 200 | `im.message.conversation.queue` | 2 | 6 | 5 | 68 | 0 | 0 | 0 |
| 100 | 200 | `im.message.conversation.redis.queue` | 4 | 8 | 7 | 2 | 0 | 0 | 0 |
| 100 | 200 | `im.message.recent.cache.queue` | 4 | 8 | 7 | 0 | 0 | 0 | 0 |
| 100 | 200 | `im.message.realtime.queue` | 2 | 4 | 3 | 1 | 0 | 0 | 0 |
| 200 | 400 | `im.message.persist.queue` | 3 | 4 | 3 | 3 | 0 | 0 | 0 |
| 200 | 400 | `im.message.conversation.queue` | 5 | 6 | 5 | 105 | 9 | 0 | 0 |
| 200 | 400 | `im.message.conversation.redis.queue` | 7 | 8 | 7 | 3 | 0 | 0 | 0 |
| 200 | 400 | `im.message.recent.cache.queue` | 7 | 8 | 7 | 3 | 0 | 0 | 0 |
| 200 | 400 | `im.message.realtime.queue` | 3 | 4 | 3 | 2 | 0 | 0 | 0 |
| 300 | 600 | `im.message.persist.queue` | 3 | 4 | 3 | 3 | 0 | 0 | 0 |
| 300 | 600 | `im.message.conversation.queue` | 5 | 6 | 5 | 139 | 6 | 0 | 0 |
| 300 | 600 | `im.message.conversation.redis.queue` | 7 | 8 | 7 | 6 | 0 | 0 | 0 |
| 300 | 600 | `im.message.recent.cache.queue` | 7 | 8 | 7 | 3 | 0 | 0 | 0 |
| 300 | 600 | `im.message.realtime.queue` | 3 | 4 | 3 | 4 | 0 | 0 | 0 |
| 400 | 800 | `im.message.persist.queue` | 3 | 4 | 3 | 77 | 0 | 0 | 0 |
| 400 | 800 | `im.message.conversation.queue` | 5 | 6 | 5 | 1415 | 11 | 0 | 0 |
| 400 | 800 | `im.message.conversation.redis.queue` | 7 | 8 | 7 | 5 | 0 | 0 | 0 |
| 400 | 800 | `im.message.recent.cache.queue` | 7 | 8 | 7 | 5 | 0 | 0 | 0 |
| 400 | 800 | `im.message.realtime.queue` | 3 | 4 | 3 | 2 | 0 | 0 | 0 |
| 600 | 1200 | `im.message.persist.queue` | 3 | 4 | 3 | 412 | 6 | 0 | 0 |
| 600 | 1200 | `im.message.conversation.queue` | 5 | 6 | 6 | 44496 | 13 | 13072 | 0 |
| 600 | 1200 | `im.message.conversation.redis.queue` | 7 | 8 | 7 | 114 | 2 | 0 | 0 |
| 600 | 1200 | `im.message.recent.cache.queue` | 7 | 8 | 7 | 6 | 0 | 0 | 0 |
| 600 | 1200 | `im.message.realtime.queue` | 3 | 4 | 3 | 13 | 0 | 0 | 0 |
| 800 | 1600 | `im.message.persist.queue` | 3 | 4 | 3 | 4419 | 9 | 0 | 0 |
| 800 | 1600 | `im.message.conversation.queue` | 5 | 6 | 6 | 53832 | 13 | 24086 | 0 |
| 800 | 1600 | `im.message.conversation.redis.queue` | 7 | 8 | 7 | 532 | 5 | 0 | 0 |
| 800 | 1600 | `im.message.recent.cache.queue` | 7 | 8 | 7 | 8 | 0 | 0 | 0 |
| 800 | 1600 | `im.message.realtime.queue` | 3 | 4 | 3 | 10 | 0 | 0 | 0 |

## 应用侧 DB 操作指标

以下数据来自 `im_db_operation_duration_seconds_*`，按每个挡位的前后差值计算。

| 用户对 | 速率 | 操作 | 次数 | 平均 ms | 总耗时 s |
|---:|---:|---|---:|---:|---:|
| 100 | 200 | `chat_message_insert` | 12000 | 0.514 | 6.170 |
| 100 | 200 | `contact_relation_upsert` | 100 | 0.792 | 0.079 |
| 200 | 400 | `chat_message_insert` | 24000 | 0.457 | 10.978 |
| 200 | 400 | `contact_relation_upsert` | 200 | 0.372 | 0.074 |
| 300 | 600 | `chat_message_insert` | 36000 | 0.484 | 17.426 |
| 300 | 600 | `contact_relation_upsert` | 300 | 0.358 | 0.107 |
| 400 | 800 | `chat_message_insert` | 48000 | 0.503 | 24.133 |
| 400 | 800 | `contact_relation_upsert` | 400 | 0.393 | 0.157 |
| 600 | 1200 | `chat_message_insert` | 72000 | 0.529 | 38.074 |
| 600 | 1200 | `contact_relation_upsert` | 600 | 0.510 | 0.306 |
| 800 | 1600 | `chat_message_insert` | 78240 | 0.528 | 41.307 |
| 800 | 1600 | `contact_relation_upsert` | 652 | 0.410 | 0.268 |

## 应用侧 MQ Consumer 指标

以下数据来自 `im_mq_consumer_duration_seconds_*`，按每个挡位的前后差值计算。

| 用户对 | 速率 | Consumer | 队列 | 次数 | 平均 ms | 总耗时 s |
|---:|---:|---|---|---:|---:|---:|
| 100 | 200 | `single_conversation_persist` | `im.message.conversation.queue` | 12000 | 1.194 | 14.323 |
| 100 | 200 | `single_conversation_redis_projection` | `im.message.conversation.redis.queue` | 12000 | 2.121 | 25.457 |
| 100 | 200 | `single_message_persist` | `im.message.persist.queue` | 12000 | 0.815 | 9.781 |
| 100 | 200 | `single_realtime_push` | `im.message.realtime.queue` | 12000 | 0.240 | 2.882 |
| 100 | 200 | `single_recent_message_cache_projection` | `im.message.recent.cache.queue` | 12000 | 0.228 | 2.735 |
| 200 | 400 | `single_conversation_persist` | `im.message.conversation.queue` | 24000 | 1.122 | 26.939 |
| 200 | 400 | `single_conversation_redis_projection` | `im.message.conversation.redis.queue` | 24000 | 2.110 | 50.636 |
| 200 | 400 | `single_message_persist` | `im.message.persist.queue` | 24000 | 0.760 | 18.229 |
| 200 | 400 | `single_realtime_push` | `im.message.realtime.queue` | 24000 | 0.237 | 5.684 |
| 200 | 400 | `single_recent_message_cache_projection` | `im.message.recent.cache.queue` | 24000 | 0.225 | 5.405 |
| 300 | 600 | `single_conversation_persist` | `im.message.conversation.queue` | 36000 | 1.239 | 44.587 |
| 300 | 600 | `single_conversation_redis_projection` | `im.message.conversation.redis.queue` | 36000 | 2.617 | 94.227 |
| 300 | 600 | `single_message_persist` | `im.message.persist.queue` | 36000 | 0.871 | 31.357 |
| 300 | 600 | `single_realtime_push` | `im.message.realtime.queue` | 36000 | 0.295 | 10.626 |
| 300 | 600 | `single_recent_message_cache_projection` | `im.message.recent.cache.queue` | 36000 | 0.278 | 10.003 |
| 400 | 800 | `single_conversation_persist` | `im.message.conversation.queue` | 47998 | 1.423 | 68.292 |
| 400 | 800 | `single_conversation_redis_projection` | `im.message.conversation.redis.queue` | 48000 | 3.005 | 144.236 |
| 400 | 800 | `single_message_persist` | `im.message.persist.queue` | 48000 | 0.951 | 45.664 |
| 400 | 800 | `single_realtime_push` | `im.message.realtime.queue` | 48000 | 0.344 | 16.503 |
| 400 | 800 | `single_recent_message_cache_projection` | `im.message.recent.cache.queue` | 48000 | 0.323 | 15.481 |
| 600 | 1200 | `single_conversation_persist` | `im.message.conversation.queue` | 72000 | 1.161 | 83.580 |
| 600 | 1200 | `single_conversation_redis_projection` | `im.message.conversation.redis.queue` | 72000 | 4.910 | 353.504 |
| 600 | 1200 | `single_message_persist` | `im.message.persist.queue` | 72000 | 1.216 | 87.554 |
| 600 | 1200 | `single_realtime_push` | `im.message.realtime.queue` | 72000 | 0.610 | 43.929 |
| 600 | 1200 | `single_recent_message_cache_projection` | `im.message.recent.cache.queue` | 72000 | 0.585 | 42.120 |
| 800 | 1600 | `single_conversation_persist` | `im.message.conversation.queue` | 78240 | 1.123 | 87.874 |
| 800 | 1600 | `single_conversation_redis_projection` | `im.message.conversation.redis.queue` | 78240 | 5.097 | 398.762 |
| 800 | 1600 | `single_message_persist` | `im.message.persist.queue` | 78240 | 1.231 | 96.289 |
| 800 | 1600 | `single_realtime_push` | `im.message.realtime.queue` | 78240 | 0.598 | 46.804 |
| 800 | 1600 | `single_recent_message_cache_projection` | `im.message.recent.cache.queue` | 78240 | 0.611 | 47.838 |

## MySQL Digest Top 语句

以下 Top 行来自每个挡位的 `mysql-digest.txt`。

### 用户对 100，速率 200 msg/s

| 次数 | 总耗时 s | 平均 ms | 最大 ms | Digest |
|---:|---:|---:|---:|---|
| 72719 | 5.7413 | 0.0790 | 1176.4215 | `SET `autocommit` = ?` |
| 36197 | 5.7061 | 0.1576 | 724.2068 | `COMMIT` |
| 23995 | 3.2852 | 0.1369 | 724.1121 | `SELECT `id` , `username` , PASSWORD , `role_code` , STATUS , `create_time` , `update_time` , `delete_time` FROM `t_user` WHERE `id` = ? AND STATUS = ?` |
| 12177 | 3.1466 | 0.2584 | 75.8017 | `INSERT INTO `chat_conversation` ( `conversation_id` , `owner_user_id` , `target_id` , TYPE , `last_message` , `last_message_time` , `last_server_message_id` , `unread_count` , `is_...` |
| 12000 | 2.3752 | 0.1979 | 5.8445 | `INSERT INTO `chat_message` ( `server_message_id` , `conversation_id` , `conversation_type` , `sender_id` , `receiver_id` , `client_message_id` , `sender_location` , `message_type` ...` |
| 12177 | 2.3548 | 0.1934 | 4.6176 | `INSERT INTO `chat_conversation` ( `conversation_id` , `owner_user_id` , `target_id` , TYPE , `last_message` , `last_message_time` , `last_server_message_id` , `unread_count` , `is_...` |
| 36953 | 2.2725 | 0.0615 | 0.8981 | `SELECT @@SESSION . `transaction_read_only`` |
| 23998 | 2.0699 | 0.0863 | 0.8236 | `SELECT `user_id` AS `userId` , `target_user_id` AS `targetUserId` , `is_contact` AS `isContact` , `is_dm_contact` AS `isDmContact` , `is_blocked` AS `isBlocked` , `is_muted` AS `is...` |

### 用户对 200，速率 400 msg/s

| 次数 | 总耗时 s | 平均 ms | 最大 ms | Digest |
|---:|---:|---:|---:|---|
| 72397 | 9.4963 | 0.1312 | 6.9210 | `COMMIT` |
| 145303 | 8.6200 | 0.0593 | 1182.2865 | `SET `autocommit` = ?` |
| 24274 | 5.5128 | 0.2271 | 6.0498 | `INSERT INTO `chat_conversation` ( `conversation_id` , `owner_user_id` , `target_id` , TYPE , `last_message` , `last_message_time` , `last_server_message_id` , `unread_count` , `is_...` |
| 47993 | 4.8643 | 0.1014 | 0.9295 | `SELECT `id` , `username` , PASSWORD , `role_code` , STATUS , `create_time` , `update_time` , `delete_time` FROM `t_user` WHERE `id` = ? AND STATUS = ?` |
| 47997 | 4.4710 | 0.0932 | 0.7350 | `SELECT `user_id` AS `userId` , `target_user_id` AS `targetUserId` , `is_contact` AS `isContact` , `is_dm_contact` AS `isDmContact` , `is_blocked` AS `isBlocked` , `is_muted` AS `is...` |
| 73746 | 4.3808 | 0.0594 | 0.6349 | `SELECT @@SESSION . `transaction_read_only`` |
| 24274 | 4.3668 | 0.1799 | 3.7267 | `INSERT INTO `chat_conversation` ( `conversation_id` , `owner_user_id` , `target_id` , TYPE , `last_message` , `last_message_time` , `last_server_message_id` , `unread_count` , `is_...` |
| 23999 | 4.3191 | 0.1800 | 6.2729 | `INSERT INTO `chat_message` ( `server_message_id` , `conversation_id` , `conversation_type` , `sender_id` , `receiver_id` , `client_message_id` , `sender_location` , `message_type` ...` |

### 用户对 300，速率 600 msg/s

| 次数 | 总耗时 s | 平均 ms | 最大 ms | Digest |
|---:|---:|---:|---:|---|
| 217675 | 13.6110 | 0.0625 | 799.9962 | `SET `autocommit` = ?` |
| 36260 | 10.0206 | 0.2764 | 1184.1443 | `INSERT INTO `chat_conversation` ( `conversation_id` , `owner_user_id` , `target_id` , TYPE , `last_message` , `last_message_time` , `last_server_message_id` , `unread_count` , `is_...` |
| 108594 | 9.5879 | 0.0883 | 32.9982 | `COMMIT` |
| 36260 | 8.4701 | 0.2336 | 800.5220 | `INSERT INTO `chat_conversation` ( `conversation_id` , `owner_user_id` , `target_id` , TYPE , `last_message` , `last_message_time` , `last_server_message_id` , `unread_count` , `is_...` |
| 71989 | 7.9290 | 0.1101 | 4.0947 | `SELECT `id` , `username` , PASSWORD , `role_code` , STATUS , `create_time` , `update_time` , `delete_time` FROM `t_user` WHERE `id` = ? AND STATUS = ?` |
| 71991 | 7.5343 | 0.1047 | 2.0595 | `SELECT `user_id` AS `userId` , `target_user_id` AS `targetUserId` , `is_contact` AS `isContact` , `is_dm_contact` AS `isDmContact` , `is_blocked` AS `isBlocked` , `is_muted` AS `is...` |
| 110313 | 6.9598 | 0.0631 | 1.3378 | `SELECT @@SESSION . `transaction_read_only`` |
| 35994 | 6.7060 | 0.1863 | 12.0171 | `INSERT INTO `chat_message` ( `server_message_id` , `conversation_id` , `conversation_type` , `sender_id` , `receiver_id` , `client_message_id` , `sender_location` , `message_type` ...` |

### 用户对 400，速率 800 msg/s

| 次数 | 总耗时 s | 平均 ms | 最大 ms | Digest |
|---:|---:|---:|---:|---|
| 289992 | 17.7951 | 0.0614 | 789.8448 | `SET `autocommit` = ?` |
| 144786 | 15.2412 | 0.1053 | 153.7614 | `COMMIT` |
| 48214 | 14.6057 | 0.3029 | 71.0613 | `INSERT INTO `chat_conversation` ( `conversation_id` , `owner_user_id` , `target_id` , TYPE , `last_message` , `last_message_time` , `last_server_message_id` , `unread_count` , `is_...` |
| 95987 | 11.2796 | 0.1175 | 3.8642 | `SELECT `id` , `username` , PASSWORD , `role_code` , STATUS , `create_time` , `update_time` , `delete_time` FROM `t_user` WHERE `id` = ? AND STATUS = ?` |
| 146817 | 11.0322 | 0.0751 | 1184.3984 | `SELECT @@SESSION . `transaction_read_only`` |
| 95983 | 10.8160 | 0.1127 | 1.5861 | `SELECT `user_id` AS `userId` , `target_user_id` AS `targetUserId` , `is_contact` AS `isContact` , `is_dm_contact` AS `isDmContact` , `is_blocked` AS `isBlocked` , `is_muted` AS `is...` |
| 48214 | 10.1636 | 0.2108 | 10.1620 | `INSERT INTO `chat_conversation` ( `conversation_id` , `owner_user_id` , `target_id` , TYPE , `last_message` , `last_message_time` , `last_server_message_id` , `unread_count` , `is_...` |
| 47989 | 9.3915 | 0.1957 | 11.2997 | `INSERT INTO `chat_message` ( `server_message_id` , `conversation_id` , `conversation_type` , `sender_id` , `receiver_id` , `client_message_id` , `sender_location` , `message_type` ...` |

### 用户对 600，速率 1200 msg/s

| 次数 | 总耗时 s | 平均 ms | 最大 ms | Digest |
|---:|---:|---:|---:|---|
| 217181 | 28.3896 | 0.1307 | 851.0354 | `COMMIT` |
| 435093 | 26.2195 | 0.0603 | 1207.1092 | `SET `autocommit` = ?` |
| 143979 | 19.5952 | 0.1361 | 752.7200 | `SELECT `id` , `username` , PASSWORD , `role_code` , STATUS , `create_time` , `update_time` , `delete_time` FROM `t_user` WHERE `id` = ? AND STATUS = ?` |
| 72370 | 19.3737 | 0.2677 | 1189.4570 | `INSERT INTO `chat_conversation` ( `conversation_id` , `owner_user_id` , `target_id` , TYPE , `last_message` , `last_message_time` , `last_server_message_id` , `unread_count` , `is_...` |
| 143986 | 18.2778 | 0.1269 | 6.3680 | `SELECT `user_id` AS `userId` , `target_user_id` AS `targetUserId` , `is_contact` AS `isContact` , `is_dm_contact` AS `isDmContact` , `is_blocked` AS `isBlocked` , `is_muted` AS `is...` |
| 71991 | 15.6652 | 0.2176 | 808.3622 | `INSERT INTO `chat_message` ( `server_message_id` , `conversation_id` , `conversation_type` , `sender_id` , `receiver_id` , `client_message_id` , `sender_location` , `message_type` ...` |
| 220328 | 13.7057 | 0.0622 | 808.3359 | `SELECT @@SESSION . `transaction_read_only`` |
| 72370 | 13.4282 | 0.1855 | 788.9436 | `INSERT INTO `chat_conversation` ( `conversation_id` , `owner_user_id` , `target_id` , TYPE , `last_message` , `last_message_time` , `last_server_message_id` , `unread_count` , `is_...` |

### 用户对 800，速率 1600 msg/s

| 次数 | 总耗时 s | 平均 ms | 最大 ms | Digest |
|---:|---:|---:|---:|---|
| 236310 | 28.2727 | 0.1196 | 1445.8532 | `COMMIT` |
| 473387 | 27.3935 | 0.0579 | 526.2520 | `SET `autocommit` = ?` |
| 156462 | 22.4395 | 0.1434 | 1445.5725 | `SELECT `id` , `username` , PASSWORD , `role_code` , STATUS , `create_time` , `update_time` , `delete_time` FROM `t_user` WHERE `id` = ? AND STATUS = ?` |
| 156458 | 20.6037 | 0.1317 | 801.2270 | `SELECT `user_id` AS `userId` , `target_user_id` AS `targetUserId` , `is_contact` AS `isContact` , `is_dm_contact` AS `isDmContact` , `is_blocked` AS `isBlocked` , `is_muted` AS `is...` |
| 78232 | 20.4236 | 0.2611 | 1445.7779 | `INSERT INTO `chat_message` ( `server_message_id` , `conversation_id` , `conversation_type` , `sender_id` , `receiver_id` , `client_message_id` , `sender_location` , `message_type` ...` |
| 78638 | 18.1154 | 0.2304 | 75.2166 | `INSERT INTO `chat_conversation` ( `conversation_id` , `owner_user_id` , `target_id` , TYPE , `last_message` , `last_message_time` , `last_server_message_id` , `unread_count` , `is_...` |
| 240010 | 14.4491 | 0.0602 | 799.7079 | `SELECT @@SESSION . `transaction_read_only`` |
| 78638 | 14.3314 | 0.1822 | 1445.7149 | `INSERT INTO `chat_conversation` ( `conversation_id` , `owner_user_id` , `target_id` , TYPE , `last_message` , `last_message_time` , `last_server_message_id` , `unread_count` , `is_...` |

## MySQL Waits Top 事件

以下 Top 行来自每个挡位的 `mysql-waits.txt`，已排除 `idle`。

### 用户对 100，速率 200 msg/s

| 事件 | 次数 | 总耗时 s | 平均 ms | 最大 ms |
|---|---:|---:|---:|---:|
| `wait/io/table/sql/handler` | 168756 | 4.2753 | 0.0253 | 81.3463 |
| `wait/io/file/innodb/innodb_data_file` | 11756 | 2.5093 | 0.2135 | 5.6839 |
| `wait/io/file/innodb/innodb_dblwr_file` | 4022 | 1.4573 | 0.3623 | 4.8293 |
| `wait/io/file/innodb/innodb_log_file` | 75981 | 0.8243 | 0.0108 | 7.6584 |
| `wait/io/file/sql/binlog` | 24231 | 0.1920 | 0.0079 | 0.2252 |
| `wait/lock/table/sql/handler` | 122154 | 0.0750 | 0.0006 | 0.0541 |
| `wait/io/file/csv/metadata` | 7 | 0.0007 | 0.0947 | 0.3819 |
| `wait/io/file/csv/data` | 6 | 0.0001 | 0.0120 | 0.0324 |

### 用户对 200，速率 400 msg/s

| 事件 | 次数 | 总耗时 s | 平均 ms | 最大 ms |
|---|---:|---:|---:|---:|
| `wait/io/table/sql/handler` | 337966 | 7.0846 | 0.0210 | 6.5501 |
| `wait/io/file/innodb/innodb_data_file` | 11972 | 2.6639 | 0.2225 | 4.8802 |
| `wait/io/file/innodb/innodb_dblwr_file` | 4100 | 1.6124 | 0.3933 | 15.8939 |
| `wait/io/file/innodb/innodb_log_file` | 134982 | 1.0609 | 0.0079 | 3.2090 |
| `wait/io/file/sql/binlog` | 47710 | 0.3420 | 0.0072 | 0.2469 |
| `wait/lock/table/sql/handler` | 244148 | 0.1361 | 0.0006 | 0.3538 |
| `wait/io/file/csv/data` | 4 | 0.0001 | 0.0163 | 0.0322 |

### 用户对 300，速率 600 msg/s

| 事件 | 次数 | 总耗时 s | 平均 ms | 最大 ms |
|---|---:|---:|---:|---:|
| `wait/io/table/sql/handler` | 507324 | 11.1809 | 0.0220 | 12.5806 |
| `wait/io/file/innodb/innodb_data_file` | 15328 | 4.5574 | 0.2973 | 28.0275 |
| `wait/io/file/innodb/innodb_dblwr_file` | 5154 | 2.9516 | 0.5727 | 12.8445 |
| `wait/io/file/innodb/innodb_log_file` | 1201426 | 1.5969 | 0.0013 | 7.6287 |
| `wait/io/file/sql/binlog` | 72022 | 0.5466 | 0.0076 | 0.4511 |
| `wait/lock/table/sql/handler` | 365920 | 0.2140 | 0.0006 | 0.4768 |
| `wait/io/file/csv/data` | 4 | 0.0001 | 0.0136 | 0.0288 |

### 用户对 400，速率 800 msg/s

| 事件 | 次数 | 总耗时 s | 平均 ms | 最大 ms |
|---|---:|---:|---:|---:|
| `wait/io/table/sql/handler` | 677706 | 18.4296 | 0.0272 | 76.2740 |
| `wait/io/file/innodb/innodb_data_file` | 21025 | 6.9294 | 0.3296 | 13.4582 |
| `wait/io/file/innodb/innodb_dblwr_file` | 7104 | 4.7354 | 0.6666 | 13.4585 |
| `wait/io/file/innodb/innodb_log_file` | 1639310 | 2.7537 | 0.0017 | 20.5064 |
| `wait/io/file/sql/binlog` | 95770 | 0.7195 | 0.0075 | 1.1716 |
| `wait/lock/table/sql/handler` | 487628 | 0.2973 | 0.0006 | 0.2359 |
| `wait/io/file/csv/data` | 4 | 0.0001 | 0.0139 | 0.0281 |

### 用户对 600，速率 1200 msg/s

| 事件 | 次数 | 总耗时 s | 平均 ms | 最大 ms |
|---|---:|---:|---:|---:|
| `wait/io/table/sql/handler` | 1017345 | 24.7008 | 0.0243 | 26.8369 |
| `wait/io/file/innodb/innodb_data_file` | 27884 | 9.0975 | 0.3263 | 10.6047 |
| `wait/io/file/innodb/innodb_dblwr_file` | 9570 | 6.1816 | 0.6459 | 34.0193 |
| `wait/io/file/innodb/innodb_log_file` | 2278179 | 3.6627 | 0.0016 | 24.9000 |
| `wait/io/file/sql/binlog` | 144036 | 1.0199 | 0.0071 | 2.9084 |
| `wait/lock/table/sql/handler` | 731540 | 0.4545 | 0.0006 | 0.7040 |
| `wait/io/file/csv/data` | 4 | 0.0001 | 0.0199 | 0.0398 |

### 用户对 800，速率 1600 msg/s

| 事件 | 次数 | 总耗时 s | 平均 ms | 最大 ms |
|---|---:|---:|---:|---:|
| `wait/io/table/sql/handler` | 1107575 | 25.9288 | 0.0234 | 80.2642 |
| `wait/io/file/innodb/innodb_data_file` | 32403 | 10.7227 | 0.3309 | 15.7743 |
| `wait/io/file/innodb/innodb_dblwr_file` | 10982 | 7.2707 | 0.6621 | 14.0028 |
| `wait/io/file/innodb/innodb_log_file` | 2101524 | 3.7666 | 0.0018 | 18.9074 |
| `wait/io/file/sql/binlog` | 156717 | 1.1620 | 0.0074 | 21.1427 |
| `wait/lock/table/sql/handler` | 796697 | 0.4961 | 0.0006 | 2.4571 |
| `wait/io/file/csv/data` | 4 | 0.0001 | 0.0193 | 0.0366 |

## 原始文件索引

### 用户对 100，速率 200 msg/s

目录：`D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-100-rate-200`

- `summary.json` (59987 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-100-rate-200\summary.json`
- `k6.log` (17689 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-100-rate-200\k6.log`
- `k6.err.log` (0 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-100-rate-200\k6.err.log`
- `k6-exit-code.txt` (3 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-100-rate-200\k6-exit-code.txt`
- `queues-before.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-100-rate-200\queues-before.json`
- `queue-samples.jsonl` (27278 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-100-rate-200\queue-samples.jsonl`
- `queues-after-k6.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-100-rate-200\queues-after-k6.json`
- `queues-after-20s.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-100-rate-200\queues-after-20s.json`
- `metrics-before.prom` (79839 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-100-rate-200\metrics-before.prom`
- `metrics-after-k6.prom` (523631 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-100-rate-200\metrics-after-k6.prom`
- `metrics-after-20s.prom` (523623 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-100-rate-200\metrics-after-20s.prom`
- `mysql-digest.txt` (4671 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-100-rate-200\mysql-digest.txt`
- `mysql-slow-group.txt` (0 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-100-rate-200\mysql-slow-group.txt`
- `mysql-waits.txt` (544 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-100-rate-200\mysql-waits.txt`

### 用户对 200，速率 400 msg/s

目录：`D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-200-rate-400`

- `summary.json` (108874 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-200-rate-400\summary.json`
- `k6.log` (17344 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-200-rate-400\k6.log`
- `k6.err.log` (0 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-200-rate-400\k6.err.log`
- `k6-exit-code.txt` (3 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-200-rate-400\k6-exit-code.txt`
- `queues-before.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-200-rate-400\queues-before.json`
- `queue-samples.jsonl` (27296 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-200-rate-400\queue-samples.jsonl`
- `queues-after-k6.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-200-rate-400\queues-after-k6.json`
- `queues-after-20s.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-200-rate-400\queues-after-20s.json`
- `metrics-before.prom` (523621 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-200-rate-400\metrics-before.prom`
- `metrics-after-k6.prom` (523669 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-200-rate-400\metrics-after-k6.prom`
- `metrics-after-20s.prom` (523672 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-200-rate-400\metrics-after-20s.prom`
- `mysql-digest.txt` (4667 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-200-rate-400\mysql-digest.txt`
- `mysql-slow-group.txt` (0 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-200-rate-400\mysql-slow-group.txt`
- `mysql-waits.txt` (496 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-200-rate-400\mysql-waits.txt`

### 用户对 300，速率 600 msg/s

目录：`D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-300-rate-600`

- `summary.json` (157772 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-300-rate-600\summary.json`
- `k6.log` (17610 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-300-rate-600\k6.log`
- `k6.err.log` (0 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-300-rate-600\k6.err.log`
- `k6-exit-code.txt` (3 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-300-rate-600\k6-exit-code.txt`
- `queues-before.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-300-rate-600\queues-before.json`
- `queue-samples.jsonl` (27290 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-300-rate-600\queue-samples.jsonl`
- `queues-after-k6.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-300-rate-600\queues-after-k6.json`
- `queues-after-20s.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-300-rate-600\queues-after-20s.json`
- `metrics-before.prom` (523669 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-300-rate-600\metrics-before.prom`
- `metrics-after-k6.prom` (525040 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-300-rate-600\metrics-after-k6.prom`
- `metrics-after-20s.prom` (525044 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-300-rate-600\metrics-after-20s.prom`
- `mysql-digest.txt` (4680 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-300-rate-600\mysql-digest.txt`
- `mysql-slow-group.txt` (0 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-300-rate-600\mysql-slow-group.txt`
- `mysql-waits.txt` (500 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-300-rate-600\mysql-waits.txt`

### 用户对 400，速率 800 msg/s

目录：`D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-400-rate-800`

- `summary.json` (206451 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-400-rate-800\summary.json`
- `k6.log` (17895 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-400-rate-800\k6.log`
- `k6.err.log` (0 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-400-rate-800\k6.err.log`
- `k6-exit-code.txt` (3 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-400-rate-800\k6-exit-code.txt`
- `queues-before.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-400-rate-800\queues-before.json`
- `queue-samples.jsonl` (27332 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-400-rate-800\queue-samples.jsonl`
- `queues-after-k6.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-400-rate-800\queues-after-k6.json`
- `queues-after-20s.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-400-rate-800\queues-after-20s.json`
- `metrics-before.prom` (525042 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-400-rate-800\metrics-before.prom`
- `metrics-after-k6.prom` (528627 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-400-rate-800\metrics-after-k6.prom`
- `metrics-after-20s.prom` (528628 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-400-rate-800\metrics-after-20s.prom`
- `mysql-digest.txt` (4683 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-400-rate-800\mysql-digest.txt`
- `mysql-slow-group.txt` (0 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-400-rate-800\mysql-slow-group.txt`
- `mysql-waits.txt` (501 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-400-rate-800\mysql-waits.txt`

### 用户对 600，速率 1200 msg/s

目录：`D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-600-rate-1200`

- `summary.json` (304266 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-600-rate-1200\summary.json`
- `k6.log` (18820 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-600-rate-1200\k6.log`
- `k6.err.log` (0 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-600-rate-1200\k6.err.log`
- `k6-exit-code.txt` (3 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-600-rate-1200\k6-exit-code.txt`
- `queues-before.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-600-rate-1200\queues-before.json`
- `queue-samples.jsonl` (27439 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-600-rate-1200\queue-samples.jsonl`
- `queues-after-k6.json` (1571 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-600-rate-1200\queues-after-k6.json`
- `queues-after-20s.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-600-rate-1200\queues-after-20s.json`
- `metrics-before.prom` (528624 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-600-rate-1200\metrics-before.prom`
- `metrics-after-k6.prom` (528988 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-600-rate-1200\metrics-after-k6.prom`
- `metrics-after-20s.prom` (529004 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-600-rate-1200\metrics-after-20s.prom`
- `mysql-digest.txt` (4698 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-600-rate-1200\mysql-digest.txt`
- `mysql-slow-group.txt` (0 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-600-rate-1200\mysql-slow-group.txt`
- `mysql-waits.txt` (503 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-600-rate-1200\mysql-waits.txt`

### 用户对 800，速率 1600 msg/s

目录：`D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-800-rate-1600`

- `summary.json` (402412 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-800-rate-1600\summary.json`
- `k6.log` (19550 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-800-rate-1600\k6.log`
- `k6.err.log` (58754 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-800-rate-1600\k6.err.log`
- `k6-exit-code.txt` (3 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-800-rate-1600\k6-exit-code.txt`
- `queues-before.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-800-rate-1600\queues-before.json`
- `queue-samples.jsonl` (27462 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-800-rate-1600\queue-samples.jsonl`
- `queues-after-k6.json` (1571 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-800-rate-1600\queues-after-k6.json`
- `queues-after-20s.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-800-rate-1600\queues-after-20s.json`
- `metrics-before.prom` (529008 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-800-rate-1600\metrics-before.prom`
- `metrics-after-k6.prom` (529016 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-800-rate-1600\metrics-after-k6.prom`
- `metrics-after-20s.prom` (529008 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-800-rate-1600\metrics-after-20s.prom`
- `mysql-digest.txt` (4709 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-800-rate-1600\mysql-digest.txt`
- `mysql-slow-group.txt` (0 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-800-rate-1600\mysql-slow-group.txt`
- `mysql-waits.txt` (506 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-204407\pairs-800-rate-1600\mysql-waits.txt`

## 本次补充结论

本次压测使用 `im_ws_online_pairs_window_cache.js`，会在发送前调用 `/me/im/conversations` 预热 sender/receiver 的 Redis 会话窗口缓存，因此 `single_conversation_redis_projection` 不是空跑，而是会进入 Redis Lua 窗口投影链路。

### 容量判断

| 用户对 | 目标 msg/s | Accepted | 接收方收到 | Check 失败 | Accepted P95 ms | 窗口读次数 | 窗口读 P95 ms |
|---:|---:|---:|---:|---:|---:|---:|---:|
| 100 | 200 | 12000 | 12000 | 0 | 6 | 400 | 35 |
| 200 | 400 | 24000 | 24000 | 0 | 5 | 800 | 13 |
| 300 | 600 | 36000 | 36000 | 0 | 6 | 1200 | 29 |
| 400 | 800 | 48000 | 48000 | 0 | 7 | 1600 | 11 |
| 600 | 1200 | 72000 | 72000 | 0 | 20 | 2400 | 24 |
| 800 | 1600 | 78240 | 55440 | 994 | 40 | 3200 | 53 |

- `1200 msg/s` 挡位仍能完成 accepted、receiver received 和 20s drain，属于本次配置下的可恢复高压区。
- `1600 msg/s` 挡位出现 WebSocket 连接和窗口读取失败，接收方只收到 55440 / 78240，已经超过稳定容量。
- 首个明显积压队列仍是 `im.message.conversation.queue`，不是 Redis projection 队列。

### Redis 窗口投影指标

| 用户对 | 目标 msg/s | `cmdstat_evalsha` calls | `evalsha` 平均 us | `cmdstat_hget` calls | `cmdstat_zadd` calls | `cmdstat_sadd` calls |
|---:|---:|---:|---:|---:|---:|---:|
| 100 | 200 | 24171 | 61.24 | 48171 | 24086 | 11929 |
| 200 | 400 | 48343 | 54.84 | 96343 | 48192 | 23857 |
| 300 | 600 | 72427 | 55.84 | 144427 | 72285 | 35873 |
| 400 | 800 | 96465 | 57.20 | 192465 | 96398 | 47935 |
| 600 | 1200 | 144693 | 64.64 | 288693 | 144587 | 71907 |
| 800 | 1600 | 157235 | 58.56 | 313715 | 157122 | 78137 |

Redis `evalsha` 平均耗时保持在约 55-65us，`im.message.conversation.redis.queue` 最大积压在 `1200 msg/s` 为 114、`1600 msg/s` 为 532，且 20s 后均为 0。说明本次 Redis Lua 投影不是主瓶颈。

### DB conversation upsert 指标

`mysql-conversation-digest.txt` 显示发送方和接收方窗口都已经进入 `INSERT ... ON DUPLICATE KEY UPDATE` 路径。`chat_conversation` 的两条 upsert 在高压下平均耗时大致为：

| 用户对 | 目标 msg/s | 发送方 upsert 平均 ms | 接收方 upsert 平均 ms |
|---:|---:|---:|---:|
| 100 | 200 | 0.2584 | 0.1934 |
| 200 | 400 | 0.2271 | 0.1799 |
| 300 | 600 | 0.2764 | 0.2336 |
| 400 | 800 | 0.3029 | 0.2108 |
| 600 | 1200 | 0.2677 | 0.1855 |
| 800 | 1600 | 0.2304 | 0.1822 |

虽然单条 upsert 平均耗时不高，但 `im.message.conversation.queue` 在 `800 msg/s` 开始有明显瞬时积压，在 `1200 msg/s` 和 `1600 msg/s` 出现大积压后再恢复，说明 conversation 持久化消费者仍是最先被打满的下游环节。后续如果继续优化，应优先看 `single_conversation_persist` 消费者并发、prefetch、批量写入或按会话分片削峰，而不是 Redis Lua。

### 注意事项

本次 `k6-exit-code.txt` 写成了 `{}`，原因是 runner 在 Windows PowerShell 下读取 `Start-Process` 的 `ExitCode` 不兼容。压测 summary、队列采样、Prometheus、MySQL、Redis artifacts 均已正常生成；runner 已在脚本中修复，后续运行会写入数字 exit code。
