# IM Load Test Run Record

记录时间：2026-04-18

本文档根据当前工作区中的 k6 结果文件整理，来源主要是：

- `loadtest/results/runs/**/summary.json`
- `loadtest/results/runs/**/k6.log`
- `loadtest/results/runs/**/metrics-*.prom`
- `loadtest/results/runs/**/queues-*.json`
- `loadtest/results/runs/**/mysql-*.txt`

说明：

- `accepted` 指客户端收到 `send_message_accepted`，这是 WebSocket 入口侧确认，不等价于所有 MQ 消费和 DB 投影完成。
- `k6观测速率` 是 k6 按整个测试 wall time 计算的 rate，包含 setup、等待 receiver、收尾等待等时间；它不一定等于脚本设定的目标发送速率。
- `参数` 中 `rate/vus/durationSeconds/perVuIntervalMs/messagesPerVu` 来自 `summary.json.setup_data`；没有写入 `setup_data` 的旧脚本参数根据 `k6.log` 和发送总量推断。
- k6 没有把所有启动环境变量完整写进 summary。本文中 `BASE_URL/WS_URL` 只列脚本默认值和已知运行上下文；如果当时 wrapper 命令覆盖了环境变量，结果文件本身无法完全反推。

## 脚本参数定义

### `im_ws_accepted.js`

目的：固定 VU 数，每个 VU 建立一个发送方 WebSocket，向自己的接收方连续发送固定条数消息，统计入口 accepted 延迟和成功率。

完整参数：

| 参数 | 默认值 | 含义 |
|---|---:|---|
| `BASE_URL` | `http://host.docker.internal:8080` | HTTP API 地址 |
| `WS_URL` | `BASE_URL` 替换为 `ws://...` | WebSocket 地址 |
| `VUS` | `10` | 并发 WebSocket 发送连接数 |
| `MESSAGES_PER_VU` | `100` | 每个 VU 发送消息数 |
| `ACCEPT_TIMEOUT_MS` | `15000` | 等待 `send_message_accepted` 超时时间 |
| `SEND_INTERVAL_MS` | `0` | 单 VU 内两条消息间隔；`0` 表示打开连接后批量发送 |
| `MAX_DURATION` | `10m` | k6 场景最大运行时间 |
| `TEST_PASSWORD` | `K6test123456` | 测试用户密码 |
| `SHARED_SENDER` | `false` | 是否多个 VU 共用同一个 sender |

### `im_ws_constant_rate.js`

目的：固定总目标发送速率，按 VU 均摊发送间隔，观察入口 accepted、RabbitMQ 队列积压、Prometheus/Micrometer 指标。

完整参数：

| 参数 | 默认值 | 含义 |
|---|---:|---|
| `BASE_URL` | `http://host.docker.internal:8080` | HTTP API 地址 |
| `WS_URL` | `BASE_URL` 替换为 `ws://...` | WebSocket 地址 |
| `RATE` | `500` | 目标消息发送速率，单位 msg/s |
| `VUS` | `50` | 并发 WebSocket 发送连接数 |
| `DURATION_SECONDS` | `30` | 发送窗口时长 |
| `ACCEPT_TIMEOUT_MS` | `10000` | 等待 accepted 超时时间 |
| `TEST_PASSWORD` | `K6test123456` | 测试用户密码 |
| `DEBUG_ERRORS` | `false` | 是否打印业务错误 |
| `DEBUG_ERROR_LIMIT` | `10` | 最多打印错误数 |
| `perVuIntervalMs` | `round(1000 * VUS / RATE)` | 脚本派生值，每个 VU 的发送间隔 |
| `messagesPerVu` | `floor(RATE * DURATION_SECONDS / VUS)` | 脚本派生值，每个 VU 发送条数 |
| `maxDurationSeconds` | `ceil(DURATION_SECONDS + ACCEPT_TIMEOUT_MS/1000 + 30)` | 脚本派生值，k6 最大运行时间 |

### `im_ws_online_pairs_constant_rate.js`

目的：更贴近真实私聊场景，建立 `PAIRS` 对 sender/receiver，每个 receiver 在线，每个 sender 按固定频率发送，统计入口 accepted 和接收端 `message_received`。

完整参数：

