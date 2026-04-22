# IM MQ / RabbitMQ

对应仪表盘文件：`im-mq-rabbitmq.json`

## 监控范围

这个页面用于观察 IM 消息队列的积压、发布、投递和消费情况。

监控对象：

- RabbitMQ 服务：`bilibili-rabbitmq`
- IM 业务队列：`im.message.*`
- Spring Boot 内部 MQ 消费埋点：`im.mq.consumer.*`

## 采集接口

RabbitMQ 指标来自 RabbitMQ Prometheus 插件。

Prometheus scrape 配置：

```yaml
job_name: rabbitmq
metrics_path: /metrics
targets:
  - rabbitmq:15692
```

Spring Boot 内部 MQ 消费指标来自 Actuator Prometheus 端点。

```yaml
job_name: spring-boot-app
metrics_path: /actuator/prometheus
targets:
  - app:8080
```

本机调试入口：

- RabbitMQ metrics：容器网络内为 `http://rabbitmq:15692/metrics`
- RabbitMQ 管理台：`http://localhost:15672`
- Spring Boot metrics：`http://localhost:8080/actuator/prometheus`

## 图表说明

| 图表 | 监控对象 | 指标来源 | 含义 |
| --- | --- | --- | --- |
| RabbitMQ IM 队列积压 | `im.message.*` 队列 | `rabbitmq_queue_messages_ready`、`rabbitmq_queue_messages_unacked` | 队列待消费和已投递未确认消息数 |
| IM MQ 消费吞吐 | Spring Boot MQ consumer | `im_mq_consumer_messages_total`、`im_mq_consumer_errors_total` | 各 consumer 的消费速率和错误速率 |
| IM MQ 消费耗时与滞后 | Spring Boot MQ consumer | `im_mq_consumer_duration_seconds_*`、`im_mq_consumer_lag_seconds_*` | 消费处理耗时和从发送到消费完成的滞后 |
| RabbitMQ 发布与投递 | RabbitMQ broker | `rabbitmq_global_messages_received_total`、`rabbitmq_global_messages_routed_total`、`rabbitmq_global_messages_delivered_total`、`rabbitmq_global_messages_acknowledged_total` | broker 全局消息接收、路由、投递、确认速率 |

## 注意事项

RabbitMQ 的 `/metrics` 只能说明 broker 和队列状态；业务 consumer 是否处理成功、耗时多少，要看 Spring Boot 的 `im.mq.consumer.*` 埋点。
