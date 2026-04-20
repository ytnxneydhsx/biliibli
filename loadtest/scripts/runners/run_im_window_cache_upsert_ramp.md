# IM 窗口缓存与 upsert 阶梯压测脚本

对应脚本：`run_im_window_cache_upsert_ramp.ps1`

## 用途

用于验证单聊消息链路在以下优化后的吞吐、积压和耗时变化：

- `chat_conversation` 发送方窗口摘要使用 `INSERT ... ON DUPLICATE KEY UPDATE`。
- `chat_conversation` 接收方窗口摘要使用单条 upsert，同时完成未读数自增和摘要版本判断。
- Redis 会话窗口缓存已初始化时，`im.message.conversation.redis.queue` 消费者会执行 Lua 投影。

这个脚本和普通 online-pairs 压测的关键区别是：runner 会直接向 Redis 写入 `im:conv:init:*` 预热标记，使后续消息真正进入 Redis 窗口投影逻辑，而不是因为未初始化直接跳过。默认不会调用 `/me/im/conversations`，避免把窗口列表 HTTP 查询压力混入消息链路压测。

## 运行方式

```powershell
powershell -ExecutionPolicy Bypass -File loadtest/scripts/runners/run_im_window_cache_upsert_ramp.ps1
```

常用缩小版：

```powershell
powershell -ExecutionPolicy Bypass -File loadtest/scripts/runners/run_im_window_cache_upsert_ramp.ps1 `
  -PairsList 50,100 `
  -DurationSeconds 30
```

高压版示例：

```powershell
powershell -ExecutionPolicy Bypass -File loadtest/scripts/runners/run_im_window_cache_upsert_ramp.ps1 `
  -PairsList 200,400,600,800,1000 `
  -DurationSeconds 60 `
  -DrainWaitSeconds 30
```

## 主要参数

| 参数 | 含义 | 默认值 |
|---|---|---|
| `PairsList` | 用户对挡位；每对包含 1 个 sender 和 1 个 receiver | `100,200,300,400,600,800` |
| `DurationSeconds` | 每个挡位持续发送时间 | `60` |
| `ReceiverWarmupMs` | receiver WebSocket 预热时间 | `5000` |
| `AcceptTimeoutMs` | sender 等待 accepted 回执的超时 | `10000` |
| `SampleIntervalSeconds` | RabbitMQ 队列采样间隔 | `5` |
| `DrainWaitSeconds` | k6 结束后等待消费者追平的时间 | `20` |
| `BaseUrl` | 被压服务地址 | `http://host.docker.internal:8080` |
| `PrewarmInitKeys` | 是否直接写 Redis `im:conv:init:*` 预热标记 | `true` |
| `RunRoot` | 结果根目录；为空时自动生成到 `results/runs/im-window-cache-upsert-ramp/` | 空 |

## 每个挡位采集的 artifacts

- k6：`summary.json`、`k6.log`、`k6.err.log`、`k6-exit-code.txt`
- RabbitMQ：`queues-before.json`、`queue-samples.jsonl`、`queues-after-k6.json`、`queues-after-20s.json`
- Prometheus：`metrics-before.prom`、`metrics-after-k6.prom`、`metrics-after-20s.prom`
- MySQL：`mysql-digest.txt`、`mysql-conversation-digest.txt`、`mysql-slow-group.txt`、`mysql-waits.txt`
- Redis：`redis-before-*`、`redis-after-k6-*`、`redis-after-drain-*`

## 结果分析

可以复用现有报告生成器：

```powershell
$root = Get-Content loadtest/results/latest/latest_im_window_cache_upsert_ramp_run.txt
powershell -ExecutionPolicy Bypass -File loadtest/scripts/reports/generate_im_ramp_report.ps1 `
  -Root $root `
  -OutputPath loadtest/results/reports/im-window-cache-upsert-ramp-analysis-$(Get-Date -Format yyyyMMdd).md
```

分析重点：

- `im.message.conversation.queue` 是否还先积压；如果仍积压，说明 DB 窗口 upsert 仍是瓶颈。
- `im.message.conversation.redis.queue` 是否开始积压；如果它变成第一积压队列，说明 Redis Lua / 序列化 / 连接池成为新瓶颈。
- `mysql-conversation-digest.txt` 中 `chat_conversation` upsert 的 `COUNT_STAR`、`avg_ms`、`total_s` 是否低于优化前。
- `redis-after-k6-commandstats.txt` 中 `cmdstat_evalsha`、`cmdstat_hget`、`cmdstat_zadd`、`cmdstat_sadd` 的调用次数和耗时是否随消息数线性增长。
