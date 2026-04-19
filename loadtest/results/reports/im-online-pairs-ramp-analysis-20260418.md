# IM 在线用户对阶梯压测记录

生成时间：2026-04-18 Asia/Shanghai

## 数据范围

本文档汇总本次阶梯压测每个挡位的结果数据。每个挡位包含 k6 汇总、RabbitMQ 队列快照和采样、应用侧 Prometheus 指标，以及 MySQL Performance Schema / slow log 导出的统计。

原始结果目录：
- `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625`
- `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-205511`
- `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-210228`

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
| 50 | 100 | 6000 | 6000 | 6000 | 6000 | 0 | 4.4 | 6.0 | 54.0 | - |
| 100 | 200 | 12000 | 12000 | 12000 | 12000 | 0 | 4.1 | 6.0 | 31.0 | - |
| 150 | 300 | 18000 | 18000 | 18000 | 18000 | 0 | 4.6 | 6.0 | 84.0 | - |
| 200 | 400 | 24000 | 24000 | 24000 | 24000 | 0 | 4.7 | 7.0 | 122.0 | - |
| 250 | 500 | 30000 | 30000 | 30000 | 30000 | 0 | 6.0 | 10.0 | 88.0 | - |
| 300 | 600 | 36000 | 36000 | 36000 | 36000 | 0 | 6.7 | 10.0 | 70.0 | - |
| 350 | 700 | 42000 | 42000 | 42000 | 42000 | 0 | 10.3 | 19.0 | 214.0 | - |
| 400 | 800 | 48000 | 48000 | 48000 | 48000 | 0 | 9.0 | 15.0 | 104.0 | - |
| 450 | 900 | 54000 | 54000 | 54000 | 54000 | 0 | 9.4 | 18.0 | 124.0 | - |
| 500 | 1000 | 60000 | 60000 | 60000 | 60000 | 0 | 10.2 | 17.0 | 122.0 | - |
| 600 | 1200 | 72000 | 71989 | 71989 | 71989 | 0 | 12.8 | 23.0 | 130.0 | - |
| 800 | 1600 | 96000 | 96000 | 96000 | 96000 | 0 | 53.8 | 168.0 | 649.0 | - |

## RabbitMQ 队列积压汇总

`最大积压` 是 `queue-samples.jsonl` 里该队列 `messages` 的最大值。`>=100 采样次数` 表示采样时该队列积压至少 100 条的次数。`k6 结束后` 和 `20s 后` 来自对应的队列快照文件。

| 用户对 | 速率 | 队列 | Consumer 数 | 最大积压 | >=100 采样次数 | k6 结束后 | 20s 后 |
|---:|---:|---|---:|---:|---:|---:|---:|
| 50 | 100 | `im.message.persist.queue` | 3 | 1 | 0 | 0 | 0 |
| 50 | 100 | `im.message.conversation.queue` | 5 | 1 | 0 | 0 | 0 |
| 50 | 100 | `im.message.conversation.redis.queue` | 7 | 0 | 0 | 0 | 0 |
| 50 | 100 | `im.message.recent.cache.queue` | 7 | 0 | 0 | 0 | 0 |
| 50 | 100 | `im.message.realtime.queue` | 3 | 1 | 0 | 0 | 0 |
| 100 | 200 | `im.message.persist.queue` | 3 | 1 | 0 | 0 | 0 |
| 100 | 200 | `im.message.conversation.queue` | 5 | 3 | 0 | 0 | 0 |
| 100 | 200 | `im.message.conversation.redis.queue` | 7 | 0 | 0 | 0 | 0 |
| 100 | 200 | `im.message.recent.cache.queue` | 7 | 0 | 0 | 0 | 0 |
| 100 | 200 | `im.message.realtime.queue` | 3 | 1 | 0 | 0 | 0 |
| 150 | 300 | `im.message.persist.queue` | 3 | 6 | 0 | 0 | 0 |
| 150 | 300 | `im.message.conversation.queue` | 5 | 7 | 0 | 0 | 0 |
| 150 | 300 | `im.message.conversation.redis.queue` | 7 | 1 | 0 | 0 | 0 |
| 150 | 300 | `im.message.recent.cache.queue` | 7 | 1 | 0 | 0 | 0 |
| 150 | 300 | `im.message.realtime.queue` | 3 | 3 | 0 | 0 | 0 |
| 200 | 400 | `im.message.persist.queue` | 3 | 7 | 0 | 0 | 0 |
| 200 | 400 | `im.message.conversation.queue` | 5 | 7 | 0 | 0 | 0 |
| 200 | 400 | `im.message.conversation.redis.queue` | 7 | 4 | 0 | 0 | 0 |
| 200 | 400 | `im.message.recent.cache.queue` | 7 | 2 | 0 | 0 | 0 |
| 200 | 400 | `im.message.realtime.queue` | 3 | 2 | 0 | 0 | 0 |
| 250 | 500 | `im.message.persist.queue` | 3 | 71 | 0 | 0 | 0 |
| 250 | 500 | `im.message.conversation.queue` | 5 | 11 | 0 | 0 | 0 |
| 250 | 500 | `im.message.conversation.redis.queue` | 7 | 5 | 0 | 0 | 0 |
| 250 | 500 | `im.message.recent.cache.queue` | 7 | 2 | 0 | 0 | 0 |
| 250 | 500 | `im.message.realtime.queue` | 3 | 2 | 0 | 0 | 0 |
| 300 | 600 | `im.message.persist.queue` | 3 | 1135 | 11 | 0 | 0 |
| 300 | 600 | `im.message.conversation.queue` | 5 | 111 | 1 | 0 | 0 |
| 300 | 600 | `im.message.conversation.redis.queue` | 7 | 3 | 0 | 0 | 0 |
| 300 | 600 | `im.message.recent.cache.queue` | 7 | 6 | 0 | 0 | 0 |
| 300 | 600 | `im.message.realtime.queue` | 3 | 5 | 0 | 0 | 0 |
| 350 | 700 | `im.message.persist.queue` | 4 | 12900 | 12 | 0 | 0 |
| 350 | 700 | `im.message.conversation.queue` | 5 | 4874 | 10 | 0 | 0 |
| 350 | 700 | `im.message.conversation.redis.queue` | 7 | 6 | 0 | 0 | 0 |
| 350 | 700 | `im.message.recent.cache.queue` | 7 | 4 | 0 | 0 | 0 |
| 350 | 700 | `im.message.realtime.queue` | 3 | 3 | 0 | 0 | 0 |
| 400 | 800 | `im.message.persist.queue` | 4 | 20873 | 13 | 0 | 0 |
| 400 | 800 | `im.message.conversation.queue` | 6 | 12962 | 11 | 0 | 0 |
| 400 | 800 | `im.message.conversation.redis.queue` | 7 | 3 | 0 | 0 | 0 |
| 400 | 800 | `im.message.recent.cache.queue` | 7 | 4 | 0 | 0 | 0 |
| 400 | 800 | `im.message.realtime.queue` | 3 | 3 | 0 | 0 | 0 |
| 450 | 900 | `im.message.persist.queue` | 4 | 25470 | 14 | 0 | 0 |
| 450 | 900 | `im.message.conversation.queue` | 6 | 23243 | 13 | 0 | 0 |
| 450 | 900 | `im.message.conversation.redis.queue` | 7 | 10 | 0 | 0 | 0 |
| 450 | 900 | `im.message.recent.cache.queue` | 7 | 7 | 0 | 0 | 0 |
| 450 | 900 | `im.message.realtime.queue` | 3 | 4 | 0 | 0 | 0 |
| 500 | 1000 | `im.message.persist.queue` | 4 | 35242 | 14 | 10025 | 0 |
| 500 | 1000 | `im.message.conversation.queue` | 6 | 27982 | 14 | 0 | 0 |
| 500 | 1000 | `im.message.conversation.redis.queue` | 7 | 8 | 0 | 0 | 0 |
| 500 | 1000 | `im.message.recent.cache.queue` | 7 | 6 | 0 | 0 | 0 |
| 500 | 1000 | `im.message.realtime.queue` | 3 | 4 | 0 | 0 | 0 |
| 600 | 1200 | `im.message.persist.queue` | 4 | 52342 | 16 | 15501 | 0 |
| 600 | 1200 | `im.message.conversation.queue` | 6 | 45862 | 16 | 1212 | 0 |
| 600 | 1200 | `im.message.conversation.redis.queue` | 7 | 10 | 0 | 0 | 0 |
| 600 | 1200 | `im.message.recent.cache.queue` | 7 | 10 | 0 | 0 | 0 |
| 600 | 1200 | `im.message.realtime.queue` | 3 | 5 | 0 | 0 | 0 |
| 800 | 1600 | `im.message.persist.queue` | 4 | 87654 | 12 | 66995 | 48363 |
| 800 | 1600 | `im.message.conversation.queue` | 6 | 84376 | 12 | 59279 | 36616 |
| 800 | 1600 | `im.message.conversation.redis.queue` | 7 | 12 | 0 | 0 | 0 |
| 800 | 1600 | `im.message.recent.cache.queue` | 7 | 9 | 0 | 0 | 0 |
| 800 | 1600 | `im.message.realtime.queue` | 3 | 8 | 0 | 0 | 0 |