| 参数 | 默认值 | 含义 |
|---|---:|---|
| `BASE_URL` | `http://host.docker.internal:8080` | HTTP API 地址 |
| `WS_URL` | `BASE_URL` 替换为 `ws://...` | WebSocket 地址 |
| `PAIRS` | `300` | sender/receiver 对数 |
| `RATE` | `600` | 总目标发送速率，单位 msg/s |
| `DURATION_SECONDS` | `30` | 发送窗口时长 |
| `ACCEPT_TIMEOUT_MS` | `10000` | sender 等待 accepted 超时时间 |
| `RECEIVER_WARMUP_MS` | `5000` | receiver 先上线等待时间 |
| `TEST_PASSWORD` | `K6test123456` | 测试用户密码 |
| `DEBUG_ERRORS` | `false` | 是否打印业务错误 |
| `DEBUG_ERROR_LIMIT` | `10` | 最多打印错误数 |
| `totalVus` | `PAIRS * 2` | 脚本派生值，receiver VU + sender VU |
| `senderIntervalMs` | `round(1000 * PAIRS / RATE)` | 脚本派生值，每个 sender 的发送间隔 |
| `messagesPerSender` | `floor(RATE * DURATION_SECONDS / PAIRS)` | 脚本派生值，每个 sender 发送条数 |
| `maxDurationSeconds` | `ceil(RECEIVER_WARMUP_MS/1000 + DURATION_SECONDS + ACCEPT_TIMEOUT_MS/1000 + 30)` | 脚本派生值 |

## 全局运行条件

| 条件 | 记录 |
|---|---|
| 压测工具 | k6 |
| 服务依赖 | Docker MySQL、RabbitMQ、Redis、MinIO、Nginx |
| 4 月 18 日后半段测试应用形态 | 本地 Java 应用 wrapper + Docker 依赖服务；测试结束后 Docker app/nginx 已恢复 |
| MQ 观测 | RabbitMQ queue snapshot：`queues-before-k6.json`、`queues-after-k6.json`、`queues-after-20s.json`、部分包含 `queues-after-drain-wait.json` |
| 应用指标观测 | Prometheus/Micrometer scrape：`metrics-before-k6.prom`、`metrics-after-k6.prom`、`metrics-after-20s.prom` |
| MySQL 观测 | 后续 DB 分析打开过 slow query log、Performance Schema digest/table/index/wait 统计 |
| 测试数据 | k6 脚本每轮注册 sender/receiver 测试账号；大多数测试未清理业务表 |
| MySQL 系统性能表清理 | 仅 `im-online-300pairs-cleanperf-20260418-152245` 明确清理 Performance Schema summary 和 `mysql.slow_log` 后执行 |

## 1. Accepted Burst 基线测试

目的：验证 WebSocket 入口在突发批量发送下的 accepted 成功率和延迟，早期主要用于确认发送路径、异步确认、边界指标是否生效。

场景特点：

- 脚本：`im_ws_accepted.js`
- 发送方式：每个 VU 建立一个 sender WebSocket，向一个 receiver 发送固定条数。
- 旧结果未在 `setup_data` 中保存完整 env；`MESSAGES_PER_VU` 由 `sent / VUS` 推断。
- 这些测试更偏入口突发，不代表真实“很多用户低频发送”的业务模型。

