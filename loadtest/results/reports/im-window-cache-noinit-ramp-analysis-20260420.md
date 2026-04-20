# IM 在线用户对阶梯压测记录

生成时间：2026-04-20 Asia/Shanghai

## 数据范围

本文档汇总本次阶梯压测每个挡位的结果数据。每个挡位包含 k6 汇总、RabbitMQ 队列快照和采样、应用侧 Prometheus 指标，以及 MySQL Performance Schema / slow log 导出的统计。

原始结果目录：
- `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-215748`

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
| 300 | 600 | 36000 | 36000 | 36000 | 36000 | 0 | 4.7 | 6.0 | 1,306.0 | 0 |
| 500 | 1000 | 60000 | 60000 | 60000 | 60000 | 0 | 6.0 | 8.0 | 1,375.0 | 0 |
| 600 | 1200 | 72000 | 72000 | 72000 | 72000 | 0 | 9.8 | 23.0 | 1,279.0 | 0 |
| 800 | 1600 | 96000 | 96000 | 96000 | 96000 | 0 | 12.7 | 35.0 | 1,303.0 | 0 |

## RabbitMQ 队列积压汇总

`最大积压` 是 `queue-samples.jsonl` 里该队列 `messages` 的最大值。`>=100 采样次数` 表示采样时该队列积压至少 100 条的次数。`Consumer 最小/最大/最后` 来自压测期间 RabbitMQ 队列采样，用于观察 Spring AMQP 动态扩容情况。`k6 结束后` 和 `20s 后` 来自对应的队列快照文件。

| 用户对 | 速率 | 队列 | Consumer 最小 | Consumer 最大 | Consumer 最后 | 最大积压 | >=100 采样次数 | k6 结束后 | 20s 后 |
|---:|---:|---|---:|---:|---:|---:|---:|---:|---:|
| 300 | 600 | `im.message.persist.queue` | 2 | 4 | 3 | 3 | 0 | 0 | 0 |
| 300 | 600 | `im.message.conversation.queue` | 2 | 6 | 5 | 149 | 8 | 0 | 0 |
| 300 | 600 | `im.message.conversation.redis.queue` | 4 | 8 | 7 | 3 | 0 | 0 | 0 |
| 300 | 600 | `im.message.recent.cache.queue` | 4 | 8 | 7 | 2 | 0 | 0 | 0 |
| 300 | 600 | `im.message.realtime.queue` | 2 | 4 | 3 | 4 | 0 | 0 | 0 |
| 500 | 1000 | `im.message.persist.queue` | 3 | 4 | 3 | 60 | 0 | 0 | 0 |
| 500 | 1000 | `im.message.conversation.queue` | 5 | 6 | 6 | 18208 | 12 | 0 | 0 |
| 500 | 1000 | `im.message.conversation.redis.queue` | 7 | 8 | 7 | 7 | 0 | 0 | 0 |
| 500 | 1000 | `im.message.recent.cache.queue` | 7 | 8 | 7 | 4 | 0 | 0 | 0 |
| 500 | 1000 | `im.message.realtime.queue` | 3 | 4 | 3 | 6 | 0 | 0 | 0 |
| 600 | 1200 | `im.message.persist.queue` | 3 | 4 | 3 | 428 | 3 | 0 | 0 |
| 600 | 1200 | `im.message.conversation.queue` | 5 | 6 | 6 | 38120 | 13 | 8605 | 0 |
| 600 | 1200 | `im.message.conversation.redis.queue` | 7 | 8 | 7 | 5 | 0 | 0 | 0 |
| 600 | 1200 | `im.message.recent.cache.queue` | 7 | 8 | 7 | 11 | 0 | 0 | 0 |
| 600 | 1200 | `im.message.realtime.queue` | 3 | 4 | 3 | 7 | 0 | 0 | 0 |
| 800 | 1600 | `im.message.persist.queue` | 3 | 4 | 3 | 15053 | 10 | 0 | 0 |
| 800 | 1600 | `im.message.conversation.queue` | 5 | 6 | 6 | 74203 | 13 | 47673 | 20505 |
| 800 | 1600 | `im.message.conversation.redis.queue` | 7 | 8 | 7 | 10 | 0 | 0 | 0 |
| 800 | 1600 | `im.message.recent.cache.queue` | 7 | 8 | 7 | 9 | 0 | 0 | 0 |
| 800 | 1600 | `im.message.realtime.queue` | 3 | 4 | 3 | 9 | 0 | 0 | 0 |

