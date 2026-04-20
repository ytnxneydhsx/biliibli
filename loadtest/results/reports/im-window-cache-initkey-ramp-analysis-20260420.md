# IM 在线用户对阶梯压测记录

生成时间：2026-04-20 Asia/Shanghai

## 数据范围

本文档汇总本次阶梯压测每个挡位的结果数据。每个挡位包含 k6 汇总、RabbitMQ 队列快照和采样、应用侧 Prometheus 指标，以及 MySQL Performance Schema / slow log 导出的统计。

原始结果目录：
- `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948`

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
| 100 | 200 | 12000 | 11965 | 11965 | 11965 | 0 | 4.1 | 5.0 | 877.0 | 0 |
| 200 | 400 | 24000 | 23905 | 23905 | 23905 | 0 | 4.0 | 5.0 | 2,254.0 | 0 |
| 300 | 600 | 36000 | 35815 | 35815 | 35815 | 0 | 4.9 | 6.0 | 2,211.0 | 0 |
| 400 | 800 | 48000 | 48000 | 48000 | 48000 | 0 | 5.1 | 6.0 | 1,296.0 | 0 |
| 600 | 1200 | 72000 | 72000 | 72000 | 72000 | 0 | 6.9 | 11.0 | 1,257.0 | 0 |
| 800 | 1600 | 95880 | 95880 | 95880 | 95880 | 1 | 11.2 | 27.0 | 1,269.0 | 0 |

## RabbitMQ 队列积压汇总

`最大积压` 是 `queue-samples.jsonl` 里该队列 `messages` 的最大值。`>=100 采样次数` 表示采样时该队列积压至少 100 条的次数。`Consumer 最小/最大/最后` 来自压测期间 RabbitMQ 队列采样，用于观察 Spring AMQP 动态扩容情况。`k6 结束后` 和 `20s 后` 来自对应的队列快照文件。

| 用户对 | 速率 | 队列 | Consumer 最小 | Consumer 最大 | Consumer 最后 | 最大积压 | >=100 采样次数 | k6 结束后 | 20s 后 |
|---:|---:|---|---:|---:|---:|---:|---:|---:|---:|
| 100 | 200 | `im.message.persist.queue` | 2 | 4 | 3 | 0 | 0 | 0 | 0 |
| 100 | 200 | `im.message.conversation.queue` | 2 | 6 | 5 | 94 | 0 | 0 | 0 |
| 100 | 200 | `im.message.conversation.redis.queue` | 4 | 8 | 7 | 0 | 0 | 0 | 0 |
| 100 | 200 | `im.message.recent.cache.queue` | 4 | 8 | 7 | 0 | 0 | 0 | 0 |
| 100 | 200 | `im.message.realtime.queue` | 2 | 4 | 3 | 2 | 0 | 0 | 0 |
| 200 | 400 | `im.message.persist.queue` | 3 | 4 | 3 | 1 | 0 | 0 | 0 |
| 200 | 400 | `im.message.conversation.queue` | 5 | 6 | 5 | 103 | 6 | 0 | 0 |
| 200 | 400 | `im.message.conversation.redis.queue` | 7 | 8 | 7 | 2 | 0 | 0 | 0 |
| 200 | 400 | `im.message.recent.cache.queue` | 7 | 8 | 7 | 1 | 0 | 0 | 0 |
| 200 | 400 | `im.message.realtime.queue` | 3 | 4 | 3 | 4 | 0 | 0 | 0 |
| 300 | 600 | `im.message.persist.queue` | 3 | 4 | 3 | 6 | 0 | 0 | 0 |
| 300 | 600 | `im.message.conversation.queue` | 5 | 6 | 5 | 238 | 9 | 0 | 0 |
| 300 | 600 | `im.message.conversation.redis.queue` | 7 | 8 | 7 | 6 | 0 | 0 | 0 |
| 300 | 600 | `im.message.recent.cache.queue` | 7 | 8 | 7 | 3 | 0 | 0 | 0 |
| 300 | 600 | `im.message.realtime.queue` | 3 | 4 | 3 | 2 | 0 | 0 | 0 |
| 400 | 800 | `im.message.persist.queue` | 3 | 4 | 3 | 3 | 0 | 0 | 0 |
| 400 | 800 | `im.message.conversation.queue` | 5 | 6 | 5 | 752 | 11 | 0 | 0 |
| 400 | 800 | `im.message.conversation.redis.queue` | 7 | 8 | 7 | 4 | 0 | 0 | 0 |
| 400 | 800 | `im.message.recent.cache.queue` | 7 | 8 | 7 | 4 | 0 | 0 | 0 |
| 400 | 800 | `im.message.realtime.queue` | 3 | 4 | 3 | 5 | 0 | 0 | 0 |
| 600 | 1200 | `im.message.persist.queue` | 3 | 4 | 3 | 160 | 1 | 0 | 0 |
| 600 | 1200 | `im.message.conversation.queue` | 5 | 6 | 6 | 37337 | 13 | 6901 | 0 |
| 600 | 1200 | `im.message.conversation.redis.queue` | 7 | 8 | 7 | 4 | 0 | 0 | 0 |
| 600 | 1200 | `im.message.recent.cache.queue` | 7 | 8 | 7 | 6 | 0 | 0 | 0 |
| 600 | 1200 | `im.message.realtime.queue` | 3 | 4 | 3 | 11 | 0 | 0 | 0 |
| 800 | 1600 | `im.message.persist.queue` | 3 | 4 | 3 | 8046 | 10 | 0 | 0 |
| 800 | 1600 | `im.message.conversation.queue` | 5 | 6 | 6 | 72562 | 13 | 43867 | 17880 |
| 800 | 1600 | `im.message.conversation.redis.queue` | 7 | 8 | 7 | 8 | 0 | 0 | 0 |
| 800 | 1600 | `im.message.recent.cache.queue` | 7 | 8 | 7 | 8 | 0 | 0 | 0 |
| 800 | 1600 | `im.message.realtime.queue` | 3 | 4 | 3 | 9 | 0 | 0 | 0 |

