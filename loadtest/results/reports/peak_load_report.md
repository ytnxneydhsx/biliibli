# IM WebSocket 峰值压测与 MQ Confirm 优化对比报告

报告更新时间: 2026-04-17 14:06:00 +08:00

当前代码 Git 版本:

- 分支: `codex/im-backend-load-test-fixes`
- HEAD: `008ec168b6819c0725fef3b3efc5d3e95c1f7234`
- HEAD 摘要: `008ec16 2026-04-17T00:18:59+08:00 feat add im websocket boundary metrics`
- 工作区状态: 后端已提交代码无未提交修改；存在未跟踪的本地压测脚本、压测结果和日志目录。

本报告涉及的关键后端版本:

- 同步 confirm 基线: `61d8a07 feat add im send path metrics`
- 异步 confirm 优化: `382ed72 feat make im mq confirms asynchronous`
- 当前额外边界埋点: `008ec16 feat add im websocket boundary metrics`

## 1. 测试目标

本阶段测试围绕 IM 私聊 `send_message` 入口展开，核心问题是:

```text
RabbitMQ publisher confirm 是否是入口 accepted 链路的主要耗时来源？
把同步等待 confirm 改为异步 confirm 后，入口 accepted 性能是否明显改善？
```

这里的 `send_message_accepted` 表示服务端已经接收 WebSocket 消息并完成入口处理，不代表后续 MQ 消费者已经完成落库、会话窗口更新、缓存更新和实时推送。

## 2. 测试环境

- 本机 Docker Compose 环境。
- 入口: `http://localhost:8080`，经 `bilibili-nginx` 转发到 `bilibili-app`。
- 依赖组件: MySQL、RabbitMQ、Redis、MinIO。
- 压测工具: K6 `grafana/k6:0.49.0`。
- 主要脚本: `loadtest/scripts/scenarios/im_ws_accepted.js`。

## 3. 测试口径

后端在 `send_message_accepted` 中返回 `clientMessageId` 后，K6 使用该字段做精确匹配:

```text
发送 send_message(clientMessageId = X)
等待 type == send_message_accepted 且 data.clientMessageId == X
记录当前消息 accepted 延迟
```

因此本报告中的 accepted 延迟是“当前消息发送后，到收到它自己的 accepted 响应”的耗时。

## 4. 峰值压测基线

同步 confirm 基线阶段的代表结果目录:

- `loadtest/results/runs/im-ws/im-ws-accepted-20vu-isolated-20260416-203044`
- `loadtest/results/runs/im-ws/im-ws-accepted-50vu-isolated-20260416-202601`
- `loadtest/results/runs/im-ws/im-ws-accepted-100vu-isolated-20260416-202004`
- `loadtest/results/runs/im-ws/im-ws-metrics-50vu-sampled-20260416-205818`

| 场景 | 发送数 | accepted 数 | 成功率 | accepted 吞吐 | Avg | Median | P90 | P95 | Max |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| 20 VU isolated | 6000 | 6000 | 100% | 1243.3/s | 2368.06ms | 2453.5ms | 4078.2ms | 4278ms | 4488ms |
| 50 VU isolated | 15000 | 15000 | 100% | 1299.7/s | 5303.53ms | 5250.5ms | 9651ms | 10187.05ms | 10758ms |
| 100 VU isolated | 30000 | 30000 | 100% | 1252.7/s | 10087.65ms | 10033.5ms | 17981ms | 19038.1ms | 21933ms |
| 50 VU sampled | 15000 | 15000 | 100% | 1234.4/s | 5646.39ms | 5642ms | 10095.1ms | 10608.05ms | 11164ms |

### 4.1 基线现象

1. 入口 accepted 峰值吞吐约在 `1200-1300 msg/s`。
2. VU 从 50 增加到 100 后，吞吐没有继续线性提升。
3. accepted 成功率仍然是 100%，但 P95 从约 10 秒上升到约 19 秒。
4. 这说明入口并非直接拒绝请求，而是出现了明显排队。

## 5. 同步 Confirm 服务端埋点

同步 confirm 基线取 `im-ws-metrics-50vu-sampled-20260416-205818`。

客户端结果:

| 指标 | 数值 |
|---|---:|
| sent | 15000 |
| accepted | 15000 |
| accepted 成功率 | 100% |
| accepted 吞吐 | 1234.4/s |
| accepted Avg | 5646.39ms |
| accepted P95 | 10608.05ms |
| accepted Max | 11164ms |

服务端 Micrometer 指标:

| 指标 | Avg | P95 | 含义 |
|---|---:|---:|---|
| `im.send.accept.total` | 6.714ms | 9.415ms | `acceptMessage` 主体耗时 |
| `im.send.accept.validation` | 0.586ms | 0.963ms | 入口校验 |
| `im.send.accept.conversation` | 1.305ms | 1.716ms | 会话相关处理 |
| `im.send.accept.location` | 0.257ms | 0.953ms | 位置相关处理 |
| `im.send.accept.publish` | 4.555ms | 6.951ms | MQ publish 调用整体 |
| `im.mq.publish.send` | 0.109ms | 0.951ms | RabbitTemplate 实际发送 |
| `im.mq.publish.confirm` | 4.437ms | 6.872ms | 等 RabbitMQ publisher confirm |

Hikari 连接池采样:

- active 最大值: 10
- pending 最大值: 42
- acquire 最大值: 约 131ms
- usage 最大值: 约 87ms

### 5.1 同步 Confirm 结论

`im.send.accept.publish` P95 为 `6.951ms`，其中 `im.mq.publish.confirm` P95 为 `6.872ms`。这说明同步版本中，MQ publish 阶段的大部分时间不是花在发送动作本身，而是花在等待 RabbitMQ confirm。

同时，`im.send.accept.total` P95 只有 `9.415ms`，远低于客户端看到的 `10s` 级 accepted P95。这个差异说明客户端长延迟主要来自高并发突发后的排队，而不是单次业务代码真的执行了 10 秒。

## 6. 异步 Confirm 优化

异步 confirm 版本对应提交:

```text
382ed72 feat make im mq confirms asynchronous
```

优化前:

```text
publish -> 等 RabbitMQ confirm -> 返回 send_message_accepted
```

优化后:

```text
publish -> 注册 confirm callback -> 立即返回 send_message_accepted
RabbitMQ confirm 在回调中异步处理
```

该方案是纯内存异步 confirm，不是 outbox。它可以减少入口等待，但进程在 confirm 前崩溃时仍有消息可靠性风险。

异步 confirm 代表结果目录:

- `loadtest/results/runs/im-ws/im-ws-async-confirm-50vu-20260416-233335`

客户端结果:

| 指标 | 数值 |
|---|---:|
| sent | 15000 |
| accepted | 15000 |
| accepted 成功率 | 100% |
| accepted 吞吐 | 约 1383/s |
| accepted P95 | 8661ms |
| confirm ACK | 15000 |
| confirm NACK | 0 |
| confirm timeout | 0 |
| confirm retry | 0 |
| confirm giveup | 0 |

服务端 Micrometer 指标:

| 指标 | Avg | P95 | 含义 |
|---|---:|---:|---|
| `im.send.accept.total` | 3.723ms | 6.969ms | 入口主体耗时 |
| `im.send.accept.validation` | 0.823ms | 1.633ms | 入口校验 |
| `im.send.accept.conversation` | 1.882ms | 3.318ms | 会话相关处理 |
| `im.send.accept.location` | 0.626ms | 1.784ms | 位置相关处理 |
| `im.send.accept.publish` | 0.375ms | 1.366ms | 入口内 MQ publish 调用 |
| `im.mq.publish.send` | 0.360ms | 1.329ms | RabbitTemplate 实际发送 |
| `im.mq.publish.confirm` | 8.071ms | 14.749ms | 异步 callback 中观察到的 confirm 延迟 |

## 7. 同步与异步对比

| 指标 | 同步 confirm | 异步 confirm | 变化 |
|---|---:|---:|---:|
| accepted 发送数 | 15000 | 15000 | 持平 |
| accepted 成功率 | 100% | 100% | 持平 |
| accepted 吞吐 | 1234.4/s | 约 1383/s | 小幅提升 |
| 客户端 accepted P95 | 10608.05ms | 8661ms | 下降约 18.4% |
| `im.send.accept.total` Avg | 6.714ms | 3.723ms | 下降约 44.5% |
| `im.send.accept.total` P95 | 9.415ms | 6.969ms | 下降约 26.0% |
| `im.send.accept.publish` Avg | 4.555ms | 0.375ms | 下降约 91.8% |
| `im.send.accept.publish` P95 | 6.951ms | 1.366ms | 下降约 80.3% |
| `im.mq.publish.confirm` P95 | 6.872ms | 14.749ms | 不再阻塞入口 |

### 7.1 对比结论

