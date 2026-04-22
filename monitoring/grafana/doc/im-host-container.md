# IM 主机与容器

对应仪表盘文件：`../dashboards/im-host-container.json`

## 监控范围

这个页面用于观察宿主机资源和业务容器网络流量。

监控对象：

- 宿主机 CPU、Load、磁盘、网络
- 业务容器网络 IO：
  - `bilibili-app`
  - `bilibili-mysql`
  - `bilibili-redis`
  - `bilibili-rabbitmq`
  - `bilibili-nginx`
  - `bilibili-minio`

## 采集接口

宿主机指标来自 `node-exporter`。

Prometheus scrape 配置：

```yaml
job_name: node
metrics_path: /metrics
targets:
  - node-exporter:9100
```

容器网络指标来自 `cAdvisor`。

```yaml
job_name: cadvisor
metrics_path: /metrics
targets:
  - cadvisor:8080
```

本机调试入口：

- node-exporter：`http://localhost:9100/metrics`
- cAdvisor：`http://localhost:8081/metrics`

## 图表说明

| 图表 | 监控对象 | 指标来源 | 含义 |
| --- | --- | --- | --- |
| 主机 CPU 与负载 | 宿主机 | `node_cpu_seconds_total`、`node_load1`、`node_load5` | 宿主机 CPU 使用率和负载 |
| 主机磁盘与网络 | 宿主机 | `node_filesystem_*`、`node_network_*` | 宿主机磁盘占用、网卡收发速率 |
| 容器网络 IO | 业务容器 | `container_network_receive_bytes_total`、`container_network_transmit_bytes_total` | 每个业务容器的网络收发速率 |

## 注意事项

`node-exporter` 监控宿主机，不按容器区分。容器级别资源由 `cAdvisor` 提供。
