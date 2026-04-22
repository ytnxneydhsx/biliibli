# IM 应用层 / Spring Boot

对应仪表盘文件：`im-spring-boot-application.json`

## 监控范围

这个页面用于观察 Spring Boot 应用自身运行状态和 HTTP 接口表现。

监控对象：

- Spring Boot 应用容器：`bilibili-app`
- JVM 内存和线程
- 进程 CPU
- HTTP 请求吞吐和延迟

## 采集接口

这些指标由 Spring Boot Actuator 和 Micrometer 暴露。

Prometheus scrape 配置：

```yaml
job_name: spring-boot-app
metrics_path: /actuator/prometheus
targets:
  - app:8080
```

本机调试入口：

- 通过 nginx 访问：`http://localhost:8080/actuator/prometheus`
- 容器网络内访问：`http://app:8080/actuator/prometheus`

## 图表说明

| 图表 | 监控对象 | 指标来源 | 含义 |
| --- | --- | --- | --- |
| Spring Boot JVM 内存 | JVM | `jvm_memory_used_bytes`、`jvm_memory_committed_bytes` | JVM heap/nonheap 已用和已提交内存 |
| Spring Boot CPU 与线程 | Spring Boot 进程/JVM | `process_cpu_usage`、`jvm_threads_live_threads`、`jvm_threads_daemon_threads` | 应用进程 CPU 使用率、活跃线程数、守护线程数 |
| HTTP 请求吞吐 | Spring MVC 接口 | `http_server_requests_seconds_count` | 按 method、uri、status 统计 HTTP 请求速率 |
| HTTP 平均延迟 | Spring MVC 接口 | `http_server_requests_seconds_sum`、`http_server_requests_seconds_count`、`http_server_requests_seconds_max` | HTTP 平均耗时和最大耗时 |

## 注意事项

这个页面反映 Spring Boot 应用内部状态。容器 CPU/内存请看总览或主机与容器页面；WebSocket、发送链路、MQ、DB 的业务埋点在对应分类页面。