| 结果目录 | 目的 | 参数 | sent | accepted | k6观测速率 | accepted延迟 | 备注 |
|---|---|---|---:|---:|---:|---|---|
| `loadtest/results/runs/im-ws/im-ws-accepted-100vu-20260416-201735` | 100 VU 突发基线 | `VUS=100`; `MESSAGES_PER_VU≈300`; `MAX_DURATION=10m`; `ACCEPT_TIMEOUT_MS=15000`; `SEND_INTERVAL_MS=0` | 30000 | 23758 | 486.0/s | avg 15713.4 ms / p95 28946.3 ms / max 30271 ms | accepted 未全成功，属于问题暴露 run |
| `loadtest/results/runs/im-ws/im-ws-accepted-100vu-isolated-20260416-202004` | 100 VU 隔离用户复测 | `VUS=100`; `MESSAGES_PER_VU≈300`; `MAX_DURATION=10m`; `ACCEPT_TIMEOUT_MS=15000`; `SEND_INTERVAL_MS=0` | 30000 | 30000 | 1252.7/s | avg 10087.6 ms / p95 19038.1 ms / max 21933 ms | 成功率恢复，但入口延迟高 |
| `loadtest/results/runs/im-ws/im-ws-accepted-50vu-isolated-20260416-202601` | 50 VU 隔离用户 | `VUS=50`; `MESSAGES_PER_VU≈300`; `MAX_DURATION=10m`; `ACCEPT_TIMEOUT_MS=15000`; `SEND_INTERVAL_MS=0` | 15000 | 15000 | 1299.7/s | avg 5303.5 ms / p95 10187 ms / max 10758 ms | 入口突发延迟仍高 |
| `loadtest/results/runs/im-ws/im-ws-accepted-20vu-isolated-20260416-203044` | 20 VU 隔离用户 | `VUS=20`; `MESSAGES_PER_VU≈300`; `MAX_DURATION=10m`; `ACCEPT_TIMEOUT_MS=15000`; `SEND_INTERVAL_MS=0` | 6000 | 6000 | 1243.3/s | avg 2368.1 ms / p95 4278 ms / max 4488 ms | 降低 VU 后延迟下降 |
| `loadtest/results/runs/im-ws/im-ws-metrics-smoke-20260416-205136` | Micrometer 指标 smoke | `VUS=2`; `MESSAGES_PER_VU≈1`; `MAX_DURATION=1m`; `ACCEPT_TIMEOUT_MS=15000` | 2 | 2 | 4.7/s | avg 82.5 ms / p95 83 ms / max 83 ms | 小流量验证 |
| `loadtest/results/runs/im-ws/im-ws-metrics-20vu-20260416-205401` | 20 VU + 指标采集 | `VUS=20`; `MESSAGES_PER_VU≈300`; `MAX_DURATION=10m`; `ACCEPT_TIMEOUT_MS=15000` | 6000 | 6000 | 990.2/s | avg 2891.7 ms / p95 5163 ms / max 5404 ms | 带 Prometheus 前后快照 |
| `loadtest/results/runs/im-ws/im-ws-metrics-50vu-20260416-205639` | 50 VU + 指标采集 | `VUS=50`; `MESSAGES_PER_VU≈300`; `MAX_DURATION=10m`; `ACCEPT_TIMEOUT_MS=15000` | 15000 | 15000 | 1230.7/s | avg 5606.3 ms / p95 10398 ms / max 11140 ms | 带 Prometheus 前后快照 |
| `loadtest/results/runs/im-ws/im-ws-metrics-50vu-sampled-20260416-205818` | 50 VU + 采样指标 | `VUS=50`; `MESSAGES_PER_VU≈300`; `MAX_DURATION=10m`; `ACCEPT_TIMEOUT_MS=15000` | 15000 | 15000 | 1234.4/s | avg 5646.4 ms / p95 10608 ms / max 11164 ms | 多了运行中采样 |
| `loadtest/results/runs/im-ws/im-ws-async-confirm-smoke-20260416-233248` | async confirm smoke | `VUS=2`; `MESSAGES_PER_VU≈3`; `MAX_DURATION=1m`; `ACCEPT_TIMEOUT_MS=15000` | 6 | 6 | 15.2/s | avg 91.3 ms / p95 110 ms / max 110 ms | 小流量验证 |
| `loadtest/results/runs/im-ws/im-ws-async-confirm-50vu-20260416-233335` | async confirm 50 VU | `VUS=50`; `MESSAGES_PER_VU≈300`; `MAX_DURATION=10m`; `ACCEPT_TIMEOUT_MS=15000` | 15000 | 15000 | 1383.2/s | avg 5829.6 ms / p95 8661 ms / max 9239 ms | 对比同步 confirm 行为 |
| `loadtest/results/runs/im-ws/im-ws-boundary-metrics-smoke-20260417-001627` | 边界指标 smoke | `VUS=2`; `MESSAGES_PER_VU≈3`; `MAX_DURATION=1m`; `ACCEPT_TIMEOUT_MS=15000` | 6 | 6 | 16.5/s | avg 70 ms / p95 83 ms / max 83 ms | 小流量验证 |
| `loadtest/results/runs/im-ws/im-ws-boundary-metrics-50vu-20260417-001711` | 边界指标 50 VU | `VUS=50`; `MESSAGES_PER_VU≈300`; `MAX_DURATION=10m`; `ACCEPT_TIMEOUT_MS=15000` | 15000 | 15000 | 1821.6/s | avg 3890.4 ms / p95 6686 ms / max 7011 ms | 入口边界指标对比 |

## 2. Constant Rate 梯度测试