## 应用侧 DB 操作指标

以下数据来自 `im_db_operation_duration_seconds_*`，按每个挡位的前后差值计算。

| 用户对 | 速率 | 操作 | 次数 | 平均 ms | 总耗时 s |
|---:|---:|---|---:|---:|---:|
| 300 | 600 | `chat_message_insert` | 36000 | 0.461 | 16.580 |
| 300 | 600 | `contact_relation_upsert` | 300 | 0.305 | 0.091 |
| 500 | 1000 | `chat_message_insert` | 60000 | 0.515 | 30.907 |
| 500 | 1000 | `contact_relation_upsert` | 500 | 0.410 | 0.205 |
| 600 | 1200 | `chat_message_insert` | 72000 | 0.516 | 37.187 |
| 600 | 1200 | `contact_relation_upsert` | 600 | 0.401 | 0.240 |
| 800 | 1600 | `chat_message_insert` | 96000 | 0.497 | 47.722 |
| 800 | 1600 | `contact_relation_upsert` | 800 | 0.387 | 0.309 |

## 应用侧 MQ Consumer 指标

以下数据来自 `im_mq_consumer_duration_seconds_*`，按每个挡位的前后差值计算。

| 用户对 | 速率 | Consumer | 队列 | 次数 | 平均 ms | 总耗时 s |
|---:|---:|---|---|---:|---:|---:|
| 300 | 600 | `single_conversation_persist` | `im.message.conversation.queue` | 36000 | 1.136 | 40.906 |
| 300 | 600 | `single_conversation_redis_projection` | `im.message.conversation.redis.queue` | 36000 | 0.458 | 16.505 |
| 300 | 600 | `single_message_persist` | `im.message.persist.queue` | 36000 | 0.778 | 28.001 |
| 300 | 600 | `single_realtime_push` | `im.message.realtime.queue` | 36000 | 0.276 | 9.938 |
| 300 | 600 | `single_recent_message_cache_projection` | `im.message.recent.cache.queue` | 36000 | 0.246 | 8.871 |
| 500 | 1000 | `single_conversation_persist` | `im.message.conversation.queue` | 59999 | 1.236 | 74.143 |
| 500 | 1000 | `single_conversation_redis_projection` | `im.message.conversation.redis.queue` | 60000 | 0.619 | 37.117 |
| 500 | 1000 | `single_message_persist` | `im.message.persist.queue` | 60000 | 0.942 | 56.540 |
| 500 | 1000 | `single_realtime_push` | `im.message.realtime.queue` | 60000 | 0.398 | 23.880 |
| 500 | 1000 | `single_recent_message_cache_projection` | `im.message.recent.cache.queue` | 60000 | 0.336 | 20.146 |
| 600 | 1200 | `single_conversation_persist` | `im.message.conversation.queue` | 72000 | 1.142 | 82.206 |
| 600 | 1200 | `single_conversation_redis_projection` | `im.message.conversation.redis.queue` | 72000 | 0.728 | 52.450 |
| 600 | 1200 | `single_message_persist` | `im.message.persist.queue` | 72000 | 0.974 | 70.163 |
| 600 | 1200 | `single_realtime_push` | `im.message.realtime.queue` | 72000 | 0.487 | 35.057 |
| 600 | 1200 | `single_recent_message_cache_projection` | `im.message.recent.cache.queue` | 72000 | 0.396 | 28.539 |
| 800 | 1600 | `single_conversation_persist` | `im.message.conversation.queue` | 75554 | 1.079 | 81.509 |
| 800 | 1600 | `single_conversation_redis_projection` | `im.message.conversation.redis.queue` | 96000 | 0.814 | 78.104 |
| 800 | 1600 | `single_message_persist` | `im.message.persist.queue` | 96000 | 0.967 | 92.875 |
| 800 | 1600 | `single_realtime_push` | `im.message.realtime.queue` | 96000 | 0.589 | 56.575 |
| 800 | 1600 | `single_recent_message_cache_projection` | `im.message.recent.cache.queue` | 96000 | 0.443 | 42.523 |

## MySQL Digest Top 语句

以下 Top 行来自每个挡位的 `mysql-digest.txt`。

