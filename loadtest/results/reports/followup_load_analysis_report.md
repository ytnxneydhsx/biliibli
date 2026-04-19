# IM WebSocket 后续分层压测分析报告

报告生成时间: 2026-04-17 13:19:26 +08:00

报告更新时间: 2026-04-17 14:06:00 +08:00

对应代码 Git 版本:

- 分支: `codex/im-backend-load-test-fixes`
- HEAD: `008ec168b6819c0725fef3b3efc5d3e95c1f7234`
- HEAD 摘要: `008ec16 2026-04-17T00:18:59+08:00 feat add im websocket boundary metrics`
- 工作区状态: 后端已提交代码无未提交修改；存在未跟踪的本地压测脚本、压测结果和日志目录。

本报告覆盖“MQ confirm 异步化之后”的后续测试，重点是分清:

- WebSocket 入口 accepted 能力。
- 服务端边界耗时和业务方法内部耗时。
- RabbitMQ 后续消费者是否积压。
- 全链路稳定处理能力。

## 1. 测试目标

前一阶段已经验证: 把 RabbitMQ publisher confirm 从同步等待改为异步回调后，入口方法内部耗时明显下降。

后续测试继续回答三个问题:

1. 异步 confirm 后，入口 accepted 还能打到多高？
2. 客户端 accepted 延迟和服务端内部埋点为什么不完全一致？
3. 如果要求 RabbitMQ 队列不持续积压，系统全链路稳定能力是多少？

## 2. 测试口径

### 2.1 入口 accepted 口径

K6 脚本发送 WebSocket `send_message`，并使用 `clientMessageId` 精确匹配:

```text
type == send_message_accepted
data.clientMessageId == 当前消息 clientMessageId
```

该口径只表示入口已经 accepted，不代表消费者已经落库或更新会话。

### 2.2 恒定速率口径

后续新增恒定速率脚本:

```text
loadtest/scripts/scenarios/im_ws_constant_rate.js
```

脚本中的 `RATE` 是全局目标速率，不是单个 VU 的速率:

```text
总消息数 = RATE * DURATION_SECONDS
每个 VU 发送数 = 总消息数 / VUS
每个 VU 发送间隔 = 1000ms * VUS / RATE
```

例如:

```text
RATE=300, VUS=20, DURATION_SECONDS=30
总发送数 = 9000
20 个 VU 合计约 300 msg/s
```

### 2.3 全链路不积压口径

全链路稳定能力不只看 `send_message_accepted`，还要求压测结束并等待一段时间后，RabbitMQ 关键队列不持续积压:

- `im.message.persist.queue`
- `im.message.conversation.queue`
- `im.message.conversation.redis.queue`
- `im.message.realtime.queue`
- `im.message.recent.cache.queue`

## 3. WebSocket 边界埋点测试

代表结果目录:

```text
loadtest/results/runs/im-ws/im-ws-boundary-metrics-50vu-20260417-001711
```

客户端 accepted 结果:

| 指标 | 数值 |
|---|---:|
| sent | 15000 |
| accepted | 15000 |
| accepted 成功率 | 100% |
| accepted 吞吐 | 1821.6/s |
| accepted Avg | 3890.41ms |
| accepted Median | 4112.5ms |
| accepted P90 | 6408.1ms |
| accepted P95 | 6686.05ms |
| accepted Max | 7011ms |

服务端边界指标，按 Prometheus before/after 差分计算:

| 指标 | Count | Avg | P95 bucket 上界 | 含义 |
|---|---:|---:|---:|---|
| `im.send.accept.total` | 15000 | 3.665ms | 6.991ms | `acceptMessage` 方法内部 |
| `im.ws.inbound.handle` | 15000 | 22.957ms | 50.332ms | WebSocket handler 总处理 |
| `im.ws.protocol.dispatch` | 15000 | 22.810ms | 50.332ms | 协议分发 |
| `im.ws.protocol.accept_call` | 15000 | 22.097ms | 50.332ms | 调用应用服务 accepted |
| `im.ws.outbound.send` | 15000 | 0.067ms | 1ms | accepted WebSocket 发送 |
| `im.ws.push.send` | 14953 | 0.072ms | 1ms | `message_received` 推送发送 |