目的：用固定目标速率找入口 accepted 能承受的大致区间，并观察从低速到高速时延迟和成功率变化。

场景特点：

- 脚本：`im_ws_constant_rate.js`
- 每个 VU 按 `perVuIntervalMs` 定时发送，不是一次性全量突发。
- `messagesPerVu=floor(RATE*DURATION_SECONDS/VUS)`。

| 结果目录 | 目的 | 参数 | sent | accepted | k6观测速率 | accepted延迟 | 备注 |
|---|---|---|---:|---:|---:|---|---|
| `loadtest/results/runs/im-ws/im-ws-rate-smoke-20260417-002515` | constant-rate smoke | `RATE=100`; `VUS=10`; `DURATION_SECONDS=5`; `perVuIntervalMs=100`; `messagesPerVu=50`; `ACCEPT_TIMEOUT_MS=10000` | 450 | 450 | 84.3/s | avg 4.1 ms / p95 5 ms / max 13 ms | 发送量少于理论 500，属于 smoke |
| `loadtest/results/runs/im-ws/im-ws-rate-smoke-20260417-002704` | constant-rate smoke 复测 | `RATE=100`; `VUS=10`; `DURATION_SECONDS=5`; `perVuIntervalMs=100`; `messagesPerVu=50`; `ACCEPT_TIMEOUT_MS=10000` | 500 | 500 | 93.8/s | avg 4 ms / p95 6 ms / max 10 ms | smoke 有效 |
| `loadtest/results/runs/im-ws/im-ws-rate-ladder-20260417-002737/rate-500` | 500 msg/s 梯度 | `RATE=500`; `VUS=50`; `DURATION_SECONDS=30`; `perVuIntervalMs=100`; `messagesPerVu=300`; `ACCEPT_TIMEOUT_MS=10000` | 15000 | 15000 | 439.1/s | avg 4.3 ms / p95 5 ms / max 2364 ms | 队列也有采样 |
| `loadtest/results/runs/im-ws/im-ws-rate-ladder-20260417-002737/rate-1000` | 1000 msg/s 梯度 | `RATE=1000`; `VUS=50`; `DURATION_SECONDS=30`; `perVuIntervalMs=50`; `messagesPerVu=600`; `ACCEPT_TIMEOUT_MS=10000` | 30000 | 30000 | 939.4/s | avg 5 ms / p95 5 ms / max 898 ms | 入口 accepted 正常 |
| `loadtest/results/runs/im-ws/im-ws-rate-ladder-20260417-002737/rate-1500` | 1500 msg/s 梯度 | `RATE=1500`; `VUS=50`; `DURATION_SECONDS=30`; `perVuIntervalMs=33`; `messagesPerVu=900`; `ACCEPT_TIMEOUT_MS=10000` | 45000 | 44603 | 737.4/s | avg 5 ms / p95 6 ms / max 2324 ms | accepted 少量缺失 |
| `loadtest/results/runs/im-ws/im-ws-rate-ladder-20260417-002737/rate-2000` | 2000 msg/s 梯度 | `RATE=2000`; `VUS=50`; `DURATION_SECONDS=30`; `perVuIntervalMs=25`; `messagesPerVu=1200`; `ACCEPT_TIMEOUT_MS=10000` | 60000 | 59624 | 981.8/s | avg 5.4 ms / p95 9 ms / max 1340 ms | accepted 少量缺失 |
| `loadtest/results/runs/im-ws/im-ws-rate-ladder-20260417-002737/rate-2500` | 2500 msg/s 梯度 | `RATE=2500`; `VUS=50`; `DURATION_SECONDS=30`; `perVuIntervalMs=20`; `messagesPerVu=1500`; `ACCEPT_TIMEOUT_MS=10000` | 75000 | 75000 | 2269.4/s | avg 16.7 ms / p95 58 ms / max 1328 ms | 结果较好，但需结合 MQ 积压看 |
| `loadtest/results/runs/im-ws/im-ws-rate-debug-1500-20260417-003526` | 1500 msg/s debug | `RATE=1500`; `VUS=50`; `DURATION_SECONDS=30`; `perVuIntervalMs=33`; `messagesPerVu=900`; `ACCEPT_TIMEOUT_MS=10000` | 45000 | 45000 | 1368.6/s | avg 6.4 ms / p95 8 ms / max 1339 ms | 调试复测 |
| `loadtest/results/runs/im-ws/im-ws-rate-confirm-20260417-003623/rate-2000` | 2000 msg/s 确认 | `RATE=2000`; `VUS=50`; `DURATION_SECONDS=30`; `perVuIntervalMs=25`; `messagesPerVu=1200`; `ACCEPT_TIMEOUT_MS=10000` | 60000 | 60000 | 1814.8/s | avg 8.8 ms / p95 20 ms / max 1343 ms | 入口 accepted 成功 |
| `loadtest/results/runs/im-ws/im-ws-rate-confirm-20260417-003623/rate-3000` | 3000 msg/s 确认 | `RATE=3000`; `VUS=50`; `DURATION_SECONDS=30`; `perVuIntervalMs=17`; `messagesPerVu=1800`; `ACCEPT_TIMEOUT_MS=10000` | 90000 | 90000 | 2422.4/s | avg 1877.9 ms / p95 3809 ms / max 4456 ms | 成功但延迟明显上升 |
| `loadtest/results/runs/im-ws/im-ws-rate-confirm-20260417-003623/rate-3500` | 3500 msg/s 确认 | `RATE=3500`; `VUS=50`; `DURATION_SECONDS=30`; `perVuIntervalMs=14`; `messagesPerVu=2100`; `ACCEPT_TIMEOUT_MS=10000` | 105000 | 82645 | 1716.6/s | avg 5250.4 ms / p95 9680 ms / max 11225 ms | 超过入口稳定区间 |

