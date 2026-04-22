# IM 总览 / 健康状态

对应仪表盘文件：`../dashboards/im-health-status.json`

## 监控范围

这个页面用于快速判断核心业务容器是否存在明显资源异常。它只看容器级别的 CPU 和内存，不读取应用业务接口。

监控对象：

- `bilibili-app`
- `bilibili-mysql`
- `bilibili-redis`
- `bilibili-rabbitmq`

## 采集接口

这些指标来自 `cAdvisor`。

Prometheus scrape 配置：

```yaml
job_name: cadvisor
metrics_path: /metrics
targets:
  - cadvisor:8080
```

本机调试入口：

- cAdvisor Web/metrics：`http://localhost:8081/metrics`
- Prometheus 查询入口：`http://localhost:19090`

## 图表说明

| 图表 | 监控对象 | 指标来源 | 含义 |
| --- | --- | --- | --- |
| 核心容器 CPU 使用率 | app、mysql、redis、rabbitmq 容器 | `container_cpu_usage_seconds_total` | 按容器统计 CPU 使用速率 |
| 核心容器内存 | app、mysql、redis、rabbitmq 容器 | `container_memory_working_set_bytes` | 容器当前工作集内存 |

## 注意事项

cAdvisor 看到的是 Docker 容器资源，不知道应用内部的接口耗时、WebSocket 在线数、MQ 消费状态。应用内部指标要看 Spring Boot、WebSocket、发送链路、MQ、DB 等分类页面。
