# IM 在线用户对阶梯压测记录

生成时间：2026-04-21 Asia/Shanghai

## 数据范围

本文档汇总本次阶梯压测每个挡位的结果数据。每个挡位包含 k6 汇总、RabbitMQ 队列快照和采样、应用侧 Prometheus 指标，以及 MySQL Performance Schema / slow log 导出的统计。

原始结果目录：
- `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020`

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
| 100 | 200 | 12000 | 12000 | 12000 | 12000 | 0 | 4.4 | 6.0 | 618.0 | 0 |
| 200 | 400 | 24000 | 24000 | 24000 | 24000 | 0 | 4.1 | 5.0 | 613.0 | 0 |
| 300 | 600 | 36000 | 36000 | 36000 | 36000 | 0 | 4.4 | 5.0 | 469.0 | 0 |
| 400 | 800 | 48000 | 48000 | 48000 | 48000 | 0 | 5.3 | 8.0 | 622.0 | 0 |
| 600 | 1200 | 72000 | 72000 | 72000 | 72000 | 0 | 10.2 | 22.0 | 738.0 | 0 |
| 800 | 1600 | 87240 | 87240 | 74760 | 87240 | 184 | 10.7 | 23.0 | 646.0 | 0 |

## RabbitMQ 队列积压汇总

`最大积压` 是 `queue-samples.jsonl` 里该队列 `messages` 的最大值。`>=100 采样次数` 表示采样时该队列积压至少 100 条的次数。`Consumer 最小/最大/最后` 来自压测期间 RabbitMQ 队列采样，用于观察 Spring AMQP 动态扩容情况。`k6 结束后` 和 `20s 后` 来自对应的队列快照文件。

| 用户对 | 速率 | 队列 | Consumer 最小 | Consumer 最大 | Consumer 最后 | 最大积压 | >=100 采样次数 | k6 结束后 | 20s 后 |
|---:|---:|---|---:|---:|---:|---:|---:|---:|---:|
| 100 | 200 | `im.message.persist.queue` | 2 | 4 | 3 | 1 | 0 | 0 | 0 |
| 100 | 200 | `im.message.conversation.queue` | 2 | 6 | 5 | 2 | 0 | 0 | 0 |
| 100 | 200 | `im.message.conversation.redis.queue` | 4 | 8 | 7 | 1 | 0 | 0 | 0 |
| 100 | 200 | `im.message.recent.cache.queue` | 4 | 8 | 7 | 0 | 0 | 0 | 0 |
| 100 | 200 | `im.message.realtime.queue` | 2 | 4 | 3 | 1 | 0 | 0 | 0 |
| 200 | 400 | `im.message.persist.queue` | 3 | 4 | 3 | 2 | 0 | 0 | 0 |
| 200 | 400 | `im.message.conversation.queue` | 5 | 6 | 5 | 5 | 0 | 0 | 0 |
| 200 | 400 | `im.message.conversation.redis.queue` | 7 | 8 | 7 | 3 | 0 | 0 | 0 |
| 200 | 400 | `im.message.recent.cache.queue` | 7 | 8 | 7 | 0 | 0 | 0 | 0 |
| 200 | 400 | `im.message.realtime.queue` | 3 | 4 | 3 | 1 | 0 | 0 | 0 |
| 300 | 600 | `im.message.persist.queue` | 3 | 4 | 3 | 2 | 0 | 0 | 0 |
| 300 | 600 | `im.message.conversation.queue` | 5 | 6 | 5 | 5 | 0 | 0 | 0 |
| 300 | 600 | `im.message.conversation.redis.queue` | 7 | 8 | 7 | 3 | 0 | 0 | 0 |
| 300 | 600 | `im.message.recent.cache.queue` | 7 | 8 | 7 | 6 | 0 | 0 | 0 |
| 300 | 600 | `im.message.realtime.queue` | 3 | 4 | 3 | 3 | 0 | 0 | 0 |
| 400 | 800 | `im.message.persist.queue` | 3 | 4 | 3 | 5 | 0 | 0 | 0 |
| 400 | 800 | `im.message.conversation.queue` | 5 | 6 | 5 | 413 | 1 | 0 | 0 |
| 400 | 800 | `im.message.conversation.redis.queue` | 7 | 8 | 7 | 4 | 0 | 0 | 0 |
| 400 | 800 | `im.message.recent.cache.queue` | 7 | 8 | 7 | 4 | 0 | 0 | 0 |
| 400 | 800 | `im.message.realtime.queue` | 3 | 4 | 3 | 8 | 0 | 0 | 0 |
| 600 | 1200 | `im.message.persist.queue` | 3 | 4 | 3 | 546 | 7 | 0 | 0 |
| 600 | 1200 | `im.message.conversation.queue` | 5 | 6 | 5 | 312 | 4 | 0 | 0 |
| 600 | 1200 | `im.message.conversation.redis.queue` | 7 | 8 | 7 | 12 | 0 | 0 | 0 |
| 600 | 1200 | `im.message.recent.cache.queue` | 7 | 8 | 7 | 12 | 0 | 0 | 0 |
| 600 | 1200 | `im.message.realtime.queue` | 3 | 4 | 3 | 8 | 0 | 0 | 0 |
| 800 | 1600 | `im.message.persist.queue` | 3 | 4 | 4 | 25369 | 10 | 0 | 0 |
| 800 | 1600 | `im.message.conversation.queue` | 5 | 6 | 5 | 7897 | 10 | 0 | 0 |
| 800 | 1600 | `im.message.conversation.redis.queue` | 7 | 8 | 7 | 18 | 0 | 0 | 0 |
| 800 | 1600 | `im.message.recent.cache.queue` | 7 | 8 | 7 | 6 | 0 | 0 | 0 |
| 800 | 1600 | `im.message.realtime.queue` | 3 | 4 | 3 | 20 | 0 | 0 | 0 |