异步 confirm 对入口业务耗时改善明显，尤其是 `im.send.accept.publish`:

```text
同步 P95: 6.951ms
异步 P95: 1.366ms
下降约 80.3%
```

这验证了最开始的判断: 同步等待 RabbitMQ confirm 确实是入口方法内的重要耗时来源。

但吞吐没有按业务耗时下降比例线性提升。原因是峰值压测下还有其他排队点:

- WebSocket 连接上的消息读写调度。
- Tomcat/WebSocket 容器线程调度。
- Spring 事务代理和数据库连接获取。
- MQ 后续消费者处理速度。
- 压测客户端本身的调度和消息匹配开销。

因此，异步 confirm 解决的是“入口方法内部等待 ACK”的问题，不等于把整个系统吞吐直接提升数倍。

## 8. Worktree 双版本复测

为了排除历史测试时间不同、环境状态不同带来的干扰，后续使用 Git worktree 重新构建两个独立版本，并用同一套脚本复测。

复测版本:

| 版本 | Worktree | Commit |
|---|---|---|
| 同步 confirm | `D:\biliibli-sync-confirm` | `61d8a07 feat add im send path metrics` |
| 异步 confirm | `D:\biliibli-async-confirm` | `382ed72 feat make im mq confirms asynchronous` |

复测结果目录:

```text
loadtest/results/runs/compare/confirm-worktree-compare-20260417-134044
```

复测流程:

1. 分别 checkout 两个 commit 到独立 worktree。
2. 每个版本独立构建 Docker 镜像。
3. 每轮启动前执行 `docker compose down -v` 清理本地数据。
4. 每个压测场景前清空 RabbitMQ IM 测试队列。
5. 使用相同 K6 脚本、相同参数、相同本机 Docker 环境压测。
6. 每轮采集 K6 summary、Prometheus 指标和 RabbitMQ 队列状态。

### 8.1 50 VU 突发测试

测试参数:

```text
VUS=50
MESSAGES_PER_VU=300
总消息数=15000
```

客户端结果:

| 版本 | accepted | 成功率 | accepted 吞吐 | Avg | Median | P90 | P95 | Max |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| 同步 confirm | 15000/15000 | 100% | 1131.0/s | 6420.02ms | 6572.5ms | 10794.1ms | 11318.05ms | 11861ms |
| 异步 confirm | 15000/15000 | 100% | 1733.3/s | 4074.36ms | 4244.5ms | 6747.1ms | 7028ms | 7335ms |

服务端指标:

| 指标 | 同步 confirm | 异步 confirm | 变化 |
|---|---:|---:|---:|
| `im.send.accept.total` Avg | 7.076ms | 3.828ms | 下降约 45.9% |
| `im.send.accept.total` P95 bucket | 11.185ms | 6.991ms | 下降约 37.5% |
| `im.send.accept.publish` Avg | 4.606ms | 0.382ms | 下降约 91.7% |
| `im.send.accept.publish` P95 bucket | 6.991ms | 1.398ms | 下降约 80.0% |
| `im.mq.publish.confirm` Avg | 4.444ms | 7.718ms | 异步后台观察 |
| `im.mq.publish.confirm` P95 bucket | 6.991ms | 13.981ms | 不再阻塞入口 |
| Hikari acquire Avg | 20.109ms | 11.930ms | 下降约 40.7% |
| Hikari usage Avg | 5.457ms | 3.526ms | 下降约 35.4% |

RabbitMQ 队列残留:

| 版本 | 队列总残留 | persist 残留 | conversation 残留 |
|---|---:|---:|---:|
| 同步 confirm | 24240 | 11794 | 12446 |
| 异步 confirm | 24407 | 11909 | 12498 |

突发测试结论:

1. 异步 confirm 将入口 accepted 吞吐从 `1131.0/s` 提升到 `1733.3/s`，提升约 `53.3%`。
2. 客户端 accepted P95 从 `11318.05ms` 降到 `7028ms`，下降约 `37.9%`。
3. 服务端 `publish` P95 bucket 从 `6.991ms` 降到 `1.398ms`，入口不再同步等待 RabbitMQ confirm。
4. 两个版本队列残留接近，说明这个优化主要改善入口 accepted，不直接提升后续消费者能力。

### 8.2 RATE=2000 恒定速率测试

测试参数:

```text
RATE=2000
VUS=50
DURATION_SECONDS=30
总消息数=60000
```

客户端结果:

| 版本 | sent | accepted | 成功率 | timeout/error | accepted 吞吐 | Avg | Median | P90 | P95 | Max |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| 同步 confirm | 60000 | 47738 | 79.56% | 12262 | 782.1/s | 5145.00ms | 5115ms | 9309ms | 9764.15ms | 10497ms |
| 异步 confirm | 60000 | 59817 | 99.70% | 183 | 974.2/s | 6.02ms | 5ms | 8ms | 12ms | 77ms |

服务端指标:

| 指标 | 同步 confirm | 异步 confirm | 变化 |
|---|---:|---:|---:|
| `im.send.accept.total` Avg | 6.411ms | 2.749ms | 下降约 57.1% |
| `im.send.accept.total` P95 bucket | 8.389ms | 4.194ms | 下降约 50.0% |
| `im.send.accept.publish` Avg | 4.220ms | 0.211ms | 下降约 95.0% |
| `im.send.accept.publish` P95 bucket | 5.592ms | 1ms | 下降约 82.1% |
| `im.mq.publish.confirm` Avg | 4.108ms | 6.929ms | 异步后台观察 |
| `im.mq.publish.confirm` P95 bucket | 5.592ms | 11.185ms | 不再阻塞入口 |
| Hikari acquire Avg | 21.593ms | 0.571ms | 明显下降 |
| Hikari usage Avg | 5.413ms | 2.944ms | 下降约 45.6% |

RabbitMQ 队列残留:

| 版本 | 队列总残留 | persist 残留 | conversation 残留 |
|---|---:|---:|---:|
| 同步 confirm | 99964 | 48760 | 51204 |
| 异步 confirm | 85997 | 41000 | 44997 |

恒定速率测试结论:

1. 在 `RATE=2000` 下，同步 confirm 版本 accepted 成功率只有 `79.56%`，出现 `12262` 条 timeout。
2. 同样压力下，异步 confirm 版本 accepted 成功率达到 `99.70%`，P95 只有 `12ms`。
3. 该结果比突发测试更能说明问题: 同步等待 confirm 会显著限制入口持续承载能力。
4. 异步后队列仍有明显残留，说明后续消费者仍是全链路瓶颈。

### 8.3 Worktree 复测结论

这次复测比历史结果更适合作为正式对照实验，因为它保证了:

- 同一机器。
- 同一压测脚本。
- 同一 Docker Compose 环境。
- 同一测试参数。
- 两个明确 Git commit。
- 每轮清理本地数据和 RabbitMQ 队列。

复测结论:

1. 异步 confirm 明显提升入口 accepted 能力。
2. 同步 confirm 在 `RATE=2000` 下出现大量 timeout，异步 confirm 基本能承受同样入口压力。
3. `im.send.accept.publish` 是改善最明显的服务端指标。
4. `im.mq.publish.confirm` 在异步版本中仍然存在，但它变成后台 callback 观察到的延迟，不再阻塞 `send_message_accepted`。
5. RabbitMQ 后续队列仍然积压，因此异步 confirm 不是全链路吞吐优化的终点。

## 9. 当前阶段结论

1. 同步 confirm 版本中，RabbitMQ publisher confirm 是入口 `publish` 阶段的主要耗时。
2. 改为异步 confirm 后，入口内 `publish` P95 从历史测试的 `6.951ms` 降到 `1.366ms`；worktree 复测中从 `6.991ms` 降到 `1.398ms`。
3. 在 worktree 复测的突发场景中，入口 accepted 吞吐从 `1131.0/s` 提升到 `1733.3/s`。
4. 在 `RATE=2000` 恒定速率场景中，同步版本 accepted 成功率为 `79.56%`，异步版本为 `99.70%`。
5. 异步 confirm 对入口 accepted 有明确收益，但 RabbitMQ 后续队列仍会积压。
6. 后续要继续优化，需要把排队位置进一步拆到 WebSocket 边界、事务代理、Hikari 获取连接、消费者处理和 RabbitMQ 队列积压。

## 10. 后续测评方向

下一阶段建议继续分层测:

1. 入口层: `send_message` 到 `send_message_accepted`。
2. 事务层: 方法外事务代理、连接获取、commit/release。
3. MQ 发布层: RabbitTemplate send、confirm callback、pending confirm 数。
4. 消费者层: persist、conversation、redis、realtime、recent。
5. 全链路层: RabbitMQ 队列是否持续积压，消息最终是否完成落库和查询。

当前报告只证明异步 confirm 对入口耗时有明显帮助，不证明全链路稳定吞吐已经达到峰值 accepted 吞吐。