## 应用侧 DB 操作指标

以下数据来自 `im_db_operation_duration_seconds_*`，按每个挡位的前后差值计算。

| 用户对 | 速率 | 操作 | 次数 | 平均 ms | 总耗时 s |
|---:|---:|---|---:|---:|---:|
| 50 | 100 | `chat_message_insert` | 6000 | 0.537 | 3.225 |
| 50 | 100 | `contact_relation_upsert` | 50 | 0.635 | 0.032 |
| 100 | 200 | `chat_message_insert` | 12000 | 0.480 | 5.755 |
| 100 | 200 | `contact_relation_upsert` | 100 | 0.493 | 0.049 |
| 150 | 300 | `chat_message_insert` | 18000 | 0.517 | 9.300 |
| 150 | 300 | `contact_relation_upsert` | 150 | 0.396 | 0.059 |
| 200 | 400 | `chat_message_insert` | 24000 | 0.500 | 11.992 |
| 200 | 400 | `contact_relation_upsert` | 200 | 0.352 | 0.070 |
| 250 | 500 | `chat_message_insert` | 30000 | 0.505 | 15.136 |
| 250 | 500 | `contact_relation_upsert` | 250 | 0.450 | 0.113 |
| 300 | 600 | `chat_message_insert` | 36000 | 0.483 | 17.401 |
| 300 | 600 | `contact_relation_upsert` | 300 | 0.434 | 0.130 |
| 350 | 700 | `chat_message_insert` | 42000 | 0.471 | 19.777 |
| 350 | 700 | `contact_relation_upsert` | 350 | 0.430 | 0.151 |
| 400 | 800 | `chat_message_insert` | 48000 | 0.462 | 22.183 |
| 400 | 800 | `contact_relation_upsert` | 400 | 0.484 | 0.193 |
| 450 | 900 | `chat_message_insert` | 54000 | 0.452 | 24.391 |
| 450 | 900 | `contact_relation_upsert` | 450 | 0.424 | 0.191 |
| 500 | 1000 | `chat_message_insert` | 60000 | 0.444 | 26.636 |
| 500 | 1000 | `contact_relation_upsert` | 500 | 0.436 | 0.218 |
| 600 | 1200 | `chat_message_insert` | 71989 | 0.430 | 30.954 |
| 600 | 1200 | `contact_relation_upsert` | 600 | 0.477 | 0.286 |
| 800 | 1600 | `chat_message_insert` | 47662 | 0.461 | 21.954 |
| 800 | 1600 | `contact_relation_upsert` | 800 | 0.585 | 0.468 |

## 应用侧 MQ Consumer 指标

以下数据来自 `im_mq_consumer_duration_seconds_*`，按每个挡位的前后差值计算。

| 用户对 | 速率 | Consumer | 队列 | 次数 | 平均 ms | 总耗时 s |
|---:|---:|---|---|---:|---:|---:|
| 50 | 100 | `single_conversation_persist` | `im.message.conversation.queue` | 6000 | 1.868 | 11.206 |
| 50 | 100 | `single_conversation_redis_projection` | `im.message.conversation.redis.queue` | 6000 | 0.454 | 2.726 |
| 50 | 100 | `single_message_persist` | `im.message.persist.queue` | 6000 | 0.838 | 5.025 |
| 50 | 100 | `single_realtime_push` | `im.message.realtime.queue` | 6000 | 0.252 | 1.510 |
| 50 | 100 | `single_recent_message_cache_projection` | `im.message.recent.cache.queue` | 6000 | 0.248 | 1.489 |
| 100 | 200 | `single_conversation_persist` | `im.message.conversation.queue` | 12000 | 1.848 | 22.173 |
| 100 | 200 | `single_conversation_redis_projection` | `im.message.conversation.redis.queue` | 12000 | 0.412 | 4.941 |
| 100 | 200 | `single_message_persist` | `im.message.persist.queue` | 12000 | 0.753 | 9.031 |
| 100 | 200 | `single_realtime_push` | `im.message.realtime.queue` | 12000 | 0.233 | 2.797 |
| 100 | 200 | `single_recent_message_cache_projection` | `im.message.recent.cache.queue` | 12000 | 0.225 | 2.699 |
| 150 | 300 | `single_conversation_persist` | `im.message.conversation.queue` | 18000 | 2.035 | 36.628 |
| 150 | 300 | `single_conversation_redis_projection` | `im.message.conversation.redis.queue` | 18000 | 0.449 | 8.089 |
| 150 | 300 | `single_message_persist` | `im.message.persist.queue` | 18000 | 0.826 | 14.860 |
| 150 | 300 | `single_realtime_push` | `im.message.realtime.queue` | 18000 | 0.266 | 4.791 |
| 150 | 300 | `single_recent_message_cache_projection` | `im.message.recent.cache.queue` | 18000 | 0.249 | 4.478 |
| 200 | 400 | `single_conversation_persist` | `im.message.conversation.queue` | 24000 | 2.023 | 48.564 |
| 200 | 400 | `single_conversation_redis_projection` | `im.message.conversation.redis.queue` | 24000 | 0.456 | 10.940 |
| 200 | 400 | `single_message_persist` | `im.message.persist.queue` | 24000 | 0.811 | 19.465 |
| 200 | 400 | `single_realtime_push` | `im.message.realtime.queue` | 24000 | 0.274 | 6.569 |
| 200 | 400 | `single_recent_message_cache_projection` | `im.message.recent.cache.queue` | 24000 | 0.251 | 6.023 |
| 250 | 500 | `single_conversation_persist` | `im.message.conversation.queue` | 30000 | 2.154 | 64.619 |
| 250 | 500 | `single_conversation_redis_projection` | `im.message.conversation.redis.queue` | 30000 | 0.502 | 15.075 |
| 250 | 500 | `single_message_persist` | `im.message.persist.queue` | 30000 | 0.851 | 25.545 |
| 250 | 500 | `single_realtime_push` | `im.message.realtime.queue` | 30000 | 0.303 | 9.099 |
| 250 | 500 | `single_recent_message_cache_projection` | `im.message.recent.cache.queue` | 30000 | 0.277 | 8.296 |
| 300 | 600 | `single_conversation_persist` | `im.message.conversation.queue` | 36000 | 2.134 | 76.841 |
| 300 | 600 | `single_conversation_redis_projection` | `im.message.conversation.redis.queue` | 36000 | 0.492 | 17.727 |
| 300 | 600 | `single_message_persist` | `im.message.persist.queue` | 36000 | 0.827 | 29.764 |
| 300 | 600 | `single_realtime_push` | `im.message.realtime.queue` | 36000 | 0.310 | 11.143 |
| 300 | 600 | `single_recent_message_cache_projection` | `im.message.recent.cache.queue` | 36000 | 0.271 | 9.754 |
| 350 | 700 | `single_conversation_persist` | `im.message.conversation.queue` | 42000 | 2.164 | 90.870 |
| 350 | 700 | `single_conversation_redis_projection` | `im.message.conversation.redis.queue` | 42000 | 0.581 | 24.408 |
| 350 | 700 | `single_message_persist` | `im.message.persist.queue` | 42000 | 0.817 | 34.295 |
| 350 | 700 | `single_realtime_push` | `im.message.realtime.queue` | 42000 | 0.363 | 15.255 |
| 350 | 700 | `single_recent_message_cache_projection` | `im.message.recent.cache.queue` | 42000 | 0.319 | 13.398 |
| 400 | 800 | `single_conversation_persist` | `im.message.conversation.queue` | 48000 | 2.114 | 101.451 |
| 400 | 800 | `single_conversation_redis_projection` | `im.message.conversation.redis.queue` | 48000 | 0.563 | 27.015 |
| 400 | 800 | `single_message_persist` | `im.message.persist.queue` | 48000 | 0.805 | 38.656 |
| 400 | 800 | `single_realtime_push` | `im.message.realtime.queue` | 48000 | 0.359 | 17.240 |
| 400 | 800 | `single_recent_message_cache_projection` | `im.message.recent.cache.queue` | 48000 | 0.309 | 14.820 |
| 450 | 900 | `single_conversation_persist` | `im.message.conversation.queue` | 54000 | 1.998 | 107.909 |
| 450 | 900 | `single_conversation_redis_projection` | `im.message.conversation.redis.queue` | 54000 | 0.580 | 31.294 |
| 450 | 900 | `single_message_persist` | `im.message.persist.queue` | 54000 | 0.793 | 42.802 |
| 450 | 900 | `single_realtime_push` | `im.message.realtime.queue` | 54000 | 0.374 | 20.181 |
| 450 | 900 | `single_recent_message_cache_projection` | `im.message.recent.cache.queue` | 54000 | 0.317 | 17.098 |
| 500 | 1000 | `single_conversation_persist` | `im.message.conversation.queue` | 60000 | 2.022 | 121.302 |
| 500 | 1000 | `single_conversation_redis_projection` | `im.message.conversation.redis.queue` | 60000 | 0.563 | 33.770 |
| 500 | 1000 | `single_message_persist` | `im.message.persist.queue` | 60000 | 0.781 | 46.863 |
| 500 | 1000 | `single_realtime_push` | `im.message.realtime.queue` | 60000 | 0.376 | 22.575 |
| 500 | 1000 | `single_recent_message_cache_projection` | `im.message.recent.cache.queue` | 60000 | 0.308 | 18.504 |
| 600 | 1200 | `single_conversation_persist` | `im.message.conversation.queue` | 71989 | 1.974 | 142.087 |
| 600 | 1200 | `single_conversation_redis_projection` | `im.message.conversation.redis.queue` | 71989 | 0.629 | 45.261 |
| 600 | 1200 | `single_message_persist` | `im.message.persist.queue` | 71989 | 0.765 | 55.069 |
| 600 | 1200 | `single_realtime_push` | `im.message.realtime.queue` | 71989 | 0.417 | 30.021 |
| 600 | 1200 | `single_recent_message_cache_projection` | `im.message.recent.cache.queue` | 71989 | 0.341 | 24.528 |
| 800 | 1600 | `single_conversation_persist` | `im.message.conversation.queue` | 59420 | 1.984 | 117.866 |
| 800 | 1600 | `single_conversation_redis_projection` | `im.message.conversation.redis.queue` | 96000 | 0.892 | 85.644 |
| 800 | 1600 | `single_message_persist` | `im.message.persist.queue` | 47662 | 0.859 | 40.934 |
| 800 | 1600 | `single_realtime_push` | `im.message.realtime.queue` | 96000 | 0.607 | 58.313 |
| 800 | 1600 | `single_recent_message_cache_projection` | `im.message.recent.cache.queue` | 96000 | 0.487 | 46.725 |