## 3. Full Chain 梯度测试

目的：在开启完整链路观测时找更保守的稳定速率，结合 RabbitMQ 队列和 Prometheus 快照看后半链路。

场景特点：

- 脚本：`im_ws_constant_rate.js`
- 主要速率范围：100、200、300、350、375、400、450 msg/s。
- 每个 run 同时保存 Prometheus 和 RabbitMQ 快照。

| 结果目录 | 参数 | sent | accepted | k6观测速率 | accepted延迟 | 备注 |
|---|---|---:|---:|---:|---|---|
| `loadtest/results/runs/im-ws/im-ws-fullchain-ladder-20260417-004144/rate-100` | `RATE=100`; `VUS=20`; `DURATION_SECONDS=30`; `perVuIntervalMs=200`; `messagesPerVu=150` | 3000 | 3000 | 91.8/s | avg 3.6 ms / p95 5 ms / max 10 ms | 低速基线 |
| `loadtest/results/runs/im-ws/im-ws-fullchain-ladder-20260417-004144/rate-200` | `RATE=200`; `VUS=20`; `DURATION_SECONDS=30`; `perVuIntervalMs=100`; `messagesPerVu=300` | 6000 | 6000 | 178.8/s | avg 3.3 ms / p95 4 ms / max 21 ms | 稳定 |
| `loadtest/results/runs/im-ws/im-ws-fullchain-ladder-20260417-004144/rate-300` | `RATE=300`; `VUS=20`; `DURATION_SECONDS=30`; `perVuIntervalMs=67`; `messagesPerVu=450` | 9000 | 9000 | 273.5/s | avg 3.6 ms / p95 4 ms / max 1342 ms | 有偶发 max |
| `loadtest/results/runs/im-ws/im-ws-fullchain-ladder-20260417-004144/rate-400` | `RATE=400`; `VUS=20`; `DURATION_SECONDS=30`; `perVuIntervalMs=50`; `messagesPerVu=600` | 12000 | 12000 | 369.9/s | avg 3.9 ms / p95 5 ms / max 1339 ms | 入口正常 |
| `loadtest/results/runs/im-ws/im-ws-fullchain-refine-20260417-004603/rate-350` | `RATE=350`; `VUS=20`; `DURATION_SECONDS=30`; `perVuIntervalMs=57`; `messagesPerVu=525` | 10500 | 10500 | 323.5/s | avg 3.8 ms / p95 5 ms / max 1349 ms | 精细化区间 |
| `loadtest/results/runs/im-ws/im-ws-fullchain-refine-20260417-004603/rate-375` | `RATE=375`; `VUS=20`; `DURATION_SECONDS=30`; `perVuIntervalMs=53`; `messagesPerVu=562` | 11240 | 11240 | 347.8/s | avg 3.7 ms / p95 5 ms / max 1349 ms | 精细化区间 |
| `loadtest/results/runs/im-ws/im-ws-fullchain-final-20260417-004825/rate-400` | `RATE=400`; `VUS=20`; `DURATION_SECONDS=30`; `perVuIntervalMs=50`; `messagesPerVu=600` | 12000 | 12000 | 369.1/s | avg 3.6 ms / p95 4 ms / max 1336 ms | final 对照 |
| `loadtest/results/runs/im-ws/im-ws-fullchain-final-20260417-004825/rate-450` | `RATE=450`; `VUS=20`; `DURATION_SECONDS=30`; `perVuIntervalMs=44`; `messagesPerVu=675` | 13500 | 13500 | 419.1/s | avg 3.6 ms / p95 4 ms / max 1348 ms | final 高一点速率 |
| `loadtest/results/runs/im-ws/im-ws-fullchain-375-clean-20260417-005044` | `RATE=375`; `VUS=20`; `DURATION_SECONDS=30`; `perVuIntervalMs=53`; `messagesPerVu=562` | 11240 | 11240 | 347.8/s | avg 3.9 ms / p95 4 ms / max 1354 ms | clean 复测 |