Hikari 连接池:

| 指标 | 数值 |
|---|---:|
| acquire count | 20259 |
| acquire avg | 13.625ms |
| acquire max | 133.661ms |
| usage count | 20257 |
| usage avg | 3.609ms |
| usage max | 59ms |
| after active | 2 |
| after pending | 0 |
| timeout total | 0 |

### 3.1 边界埋点结论

`acceptMessage` 方法内部 P95 只有约 `6.991ms`，但 WebSocket handler/dispatch/accept_call 的 P95 bucket 已经到约 `50.332ms`。这说明入口 accepted 链路的耗时不只在业务方法内部。

结合 Hikari acquire avg `13.625ms`、max `133.661ms`，可以推断一部分时间消耗在事务代理、数据库连接获取、容器线程调度等业务方法外层环节。

同时，WebSocket outbound send 本身非常快，`send_message_accepted` 写回不是主要瓶颈。

## 4. 入口恒定速率阶梯测试

代表结果目录:

```text
loadtest/results/runs/im-ws/im-ws-rate-ladder-20260417-002737
loadtest/results/runs/im-ws/im-ws-rate-debug-1500-20260417-003526
loadtest/results/runs/im-ws/im-ws-rate-confirm-20260417-003623
```

| RATE | sent | accepted | 成功率 | accepted 吞吐 | Avg | Median | P90 | P95 | Max | 备注 |
|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|
| 500 | 15000 | 15000 | 100% | 439.1/s | 4.32ms | 4ms | 4ms | 5ms | 2364ms | 正常 |
| 1000 | 30000 | 30000 | 100% | 939.4/s | 5.01ms | 4ms | 5ms | 5ms | 898ms | 正常 |
| 1500 | 45000 | 45000 | 100% | 1368.6/s | 6.40ms | 4ms | 6ms | 8ms | 1339ms | 复测正常 |
| 2000 | 60000 | 60000 | 100% | 1814.8/s | 8.78ms | 5ms | 8ms | 20ms | 1343ms | 正常 |
| 2500 | 75000 | 75000 | 100% | 2269.4/s | 16.72ms | 8ms | 40ms | 58ms | 1328ms | 入口仍可承载 |
| 3000 | 90000 | 90000 | 100% | 2422.4/s | 1877.91ms | 1779ms | 3566ms | 3809ms | 4456ms | 明显排队 |
| 3500 | 105000 | 82645 | 78.71% | 1351.1/s | 5250.42ms | 5411ms | 9181ms | 9680ms | 11225ms | 过载 |

### 4.1 入口恒定速率结论

只看 `send_message_accepted` 入口能力:

```text
2500 msg/s 左右仍能全部 accepted，P95 约 58ms。
3000 msg/s 虽然成功率仍为 100%，但 P95 已经到 3.8s，说明开始明显排队。
3500 msg/s 成功率下降到 78.71%，系统入口过载。
```

因此，当前本机环境下，入口 accepted 的实用上限更接近 `2500 msg/s`，不建议把 `3000 msg/s` 视为稳定能力。

## 5. 全链路不积压测试

代表结果目录:

```text
loadtest/results/runs/im-ws/im-ws-fullchain-ladder-20260417-004144
loadtest/results/runs/im-ws/im-ws-fullchain-refine-20260417-004603
loadtest/results/runs/im-ws/im-ws-fullchain-final-20260417-004825
loadtest/results/runs/im-ws/im-ws-fullchain-375-clean-20260417-005044
```

压测方法:

1. 清空本地 RabbitMQ IM 测试队列。
2. 按固定 RATE 发送 30 秒。
3. 压测结束后额外等待 15 秒。
4. 读取 RabbitMQ 队列剩余消息数。

| RATE | sent | accepted | 成功率 | accepted P95 | 队列残留 | persist 残留 | conversation 残留 | 结论 |
|---:|---:|---:|---:|---:|---:|---:|---:|---|
| 100 | 3000 | 3000 | 100% | 5ms | 0 | 0 | 0 | 稳定 |
| 200 | 6000 | 6000 | 100% | 4ms | 0 | 0 | 0 | 稳定 |
| 300 | 9000 | 9000 | 100% | 4ms | 0 | 0 | 0 | 稳定 |
| 350 | 10500 | 10500 | 100% | 5ms | 527 | 0 | 527 | 开始积压 |
| 375 clean | 11240 | 11240 | 100% | 4ms | 1218 | 0 | 1218 | 积压确认 |
| 400 | 12000 | 12000 | 100% | 4ms | 673 | 0 | 673 | 积压确认 |
| 450 | 13500 | 13500 | 100% | 4ms | 5205 | 1774 | 3431 | 明显积压 |

### 5.1 全链路结论

如果评价标准是“入口 accepted 快”，系统可以承载远高于 300 msg/s 的发送。

但如果评价标准是“后续消费者也能消化，RabbitMQ 不持续积压”，当前本机稳定能力应保守看作:

```text
约 300 msg/s
```

从 350 msg/s 开始，`im.message.conversation.queue` 已经出现残留。450 msg/s 时，`im.message.persist.queue` 和 `im.message.conversation.queue` 都出现明显积压。

## 6. 关键判断

### 6.1 Worktree 复测对后续分析的补充

为验证“异步 confirm 主要提升入口 accepted，而不是直接解决全链路消费者瓶颈”，后续补做了 worktree 双版本复测。

复测版本:

| 版本 | Worktree | Commit |
|---|---|---|
| 同步 confirm | `D:\biliibli-sync-confirm` | `61d8a07 feat add im send path metrics` |
| 异步 confirm | `D:\biliibli-async-confirm` | `382ed72 feat make im mq confirms asynchronous` |

结果目录:

```text
loadtest/results/runs/compare/confirm-worktree-compare-20260417-134044
```

#### 6.1.1 突发场景对比

参数:

```text
VUS=50
MESSAGES_PER_VU=300
总消息数=15000
```

| 版本 | accepted | 成功率 | accepted 吞吐 | Avg | P95 | 队列残留 | persist 残留 | conversation 残留 |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| 同步 confirm | 15000/15000 | 100% | 1131.0/s | 6420.02ms | 11318.05ms | 24240 | 11794 | 12446 |
| 异步 confirm | 15000/15000 | 100% | 1733.3/s | 4074.36ms | 7028ms | 24407 | 11909 | 12498 |

服务端入口指标:

| 指标 | 同步 confirm | 异步 confirm |
|---|---:|---:|
| `im.send.accept.total` Avg | 7.076ms | 3.828ms |
| `im.send.accept.total` P95 bucket | 11.185ms | 6.991ms |
| `im.send.accept.publish` Avg | 4.606ms | 0.382ms |
| `im.send.accept.publish` P95 bucket | 6.991ms | 1.398ms |
| Hikari acquire Avg | 20.109ms | 11.930ms |

突发场景说明:

```text
入口 accepted 明显提升，但队列残留几乎没有改善。
```

这说明异步 confirm 解决的是入口等待 ACK 的问题，后续消费者处理能力没有因为这个改动同步提升。

#### 6.1.2 RATE=2000 恒定速率对比

参数:

```text
RATE=2000
VUS=50
DURATION_SECONDS=30
总消息数=60000
```