## 应用侧 DB 操作指标

以下数据来自 `im_db_operation_duration_seconds_*`，按每个挡位的前后差值计算。

| 用户对 | 速率 | 操作 | 次数 | 平均 ms | 总耗时 s |
|---:|---:|---|---:|---:|---:|
| 100 | 200 | `chat_message_insert` | 12000 | 0.508 | 6.096 |
| 100 | 200 | `contact_relation_upsert` | 100 | 0.527 | 0.053 |
| 200 | 400 | `chat_message_insert` | 24000 | 0.473 | 11.358 |
| 200 | 400 | `contact_relation_upsert` | 200 | 0.488 | 0.098 |
| 300 | 600 | `chat_message_insert` | 36000 | 0.495 | 17.827 |
| 300 | 600 | `contact_relation_upsert` | 300 | 0.340 | 0.102 |
| 400 | 800 | `chat_message_insert` | 48000 | 0.529 | 25.412 |
| 400 | 800 | `contact_relation_upsert` | 400 | 0.380 | 0.152 |
| 600 | 1200 | `chat_message_insert` | 72000 | 0.534 | 38.455 |
| 600 | 1200 | `contact_relation_upsert` | 600 | 0.397 | 0.238 |
| 800 | 1600 | `chat_message_insert` | 87240 | 0.509 | 44.412 |
| 800 | 1600 | `contact_relation_upsert` | 727 | 0.442 | 0.321 |

## 应用侧 MQ Consumer 指标

以下数据来自 `im_mq_consumer_duration_seconds_*`，按每个挡位的前后差值计算。