## 4. Producer Confirm 同步/异步对比

目的：比较 RabbitMQ publisher confirm 同步等待和异步确认对入口 accepted 的影响。

场景特点：

- `sync-confirm`：发送端等待 broker confirm 的版本。
- `async-confirm`：发送端异步 confirm 的版本。
- 两类场景都包含突发 `im_ws_accepted.js` 和 constant-rate `im_ws_constant_rate.js`。

| 结果目录 | 版本 | 参数 | sent | accepted | k6观测速率 | accepted延迟 | 备注 |
|---|---|---|---:|---:|---:|---|---|
| `loadtest/results/runs/compare/confirm-worktree-compare-20260417-134044/sync-confirm/burst-50vu-15000` | sync confirm | `im_ws_accepted`; `VUS=50`; `MESSAGES_PER_VU≈300`; `MAX_DURATION=10m` | 15000 | 15000 | 1131.0/s | avg 6420 ms / p95 11318 ms / max 11861 ms | 突发入口延迟高 |
| `loadtest/results/runs/compare/confirm-worktree-compare-20260417-134044/async-confirm/burst-50vu-15000` | async confirm | `im_ws_accepted`; `VUS=50`; `MESSAGES_PER_VU≈300`; `MAX_DURATION=10m` | 15000 | 15000 | 1733.3/s | avg 4074.4 ms / p95 7028 ms / max 7335 ms | 异步 confirm 明显改善 |
| `loadtest/results/runs/compare/confirm-worktree-compare-20260417-134044/sync-confirm/rate-2000-50vu-30s` | sync confirm | `RATE=2000`; `VUS=50`; `DURATION_SECONDS=30`; `perVuIntervalMs=25`; `messagesPerVu=1200` | 60000 | 47738 | 983.0/s | avg 5145 ms / p95 9764.2 ms / max 10497 ms | accepted 缺失明显 |
| `loadtest/results/runs/compare/confirm-worktree-compare-20260417-134044/async-confirm/rate-2000-50vu-30s` | async confirm | `RATE=2000`; `VUS=50`; `DURATION_SECONDS=30`; `perVuIntervalMs=25`; `messagesPerVu=1200` | 60000 | 59817 | 977.2/s | avg 6 ms / p95 12 ms / max 77 ms | 入口确认改善明显 |

## 5. MQ Listener 串行/并行配置对比

目的：比较所有消费者基本串行消费和按队列配置并发/prefetch 后，对入口 accepted、MQ 积压、消费者处理速度的影响。

当前并行配置默认值：

| 消费类别 | concurrency | max-concurrency | prefetch |
|---|---:|---:|---:|
| `persist` | 2 | 4 | 20 |
| `conversation` | 2 | 6 | 20 |
| `redis-projection` | 4 | 8 | 100 |
| `realtime-push` | 2 | 4 | 50 |
| `group-persist` | 1 | 2 | 10 |
| `group-realtime-push` | 2 | 6 | 50 |

串行对照含义：当时用于对比的消费者并发/预取基本按 1 个消费者、低 prefetch 运行，用来模拟未拆分调优前的队列处理方式。

共同 k6 参数：

- 脚本：`im_ws_constant_rate.js`
- `RATE=600`
- `VUS=30`
- `DURATION_SECONDS=30`
- `perVuIntervalMs=50`
- `messagesPerVu=600`
- 理论发送量：18000
- `ACCEPT_TIMEOUT_MS=10000`

