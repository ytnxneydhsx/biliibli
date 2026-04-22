# IM MySQL / Redis

对应仪表盘文件：`im-mysql-redis.json`

## 监控范围

这个页面用于观察数据库、缓存和应用内部 DB 操作状态。

监控对象：

- MySQL 服务：`bilibili-mysql`
- Redis 服务：`bilibili-redis`
- Spring Boot 内部 DB 操作埋点：`im.db.operation.*`

## 采集接口

MySQL 指标来自 `mysqld-exporter`。exporter 通过 `monitoring/mysql/.my.cnf` 中的只读监控用户连接 MySQL。

Prometheus scrape 配置：

```yaml
job_name: mysql
metrics_path: /metrics
targets:
  - mysqld-exporter:9104
```

Redis 指标来自 `redis-exporter`。exporter 通过 `REDIS_ADDR` 连接 Redis。

```yaml
job_name: redis
metrics_path: /metrics
targets:
  - redis-exporter:9121
```

Spring Boot DB 操作指标来自 Actuator Prometheus 端点。

```yaml
job_name: spring-boot-app
metrics_path: /actuator/prometheus
targets:
  - app:8080
```

本机调试入口：

- mysqld-exporter：`http://localhost:9104/metrics`
- redis-exporter：`http://localhost:9121/metrics`
- Spring Boot metrics：`http://localhost:8080/actuator/prometheus`

## 图表说明

| 图表 | 监控对象 | 指标来源 | 含义 |
| --- | --- | --- | --- |
| MySQL 连接状态 | MySQL | `mysql_global_status_threads_connected`、`mysql_global_status_threads_running` | 当前连接数和正在运行线程数 |
| MySQL 查询吞吐 | MySQL | `mysql_global_status_questions` | MySQL 查询请求速率 |
| IM DB 操作吞吐 | Spring Boot 应用 | `im_db_operation_calls_total`、`im_db_operation_errors_total` | IM 业务 DB 操作调用速率和错误速率 |
| IM DB 操作耗时 | Spring Boot 应用 | `im_db_operation_duration_seconds_*` | IM 业务 DB 操作平均耗时 |
| Redis 命令吞吐 | Redis | `redis_commands_processed_total` | Redis 命令处理速率 |
| Redis 内存与连接 | Redis | `redis_memory_used_bytes`、`redis_memory_max_bytes`、`redis_connected_clients` | Redis 内存使用和客户端连接数 |

## 注意事项

MySQL/Redis exporter 只能反映中间件自身状态；具体哪个 IM 操作慢或失败，要看 Spring Boot 暴露的 `im.db.operation.*` 业务埋点。