## 与 2026-04-19 redo2-binlog0 对比

本次压测关闭 Redis `initKey` 预热，不调用 `/me/im/conversations`，因此 Redis 窗口投影基本走未初始化跳过路径。对比基准为 `im-online-pairs-ramp-redo2-binlog0-analysis-20260419.md` 的同用户量级挡位。

### K6 端到端结果

| 用户对 | 速率 | 2026-04-19 Accepted P95 ms | 本次 Accepted P95 ms | 2026-04-19 接收 | 本次接收 |
|---:|---:|---:|---:|---:|---:|
| 300 | 600 | 7.0 | 6.0 | 36000 | 36000 |
| 500 | 1000 | 24.0 | 8.0 | 59995 | 60000 |
| 600 | 1200 | 16.0 | 23.0 | 72000 | 72000 |
| 800 | 1600 | 68.0 | 35.0 | 95997 | 96000 |

### conversation 持久化队列

| 用户对 | 速率 | 2026-04-19 最大积压 | 本次最大积压 | 2026-04-19 20s 后 | 本次 20s 后 |
|---:|---:|---:|---:|---:|---:|
| 300 | 600 | 6 | 149 | 0 | 0 |
| 500 | 1000 | 151 | 18208 | 0 | 0 |
| 600 | 1200 | 820 | 38120 | 0 | 0 |
| 800 | 1600 | 56334 | 74203 | 0 | 20505 |

### Consumer 应用内耗时

| 用户对 | 速率 | 2026-04-19 persist 平均 ms | 本次 persist 平均 ms | 2026-04-19 redis 投影平均 ms | 本次 redis 投影平均 ms |
|---:|---:|---:|---:|---:|---:|
| 300 | 600 | 2.206 | 1.136 | 0.546 | 0.458 |
| 500 | 1000 | 2.305 | 1.236 | 0.701 | 0.619 |
| 600 | 1200 | 2.392 | 1.142 | 0.742 | 0.728 |
| 800 | 1600 | 2.121 | 1.079 | 0.845 | 0.814 |

结论：关闭 `initKey` 后，Redis 投影路径不是本次 `im.message.conversation.queue` 积压的原因；`im.message.conversation.redis.queue` 仍保持个位数积压，Redis 投影平均耗时也接近或低于 2026-04-19。真正退化的是 DB 窗口持久化队列的峰值积压，尤其 500/1000 和 600/1200 档位明显高于 2026-04-19。

同时要注意，本次 `single_conversation_persist` 应用内 timer 平均耗时比 2026-04-19 更低，但队列峰值更高。这说明当前 timer 不能单独解释队列吞吐，可能没有覆盖事务提交、Rabbit ack、线程调度、连接池等待或阶段性抖动等队列层面的成本。800/1600 档位本次 20 秒后仍剩 20505 条，所以该档位的 persist 次数和平均耗时只覆盖已消费的 75554 条，不代表完整清空后的总成本。

下一步如果要定位为什么 500/1000 和 600/1200 的队列峰值异常变大，建议单独给 `ConversationWindowPersistConsumer.consume` 外层增加“从 listener 进入到方法返回”的完整耗时指标，或者直接在 Spring AMQP listener 层统计 ack 前耗时；同时采集 Hikari active/pending、Rabbit delivery/ack rate、JVM GC pause，避免只看业务 lambda timer。

### 用户对 300，速率 600 msg/s