| 结果目录 | 配置 | sent | accepted | k6观测速率 | accepted延迟 | 监控条件 | 备注 |
|---|---|---:|---:|---:|---|---|---|
| `loadtest/results/runs/im-mq/im-mq-listener-ab-20260417-215509/serial` | 串行 | 18000 | 18000 | 548.6/s | avg 178.3 ms / p95 319 ms / max 947 ms | RabbitMQ after-k6/after-20s | 第一轮 A/B |
| `loadtest/results/runs/im-mq/im-mq-listener-ab-20260417-215509/parallel` | 并行 | 18000 | 18000 | 549.3/s | avg 267.4 ms / p95 557 ms / max 1545 ms | RabbitMQ after-k6/after-20s | 第一轮 A/B |
| `loadtest/results/runs/im-mq/im-mq-listener-ab-rerun-20260417-220641/serial` | 串行 | 18000 | 18000 | 556.9/s | avg 34.6 ms / p95 100 ms / max 721 ms | RabbitMQ before/after/after-20s | 复测中串行表现较好 |
| `loadtest/results/runs/im-mq/im-mq-listener-ab-rerun-20260417-220641/parallel` | 并行 | 18000 | 18000 | 438.9/s | avg 3850.6 ms / p95 7982 ms / max 8582 ms | RabbitMQ before/after/after-20s/drain-wait | 并行后入口明显变慢，后续判断与 DB/连接池/日志刷盘竞争有关 |
| `loadtest/results/runs/im-mq/im-mq-metrics-ab-20260417-231753/serial` | 串行 + 消费者 Micrometer | 18000 | 18000 | 555.8/s | avg 19.5 ms / p95 35 ms / max 774 ms | RabbitMQ + Prometheus consumer metrics | 加入统一 `ImMqConsumerMetrics` 后复测 |
| `loadtest/results/runs/im-mq/im-mq-metrics-ab-20260417-231753/parallel` | 并行 + 消费者 Micrometer | 18000 | 18000 | 444.3/s | avg 3978.5 ms / p95 7289 ms / max 7842 ms | RabbitMQ + Prometheus consumer metrics | 并行仍慢，后续转向 DB 操作拆分埋点 |

## 6. DB 操作拆分埋点测试

目的：定位 `single_message_persist` 消费者中哪个 DB 操作拖慢速度。

新增观测：

- `chat_message_insert`
- `chat_message_duplicate_lookup`
- `contact_relation_upsert`
- MySQL slow query log 临时打开，`long_query_time=0.001s`
- Performance Schema digest / lock waits
- Prometheus 应用指标
- RabbitMQ 队列快照

| 结果目录 | 参数 | sent | accepted | k6观测速率 | accepted延迟 | 关键观测 | 备注 |
|---|---|---:|---:|---:|---|---|---|
| `loadtest/results/runs/im-db-breakdown/im-db-breakdown-20260418-135700` | `im_ws_constant_rate`; `RATE=600`; `VUS=30`; `DURATION_SECONDS=20`; `perVuIntervalMs=50`; `messagesPerVu=400`; `ACCEPT_TIMEOUT_MS=10000` | 12000 | 12000 | 493.0/s | avg 400.2 ms / p95 1268 ms / max 3313 ms | `chat_message_insert` avg 约 1.37 ms；`contact_relation_upsert` avg 约 2.86 ms；MySQL digest 显示 contact upsert 写等待更重 | 证明瓶颈偏向 `contact_relation_upsert` 和 MySQL 写/锁/日志等待 |

## 7. 300 Sender + 300 Receiver 在线私聊测试

目的：修正早期“单用户或少量用户高频发送”的不真实模型，改为 300 个 sender、300 个 receiver，每个 sender 2 msg/s，总目标 600 msg/s。

共同参数：

- 脚本：`im_ws_online_pairs_constant_rate.js`
- `PAIRS=300`
- `RATE=600`
- `DURATION_SECONDS=30`
- `RECEIVER_WARMUP_MS=5000`
- `ACCEPT_TIMEOUT_MS=10000`
- `totalVus=600`
- `senderIntervalMs=500`
- `messagesPerSender=60`
- 理论发送量：18000

权限检测条件：

