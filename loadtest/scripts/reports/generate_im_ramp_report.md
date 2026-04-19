# IM 阶梯压测报告生成脚本

对应脚本：`generate_im_ramp_report.ps1`

## 用途

从 IM 阶梯压测结果目录读取 k6、RabbitMQ、Prometheus 和 MySQL 产物，生成中文 Markdown 明细报告。

## 输入输出

| 参数 | 含义 |
|---|---|
| `Root` | 结果根目录。可以是包含多个 run 目录的上层目录，也可以是单次 run 目录；为空时默认读取 `results/runs/im-online-pairs-ramp/` |
| `OutputPath` | 生成的 Markdown 报告路径；为空时默认写入 `results/reports/` |

## 报告结构

默认生成以下章节：

- 数据范围
- 压测场景
- K6 汇总
- RabbitMQ 队列积压汇总
- 应用侧 DB 操作指标
- 应用侧 MQ Consumer 指标
- MySQL Digest Top 语句
- MySQL Waits Top 事件
- 原始文件索引

默认不生成 `MySQL Slow Log 分组 Top 行` 章节；slow log 文件只保留在原始文件索引中。

## 指标口径

- K6 指标来自每个挡位的 summary 文件
- RabbitMQ 最大积压来自压测期间队列采样
- `>=100 采样次数` 表示该队列在采样时积压不少于 100 条的次数
- `Consumer 最小 / 最大 / 最后` 来自 RabbitMQ 队列采样，不代表固定配置线程数
- 应用侧 DB 和 MQ 指标来自 Prometheus timer/counter 的 `after - before` 差值
- MySQL Digest 和 Waits 来自每个挡位导出的数据库统计文件