## 应用侧 DB 操作指标

以下数据来自 `im_db_operation_duration_seconds_*`，按每个挡位的前后差值计算。

| 用户对 | 速率 | 操作 | 次数 | 平均 ms | 总耗时 s |
|---:|---:|---|---:|---:|---:|
| 100 | 200 | `chat_message_insert` | 11965 | 0.486 | 5.816 |
| 100 | 200 | `contact_relation_upsert` | 100 | 0.434 | 0.043 |
| 200 | 400 | `chat_message_insert` | 23905 | 0.449 | 10.731 |
| 200 | 400 | `contact_relation_upsert` | 200 | 0.352 | 0.070 |
| 300 | 600 | `chat_message_insert` | 35815 | 0.470 | 16.819 |
| 300 | 600 | `contact_relation_upsert` | 300 | 0.334 | 0.100 |
| 400 | 800 | `chat_message_insert` | 48000 | 0.482 | 23.158 |
| 400 | 800 | `contact_relation_upsert` | 400 | 0.471 | 0.189 |
| 600 | 1200 | `chat_message_insert` | 72000 | 0.522 | 37.605 |
| 600 | 1200 | `contact_relation_upsert` | 600 | 0.442 | 0.265 |
| 800 | 1600 | `chat_message_insert` | 95880 | 0.507 | 48.618 |
| 800 | 1600 | `contact_relation_upsert` | 799 | 0.439 | 0.351 |

## 应用侧 MQ Consumer 指标

以下数据来自 `im_mq_consumer_duration_seconds_*`，按每个挡位的前后差值计算。

| 用户对 | 速率 | Consumer | 队列 | 次数 | 平均 ms | 总耗时 s |
|---:|---:|---|---|---:|---:|---:|
| 100 | 200 | `single_conversation_persist` | `im.message.conversation.queue` | 11965 | 1.085 | 12.979 |
| 100 | 200 | `single_conversation_redis_projection` | `im.message.conversation.redis.queue` | 11965 | 0.416 | 4.983 |
| 100 | 200 | `single_message_persist` | `im.message.persist.queue` | 11965 | 0.752 | 9.003 |
| 100 | 200 | `single_realtime_push` | `im.message.realtime.queue` | 11965 | 0.233 | 2.785 |
| 100 | 200 | `single_recent_message_cache_projection` | `im.message.recent.cache.queue` | 11965 | 0.226 | 2.709 |
| 200 | 400 | `single_conversation_persist` | `im.message.conversation.queue` | 23905 | 1.072 | 25.638 |
| 200 | 400 | `single_conversation_redis_projection` | `im.message.conversation.redis.queue` | 23905 | 0.406 | 9.707 |
| 200 | 400 | `single_message_persist` | `im.message.persist.queue` | 23905 | 0.719 | 17.197 |
| 200 | 400 | `single_realtime_push` | `im.message.realtime.queue` | 23905 | 0.240 | 5.746 |
| 200 | 400 | `single_recent_message_cache_projection` | `im.message.recent.cache.queue` | 23905 | 0.221 | 5.288 |
| 300 | 600 | `single_conversation_persist` | `im.message.conversation.queue` | 35815 | 1.179 | 42.242 |
| 300 | 600 | `single_conversation_redis_projection` | `im.message.conversation.redis.queue` | 35815 | 0.455 | 16.296 |
| 300 | 600 | `single_message_persist` | `im.message.persist.queue` | 35815 | 0.796 | 28.520 |
| 300 | 600 | `single_realtime_push` | `im.message.realtime.queue` | 35815 | 0.282 | 10.103 |
| 300 | 600 | `single_recent_message_cache_projection` | `im.message.recent.cache.queue` | 35815 | 0.247 | 8.845 |
| 400 | 800 | `single_conversation_persist` | `im.message.conversation.queue` | 47996 | 1.317 | 63.205 |
| 400 | 800 | `single_conversation_redis_projection` | `im.message.conversation.redis.queue` | 48000 | 0.496 | 23.831 |
| 400 | 800 | `single_message_persist` | `im.message.persist.queue` | 48000 | 0.840 | 40.335 |
| 400 | 800 | `single_realtime_push` | `im.message.realtime.queue` | 48000 | 0.313 | 15.029 |
| 400 | 800 | `single_recent_message_cache_projection` | `im.message.recent.cache.queue` | 48000 | 0.270 | 12.950 |
| 600 | 1200 | `single_conversation_persist` | `im.message.conversation.queue` | 72000 | 1.171 | 84.292 |
| 600 | 1200 | `single_conversation_redis_projection` | `im.message.conversation.redis.queue` | 72000 | 0.668 | 48.102 |
| 600 | 1200 | `single_message_persist` | `im.message.persist.queue` | 72000 | 0.980 | 70.535 |
| 600 | 1200 | `single_realtime_push` | `im.message.realtime.queue` | 72000 | 0.448 | 32.265 |
| 600 | 1200 | `single_recent_message_cache_projection` | `im.message.recent.cache.queue` | 72000 | 0.358 | 25.788 |
| 800 | 1600 | `single_conversation_persist` | `im.message.conversation.queue` | 78057 | 1.076 | 84.003 |
| 800 | 1600 | `single_conversation_redis_projection` | `im.message.conversation.redis.queue` | 95880 | 0.781 | 74.861 |
| 800 | 1600 | `single_message_persist` | `im.message.persist.queue` | 95880 | 0.992 | 95.080 |
| 800 | 1600 | `single_realtime_push` | `im.message.realtime.queue` | 95880 | 0.566 | 54.235 |
| 800 | 1600 | `single_recent_message_cache_projection` | `im.message.recent.cache.queue` | 95880 | 0.423 | 40.528 |