| 次数 | 总耗时 s | 平均 ms | 最大 ms | Digest |
|---:|---:|---:|---:|---|
| 217592 | 12.6223 | 0.0580 | 1262.5832 | `SET `autocommit` = ?` |
| 108593 | 10.6564 | 0.0981 | 795.8741 | `COMMIT` |
| 36212 | 9.2527 | 0.2555 | 1302.7497 | `INSERT INTO `chat_conversation` ( `conversation_id` , `owner_user_id` , `target_id` , TYPE , `last_message` , `last_message_time` , `last_server_message_id` , `unread_count` , `is_...` |
| 71995 | 8.8268 | 0.1226 | 1302.6645 | `SELECT `id` , `username` , PASSWORD , `role_code` , STATUS , `create_time` , `update_time` , `delete_time` FROM `t_user` WHERE `id` = ? AND STATUS = ?` |
| 71990 | 7.9504 | 0.1104 | 832.0020 | `SELECT `user_id` AS `userId` , `target_user_id` AS `targetUserId` , `is_contact` AS `isContact` , `is_dm_contact` AS `isDmContact` , `is_blocked` AS `isBlocked` , `is_muted` AS `is...` |
| 36213 | 7.2453 | 0.2001 | 832.0271 | `INSERT INTO `chat_conversation` ( `conversation_id` , `owner_user_id` , `target_id` , TYPE , `last_message` , `last_message_time` , `last_server_message_id` , `unread_count` , `is_...` |
| 110222 | 6.6009 | 0.0599 | 0.7801 | `SELECT @@SESSION . `transaction_read_only`` |
| 35989 | 6.3315 | 0.1759 | 11.7586 | `INSERT INTO `chat_message` ( `server_message_id` , `conversation_id` , `conversation_type` , `sender_id` , `receiver_id` , `client_message_id` , `sender_location` , `message_type` ...` |

### 用户对 500，速率 1000 msg/s

| 次数 | 总耗时 s | 平均 ms | 最大 ms | Digest |
|---:|---:|---:|---:|---|
| 362500 | 24.2736 | 0.0670 | 1277.4157 | `SET `autocommit` = ?` |
| 180984 | 17.7478 | 0.0981 | 103.3103 | `COMMIT` |
| 119982 | 17.1409 | 0.1429 | 1277.5769 | `SELECT `id` , `username` , PASSWORD , `role_code` , STATUS , `create_time` , `update_time` , `delete_time` FROM `t_user` WHERE `id` = ? AND STATUS = ?` |
| 119981 | 16.5594 | 0.1380 | 1370.6601 | `SELECT `user_id` AS `userId` , `target_user_id` AS `targetUserId` , `is_contact` AS `isContact` , `is_dm_contact` AS `isDmContact` , `is_blocked` AS `isBlocked` , `is_muted` AS `is...` |
| 60271 | 15.9429 | 0.2645 | 103.4087 | `INSERT INTO `chat_conversation` ( `conversation_id` , `owner_user_id` , `target_id` , TYPE , `last_message` , `last_message_time` , `last_server_message_id` , `unread_count` , `is_...` |
| 59989 | 14.6849 | 0.2448 | 1281.3488 | `INSERT INTO `chat_message` ( `server_message_id` , `conversation_id` , `conversation_type` , `sender_id` , `receiver_id` , `client_message_id` , `sender_location` , `message_type` ...` |
| 183530 | 11.3825 | 0.0620 | 7.0065 | `SELECT @@SESSION . `transaction_read_only`` |
| 60271 | 11.3580 | 0.1884 | 8.7813 | `INSERT INTO `chat_conversation` ( `conversation_id` , `owner_user_id` , `target_id` , TYPE , `last_message` , `last_message_time` , `last_server_message_id` , `unread_count` , `is_...` |

### 用户对 600，速率 1200 msg/s

| 次数 | 总耗时 s | 平均 ms | 最大 ms | Digest |
|---:|---:|---:|---:|---|
| 435024 | 25.8700 | 0.0595 | 1266.1496 | `SET `autocommit` = ?` |
| 217188 | 22.5518 | 0.1038 | 813.5575 | `COMMIT` |
| 143981 | 22.3740 | 0.1554 | 1262.4766 | `SELECT `id` , `username` , PASSWORD , `role_code` , STATUS , `create_time` , `update_time` , `delete_time` FROM `t_user` WHERE `id` = ? AND STATUS = ?` |
| 143979 | 20.5326 | 0.1426 | 1262.0811 | `SELECT `user_id` AS `userId` , `target_user_id` AS `targetUserId` , `is_contact` AS `isContact` , `is_dm_contact` AS `isDmContact` , `is_blocked` AS `isBlocked` , `is_muted` AS `is...` |
| 72340 | 19.7036 | 0.2724 | 1262.5722 | `INSERT INTO `chat_conversation` ( `conversation_id` , `owner_user_id` , `target_id` , TYPE , `last_message` , `last_message_time` , `last_server_message_id` , `unread_count` , `is_...` |
| 71987 | 17.0083 | 0.2363 | 1266.3443 | `INSERT INTO `chat_message` ( `server_message_id` , `conversation_id` , `conversation_type` , `sender_id` , `receiver_id` , `client_message_id` , `sender_location` , `message_type` ...` |
| 72340 | 13.9196 | 0.1924 | 1266.3589 | `INSERT INTO `chat_conversation` ( `conversation_id` , `owner_user_id` , `target_id` , TYPE , `last_message` , `last_message_time` , `last_server_message_id` , `unread_count` , `is_...` |
| 220266 | 13.0492 | 0.0592 | 3.1974 | `SELECT @@SESSION . `transaction_read_only`` |

### 用户对 800，速率 1600 msg/s

| 次数 | 总耗时 s | 平均 ms | 最大 ms | Digest |
|---:|---:|---:|---:|---|
| 543060 | 33.1254 | 0.0610 | 842.6815 | `SET `autocommit` = ?` |
| 271125 | 31.1681 | 0.1150 | 843.5607 | `COMMIT` |
| 191974 | 28.1944 | 0.1469 | 1285.2396 | `SELECT `user_id` AS `userId` , `target_user_id` AS `targetUserId` , `is_contact` AS `isContact` , `is_dm_contact` AS `isDmContact` , `is_blocked` AS `isBlocked` , `is_muted` AS `is...` |
| 191977 | 25.8521 | 0.1347 | 833.0307 | `SELECT `id` , `username` , PASSWORD , `role_code` , STATUS , `create_time` , `update_time` , `delete_time` FROM `t_user` WHERE `id` = ? AND STATUS = ?` |
| 95988 | 19.9006 | 0.2073 | 1275.9406 | `INSERT INTO `chat_message` ( `server_message_id` , `conversation_id` , `conversation_type` , `sender_id` , `receiver_id` , `client_message_id` , `sender_location` , `message_type` ...` |
| 77964 | 17.7550 | 0.2277 | 111.4296 | `INSERT INTO `chat_conversation` ( `conversation_id` , `owner_user_id` , `target_id` , TYPE , `last_message` , `last_message_time` , `last_server_message_id` , `unread_count` , `is_...` |
| 256720 | 14.4819 | 0.0564 | 2.6026 | `SELECT @@SESSION . `transaction_read_only`` |
| 95990 | 14.1746 | 0.1477 | 6.5562 | `SELECT `id` , `conversation_id` , `owner_user_id` , `target_id` , TYPE , `last_message` , `last_message_time` , `last_server_message_id` , `unread_count` , `is_muted` , `create_tim...` |

## MySQL Waits Top 事件

以下 Top 行来自每个挡位的 `mysql-waits.txt`，已排除 `idle`。

### 用户对 300，速率 600 msg/s

| 事件 | 次数 | 总耗时 s | 平均 ms | 最大 ms |
|---|---:|---:|---:|---:|
| `wait/io/table/sql/handler` | 505466 | 10.0745 | 0.0199 | 12.3126 |
| `wait/io/file/innodb/innodb_data_file` | 16946 | 4.0788 | 0.2407 | 8.5617 |
| `wait/io/file/innodb/innodb_dblwr_file` | 5760 | 2.5254 | 0.4384 | 11.6459 |
| `wait/io/file/innodb/innodb_log_file` | 1193860 | 1.5328 | 0.0013 | 16.8302 |
| `wait/io/file/sql/binlog` | 72079 | 0.4837 | 0.0067 | 0.6767 |
| `wait/lock/table/sql/handler` | 363426 | 0.1957 | 0.0005 | 0.1640 |
| `wait/io/file/csv/data` | 1005 | 0.0053 | 0.0053 | 0.1210 |
| `wait/io/file/csv/metadata` | 7 | 0.0008 | 0.1097 | 0.4330 |

### 用户对 500，速率 1000 msg/s

| 事件 | 次数 | 总耗时 s | 平均 ms | 最大 ms |
|---|---:|---:|---:|---:|
| `wait/io/table/sql/handler` | 842568 | 21.3424 | 0.0253 | 113.0417 |
| `wait/io/file/innodb/innodb_data_file` | 22972 | 7.4177 | 0.3229 | 39.9360 |
| `wait/io/file/innodb/innodb_dblwr_file` | 7874 | 5.0318 | 0.6390 | 58.3791 |
| `wait/io/file/innodb/innodb_log_file` | 2049549 | 3.1123 | 0.0015 | 46.7237 |
| `wait/io/file/sql/binlog` | 119598 | 0.8420 | 0.0070 | 0.9614 |
| `wait/lock/table/sql/handler` | 605542 | 0.3618 | 0.0006 | 0.5582 |
| `wait/io/file/csv/data` | 4789 | 0.0453 | 0.0095 | 2.9101 |

### 用户对 600，速率 1200 msg/s

| 事件 | 次数 | 总耗时 s | 平均 ms | 最大 ms |
|---|---:|---:|---:|---:|
| `wait/io/table/sql/handler` | 1012171 | 24.1183 | 0.0238 | 109.8433 |
| `wait/io/file/innodb/innodb_data_file` | 27219 | 7.5843 | 0.2786 | 11.3080 |
| `wait/io/file/innodb/innodb_dblwr_file` | 9194 | 4.9105 | 0.5341 | 11.2724 |
| `wait/io/file/innodb/innodb_log_file` | 2234988 | 3.2331 | 0.0014 | 17.9917 |
| `wait/io/file/sql/binlog` | 143869 | 1.0542 | 0.0073 | 2.0943 |
| `wait/lock/table/sql/handler` | 726680 | 0.4422 | 0.0006 | 0.6940 |
| `wait/io/file/csv/data` | 4793 | 0.0391 | 0.0082 | 1.3612 |

### 用户对 800，速率 1600 msg/s

| 事件 | 次数 | 总耗时 s | 平均 ms | 最大 ms |
|---|---:|---:|---:|---:|
| `wait/io/table/sql/handler` | 1240844 | 27.5552 | 0.0222 | 119.7193 |
| `wait/io/file/innodb/innodb_data_file` | 33345 | 11.0856 | 0.3325 | 22.4318 |
| `wait/io/file/innodb/innodb_dblwr_file` | 11490 | 7.4021 | 0.6442 | 13.6735 |
| `wait/io/file/innodb/innodb_log_file` | 2118811 | 3.8529 | 0.0018 | 68.8127 |
| `wait/io/file/sql/binlog` | 173635 | 1.4231 | 0.0082 | 4.0431 |
| `wait/lock/table/sql/handler` | 932334 | 0.5643 | 0.0006 | 1.0957 |
| `wait/io/file/csv/data` | 7529 | 0.0619 | 0.0082 | 1.8437 |

## 原始文件索引

### 用户对 300，速率 600 msg/s

目录：`D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-215748\pairs-300-rate-600`

- `summary.json` (154301 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-215748\pairs-300-rate-600\summary.json`
- `k6.log` (17013 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-215748\pairs-300-rate-600\k6.log`
- `k6.err.log` (0 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-215748\pairs-300-rate-600\k6.err.log`
- `k6-exit-code.txt` (6 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-215748\pairs-300-rate-600\k6-exit-code.txt`
- `queues-before.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-215748\pairs-300-rate-600\queues-before.json`
- `queue-samples.jsonl` (27298 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-215748\pairs-300-rate-600\queue-samples.jsonl`
- `queues-after-k6.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-215748\pairs-300-rate-600\queues-after-k6.json`
- `queues-after-20s.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-215748\pairs-300-rate-600\queues-after-20s.json`
- `metrics-before.prom` (555694 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-215748\pairs-300-rate-600\metrics-before.prom`
- `metrics-after-k6.prom` (556010 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-215748\pairs-300-rate-600\metrics-after-k6.prom`
- `metrics-after-20s.prom` (556008 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-215748\pairs-300-rate-600\metrics-after-20s.prom`
- `mysql-digest.txt` (5250 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-215748\pairs-300-rate-600\mysql-digest.txt`
- `mysql-slow-group.txt` (2693 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-215748\pairs-300-rate-600\mysql-slow-group.txt`
- `mysql-waits.txt` (560 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-215748\pairs-300-rate-600\mysql-waits.txt`

### 用户对 500，速率 1000 msg/s

目录：`D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-215748\pairs-500-rate-1000`

- `summary.json` (252100 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-215748\pairs-500-rate-1000\summary.json`
- `k6.log` (17540 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-215748\pairs-500-rate-1000\k6.log`
- `k6.err.log` (0 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-215748\pairs-500-rate-1000\k6.err.log`
- `k6-exit-code.txt` (6 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-215748\pairs-500-rate-1000\k6-exit-code.txt`
- `queues-before.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-215748\pairs-500-rate-1000\queues-before.json`
- `queue-samples.jsonl` (27366 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-215748\pairs-500-rate-1000\queue-samples.jsonl`
- `queues-after-k6.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-215748\pairs-500-rate-1000\queues-after-k6.json`
- `queues-after-20s.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-215748\pairs-500-rate-1000\queues-after-20s.json`
- `metrics-before.prom` (556008 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-215748\pairs-500-rate-1000\metrics-before.prom`
- `metrics-after-k6.prom` (556016 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-215748\pairs-500-rate-1000\metrics-after-k6.prom`
- `metrics-after-20s.prom` (556014 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-215748\pairs-500-rate-1000\metrics-after-20s.prom`
- `mysql-digest.txt` (5265 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-215748\pairs-500-rate-1000\mysql-digest.txt`
- `mysql-slow-group.txt` (7421 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-215748\pairs-500-rate-1000\mysql-slow-group.txt`
- `mysql-waits.txt` (513 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-215748\pairs-500-rate-1000\mysql-waits.txt`

### 用户对 600，速率 1200 msg/s

目录：`D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-215748\pairs-600-rate-1200`

- `summary.json` (301008 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-215748\pairs-600-rate-1200\summary.json`
- `k6.log` (17723 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-215748\pairs-600-rate-1200\k6.log`
- `k6.err.log` (0 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-215748\pairs-600-rate-1200\k6.err.log`
- `k6-exit-code.txt` (6 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-215748\pairs-600-rate-1200\k6-exit-code.txt`
- `queues-before.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-215748\pairs-600-rate-1200\queues-before.json`
- `queue-samples.jsonl` (25795 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-215748\pairs-600-rate-1200\queue-samples.jsonl`
- `queues-after-k6.json` (1569 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-215748\pairs-600-rate-1200\queues-after-k6.json`
- `queues-after-20s.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-215748\pairs-600-rate-1200\queues-after-20s.json`
- `metrics-before.prom` (556016 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-215748\pairs-600-rate-1200\metrics-before.prom`
- `metrics-after-k6.prom` (556023 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-215748\pairs-600-rate-1200\metrics-after-k6.prom`
- `metrics-after-20s.prom` (556033 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-215748\pairs-600-rate-1200\metrics-after-20s.prom`
- `mysql-digest.txt` (5268 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-215748\pairs-600-rate-1200\mysql-digest.txt`
- `mysql-slow-group.txt` (8522 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-215748\pairs-600-rate-1200\mysql-slow-group.txt`
- `mysql-waits.txt` (514 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-215748\pairs-600-rate-1200\mysql-waits.txt`

### 用户对 800，速率 1600 msg/s

目录：`D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-215748\pairs-800-rate-1600`

- `summary.json` (398700 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-215748\pairs-800-rate-1600\summary.json`
- `k6.log` (17832 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-215748\pairs-800-rate-1600\k6.log`
- `k6.err.log` (0 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-215748\pairs-800-rate-1600\k6.err.log`
- `k6-exit-code.txt` (6 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-215748\pairs-800-rate-1600\k6-exit-code.txt`
- `queues-before.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-215748\pairs-800-rate-1600\queues-before.json`
- `queue-samples.jsonl` (25859 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-215748\pairs-800-rate-1600\queue-samples.jsonl`
- `queues-after-k6.json` (1571 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-215748\pairs-800-rate-1600\queues-after-k6.json`
- `queues-after-20s.json` (1571 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-215748\pairs-800-rate-1600\queues-after-20s.json`
- `metrics-before.prom` (556032 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-215748\pairs-800-rate-1600\metrics-before.prom`
- `metrics-after-k6.prom` (556028 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-215748\pairs-800-rate-1600\metrics-after-k6.prom`
- `metrics-after-20s.prom` (556015 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-215748\pairs-800-rate-1600\metrics-after-20s.prom`
- `mysql-digest.txt` (5261 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-215748\pairs-800-rate-1600\mysql-digest.txt`
- `mysql-slow-group.txt` (8038 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-215748\pairs-800-rate-1600\mysql-slow-group.txt`
- `mysql-waits.txt` (510 bytes): `D:\biliibli\loadtest\results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-20260420-215748\pairs-800-rate-1600\mysql-waits.txt`

