# IM 发送链路

对应仪表盘文件：`im-send-pipeline.json`

## 监控范围

这个页面用于观察 IM 消息发送入口和 MQ 发布确认链路。

监控对象：

- Spring Boot 发送入口埋点：`im.send.accept.*`
- Spring Boot RabbitMQ 发布确认埋点：`im.mq.publish.confirm.*`

## 采集接口

这些指标由 Spring Boot 应用通过 Micrometer 暴露到 Actuator Prometheus 端点。

Prometheus scrape 配置：

```yaml
job_name: spring-boot-app
metrics_path: /actuator/prometheus
targets:
  - app:8080
```

本机调试入口：

- Spring Boot metrics：`http://localhost:8080/actuator/prometheus`

## 图表说明

| 图表 | 监控对象 | 指标来源 | 含义 |
| --- | --- | --- | --- |
| IM 发送接入耗时 | Spring Boot 发送入口 | `im_send_accept_total_seconds_*`、`im_send_accept_validation_seconds_*`、`im_send_accept_conversation_seconds_*`、`im_send_accept_publish_seconds_*` | 发送请求整体耗时、校验耗时、会话解析耗时、发布调用耗时 |
| IM MQ 发布确认 | Spring Boot MQ publisher | `im_mq_publish_confirm_ack_total`、`im_mq_publish_confirm_nack_total`、`im_mq_publish_confirm_timeout_total`、`im_mq_publish_confirm_retry_total`、`im_mq_publish_confirm_pending` | RabbitMQ publisher confirm 的 ack、nack、超时、重试和待确认数量 |

## 注意事项

这个页面依赖业务发送流程被触发。没有发送消息时，相关 Timer/Counter 可能还没有生成时间序列，Grafana 会显示 `No data`。