## MySQL Digest Top 语句

以下 Top 行来自每个挡位的 `mysql-digest.txt`。

### 用户对 100，速率 200 msg/s

| 次数 | 总耗时 s | 平均 ms | 最大 ms | Digest |
|---:|---:|---:|---:|---|
| 36092 | 5.2397 | 0.1452 | 872.8574 | `COMMIT` |
| 72598 | 3.6825 | 0.0507 | 0.9087 | `SET `autocommit` = ?` |
| 36893 | 3.0521 | 0.0827 | 872.7907 | `SELECT @@SESSION . `transaction_read_only`` |
| 12164 | 2.7223 | 0.2238 | 7.1066 | `INSERT INTO `chat_conversation` ( `conversation_id` , `owner_user_id` , `target_id` , TYPE , `last_message` , `last_message_time` , `last_server_message_id` , `unread_count` , `is_...` |
| 23988 | 2.5335 | 0.1056 | 0.8175 | `SELECT `id` , `username` , PASSWORD , `role_code` , STATUS , `create_time` , `update_time` , `delete_time` FROM `t_user` WHERE `id` = ? AND STATUS = ?` |
| 11964 | 2.3758 | 0.1986 | 8.5760 | `INSERT INTO `chat_message` ( `server_message_id` , `conversation_id` , `conversation_type` , `sender_id` , `receiver_id` , `client_message_id` , `sender_location` , `message_type` ...` |
| 23998 | 2.0601 | 0.0858 | 0.5876 | `SELECT `user_id` AS `userId` , `target_user_id` AS `targetUserId` , `is_contact` AS `isContact` , `is_dm_contact` AS `isDmContact` , `is_blocked` AS `isBlocked` , `is_muted` AS `is...` |
| 12165 | 2.0464 | 0.1682 | 3.7808 | `INSERT INTO `chat_conversation` ( `conversation_id` , `owner_user_id` , `target_id` , TYPE , `last_message` , `last_message_time` , `last_server_message_id` , `unread_count` , `is_...` |

### 用户对 200，速率 400 msg/s

| 次数 | 总耗时 s | 平均 ms | 最大 ms | Digest |
|---:|---:|---:|---:|---|
| 72111 | 9.0797 | 0.1259 | 49.6092 | `COMMIT` |
| 144910 | 7.0127 | 0.0484 | 0.5543 | `SET `autocommit` = ?` |
| 24179 | 5.0311 | 0.2081 | 18446743770.2758 | `INSERT INTO `chat_conversation` ( `conversation_id` , `owner_user_id` , `target_id` , TYPE , `last_message` , `last_message_time` , `last_server_message_id` , `unread_count` , `is_...` |
| 47988 | 4.6861 | 0.0977 | 0.8612 | `SELECT `id` , `username` , PASSWORD , `role_code` , STATUS , `create_time` , `update_time` , `delete_time` FROM `t_user` WHERE `id` = ? AND STATUS = ?` |
| 47987 | 4.2347 | 0.0882 | 0.6730 | `SELECT `user_id` AS `userId` , `target_user_id` AS `targetUserId` , `is_contact` AS `isContact` , `is_dm_contact` AS `isDmContact` , `is_blocked` AS `isBlocked` , `is_muted` AS `is...` |
| 73460 | 4.1729 | 0.0568 | 0.4290 | `SELECT @@SESSION . `transaction_read_only`` |
| 24179 | 4.1339 | 0.1710 | 4.3208 | `INSERT INTO `chat_conversation` ( `conversation_id` , `owner_user_id` , `target_id` , TYPE , `last_message` , `last_message_time` , `last_server_message_id` , `unread_count` , `is_...` |
| 23898 | 3.9710 | 0.1662 | 18446743770.2424 | `INSERT INTO `chat_message` ( `server_message_id` , `conversation_id` , `conversation_type` , `sender_id` , `receiver_id` , `client_message_id` , `sender_location` , `message_type` ...` |

### 用户对 300，速率 600 msg/s

| 次数 | 总耗时 s | 平均 ms | 最大 ms | Digest |
|---:|---:|---:|---:|---|
| 216953 | 11.5674 | 0.0533 | 1.4627 | `SET `autocommit` = ?` |
| 108038 | 9.3852 | 0.0869 | 42.6658 | `COMMIT` |
| 36078 | 8.0297 | 0.2226 | 18446743744.0240 | `INSERT INTO `chat_conversation` ( `conversation_id` , `owner_user_id` , `target_id` , TYPE , `last_message` , `last_message_time` , `last_server_message_id` , `unread_count` , `is_...` |
| 71991 | 7.7024 | 0.1070 | 1.3437 | `SELECT `id` , `username` , PASSWORD , `role_code` , STATUS , `create_time` , `update_time` , `delete_time` FROM `t_user` WHERE `id` = ? AND STATUS = ?` |
| 71991 | 7.3342 | 0.1019 | 2.7353 | `SELECT `user_id` AS `userId` , `target_user_id` AS `targetUserId` , `is_contact` AS `isContact` , `is_dm_contact` AS `isDmContact` , `is_blocked` AS `isBlocked` , `is_muted` AS `is...` |
| 35810 | 7.2705 | 0.2030 | 772.6561 | `INSERT INTO `chat_message` ( `server_message_id` , `conversation_id` , `conversation_type` , `sender_id` , `receiver_id` , `client_message_id` , `sender_location` , `message_type` ...` |
| 109764 | 6.7100 | 0.0611 | 1.1011 | `SELECT @@SESSION . `transaction_read_only`` |
| 36078 | 6.6135 | 0.1833 | 9.4619 | `INSERT INTO `chat_conversation` ( `conversation_id` , `owner_user_id` , `target_id` , TYPE , `last_message` , `last_message_time` , `last_server_message_id` , `unread_count` , `is_...` |

### 用户对 400，速率 800 msg/s

| 次数 | 总耗时 s | 平均 ms | 最大 ms | Digest |
|---:|---:|---:|---:|---|
| 290006 | 19.6078 | 0.0676 | 1288.8436 | `SET `autocommit` = ?` |
| 144781 | 16.6387 | 0.1149 | 1264.7708 | `COMMIT` |
| 48218 | 16.3362 | 0.3388 | 1264.8251 | `INSERT INTO `chat_conversation` ( `conversation_id` , `owner_user_id` , `target_id` , TYPE , `last_message` , `last_message_time` , `last_server_message_id` , `unread_count` , `is_...` |
| 95987 | 10.7573 | 0.1121 | 3.5475 | `SELECT `id` , `username` , PASSWORD , `role_code` , STATUS , `create_time` , `update_time` , `delete_time` FROM `t_user` WHERE `id` = ? AND STATUS = ?` |
| 95986 | 10.3820 | 0.1082 | 1.3731 | `SELECT `user_id` AS `userId` , `target_user_id` AS `targetUserId` , `is_contact` AS `isContact` , `is_dm_contact` AS `isDmContact` , `is_blocked` AS `isBlocked` , `is_muted` AS `is...` |
| 47987 | 10.1748 | 0.2120 | 1289.0436 | `INSERT INTO `chat_message` ( `server_message_id` , `conversation_id` , `conversation_type` , `sender_id` , `receiver_id` , `client_message_id` , `sender_location` , `message_type` ...` |
| 48220 | 9.8183 | 0.2036 | 9.1157 | `INSERT INTO `chat_conversation` ( `conversation_id` , `owner_user_id` , `target_id` , TYPE , `last_message` , `last_message_time` , `last_server_message_id` , `unread_count` , `is_...` |
| 146835 | 9.4133 | 0.0641 | 1.2351 | `SELECT @@SESSION . `transaction_read_only`` |

### 用户对 600，速率 1200 msg/s

| 次数 | 总耗时 s | 平均 ms | 最大 ms | Digest |
|---:|---:|---:|---:|---|
| 435012 | 26.5340 | 0.0610 | 1251.6811 | `SET `autocommit` = ?` |
| 217179 | 21.9960 | 0.1013 | 114.4115 | `COMMIT` |
| 72333 | 21.1436 | 0.2923 | 1251.8970 | `INSERT INTO `chat_conversation` ( `conversation_id` , `owner_user_id` , `target_id` , TYPE , `last_message` , `last_message_time` , `last_server_message_id` , `unread_count` , `is_...` |
| 143978 | 18.4353 | 0.1280 | 852.9181 | `SELECT `id` , `username` , PASSWORD , `role_code` , STATUS , `create_time` , `update_time` , `delete_time` FROM `t_user` WHERE `id` = ? AND STATUS = ?` |
| 143980 | 17.9264 | 0.1245 | 852.9119 | `SELECT `user_id` AS `userId` , `target_user_id` AS `targetUserId` , `is_contact` AS `isContact` , `is_dm_contact` AS `isDmContact` , `is_blocked` AS `isBlocked` , `is_muted` AS `is...` |
| 71984 | 16.0056 | 0.2223 | 1245.6162 | `INSERT INTO `chat_message` ( `server_message_id` , `conversation_id` , `conversation_type` , `sender_id` , `receiver_id` , `client_message_id` , `sender_location` , `message_type` ...` |
| 220256 | 13.9038 | 0.0631 | 774.5848 | `SELECT @@SESSION . `transaction_read_only`` |
| 72333 | 13.2059 | 0.1826 | 10.2588 | `INSERT INTO `chat_conversation` ( `conversation_id` , `owner_user_id` , `target_id` , TYPE , `last_message` , `last_message_time` , `last_server_message_id` , `unread_count` , `is_...` |

### 用户对 800，速率 1600 msg/s

| 次数 | 总耗时 s | 平均 ms | 最大 ms | Digest |
|---:|---:|---:|---:|---|
| 274173 | 34.0988 | 0.1244 | 1262.8771 | `COMMIT` |
| 549171 | 32.3186 | 0.0588 | 818.0047 | `SET `autocommit` = ?` |
| 191738 | 26.6601 | 0.1390 | 1262.9644 | `SELECT `id` , `username` , PASSWORD , `role_code` , STATUS , `create_time` , `update_time` , `delete_time` FROM `t_user` WHERE `id` = ? AND STATUS = ?` |
| 191739 | 23.9098 | 0.1247 | 3.3448 | `SELECT `user_id` AS `userId` , `target_user_id` AS `targetUserId` , `is_contact` AS `isContact` , `is_dm_contact` AS `isDmContact` , `is_blocked` AS `isBlocked` , `is_muted` AS `is...` |
| 95870 | 19.8951 | 0.2075 | 778.3321 | `INSERT INTO `chat_message` ( `server_message_id` , `conversation_id` , `conversation_type` , `sender_id` , `receiver_id` , `client_message_id` , `sender_location` , `message_type` ...` |
| 81254 | 19.7612 | 0.2432 | 819.1139 | `INSERT INTO `chat_conversation` ( `conversation_id` , `owner_user_id` , `target_id` , TYPE , `last_message` , `last_message_time` , `last_server_message_id` , `unread_count` , `is_...` |
| 95877 | 15.1773 | 0.1583 | 1262.9846 | `SELECT `id` , `conversation_id` , `owner_user_id` , `target_id` , TYPE , `last_message` , `last_message_time` , `last_server_message_id` , `unread_count` , `is_muted` , `create_tim...` |
| 81254 | 14.9449 | 0.1839 | 1227.1222 | `INSERT INTO `chat_conversation` ( `conversation_id` , `owner_user_id` , `target_id` , TYPE , `last_message` , `last_message_time` , `last_server_message_id` , `unread_count` , `is_...` |

## MySQL Waits Top 事件

以下 Top 行来自每个挡位的 `mysql-waits.txt`，已排除 `idle`。

### 用户对 100，速率 200 msg/s

| 事件 | 次数 | 总耗时 s | 平均 ms | 最大 ms |
|---|---:|---:|---:|---:|
| `wait/io/table/sql/handler` | 167630 | 3.6744 | 0.0219 | 9.2527 |
| `wait/io/file/innodb/innodb_data_file` | 11500 | 2.2050 | 0.1917 | 8.9645 |
| `wait/io/file/innodb/innodb_dblwr_file` | 3924 | 1.2339 | 0.3145 | 6.6230 |
| `wait/io/file/innodb/innodb_log_file` | 70651 | 0.5479 | 0.0078 | 2.7654 |
| `wait/io/file/sql/binlog` | 24038 | 0.1891 | 0.0079 | 0.2413 |
| `wait/lock/table/sql/handler` | 121295 | 0.0702 | 0.0006 | 0.0944 |
| `wait/io/file/csv/metadata` | 7 | 0.0006 | 0.0907 | 0.3712 |
| `wait/io/file/csv/data` | 6 | 0.0001 | 0.0105 | 0.0230 |

### 用户对 200，速率 400 msg/s

| 事件 | 次数 | 总耗时 s | 平均 ms | 最大 ms |
|---|---:|---:|---:|---:|
| `wait/io/table/sql/handler` | 335689 | 6.9458 | 0.0207 | 8.4658 |
| `wait/io/file/innodb/innodb_data_file` | 12812 | 2.8884 | 0.2254 | 6.4353 |
| `wait/io/file/innodb/innodb_dblwr_file` | 4370 | 1.7443 | 0.3992 | 8.7022 |
| `wait/io/file/innodb/innodb_log_file` | 132324 | 1.0912 | 0.0082 | 2.5894 |
| `wait/io/file/sql/binlog` | 47483 | 0.3275 | 0.0069 | 0.3547 |
| `wait/lock/table/sql/handler` | 242263 | 0.1248 | 0.0005 | 0.1681 |
| `wait/io/file/csv/data` | 4 | 0.0001 | 0.0170 | 0.0388 |

### 用户对 300，速率 600 msg/s

| 事件 | 次数 | 总耗时 s | 平均 ms | 最大 ms |
|---|---:|---:|---:|---:|
| `wait/io/table/sql/handler` | 504105 | 10.6702 | 0.0212 | 14.8961 |
| `wait/io/file/innodb/innodb_data_file` | 15062 | 4.1712 | 0.2769 | 10.8038 |
| `wait/io/file/innodb/innodb_dblwr_file` | 5124 | 2.6172 | 0.5108 | 7.5486 |
| `wait/io/file/innodb/innodb_log_file` | 1198240 | 1.5261 | 0.0013 | 8.0147 |
| `wait/io/file/sql/binlog` | 71666 | 0.5092 | 0.0071 | 1.6928 |
| `wait/lock/table/sql/handler` | 362971 | 0.1998 | 0.0006 | 0.2993 |
| `wait/io/file/csv/data` | 482 | 0.0036 | 0.0075 | 0.2259 |

### 用户对 400，速率 800 msg/s

| 事件 | 次数 | 总耗时 s | 平均 ms | 最大 ms |
|---|---:|---:|---:|---:|
| `wait/io/table/sql/handler` | 673937 | 17.2813 | 0.0256 | 112.6644 |
| `wait/io/file/innodb/innodb_data_file` | 19728 | 6.0479 | 0.3066 | 7.9998 |
| `wait/io/file/innodb/innodb_dblwr_file` | 6514 | 3.9117 | 0.6005 | 7.2363 |
| `wait/io/file/innodb/innodb_log_file` | 1623469 | 2.1689 | 0.0013 | 12.0137 |
| `wait/io/file/sql/binlog` | 95750 | 0.6906 | 0.0072 | 0.6975 |
| `wait/lock/table/sql/handler` | 484440 | 0.2782 | 0.0006 | 0.3919 |
| `wait/io/file/csv/data` | 1010 | 0.0070 | 0.0069 | 0.1735 |

### 用户对 600，速率 1200 msg/s

| 事件 | 次数 | 总耗时 s | 平均 ms | 最大 ms |
|---|---:|---:|---:|---:|
| `wait/io/table/sql/handler` | 1011992 | 25.5735 | 0.0253 | 80.5158 |
| `wait/io/file/innodb/innodb_data_file` | 30056 | 9.8027 | 0.3261 | 27.0963 |
| `wait/io/file/innodb/innodb_dblwr_file` | 10316 | 6.5953 | 0.6393 | 26.9220 |
| `wait/io/file/innodb/innodb_log_file` | 2491037 | 3.6695 | 0.0015 | 12.9939 |
| `wait/io/file/sql/binlog` | 143462 | 1.2191 | 0.0085 | 11.0682 |
| `wait/lock/table/sql/handler` | 726666 | 0.4327 | 0.0006 | 0.7012 |
| `wait/io/file/csv/data` | 4006 | 0.0363 | 0.0091 | 3.6326 |
| `wait/io/file/sql/binlog_index` | 18 | 0.0016 | 0.0901 | 0.4399 |

### 用户对 800，速率 1600 msg/s

| 事件 | 次数 | 总耗时 s | 平均 ms | 最大 ms |
|---|---:|---:|---:|---:|
| `wait/io/table/sql/handler` | 1260288 | 29.6457 | 0.0235 | 85.1371 |
| `wait/io/file/innodb/innodb_data_file` | 35345 | 11.2926 | 0.3195 | 13.1415 |
| `wait/io/file/innodb/innodb_dblwr_file` | 11988 | 7.5072 | 0.6262 | 8.8919 |
| `wait/io/file/innodb/innodb_log_file` | 2194897 | 3.9258 | 0.0018 | 86.1561 |
| `wait/io/file/sql/binlog` | 177075 | 1.4372 | 0.0081 | 16.4460 |
| `wait/lock/table/sql/handler` | 938165 | 0.5582 | 0.0006 | 0.7360 |
| `wait/io/file/csv/data` | 7506 | 0.0631 | 0.0084 | 2.3309 |

## 原始文件索引

### 用户对 100，速率 200 msg/s

目录：`D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-100-rate-200`

- `summary.json` (56782 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-100-rate-200\summary.json`
- `k6.log` (18376 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-100-rate-200\k6.log`
- `k6.err.log` (0 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-100-rate-200\k6.err.log`
- `k6-exit-code.txt` (6 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-100-rate-200\k6-exit-code.txt`
- `queues-before.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-100-rate-200\queues-before.json`
- `queue-samples.jsonl` (30482 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-100-rate-200\queue-samples.jsonl`
- `queues-after-k6.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-100-rate-200\queues-after-k6.json`
- `queues-after-20s.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-100-rate-200\queues-after-20s.json`
- `metrics-before.prom` (528599 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-100-rate-200\metrics-before.prom`
- `metrics-after-k6.prom` (555589 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-100-rate-200\metrics-after-k6.prom`
- `metrics-after-20s.prom` (555592 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-100-rate-200\metrics-after-20s.prom`
- `mysql-digest.txt` (4241 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-100-rate-200\mysql-digest.txt`
- `mysql-slow-group.txt` (0 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-100-rate-200\mysql-slow-group.txt`
- `mysql-waits.txt` (551 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-100-rate-200\mysql-waits.txt`

### 用户对 200，速率 400 msg/s

目录：`D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-200-rate-400`

- `summary.json` (105646 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-200-rate-400\summary.json`
- `k6.log` (18224 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-200-rate-400\k6.log`
- `k6.err.log` (0 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-200-rate-400\k6.err.log`
- `k6-exit-code.txt` (6 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-200-rate-400\k6-exit-code.txt`
- `queues-before.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-200-rate-400\queues-before.json`
- `queue-samples.jsonl` (30494 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-200-rate-400\queue-samples.jsonl`
- `queues-after-k6.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-200-rate-400\queues-after-k6.json`
- `queues-after-20s.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-200-rate-400\queues-after-20s.json`
- `metrics-before.prom` (555578 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-200-rate-400\metrics-before.prom`
- `metrics-after-k6.prom` (555840 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-200-rate-400\metrics-after-k6.prom`
- `metrics-after-20s.prom` (555826 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-200-rate-400\metrics-after-20s.prom`
- `mysql-digest.txt` (5249 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-200-rate-400\mysql-digest.txt`
- `mysql-slow-group.txt` (0 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-200-rate-400\mysql-slow-group.txt`
- `mysql-waits.txt` (502 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-200-rate-400\mysql-waits.txt`

### 用户对 300，速率 600 msg/s

目录：`D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-300-rate-600`

- `summary.json` (154445 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-300-rate-600\summary.json`
- `k6.log` (18338 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-300-rate-600\k6.log`
- `k6.err.log` (0 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-300-rate-600\k6.err.log`
- `k6-exit-code.txt` (6 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-300-rate-600\k6-exit-code.txt`
- `queues-before.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-300-rate-600\queues-before.json`
- `queue-samples.jsonl` (28902 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-300-rate-600\queue-samples.jsonl`
- `queues-after-k6.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-300-rate-600\queues-after-k6.json`
- `queues-after-20s.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-300-rate-600\queues-after-20s.json`
- `metrics-before.prom` (555803 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-300-rate-600\metrics-before.prom`
- `metrics-after-k6.prom` (555856 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-300-rate-600\metrics-after-k6.prom`
- `metrics-after-20s.prom` (555882 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-300-rate-600\metrics-after-20s.prom`
- `mysql-digest.txt` (4254 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-300-rate-600\mysql-digest.txt`
- `mysql-slow-group.txt` (1132 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-300-rate-600\mysql-slow-group.txt`
- `mysql-waits.txt` (508 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-300-rate-600\mysql-waits.txt`

### 用户对 400，速率 800 msg/s

目录：`D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-400-rate-800`

- `summary.json` (203250 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-400-rate-800\summary.json`
- `k6.log` (17046 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-400-rate-800\k6.log`
- `k6.err.log` (0 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-400-rate-800\k6.err.log`
- `k6-exit-code.txt` (6 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-400-rate-800\k6-exit-code.txt`
- `queues-before.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-400-rate-800\queues-before.json`
- `queue-samples.jsonl` (25715 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-400-rate-800\queue-samples.jsonl`
- `queues-after-k6.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-400-rate-800\queues-after-k6.json`
- `queues-after-20s.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-400-rate-800\queues-after-20s.json`
- `metrics-before.prom` (555883 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-400-rate-800\metrics-before.prom`
- `metrics-after-k6.prom` (555867 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-400-rate-800\metrics-after-k6.prom`
- `metrics-after-20s.prom` (555851 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-400-rate-800\metrics-after-20s.prom`
- `mysql-digest.txt` (5248 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-400-rate-800\mysql-digest.txt`
- `mysql-slow-group.txt` (1395 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-400-rate-800\mysql-slow-group.txt`
- `mysql-waits.txt` (509 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-400-rate-800\mysql-waits.txt`

### 用户对 600，速率 1200 msg/s

目录：`D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-600-rate-1200`

- `summary.json` (301013 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-600-rate-1200\summary.json`
- `k6.log` (17630 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-600-rate-1200\k6.log`
- `k6.err.log` (0 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-600-rate-1200\k6.err.log`
- `k6-exit-code.txt` (6 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-600-rate-1200\k6-exit-code.txt`
- `queues-before.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-600-rate-1200\queues-before.json`
- `queue-samples.jsonl` (25789 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-600-rate-1200\queue-samples.jsonl`
- `queues-after-k6.json` (1569 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-600-rate-1200\queues-after-k6.json`
- `queues-after-20s.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-600-rate-1200\queues-after-20s.json`
- `metrics-before.prom` (555850 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-600-rate-1200\metrics-before.prom`
- `metrics-after-k6.prom` (555854 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-600-rate-1200\metrics-after-k6.prom`
- `metrics-after-20s.prom` (555850 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-600-rate-1200\metrics-after-20s.prom`
- `mysql-digest.txt` (5263 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-600-rate-1200\mysql-digest.txt`
- `mysql-slow-group.txt` (5442 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-600-rate-1200\mysql-slow-group.txt`
- `mysql-waits.txt` (569 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-600-rate-1200\mysql-waits.txt`

### 用户对 800，速率 1600 msg/s

目录：`D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-800-rate-1600`

- `summary.json` (398824 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-800-rate-1600\summary.json`
- `k6.log` (17878 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-800-rate-1600\k6.log`
- `k6.err.log` (0 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-800-rate-1600\k6.err.log`
- `k6-exit-code.txt` (6 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-800-rate-1600\k6-exit-code.txt`
- `queues-before.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-800-rate-1600\queues-before.json`
- `queue-samples.jsonl` (25847 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-800-rate-1600\queue-samples.jsonl`
- `queues-after-k6.json` (1571 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-800-rate-1600\queues-after-k6.json`
- `queues-after-20s.json` (1571 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-800-rate-1600\queues-after-20s.json`
- `metrics-before.prom` (555852 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-800-rate-1600\metrics-before.prom`
- `metrics-after-k6.prom` (556014 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-800-rate-1600\metrics-after-k6.prom`
- `metrics-after-20s.prom` (556001 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-800-rate-1600\metrics-after-20s.prom`
- `mysql-digest.txt` (4279 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-800-rate-1600\mysql-digest.txt`
- `mysql-slow-group.txt` (8219 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-800-rate-1600\mysql-slow-group.txt`
- `mysql-waits.txt` (514 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-212948\pairs-800-rate-1600\mysql-waits.txt`

## 本次补充结论

本次压测已去掉 `/me/im/conversations` 调用，runner 只直接写 Redis `im:conv:init:*` 做预热。每个挡位写入的 initKey 数量分别为 5200、5400、5600、5800、6200、6600。

### K6 与容量

| 用户对 | 目标 msg/s | 已发送 | Accepted | 接收方收到 | Check 失败 | Accepted P95 ms |
|---:|---:|---:|---:|---:|---:|---:|
| 100 | 200 | 12000 | 11965 | 11965 | 0 | 5 |
| 200 | 400 | 24000 | 23905 | 23905 | 0 | 5 |
| 300 | 600 | 36000 | 35815 | 35815 | 0 | 6 |
| 400 | 800 | 48000 | 48000 | 48000 | 0 | 6 |
| 600 | 1200 | 72000 | 72000 | 72000 | 0 | 11 |
| 800 | 1600 | 95880 | 95880 | 95880 | 1 | 27 |

去掉窗口接口后，`1600 msg/s` 恢复到基本全量 accepted 和 received，说明上一轮 `1600 msg/s` 失败主要来自额外窗口接口和完整窗口缓存读写干扰，不是 SQL upsert 本身。

### Redis projection 耗时

| 用户对 | 目标 msg/s | `single_conversation_redis_projection` 平均 ms | Redis 队列最大积压 |
|---:|---:|---:|---:|
| 100 | 200 | 0.416 | 0 |
| 200 | 400 | 0.406 | 2 |
| 300 | 600 | 0.455 | 6 |
| 400 | 800 | 0.496 | 4 |
| 600 | 1200 | 0.668 | 4 |
| 800 | 1600 | 0.781 | 8 |

Redis projection 已从上一轮的 3-5ms 回落到 0.4-0.8ms。这说明上一轮高耗时不是 Redis Lua 本身慢，而是 `/me/im/conversations` 预热/读取让窗口 meta/list 完整存在后，Redis projection 执行了更多 JSON、写入和窗口推送逻辑。

注意：本次只是预热 initKey，没有预热每个用户的 `im:conv:meta:*` 和 `im:conv:list:*` baseline。因此 Redis projection 主要验证“initKey 打开但窗口内容缺失时的快速路径”，不能代表真实会话列表已打开后的完整缓存投影成本。

### Conversation 持久化队列

| 用户对 | 目标 msg/s | `single_conversation_persist` 平均 ms | 最大积压 | k6 结束后 | 20s 后 |
|---:|---:|---:|---:|---:|---:|
| 400 | 800 | 1.317 | 752 | 0 | 0 |
| 600 | 1200 | 1.171 | 37337 | 6901 | 0 |
| 800 | 1600 | 1.076 | 72562 | 43867 | 17880 |

DB upsert 后单条 conversation persist 平均耗时约 1.1-1.3ms，明显低于旧报告中的 2ms+。但在 `1200 msg/s` 以上仍会出现大积压，说明消费能力仍受限于 conversation 持久化消费者并发、RabbitMQ 预取、数据库写入抖动或 JVM 调度；`1600 msg/s` 在 20s 后仍剩余 17880 条 conversation 队列积压，已经超过当前配置的可恢复容量。