| 版本 | sent | accepted | 成功率 | timeout/error | Avg | P95 | 队列残留 | persist 残留 | conversation 残留 |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| 同步 confirm | 60000 | 47738 | 79.56% | 12262 | 5145.00ms | 9764.15ms | 99964 | 48760 | 51204 |
| 异步 confirm | 60000 | 59817 | 99.70% | 183 | 6.02ms | 12ms | 85997 | 41000 | 44997 |

服务端入口指标:

| 指标 | 同步 confirm | 异步 confirm |
|---|---:|---:|
| `im.send.accept.total` Avg | 6.411ms | 2.749ms |
| `im.send.accept.total` P95 bucket | 8.389ms | 4.194ms |
| `im.send.accept.publish` Avg | 4.220ms | 0.211ms |
| `im.send.accept.publish` P95 bucket | 5.592ms | 1ms |
| Hikari acquire Avg | 21.593ms | 0.571ms |

恒定速率场景说明:

```text
同步 confirm 在 RATE=2000 下入口已经大量 timeout。
异步 confirm 基本能完成 accepted，但 RabbitMQ 队列仍然有明显积压。
```

因此这组复测强化了本报告的分层结论:

- 入口 accepted 能力: 异步 confirm 明显更强。
- 全链路稳定能力: 仍受消费者队列处理速度限制。
- 主要后续瓶颈: `im.message.conversation.queue` 和 `im.message.persist.queue`。

### 6.2 为什么入口 2500 msg/s 和全链路 300 msg/s 不矛盾

入口 accepted 是前半段能力:

```text
WebSocket 收到消息 -> 入口校验 -> publish 到 MQ -> 返回 accepted
```

全链路稳定能力还包括后半段:

```text
MQ 消费 -> 消息落库 -> 会话窗口更新 -> Redis/缓存更新 -> 推送
```

当前系统的前半段明显快于后半段。因此高压下会出现:

```text
入口还能 accepted
但 RabbitMQ 后续队列开始堆积
```

### 6.3 当前最明显的后续瓶颈

从队列残留看，最先暴露的是:

```text
im.message.conversation.queue
```

在更高 RATE 下，`im.message.persist.queue` 也开始积压。

这说明后续优化不能只盯着发送入口，还要拆消费者:

- conversation 消费者处理逻辑。
- persist 消费者落库逻辑。
- 消费线程并发配置。
- 批量写入或批量更新可能性。
- 数据库索引和 SQL 耗时。

### 6.4 边界耗时的意义

服务端业务方法 `im.send.accept.total` 已经比较低，但 WebSocket 边界和事务外层仍然存在额外耗时。后续如果继续优化入口延迟，应重点看:

- Spring 事务代理和连接获取。
- Hikari acquire 延迟。
- WebSocket 容器线程调度。
- 单连接消息读取和写回顺序。
- 客户端突发发送导致的 socket 排队。

## 7. 当前能力结论

| 口径 | 当前本机结论 |
|---|---:|
| 入口 accepted 可承载 | 约 2500 msg/s |
| 入口开始明显排队 | 约 3000 msg/s |
| 入口明显过载 | 约 3500 msg/s |
| 全链路不积压稳定能力 | 约 300 msg/s |
| 首个明显后续瓶颈 | `im.message.conversation.queue` |

## 8. 后续报告建议

如果要写正式技术报告，可以把两份报告合并成完整链路:

1. 同步 confirm 阶段: 证明 RabbitMQ confirm 等待是入口方法内部主要耗时。
2. 异步 confirm 阶段: 证明入口方法内部耗时下降，但吞吐没有线性增长。
3. 边界埋点阶段: 证明方法外层、连接池和 WebSocket 调度仍有额外排队。
4. 全链路测试阶段: 证明真正稳定能力由消费者和队列积压决定。

最终表达应避免只报一个“系统 QPS”，而是按口径分别报告:

- 入口 accepted QPS。
- 入口 accepted P95。
- MQ confirm 延迟。
- RabbitMQ 队列积压。
- 消费者处理能力。
- 全链路稳定吞吐。

