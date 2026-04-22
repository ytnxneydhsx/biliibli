# IM Minimal Overview

对应仪表盘文件：`im-minimal-overview.json`

## 监控范围

这是完整总览页，按 Row 聚合所有分类页面的核心图表。它覆盖容器资源、Spring Boot、WebSocket、发送链路、RabbitMQ、MySQL、Redis、宿主机和监控系统自身。

分类页面：

- `im-health-status.json`：总览 / 健康状态
- `im-spring-boot-application.json`：应用层 / Spring Boot
- `im-websocket-realtime.json`：WebSocket 实时链路
- `im-send-pipeline.json`：发送链路
- `im-mq-rabbitmq.json`：MQ / RabbitMQ
- `im-mysql-redis.json`：MySQL / Redis
- `im-host-container.json`：主机与容器
- `im-monitoring-system.json`：监控系统自身

## 采集接口总表

| 监控对象 | Prometheus job | 采集接口 | 说明 |
| --- | --- | --- | --- |
| Spring Boot 应用 | `spring-boot-app` | `app:8080/actuator/prometheus` | Actuator + Micrometer 暴露 JVM、HTTP、IM 业务埋点 |
| RabbitMQ | `rabbitmq` | `rabbitmq:15692/metrics` | RabbitMQ Prometheus 插件暴露 broker 和队列指标 |
| Redis | `redis` | `redis-exporter:9121/metrics` | redis-exporter 连接 `redis://redis:6379` 后暴露 Redis 指标 |
| MySQL | `mysql` | `mysqld-exporter:9104/metrics` | mysqld-exporter 连接 `mysql:3306` 后暴露 MySQL 指标 |
| 容器资源 | `cadvisor` | `cadvisor:8080/metrics` | cAdvisor 暴露 Docker 容器 CPU、内存、网络等指标 |
| 宿主机资源 | `node` | `node-exporter:9100/metrics` | node-exporter 暴露宿主机 CPU、负载、磁盘、网络等指标 |
| Prometheus | `prometheus` | `prometheus:9090/metrics` | Prometheus 自身运行指标 |
| Grafana | `grafana` | `grafana:3000/metrics` | Grafana 自身运行指标 |

## 本机调试入口

- Grafana：`http://localhost:3000`
- Prometheus：`http://localhost:19090`
- Spring Boot metrics：`http://localhost:8080/actuator/prometheus`
- cAdvisor：`http://localhost:8081/metrics`
- node-exporter：`http://localhost:9100/metrics`
- redis-exporter：`http://localhost:9121/metrics`
- mysqld-exporter：`http://localhost:9104/metrics`
- RabbitMQ 管理台：`http://localhost:15672`

## 图表分组

| 分组 | 监控对象 | 对应分类文档 |
| --- | --- | --- |
| 一、总览 / 健康状态 | 核心容器 CPU、内存 | `im-health-status.md` |
| 二、应用层 / Spring Boot | JVM、进程、HTTP | `im-spring-boot-application.md` |
| 三、IM WebSocket 实时链路 | WebSocket 连接、心跳、消息处理 | `im-websocket-realtime.md` |
| 四、IM 发送链路 | 发送入口、MQ 发布确认 | `im-send-pipeline.md` |
| 五、MQ / RabbitMQ | RabbitMQ 队列、broker、IM MQ consumer | `im-mq-rabbitmq.md` |
| 六、MySQL / Redis | MySQL、Redis、IM DB 操作 | `im-mysql-redis.md` |
| 七、主机与容器 | 宿主机、容器网络 | `im-host-container.md` |
| 八、监控系统自身 | Prometheus、Grafana | `im-monitoring-system.md` |

## 注意事项

总览页用于快速排查。需要看某一类指标的细节时，优先打开对应分类仪表盘和对应 `.md` 文档。
