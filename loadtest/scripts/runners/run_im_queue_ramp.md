# IM 队列阶梯压测脚本

对应脚本：`run_im_queue_ramp.ps1`

## 用途

按用户对数量分档运行 IM 在线用户对压测，并在每个挡位采集 k6、RabbitMQ、Prometheus 和 MySQL Performance Schema / slow log 产物。

## 主要参数

| 参数 | 含义 | 默认值 |
|---|---|---|
| `PairsList` | 用户对列表；每对包含 1 个 sender 和 1 个 receiver | `50,100,150,200,250,300,350,400` |
| `DurationSeconds` | 每个挡位持续发送时间 | `60` |
| `ReceiverWarmupMs` | receiver WebSocket 预热时间 | `5000` |
| `AcceptTimeoutMs` | sender 等待 accepted 回执的超时时间 | `10000` |
| `SampleIntervalSeconds` | RabbitMQ 队列采样间隔 | `5` |
| `BaseUrl` | 被压服务地址 | `http://host.docker.internal:8080` |
| `RunRoot` | 本次结果根目录；为空时自动生成到 `results/runs/im-online-pairs-ramp/` | 空 |

## 每个挡位采集的产物

- k6 summary、stdout/stderr 和 exit code
- RabbitMQ 队列压测前、压测中、k6 结束后、等待 drain 后的快照
- 应用 Prometheus 指标压测前、k6 结束后、等待 drain 后的快照
- MySQL digest、wait events、slow log 分组统计

## 结果口径

- 目标速率 = `用户对数量 * 2 msg/s`
- 应用 Prometheus counter/timer 使用每个挡位的 `after - before` 差值
- RabbitMQ `consumers` 是采样值，应按最小、最大、最后值解释，不应当作固定配置线程数
- `Accepted` 只表示发送方收到接入层 accepted 回执，不等于 MQ、DB、Redis 和实时推送都已完成