## MySQL Digest Top 语句

以下 Top 行来自每个挡位的 `mysql-digest.txt`。

### 用户对 50，速率 100 msg/s

| 次数 | 总耗时 s | 平均 ms | 最大 ms | Digest |
|---:|---:|---:|---:|---|
| 18100 | 27.8323 | 1.5377 | 9.7504 | `COMMIT` |
| 24000 | 2.6270 | 0.1095 | 0.8802 | `SELECT `id` , `conversation_id` , `owner_user_id` , `target_id` , TYPE , `last_message` , `last_message_time` , `last_server_message_id` , `unread_count` , `is_muted` , `create_tim...` |
| 12000 | 1.9468 | 0.1622 | 5.5410 | `UPDATE `chat_conversation` SET `conversation_id` = ? , `last_message` = ? , `last_message_time` = ? , `last_server_message_id` = ? , `update_time` = NOW WHERE `owner_user_id` = ? A...` |
| 36194 | 1.9140 | 0.0529 | 0.4759 | `SET `autocommit` = ?` |
| 24350 | 1.4107 | 0.0579 | 0.3961 | `SELECT @@SESSION . `transaction_read_only`` |
| 12000 | 1.3206 | 0.1100 | 1.0696 | `SELECT `id` , `username` , PASSWORD , `role_code` , STATUS , `create_time` , `update_time` , `delete_time` FROM `t_user` WHERE `id` = ? AND STATUS = ?` |
| 6000 | 1.2251 | 0.2042 | 8.0947 | `INSERT INTO `chat_message` ( `server_message_id` , `conversation_id` , `conversation_type` , `sender_id` , `receiver_id` , `client_message_id` , `sender_location` , `message_type` ...` |
| 12000 | 0.9929 | 0.0827 | 0.6984 | `SELECT `user_id` AS `userId` , `target_user_id` AS `targetUserId` , `is_contact` AS `isContact` , `is_dm_contact` AS `isDmContact` , `is_blocked` AS `isBlocked` , `is_muted` AS `is...` |

### 用户对 100，速率 200 msg/s

| 次数 | 总耗时 s | 平均 ms | 最大 ms | Digest |
|---:|---:|---:|---:|---|
| 36173 | 70.3659 | 1.9453 | 97.1446 | `COMMIT` |
| 47995 | 5.4966 | 0.1145 | 83.4440 | `SELECT `id` , `conversation_id` , `owner_user_id` , `target_id` , TYPE , `last_message` , `last_message_time` , `last_server_message_id` , `unread_count` , `is_muted` , `create_tim...` |
| 24000 | 3.9144 | 0.1631 | 26.6912 | `UPDATE `chat_conversation` SET `conversation_id` = ? , `last_message` = ? , `last_message_time` = ? , `last_server_message_id` = ? , `update_time` = NOW WHERE `owner_user_id` = ? A...` |
| 72360 | 3.8296 | 0.0529 | 1.8738 | `SET `autocommit` = ?` |
| 48695 | 2.8327 | 0.0582 | 0.8352 | `SELECT @@SESSION . `transaction_read_only`` |
| 23993 | 2.4442 | 0.1019 | 1.3035 | `SELECT `id` , `username` , PASSWORD , `role_code` , STATUS , `create_time` , `update_time` , `delete_time` FROM `t_user` WHERE `id` = ? AND STATUS = ?` |
| 12000 | 2.2949 | 0.1912 | 12.5028 | `INSERT INTO `chat_message` ( `server_message_id` , `conversation_id` , `conversation_type` , `sender_id` , `receiver_id` , `client_message_id` , `sender_location` , `message_type` ...` |
| 23999 | 2.1314 | 0.0888 | 0.7188 | `SELECT `user_id` AS `userId` , `target_user_id` AS `targetUserId` , `is_contact` AS `isContact` , `is_dm_contact` AS `isDmContact` , `is_blocked` AS `isBlocked` , `is_muted` AS `is...` |

### 用户对 150，速率 300 msg/s

| 次数 | 总耗时 s | 平均 ms | 最大 ms | Digest |
|---:|---:|---:|---:|---|
| 54248 | 130.8941 | 2.4129 | 99.8022 | `COMMIT` |
| 71997 | 8.5578 | 0.1189 | 3.4601 | `SELECT `id` , `conversation_id` , `owner_user_id` , `target_id` , TYPE , `last_message` , `last_message_time` , `last_server_message_id` , `unread_count` , `is_muted` , `create_tim...` |
| 35999 | 6.3967 | 0.1777 | 7.7530 | `UPDATE `chat_conversation` SET `conversation_id` = ? , `last_message` = ? , `last_message_time` = ? , `last_server_message_id` = ? , `update_time` = NOW WHERE `owner_user_id` = ? A...` |
| 108567 | 6.3517 | 0.0585 | 1.3509 | `SET `autocommit` = ?` |
| 73037 | 4.5888 | 0.0628 | 0.7294 | `SELECT @@SESSION . `transaction_read_only`` |
| 35995 | 3.9867 | 0.1108 | 0.7174 | `SELECT `id` , `username` , PASSWORD , `role_code` , STATUS , `create_time` , `update_time` , `delete_time` FROM `t_user` WHERE `id` = ? AND STATUS = ?` |
| 17998 | 3.7627 | 0.2091 | 12.9058 | `INSERT INTO `chat_message` ( `server_message_id` , `conversation_id` , `conversation_type` , `sender_id` , `receiver_id` , `client_message_id` , `sender_location` , `message_type` ...` |
| 35996 | 3.6231 | 0.1007 | 1.9235 | `SELECT `user_id` AS `userId` , `target_user_id` AS `targetUserId` , `is_contact` AS `isContact` , `is_dm_contact` AS `isDmContact` , `is_blocked` AS `isBlocked` , `is_muted` AS `is...` |

### 用户对 200，速率 400 msg/s

| 次数 | 总耗时 s | 平均 ms | 最大 ms | Digest |
|---:|---:|---:|---:|---|
| 72319 | 188.0294 | 2.6000 | 119.9173 | `COMMIT` |
| 95990 | 11.3410 | 0.1181 | 3.3513 | `SELECT `id` , `conversation_id` , `owner_user_id` , `target_id` , TYPE , `last_message` , `last_message_time` , `last_server_message_id` , `unread_count` , `is_muted` , `create_tim...` |
| 144740 | 8.4329 | 0.0583 | 115.7624 | `SET `autocommit` = ?` |
| 47996 | 8.3411 | 0.1738 | 9.8687 | `UPDATE `chat_conversation` SET `conversation_id` = ? , `last_message` = ? , `last_message_time` = ? , `last_server_message_id` = ? , `update_time` = NOW WHERE `owner_user_id` = ? A...` |
| 97387 | 6.0795 | 0.0624 | 2.8700 | `SELECT @@SESSION . `transaction_read_only`` |
| 47980 | 5.2190 | 0.1088 | 1.5793 | `SELECT `id` , `username` , PASSWORD , `role_code` , STATUS , `create_time` , `update_time` , `delete_time` FROM `t_user` WHERE `id` = ? AND STATUS = ?` |
| 47996 | 4.8647 | 0.1014 | 3.7508 | `SELECT `user_id` AS `userId` , `target_user_id` AS `targetUserId` , `is_contact` AS `isContact` , `is_dm_contact` AS `isDmContact` , `is_blocked` AS `isBlocked` , `is_muted` AS `is...` |
| 23992 | 4.8522 | 0.2022 | 11.7005 | `INSERT INTO `chat_message` ( `server_message_id` , `conversation_id` , `conversation_type` , `sender_id` , `receiver_id` , `client_message_id` , `sender_location` , `message_type` ...` |

### 用户对 250，速率 500 msg/s

| 次数 | 总耗时 s | 平均 ms | 最大 ms | Digest |
|---:|---:|---:|---:|---|
| 90424 | 280.4941 | 3.1020 | 86.8747 | `COMMIT` |
| 119982 | 14.9701 | 0.1248 | 5.7657 | `SELECT `id` , `conversation_id` , `owner_user_id` , `target_id` , TYPE , `last_message` , `last_message_time` , `last_server_message_id` , `unread_count` , `is_muted` , `create_tim...` |
| 59990 | 10.8913 | 0.1816 | 9.2009 | `UPDATE `chat_conversation` SET `conversation_id` = ? , `last_message` = ? , `last_message_time` = ? , `last_server_message_id` = ? , `update_time` = NOW WHERE `owner_user_id` = ? A...` |
| 180918 | 10.7688 | 0.0595 | 2.4230 | `SET `autocommit` = ?` |
| 121738 | 7.9462 | 0.0653 | 2.8426 | `SELECT @@SESSION . `transaction_read_only`` |
| 59990 | 7.0527 | 0.1176 | 83.5116 | `SELECT `id` , `username` , PASSWORD , `role_code` , STATUS , `create_time` , `update_time` , `delete_time` FROM `t_user` WHERE `id` = ? AND STATUS = ?` |
| 59988 | 6.6107 | 0.1102 | 2.9953 | `SELECT `user_id` AS `userId` , `target_user_id` AS `targetUserId` , `is_contact` AS `isContact` , `is_dm_contact` AS `isDmContact` , `is_blocked` AS `isBlocked` , `is_muted` AS `is...` |
| 29990 | 6.1885 | 0.2064 | 11.4394 | `INSERT INTO `chat_message` ( `server_message_id` , `conversation_id` , `conversation_type` , `sender_id` , `receiver_id` , `client_message_id` , `sender_location` , `message_type` ...` |

### 用户对 300，速率 600 msg/s

| 次数 | 总耗时 s | 平均 ms | 最大 ms | Digest |
|---:|---:|---:|---:|---|
| 108517 | 343.2444 | 3.1630 | 68.3336 | `COMMIT` |
| 143979 | 17.6306 | 0.1225 | 5.3898 | `SELECT `id` , `conversation_id` , `owner_user_id` , `target_id` , TYPE , `last_message` , `last_message_time` , `last_server_message_id` , `unread_count` , `is_muted` , `create_tim...` |
| 71986 | 12.8148 | 0.1780 | 6.5208 | `UPDATE `chat_conversation` SET `conversation_id` = ? , `last_message` = ? , `last_message_time` = ? , `last_server_message_id` = ? , `update_time` = NOW WHERE `owner_user_id` = ? A...` |
| 217115 | 12.6076 | 0.0581 | 2.0047 | `SET `autocommit` = ?` |
| 146078 | 9.4269 | 0.0645 | 1.2076 | `SELECT @@SESSION . `transaction_read_only`` |
| 71980 | 8.3488 | 0.1160 | 2.4481 | `SELECT `id` , `username` , PASSWORD , `role_code` , STATUS , `create_time` , `update_time` , `delete_time` FROM `t_user` WHERE `id` = ? AND STATUS = ?` |
| 71993 | 8.0269 | 0.1115 | 2.0159 | `SELECT `user_id` AS `userId` , `target_user_id` AS `targetUserId` , `is_contact` AS `isContact` , `is_dm_contact` AS `isDmContact` , `is_blocked` AS `isBlocked` , `is_muted` AS `is...` |
| 35987 | 7.0986 | 0.1973 | 24.2855 | `INSERT INTO `chat_message` ( `server_message_id` , `conversation_id` , `conversation_type` , `sender_id` , `receiver_id` , `client_message_id` , `sender_location` , `message_type` ...` |

### 用户对 350，速率 700 msg/s

| 次数 | 总耗时 s | 平均 ms | 最大 ms | Digest |
|---:|---:|---:|---:|---|
| 126596 | 384.3703 | 3.0362 | 88.9768 | `COMMIT` |
| 167960 | 21.1771 | 0.1261 | 82.4258 | `SELECT `id` , `conversation_id` , `owner_user_id` , `target_id` , TYPE , `last_message` , `last_message_time` , `last_server_message_id` , `unread_count` , `is_muted` , `create_tim...` |
| 83987 | 15.3053 | 0.1822 | 18.5993 | `UPDATE `chat_conversation` SET `conversation_id` = ? , `last_message` = ? , `last_message_time` = ? , `last_server_message_id` = ? , `update_time` = NOW WHERE `owner_user_id` = ? A...` |
| 253300 | 14.9327 | 0.0590 | 5.6794 | `SET `autocommit` = ?` |
| 170423 | 11.0736 | 0.0650 | 82.3999 | `SELECT @@SESSION . `transaction_read_only`` |
| 83983 | 10.3758 | 0.1235 | 3.4817 | `SELECT `id` , `username` , PASSWORD , `role_code` , STATUS , `create_time` , `update_time` , `delete_time` FROM `t_user` WHERE `id` = ? AND STATUS = ?` |
| 83985 | 9.9686 | 0.1187 | 3.3658 | `SELECT `user_id` AS `userId` , `target_user_id` AS `targetUserId` , `is_contact` AS `isContact` , `is_dm_contact` AS `isDmContact` , `is_blocked` AS `isBlocked` , `is_muted` AS `is...` |
| 41994 | 7.8608 | 0.1872 | 12.0351 | `INSERT INTO `chat_message` ( `server_message_id` , `conversation_id` , `conversation_type` , `sender_id` , `receiver_id` , `client_message_id` , `sender_location` , `message_type` ...` |

### 用户对 400，速率 800 msg/s

| 次数 | 总耗时 s | 平均 ms | 最大 ms | Digest |
|---:|---:|---:|---:|---|
| 144691 | 424.5613 | 2.9343 | 100.3817 | `COMMIT` |
| 191966 | 23.8432 | 0.1242 | 92.6017 | `SELECT `id` , `conversation_id` , `owner_user_id` , `target_id` , TYPE , `last_message` , `last_message_time` , `last_server_message_id` , `unread_count` , `is_muted` , `create_tim...` |
| 95977 | 16.9839 | 0.1770 | 8.9065 | `UPDATE `chat_conversation` SET `conversation_id` = ? , `last_message` = ? , `last_message_time` = ? , `last_server_message_id` = ? , `update_time` = NOW WHERE `owner_user_id` = ? A...` |
| 289512 | 16.7512 | 0.0579 | 9.7693 | `SET `autocommit` = ?` |
| 194749 | 12.2963 | 0.0631 | 6.8123 | `SELECT @@SESSION . `transaction_read_only`` |
| 95980 | 11.7119 | 0.1220 | 2.5115 | `SELECT `id` , `username` , PASSWORD , `role_code` , STATUS , `create_time` , `update_time` , `delete_time` FROM `t_user` WHERE `id` = ? AND STATUS = ?` |
| 95979 | 11.3523 | 0.1183 | 2.7103 | `SELECT `user_id` AS `userId` , `target_user_id` AS `targetUserId` , `is_contact` AS `isContact` , `is_dm_contact` AS `isDmContact` , `is_blocked` AS `isBlocked` , `is_muted` AS `is...` |
| 47993 | 8.7944 | 0.1832 | 11.3593 | `INSERT INTO `chat_message` ( `server_message_id` , `conversation_id` , `conversation_type` , `sender_id` , `receiver_id` , `client_message_id` , `sender_location` , `message_type` ...` |

### 用户对 450，速率 900 msg/s

| 次数 | 总耗时 s | 平均 ms | 最大 ms | Digest |
|---:|---:|---:|---:|---|
| 162755 | 442.8730 | 2.7211 | 125.5780 | `COMMIT` |
| 215961 | 26.0503 | 0.1206 | 84.4755 | `SELECT `id` , `conversation_id` , `owner_user_id` , `target_id` , TYPE , `last_message` , `last_message_time` , `last_server_message_id` , `unread_count` , `is_muted` , `create_tim...` |
| 325692 | 18.3752 | 0.0564 | 5.7232 | `SET `autocommit` = ?` |
| 107974 | 18.1635 | 0.1682 | 87.5826 | `UPDATE `chat_conversation` SET `conversation_id` = ? , `last_message` = ? , `last_message_time` = ? , `last_server_message_id` = ? , `update_time` = NOW WHERE `owner_user_id` = ? A...` |
| 219094 | 13.3788 | 0.0611 | 119.6515 | `SELECT @@SESSION . `transaction_read_only`` |
| 107971 | 13.1576 | 0.1219 | 3.8703 | `SELECT `id` , `username` , PASSWORD , `role_code` , STATUS , `create_time` , `update_time` , `delete_time` FROM `t_user` WHERE `id` = ? AND STATUS = ?` |
| 107982 | 12.8305 | 0.1188 | 84.4201 | `SELECT `user_id` AS `userId` , `target_user_id` AS `targetUserId` , `is_contact` AS `isContact` , `is_dm_contact` AS `isDmContact` , `is_blocked` AS `isBlocked` , `is_muted` AS `is...` |
| 53986 | 9.6730 | 0.1792 | 11.9445 | `INSERT INTO `chat_message` ( `server_message_id` , `conversation_id` , `conversation_type` , `sender_id` , `receiver_id` , `client_message_id` , `sender_location` , `message_type` ...` |

### 用户对 500，速率 1000 msg/s

| 次数 | 总耗时 s | 平均 ms | 最大 ms | Digest |
|---:|---:|---:|---:|---|
| 180834 | 490.6991 | 2.7135 | 104.5566 | `COMMIT` |
| 239946 | 29.0500 | 0.1211 | 98.0234 | `SELECT `id` , `conversation_id` , `owner_user_id` , `target_id` , TYPE , `last_message` , `last_message_time` , `last_server_message_id` , `unread_count` , `is_muted` , `create_tim...` |
| 361864 | 20.3913 | 0.0564 | 7.0617 | `SET `autocommit` = ?` |
| 119976 | 20.2193 | 0.1685 | 15.4498 | `UPDATE `chat_conversation` SET `conversation_id` = ? , `last_message` = ? , `last_message_time` = ? , `last_server_message_id` = ? , `update_time` = NOW WHERE `owner_user_id` = ? A...` |
| 243450 | 14.6742 | 0.0603 | 1.8581 | `SELECT @@SESSION . `transaction_read_only`` |
| 119973 | 14.6628 | 0.1222 | 98.1277 | `SELECT `id` , `username` , PASSWORD , `role_code` , STATUS , `create_time` , `update_time` , `delete_time` FROM `t_user` WHERE `id` = ? AND STATUS = ?` |
| 119979 | 14.2405 | 0.1187 | 98.0390 | `SELECT `user_id` AS `userId` , `target_user_id` AS `targetUserId` , `is_contact` AS `isContact` , `is_dm_contact` AS `isDmContact` , `is_blocked` AS `isBlocked` , `is_muted` AS `is...` |
| 59989 | 10.5623 | 0.1761 | 70.4070 | `INSERT INTO `chat_message` ( `server_message_id` , `conversation_id` , `conversation_type` , `sender_id` , `receiver_id` , `client_message_id` , `sender_location` , `message_type` ...` |

### 用户对 600，速率 1200 msg/s

| 次数 | 总耗时 s | 平均 ms | 最大 ms | Digest |
|---:|---:|---:|---:|---|
| 217062 | 563.3394 | 2.5953 | 18446744066.2376 | `COMMIT` |
| 287897 | 34.5599 | 0.1200 | 9.8168 | `SELECT `id` , `conversation_id` , `owner_user_id` , `target_id` , TYPE , `last_message` , `last_message_time` , `last_server_message_id` , `unread_count` , `is_muted` , `create_tim...` |
| 434225 | 24.1783 | 0.0557 | 3.4660 | `SET `autocommit` = ?` |
| 143960 | 23.8232 | 0.1655 | 96.2637 | `UPDATE `chat_conversation` SET `conversation_id` = ? , `last_message` = ? , `last_message_time` = ? , `last_server_message_id` = ? , `update_time` = NOW WHERE `owner_user_id` = ? A...` |
| 143963 | 18.1715 | 0.1262 | 18446744060.0487 | `SELECT `id` , `username` , PASSWORD , `role_code` , STATUS , `create_time` , `update_time` , `delete_time` FROM `t_user` WHERE `id` = ? AND STATUS = ?` |
| 143965 | 17.6893 | 0.1229 | 3.4282 | `SELECT `user_id` AS `userId` , `target_user_id` AS `targetUserId` , `is_contact` AS `isContact` , `is_dm_contact` AS `isDmContact` , `is_blocked` AS `isBlocked` , `is_muted` AS `is...` |
| 292094 | 17.0521 | 0.0584 | 18446744060.0415 | `SELECT @@SESSION . `transaction_read_only`` |
| 71973 | 12.2900 | 0.1708 | 66.9604 | `INSERT INTO `chat_message` ( `server_message_id` , `conversation_id` , `conversation_type` , `sender_id` , `receiver_id` , `client_message_id` , `sender_location` , `message_type` ...` |

### 用户对 800，速率 1600 msg/s

| 次数 | 总耗时 s | 平均 ms | 最大 ms | Digest |
|---:|---:|---:|---:|---|
| 205411 | 434.9838 | 2.1176 | 18446744068.8169 | `COMMIT` |
| 275446 | 36.1097 | 0.1311 | 91.6247 | `SELECT `id` , `conversation_id` , `owner_user_id` , `target_id` , TYPE , `last_message` , `last_message_time` , `last_server_message_id` , `unread_count` , `is_muted` , `create_tim...` |
| 191979 | 27.8392 | 0.1450 | 9.7386 | `SELECT `id` , `username` , PASSWORD , `role_code` , STATUS , `create_time` , `update_time` , `delete_time` FROM `t_user` WHERE `id` = ? AND STATUS = ?` |
| 191972 | 27.0067 | 0.1407 | 14.2756 | `SELECT `user_id` AS `userId` , `target_user_id` AS `targetUserId` , `is_contact` AS `isContact` , `is_dm_contact` AS `isDmContact` , `is_blocked` AS `isBlocked` , `is_muted` AS `is...` |
| 410781 | 25.6491 | 0.0624 | 4.6120 | `SET `autocommit` = ?` |
| 119654 | 20.0878 | 0.1679 | 104.9560 | `UPDATE `chat_conversation` SET `conversation_id` = ? , `last_message` = ? , `last_message_time` = ? , `last_server_message_id` = ? , `update_time` = NOW WHERE `owner_user_id` = ? A...` |
| 233060 | 13.5405 | 0.0581 | 2.7875 | `SELECT @@SESSION . `transaction_read_only`` |
| 95988 | 12.7244 | 0.1326 | 3.5523 | `SELECT `user_id` AS `userId` , `like_enabled` AS `likeEnabled` , `comment_enabled` AS `commentEnabled` , `im_message_send_enabled` AS `imMessageSendEnabled` , `video_upload_enabled...` |

## MySQL Waits Top 事件

以下 Top 行来自每个挡位的 `mysql-waits.txt`，已排除 `idle`。

### 用户对 50，速率 100 msg/s

| 事件 | 次数 | 总耗时 s | 平均 ms | 最大 ms |
|---|---:|---:|---:|---:|
| `wait/io/file/innodb/innodb_log_file` | 73775 | 18.9274 | 0.2566 | 8.3835 |
| `wait/io/file/sql/binlog` | 24260 | 10.7428 | 0.4428 | 5.9124 |
| `wait/io/table/sql/handler` | 102550 | 1.5138 | 0.0148 | 7.2568 |
| `wait/io/file/innodb/innodb_data_file` | 2745 | 0.6174 | 0.2249 | 6.3921 |
| `wait/io/file/innodb/innodb_dblwr_file` | 946 | 0.3295 | 0.3483 | 7.6906 |
| `wait/lock/table/sql/handler` | 84550 | 0.0400 | 0.0005 | 0.0310 |
| `wait/io/file/csv/metadata` | 7 | 0.0009 | 0.1239 | 0.4559 |
| `wait/io/file/csv/data` | 6 | 0.0001 | 0.0093 | 0.0154 |

### 用户对 100，速率 200 msg/s

| 事件 | 次数 | 总耗时 s | 平均 ms | 最大 ms |
|---|---:|---:|---:|---:|
| `wait/io/file/innodb/innodb_log_file` | 112592 | 27.9292 | 0.2481 | 7.8397 |
| `wait/io/file/sql/binlog` | 38863 | 20.4500 | 0.5262 | 10.6465 |
| `wait/io/table/sql/handler` | 205100 | 2.9253 | 0.0143 | 26.5996 |
| `wait/io/file/innodb/innodb_data_file` | 6135 | 1.4473 | 0.2359 | 4.9002 |
| `wait/io/file/innodb/innodb_dblwr_file` | 2048 | 0.8835 | 0.4314 | 29.5684 |
| `wait/lock/table/sql/handler` | 169100 | 0.0751 | 0.0004 | 0.1033 |
| `wait/io/file/csv/data` | 4 | 0.0001 | 0.0191 | 0.0387 |

### 用户对 150，速率 300 msg/s

| 事件 | 次数 | 总耗时 s | 平均 ms | 最大 ms |
|---|---:|---:|---:|---:|
| `wait/io/file/innodb/innodb_log_file` | 146890 | 38.8124 | 0.2642 | 18.7591 |
| `wait/io/file/sql/binlog` | 52347 | 33.1894 | 0.6340 | 18.3060 |
| `wait/io/table/sql/handler` | 307650 | 4.5376 | 0.0147 | 12.8682 |
| `wait/io/file/innodb/innodb_data_file` | 8374 | 2.1519 | 0.2570 | 6.4644 |
| `wait/io/file/innodb/innodb_dblwr_file` | 2806 | 1.3112 | 0.4673 | 8.2621 |
| `wait/lock/table/sql/handler` | 253650 | 0.1234 | 0.0005 | 0.1325 |
| `wait/io/file/csv/data` | 4 | 0.0001 | 0.0143 | 0.0260 |

### 用户对 200，速率 400 msg/s

| 事件 | 次数 | 总耗时 s | 平均 ms | 最大 ms |
|---|---:|---:|---:|---:|
| `wait/io/file/innodb/innodb_log_file` | 168162 | 44.4449 | 0.2643 | 18.3338 |
| `wait/io/file/sql/binlog` | 60675 | 40.2202 | 0.6629 | 11.2022 |
| `wait/io/table/sql/handler` | 410200 | 5.7456 | 0.0140 | 11.4194 |
| `wait/io/file/innodb/innodb_data_file` | 10797 | 2.8446 | 0.2635 | 6.0886 |
| `wait/io/file/innodb/innodb_dblwr_file` | 3612 | 1.7624 | 0.4879 | 6.4863 |
| `wait/lock/table/sql/handler` | 338200 | 0.1612 | 0.0005 | 0.3479 |
| `wait/io/file/csv/data` | 4 | 0.0001 | 0.0207 | 0.0516 |

### 用户对 250，速率 500 msg/s

| 事件 | 次数 | 总耗时 s | 平均 ms | 最大 ms |
|---|---:|---:|---:|---:|
| `wait/io/file/innodb/innodb_log_file` | 178973 | 49.5812 | 0.2770 | 24.5934 |
| `wait/io/file/sql/binlog` | 63736 | 48.1930 | 0.7561 | 26.0777 |
| `wait/io/table/sql/handler` | 512750 | 7.6894 | 0.0150 | 11.3586 |
| `wait/io/file/innodb/innodb_data_file` | 12027 | 4.0964 | 0.3406 | 8.5360 |
| `wait/io/file/innodb/innodb_dblwr_file` | 3984 | 2.7123 | 0.6808 | 5.0345 |
| `wait/lock/table/sql/handler` | 422750 | 0.2107 | 0.0005 | 0.9371 |
| `wait/io/file/csv/data` | 4 | 0.0001 | 0.0155 | 0.0390 |

### 用户对 300，速率 600 msg/s

| 事件 | 次数 | 总耗时 s | 平均 ms | 最大 ms |
|---|---:|---:|---:|---:|
| `wait/io/file/sql/binlog` | 70223 | 54.0967 | 0.7704 | 11.9009 |
| `wait/io/file/innodb/innodb_log_file` | 197272 | 53.7167 | 0.2723 | 11.7567 |
| `wait/io/table/sql/handler` | 615300 | 8.7914 | 0.0143 | 24.3118 |
| `wait/io/file/innodb/innodb_data_file` | 13900 | 4.7263 | 0.3400 | 4.1323 |
| `wait/io/file/innodb/innodb_dblwr_file` | 4618 | 3.1794 | 0.6885 | 7.4241 |
| `wait/lock/table/sql/handler` | 507300 | 0.2466 | 0.0005 | 0.8455 |
| `wait/io/file/csv/data` | 4 | 0.0001 | 0.0138 | 0.0297 |

### 用户对 350，速率 700 msg/s

| 事件 | 次数 | 总耗时 s | 平均 ms | 最大 ms |
|---|---:|---:|---:|---:|
| `wait/io/file/innodb/innodb_log_file` | 314793 | 63.6581 | 0.2022 | 27.4272 |
| `wait/io/file/sql/binlog` | 83737 | 62.1132 | 0.7418 | 26.2820 |
| `wait/io/table/sql/handler` | 717850 | 10.3496 | 0.0144 | 18.5025 |
| `wait/io/file/innodb/innodb_data_file` | 16315 | 6.1937 | 0.3796 | 30.9175 |
| `wait/io/file/innodb/innodb_dblwr_file` | 5462 | 4.1625 | 0.7621 | 12.1705 |
| `wait/lock/table/sql/handler` | 591850 | 0.2945 | 0.0005 | 0.3651 |
| `wait/io/file/csv/data` | 4 | 0.0001 | 0.0285 | 0.0507 |

### 用户对 400，速率 800 msg/s

| 事件 | 次数 | 总耗时 s | 平均 ms | 最大 ms |
|---|---:|---:|---:|---:|
| `wait/io/file/innodb/innodb_log_file` | 354436 | 70.6826 | 0.1994 | 16.5441 |
| `wait/io/file/sql/binlog` | 95618 | 69.4309 | 0.7261 | 16.8169 |
| `wait/io/table/sql/handler` | 820400 | 11.2903 | 0.0138 | 12.1309 |
| `wait/io/file/innodb/innodb_data_file` | 18253 | 5.6132 | 0.3075 | 13.8904 |
| `wait/io/file/innodb/innodb_dblwr_file` | 6124 | 3.6329 | 0.5932 | 13.8581 |
| `wait/lock/table/sql/handler` | 676400 | 0.3327 | 0.0005 | 0.3831 |
| `wait/io/file/csv/data` | 4 | 0.0000 | 0.0125 | 0.0256 |

### 用户对 450，速率 900 msg/s

| 事件 | 次数 | 总耗时 s | 平均 ms | 最大 ms |
|---|---:|---:|---:|---:|
| `wait/io/file/innodb/innodb_log_file` | 343412 | 74.5770 | 0.2172 | 13.7801 |
| `wait/io/file/sql/binlog` | 104738 | 73.1481 | 0.6984 | 14.6469 |
| `wait/io/table/sql/handler` | 922950 | 12.3644 | 0.0134 | 11.8181 |
| `wait/io/file/innodb/innodb_data_file` | 20060 | 6.5264 | 0.3253 | 9.8213 |
| `wait/io/file/innodb/innodb_dblwr_file` | 6794 | 4.3393 | 0.6387 | 9.8095 |
| `wait/lock/table/sql/handler` | 760950 | 0.3598 | 0.0005 | 0.3318 |
| `wait/io/file/csv/metadata` | 7 | 0.0007 | 0.1012 | 0.4623 |
| `wait/io/file/csv/data` | 6 | 0.0001 | 0.0174 | 0.0541 |

### 用户对 500，速率 1000 msg/s

| 事件 | 次数 | 总耗时 s | 平均 ms | 最大 ms |
|---|---:|---:|---:|---:|
| `wait/io/file/innodb/innodb_log_file` | 441248 | 83.6582 | 0.1896 | 17.8052 |
| `wait/io/file/sql/binlog` | 119027 | 82.0684 | 0.6895 | 21.0680 |
| `wait/io/table/sql/handler` | 1025500 | 13.4778 | 0.0131 | 15.1689 |
| `wait/io/file/innodb/innodb_data_file` | 22941 | 7.5753 | 0.3302 | 15.6519 |
| `wait/io/file/innodb/innodb_dblwr_file` | 7776 | 5.1724 | 0.6652 | 60.2043 |
| `wait/lock/table/sql/handler` | 845500 | 0.4034 | 0.0005 | 0.9062 |
| `wait/io/file/csv/data` | 4 | 0.0000 | 0.0107 | 0.0206 |

### 用户对 600，速率 1200 msg/s

| 事件 | 次数 | 总耗时 s | 平均 ms | 最大 ms |
|---|---:|---:|---:|---:|
| `wait/io/file/innodb/innodb_log_file` | 385932 | 96.6920 | 0.2505 | 21.7489 |
| `wait/io/file/sql/binlog` | 140835 | 95.0792 | 0.6751 | 21.6205 |
| `wait/io/table/sql/handler` | 1230490 | 16.1086 | 0.0131 | 14.5827 |
| `wait/io/file/innodb/innodb_data_file` | 27282 | 9.0406 | 0.3314 | 11.3914 |
| `wait/io/file/innodb/innodb_dblwr_file` | 9242 | 6.0908 | 0.6590 | 45.0049 |
| `wait/io/file/csv/data` | 180641 | 0.6270 | 0.0035 | 3.5836 |
| `wait/lock/table/sql/handler` | 1014523 | 0.4828 | 0.0005 | 0.7044 |

### 用户对 800，速率 1600 msg/s

| 事件 | 次数 | 总耗时 s | 平均 ms | 最大 ms |
|---|---:|---:|---:|---:|
| `wait/io/file/innodb/innodb_log_file` | 275543 | 84.5868 | 0.3070 | 27.4665 |
| `wait/io/file/sql/binlog` | 101430 | 73.0465 | 0.7202 | 30.1214 |
| `wait/io/table/sql/handler` | 1268721 | 16.6733 | 0.0131 | 29.3885 |
| `wait/io/file/innodb/innodb_data_file` | 20875 | 8.3522 | 0.4001 | 15.5985 |
| `wait/io/file/innodb/innodb_dblwr_file` | 7018 | 5.6629 | 0.8069 | 19.3844 |
| `wait/io/file/csv/data` | 278484 | 0.9408 | 0.0034 | 6.1901 |
| `wait/lock/table/sql/handler` | 1088783 | 0.5819 | 0.0005 | 1.3988 |
| `wait/io/file/sql/binlog_index` | 18 | 0.0017 | 0.0947 | 0.8558 |

## 原始文件索引

### 用户对 50，速率 100 msg/s

目录：`D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-50-rate-100`

- `summary.json` (32025 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-50-rate-100\summary.json`
- `k6.log` (17487 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-50-rate-100\k6.log`
- `k6.err.log` (0 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-50-rate-100\k6.err.log`
- `k6-exit-code.txt` (3 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-50-rate-100\k6-exit-code.txt`
- `queues-before.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-50-rate-100\queues-before.json`
- `queue-samples.jsonl` (27254 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-50-rate-100\queue-samples.jsonl`
- `queues-after-k6.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-50-rate-100\queues-after-k6.json`
- `queues-after-20s.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-50-rate-100\queues-after-20s.json`
- `metrics-before.prom` (80689 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-50-rate-100\metrics-before.prom`
- `metrics-after-k6.prom` (446227 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-50-rate-100\metrics-after-k6.prom`
- `metrics-after-20s.prom` (446229 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-50-rate-100\metrics-after-20s.prom`
- `mysql-digest.txt` (3201 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-50-rate-100\mysql-digest.txt`
- `mysql-slow-group.txt` (0 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-50-rate-100\mysql-slow-group.txt`
- `mysql-waits.txt` (543 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-50-rate-100\mysql-waits.txt`

### 用户对 100，速率 200 msg/s

目录：`D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-100-rate-200`

- `summary.json` (56484 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-100-rate-200\summary.json`
- `k6.log` (17652 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-100-rate-200\k6.log`
- `k6.err.log` (0 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-100-rate-200\k6.err.log`
- `k6-exit-code.txt` (3 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-100-rate-200\k6-exit-code.txt`
- `queues-before.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-100-rate-200\queues-before.json`
- `queue-samples.jsonl` (27254 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-100-rate-200\queue-samples.jsonl`
- `queues-after-k6.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-100-rate-200\queues-after-k6.json`
- `queues-after-20s.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-100-rate-200\queues-after-20s.json`
- `metrics-before.prom` (446231 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-100-rate-200\metrics-before.prom`
- `metrics-after-k6.prom` (449106 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-100-rate-200\metrics-after-k6.prom`
- `metrics-after-20s.prom` (449111 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-100-rate-200\metrics-after-20s.prom`
- `mysql-digest.txt` (3210 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-100-rate-200\mysql-digest.txt`
- `mysql-slow-group.txt` (0 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-100-rate-200\mysql-slow-group.txt`
- `mysql-waits.txt` (499 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-100-rate-200\mysql-waits.txt`

### 用户对 150，速率 300 msg/s

目录：`D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-150-rate-300`

- `summary.json` (80965 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-150-rate-300\summary.json`
- `k6.log` (17666 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-150-rate-300\k6.log`
- `k6.err.log` (0 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-150-rate-300\k6.err.log`
- `k6-exit-code.txt` (3 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-150-rate-300\k6-exit-code.txt`
- `queues-before.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-150-rate-300\queues-before.json`
- `queue-samples.jsonl` (27254 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-150-rate-300\queue-samples.jsonl`
- `queues-after-k6.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-150-rate-300\queues-after-k6.json`
- `queues-after-20s.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-150-rate-300\queues-after-20s.json`
- `metrics-before.prom` (449106 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-150-rate-300\metrics-before.prom`
- `metrics-after-k6.prom` (451518 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-150-rate-300\metrics-after-k6.prom`
- `metrics-after-20s.prom` (451516 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-150-rate-300\metrics-after-20s.prom`
- `mysql-digest.txt` (3210 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-150-rate-300\mysql-digest.txt`
- `mysql-slow-group.txt` (0 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-150-rate-300\mysql-slow-group.txt`
- `mysql-waits.txt` (500 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-150-rate-300\mysql-waits.txt`

### 用户对 200，速率 400 msg/s

目录：`D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-200-rate-400`

- `summary.json` (105289 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-200-rate-400\summary.json`
- `k6.log` (17676 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-200-rate-400\k6.log`
- `k6.err.log` (0 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-200-rate-400\k6.err.log`
- `k6-exit-code.txt` (3 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-200-rate-400\k6-exit-code.txt`
- `queues-before.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-200-rate-400\queues-before.json`
- `queue-samples.jsonl` (27254 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-200-rate-400\queue-samples.jsonl`
- `queues-after-k6.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-200-rate-400\queues-after-k6.json`
- `queues-after-20s.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-200-rate-400\queues-after-20s.json`
- `metrics-before.prom` (451513 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-200-rate-400\metrics-before.prom`
- `metrics-after-k6.prom` (451640 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-200-rate-400\metrics-after-k6.prom`
- `metrics-after-20s.prom` (451635 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-200-rate-400\metrics-after-20s.prom`
- `mysql-digest.txt` (3216 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-200-rate-400\mysql-digest.txt`
- `mysql-slow-group.txt` (0 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-200-rate-400\mysql-slow-group.txt`
- `mysql-waits.txt` (501 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-200-rate-400\mysql-waits.txt`

### 用户对 250，速率 500 msg/s

目录：`D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-250-rate-500`

- `summary.json` (129821 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-250-rate-500\summary.json`
- `k6.log` (17864 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-250-rate-500\k6.log`
- `k6.err.log` (0 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-250-rate-500\k6.err.log`
- `k6-exit-code.txt` (3 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-250-rate-500\k6-exit-code.txt`
- `queues-before.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-250-rate-500\queues-before.json`
- `queue-samples.jsonl` (27272 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-250-rate-500\queue-samples.jsonl`
- `queues-after-k6.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-250-rate-500\queues-after-k6.json`
- `queues-after-20s.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-250-rate-500\queues-after-20s.json`
- `metrics-before.prom` (451626 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-250-rate-500\metrics-before.prom`
- `metrics-after-k6.prom` (451667 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-250-rate-500\metrics-after-k6.prom`
- `metrics-after-20s.prom` (451668 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-250-rate-500\metrics-after-20s.prom`
- `mysql-digest.txt` (3216 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-250-rate-500\mysql-digest.txt`
- `mysql-slow-group.txt` (0 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-250-rate-500\mysql-slow-group.txt`
- `mysql-waits.txt` (501 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-250-rate-500\mysql-waits.txt`

### 用户对 300，速率 600 msg/s

目录：`D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-300-rate-600`

- `summary.json` (154287 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-300-rate-600\summary.json`
- `k6.log` (17884 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-300-rate-600\k6.log`
- `k6.err.log` (0 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-300-rate-600\k6.err.log`
- `k6-exit-code.txt` (3 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-300-rate-600\k6-exit-code.txt`
- `queues-before.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-300-rate-600\queues-before.json`
- `queue-samples.jsonl` (27328 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-300-rate-600\queue-samples.jsonl`
- `queues-after-k6.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-300-rate-600\queues-after-k6.json`
- `queues-after-20s.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-300-rate-600\queues-after-20s.json`
- `metrics-before.prom` (451663 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-300-rate-600\metrics-before.prom`
- `metrics-after-k6.prom` (454555 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-300-rate-600\metrics-after-k6.prom`
- `metrics-after-20s.prom` (454531 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-300-rate-600\metrics-after-20s.prom`
- `mysql-digest.txt` (3217 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-300-rate-600\mysql-digest.txt`
- `mysql-slow-group.txt` (0 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-300-rate-600\mysql-slow-group.txt`
- `mysql-waits.txt` (501 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-300-rate-600\mysql-waits.txt`

### 用户对 350，速率 700 msg/s

目录：`D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-350-rate-700`

- `summary.json` (178753 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-350-rate-700\summary.json`
- `k6.log` (18047 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-350-rate-700\k6.log`
- `k6.err.log` (0 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-350-rate-700\k6.err.log`
- `k6-exit-code.txt` (3 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-350-rate-700\k6-exit-code.txt`
- `queues-before.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-350-rate-700\queues-before.json`
- `queue-samples.jsonl` (27418 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-350-rate-700\queue-samples.jsonl`
- `queues-after-k6.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-350-rate-700\queues-after-k6.json`
- `queues-after-20s.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-350-rate-700\queues-after-20s.json`
- `metrics-before.prom` (454533 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-350-rate-700\metrics-before.prom`
- `metrics-after-k6.prom` (454543 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-350-rate-700\metrics-after-k6.prom`
- `metrics-after-20s.prom` (454585 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-350-rate-700\metrics-after-20s.prom`
- `mysql-digest.txt` (3223 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-350-rate-700\mysql-digest.txt`
- `mysql-slow-group.txt` (0 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-350-rate-700\mysql-slow-group.txt`
- `mysql-waits.txt` (504 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-350-rate-700\mysql-waits.txt`

### 用户对 400，速率 800 msg/s

目录：`D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-400-rate-800`

- `summary.json` (203201 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-400-rate-800\summary.json`
- `k6.log` (18142 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-400-rate-800\k6.log`
- `k6.err.log` (0 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-400-rate-800\k6.err.log`
- `k6-exit-code.txt` (3 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-400-rate-800\k6-exit-code.txt`
- `queues-before.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-400-rate-800\queues-before.json`
- `queue-samples.jsonl` (27455 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-400-rate-800\queue-samples.jsonl`
- `queues-after-k6.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-400-rate-800\queues-after-k6.json`
- `queues-after-20s.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-400-rate-800\queues-after-20s.json`
- `metrics-before.prom` (454588 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-400-rate-800\metrics-before.prom`
- `metrics-after-k6.prom` (454634 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-400-rate-800\metrics-after-k6.prom`
- `metrics-after-20s.prom` (454633 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-400-rate-800\metrics-after-20s.prom`
- `mysql-digest.txt` (3223 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-400-rate-800\mysql-digest.txt`
- `mysql-slow-group.txt` (0 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-400-rate-800\mysql-slow-group.txt`
- `mysql-waits.txt` (504 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-203625\pairs-400-rate-800\mysql-waits.txt`

### 用户对 450，速率 900 msg/s

目录：`D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-205511\pairs-450-rate-900`

- `summary.json` (227679 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-205511\pairs-450-rate-900\summary.json`
- `k6.log` (18128 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-205511\pairs-450-rate-900\k6.log`
- `k6.err.log` (0 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-205511\pairs-450-rate-900\k6.err.log`
- `k6-exit-code.txt` (3 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-205511\pairs-450-rate-900\k6-exit-code.txt`
- `queues-before.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-205511\pairs-450-rate-900\queues-before.json`
- `queue-samples.jsonl` (27491 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-205511\pairs-450-rate-900\queue-samples.jsonl`
- `queues-after-k6.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-205511\pairs-450-rate-900\queues-after-k6.json`
- `queues-after-20s.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-205511\pairs-450-rate-900\queues-after-20s.json`
- `metrics-before.prom` (454303 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-205511\pairs-450-rate-900\metrics-before.prom`
- `metrics-after-k6.prom` (454644 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-205511\pairs-450-rate-900\metrics-after-k6.prom`
- `metrics-after-20s.prom` (454649 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-205511\pairs-450-rate-900\metrics-after-20s.prom`
- `mysql-digest.txt` (3228 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-205511\pairs-450-rate-900\mysql-digest.txt`
- `mysql-slow-group.txt` (0 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-205511\pairs-450-rate-900\mysql-slow-group.txt`
- `mysql-waits.txt` (554 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-205511\pairs-450-rate-900\mysql-waits.txt`

### 用户对 500，速率 1000 msg/s

目录：`D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-205511\pairs-500-rate-1000`

- `summary.json` (252102 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-205511\pairs-500-rate-1000\summary.json`
- `k6.log` (18676 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-205511\pairs-500-rate-1000\k6.log`
- `k6.err.log` (0 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-205511\pairs-500-rate-1000\k6.err.log`
- `k6-exit-code.txt` (3 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-205511\pairs-500-rate-1000\k6-exit-code.txt`
- `queues-before.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-205511\pairs-500-rate-1000\queues-before.json`
- `queue-samples.jsonl` (27506 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-205511\pairs-500-rate-1000\queue-samples.jsonl`
- `queues-after-k6.json` (1569 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-205511\pairs-500-rate-1000\queues-after-k6.json`
- `queues-after-20s.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-205511\pairs-500-rate-1000\queues-after-20s.json`
- `metrics-before.prom` (454646 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-205511\pairs-500-rate-1000\metrics-before.prom`
- `metrics-after-k6.prom` (454618 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-205511\pairs-500-rate-1000\metrics-after-k6.prom`
- `metrics-after-20s.prom` (454625 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-205511\pairs-500-rate-1000\metrics-after-20s.prom`
- `mysql-digest.txt` (4223 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-205511\pairs-500-rate-1000\mysql-digest.txt`
- `mysql-slow-group.txt` (0 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-205511\pairs-500-rate-1000\mysql-slow-group.txt`
- `mysql-waits.txt` (506 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-205511\pairs-500-rate-1000\mysql-waits.txt`

### 用户对 600，速率 1200 msg/s

目录：`D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-205511\pairs-600-rate-1200`

- `summary.json` (301275 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-205511\pairs-600-rate-1200\summary.json`
- `k6.log` (20697 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-205511\pairs-600-rate-1200\k6.log`
- `k6.err.log` (0 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-205511\pairs-600-rate-1200\k6.err.log`
- `k6-exit-code.txt` (3 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-205511\pairs-600-rate-1200\k6-exit-code.txt`
- `queues-before.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-205511\pairs-600-rate-1200\queues-before.json`
- `queue-samples.jsonl` (30758 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-205511\pairs-600-rate-1200\queue-samples.jsonl`
- `queues-after-k6.json` (1578 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-205511\pairs-600-rate-1200\queues-after-k6.json`
- `queues-after-20s.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-205511\pairs-600-rate-1200\queues-after-20s.json`
- `metrics-before.prom` (454621 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-205511\pairs-600-rate-1200\metrics-before.prom`
- `metrics-after-k6.prom` (481257 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-205511\pairs-600-rate-1200\metrics-after-k6.prom`
- `metrics-after-20s.prom` (481276 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-205511\pairs-600-rate-1200\metrics-after-20s.prom`
- `mysql-digest.txt` (4283 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-205511\pairs-600-rate-1200\mysql-digest.txt`
- `mysql-slow-group.txt` (5788 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-205511\pairs-600-rate-1200\mysql-slow-group.txt`
- `mysql-waits.txt` (517 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-205511\pairs-600-rate-1200\mysql-waits.txt`

### 用户对 800，速率 1600 msg/s

目录：`D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-210228\pairs-800-rate-1600`

- `summary.json` (398828 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-210228\pairs-800-rate-1600\summary.json`
- `k6.log` (19320 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-210228\pairs-800-rate-1600\k6.log`
- `k6.err.log` (0 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-210228\pairs-800-rate-1600\k6.err.log`
- `k6-exit-code.txt` (3 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-210228\pairs-800-rate-1600\k6-exit-code.txt`
- `queues-before.json` (1561 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-210228\pairs-800-rate-1600\queues-before.json`
- `queue-samples.jsonl` (25881 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-210228\pairs-800-rate-1600\queue-samples.jsonl`
- `queues-after-k6.json` (1580 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-210228\pairs-800-rate-1600\queues-after-k6.json`
- `queues-after-20s.json` (1580 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-210228\pairs-800-rate-1600\queues-after-20s.json`
- `metrics-before.prom` (481255 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-210228\pairs-800-rate-1600\metrics-before.prom`
- `metrics-after-k6.prom` (481206 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-210228\pairs-800-rate-1600\metrics-after-k6.prom`
- `metrics-after-20s.prom` (481205 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-210228\pairs-800-rate-1600\metrics-after-20s.prom`
- `mysql-digest.txt` (4233 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-210228\pairs-800-rate-1600\mysql-digest.txt`
- `mysql-slow-group.txt` (5336 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-210228\pairs-800-rate-1600\mysql-slow-group.txt`
- `mysql-waits.txt` (622 bytes): `D:\biliibli\loadtest\results\runs\im-online-pairs-ramp\im-online-pairs-ramp-20260418-210228\pairs-800-rate-1600\mysql-waits.txt`