| 用户对 | 速率 | Consumer | 队列 | 次数 | 平均 ms | 总耗时 s |
|---:|---:|---|---|---:|---:|---:|
| 100 | 200 | `single_conversation_persist` | `im.message.conversation.queue` | 12000 | 1.133 | 13.601 |
| 100 | 200 | `single_conversation_redis_projection` | `im.message.conversation.redis.queue` | 12000 | 0.444 | 5.330 |
| 100 | 200 | `single_message_persist` | `im.message.persist.queue` | 12000 | 0.797 | 9.569 |
| 100 | 200 | `single_realtime_push` | `im.message.realtime.queue` | 12000 | 0.254 | 3.048 |
| 100 | 200 | `single_recent_message_cache_projection` | `im.message.recent.cache.queue` | 12000 | 0.240 | 2.883 |
| 200 | 400 | `single_conversation_persist` | `im.message.conversation.queue` | 24000 | 1.122 | 26.920 |
| 200 | 400 | `single_conversation_redis_projection` | `im.message.conversation.redis.queue` | 24000 | 0.430 | 10.319 |
| 200 | 400 | `single_message_persist` | `im.message.persist.queue` | 24000 | 0.761 | 18.265 |
| 200 | 400 | `single_realtime_push` | `im.message.realtime.queue` | 24000 | 0.253 | 6.065 |
| 200 | 400 | `single_recent_message_cache_projection` | `im.message.recent.cache.queue` | 24000 | 0.231 | 5.551 |
| 300 | 600 | `single_conversation_persist` | `im.message.conversation.queue` | 36000 | 1.236 | 44.507 |
| 300 | 600 | `single_conversation_redis_projection` | `im.message.conversation.redis.queue` | 36000 | 0.490 | 17.654 |
| 300 | 600 | `single_message_persist` | `im.message.persist.queue` | 36000 | 0.831 | 29.928 |
| 300 | 600 | `single_realtime_push` | `im.message.realtime.queue` | 36000 | 0.300 | 10.802 |
| 300 | 600 | `single_recent_message_cache_projection` | `im.message.recent.cache.queue` | 36000 | 0.262 | 9.445 |
| 400 | 800 | `single_conversation_persist` | `im.message.conversation.queue` | 48000 | 1.386 | 66.518 |
| 400 | 800 | `single_conversation_redis_projection` | `im.message.conversation.redis.queue` | 48000 | 0.591 | 28.367 |
| 400 | 800 | `single_message_persist` | `im.message.persist.queue` | 48000 | 0.937 | 44.955 |
| 400 | 800 | `single_realtime_push` | `im.message.realtime.queue` | 48000 | 0.375 | 18.004 |
| 400 | 800 | `single_recent_message_cache_projection` | `im.message.recent.cache.queue` | 48000 | 0.317 | 15.196 |
| 600 | 1200 | `single_conversation_persist` | `im.message.conversation.queue` | 72000 | 1.515 | 109.051 |
| 600 | 1200 | `single_conversation_redis_projection` | `im.message.conversation.redis.queue` | 72000 | 0.807 | 58.113 |
| 600 | 1200 | `single_message_persist` | `im.message.persist.queue` | 72000 | 1.014 | 72.988 |
| 600 | 1200 | `single_realtime_push` | `im.message.realtime.queue` | 72000 | 0.538 | 38.706 |
| 600 | 1200 | `single_recent_message_cache_projection` | `im.message.recent.cache.queue` | 72000 | 0.434 | 31.225 |
| 800 | 1600 | `single_conversation_persist` | `im.message.conversation.queue` | 87240 | 1.550 | 135.258 |
| 800 | 1600 | `single_conversation_redis_projection` | `im.message.conversation.redis.queue` | 87240 | 0.856 | 74.687 |
| 800 | 1600 | `single_message_persist` | `im.message.persist.queue` | 87240 | 0.985 | 85.939 |
| 800 | 1600 | `single_realtime_push` | `im.message.realtime.queue` | 87240 | 0.572 | 49.933 |
| 800 | 1600 | `single_recent_message_cache_projection` | `im.message.recent.cache.queue` | 87240 | 0.460 | 40.157 |

## MySQL Digest Top 语句

以下 Top 行来自每个挡位的 `mysql-digest.txt`。

### 用户对 100，速率 200 msg/s