- 每轮注册新用户。
- 新用户默认 `private_message_policy=ALLOW_ALL`，所以不会因为“接收者未回复”被拦截。
- 测试中会走 `validateCanSendMessage`、用户状态、黑名单、联系人、隐私设置等权限检查。

| 结果目录 | 目的 | sent | accepted | receiver received | sender received | k6观测速率 | accepted延迟 | MQ 积压观测 | 备注 |
|---|---|---:|---:|---:|---:|---:|---|---|---|
| `loadtest/results/runs/im-online/im-online-300pairs-20260418-145626` | 在线场景首次尝试 | 0 | 0 | 0 | 0 | - | 0 ms | 无有效业务数据 | checks `0/2`，属于无效 run |
| `loadtest/results/runs/im-online/im-online-300pairs-20260418-145907` | 在线场景有效 run | 18000 | 18000 | 18000 | 17779 | 285.8/s | avg 2053.2 ms / p95 5361 ms / max 7183 ms | after-k6 有 DB 队列积压；after-20s 仍有部分积压 | 环境抖动较明显，sender received 少量未收到 |
| `loadtest/results/runs/im-online/im-online-300pairs-cleanperf-20260418-152245` | 清理 MySQL 性能表后复测 | 18000 | 18000 | 18000 | 18000 | 291.7/s | avg 114.2 ms / p95 227 ms / max 1945 ms | after-k6 persist/conversation 有积压；after-20s 和 drain-wait 清零 | 当前最干净、最适合分析的在线场景结果 |

清理条件：

- 清理 `mysql.slow_log`
- 清理 Performance Schema statement/wait/stage/table/index/file/socket summary 表
- 开启 slow log 到 TABLE，`long_query_time=0.001`
- 测试后恢复 `slow_query_log=OFF`，`long_query_time=10`

关键 DB 结果：

| 指标 | 结果 |
|---|---:|
| `chat_message_insert` 应用侧 count | 18000 |
| `chat_message_insert` 应用侧 avg | 约 1.286 ms |
| `contact_relation_upsert` 应用侧 count | 18000 |
| `contact_relation_upsert` 应用侧 avg | 约 2.663 ms |
| MySQL digest `contact_relation_upsert` total | 约 27.52 s |
| MySQL digest `contact_relation_upsert` avg | 约 1.53 ms |
| MySQL digest `chat_message insert` total | 约 2.60 s |
| MySQL digest `chat_message insert` avg | 约 0.144 ms |
| slow log 中 `contact_relation_upsert` 慢查询数 | 约 9607 |
| slow log 中 `contact_relation_upsert` avg | 约 2.454 ms |
| Performance Schema `contact_relation` table write wait | 约 26.90 s |
| `wait/io/file/innodb/innodb_log_file` | 约 40.18 s |
| `wait/io/file/sql/binlog` | 约 31.77 s |

## 8. 结果解释边界

这些 run 不能直接互相横向比较的原因：

- 早期 `im_ws_accepted.js` 是突发模型，后期 `im_ws_constant_rate.js` 是定速模型，`online_pairs` 是在线收发双方模型。
- 有些 run 是 Docker app，有些 run 是本地 Java wrapper + Docker 依赖。
- 部分 run 只看入口 accepted，后续才加入 RabbitMQ、Prometheus、MySQL slow log、Performance Schema。
- 业务表多数没有清空，只有 MySQL 系统性能表在 cleanperf run 前明确重置。
- k6 的 rate 包含 setup、等待、收尾，所以不能直接当作业务发送窗口内真实吞吐。

当前最适合继续分析的基准 run：

1. `loadtest/results/runs/im-online/im-online-300pairs-cleanperf-20260418-152245`
2. `loadtest/results/runs/im-db-breakdown/im-db-breakdown-20260418-135700`
3. `loadtest/results/runs/im-mq/im-mq-metrics-ab-20260417-231753/serial`
4. `loadtest/results/runs/im-mq/im-mq-metrics-ab-20260417-231753/parallel`

后续如果要做严格 A/B，建议固定：

- 同一个应用部署方式。
- 同一份业务数据初始化脚本。
- 每轮压测前清理 RabbitMQ 队列、Redis 测试 key、MySQL 业务测试数据。
- 每轮压测前重置 MySQL Performance Schema summary 和 `mysql.slow_log`。
- 统一记录 k6 启动命令和所有 env。
- 每轮记录 RabbitMQ before/after/after-drain、Prometheus before/after/after-drain、MySQL digest/slow/table/index/wait。


