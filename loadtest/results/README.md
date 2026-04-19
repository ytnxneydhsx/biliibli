# 压测结果目录

本目录按文件性质拆分：

- `runs/`：原始压测运行产物，按场景类型分组。
- `reports/`：由结果产物生成的 Markdown 分析报告。
- `latest/`：指向最近一次相关 run 的指针文件。

## runs 分组

| 目录 | 内容 |
|---|---|
| `runs/im-online-pairs-ramp/` | IM 在线用户对阶梯压测 |
| `runs/im-online/` | IM 在线用户固定用户对压测 |
| `runs/im-db-breakdown/` | IM DB 细分分析压测 |
| `runs/im-mq/` | IM MQ listener / metrics A/B 压测 |
| `runs/im-ws/` | IM WebSocket accepted、rate、full-chain 等历史压测 |
| `runs/compare/` | worktree / 配置对比压测 |

结果报告不和原始 run 目录混放，应放在 `reports/`。

## reports

`reports/` 集中存放人工整理报告和由压测结果生成的 Markdown 分析报告。原来分散在 `docs/im_*` 下的 IM 压测报告也统一放到这里。