| 次数 | 总耗时 s | 平均 ms | 最大 ms | Digest |
|---:|---:|---:|---:|---|
| 36199 | 4.3216 | 0.1194 | 6.5996 | `COMMIT` |
| 72381 | 3.7301 | 0.0515 | 0.7649 | `SET `autocommit` = ?` |
| 23997 | 2.5415 | 0.1059 | 0.4951 | `SELECT `id` , `username` , PASSWORD , `role_code` , STATUS , `create_time` , `update_time` , `delete_time` FROM `t_user` WHERE `id` = ? AND STATUS = ?` |
| 12104 | 2.4403 | 0.2016 | 3.8400 | `UPDATE `chat_conversation` SET `last_message` = CASE WHEN `last_server_message_id` IS NULL OR `last_server_message_id` < ? THEN ? ELSE `last_message` END , `last_message_time` = CA...` |
| 12000 | 2.3424 | 0.1952 | 5.6790 | `INSERT INTO `chat_message` ( `server_message_id` , `conversation_id` , `conversation_type` , `sender_id` , `receiver_id` , `client_message_id` , `sender_location` , `message_type` ...` |
| 36905 | 2.3154 | 0.0627 | 0.7145 | `SELECT @@SESSION . `transaction_read_only`` |
| 23999 | 2.1016 | 0.0876 | 1.0761 | `SELECT `user_id` AS `userId` , `target_user_id` AS `targetUserId` , `is_contact` AS `isContact` , `is_dm_contact` AS `isDmContact` , `is_blocked` AS `isBlocked` , `is_muted` AS `is...` |
| 12100 | 1.9239 | 0.1590 | 4.4133 | `UPDATE `chat_conversation` SET `unread_count` = COALESCE ( `unread_count` , ? ) + ? , `last_message` = CASE WHEN `last_server_message_id` IS NULL OR `last_server_message_id` < ? TH...` |

### 用户对 200，速率 400 msg/s

| 次数 | 总耗时 s | 平均 ms | 最大 ms | Digest |
|---:|---:|---:|---:|---|
| 72389 | 9.6146 | 0.1328 | 8.1271 | `COMMIT` |
| 144736 | 8.5373 | 0.0590 | 583.3909 | `SET `autocommit` = ?` |
| 73795 | 5.1849 | 0.0703 | 608.1804 | `SELECT @@SESSION . `transaction_read_only`` |
| 23993 | 5.1311 | 0.2139 | 608.3291 | `INSERT INTO `chat_message` ( `server_message_id` , `conversation_id` , `conversation_type` , `sender_id` , `receiver_id` , `client_message_id` , `sender_location` , `message_type` ...` |
| 47994 | 5.0771 | 0.1058 | 608.1913 | `SELECT `user_id` AS `userId` , `target_user_id` AS `targetUserId` , `is_contact` AS `isContact` , `is_dm_contact` AS `isDmContact` , `is_blocked` AS `isBlocked` , `is_muted` AS `is...` |
| 47988 | 4.9308 | 0.1028 | 0.7062 | `SELECT `id` , `username` , PASSWORD , `role_code` , STATUS , `create_time` , `update_time` , `delete_time` FROM `t_user` WHERE `id` = ? AND STATUS = ?` |
| 24197 | 4.7265 | 0.1953 | 7.1545 | `UPDATE `chat_conversation` SET `last_message` = CASE WHEN `last_server_message_id` IS NULL OR `last_server_message_id` < ? THEN ? ELSE `last_message` END , `last_message_time` = CA...` |
| 24199 | 3.9035 | 0.1613 | 6.5347 | `UPDATE `chat_conversation` SET `unread_count` = COALESCE ( `unread_count` , ? ) + ? , `last_message` = CASE WHEN `last_server_message_id` IS NULL OR `last_server_message_id` < ? TH...` |

### 用户对 300，速率 600 msg/s

| 次数 | 总耗时 s | 平均 ms | 最大 ms | Digest |
|---:|---:|---:|---:|---|
| 217101 | 12.1731 | 0.0561 | 2.9436 | `SET `autocommit` = ?` |
| 108588 | 10.6905 | 0.0984 | 461.6433 | `COMMIT` |
| 71987 | 8.3291 | 0.1157 | 461.6651 | `SELECT `user_id` AS `userId` , `target_user_id` AS `targetUserId` , `is_contact` AS `isContact` , `is_dm_contact` AS `isDmContact` , `is_blocked` AS `isBlocked` , `is_muted` AS `is...` |
| 71985 | 7.8968 | 0.1097 | 1.2677 | `SELECT `id` , `username` , PASSWORD , `role_code` , STATUS , `create_time` , `update_time` , `delete_time` FROM `t_user` WHERE `id` = ? AND STATUS = ?` |
| 36294 | 7.5044 | 0.2068 | 7.8246 | `UPDATE `chat_conversation` SET `last_message` = CASE WHEN `last_server_message_id` IS NULL OR `last_server_message_id` < ? THEN ? ELSE `last_message` END , `last_message_time` = CA...` |
| 110697 | 7.2730 | 0.0657 | 1.4589 | `SELECT @@SESSION . `transaction_read_only`` |
| 35981 | 7.0618 | 0.1963 | 12.0713 | `INSERT INTO `chat_message` ( `server_message_id` , `conversation_id` , `conversation_type` , `sender_id` , `receiver_id` , `client_message_id` , `sender_location` , `message_type` ...` |
| 36298 | 6.3626 | 0.1753 | 9.8858 | `UPDATE `chat_conversation` SET `unread_count` = COALESCE ( `unread_count` , ? ) + ? , `last_message` = CASE WHEN `last_server_message_id` IS NULL OR `last_server_message_id` < ? TH...` |

### 用户对 400，速率 800 msg/s

| 次数 | 总耗时 s | 平均 ms | 最大 ms | Digest |
|---:|---:|---:|---:|---|
| 289507 | 17.5907 | 0.0608 | 5.1275 | `SET `autocommit` = ?` |
| 144783 | 15.4968 | 0.1070 | 62.4879 | `COMMIT` |
| 95974 | 12.9580 | 0.1350 | 613.5829 | `SELECT `id` , `username` , PASSWORD , `role_code` , STATUS , `create_time` , `update_time` , `delete_time` FROM `t_user` WHERE `id` = ? AND STATUS = ?` |
| 95986 | 11.2144 | 0.1168 | 3.7619 | `SELECT `user_id` AS `userId` , `target_user_id` AS `targetUserId` , `is_contact` AS `isContact` , `is_dm_contact` AS `isDmContact` , `is_blocked` AS `isBlocked` , `is_muted` AS `is...` |
| 48391 | 10.9430 | 0.2261 | 11.9190 | `UPDATE `chat_conversation` SET `last_message` = CASE WHEN `last_server_message_id` IS NULL OR `last_server_message_id` < ? THEN ? ELSE `last_message` END , `last_message_time` = CA...` |
| 147582 | 10.3825 | 0.0704 | 2.0946 | `SELECT @@SESSION . `transaction_read_only`` |
| 48388 | 10.2814 | 0.2125 | 407.7730 | `UPDATE `chat_conversation` SET `unread_count` = COALESCE ( `unread_count` , ? ) + ? , `last_message` = CASE WHEN `last_server_message_id` IS NULL OR `last_server_message_id` < ? TH...` |
| 47990 | 9.9522 | 0.2074 | 11.8031 | `INSERT INTO `chat_message` ( `server_message_id` , `conversation_id` , `conversation_type` , `sender_id` , `receiver_id` , `client_message_id` , `sender_location` , `message_type` ...` |

### 用户对 600，速率 1200 msg/s

| 次数 | 总耗时 s | 平均 ms | 最大 ms | Digest |
|---:|---:|---:|---:|---|
| 434302 | 28.6730 | 0.0660 | 628.7835 | `SET `autocommit` = ?` |
| 217174 | 25.8905 | 0.1192 | 443.1955 | `COMMIT` |
| 143979 | 21.0831 | 0.1464 | 629.4053 | `SELECT `id` , `username` , PASSWORD , `role_code` , STATUS , `create_time` , `update_time` , `delete_time` FROM `t_user` WHERE `id` = ? AND STATUS = ?` |
| 143981 | 19.7748 | 0.1373 | 628.9483 | `SELECT `user_id` AS `userId` , `target_user_id` AS `targetUserId` , `is_contact` AS `isContact` , `is_dm_contact` AS `isDmContact` , `is_blocked` AS `isBlocked` , `is_muted` AS `is...` |
| 72594 | 17.4377 | 0.2402 | 11.7498 | `UPDATE `chat_conversation` SET `last_message` = CASE WHEN `last_server_message_id` IS NULL OR `last_server_message_id` < ? THEN ? ELSE `last_message` END , `last_message_time` = CA...` |
| 221371 | 16.9148 | 0.0764 | 599.8771 | `SELECT @@SESSION . `transaction_read_only`` |
| 71989 | 15.6382 | 0.2172 | 443.2892 | `INSERT INTO `chat_message` ( `server_message_id` , `conversation_id` , `conversation_type` , `sender_id` , `receiver_id` , `client_message_id` , `sender_location` , `message_type` ...` |
| 72588 | 15.3307 | 0.2112 | 14.6099 | `UPDATE `chat_conversation` SET `unread_count` = COALESCE ( `unread_count` , ? ) + ? , `last_message` = CASE WHEN `last_server_message_id` IS NULL OR `last_server_message_id` < ? TH...` |

### 用户对 800，速率 1600 msg/s

| 次数 | 总耗时 s | 平均 ms | 最大 ms | Digest |
|---:|---:|---:|---:|---|
| 526542 | 33.9774 | 0.0645 | 620.8776 | `SET `autocommit` = ?` |
| 263296 | 32.6245 | 0.1239 | 620.9339 | `COMMIT` |
| 174472 | 24.2420 | 0.1389 | 433.1757 | `SELECT `id` , `username` , PASSWORD , `role_code` , STATUS , `create_time` , `update_time` , `delete_time` FROM `t_user` WHERE `id` = ? AND STATUS = ?` |
| 174469 | 23.1978 | 0.1330 | 7.5126 | `SELECT `user_id` AS `userId` , `target_user_id` AS `targetUserId` , `is_contact` AS `isContact` , `is_dm_contact` AS `isDmContact` , `is_blocked` AS `isBlocked` , `is_muted` AS `is...` |
| 87959 | 21.5473 | 0.2450 | 609.9602 | `UPDATE `chat_conversation` SET `last_message` = CASE WHEN `last_server_message_id` IS NULL OR `last_server_message_id` < ? THEN ? ELSE `last_message` END , `last_message_time` = CA...` |
| 268526 | 20.3947 | 0.0760 | 458.4806 | `SELECT @@SESSION . `transaction_read_only`` |
| 87961 | 19.4638 | 0.2213 | 621.0605 | `UPDATE `chat_conversation` SET `unread_count` = COALESCE ( `unread_count` , ? ) + ? , `last_message` = CASE WHEN `last_server_message_id` IS NULL OR `last_server_message_id` < ? TH...` |
| 87232 | 18.3414 | 0.2103 | 610.1500 | `INSERT INTO `chat_message` ( `server_message_id` , `conversation_id` , `conversation_type` , `sender_id` , `receiver_id` , `client_message_id` , `sender_location` , `message_type` ...` |

## MySQL Waits Top 事件

以下 Top 行来自每个挡位的 `mysql-waits.txt`，已排除 `idle`。

### 用户对 100，速率 200 msg/s

| 事件 | 次数 | 总耗时 s | 平均 ms | 最大 ms |
|---|---:|---:|---:|---:|
| `wait/io/table/sql/handler` | 133266 | 2.0180 | 0.0151 | 5.5531 |
| `wait/io/file/innodb/innodb_data_file` | 6084 | 1.0862 | 0.1785 | 2.8567 |
| `wait/io/file/innodb/innodb_dblwr_file` | 1948 | 0.5820 | 0.2988 | 2.1811 |
| `wait/io/file/innodb/innodb_log_file` | 67065 | 0.5154 | 0.0077 | 3.5581 |
| `wait/io/file/sql/binlog` | 23980 | 0.1686 | 0.0070 | 0.1870 |
| `wait/lock/table/sql/handler` | 109308 | 0.0654 | 0.0006 | 0.1170 |
| `wait/io/file/csv/metadata` | 7 | 0.0013 | 0.1791 | 0.7757 |
| `wait/io/file/csv/data` | 6 | 0.0001 | 0.0193 | 0.0477 |

### 用户对 200，速率 400 msg/s

| 事件 | 次数 | 总耗时 s | 平均 ms | 最大 ms |
|---|---:|---:|---:|---:|
| `wait/io/table/sql/handler` | 266587 | 3.9384 | 0.0148 | 11.6320 |
| `wait/io/file/innodb/innodb_data_file` | 10484 | 2.3933 | 0.2283 | 14.5005 |
| `wait/io/file/innodb/innodb_dblwr_file` | 3494 | 1.5163 | 0.4340 | 6.0695 |
| `wait/io/file/innodb/innodb_log_file` | 120491 | 1.0460 | 0.0087 | 17.8248 |
| `wait/io/file/sql/binlog` | 46576 | 0.3168 | 0.0068 | 2.4702 |
| `wait/lock/table/sql/handler` | 218610 | 0.1219 | 0.0006 | 0.1849 |
| `wait/io/file/csv/data` | 4 | 0.0000 | 0.0118 | 0.0227 |

### 用户对 300，速率 600 msg/s

| 事件 | 次数 | 总耗时 s | 平均 ms | 最大 ms |
|---|---:|---:|---:|---:|
| `wait/io/table/sql/handler` | 399831 | 6.3289 | 0.0158 | 12.0579 |
| `wait/io/file/innodb/innodb_data_file` | 13188 | 3.8305 | 0.2905 | 12.8808 |
| `wait/io/file/innodb/innodb_dblwr_file` | 4418 | 2.4800 | 0.5613 | 7.1291 |
| `wait/io/file/innodb/innodb_log_file` | 850183 | 1.4973 | 0.0018 | 7.4903 |
| `wait/io/file/sql/binlog` | 71023 | 0.4844 | 0.0068 | 1.5406 |
| `wait/lock/table/sql/handler` | 327910 | 0.1894 | 0.0006 | 0.2968 |
| `wait/io/file/csv/data` | 4 | 0.0000 | 0.0114 | 0.0213 |

### 用户对 400，速率 800 msg/s

| 事件 | 次数 | 总耗时 s | 平均 ms | 最大 ms |
|---|---:|---:|---:|---:|
| `wait/io/table/sql/handler` | 533118 | 9.2878 | 0.0174 | 11.7000 |
| `wait/io/file/innodb/innodb_data_file` | 17829 | 5.0910 | 0.2855 | 5.8183 |
| `wait/io/file/innodb/innodb_dblwr_file` | 6040 | 3.2571 | 0.5393 | 6.4091 |
| `wait/io/file/innodb/innodb_log_file` | 1129053 | 1.9256 | 0.0017 | 24.4229 |
| `wait/io/file/sql/binlog` | 94102 | 0.6829 | 0.0073 | 0.7589 |
| `wait/lock/table/sql/handler` | 437214 | 0.2717 | 0.0006 | 0.8834 |
| `wait/io/file/csv/data` | 4 | 0.0002 | 0.0544 | 0.1577 |

### 用户对 600，速率 1200 msg/s

| 事件 | 次数 | 总耗时 s | 平均 ms | 最大 ms |
|---|---:|---:|---:|---:|
| `wait/io/table/sql/handler` | 799715 | 15.1640 | 0.0190 | 14.4419 |
| `wait/io/file/innodb/innodb_data_file` | 22940 | 8.7582 | 0.3818 | 9.4815 |
| `wait/io/file/innodb/innodb_dblwr_file` | 7794 | 6.2375 | 0.8003 | 14.8621 |
| `wait/io/file/innodb/innodb_log_file` | 1702343 | 3.0653 | 0.0018 | 17.1055 |
| `wait/io/file/sql/binlog` | 141531 | 1.0516 | 0.0074 | 2.6792 |
| `wait/lock/table/sql/handler` | 655812 | 0.4226 | 0.0006 | 1.5203 |
| `wait/io/file/csv/data` | 4 | 0.0001 | 0.0141 | 0.0296 |

### 用户对 800，速率 1600 msg/s

| 事件 | 次数 | 总耗时 s | 平均 ms | 最大 ms |
|---|---:|---:|---:|---:|
| `wait/io/table/sql/handler` | 969673 | 17.9993 | 0.0186 | 17.8294 |
| `wait/io/file/innodb/innodb_data_file` | 27650 | 10.6047 | 0.3835 | 9.7911 |
| `wait/io/file/innodb/innodb_dblwr_file` | 9202 | 7.2241 | 0.7851 | 12.8108 |
| `wait/io/file/innodb/innodb_log_file` | 2072980 | 3.8363 | 0.0019 | 14.5351 |
| `wait/io/file/sql/binlog` | 171720 | 1.2728 | 0.0074 | 0.8720 |
| `wait/lock/table/sql/handler` | 795205 | 0.5133 | 0.0006 | 0.8465 |
| `wait/io/file/csv/data` | 4 | 0.0002 | 0.0483 | 0.1385 |

## 原始文件索引

### 用户对 100，速率 200 msg/s

目录：`D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-100-rate-200`

- `summary.json` (56406 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-100-rate-200\summary.json`
- `k6.log` (17190 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-100-rate-200\k6.log`
- `k6.err.log` (0 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-100-rate-200\k6.err.log`
- `k6-exit-code.txt` (6 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-100-rate-200\k6-exit-code.txt`
- `queues-before.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-100-rate-200\queues-before.json`
- `queue-samples.jsonl` (27254 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-100-rate-200\queue-samples.jsonl`
- `queues-after-k6.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-100-rate-200\queues-after-k6.json`
- `queues-after-20s.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-100-rate-200\queues-after-20s.json`
- `metrics-before.prom` (75052 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-100-rate-200\metrics-before.prom`
- `metrics-after-k6.prom` (503030 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-100-rate-200\metrics-after-k6.prom`
- `metrics-after-20s.prom` (503007 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-100-rate-200\metrics-after-20s.prom`
- `mysql-digest.txt` (3644 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-100-rate-200\mysql-digest.txt`
- `mysql-slow-group.txt` (0 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-100-rate-200\mysql-slow-group.txt`
- `mysql-waits.txt` (544 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-100-rate-200\mysql-waits.txt`

### 用户对 200，速率 400 msg/s

目录：`D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-200-rate-400`

- `summary.json` (105375 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-200-rate-400\summary.json`
- `k6.log` (17166 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-200-rate-400\k6.log`
- `k6.err.log` (0 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-200-rate-400\k6.err.log`
- `k6-exit-code.txt` (6 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-200-rate-400\k6-exit-code.txt`
- `queues-before.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-200-rate-400\queues-before.json`
- `queue-samples.jsonl` (27254 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-200-rate-400\queue-samples.jsonl`
- `queues-after-k6.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-200-rate-400\queues-after-k6.json`
- `queues-after-20s.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-200-rate-400\queues-after-20s.json`
- `metrics-before.prom` (503007 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-200-rate-400\metrics-before.prom`
- `metrics-after-k6.prom` (503081 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-200-rate-400\metrics-after-k6.prom`
- `metrics-after-20s.prom` (503108 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-200-rate-400\metrics-after-20s.prom`
- `mysql-digest.txt` (3656 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-200-rate-400\mysql-digest.txt`
- `mysql-slow-group.txt` (0 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-200-rate-400\mysql-slow-group.txt`
- `mysql-waits.txt` (499 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-200-rate-400\mysql-waits.txt`

### 用户对 300，速率 600 msg/s

目录：`D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-300-rate-600`

- `summary.json` (154311 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-300-rate-600\summary.json`
- `k6.log` (17273 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-300-rate-600\k6.log`
- `k6.err.log` (0 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-300-rate-600\k6.err.log`
- `k6-exit-code.txt` (6 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-300-rate-600\k6-exit-code.txt`
- `queues-before.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-300-rate-600\queues-before.json`
- `queue-samples.jsonl` (27254 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-300-rate-600\queue-samples.jsonl`
- `queues-after-k6.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-300-rate-600\queues-after-k6.json`
- `queues-after-20s.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-300-rate-600\queues-after-20s.json`
- `metrics-before.prom` (503109 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-300-rate-600\metrics-before.prom`
- `metrics-after-k6.prom` (503948 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-300-rate-600\metrics-after-k6.prom`
- `metrics-after-20s.prom` (503953 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-300-rate-600\metrics-after-20s.prom`
- `mysql-digest.txt` (3656 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-300-rate-600\mysql-digest.txt`
- `mysql-slow-group.txt` (0 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-300-rate-600\mysql-slow-group.txt`
- `mysql-waits.txt` (498 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-300-rate-600\mysql-waits.txt`

### 用户对 400，速率 800 msg/s

目录：`D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-400-rate-800`

- `summary.json` (203149 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-400-rate-800\summary.json`
- `k6.log` (17534 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-400-rate-800\k6.log`
- `k6.err.log` (0 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-400-rate-800\k6.err.log`
- `k6-exit-code.txt` (6 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-400-rate-800\k6-exit-code.txt`
- `queues-before.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-400-rate-800\queues-before.json`
- `queue-samples.jsonl` (27260 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-400-rate-800\queue-samples.jsonl`
- `queues-after-k6.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-400-rate-800\queues-after-k6.json`
- `queues-after-20s.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-400-rate-800\queues-after-20s.json`
- `metrics-before.prom` (503953 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-400-rate-800\metrics-before.prom`
- `metrics-after-k6.prom` (506841 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-400-rate-800\metrics-after-k6.prom`
- `metrics-after-20s.prom` (506843 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-400-rate-800\metrics-after-20s.prom`
- `mysql-digest.txt` (3663 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-400-rate-800\mysql-digest.txt`
- `mysql-slow-group.txt` (0 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-400-rate-800\mysql-slow-group.txt`
- `mysql-waits.txt` (499 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-400-rate-800\mysql-waits.txt`

### 用户对 600，速率 1200 msg/s

目录：`D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-600-rate-1200`

- `summary.json` (301002 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-600-rate-1200\summary.json`
- `k6.log` (18178 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-600-rate-1200\k6.log`
- `k6.err.log` (0 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-600-rate-1200\k6.err.log`
- `k6-exit-code.txt` (6 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-600-rate-1200\k6-exit-code.txt`
- `queues-before.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-600-rate-1200\queues-before.json`
- `queue-samples.jsonl` (25720 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-600-rate-1200\queue-samples.jsonl`
- `queues-after-k6.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-600-rate-1200\queues-after-k6.json`
- `queues-after-20s.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-600-rate-1200\queues-after-20s.json`
- `metrics-before.prom` (506839 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-600-rate-1200\metrics-before.prom`
- `metrics-after-k6.prom` (506884 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-600-rate-1200\metrics-after-k6.prom`
- `metrics-after-20s.prom` (506888 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-600-rate-1200\metrics-after-20s.prom`
- `mysql-digest.txt` (3678 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-600-rate-1200\mysql-digest.txt`
- `mysql-slow-group.txt` (0 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-600-rate-1200\mysql-slow-group.txt`
- `mysql-waits.txt` (503 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-600-rate-1200\mysql-waits.txt`

### 用户对 800，速率 1600 msg/s

目录：`D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-800-rate-1600`

- `summary.json` (398839 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-800-rate-1600\summary.json`
- `k6.log` (18385 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-800-rate-1600\k6.log`
- `k6.err.log` (117 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-800-rate-1600\k6.err.log`
- `k6-exit-code.txt` (6 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-800-rate-1600\k6-exit-code.txt`
- `queues-before.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-800-rate-1600\queues-before.json`
- `queue-samples.jsonl` (25817 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-800-rate-1600\queue-samples.jsonl`
- `queues-after-k6.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-800-rate-1600\queues-after-k6.json`
- `queues-after-20s.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-800-rate-1600\queues-after-20s.json`
- `metrics-before.prom` (506891 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-800-rate-1600\metrics-before.prom`
- `metrics-after-k6.prom` (506891 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-800-rate-1600\metrics-after-k6.prom`
- `metrics-after-20s.prom` (506895 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-800-rate-1600\metrics-after-20s.prom`
- `mysql-digest.txt` (3677 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-800-rate-1600\mysql-digest.txt`
- `mysql-slow-group.txt` (0 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-800-rate-1600\mysql-slow-group.txt`
- `mysql-waits.txt` (504 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260421-131020\pairs-800-rate-1600\mysql-waits.txt`

